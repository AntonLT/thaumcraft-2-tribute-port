package dev.thaumcraft.mixin;

import dev.thaumcraft.client.legacy.PortalViews;
import net.caffeinemc.mods.sodium.client.util.FogParameters;
import net.caffeinemc.mods.sodium.client.util.FogStorage;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class SodiumPortalGameRendererMixin {
    @Dynamic("Added by Sodium")
    @Inject(method="sodium$getProjectionMatrix",at=@At("HEAD"),cancellable=true,require=0,remap=false)
    private void thaumcraft$portalProjection(CallbackInfoReturnable<Matrix4fc> ci){
        if(PortalViews.active())ci.setReturnValue(PortalViews.projection());
    }
    @Dynamic("Added by Sodium")
    @Inject(method="sodium$getFogParameters",at=@At("HEAD"),cancellable=true,require=0,remap=false)
    private void thaumcraft$portalFog(CallbackInfoReturnable<FogParameters> ci){
        if(PortalViews.active())ci.setReturnValue(((FogStorage)PortalViews.fog()).sodium$getFogParameters());
    }
}
