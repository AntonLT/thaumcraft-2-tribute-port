package dev.thaumcraft.mixin;

import net.minecraft.world.entity.ExperienceOrb;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Merged modern orbs retain several identical XP awards in one entity. */
@Mixin(ExperienceOrb.class)
public interface ExperienceOrbAccess {
    @Accessor("count") int thaumcraft$count();
    @Accessor("count") void thaumcraft$count(int count);
}
