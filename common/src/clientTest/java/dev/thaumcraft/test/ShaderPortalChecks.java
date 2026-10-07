package dev.thaumcraft.test;

import dev.thaumcraft.Thaumcraft;
import dev.thaumcraft.client.legacy.IrisRendering;
import dev.thaumcraft.client.legacy.LegacyVisuals;
import net.minecraft.client.Minecraft;
import java.util.Map;

/** Shader buffers must survive secondary views without becoming a destination's buffers. */
final class ShaderPortalChecks {
    private static Object mainPipeline;
    private static Object nearPipeline;
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError("Shader portal: "+message);}
    static void capture(){
        if(IrisRendering.active())mainPipeline=net.irisshaders.iris.Iris.getPipelineManager().getPipelineNullable();
    }
    private static Map<?,?> views() throws ReflectiveOperationException {
        var support=Class.forName("dev.thaumcraft.client.legacy.IrisRendering$Support");
        var field=support.getDeclaredField("VIEWS");field.setAccessible(true);return (Map<?,?>)field.get(null);
    }
    static void captureNear(){
        if(mainPipeline==null)return;
        try{
            nearPipeline=views().get(dev.thaumcraft.client.legacy.PortalViews.framebuffer(PortalSceneChecks.SOURCE));
            check(nearPipeline!=null&&nearPipeline!=mainPipeline,"near framebuffer owns its shader pipeline");
        }catch(ReflectiveOperationException e){throw new AssertionError("Cannot inspect near shader ownership",e);}
    }
    static void verify(){
        if(mainPipeline==null)return;
        var mc=Minecraft.getInstance();
        check(net.irisshaders.iris.Iris.getPipelineManager().getPipelineNullable()==mainPipeline,"main shader pipeline is restored");
        try {
            var pipelines=views();
            var remotePipeline=pipelines.get(dev.thaumcraft.client.legacy.PortalViews.framebuffer(PortalSceneChecks.SOURCE));
            check(remotePipeline!=null,"remote framebuffer owns its shader pipeline");
            if(nearPipeline!=null)check(remotePipeline!=nearPipeline&&!pipelines.containsValue(nearPipeline),"destination change releases the previous shader history");
            check(pipelines.values().stream().noneMatch(p->p==mainPipeline),"portal views never borrow the main pipeline");
            var field=mainPipeline.getClass().getDeclaredField("renderTargets");field.setAccessible(true);
            var targets=(net.irisshaders.iris.targets.RenderTargets)field.get(mainPipeline);
            check(targets.getCurrentWidth()==mc.getMainRenderTarget().width&&targets.getCurrentHeight()==mc.getMainRenderTarget().height,"main shader buffers retain window dimensions");
        }catch(ReflectiveOperationException e){throw new AssertionError("Cannot inspect shader buffer ownership",e);}
        var portal=LegacyVisuals.render(mc.level.getBlockEntity(PortalSceneChecks.SOURCE),.5f,0xf000f0).stream()
            .filter(batch->batch.texture().getPath().startsWith("portal/")).findFirst().orElseThrow();
        check(portal.vertices().size()==40,"ten-sided aperture survives shader program replacement");
        for(int i=0;i<40;i+=4){var center=portal.vertices().get(i);check(Math.abs(center.u()-.5f)<1e-6&&Math.abs(center.v()-.5f)<1e-6,"aperture triangle has the texture center");}
        Thaumcraft.LOG.info("THAUMCRAFT_SHADER_PORTAL_PASS buffer_ownership_restore_aperture");
    }
    static void closed(){
        if(mainPipeline==null)return;
        try{check(views().isEmpty(),"portal cleanup releases all destination shader pipelines");}
        catch(ReflectiveOperationException e){throw new AssertionError("Cannot inspect shader cleanup",e);}
    }
    static void reload(){
        if(mainPipeline==null)return;
        try{net.irisshaders.iris.Iris.reload();}
        catch(java.io.IOException e){throw new AssertionError("Shader reload failed",e);}
        closed();
        check(net.irisshaders.iris.Iris.getPipelineManager().getPipelineNullable()!=mainPipeline,"shader reload replaces the main pipeline");
        Thaumcraft.LOG.info("THAUMCRAFT_SHADER_RELOAD_PASS portal_cleanup");
    }
}
