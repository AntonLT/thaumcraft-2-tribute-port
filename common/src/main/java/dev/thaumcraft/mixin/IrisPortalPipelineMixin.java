package dev.thaumcraft.mixin;

import dev.thaumcraft.client.legacy.PortalViews;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.shaderpack.materialmap.NamespacedId;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets="net.irisshaders.iris.pipeline.PipelineManager",remap=false)
public class IrisPortalPipelineMixin {
    @Shadow private WorldRenderingPipeline pipeline;
    @Inject(method="preparePipeline",at=@At("HEAD"),cancellable=true)
    private void thaumcraft$portalPipeline(NamespacedId dimension,CallbackInfoReturnable<WorldRenderingPipeline> ci){
        if(PortalViews.active())ci.setReturnValue(pipeline);
    }
    @Inject(method="destroyPipeline",at=@At("HEAD"))
    private void thaumcraft$closePortalPipelines(CallbackInfo ci){PortalViews.reset();}
}
