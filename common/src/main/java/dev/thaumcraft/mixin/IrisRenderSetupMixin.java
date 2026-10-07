package dev.thaumcraft.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import dev.thaumcraft.client.legacy.IrisRendering;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.HashMap;
import java.util.Map;

@Mixin(RenderSetup.class)
public class IrisRenderSetupMixin {
    @Shadow @Final private RenderPipeline pipeline;
    @Inject(method="getTextures",at=@At("RETURN"),cancellable=true)
    private void thaumcraft$irisSamplers(CallbackInfoReturnable<Map<String,RenderSetup.TextureAndSampler>> ci){
        if(!IrisRendering.active())return;
        var format=pipeline.getVertexFormat();var textures=ci.getReturnValue();
        boolean overlay=format.contains(VertexFormatElement.UV1)&&!textures.containsKey("Sampler1");
        boolean lightmap=format.contains(VertexFormatElement.UV2)&&!textures.containsKey("Sampler2");
        if(!overlay&&!lightmap)return;
        // Iris's entity programs expose these samplers even for NO_OVERLAY/item
        // pipelines. Bind the engine's neutral overlay/lightmap instead of
        // letting Blaze3D reject the draw for a missing sampler.
        var result=new HashMap<>(textures);var mc=Minecraft.getInstance();
        var sampler=RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
        if(overlay)result.put("Sampler1",new RenderSetup.TextureAndSampler(mc.gameRenderer.overlayTexture().getTextureView(),sampler));
        if(lightmap)result.put("Sampler2",new RenderSetup.TextureAndSampler(mc.gameRenderer.lightmap(),sampler));
        ci.setReturnValue(result);
    }
}
