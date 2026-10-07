package dev.thaumcraft.mixin;

import dev.thaumcraft.client.legacy.PortalViews;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(GameRenderer.class)
public class PortalGameRendererMixin {
    // Extract the main scene afterwards: entity renderers can reuse their extraction states.
    @Inject(method="extract",at=@At("HEAD"))
    private void thaumcraft$portalViews(DeltaTracker delta,boolean advance,CallbackInfo ci){PortalViews.render(delta,advance);}
    @Inject(method="getMainCamera",at=@At("HEAD"),cancellable=true)
    private void thaumcraft$portalCamera(CallbackInfoReturnable<Camera> ci){if(PortalViews.active())ci.setReturnValue(PortalViews.camera());}
}
