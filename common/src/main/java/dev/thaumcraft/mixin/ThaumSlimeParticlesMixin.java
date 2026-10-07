package dev.thaumcraft.mixin;

import dev.thaumcraft.entity.ThaumSlime;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Slime has no overridable landing-particle hook; TC's original slime omitted this vanilla burst. */
@Mixin(Slime.class)
abstract class ThaumSlimeParticlesMixin {
    @Redirect(method="tick",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"))
    private void thaumcraft$landingParticles(Level level,ParticleOptions particle,double x,double y,double z,double dx,double dy,double dz){
        if(!((Object)this instanceof ThaumSlime))level.addParticle(particle,x,y,z,dx,dy,dz);
    }
}
