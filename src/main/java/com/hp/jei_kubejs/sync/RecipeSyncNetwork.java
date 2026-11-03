package com.hp.jei_kubejs.sync;

import com.google.gson.JsonElement;
import com.hp.jei_kubejs.JeiKubejs;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.network.HandshakeHandler;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.IntSupplier;
import java.util.zip.GZIPOutputStream;

public final class RecipeSyncNetwork {
    private static final String VERSION = "1";
    private static final int CHUNK_SIZE = 900_000;
    private static final long WARN_BYTES = 4L * 1024 * 1024;
    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder.named(JeiKubejs.id("recipe_sync"))
            .networkProtocolVersion(() -> VERSION)
            .clientAcceptedVersions(NetworkRegistry.acceptMissingOr(VERSION))
            .serverAcceptedVersions(VERSION::equals)
            .simpleChannel();
    private static final Map<Connection, Request> REQUESTS = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<String, List<byte[]>> CACHE = new HashMap<>();
    private static int cacheGeneration = -1;

    private RecipeSyncNetwork() {
    }

    public static void register() {
        CHANNEL.messageBuilder(Query.class, 0, NetworkDirection.LOGIN_TO_CLIENT)
                .loginIndex(Query::getAsInt, Query::setLoginIndex)
                .encoder((msg, buf) -> {
                })
                .decoder(buf -> new Query())
                .markAsLoginPacket()
                .consumerNetworkThread((msg, ctx) -> {
                    SyncedRecipeJsons.clear();
                    CHANNEL.reply(new Request(RecipeSyncRequest.types(), RecipeSyncRequest.prefixes()), ctx.get());
                    ctx.get().setPacketHandled(true);
                })
                .add();
        CHANNEL.messageBuilder(Request.class, 1, NetworkDirection.LOGIN_TO_SERVER)
                .loginIndex(Request::getAsInt, Request::setLoginIndex)
                .encoder(Request::encode)
                .decoder(Request::decode)
                .consumerNetworkThread(HandshakeHandler.indexFirst((handler, msg, ctx) -> {
                    REQUESTS.put(ctx.get().getNetworkManager(), msg);
                    ctx.get().setPacketHandled(true);
                }))
                .add();
        CHANNEL.messageBuilder(Chunk.class, 2, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(Chunk::encode)
                .decoder(Chunk::decode)
                .consumerNetworkThread((msg, ctx) -> {
                    SyncedRecipeJsons.accept(msg.part(), msg.total(), msg.data());
                    ctx.get().setPacketHandled(true);
                })
                .add();
    }

    public static void onDatapackSync(OnDatapackSyncEvent event) {
        MinecraftServer server = event.getPlayerList().getServer();
        for (ServerPlayer player : event.getPlayers()) {
            Request request = REQUESTS.get(player.connection.connection);
            if (request == null) {
                continue;
            }
            List<byte[]> chunks = payloadFor(server, request);
            for (int i = 0; i < chunks.size(); i++) {
                CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new Chunk(i, chunks.size(), chunks.get(i)));
            }
        }
    }

    private static synchronized List<byte[]> payloadFor(MinecraftServer server, Request request) {
        int generation = RecipeJsonCapture.generation();
        if (generation != cacheGeneration) {
            CACHE.clear();
            cacheGeneration = generation;
        }
        return CACHE.computeIfAbsent(request.key(), key -> build(server, request));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static List<byte[]> build(MinecraftServer server, Request request) {
        RecipeManager manager = server.getRecipeManager();
        Set<ResourceLocation> ids = new LinkedHashSet<>();
        for (String typeId : request.types) {
            ResourceLocation rl = ResourceLocation.tryParse(typeId);
            RecipeType<?> type = rl == null ? null : BuiltInRegistries.RECIPE_TYPE.get(rl);
            if (type == null) {
                JeiKubejs.LOGGER.warn("[JEI KubeJS] 客户端声明同步的配方类型 {} 不存在，已跳过", typeId);
                continue;
            }
            for (Recipe<?> recipe : (List<Recipe<?>>) manager.getAllRecipesFor((RecipeType) type)) {
                ids.add(recipe.getId());
            }
        }
        if (!request.prefixes.isEmpty()) {
            for (Recipe<?> recipe : manager.getRecipes()) {
                String id = recipe.getId().toString();
                if (request.prefixes.stream().anyMatch(id::startsWith)) {
                    ids.add(recipe.getId());
                }
            }
        }

        long raw = 0;
        int missing = 0;
        List<String[]> entries = new ArrayList<>();
        for (ResourceLocation id : ids) {
            JsonElement json = RecipeJsonCapture.get(id);
            if (json == null) {
                missing++;
                continue;
            }
            String text = json.toString();
            raw += text.length();
            entries.add(new String[]{id.toString(), text});
        }

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(new GZIPOutputStream(bytes))) {
            out.writeInt(entries.size());
            for (String[] entry : entries) {
                writeString(out, entry[0]);
                writeString(out, entry[1]);
            }
        } catch (IOException e) {
            JeiKubejs.LOGGER.error("[JEI KubeJS] 打包配方 JSON 失败", e);
            return List.of();
        }
        byte[] all = bytes.toByteArray();
        List<byte[]> chunks = new ArrayList<>();
        for (int i = 0; i < all.length; i += CHUNK_SIZE) {
            chunks.add(Arrays.copyOfRange(all, i, Math.min(all.length, i + CHUNK_SIZE)));
        }
        JeiKubejs.LOGGER.info("[JEI KubeJS] 配方 JSON 同步：类型 {}，ID 前缀 {}，共 {} 条（{} 条没有 JSON），原始 {} KB，压缩后 {} KB，分 {} 个包",
                request.types, request.prefixes, entries.size(), missing, raw / 1024, all.length / 1024, chunks.size());
        if (all.length > WARN_BYTES) {
            JeiKubejs.LOGGER.warn("[JEI KubeJS] 配方 JSON 同步体积较大（压缩后 {} KB），会拖慢玩家进服，建议缩小 syncRecipeTypes / syncRecipeIds 的范围", all.length / 1024);
        }
        return chunks;
    }

    private static void writeString(DataOutputStream out, String text) throws IOException {
        byte[] data = text.getBytes(StandardCharsets.UTF_8);
        out.writeInt(data.length);
        out.write(data);
    }

    public static class Query implements IntSupplier {
        private int loginIndex;

        public Query() {
        }

        @Override
        public int getAsInt() {
            return loginIndex;
        }

        public void setLoginIndex(int loginIndex) {
            this.loginIndex = loginIndex;
        }
    }

    public static class Request implements IntSupplier {
        private final List<String> types;
        private final List<String> prefixes;
        private int loginIndex;

        public Request(List<String> types, List<String> prefixes) {
            this.types = types;
            this.prefixes = prefixes;
        }

        private static Request decode(FriendlyByteBuf buf) {
            return new Request(buf.readList(FriendlyByteBuf::readUtf), buf.readList(FriendlyByteBuf::readUtf));
        }

        private void encode(FriendlyByteBuf buf) {
            buf.writeCollection(types, FriendlyByteBuf::writeUtf);
            buf.writeCollection(prefixes, FriendlyByteBuf::writeUtf);
        }

        private String key() {
            return String.join(",", types) + "|" + String.join(",", prefixes);
        }

        @Override
        public int getAsInt() {
            return loginIndex;
        }

        public void setLoginIndex(int loginIndex) {
            this.loginIndex = loginIndex;
        }
    }

    public record Chunk(int part, int total, byte[] data) {
        private static Chunk decode(FriendlyByteBuf buf) {
            return new Chunk(buf.readVarInt(), buf.readVarInt(), buf.readByteArray());
        }

        private void encode(FriendlyByteBuf buf) {
            buf.writeVarInt(part);
            buf.writeVarInt(total);
            buf.writeByteArray(data);
        }
    }
}
