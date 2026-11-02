# JEI KubeJS

用 KubeJS 脚本新建 JEI 配方分类：布局、槽位、绘制元素、配方、催化剂、界面点击区，不用写 Java。

Create custom JEI recipe categories from KubeJS scripts — layout, slots, drawables, recipes, catalysts and screen click areas — without writing Java.

- Minecraft 1.21.1 / NeoForge 21.1+
- 依赖 Requires: JEI 19.x, KubeJS 2101 (+ Rhino)
- 只在客户端运行，脚本放在 `kubejs/client_scripts/`。Client-side only; put scripts in `kubejs/client_scripts/`.

完整示例 Full example: [`examples/all_features.js`](examples/all_features.js)

## 快速上手 Quick start

```js
JEIKubeJSEvents.registerCategories(event => {
  event.create('mypack:altar', cat => cat
    .title(Text.of('祭坛'))
    .icon('minecraft:enchanting_table')
    .size(120, 40)
    .inputSlot('in', 10, 12)
    .outputSlot('out', 95, 12)
    .arrow(50, 12, 100)
    .catalyst('minecraft:enchanting_table'))
})

JEIKubeJSEvents.registerRecipes(event => {
  event.add('mypack:altar', r => r
    .slot('in', 'minecraft:diamond')
    .stack('out', '2x minecraft:nether_star'))
})
```

## 分类 API（`event.create(id, cat => ...)`）

坐标都相对分类左上角，单位为像素。所有方法都返回 `cat`，可以链式调用。

| 方法 | 说明 |
|---|---|
| `title(text)` / `icon(item)` / `size(w, h)` | 标题、标签页图标、尺寸（默认 150x60） |
| `catalyst(item)` | 催化剂，可多次调用 |
| `inputSlot(name, x, y)` / `outputSlot(...)` / `renderSlot(...)` | 输入槽（参与 U 查询）、输出槽（参与 R 查询，大格子）、只显示槽 |
| `arrow(x, y[, ticks])` / `flame(x, y[, ticks])` / `plus(x, y)` | JEI 原生箭头、火焰、加号；带 ticks 时为动画（20 刻 = 1 秒） |
| `texture(x, y, tex, u, v, w, h[, texW, texH])` | 贴图区域，默认按 256x256 贴图计算 |
| `progress(x, y, tex, u, v, w, h[, texW, texH], ticks[, from])` | 贴图进度条动画，from 为 `left`/`right`/`top`/`bottom` |
| `label(x, y, text[, color[, shadow]])` | 固定文字，颜色写 `0xRRGGBB` 即可 |
| `text(name, x, y[, color[, shadow]])` | 由配方填入的文字 |
| `tooltip(x, y, w, h, ...lines)` | 固定悬停提示 |
| `tooltipArea(name, x, y, w, h)` | 由配方填入的悬停提示 |
| `scrollText(name, x, y, w, h)` | 由配方填入的可滚动文字 |
| `scrollPanel(x, y, w, h, contentHeight, ctx => ...)` | 由回调绘制的可滚动面板 |
| `onDraw(ctx => ...)` | 每帧绘制回调，在其他元素之后调用 |
| `onTooltip(ctx => ...)` | 悬停提示回调，用 `ctx.tooltip(text)` 添加提示行 |
| `clickArea(screenClass, x, y, w, h)` | 在某个容器界面上加点击区，点击后打开本分类 |

## 配方 API（`event.add(category, r => ...)`）

| 方法 | 说明 |
|---|---|
| `slot(name, ingredient)` | 物品或标签（`'#minecraft:coals'`），会轮换显示 |
| `stack(name, ...stacks)` | 带数量的物品（`'4x minecraft:diamond'`），适合输出槽 |
| `text(name, text)` / `tooltip(name, ...lines)` / `scrollText(name, ...lines)` | 填入对应元素 |
| `data(key, value)` | 自定义数据，回调里用 `ctx.recipe.getData(key)` 读取 |
| `skip()` | 不显示这条配方（在 `fromRecipeType` 回调里用） |

### 从现有配方类型生成 Map existing recipe types

```js
event.fromRecipeType('mypack:smelting_copy', 'minecraft:smelting', (recipe, r) => {
  if (recipe.output().isEmpty()) { r.skip(); return }
  r.slot('in', recipe.input(0)).stack('out', recipe.output())
})
```

`recipe` 提供 `id()`、`inputs()`、`input(i)`、`output()`，以及 `recipe()` 返回的原始配方对象（可以调用模组自己的方法）。

## 回调上下文 `ctx`

`ctx.recipe`、`ctx.mouseX` / `ctx.mouseY`、`ctx.tick`、`ctx.isHovered(x, y, w, h)`、`ctx.text(x, y, text[, color[, shadow]])`、`ctx.fill(x, y, w, h, color)`、`ctx.item(stack, x, y)`、`ctx.texture(tex, x, y, u, v, w, h[, texW, texH])`、`ctx.tooltip(text)`、`ctx.graphics`（原始 GuiGraphics）。

`onDraw` 每帧都会调用，回调里不要做耗时操作。回调抛出异常后会停用并记一次日志，JEI 重载后恢复。

## 重载 Reloading

1. 修改脚本后按 **F3+T** 重新加载客户端脚本。
2. 再执行 **`/reload`**（或重新进入世界）让 JEI 重建，新脚本才会生效。

## 报错 Errors

日志里搜 `[JEI KubeJS]`。脚本写错只会跳过出错的分类、配方或元素，不会让游戏崩溃。注册时还会检查：分类尺寸是否太大（超过 176x120 时建议用滚动区）、槽位和元素是否超出分类范围、分类有没有图标。

## 注意 Notes

- 滚动区里的物品只能显示，不能点击查询；需要查询的物品请放在槽位里。
- 1.21.1 会把服务端配方同步到客户端，所以 `fromRecipeType` 能读到服务端的配方。1.21.2 及以后的版本不再同步。
- ProbeJS 会直接读取本模组的 Java 类来生成类型提示。

License: All Rights Reserved（与 `gradle.properties` 中 `mod_license` 一致）
