package com.hp.jei_kubejs.data;

import dev.latvian.mods.rhino.util.HideFromJS;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.forge.ForgeTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import java.util.List;
import java.util.Locale;

public class JeiSlotView {
    private final IRecipeSlotView view;
    private final List<ITypedIngredient<?>> ingredients;

    public JeiSlotView(IRecipeSlotView view) {
        this.view = view;
        this.ingredients = List.copyOf(view.getAllIngredientsList());
    }

    public String role() {
        return view.getRole().name().toLowerCase(Locale.ROOT);
    }

    public String name() {
        return view.getSlotName().orElse(null);
    }

    public String tag() {
        return view.getTagKey().map(key -> key.location().toString()).orElse(null);
    }

    public boolean isEmpty() {
        return ingredients.isEmpty();
    }

    public int size() {
        return ingredients.size();
    }

    public List<ItemStack> items() {
        return ingredients.stream().flatMap(i -> i.getIngredient(VanillaTypes.ITEM_STACK).stream()).toList();
    }

    public ItemStack item() {
        List<ItemStack> items = items();
        return items.isEmpty() ? ItemStack.EMPTY : items.get(0);
    }

    public List<FluidStack> fluids() {
        return ingredients.stream().flatMap(i -> i.getIngredient(ForgeTypes.FLUID_STACK).stream()).toList();
    }

    public FluidStack fluid() {
        List<FluidStack> fluids = fluids();
        return fluids.isEmpty() ? FluidStack.EMPTY : fluids.get(0);
    }

    public List<Object> ingredients() {
        return ingredients.stream().<Object>map(ITypedIngredient::getIngredient).toList();
    }

    @HideFromJS
    public List<ITypedIngredient<?>> getTyped() {
        return ingredients;
    }
}
