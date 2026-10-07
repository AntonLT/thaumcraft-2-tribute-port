package dev.thaumcraft.client.legacy;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.resource.CrossFrameResourcePool;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.ProjectionType;
import dev.thaumcraft.Thaumcraft;
import dev.thaumcraft.machine.MachineBlockEntity;
import dev.thaumcraft.mixin.PortalCameraInvoker;
import dev.thaumcraft.mixin.PortalMinecraftAccess;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.GameRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

import java.util.LinkedHashMap;
import java.util.Map;

/** Original fixed destination camera, rendered into a separate world renderer and 512px target. */
public final class PortalViews {
    private static final Map<BlockPos,View> VIEWS=new LinkedHashMap<>();
    private static final int MAX_VIEWS=4;
    private static ClientLevel level;
    private static Scene scene,nearScene;
    private static long lastRequest,sequence;
    private static View active;
    private static long frame;
    private static int renderedViews;
    private static boolean constructing;
    private static int renderDistance=-1;
    public static int renderDistance(){return RenderSystem.isOnRenderThread()?renderDistance:-1;}
    public static boolean constructing(){return constructing;}
    private PortalViews() {}
    public static boolean active(){return active!=null;}
    public static Camera camera(){return scene.camera;}
    public static org.joml.Matrix4fc projection(){return scene.state.levelRenderState.cameraRenderState.projectionMatrix;}
    public static FogRenderer fog(){return scene.fog;}
    public static LevelRenderer nearRenderer(){return nearScene==null?null:nearScene.renderer;}
    public static boolean sharedWorld(){return active()&&scene.remote==null;}
    public static RenderTarget target(){return active.target;}
    public static int renderedViews(){return renderedViews;}

    public static LegacyCompat.PortalRenderer request(MachineBlockEntity seal){
        var mc=Minecraft.getInstance();var target=seal.visualPortalTarget();
        if(!dev.thaumcraft.PortConfig.portalGfx||active()||target==null||mc.level==null||!target.dimension().equals(mc.level.dimension()))return null;
        View view=VIEWS.get(seal.getBlockPos());
        if(view==null){
            if(VIEWS.size()>=MAX_VIEWS){
                var oldest=VIEWS.entrySet().stream().min(java.util.Comparator.comparingLong(e->e.getValue().seen)).orElseThrow();
                if(oldest.getValue().seen>=frame-2)return null;
                oldest.getValue().close();VIEWS.remove(oldest.getKey());
            }
            boolean[] used=new boolean[MAX_VIEWS];for(View existing:VIEWS.values())used[existing.slot]=true;
            int slot=0;while(used[slot])slot++;
            view=new View(slot);VIEWS.put(seal.getBlockPos().immutable(),view);
        }
        if(!view.destination.equals(target.pos())){
            IrisRendering.close(view.target);
            if(view.remoteScene!=null){view.remoteScene.close();view.remoteScene=null;}
            view.ready=false;view.requested=0;view.token=++sequence;view.remoteFrames=0;
        }
        view.destination=target.pos();view.facing=seal.visualPortalFacing();view.seen=frame;
        long now=System.currentTimeMillis();
        if(!loaded(mc.level,target.pos())&&now-lastRequest>=1500&&now-view.requested>=10000){
            int radius=Math.clamp(mc.options.getEffectiveRenderDistance(),2,dev.thaumcraft.network.PortalScenes.MAX_RADIUS);
            if(radius!=view.radius&&view.remoteScene!=null){view.remoteScene.close();view.remoteScene=null;view.ready=false;view.remoteFrames=0;}
            view.token=++sequence;view.requested=now;lastRequest=now;view.radius=radius;
            dev.thaumcraft.network.PortalScenes.requestSender.accept(new dev.thaumcraft.network.PortalScenes.Request(seal.getBlockPos(),view.token,view.radius));
        }
        return view.ready?view.legacy:null;
    }

    public static void render(DeltaTracker delta,boolean advance){
        var mc=Minecraft.getInstance();
        if(level!=mc.level){reset();level=mc.level;}
        if(!dev.thaumcraft.PortConfig.portalGfx||!advance||mc.level==null||mc.player==null||!mc.isGameLoadFinished()||active())return;
        frame++;
        VIEWS.entrySet().removeIf(e->{if(frame-e.getValue().seen>120){e.getValue().close();return true;}return false;});
        View view=VIEWS.values().stream().filter(v->frame-v.seen<3&&(loaded(mc.level,v.destination)||(v.remoteScene!=null&&loaded(v.remoteScene.world,v.destination))))
            .min(java.util.Comparator.comparingLong(v->v.rendered)).orElse(null);
        if(view==null)return;
        var mainRenderer=mc.levelRenderer;var mainCamera=mc.gameRenderer.getMainCamera();var mainLevel=mc.level;
        var oldFog=RenderSystem.getShaderFog();var oldGlobalSettings=RenderSystem.getGlobalSettingsUniform();
        RenderSystem.backupProjectionMatrix();active=view;LegacyCompat.PortalRenderer.renderRecursion=2;
        try {
            if(loaded(mainLevel,view.destination)){
                if(nearScene==null)nearScene=new Scene(mc,null,0);scene=nearScene;
            }else {scene=view.remoteScene;renderDistance=view.radius;}
            mc.level=scene.world;
            if(scene.remote!=null){scene.remote.expireEntities();scene.world.setTimeFromServer(mainLevel.getGameTime());scene.world.setRainLevel(mainLevel.getRainLevel(1));scene.world.setThunderLevel(mainLevel.getThunderLevel(1));}
            scene.camera.setLevel(scene.world);
            scene.camera.position(view.destination,view.facing);
            ((PortalMinecraftAccess)mc).thaumcraft$levelRenderer(scene.renderer);
            if(!scene.loaded){scene.renderer.setLevel(scene.world);scene.loaded=true;}
            IrisRendering.begin(view.target);
            scene.camera.tick();
            var options=scene.state.optionsRenderState;
            options.ambientOcclusion=mc.options.ambientOcclusion().get();options.cutoutLeaves=mc.options.cutoutLeaves().get();
            options.renderDistance=scene.remote==null?mc.options.getEffectiveRenderDistance():view.radius;options.cloudStatus=net.minecraft.client.CloudStatus.OFF;options.cloudRange=mc.options.cloudRange().get();
            options.glintSpeed=mc.options.glintSpeed().get();options.glintStrength=mc.options.glintStrength().get();
            scene.state.windowRenderState.width=512;scene.state.windowRenderState.height=512;
            var cameraState=scene.state.levelRenderState.cameraRenderState;
            scene.camera.extractRenderState(cameraState,delta.getGameTimeDeltaPartialTick(false));
            cameraState.fogType=scene.camera.getFluidInCamera();
            // Terrain vertices subtract this UBO's camera position in terrain.vsh.
            // A projection switch alone leaves far chunks offset by the main camera.
            scene.globalSettings.update(512,512,options.glintStrength,scene.world.getGameTime(),delta,0,cameraState.pos,false);
            cameraState.fogData=scene.fog.setupFog(scene.camera,options.renderDistance,delta,0,mc.level);
            scene.fog.updateBuffer(cameraState.fogData);
            var fog=scene.fog.getBuffer(FogRenderer.FogMode.WORLD);
            RenderSystem.setProjectionMatrix(scene.projection.getBuffer(cameraState.projectionMatrix),ProjectionType.PERSPECTIVE);
            RenderSystem.setShaderFog(fog);
            scene.renderer.update(scene.camera);
            scene.renderer.extractLevel(delta,scene.camera,delta.getGameTimeDeltaPartialTick(false));
            scene.renderer.renderLevel(scene.pool,delta,false,cameraState,cameraState.viewRotationMatrix,fog,cameraState.fogData.color,true,scene.state.levelRenderState.chunkSectionsToRender);
            scene.renderer.endFrame();scene.features.endFrame();scene.fog.endFrame();scene.pool.endFrame();
            view.ready=true;view.rendered=frame;renderedViews++;if(scene.remote!=null)view.remoteFrames++;
        } finally {
            IrisRendering.end();
            renderDistance=-1;
            active=null;LegacyCompat.PortalRenderer.renderRecursion=0;
            mc.level=mainLevel;
            ((PortalMinecraftAccess)mc).thaumcraft$levelRenderer(mainRenderer);
            mc.getEntityRenderDispatcher().prepare(mainCamera,mc.crosshairPickEntity);
            mc.getBlockEntityRenderDispatcher().prepare(mainCamera.position());
            RenderSystem.restoreProjectionMatrix();RenderSystem.setShaderFog(oldFog);RenderSystem.setGlobalSettingsUniform(oldGlobalSettings);
        }
    }

    public static void receive(dev.thaumcraft.network.PortalScenes.Snapshot snapshot){
        var mc=Minecraft.getInstance();var view=VIEWS.get(snapshot.source());
        if(mc.level==null||level!=mc.level||view==null||view.token!=snapshot.token()||!snapshot.target().dimension().equals(mc.level.dimension())||!snapshot.target().pos().equals(view.destination))return;
        int dx=Math.abs(snapshot.chunk().getX()-(view.destination.getX()>>4)),dz=Math.abs(snapshot.chunk().getZ()-(view.destination.getZ()>>4));
        if(dx>view.radius||dz>view.radius)return;
        if(view.remoteScene==null)view.remoteScene=new Scene(mc,view.destination,view.radius);
        view.remoteScene.remote.apply(snapshot.chunk());
    }
    public static void receiveEntities(dev.thaumcraft.network.PortalScenes.Entities snapshot){
        var mc=Minecraft.getInstance();var view=VIEWS.get(snapshot.source());
        if(mc.level==null||level!=mc.level||view==null||view.remoteScene==null||view.token!=snapshot.token()||!snapshot.target().dimension().equals(mc.level.dimension())||!snapshot.target().pos().equals(view.destination))return;
        view.remoteScene.remote.applyEntities(snapshot.entities());
    }
    public static boolean loaded(ClientLevel world,BlockPos pos){return world.getChunkSource().getChunk(pos.getX()>>4,pos.getZ()>>4,false)!=null;}
    public static boolean remoteTerrainReady(BlockPos source,BlockPos marker){
        var view=VIEWS.get(source);if(view==null||view.remoteScene==null)return false;
        return view.remoteScene.renderer.isSectionCompiledAndVisible(marker)&&view.remoteScene.renderer.countRenderedSections()>0;
    }
    public static RenderTarget framebuffer(BlockPos source){var view=VIEWS.get(source);return view==null||!view.ready?null:view.target;}
    public static int remoteFrames(BlockPos source){var view=VIEWS.get(source);return view==null?0:view.remoteFrames;}
    public static ClientLevel remoteLevel(BlockPos source){var view=VIEWS.get(source);return view==null||view.remoteScene==null?null:view.remoteScene.world;}
    public static void dirty(LevelRenderer renderer,int x,int y,int z){if(nearScene!=null&&renderer!=nearScene.renderer&&VIEWS.values().stream().noneMatch(v->v.remoteScene!=null&&v.remoteScene.renderer==renderer))nearScene.renderer.setSectionDirty(x,y,z);}
    public static void reset(){
        if(active())return;
        VIEWS.values().forEach(View::close);VIEWS.clear();
        if(nearScene!=null){nearScene.close();nearScene=null;}scene=null;
        level=null;
    }
    private static final class View {
        final int slot;final Identifier id;final TextureTarget target;final LegacyCompat.PortalRenderer legacy=new LegacyCompat.PortalRenderer();
        BlockPos destination=BlockPos.ZERO;Direction facing=Direction.NORTH;long seen,rendered,requested,token;int radius,remoteFrames;boolean ready;Scene remoteScene;
        View(int slot){
            this.slot=slot;id=Thaumcraft.id("portal/view_"+slot);target=new TextureTarget("Thaumcraft portal "+slot,512,512,true);
            Minecraft.getInstance().getTextureManager().register(id,new TargetTexture(target));legacy.portalTexture=LegacyCompat.registerTexture(id);
        }
        void close(){IrisRendering.close(target);if(remoteScene!=null){remoteScene.close();remoteScene=null;}Minecraft.getInstance().getTextureManager().release(id);target.destroyBuffers();}
    }
    private static final class TargetTexture extends AbstractTexture {
        TargetTexture(TextureTarget target){texture=target.getColorTexture();textureView=target.getColorTextureView();}
        @Override public void close(){texture=null;textureView=null;}
    }
    private static final class Scene implements AutoCloseable {
        final GameRenderState state=new GameRenderState();final PortalCamera camera=new PortalCamera();final RenderBuffers buffers=new RenderBuffers(1);
        final FeatureRenderDispatcher features;final LevelRenderer renderer;final FogRenderer fog=new FogRenderer();
        final GlobalSettingsUniform globalSettings=new GlobalSettingsUniform();
        final ProjectionMatrixBuffer projection=new ProjectionMatrixBuffer("Thaumcraft portal");final CrossFrameResourcePool pool=new CrossFrameResourcePool(3);
        final ClientLevel world;final RemotePortalLevel remote;boolean loaded;
        Scene(Minecraft mc,BlockPos destination,int radius){
            camera.setEntity(mc.player);
            features=new FeatureRenderDispatcher(new SubmitNodeStorage(),mc.getModelManager(),buffers.bufferSource(),mc.getAtlasManager(),buffers.outlineBufferSource(),buffers.crumblingBufferSource(),mc.font,state);
            renderer=new LevelRenderer(mc,mc.getEntityRenderDispatcher(),mc.getBlockEntityRenderDispatcher(),buffers,state,features);
            constructing=true;try{renderer.onResourceManagerReload(mc.getResourceManager());}finally{constructing=false;}
            remote=destination==null?null:new RemotePortalLevel(mc,renderer,destination,radius);
            world=remote==null?mc.level:remote.level;camera.setLevel(world);
            if(remote!=null){
                int previousDistance=renderDistance;renderDistance=radius;
                try{renderer.setLevel(world);loaded=true;}finally{renderDistance=previousDistance;}
            }
        }
        @Override public void close(){renderer.setLevel(null);renderer.close();features.close();fog.close();globalSettings.close();projection.close();pool.close();buffers.fixedBufferPack().close();LegacyAnimation.release(world);}
    }
    private static final class PortalCamera extends Camera {
        void position(BlockPos pos,Direction face){
            setPosition(pos.getX()+.5+face.getStepX(),pos.getY()+1.12+face.getStepY(),pos.getZ()+.5+face.getStepZ());
            setRotation(switch(face){case NORTH->180;case WEST->90;case EAST->270;default->0;},face==Direction.DOWN?90:face==Direction.UP?-90:0);
            var access=(PortalCameraInvoker)(Object)this;access.thaumcraft$perspective(.05f,384,110,512,512);
            var projection=new Matrix4f().perspective((float)Math.toRadians(110),1,.05f,384,RenderSystem.getDevice().isZZeroToOne());
            access.thaumcraft$frustum(getViewRotationMatrix(new Matrix4f()),projection,position());
        }
        @Override public boolean isInitialized(){return true;}
        @Override public float getFov(){return 110;}
        @Override public void extractRenderState(CameraRenderState state,float partial){super.extractRenderState(state,partial);state.depthFar=384;state.hudFov=110;}
    }
}
