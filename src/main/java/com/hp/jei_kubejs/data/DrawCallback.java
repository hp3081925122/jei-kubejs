package com.hp.jei_kubejs.data;

/** cat.onDraw(ctx => {...}) 与 cat.onTooltip(ctx => {...}) 的回调 */
@FunctionalInterface
public interface DrawCallback {
    void call(DrawContext ctx);
}
