package dev.thaumcraft.jei;

import dev.thaumcraft.Thaumcraft;
import dev.thaumcraft.gameplay.ClientResearch;
import dev.thaumcraft.gameplay.GameData;
import dev.thaumcraft.gameplay.ResearchGate;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Infusion recipes plus research gating. Locked vanilla crafts and infusions are hidden;
 * each gated output has a Quaesitum research guide.
 */
@JeiPlugin
public final class ThaumcraftJeiPlugin implements IModPlugin {
    public static final IRecipeType<InfusionRecipe> INFUSION =
            IRecipeType.create(Thaumcraft.MOD_ID, "infusion", InfusionRecipe.class);
    public static final IRecipeType<InfusionRecipe> DARK_INFUSION =
            IRecipeType.create(Thaumcraft.MOD_ID, "dark_infusion", InfusionRecipe.class);
    public static final IRecipeType<ResearchHint> RESEARCH =
            IRecipeType.create(Thaumcraft.MOD_ID, "research", ResearchHint.class);

    private static volatile IJeiRuntime runtime;
    private static final List<InfusionRecipe> NORMAL = new ArrayList<>();
    private static final List<InfusionRecipe> DARK = new ArrayList<>();
    private static final List<ResearchHint> HINTS = new ArrayList<>();

    static {
        ClientResearch.onChange = ThaumcraftJeiPlugin::refresh;
        dev.thaumcraft.gameplay.AddonData.clientChanged=ThaumcraftJeiPlugin::refreshCatalog;
    }

    @Override
    public Identifier getPluginUid() {
        return Thaumcraft.id("jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        var guiHelper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
                new InfusionCategory(INFUSION, false, guiHelper),
                new InfusionCategory(DARK_INFUSION, true, guiHelper),
                new ResearchCategory(RESEARCH, guiHelper));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        NORMAL.clear();
        DARK.clear();
        for (GameData.Infusion infusion : GameData.infusions()) {
            if (JeiIngredients.result(infusion.result()).isEmpty()) continue;
            (infusion.dark() ? DARK : NORMAL).add(new InfusionRecipe(infusion));
        }
        registration.addRecipes(INFUSION, List.copyOf(NORMAL));
        registration.addRecipes(DARK_INFUSION, List.copyOf(DARK));
        HINTS.clear();HINTS.addAll(researchHints());
        registration.addRecipes(RESEARCH,List.copyOf(HINTS));
        refresh();
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        ItemStack infuser = JeiIngredients.catalyst(false);
        ItemStack dark = JeiIngredients.catalyst(true);
        ItemStack quaesitum = JeiIngredients.catalyst("quaesitum");
        if (!infuser.isEmpty()) registration.addCraftingStation(INFUSION, infuser.getItem());
        if (!dark.isEmpty()) registration.addCraftingStation(DARK_INFUSION, dark.getItem());
        if (!quaesitum.isEmpty()) registration.addCraftingStation(RESEARCH, quaesitum.getItem());
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
        refresh();
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
    }

    private static List<ResearchHint> researchHints() {
        Map<String, Integer> seen = new HashMap<>();
        List<ResearchHint> hints = new ArrayList<>();
        for (GameData.Infusion infusion : GameData.infusions()) {
            if (infusion.research() < 0) continue;
            ItemStack output = JeiIngredients.result(infusion.result());
            if (output.isEmpty() || seen.putIfAbsent(output.getItem().toString(), infusion.research()) != null) continue;
            hints.add(new ResearchHint(GameData.project(infusion.research()), output));
        }
        for (GameData.Craft craft : GameData.crafts()) {
            if (craft.research() < 0) continue;
            ItemStack output = JeiIngredients.result(craft.result());
            if (output.isEmpty() || seen.putIfAbsent(output.getItem().toString(), craft.research()) != null) continue;
            hints.add(new ResearchHint(GameData.project(craft.research()), output));
        }
        return List.copyOf(hints);
    }

    private static void refreshCatalog(){
        if(runtime==null)return;
        var manager=runtime.getRecipeManager();
        var old=new ArrayList<InfusionRecipe>(NORMAL);old.addAll(DARK);
        manager.hideRecipes(INFUSION,NORMAL);manager.hideRecipes(DARK_INFUSION,DARK);
        NORMAL.clear();DARK.clear();
        var newNormal=new ArrayList<InfusionRecipe>();var newDark=new ArrayList<InfusionRecipe>();
        for(var definition:GameData.infusions()){
            var recipe=old.stream().filter(r->r.infusion().equals(definition)).findFirst().orElse(null);
            if(recipe==null){recipe=new InfusionRecipe(definition);(definition.dark()?newDark:newNormal).add(recipe);}
            (definition.dark()?DARK:NORMAL).add(recipe);
        }
        manager.addRecipes(INFUSION,newNormal);manager.addRecipes(DARK_INFUSION,newDark);
        manager.hideRecipes(RESEARCH,HINTS);
        var hints=researchHints();var added=hints.stream().filter(h->!HINTS.contains(h)).toList();
        HINTS.clear();HINTS.addAll(hints);manager.addRecipes(RESEARCH,added);manager.unhideRecipes(RESEARCH,HINTS);
        refresh();
    }

    /** Hide every recipe the local player has not researched yet. Runs on recipe sync and runtime start. */
    public static void refresh() {
        var current = runtime;
        if (current == null) return;
        var manager = current.getRecipeManager();
        var known = ClientResearch.known();

        List<InfusionRecipe> lockedNormal = NORMAL.stream()
                .filter(r -> ResearchGate.infusionLocked(known, r.infusion())).toList();
        List<InfusionRecipe> lockedDark = DARK.stream()
                .filter(r -> ResearchGate.infusionLocked(known, r.infusion())).toList();
        manager.unhideRecipes(INFUSION, NORMAL);
        manager.unhideRecipes(DARK_INFUSION, DARK);
        if (!lockedNormal.isEmpty()) manager.hideRecipes(INFUSION, lockedNormal);
        if (!lockedDark.isEmpty()) manager.hideRecipes(DARK_INFUSION, lockedDark);

        List<RecipeHolder<net.minecraft.world.item.crafting.CraftingRecipe>> lockedCrafting = new ArrayList<>();
        List<RecipeHolder<net.minecraft.world.item.crafting.CraftingRecipe>> unlockedCrafting = new ArrayList<>();
        try (var lookup = manager.createRecipeLookup(RecipeTypes.CRAFTING).includeHidden().get()) {
            lookup.forEach(holder -> {
                (ResearchGate.locked(known, holder.id().identifier()) ? lockedCrafting : unlockedCrafting).add(holder);
            });
        }
        if (!unlockedCrafting.isEmpty()) manager.unhideRecipes(RecipeTypes.CRAFTING, unlockedCrafting);
        if (!lockedCrafting.isEmpty()) manager.hideRecipes(RecipeTypes.CRAFTING, lockedCrafting);
    }
}
