package dev.thaumcraft.mixin;

import dev.thaumcraft.gameplay.ArcaneEnchantments;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class EnchantmentDamageMixin {
    @Inject(method="hurtServer",at=@At("HEAD"))
    private void thaumcraft$mark(ServerLevel level,DamageSource source,float damage,CallbackInfoReturnable<Boolean> callback) {
        if(damage>0)ArcaneEnchantments.markAttack(level,(LivingEntity)(Object)this,source);
    }
    @Inject(method="hurtServer",at=@At("RETURN"))
    private void thaumcraft$vampiric(ServerLevel level,DamageSource source,float damage,CallbackInfoReturnable<Boolean> callback) {
        if(callback.getReturnValue())ArcaneEnchantments.onHit(level,(LivingEntity)(Object)this,source);
    }
}
