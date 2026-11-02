package com.hp.jei_kubejs.jei;

import com.hp.jei_kubejs.JeiKubejs;
import com.hp.jei_kubejs.data.DrawCallback;
import com.hp.jei_kubejs.data.DrawContext;
import com.hp.jei_kubejs.data.RecipeData;
import mezz.jei.api.gui.drawable.IDrawable;
import net.minecraft.client.gui.GuiGraphics;

/** 滚动面板的内容：由脚本回调绘制。坐标相对内容左上角（JEI 已做平移和裁剪）。 */
class CallbackDrawable implements IDrawable {
    private final int width;
    private final int height;
    private final DrawCallback callback;
    private final RecipeData recipe;
    private boolean failed;

    CallbackDrawable(int width, int height, DrawCallback callback, RecipeData recipe) {
        this.width = width;
        this.height = height;
        this.callback = callback;
        this.recipe = recipe;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
        if (failed) {
            return;
        }
        graphics.pose().pushPose();
        graphics.pose().translate(xOffset, yOffset, 0);
        try {
            // 滚动面板内拿不到可靠的鼠标坐标，传 -1
            callback.call(new DrawContext(recipe, graphics, -1, -1, null));
        } catch (Exception e) {
            failed = true;
            JeiKubejs.LOGGER.error("[JEI KubeJS] 分类 {} 的 scrollPanel 回调出错，已停用", recipe.getCategory().getId(), e);
        } finally {
            graphics.pose().popPose();
        }
    }
}
