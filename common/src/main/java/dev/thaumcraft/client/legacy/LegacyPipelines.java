package dev.thaumcraft.client.legacy;

import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.platform.*;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.*;
import dev.thaumcraft.Thaumcraft;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import java.util.HashMap;
import java.util.Map;

/** Explicit blend/depth state replaces fixed-function GL state, including additive glows. */
public final class LegacyPipelines {
    private record Material(Identifier texture,int source,int destination,boolean depthWrite,boolean cull) {}
    private static final Map<Material,RenderType> MATERIALS=new HashMap<>();
    private static final Map<String,RenderPipeline> PIPELINES=new HashMap<>();
    private LegacyPipelines() {}
    public static RenderPipeline pipeline(boolean particle,int source,int destination,boolean depthWrite){
        return pipeline(particle,source,destination,depthWrite,false,false);
    }
    private static RenderPipeline pipeline(boolean particle,int source,int destination,boolean depthWrite,boolean cull,boolean cutout){
        String key=(particle?"particle":"entity")+"_"+source+"_"+destination+"_"+depthWrite+"_"+cull+"_"+cutout;
        return PIPELINES.computeIfAbsent(key,k->{
            var builder=RenderPipeline.builder().withLocation(Thaumcraft.id("pipeline/legacy_"+k))
                .withUniform("DynamicTransforms",UniformType.UNIFORM_BUFFER).withUniform("Projection",UniformType.UNIFORM_BUFFER).withUniform("Fog",UniformType.UNIFORM_BUFFER)
                .withVertexShader(particle?"core/particle":"core/entity").withFragmentShader(particle?"core/particle":"core/entity")
                .withSampler("Sampler0").withSampler("Sampler2").withVertexFormat(particle?DefaultVertexFormat.PARTICLE:DefaultVertexFormat.ENTITY,VertexFormat.Mode.QUADS)
                .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL,depthWrite)).withCull(cull);
            if(!particle)builder.withUniform("Lighting",UniformType.UNIFORM_BUFFER).withShaderDefine("NO_OVERLAY").withShaderDefine("NO_CARDINAL_LIGHTING");
            if(destination!=0)builder.withColorTargetState(new ColorTargetState(new BlendFunction(source==1?SourceFactor.ONE:SourceFactor.SRC_ALPHA,destination==1?DestFactor.ONE:DestFactor.ONE_MINUS_SRC_ALPHA)));
            if(destination==0||cutout)builder.withShaderDefine("ALPHA_CUTOUT",.1f);
            var pipeline=builder.build();
            // Shader packs light alpha-blended overlays; only additive draws are emissive.
            IrisRendering.register(pipeline,particle,destination==1,destination==0||cutout);
            return pipeline;
        });
    }
    public static RenderType material(Identifier texture,int source,int destination,boolean depthWrite,boolean cull){
        return MATERIALS.computeIfAbsent(new Material(texture,source,destination,depthWrite,cull),m->{
            // The original Duplicator enables alpha testing around this binary-alpha
            // texture. Transparent piston faces must not write depth over each other.
            // Atlas overlays keep blending, but their transparent pixels must not
            // write depth over the void chest's separately submitted interior.
            RenderPipeline pipe=pipeline(false,source,texture.equals(Thaumcraft.id("textures/legacy/duplicator.png"))?0:destination,depthWrite,cull,
                texture.equals(Thaumcraft.id("textures/legacy/blocks.png"))||texture.equals(Thaumcraft.id("textures/legacy/items.png")));
            if(texture.getNamespace().equals("thaumcraft2tp")&&texture.getPath().startsWith("portal/"))pipe=PIPELINES.computeIfAbsent("portal",k->{var pipeline=RenderPipeline.builder()
                .withLocation(Thaumcraft.id("pipeline/portal")).withVertexShader("core/entity").withFragmentShader(Thaumcraft.id("core/portal"))
                .withUniform("DynamicTransforms",UniformType.UNIFORM_BUFFER).withUniform("Projection",UniformType.UNIFORM_BUFFER).withUniform("Fog",UniformType.UNIFORM_BUFFER).withUniform("Lighting",UniformType.UNIFORM_BUFFER)
                .withSampler("Sampler0").withShaderDefine("NO_OVERLAY").withShaderDefine("NO_CARDINAL_LIGHTING").withShaderDefine("EMISSIVE")
                .withVertexFormat(DefaultVertexFormat.ENTITY,VertexFormat.Mode.QUADS).withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL,true)).withCull(false).build();
                IrisRendering.register(pipeline,false,true,false);return pipeline;
            });
            return dev.thaumcraft.mixin.RenderTypeInvoker.thaumcraft$create("thaumcraft_legacy",RenderSetup.builder(pipe).withTexture("Sampler0",texture).useLightmap().useOverlay().createRenderSetup());
        });
    }
}
