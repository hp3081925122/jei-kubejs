# 补测：图书馆上方滚动文字区（鼠标放在文字上再滚）
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
""", reset=True)
js("rt.getRecipesGui().showTypes(Java.loadClass('java.util.List').of(typeOf('mypack:library')))")
wait_screen("RecipesGui")
frames(10)
hover(x=230, y=125)
frames(3)
scroll(-3)
frames(5)
screenshot("textbox_scrolled")
leave_gui()
