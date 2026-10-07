package dev.thaumcraft.mixin;

import dev.thaumcraft.client.legacy.PortalViews;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets="net.irisshaders.iris.pathways.HandRenderer",remap=false)
public class IrisPortalHandMixin {
    @Inject(method={"renderSolid","renderTranslucent"},at=@At("HEAD"),cancellable=true)
    private void thaumcraft$noPortalHand(CallbackInfo ci){if(PortalViews.active())ci.cancel();}
}
