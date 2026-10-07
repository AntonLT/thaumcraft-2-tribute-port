package dev.thaumcraft.api;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import java.util.List;
import java.util.Optional;

/** Immutable view of one accepted infusion recipe. */
public interface InfusionRecipe {
    Identifier id();
    boolean dark();
    int cost();
    /** Item IDs, or item tags prefixed with {@code #}. Each ingredient occupies its own input slot. */
    List<String> ingredients();
    /** A new copy on every call. */
    ItemStack result();
    Optional<Identifier> research();
}
