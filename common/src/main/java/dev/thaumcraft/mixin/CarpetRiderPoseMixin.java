package dev.thaumcraft.mixin;

import dev.thaumcraft.entity.CarpetEntity;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidMobRenderer.class)
public abstract class CarpetRiderPoseMixin {
    @Inject(method="extractHumanoidRenderState",at=@At("RETURN"))
    private static void thaumcraft$standOnCarpet(LivingEntity entity,HumanoidRenderState state,float partial,ItemModelResolver resolver,CallbackInfo ci){
        if(entity.getVehicle() instanceof CarpetEntity)state.isPassenger=false;
    }
}
