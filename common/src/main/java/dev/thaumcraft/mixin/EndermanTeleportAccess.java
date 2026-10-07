package dev.thaumcraft.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets="net.minecraft.world.entity.monster.EnderMan$EndermanLookForPlayerGoal")
public interface EndermanTeleportAccess {
    @Accessor("teleportTime") void thaumcraft$setTeleportTime(int ticks);
}
