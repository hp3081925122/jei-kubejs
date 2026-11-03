package com.hp.jei_kubejs.data;

import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;
import java.util.Objects;

public class JeiRecipeView {
    private final ResourceLocation type;
    private final ResourceLocation id;
    private final Object recipe;
    private final List<JeiSlotView> slots;
    private RecipeView recipeView;

    public JeiRecipeView(ResourceLocation type, ResourceLocation id, Object recipe, IRecipeSlotsView slotsView) {
        this.type = type;
        this.id = id;
        this.recipe = recipe;
        this.slots = slotsView.getSlotViews().stream().map(JeiSlotView::new).toList();
    }

    public ResourceLocation type() {
        return type;
    }

    public ResourceLocation id() {
        return id;
    }

    public Object recipe() {
        return recipe;
    }

    public Object json() {
        if (recipeView == null && recipe instanceof RecipeHolder<?> holder && Minecraft.getInstance().level != null) {
            recipeView = new RecipeView(holder, Minecraft.getInstance().level.registryAccess());
        }
        return recipeView == null ? null : recipeView.json();
    }

    public List<JeiSlotView> slots() {
        return slots;
    }

    public List<JeiSlotView> inputs() {
        return byRole(RecipeIngredientRole.INPUT);
    }

    public List<JeiSlotView> outputs() {
        return byRole(RecipeIngredientRole.OUTPUT);
    }

    public List<JeiSlotView> catalysts() {
        return byRole(RecipeIngredientRole.CATALYST);
    }

    public List<JeiSlotView> renderOnly() {
        return byRole(RecipeIngredientRole.RENDER_ONLY);
    }

    public JeiSlotView input(int index) {
        return at(inputs(), index);
    }

    public JeiSlotView output(int index) {
        return at(outputs(), index);
    }

    public JeiSlotView catalyst(int index) {
        return at(catalysts(), index);
    }

    public JeiSlotView slot(String name) {
        return slots.stream().filter(s -> Objects.equals(s.name(), name)).findFirst().orElse(null);
    }

    private List<JeiSlotView> byRole(RecipeIngredientRole role) {
        String name = role.name().toLowerCase(java.util.Locale.ROOT);
        return slots.stream().filter(s -> s.role().equals(name)).toList();
    }

    private static JeiSlotView at(List<JeiSlotView> list, int index) {
        return index >= 0 && index < list.size() ? list.get(index) : null;
    }
}
