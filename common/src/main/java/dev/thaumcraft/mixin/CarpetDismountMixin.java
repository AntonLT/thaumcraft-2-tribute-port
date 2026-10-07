package dev.thaumcraft.mixin;

import dev.thaumcraft.entity.CarpetEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class CarpetDismountMixin {
    @Inject(method="wantsToStopRiding",at=@At("HEAD"),cancellable=true)
    private void thaumcraft$carpetDescent(CallbackInfoReturnable<Boolean> cir){
        if((Object)this instanceof ServerPlayer player&&player.getVehicle() instanceof CarpetEntity)
            cir.setReturnValue(false);
    }
}
