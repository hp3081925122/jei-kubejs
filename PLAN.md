# JEI KubeJS：用 KubeJS 编写 JEI 界面（计划表）

目标：整合包作者只写 KubeJS 脚本，就能新建 JEI 配方分类（布局、槽位、绘制元素）并往里添加配方，不需要写 Java。

环境：Minecraft 1.21.1 / NeoForge 21.1.265 / JEI 19.x / KubeJS 2101 + Rhino。

## 脚本写法草案（目标体验）

```js
// client_scripts/jei_altar.js
JEIKubeJSEvents.registerCategories(event => {
  event.create('mypack:altar', cat => {
    cat.title(Text.translate('jei.mypack.altar'))
       .icon('minecraft:enchanting_table')
       .size(150, 60)
       .inputSlot('in', 10, 20)
       .outputSlot('out', 120, 20)
       .arrow(60, 20, 100)          // 动画箭头，100 tick 一轮
       .text('time', 50, 45)        // 由配方填入的文字
       .catalyst('minecraft:enchanting_table')
  })
})

JEIKubeJSEvents.registerRecipes(event => {
  event.add('mypack:altar', r => r
    .slot('in', 'minecraft:diamond')
    .slot('out', 'minecraft:nether_star')
    .text('time', '需要 30 秒'))
})
```

事件组名暂定 `JEIKubeJSEvents`，避免和 KubeJS 自带的 `RecipeViewerEvents`（隐藏物品、信息页等）冲突。

## 阶段与任务

| 阶段 | 内容 | 完成标准 |
|---|---|---|
| 0 环境 | 构建通过（绕过本地代理）；清理模板多余依赖；`runClient` 能进游戏并装载 JEI、KubeJS | `gradlew build` 成功，游戏内能打开 JEI |
| 1 骨架 | `KubeJSPlugin` 注册事件组；`@JeiPlugin` 的 `IModPlugin`；数据模型 `CategoryDef` / `SlotDef` / `RecipeData` | 空事件能在脚本里触发，日志里有输出 |
| 2 MVP 分类 | 分类 id、标题、图标、尺寸、背景；输入、输出、催化剂槽位（坐标）；脚本添加静态配方（物品、标签、流体） | 示例脚本在 JEI 里出现新分类，按 R/U 能查到配方 |
| 3 绘制元素 | 文字、贴图、JEI 原生箭头与火焰动画、进度条、提示（tooltip）区域、槽位背景 | 示例页面完整显示，鼠标悬停能看到提示 |
| 4 数据驱动 | 把现有 RecipeType（原版、模组、KubeJS 自定义配方）映射到分类，用回调把配方转换成槽位内容；配方点击区绑定 GUI | 某个模组的配方类型能自动生成一整页，不需要逐条手写 |
| 5 进阶 | 每帧绘制回调、动态提示回调；内容过多时在面板内滚动（用 JEI 原生滚动条材质）；GUI 缩放 3 和 4 不超出屏幕 | 缩放 3、4 下检查通过；长列表能滚动 |
| 6 重载与报错 | F3+T 与 `/reload` 后分类和配方正确刷新；脚本写错时报错能定位到脚本文件和行号，不导致游戏崩溃 | 故意写错的脚本只在日志里报错 |
| 7 文档与发布 | ProbeJS 类型提示；示例脚本合集；README（中英）；CurseForge 发布 | 发布到 CF，示例脚本能直接使用 |

## 需要提前确认的技术点

1. **注册时机**：JEI 的 `registerCategories` 在进入世界、配方同步之后执行。需要确认此时 client_scripts 已经加载完，并确认重载时 JEI 会重新调用插件。
2. **配方来源**：1.21.1 仍会把服务端配方同步到客户端，所以阶段 4 可以直接读取 `RecipeManager`。1.21.2 及以后不再同步配方，以后迁移版本时要另想办法。
3. **性能**：每帧绘制回调会调用 Rhino 脚本，开销比较大。默认使用声明式元素（阶段 3），回调只作为可选功能。
4. **查询入口**：自绘内容也要注册不可见的 ingredient，保证按 R/U 能查到配方。
5. **EMI 兼容**：暂不纳入计划。以后需要时写原生 EmiPlugin，与 JEI 共用同一份数据和布局。

## 进度

- [x] 阶段 0：`gradlew build` 通过（构建时需绕过本地代理 127.0.0.1:7892）
- [x] 阶段 1~4：分类、槽位、绘制元素、fromRecipeType、点击区——mctest 实测通过
- [x] 阶段 5：onDraw / onTooltip / r.data、scrollText / scrollPanel，GUI 缩放 2/3/4 截图检查通过
- [x] 阶段 6：注册前校验（尺寸、越界、无图标）；错误用例只报错不崩溃；重载客户端脚本 + `/reload` 后新定义生效——mctest 实测通过
- [x] 阶段 7：README（中英）、`examples/all_features.js`；CurseForge 发布留给作者手动做
- 产物：`build/libs/jei_kubejs-1.21.1-neoforge-1.0.0.jar`
- 测试：`mctest/test_jei_kubejs.py`（28 步 PASS）、`mctest/test_jei_kubejs_reload.py`（9 步 PASS）

### 已知限制
- 单人存档里 `/kubejs reload client_scripts` 命令实测不会重载客户端脚本；用 F3+T 后再 `/reload`。
- 脚本点击区和原版 JEI 的点击区重叠时，JEI 会用原版的那个，所以脚本点击区要放在不重叠的位置。
- `text` 文字不会自动裁剪，超出分类宽度时会画到外面。
