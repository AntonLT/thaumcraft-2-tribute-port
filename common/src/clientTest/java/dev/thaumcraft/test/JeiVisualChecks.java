package dev.thaumcraft.test;

import dev.thaumcraft.Thaumcraft;
import dev.thaumcraft.content.Content;
import dev.thaumcraft.jei.ThaumcraftJeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Opt-in screenshots of the three JEI views with the real JEI runtime. */
final class JeiVisualChecks {
    private static int ticks;
    private static int waitTicks;
    private static boolean lookupChecked;

    static boolean tick() {
        IJeiRuntime runtime;
        try {
            var field = ThaumcraftJeiPlugin.class.getDeclaredField("runtime");
            field.setAccessible(true);
            runtime = (IJeiRuntime) field.get(null);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("JEI runtime unavailable", e);
        }
        if (runtime == null) {
            if (++waitTicks > 200) throw new AssertionError("JEI did not start");
            return false;
        }
        var mc = Minecraft.getInstance();
        if (!lookupChecked) {
            var focusFactory = runtime.getJeiHelpers().getFocusFactory();
            var locked = focusFactory.createFocus(RecipeIngredientRole.OUTPUT, VanillaTypes.ITEM_STACK,
                    new ItemStack(Content.item("thaumic_restorer")));
            var station = focusFactory.createFocus(RecipeIngredientRole.CRAFTING_STATION, VanillaTypes.ITEM_STACK,
                    new ItemStack(Content.item("quaesitum")));
            var recipes = runtime.getRecipeManager();
            if (recipes.createRecipeCategoryLookup().limitFocus(List.of(locked)).get()
                    .noneMatch(category -> category.getRecipeType().equals(ThaumcraftJeiPlugin.RESEARCH)))
                throw new AssertionError("Locked item has no Quaesitum research guide");
            if (recipes.createRecipeCategoryLookup().limitFocus(List.of(station)).get()
                    .noneMatch(category -> category.getRecipeType().equals(ThaumcraftJeiPlugin.RESEARCH)))
                throw new AssertionError("Quaesitum does not lead to research guides");
            lookupChecked = true;
        }
        if (++ticks == 20) runtime.getRecipesGui().showTypes(List.of(ThaumcraftJeiPlugin.INFUSION));
        if (ticks == 50) runtime.getRecipesGui().showTypes(List.of(ThaumcraftJeiPlugin.DARK_INFUSION));
        if (ticks == 80) runtime.getRecipesGui().showTypes(List.of(ThaumcraftJeiPlugin.RESEARCH));
        if (ticks == 40 || ticks == 70 || ticks == 100) {
            if (mc.screen == null || !mc.screen.getClass().getName().startsWith("mezz.jei"))
                throw new AssertionError("JEI recipe screen did not open");
            String name = switch (ticks) {
                case 40 -> "thaumcraft-jei-infuser.png";
                case 70 -> "thaumcraft-jei-dark-infuser.png";
                default -> "thaumcraft-jei-quaesitum.png";
            };
            Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), 1,
                    result -> Thaumcraft.LOG.info("JEI screenshot: {}", result.getString()));
        }
        if (ticks == 110) Thaumcraft.LOG.info("THAUMCRAFT_JEI_VISUAL_PASS");
        return ticks >= 110;
    }
}
