// JEI KubeJS 测试脚本（覆盖阶段 2~5 全部功能）
// 进入世界后：按 U 查看附魔台 / 高炉 / 书架，按 R 查看下界之星
// 日志里搜 "[JEI KubeJS]"

JEIKubeJSEvents.registerCategories(event => {
  // 阶段 2~3：槽位 + 绘制元素
  event.create('mypack:altar', cat => cat
    .title(Text.of('祭坛'))
    .icon('minecraft:enchanting_table')
    .size(140, 64)
    .inputSlot('in', 6, 10)
    .inputSlot('fuel', 6, 34)
    .outputSlot('out', 110, 18)
    .flame(27, 34, 200)
    .arrow(70, 18, 100)
    .plus(28, 12)
    // 1.21.1 原版熔炉进度箭头精灵图（24x16 整张），从左往右填充，3 秒一轮
    .progress(70, 34, 'minecraft:textures/gui/sprites/container/furnace/burn_progress.png', 0, 0, 24, 16, 24, 16, 60, 'left')
    .label(45, 2, Text.of('祭坛仪式'), 0x7F3FBF)
    .text('time', 45, 54)
    .tooltip(70, 18, 24, 17, Text.of('仪式进行中'), Text.gray('需要站在祭坛上'))
    .tooltipArea('info', 110, 18, 26, 26)
    .catalyst('minecraft:enchanting_table'))

  // 阶段 4：显示现有配方类型 + 界面点击区
  event.create('mypack:smelting_copy', cat => cat
    .title(Text.of('熔炼（脚本版）'))
    .icon('minecraft:blast_furnace')
    .size(82, 28)
    .inputSlot('in', 1, 6)
    .outputSlot('out', 61, 6)
    .arrow(25, 5, 200)
    .text('xp', 22, 0, 0x808080)
    .catalyst('minecraft:blast_furnace')
    // 放在熔炉界面左上角空白处，避开原版 JEI 的箭头/火焰点击区（重叠时 JEI 会优先用先注册的那个）
    .clickArea('net.minecraft.client.gui.screens.inventory.FurnaceScreen', 8, 4, 30, 10))

  // 阶段 5：脚本回调 + 滚动区
  event.create('mypack:library', cat => cat
    .title(Text.of('图书馆'))
    .icon('minecraft:bookshelf')
    .size(160, 80)
    .inputSlot('book', 4, 4)
    .scrollText('desc', 26, 2, 132, 36)
    .scrollPanel(4, 42, 154, 36, 120, ctx => {
      // 内容比面板高，会出现滚动条
      for (let i = 0; i < 10; i++) {
        ctx.fill(0, i * 12, 140, 11, i % 2 == 0 ? 0x30000000 : 0x18000000)
        ctx.text(2, i * 12 + 2, Text.of(`第 ${i + 1} 行`), 0x404040)
      }
    })
    .onDraw(ctx => {
      // 每条配方可以带自定义数据
      let mana = ctx.recipe.getData('mana')
      if (mana != null) {
        ctx.fill(4, 24, 18, 3, 0x2040A0)
        ctx.text(4, 28, Text.of(`${mana}`), 0x2040A0)
      }
    })
    .onTooltip(ctx => {
      if (ctx.isHovered(4, 24, 18, 12)) {
        ctx.tooltip(Text.aqua(`魔力：${ctx.recipe.getData('mana')}`))
      }
    })
    .catalyst('minecraft:bookshelf'))
})

JEIKubeJSEvents.registerRecipes(event => {
  event.add('mypack:altar', r => r
    .slot('in', 'minecraft:diamond')
    .slot('fuel', '#minecraft:coals')
    .slot('out', 'minecraft:nether_star')
    .text('time', '需要 30 秒')
    .tooltip('info', Text.gold('稀有产物'), '成功率 50%'))

  // 'fuel' 不填：只显示空槽位；不填 text / tooltip 时不显示
  event.add('mypack:altar', r => r
    .slot('in', 'minecraft:gold_ingot')
    .stack('out', '2x minecraft:totem_of_undying'))

  event.fromRecipeType('mypack:smelting_copy', 'minecraft:smelting', (recipe, r) => {
    if (recipe.output().isEmpty()) {
      r.skip()
      return
    }
    r.slot('in', recipe.input(0))
     .stack('out', recipe.output())
     .text('xp', `${Number(recipe.recipe().getExperience()).toFixed(1)} XP`)
  })

  event.add('mypack:library', r => r
    .slot('book', 'minecraft:enchanted_book')
    .data('mana', 500)
    .scrollText('desc',
      Text.of('一本附魔书。'),
      Text.of('这段说明故意写得很长，用来测试滚动文字区。'),
      Text.of('第三行'), Text.of('第四行'), Text.of('第五行'), Text.of('第六行')))

  // 错误用例：下面几行应该只在日志里报错，不影响其他配方
  event.add('mypack:not_exist', r => r.slot('in', 'minecraft:stone'))
  event.add('mypack:altar', r => r.slot('no_such_slot', 'minecraft:stone').slot('in', 'minecraft:stone'))
  event.fromRecipeType('mypack:smelting_copy', 'minecraft:not_a_type', (recipe, r) => {})
})
