package com.hp.jei_kubejs.data;

import com.hp.jei_kubejs.JeiKubejs;

/**
 * 阶段 5c / 6：在注册前检查脚本定义，给出能直接定位问题的警告。
 * 只警告不拦截，避免一个小问题让整个分类消失。
 */
public final class CategoryValidator {
    /** GUI 缩放 4、1080p 屏幕下 JEI 配方区大致可用的尺寸 */
    public static final int SAFE_WIDTH = 176;
    public static final int SAFE_HEIGHT = 120;

    private CategoryValidator() {}

    public static void validate(CategoryDef def) {
        String id = def.getId().toString();
        if (def.getWidth() <= 0 || def.getHeight() <= 0) {
            JeiKubejs.LOGGER.error("[JEI KubeJS] 分类 {} 的尺寸 {}x{} 无效", id, def.getWidth(), def.getHeight());
        }
        if (def.getWidth() > SAFE_WIDTH || def.getHeight() > SAFE_HEIGHT) {
            JeiKubejs.LOGGER.warn("[JEI KubeJS] 分类 {} 尺寸 {}x{} 超过 {}x{}，GUI 缩放 3/4 下可能显示不全，建议把多出的内容放进 scrollText / scrollPanel",
                    id, def.getWidth(), def.getHeight(), SAFE_WIDTH, SAFE_HEIGHT);
        }
        for (SlotDef slot : def.getSlots().values()) {
            // 输出槽背景是 26x26（向外扩 5 像素），普通槽 18x18（向外扩 1 像素）
            int pad = slot.role() == SlotDef.Role.OUTPUT ? 5 : 1;
            int size = slot.role() == SlotDef.Role.OUTPUT ? 26 : 18;
            if (slot.x() - pad < 0 || slot.y() - pad < 0
                    || slot.x() - pad + size > def.getWidth() || slot.y() - pad + size > def.getHeight()) {
                JeiKubejs.LOGGER.warn("[JEI KubeJS] 分类 {} 的槽位 '{}' 在 ({}, {}) 超出分类范围 {}x{}",
                        id, slot.name(), slot.x(), slot.y(), def.getWidth(), def.getHeight());
            }
        }
        for (ElementDef element : def.getElements()) {
            if (element.x() < 0 || element.y() < 0 || element.x() >= def.getWidth() || element.y() >= def.getHeight()) {
                JeiKubejs.LOGGER.warn("[JEI KubeJS] 分类 {} 的元素 {} 起点在分类范围外", id, element.getClass().getSimpleName());
            }
        }
        if (def.getIcon().isEmpty() && def.getCatalysts().isEmpty()) {
            JeiKubejs.LOGGER.warn("[JEI KubeJS] 分类 {} 既没有 icon 也没有 catalyst，JEI 标签页上会没有图标", id);
        }
    }
}
