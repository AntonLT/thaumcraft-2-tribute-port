package dev.thaumcraft.jei;

import dev.thaumcraft.Thaumcraft;
import dev.thaumcraft.gameplay.GameData;
import dev.thaumcraft.gameplay.ResearchLogic;
import dev.thaumcraft.machine.MachineLayout;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

record ResearchHint(GameData.Project project, ItemStack lockedOutput) {}

/** A locked item's JEI recipe points to the machine that produces its discovery. */
final class ResearchCategory implements IRecipeCategory<ResearchHint> {
    private final IRecipeType<ResearchHint> type;
    private final MachineLayout layout;
    private final IDrawable icon;
    private final IDrawable background;
    private final ItemStack quaesitum;

    ResearchCategory(IRecipeType<ResearchHint> type, IGuiHelper guiHelper) {
        this.type = type;
        this.layout = MachineLayout.get("quaesitum");
        this.quaesitum = JeiIngredients.catalyst("quaesitum");
        this.icon = quaesitum.isEmpty() ? guiHelper.createBlankDrawable(16, 16) : guiHelper.createDrawableItemStack(quaesitum);
        this.background = guiHelper.createDrawable(Thaumcraft.id("textures/legacy/" + layout.texture() + ".png"),
                0, 0, layout.width(), layout.playerY());
    }

    @Override public IRecipeType<ResearchHint> getRecipeType() { return type; }
    @Override public Component getTitle() { return Component.translatableWithFallback("jei.thaumcraft2tp.research.title", "Quaesitum Research"); }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return layout.width(); }
    private Component theoryText(){return Component.translatableWithFallback("jei.thaumcraft2tp.research.theory", "Study items with paper for a theory.");}
    private Component discoveryText(){return Component.translatableWithFallback("jei.thaumcraft2tp.research.discovery", "Study the theory for its discovery.");}
    private int discoveryY(){var font=Minecraft.getInstance().font;return Math.max(133,114+font.split(theoryText(),140).size()*font.lineHeight+1);}
    @Override public int getHeight() { var font=Minecraft.getInstance().font;return Math.max(153,discoveryY()+font.split(discoveryText(),140).size()*font.lineHeight+2); }
    @Override public boolean needsRecipeBorder() { return false; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ResearchHint recipe, IFocusGroup focuses) {
        var project = recipe.project();
        for (var cell : layout.cells()) {
            switch (cell.slot()) {
                case 0 -> builder.addSlot(RecipeIngredientRole.INPUT, cell.x(), cell.y()).add(ResearchLogic.theory(project.index()));
                case 3 -> builder.addSlot(RecipeIngredientRole.INPUT, cell.x(), cell.y()).add(Items.PAPER);
                case 9 -> builder.addSlot(RecipeIngredientRole.OUTPUT, cell.x(), cell.y())
                        .add(JeiIngredients.result(new GameData.StackDef(project.discovery(), 1)));
                default -> {}
            }
        }
        builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).add(recipe.lockedOutput());
        if (!quaesitum.isEmpty()) builder.addSlot(RecipeIngredientRole.CRAFTING_STATION, 7, 114).add(quaesitum);
    }

    @Override
    public void draw(ResearchHint recipe, IRecipeSlotsView slots, GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        background.draw(graphics, 0, 0);
        graphics.fill(0, 97, 176, getHeight(), 0xff555555);
        graphics.fill(1, 97, 175, getHeight()-1, 0xffd0d0d0);
        var font = Minecraft.getInstance().font;
        graphics.text(font, Component.translatable("block.thaumcraft2tp.quaesitum"), 8, 5, 0xff404040, false);
        graphics.text(font, font.plainSubstrByWidth(recipe.project().nameComponent().getString(), 140), 28, 100, 0xff404040, false);
        graphics.textWithWordWrap(font, theoryText(), 28, 114, 140, 0xff404040, false);
        graphics.textWithWordWrap(font, discoveryText(), 28, discoveryY(), 140, 0xff404040, false);
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, ResearchHint recipe, IRecipeSlotsView slots, double mouseX, double mouseY) {
        if (mouseY >= 99 && mouseY < 111) tooltip.add(recipe.project().nameComponent());
    }
}
