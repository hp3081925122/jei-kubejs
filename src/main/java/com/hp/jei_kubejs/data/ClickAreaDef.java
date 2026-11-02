package com.hp.jei_kubejs.data;

/** 界面上的配方点击区：点这里打开分类。screenClass 为界面类的完整类名，坐标相对界面左上角。 */
public record ClickAreaDef(String screenClass, int x, int y, int width, int height) {}
