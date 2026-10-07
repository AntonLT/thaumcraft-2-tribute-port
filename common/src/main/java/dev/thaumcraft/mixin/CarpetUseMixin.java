package dev.thaumcraft.mixin;

import dev.thaumcraft.entity.CarpetEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Vanilla picking excludes the player's own vehicle; allow looking down to use it. */
@Mixin(Minecraft.class)
public abstract class CarpetUseMixin {
    @Inject(method="startUseItem",at=@At("HEAD"),cancellable=true)
    private void thaumcraft$useMountedCarpet(CallbackInfo ci){
        var mc=(Minecraft)(Object)this;
        if(mc.player==null||!(mc.player.getVehicle() instanceof CarpetEntity carpet))return;
        var eye=mc.player.getEyePosition();
        var hit=carpet.getBoundingBox().clip(eye,eye.add(mc.player.getLookAngle().scale(mc.player.entityInteractionRange())));
        if(hit.isPresent()){
            mc.gameMode.interact(mc.player,carpet,new net.minecraft.world.phys.EntityHitResult(carpet,hit.get()),InteractionHand.MAIN_HAND);
            ci.cancel();
        }
    }
}
