// JEI KubeJS example: put this file in kubejs/client_scripts/
// In game: press U on an enchanting table, blast furnace, bookshelf, cauldron or brewing stand to open the categories.
// 1.20.1 Forge version.

// recipe.json() in multiplayer: the server needs JEI KubeJS too, and the recipes must be declared here,
// outside any event. The list is sent when you join a server, so rejoin after changing it.
JEIKubeJS.syncRecipeTypes('minecraft:smelting')

JEIKubeJSEvents.registerCategories(event => {
  // Slots and built-in drawables
  event.create('mypack:altar', cat => cat
    .title(Text.of('Altar'))
    .icon('minecraft:enchanting_table')
    .size(140, 64)
    .inputSlot('in', 6, 10)
    .inputSlot('fuel', 6, 34)
    .outputSlot('out', 110, 18)
    .plus(28, 12)
    .flame(27, 34, 200)            // burning animation, 10 s per cycle
    .arrow(70, 18, 100)            // animated arrow, 5 s per cycle
    // Progress bar made from a JEI texture (same path on both versions), fills from the bottom every 3 s
    .progress(52, 20, 'jei:textures/jei/atlas/gui/brewing_stand_bubbles.png', 0, 0, 11, 28, 11, 28, 60, 'bottom')
    .label(45, 2, Text.of('Ritual'), 0x7F3FBF)
    .text('time', 45, 54)
    .tooltip(70, 18, 24, 17, Text.of('Ritual in progress'), Text.gray('Stand on the altar'))
    .tooltipArea('info', 45, 54, 60, 10)    // per-recipe tooltip over the 'time' text
    .catalyst('minecraft:enchanting_table'))

  // A category filled from an existing recipe type, plus a click area on the furnace screen
  event.create('mypack:smelting_copy', cat => cat
    .title(Text.of('Smelting (script)'))
    .icon('minecraft:blast_furnace')
    .size(82, 34)
    .inputSlot('in', 1, 12)
    .outputSlot('out', 61, 12)
    .arrow(25, 12, 200)
    .text('xp', 22, 1, 0x808080)
    .catalyst('minecraft:blast_furnace')
    // Top-left corner of the furnace screen. JEI's own furnace click areas cover the arrow and flame,
    // and when click areas overlap JEI uses the one registered first.
    .clickArea('net.minecraft.client.gui.screens.inventory.FurnaceScreen', 8, 4, 30, 10))

  // Script callbacks and scroll areas
  event.create('mypack:library', cat => cat
    .title(Text.of('Library'))
    .icon('minecraft:bookshelf')
    .size(160, 80)
    .inputSlot('book', 4, 4)
    .scrollText('desc', 26, 2, 132, 33)   // 11 px per line, so 33 shows exactly 3 lines
    .scrollPanel(4, 42, 154, 36, 120, ctx => {
      // Content is taller than the panel, so a scroll bar appears
      for (let i = 0; i < 10; i++) {
        ctx.fill(0, i * 12, 140, 11, i % 2 == 0 ? 0x30000000 : 0x18000000)
        ctx.text(2, i * 12 + 2, Text.of(`Line ${i + 1}`), 0x404040)
      }
    })
    .onDraw(ctx => {
      // Per-recipe custom data set with r.data(...)
      let mana = ctx.recipe.getData('mana')
      if (mana != null) {
        ctx.fill(4, 24, 18, 3, 0x2040A0)
        ctx.text(4, 28, Text.of(`${mana}`), 0x2040A0)
      }
    })
    .onTooltip(ctx => {
      if (ctx.isHovered(4, 24, 18, 12)) {
        ctx.tooltip(Text.aqua(`Mana: ${ctx.recipe.getData('mana')}`))
      }
    })
    .catalyst('minecraft:bookshelf'))

  // Fluid tanks
  event.create('mypack:cauldron', cat => cat
    .title(Text.of('Cauldron'))
    .icon('minecraft:cauldron')
    .size(110, 54)
    .inputTank('water', 4, 4, 16, 46, 4000)   // x, y, width, height, capacity in mB
    .inputSlot('in', 30, 18)
    .arrow(52, 18, 120)
    .outputSlot('out', 88, 18)
    .catalyst('minecraft:cauldron'))

  // A category filled from another JEI category, see fromCategory below
  event.create('mypack:brewing_copy', cat => cat
    .title(Text.of('Brewing (script)'))
    .icon('minecraft:brewing_stand')
    .size(110, 26)
    .inputSlot('potion', 1, 4)
    .plus(20, 6)
    .inputSlot('ingredient', 36, 4)
    .arrow(57, 4, 200)
    .outputSlot('out', 88, 4)
    .catalyst('minecraft:brewing_stand'))
})

JEIKubeJSEvents.registerRecipes(event => {
  event.add('mypack:altar', r => r
    .slot('in', 'minecraft:diamond')
    .slot('fuel', '#minecraft:coals')       // tags cycle through their items
    .slot('out', 'minecraft:nether_star')
    .slotTooltip('out', Text.gold('Rare result'), '50% chance')   // extra lines under the item's own tooltip
    .text('time', 'Takes 30 s')
    .tooltip('info', Text.gray('The altar must stay lit')))

  // Slots, texts and tooltips you leave out are simply not shown
  event.add('mypack:altar', r => r
    .slot('in', 'minecraft:gold_ingot')
    .stack('out', '2x minecraft:totem_of_undying'))

  // Every vanilla smelting recipe, one call per recipe
  event.fromRecipeType('mypack:smelting_copy', 'minecraft:smelting', (recipe, r) => {
    if (recipe.output().isEmpty()) {
      r.skip()
      return
    }
    r.slot('in', recipe.input(0))
     .stack('out', recipe.output())
     .text('xp', `${Number(recipe.recipe().getExperience()).toFixed(1)} XP`)
    // The recipe as JSON (objects and arrays). null when it cannot be read, see the Wiki.
    let json = recipe.json()
    if (json != null) {
      r.slotTooltip('out', Text.gray(`Cooking time: ${json.cookingtime} ticks`))
    }
  })

  event.add('mypack:library', r => r
    .slot('book', 'minecraft:enchanted_book')
    .data('mana', 500)
    .scrollText('desc',
      Text.of('An enchanted book.'),
      Text.of('This description is long on purpose so the text area has to scroll.'),
      Text.of('Line 3'), Text.of('Line 4'), Text.of('Line 5'), Text.of('Line 6')))

  // Fluids by id or '#tag', amount in mB. linkSlots makes the linked slots cycle together,
  // so each log is shown next to its own stripped log. id() gives the recipe a fixed id for JEI bookmarks.
  event.add('mypack:cauldron', r => r
    .id('mypack:cauldron/stripping')
    .fluid('water', 'minecraft:water', 1000)
    .stack('in', 'minecraft:oak_log', 'minecraft:spruce_log', 'minecraft:birch_log')
    .stack('out', 'minecraft:stripped_oak_log', 'minecraft:stripped_spruce_log', 'minecraft:stripped_birch_log')
    .linkSlots('in', 'out'))

  // Every recipe of an existing JEI category, read from the slots that category shows.
  // Works for anything JEI displays, even recipes that are not datapack recipes (brewing, anvil, other mods' pages).
  // JEI's brewing page has 4 inputs (3 bottles, then the ingredient) and 1 output.
  // copy() keeps the slot's full content (every cycled item or fluid, with amounts and data).
  event.fromCategory('mypack:brewing_copy', 'minecraft:brewing', (recipe, r) => {
    r.copy('potion', recipe.input(0))
     .copy('ingredient', recipe.input(3))
     .copy('out', recipe.output(0))
  })
})
