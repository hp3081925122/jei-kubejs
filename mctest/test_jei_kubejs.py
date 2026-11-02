# JEI KubeJS 游戏内测试（mctest）- 1.20.1 Forge 版
# 与 1.21.1 版的区别只有：运行目录、RL 构造方式、日志编码。断言与 1.21.1 版相同。
# 运行：python "E:\模组\自用工具源码+产物\Minecraft Self MCP\python\mctest.py" run mctest\test_jei_kubejs.py
import os
import re

GAME_DIR = r"E:\模组\发cf的模组\JEI KubeJS\1.20.1-forge-JEI KubeJS\run"
LOG = os.path.join(GAME_DIR, "logs", "latest.log")

ensure_world("test_jeikjs")
wait_world()


def log_text():
    # 1.20.1 的日志按系统默认编码（GBK）写入，先试 UTF-8 再退回 GBK
    with open(LOG, "rb") as f:
        raw = f.read()
    try:
        return raw.decode("utf-8")
    except UnicodeDecodeError:
        return raw.decode("gbk", errors="replace")


wait_until(lambda: "[JEI KubeJS] 添加了" in log_text(), timeout=180, desc="JEI 插件注册完成")
log = log_text()
lines = [l for l in log.splitlines() if "[JEI KubeJS]" in l]
print("\n".join(lines))

# ---- 阶段 1/2：分类注册 ----
m = re.search(r"注册了 (\d+) 个分类", log)
expect(m and int(m.group(1)) == 3, "应注册 3 个分类，实际：%s" % (m.group(0) if m else "无"))
m = re.search(r"添加了 (\d+) 条配方", log)
expect(m and int(m.group(1)) > 5, "配方数应 > 5（含熔炼），实际：%s" % (m.group(0) if m else "无"))
m = re.search(r"从 minecraft:smelting 读取 (\d+) 条配方，加入分类 mypack:smelting_copy 共 (\d+) 条", log)
expect(m and int(m.group(2)) > 0, "fromRecipeType 应读到熔炼配方：%s" % (m.group(0) if m else "无"))

# ---- 阶段 6：错误用例只报错不崩溃 ----
expect("配方指向不存在的分类 mypack:not_exist" in log, "缺少不存在分类的报错")
expect("没有槽位 'no_such_slot'" in log, "缺少不存在槽位的报错")
expect("配方类型 minecraft:not_a_type 不存在" in log, "缺少不存在配方类型的报错")

# ---- 用 JEI 运行时 API 核对分类和配方数 ----
js("""
var Internal = Java.loadClass('mezz.jei.common.Internal');
var RL = Java.loadClass('net.minecraft.resources.ResourceLocation');
var RecipeType = Java.loadClass('mezz.jei.api.recipe.RecipeType');
var RecipeData = Java.loadClass('com.hp.jei_kubejs.data.RecipeData');
var rt = Internal.getJeiRuntime();
function typeOf(id) { return new RecipeType(new RL(id), RecipeData); }
function count(id) { return rt.getRecipeManager().createRecipeLookup(typeOf(id)).get().count(); }
""", reset=True)
for cid, expected in [("mypack:altar", 3), ("mypack:library", 1)]:
    n = js("count('%s')" % cid)
    print(cid, "recipes =", n)
    expect(int(n) == expected, "%s 应有 %d 条配方，实际 %s" % (cid, expected, n))
n = js("count('mypack:smelting_copy')")
print("mypack:smelting_copy recipes =", n)
expect(int(n) > 0, "熔炼分类应有配方")


def show(cid):
    js("rt.getRecipesGui().showTypes(Java.loadClass('java.util.List').of(typeOf('%s')))" % cid)
    wait_screen("RecipesGui")
    frames(10)


def leave_gui():
    # JEI 页面关闭后可能回到之前的界面，连续关到没有界面为止
    for _ in range(4):
        if not state().get("screen"):
            break
        close()
        frames(5)


leave_gui()
for scale in (2, 3, 4):
    js("mc.options.guiScale().set(new java.lang.Integer(%d)); mc.resizeDisplay();" % scale)
    frames(5)
    for cid in ("mypack:altar", "mypack:smelting_copy", "mypack:library"):
        show(cid)
        screenshot("%s_scale%d" % (cid.split(":")[1], scale))
        leave_gui()

# ---- 阶段 5：滚动区 ----
js("mc.options.guiScale().set(new java.lang.Integer(2)); mc.resizeDisplay();")
frames(5)
show("mypack:library")
screenshot("library_before_scroll")
scroll(-3)
frames(5)
screenshot("library_scrolled")
leave_gui()

# 本次运行中，除了故意的错误用例，不应有其他 ERROR（mod 自己的报错已在上面断言）
errs = [l for l in log_errors() if "[JEI KubeJS]" not in l]
print("其他 ERROR：", errs)
