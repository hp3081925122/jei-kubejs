package com.hp.jei_kubejs.data;

/** 分类布局里的一个命名槽位；坐标相对分类左上角。 */
public record SlotDef(String name, Role role, int x, int y, int width, int height, long capacity) {
    public boolean isTank() {
        return capacity > 0;
    }

    public enum Role {
        INPUT,
        OUTPUT,
        /** 只显示，不参与 R/U 查询 */
        RENDER_ONLY
    }
}
