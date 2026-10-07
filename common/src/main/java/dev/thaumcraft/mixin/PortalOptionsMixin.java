package dev.thaumcraft.mixin;

import dev.thaumcraft.client.legacy.PortalViews;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Options.class)
public class PortalOptionsMixin {
    // Sodium replaces vanilla terrain methods. Scope the setting read instead of redirecting those methods.
    @Inject(method="getEffectiveRenderDistance",at=@At("HEAD"),cancellable=true)
    private void thaumcraft$portalDistance(CallbackInfoReturnable<Integer> ci){
        int distance=PortalViews.renderDistance();
        if(distance>=0)ci.setReturnValue(distance);
    }
}
