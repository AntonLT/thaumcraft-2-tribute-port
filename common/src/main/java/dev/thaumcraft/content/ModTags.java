package dev.thaumcraft.content;

import dev.thaumcraft.Thaumcraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/** Pack-extensible exclusions and biome selection. */
public final class ModTags {
    private ModTags() {}
    /** Blocks the Arcane Bore skips, as it skips unbreakable blocks. */
    public static final TagKey<Block> BORE_IMMUNE=TagKey.create(Registries.BLOCK,Thaumcraft.id("bore_immune"));
    /** Blocks a Portable Hole cannot open through, as with unbreakable blocks. */
    public static final TagKey<Block> PORTABLE_HOLE_IMMUNE=TagKey.create(Registries.BLOCK,Thaumcraft.id("portable_hole_immune"));
    /** Mobs the Crucible of Souls ignores. */
    public static final TagKey<EntityType<?>> SOUL_IMMUNE=TagKey.create(Registries.ENTITY_TYPE,Thaumcraft.id("soul_immune"));
    /** Items a Traveling Trunk refuses, for example to stop storage nesting. */
    public static final TagKey<Item> TRUNK_FORBIDDEN=TagKey.create(Registries.ITEM,Thaumcraft.id("trunk_forbidden"));
    /** Items the Thaumic Duplicator refuses, independently of crucible value. */
    public static final TagKey<Item> DUPLICATOR_FORBIDDEN=TagKey.create(Registries.ITEM,Thaumcraft.id("duplicator_forbidden"));
    public static final TagKey<net.minecraft.world.level.biome.Biome> SPAWNS_WISPS=biome("spawns_wisps");
    public static final TagKey<net.minecraft.world.level.biome.Biome> SPAWNS_ARCANE_MOBS=biome("spawns_arcane_mobs");
    public static final TagKey<net.minecraft.world.level.biome.Biome> NETHER_ARCANE_VEGETATION=biome("has_nether_arcane_vegetation");
    public static TagKey<net.minecraft.world.level.biome.Biome> featureBiomes(String feature){return biome("has_"+feature);}
    private static TagKey<net.minecraft.world.level.biome.Biome> biome(String id){return TagKey.create(Registries.BIOME,Thaumcraft.id(id));}
}
