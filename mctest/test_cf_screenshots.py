# JEI KubeJS：CF 描述用截图（客户端脚本使用 examples/all_features.js）
# 运行：python "E:\模组\自用工具源码+产物\Minecraft Self MCP\python\mctest.py" run mctest\test_cf_screenshots.py

ensure_world("test_jeikjs")
wait_world()

# 准备 JEI 运行时和窗口函数
js("""
var Internal = Java.loadClass('mezz.jei.common.Internal');
var RL = Java.loadClass('net.minecraft.resources.ResourceLocation');
var RecipeType = Java.loadClass('mezz.jei.api.recipe.RecipeType');
var RecipeData = Java.loadClass('com.hp.jei_kubejs.data.RecipeData');
var GLFW = Java.loadClass('org.lwjgl.glfw.GLFW');
var rt = Internal.getJeiRuntime();
function typeOf(id) { return new RecipeType(RL.tryParse(id), RecipeData); }
function win() { return mc.getWindow(); }
function fld(o, n) { var c = o.getClass(); while (c != null) { try { var f = c.getDeclaredField(n); f.setAccessible(true); return f.get(o); } catch (e) { c = c.getSuperclass(); } } return null; }
function origin() { var r = fld(fld(fld(mc.screen, 'layouts'), 'recipeLayoutsWithButtons').get(0), 'recipeLayout').getRect(); return r.getX() + ',' + r.getY(); }
""", reset=True)

# 窗口调大，GUI 缩放 3
js("GLFW.glfwSetWindowPos(win().getWindow(), 0, 0); GLFW.glfwSetWindowSize(win().getWindow(), 1600, 1000);")
frames(20)
js("mc.options.guiScale().set(new java.lang.Integer(3)); mc.resizeDisplay();")
frames(10)
print("窗口", js("win().getWidth() + 'x' + win().getHeight()"), "缩放", js("win().getGuiScale()"))


def leave_gui():
    for _ in range(4):
        if not state().get("screen"):
            break
        close()
        frames(5)


def show(cid):
    js("rt.getRecipesGui().showTypes(Java.loadClass('java.util.List').of(typeOf('%s')))" % cid)
    wait_screen("RecipesGui")
    hover(x=1, y=1)
    frames(30)


# 悬停到第一条配方内的相对坐标（与示例脚本中的坐标一致）
def hover_at(dx, dy):
    ox, oy = [int(v) for v in str(js("origin()")).split(",")]
    hover(x=ox + dx, y=oy + dy)
    frames(10)


leave_gui()
for cid in ("mypack:altar", "mypack:smelting_copy", "mypack:library"):
    show(cid)
    screenshot("cf_%s" % cid.split(":")[1])
    leave_gui()

# 悬停提示：祭坛箭头提示、产物槽附加提示
show("mypack:altar")
hover_at(82, 26)
screenshot("cf_altar_tooltip_arrow")
hover_at(131, 41)
screenshot("cf_altar_tooltip_output")
leave_gui()

# 图书馆：魔力条提示、两个滚动区滚动后
show("mypack:library")
hover_at(12, 30)
screenshot("cf_library_tooltip_mana")
hover_at(90, 20)
scroll(-2)
hover_at(80, 60)
scroll(-3)
hover(x=1, y=1)
frames(10)
screenshot("cf_library_scrolled")
leave_gui()

# 熔炉界面：悬停并点击左上角点击区
command("clear")
p = player()
x, y, z = int(p["x"]) + 2, int(p["y"]), int(p["z"])
command("setblock %d %d %d minecraft:furnace" % (x, y, z))
use_block(x, y, z)
wait_screen("FurnaceScreen")
js("mc.gui.getChat().clearMessages(false)")
left = int(get("screen.leftPos"))
top = int(get("screen.topPos"))
hover(x=left + 20, y=top + 8)
frames(10)
screenshot("cf_furnace_click_area_hover")
click(x=left + 20, y=top + 8)
wait_screen("RecipesGui", timeout=5)
hover(x=1, y=1)
frames(20)
screenshot("cf_furnace_click_area_opened")
leave_gui()
command("setblock %d %d %d minecraft:air" % (x, y, z))
