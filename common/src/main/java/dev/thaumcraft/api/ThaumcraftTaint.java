package dev.thaumcraft.api;

import dev.thaumcraft.content.TaintBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

/**
 * Taint conversion. Which blocks and mobs convert, and into what, comes from the {@code taint_blocks} and
 * {@code taint_entities} data rules and the {@code thaumcraft2tp:taint_immune} tags. Methods must run on the server
 * thread.
 */
public final class ThaumcraftTaint {
    private ThaumcraftTaint() {}

    /**
     * Converts the block at {@code pos} to its tainted form. Returns false when the block has no tainted form, has a
     * block entity, is immune, or a {@link ThaumcraftEvents#TAINT_SPREADING} listener denies it.
     */
    public static boolean taint(ServerLevel level,BlockPos pos){serverThread(level);return TaintBlock.taint(level,pos);}
    /**
     * Restores a tainted block, taint plant, or spore pod at {@code pos}, as silverwood and purifying tools do. A block
     * produced by an addon rule is restored only where taint converted it. Returns false when nothing was tainted.
     */
    public static boolean purify(ServerLevel level,BlockPos pos){serverThread(level);return TaintBlock.purify(level,pos);}
    /**
     * Converts a living mob into its tainted form, ignoring the aura chance that taint blocks apply. Returns false
     * when the mob has no tainted form, is immune, or a {@link ThaumcraftEvents#TAINT_SPREADING} listener denies it.
     */
    public static boolean taintEntity(ServerLevel level,Entity entity){serverThread(level);return TaintBlock.convert(level,entity);}

    private static void serverThread(ServerLevel level){if(!level.getServer().isSameThread())throw new IllegalStateException("Thaumcraft taint API requires the server thread");}
}
