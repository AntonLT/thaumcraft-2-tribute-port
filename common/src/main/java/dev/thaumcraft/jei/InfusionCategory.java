package dev.thaumcraft.jei;

import dev.thaumcraft.Thaumcraft;
import dev.thaumcraft.machine.MachineLayout;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Recipe items occupy the same slots as the corresponding machine screen. */
final class InfusionCategory implements IRecipeCategory<InfusionRecipe> {
    private static final int RECIPE_HEIGHT = 155;
    private final IRecipeType<InfusionRecipe> type;
    private final boolean dark;
    private final MachineLayout layout;
    private final Identifier texture;
    private final IDrawable icon;
    private final IDrawable background;
    private final IDrawable visBar;
    private final IDrawable taintBar;
    private final Component title;

    InfusionCategory(IRecipeType<InfusionRecipe> type, boolean dark, IGuiHelper guiHelper) {
        this.type = type;
        this.dark = dark;
        this.layout = MachineLayout.get(dark ? "dark_infuser" : "thaumic_infuser");
        this.texture = Thaumcraft.id("textures/legacy/" + layout.texture() + ".png");
        ItemStack catalyst = JeiIngredients.catalyst(dark);
        this.icon = catalyst.isEmpty() ? guiHelper.createBlankDrawable(16, 16) : guiHelper.createDrawableItemStack(catalyst);
        this.background = guiHelper.createDrawable(texture, 0, 0, layout.width(), RECIPE_HEIGHT);
        this.visBar = guiHelper.createAnimatedDrawable(guiHelper.createDrawable(texture, 176, 0, dark ? 6 : 9, 46),
                100, IDrawableAnimated.StartDirection.BOTTOM, false);
        this.taintBar = dark ? guiHelper.createAnimatedDrawable(guiHelper.createDrawable(texture, 182, 0, 6, 46),
                100, IDrawableAnimated.StartDirection.BOTTOM, false) : null;
        this.title = dark ? Component.translatableWithFallback("jei.thaumcraft2tp.dark_infusion.title", "Dark Infusion") : Component.translatableWithFallback("jei.thaumcraft2tp.infusion.title", "Infusion");
    }

    @Override public IRecipeType<InfusionRecipe> getRecipeType() { return type; }
    @Override public Component getTitle() { return title; }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return layout.width(); }
    @Override public int getHeight() { return RECIPE_HEIGHT; }
    @Override public boolean needsRecipeBorder() { return false; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, InfusionRecipe recipe, IFocusGroup focuses) {
        var inputs = recipe.infusion().ingredients();
        for (var cell : layout.cells()) {
            if (cell.slot() < inputs.size()) {
                var stacks = JeiIngredients.stacks(inputs.get(cell.slot()));
                if (!stacks.isEmpty()) builder.addSlot(RecipeIngredientRole.INPUT, cell.x(), cell.y()).addItemStacks(stacks);
            } else if (cell.slot() == 9) {
                ItemStack output = JeiIngredients.result(recipe.infusion().result());
                if (!output.isEmpty()) builder.addSlot(RecipeIngredientRole.OUTPUT, cell.x(), cell.y()).add(output)
                        .addRichTooltipCallback((slot, tooltip) -> {
                            tooltip.add(cost(recipe));
                            tooltip.add(Component.translatableWithFallback("jei.thaumcraft2tp.infusion.slots", "Ingredients may use any outer slot."));
                            var id = recipe.infusion().requiredResearch();
                            if (id != null) {
                                var name=dev.thaumcraft.gameplay.GameData.findProject(id).map(dev.thaumcraft.gameplay.GameData.Project::nameComponent).orElse(Component.translatableWithFallback("message.thaumcraft2tp.research.unavailable_name", "Unavailable"));
                                tooltip.add(Component.translatableWithFallback("jei.thaumcraft2tp.research.required", "Research: %s", name));
                                tooltip.add(Component.translatableWithFallback("jei.thaumcraft2tp.research.steps", "See the Quaesitum Research tab for the research steps."));
                            }
                        });
            }
        }
    }

    @Override
    public void draw(InfusionRecipe recipe, IRecipeSlotsView slots, GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        background.draw(graphics, 0, 0);
        visBar.draw(graphics, dark ? 158 : 160, 105);
        if (taintBar != null) taintBar.draw(graphics, 164, 105);
        if (dark && Minecraft.getInstance().level != null) {
            int moon = Minecraft.getInstance().level.environmentAttributes()
                    .getValue(EnvironmentAttributes.MOON_PHASE, Vec3.ZERO).index();
            graphics.blit(RenderPipelines.GUI_TEXTURED, texture, 160, 8, 192, moon * 8, 8, 8, 256, 256);
        }
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, InfusionRecipe recipe, IRecipeSlotsView slots, double mouseX, double mouseY) {
        if (mouseX >= (dark ? 158 : 160) && mouseX < 170 && mouseY >= 105 && mouseY < 152)
            tooltip.add(cost(recipe));
    }

    private Component cost(InfusionRecipe recipe) {
        int total = recipe.infusion().cost();
        return dark ? Component.translatableWithFallback("jei.thaumcraft2tp.infusion.dark_cost", "Cost: %s vis + %s taint", Math.round(total * 2f / 3), Math.round(total / 3f))
                : Component.translatableWithFallback("jei.thaumcraft2tp.infusion.cost", "Cost: %s vis", total);
    }
}
