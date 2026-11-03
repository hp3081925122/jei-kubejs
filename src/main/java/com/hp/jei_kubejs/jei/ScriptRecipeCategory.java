package com.hp.jei_kubejs.jei;

import com.hp.jei_kubejs.JeiKubejs;
import com.hp.jei_kubejs.data.CategoryDef;
import com.hp.jei_kubejs.data.DrawCallback;
import com.hp.jei_kubejs.data.DrawContext;
import com.hp.jei_kubejs.data.ElementDef;
import com.hp.jei_kubejs.data.RecipeData;
import com.hp.jei_kubejs.data.SlotDef;
import mezz.jei.api.gui.builder.IIngredientAcceptor;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.gui.widgets.IScrollBoxWidget;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** 把脚本里的 CategoryDef 包装成 JEI 分类。每个脚本分类对应一个实例。 */
public class ScriptRecipeCategory implements IRecipeCategory<RecipeData> {
    private final CategoryDef def;
    private final RecipeType<RecipeData> recipeType;
    @Nullable
    private final IDrawable icon;
    /** 贴图和进度条的 JEI 可绘制对象，每个分类只创建一次 */
    private final Map<ElementDef, IDrawable> textureCache = new IdentityHashMap<>();
    /** 静态满火焰（flame 不带 ticks 时使用） */
    private final IDrawable flameFilled;
    /** 回调出错后停用，JEI 重载时会新建分类实例，自然恢复 */
    private boolean drawFailed;
    private boolean tooltipFailed;

    public ScriptRecipeCategory(CategoryDef def, IGuiHelper guiHelper) {
        this.def = def;
        this.flameFilled = guiHelper.getRecipeFlameFilled();
        this.recipeType = new RecipeType<>(def.getId(), RecipeData.class);
        // 没设图标时返回 null，JEI 会改用第一个催化剂作图标
        this.icon = def.getIcon().isEmpty() ? null : guiHelper.createDrawableItemStack(def.getIcon());

        for (ElementDef element : def.getElements()) {
            if (element instanceof ElementDef.Texture t) {
                textureCache.put(element, builder(guiHelper, t.tex()).build());
            } else if (element instanceof ElementDef.Progress p) {
                textureCache.put(element, builder(guiHelper, p.tex()).buildAnimated(p.ticks(), toJei(p.from()), false));
            }
        }
    }

    private static mezz.jei.api.gui.drawable.IDrawableBuilder builder(IGuiHelper guiHelper, ElementDef.Tex tex) {
        return guiHelper.drawableBuilder(tex.texture(), tex.u(), tex.v(), tex.width(), tex.height())
                .setTextureSize(tex.textureWidth(), tex.textureHeight());
    }

    public CategoryDef getDef() {
        return def;
    }

    @Override
    public RecipeType<RecipeData> getRecipeType() {
        return recipeType;
    }

    @Override
    public Component getTitle() {
        return def.getTitle();
    }

    @Override
    public int getWidth() {
        return def.getWidth();
    }

    @Override
    public int getHeight() {
        return def.getHeight();
    }

    @Override
    @Nullable
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeData recipe, IFocusGroup focuses) {
        // 按分类里定义的槽位顺序布局；配方没填的槽位只画空格子
        Map<String, IRecipeSlotBuilder> built = new HashMap<>();
        for (SlotDef slot : def.getSlots().values()) {
            IRecipeSlotBuilder slotBuilder = builder.addSlot(toJei(slot.role()), slot.x(), slot.y())
                    .setSlotName(slot.name());
            built.put(slot.name(), slotBuilder);
            List<ITypedIngredient<?>> copied = recipe.getCopied().get(slot.name());
            if (slot.isTank()) {
                slotBuilder.setFluidRenderer(slot.capacity(), false, slot.width(), slot.height())
                        .setBackground(new TankBackground(slot.width() + 2, slot.height() + 2), -1, -1);
                List<RecipeData.FluidEntry> fluids = recipe.getFluids().get(slot.name());
                if (copied != null) {
                    slotBuilder.addTypedIngredients(copied);
                } else if (fluids != null) {
                    for (RecipeData.FluidEntry fluid : fluids) {
                        slotBuilder.addFluidStack(fluid.fluid(), fluid.amount());
                    }
                }
            } else {
                if (slot.role() == SlotDef.Role.OUTPUT) {
                    slotBuilder.setOutputSlotBackground();
                } else {
                    slotBuilder.setStandardSlotBackground();
                }
                List<ItemStack> content = recipe.getSlotContents().get(slot.name());
                if (copied != null) {
                    slotBuilder.addTypedIngredients(copied);
                } else if (content != null && !content.isEmpty()) {
                    slotBuilder.addItemStacks(content);
                }
            }
            List<Component> tooltip = recipe.getSlotTooltips().get(slot.name());
            if (tooltip != null && !tooltip.isEmpty()) {
                slotBuilder.addRichTooltipCallback((view, lines) -> lines.addAll(tooltip));
            }
        }
        for (List<String> group : recipe.getLinks()) {
            builder.createFocusLink(group.stream().map(built::get).toArray(IIngredientAcceptor[]::new));
        }
    }

    @Override
    @Nullable
    public ResourceLocation getRegistryName(RecipeData recipe) {
        return recipe.getId();
    }

    /** 箭头、火焰、加号、贴图、进度条、提示区域；文字在 draw 里画 */
    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, RecipeData recipe, IFocusGroup focuses) {
        for (ElementDef element : def.getElements()) {
            try {
                switch (element) {
                    case ElementDef.Arrow a -> {
                        if (a.ticks() > 0) {
                            builder.addAnimatedRecipeArrow(a.ticks()).setPosition(a.x(), a.y());
                        } else {
                            builder.addRecipeArrow().setPosition(a.x(), a.y());
                        }
                    }
                    case ElementDef.Flame f -> {
                        if (f.ticks() > 0) {
                            builder.addAnimatedRecipeFlame(f.ticks()).setPosition(f.x(), f.y());
                        } else {
                            builder.addDrawable(flameFilled, f.x(), f.y());
                        }
                    }
                    case ElementDef.PlusSign p -> builder.addRecipePlusSign().setPosition(p.x(), p.y());
                    case ElementDef.Texture t -> builder.addDrawable(textureCache.get(t), t.x(), t.y());
                    case ElementDef.Progress p -> builder.addDrawable(textureCache.get(p), p.x(), p.y());
                    case ElementDef.Tooltip t -> builder.addTooltipArea(t.x(), t.y(), t.width(), t.height())
                            .setTooltip(t.lines());
                    case ElementDef.TooltipArea t -> {
                        List<Component> lines = recipe.getTooltips().get(t.name());
                        if (lines != null && !lines.isEmpty()) {
                            builder.addTooltipArea(t.x(), t.y(), t.width(), t.height()).setTooltip(lines);
                        }
                    }
                    case ElementDef.ScrollText s -> {
                        List<Component> lines = recipe.getScrollTexts().get(s.name());
                        if (lines != null && !lines.isEmpty()) {
                            // JEI 参数顺序：宽、高、x、y
                            builder.addScrollBoxWidget(s.width(), s.height(), s.x(), s.y())
                                    .setContents(List.<FormattedText>copyOf(lines));
                        }
                    }
                    case ElementDef.ScrollPanel p -> {
                        IScrollBoxWidget box = builder.addScrollBoxWidget(p.width(), p.height(), p.x(), p.y());
                        box.setContents(new CallbackDrawable(box.getContentAreaWidth(), p.contentHeight(), p.callback(), recipe));
                    }
                    case ElementDef.Label ignored -> {}
                    case ElementDef.Text ignored -> {}
                }
            } catch (Exception e) {
                JeiKubejs.LOGGER.error("[JEI KubeJS] 分类 {} 的元素 {} 创建失败", def.getId(), element, e);
            }
        }
    }

    @Override
    public void draw(RecipeData recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        for (ElementDef element : def.getElements()) {
            if (element instanceof ElementDef.Label l) {
                graphics.drawString(font, l.text(), l.x(), l.y(), l.color(), l.shadow());
            } else if (element instanceof ElementDef.Text t) {
                Component text = recipe.getTexts().get(t.name());
                if (text != null) {
                    graphics.drawString(font, text, t.x(), t.y(), t.color(), t.shadow());
                }
            }
        }
        DrawCallback callback = def.getDrawCallback();
        if (callback != null && !drawFailed) {
            try {
                callback.call(new DrawContext(recipe, graphics, mouseX, mouseY, null));
            } catch (Exception e) {
                // 每帧都会调用，出错后停用，避免刷屏
                drawFailed = true;
                JeiKubejs.LOGGER.error("[JEI KubeJS] 分类 {} 的 onDraw 回调出错，已停用（重载脚本后恢复）", def.getId(), e);
            }
        }
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, RecipeData recipe, IRecipeSlotsView slots, double mouseX, double mouseY) {
        DrawCallback callback = def.getTooltipCallback();
        if (callback == null || tooltipFailed) {
            return;
        }
        List<Component> lines = DrawContext.newTooltip();
        try {
            callback.call(new DrawContext(recipe, null, mouseX, mouseY, lines));
        } catch (Exception e) {
            tooltipFailed = true;
            JeiKubejs.LOGGER.error("[JEI KubeJS] 分类 {} 的 onTooltip 回调出错，已停用（重载脚本后恢复）", def.getId(), e);
            return;
        }
        tooltip.addAll(lines);
    }

    private static RecipeIngredientRole toJei(SlotDef.Role role) {
        return switch (role) {
            case INPUT -> RecipeIngredientRole.INPUT;
            case OUTPUT -> RecipeIngredientRole.OUTPUT;
            case RENDER_ONLY -> RecipeIngredientRole.RENDER_ONLY;
        };
    }

    private static IDrawableAnimated.StartDirection toJei(ElementDef.Direction direction) {
        return switch (direction) {
            case LEFT -> IDrawableAnimated.StartDirection.LEFT;
            case RIGHT -> IDrawableAnimated.StartDirection.RIGHT;
            case TOP -> IDrawableAnimated.StartDirection.TOP;
            case BOTTOM -> IDrawableAnimated.StartDirection.BOTTOM;
        };
    }
}
