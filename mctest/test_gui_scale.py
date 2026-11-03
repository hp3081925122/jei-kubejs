# JEI KubeJS：GUI 缩放 3 / 4 补测（1.21.1 和 1.20.1 通用，两个分支同一份）
# 缩放 N 要求窗口帧缓冲至少 320N x 240N，缩放 4 即 1280x960。
# 默认窗口 854x480 会把缩放限制在 2，所以这里先把窗口调大，并断言实际缩放等于目标值。
# 运行：python "E:\模组\自用工具源码+产物\Minecraft Self MCP\python\mctest.py" run mctest\test_gui_scale.py

ensure_world("test_jeikjs")
wait_world()

js("""
var Internal = Java.loadClass('mezz.jei.common.Internal');
var RL = Java.loadClass('net.minecraft.resources.ResourceLocation');
var RecipeType = Java.loadClass('mezz.jei.api.recipe.RecipeType');
var RecipeData = Java.loadClass('com.hp.jei_kubejs.data.RecipeData');
var GLFW = Java.loadClass('org.lwjgl.glfw.GLFW');
var rt = Internal.getJeiRuntime();
function typeOf(id) { return new RecipeType(RL.tryParse(id), RecipeData); }
function win() { return mc.getWindow(); }
function fbSize() { return win().getWidth() + 'x' + win().getHeight(); }
""", reset=True)

print("原窗口：", js("fbSize()"))
js("GLFW.glfwSetWindowPos(win().getWindow(), 0, 0); GLFW.glfwSetWindowSize(win().getWindow(), 1600, 1000);")
frames(20)
size = js("fbSize()")
print("调整后窗口：", size)
w, h = [int(v) for v in str(size).split("x")]
expect(w >= 1280 and h >= 960, "窗口需至少 1280x960 才能测缩放 4，实际 %s" % size)


def leave_gui():
    for _ in range(4):
        if not state().get("screen"):
            break
        close()
        frames(5)


def show(cid):
    js("rt.getRecipesGui().showTypes(Java.loadClass('java.util.List').of(typeOf('%s')))" % cid)
    wait_screen("RecipesGui")
    frames(10)


leave_gui()
for scale in (3, 4):
    js("mc.options.guiScale().set(new java.lang.Integer(%d)); mc.resizeDisplay();" % scale)
    frames(10)
    actual = js("win().getGuiScale()")
    print("目标缩放 %d，实际 %s，GUI 尺寸 %s" % (scale, actual, js("win().getGuiScaledWidth() + 'x' + win().getGuiScaledHeight()")))
    expect(int(float(actual)) == scale, "缩放 %d 没生效，实际 %s" % (scale, actual))
    for cid in ("mypack:altar", "mypack:smelting_copy", "mypack:library"):
        show(cid)
        screenshot("real_%s_scale%d" % (cid.split(":")[1], scale))
        leave_gui()

# 还原：缩放 2、默认窗口
js("mc.options.guiScale().set(new java.lang.Integer(2)); mc.resizeDisplay(); GLFW.glfwSetWindowSize(win().getWindow(), 854, 480);")
frames(10)
