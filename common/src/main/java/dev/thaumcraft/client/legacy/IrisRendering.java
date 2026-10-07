package dev.thaumcraft.client.legacy;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import dev.thaumcraft.mixin.IrisPipelineManagerAccess;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.pipeline.IrisPipelines;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.pipeline.programs.ShaderKey;
import net.minecraft.client.renderer.RenderPipelines;
import java.util.IdentityHashMap;
import java.util.Map;

/** Optional Iris integration. Each destination owns its shader buffers and frame history. */
public final class IrisRendering {
    private static final boolean PRESENT=present();
    private IrisRendering() {}
    private static boolean present(){
        try{Class.forName("net.irisshaders.iris.Iris",false,IrisRendering.class.getClassLoader());return true;}
        catch(ClassNotFoundException e){return false;}
    }
    public static void register(RenderPipeline pipeline,boolean particle,boolean emissive,boolean cutout){
        if(PRESENT)Support.register(pipeline,particle,emissive,cutout);
    }
    public static void begin(RenderTarget target){if(PRESENT)Support.begin(target);}
    public static void end(){if(PRESENT)Support.end();}
    public static void close(RenderTarget target){if(PRESENT)Support.close(target);}
    public static boolean active(){return PRESENT&&Iris.isPackInUseQuick();}

    private static final class Support {
        // Near views share a terrain renderer, but each framebuffer needs its own history.
        private static final Map<RenderTarget,WorldRenderingPipeline> VIEWS=new IdentityHashMap<>();
        private static WorldRenderingPipeline previous;
        static void register(RenderPipeline pipeline,boolean particle,boolean emissive,boolean cutout){
            if(particle)IrisPipelines.copyPipeline(RenderPipelines.TRANSLUCENT_PARTICLE,pipeline);
            else if(emissive){
                // Not EYES: packs may skip TAA jitter there, so coplanar layers drawn
                // through block programs (void starfields, infuser disks) fail depth tests.
                IrisPipelines.assignPipeline(pipeline,ShaderKey.BLOCK_ENTITY_BRIGHT);
                IrisPipelines.assignPipelineShadow(pipeline,ShaderKey.SHADOW_ENTITIES_CUTOUT);
            }else IrisPipelines.copyPipeline(cutout?RenderPipelines.ENTITY_CUTOUT:RenderPipelines.ENTITY_TRANSLUCENT,pipeline);
        }
        static void begin(RenderTarget target){
            var manager=Iris.getPipelineManager();
            if(!Iris.isPackInUseQuick())return;
            previous=manager.getPipelineNullable();
            var pipeline=VIEWS.computeIfAbsent(target,key->new IrisRenderingPipeline(Iris.getCurrentPack().orElseThrow().getProgramSet(Iris.getCurrentDimension())));
            ((IrisPipelineManagerAccess)manager).thaumcraft$pipeline(pipeline);
        }
        static void end(){
            if(previous!=null){((IrisPipelineManagerAccess)Iris.getPipelineManager()).thaumcraft$pipeline(previous);previous=null;}
        }
        static void close(RenderTarget target){var pipeline=VIEWS.remove(target);if(pipeline!=null)pipeline.destroy();}
    }
}
