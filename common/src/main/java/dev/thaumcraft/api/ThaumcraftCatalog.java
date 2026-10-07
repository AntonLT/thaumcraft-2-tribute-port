package dev.thaumcraft.api;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import java.util.List;
import java.util.Optional;

/**
 * One side's accepted Thaumcraft definitions. Every method reads a single immutable snapshot, so calls are safe
 * from any thread. The server view is authoritative; the client view mirrors what the server last sent.
 */
public interface ThaumcraftCatalog {
    Optional<Research> research(Identifier id);
    List<Research> allResearch();
    /** Book order: the four original categories, then addon categories by order and ID. */
    List<ResearchCategory> categories();
    Optional<ResearchCategory> category(Identifier id);
    /** Matching order: descending priority, then ascending recipe ID. */
    List<InfusionRecipe> infusions();
    Optional<InfusionRecipe> infusion(Identifier id);
    /**
     * Research that locks a crafting or infusion recipe, if any. A craft requirement whose research is unavailable
     * reports {@code thaumcraft2tp:unavailable} and denies everyone.
     */
    Optional<Identifier> requiredResearch(Identifier recipe);
    /** Effective crucible value. */
    float vis(ItemStack stack);
    /** Base vis per durability point, before the Restorer's upgrade and enchantment multipliers. */
    float restorerCost(ItemStack stack);
    /** Effective Quaesitum research value. */
    int researchValue(ItemStack stack);
    /** The booster rule matching a block, or empty when the block does not boost. */
    Optional<Booster> booster(net.minecraft.world.level.block.state.BlockState state);
    /** A fresh theory item, or empty when the project is not in this catalog. */
    Optional<ItemStack> theory(Identifier research);
    /** A fresh discovery item, or empty when the project is not in this catalog. */
    Optional<ItemStack> discovery(Identifier research);
}
