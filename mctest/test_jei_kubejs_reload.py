# JEI KubeJS 第二轮（1.20.1 Forge）：重载、滚动区、熔炉点击区
# 运行：python "E:\模组\自用工具源码+产物\Minecraft Self MCP\python\mctest.py" run mctest\test_jei_kubejs_reload.py
import os

GAME_DIR = r"E:\模组\发cf的模组\JEI KubeJS\1.20.1-forge-JEI KubeJS\run"
LOG = os.path.join(GAME_DIR, "logs", "latest.log")
SCRIPT = os.path.join(GAME_DIR, "kubejs", "client_scripts", "jei_kubejs_test.js")


def log_text():
    with open(LOG, "rb") as f:
        raw = f.read()
    try:
        return raw.decode("utf-8")
    except UnicodeDecodeError:
        return raw.decode("gbk", errors="replace")


def leave_gui():
    for _ in range(4):
        if not state().get("screen"):
            break
        close()
        frames(5)


leave_gui()

js("""
var Internal = Java.loadClass('mezz.jei.common.Internal');
var RL = Java.loadClass('net.minecraft.resources.ResourceLocation');
var RecipeType = Java.loadClass('mezz.jei.api.recipe.RecipeType');
var RecipeData = Java.loadClass('com.hp.jei_kubejs.data.RecipeData');
var rt = Internal.getJeiRuntime();
function typeOf(id) { return new RecipeType(new RL(id), RecipeData); }
function count(id) { return rt.getRecipeManager().createRecipeLookup(typeOf(id)).get().count(); }
function height(id) { return rt.getRecipeManager().getRecipeCategory(typeOf(id)).getHeight(); }
""", reset=True)


def reload_all(expected_height, label):
    before = log_text().count("[JEI KubeJS] 添加了")
    # 等同 F3+T 里 KubeJS 重载客户端脚本；再 /reload 让 JEI 重建
    js("Java.loadClass('dev.latvian.mods.kubejs.KubeJS').getClientScriptManager().reload(null)")
    command("reload")
    wait_until(lambda: log_text().count("[JEI KubeJS] 添加了") > before, timeout=180, desc="JEI 重建并重新触发脚本事件(%s)" % label)
    # JEI 重建后 runtime 对象会换，重新取
    js("rt = Internal.getJeiRuntime();")
    n = js("count('mypack:altar')")
    h = js("height('mypack:altar')")
    print(label, "altar recipes =", n, "height =", h)
    expect(int(n) == 3, "%s：祭坛应有 3 条配方，实际 %s" % (label, n))
    expect(int(h) == expected_height, "%s：祭坛高度应为 %d，实际 %s" % (label, expected_height, h))


h0 = js("height('mypack:altar')")
print("重载前祭坛高度 =", h0)
expect(int(h0) == 64, "重载前祭坛高度应为 64，实际 %s" % h0)

with open(SCRIPT, encoding="utf-8") as f:
    original = f.read()
expect(".size(140, 64)" in original, "示例脚本里没找到 .size(140, 64)")
try:
    with open(SCRIPT, "w", encoding="utf-8", newline="") as f:
        f.write(original.replace(".size(140, 64)", ".size(140, 70)", 1))
    reload_all(70, "改成70后")
finally:
    with open(SCRIPT, "w", encoding="utf-8", newline="") as f:
        f.write(original)
reload_all(64, "还原后")

# ---- 滚动区：鼠标放在滚动框上再滚（GUI 缩放 2，窗口 854x480） ----
js("rt.getRecipesGui().showTypes(Java.loadClass('java.util.List').of(typeOf('mypack:library')))")
wait_screen("RecipesGui")
frames(10)
screenshot("scroll_0_before")
hover(x=150, y=125)
frames(3)
scroll(-3)
frames(5)
screenshot("scroll_1_textbox")
hover(x=150, y=170)
frames(3)
scroll(-3)
frames(5)
screenshot("scroll_2_panel")
leave_gui()

# ---- 熔炉界面点击区（左上角 8,4 起 30x10） ----
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
screenshot("furnace_click_area")
leave_gui()
print("其他 ERROR：", [l for l in log_errors() if "[JEI KubeJS]" not in l])
