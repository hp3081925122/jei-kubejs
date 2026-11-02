# JEI KubeJS 第二轮：测重载（阶段 6）+ 修正后的布局和点击区
# 前提：游戏已在 test_jeikjs 存档里，jei_kubejs_test.js 已修改
LOG = r"E:\模组\发cf的模组\JEI KubeJS\1.21.1-neoforge-JEI KubeJS\runs\client\logs\latest.log"

def log_text():
    with open(LOG, encoding="utf-8", errors="replace") as f:
        return f.read()

before = log_text().count("[JEI KubeJS] 添加了")

# 阶段 6：重载客户端脚本（等同 F3+T 里 KubeJS 做的事），再 /reload 让 JEI 重建
# 注：实测 `/kubejs reload client_scripts` 命令在单人存档里不会重新加载客户端脚本，所以这里直接调用 ScriptManager
js("Java.loadClass('dev.latvian.mods.kubejs.KubeJS').getClientScriptManager().reload()")
command("reload")
wait_until(lambda: log_text().count("[JEI KubeJS] 添加了") > before, timeout=180, desc="JEI 重建并重新触发脚本事件")
print([l for l in log_text().splitlines() if "[JEI KubeJS]" in l][-8:])

js("""
var Internal = Java.loadClass('mezz.jei.common.Internal');
var RL = Java.loadClass('net.minecraft.resources.ResourceLocation');
var RecipeType = Java.loadClass('mezz.jei.api.recipe.RecipeType');
var RecipeData = Java.loadClass('com.hp.jei_kubejs.data.RecipeData');
var rt = Internal.getJeiRuntime();
function typeOf(id) { return new RecipeType(RL.parse(id), RecipeData); }
function count(id) { return rt.getRecipeManager().createRecipeLookup(typeOf(id)).get().count(); }
""", reset=True)
n = js("count('mypack:altar')")
expect(int(n) == 3, "重载后祭坛应仍有 3 条配方，实际 %s" % n)
# 新尺寸生效：分类高度应为 64
h = js("rt.getRecipeManager().getRecipeCategory(typeOf('mypack:altar')).getHeight()")
expect(int(h) == 64, "重载后祭坛高度应为 64，实际 %s" % h)

js("rt.getRecipesGui().showTypes(Java.loadClass('java.util.List').of(typeOf('mypack:altar')))")
wait_screen("RecipesGui")
hover(x=5, y=5)
frames(10)
screenshot("altar_after_reload")
# JEI 页面关闭后会回到之前打开的界面，连续关到没有界面为止
for _ in range(3):
    if not state().get("screen"):
        break
    close()
    frames(5)

# 阶段 4：点击区（熔炉界面左上角 8,4 起 30x10）
p = player()
x, y, z = int(p["x"]) + 2, int(p["y"]), int(p["z"])
command("setblock %d %d %d minecraft:furnace" % (x, y, z))
use_block(x, y, z)
wait_screen("FurnaceScreen")
left = int(get("screen.leftPos"))
top = int(get("screen.topPos"))
click(x=left + 20, y=top + 8)
wait_screen("RecipesGui", timeout=5)
frames(10)
screenshot("furnace_click_area2")
close()
