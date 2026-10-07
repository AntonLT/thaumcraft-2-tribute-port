package dev.thaumcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.thaumcraft.Thaumcraft;
import dev.thaumcraft.client.legacy.*;
import dev.thaumcraft.entity.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import java.util.List;

/** Original elemental two-pass lightning ribbons, joined corners, caps, reveal and fade. */
public final class LightningEffectRenderer extends EntityRenderer<LightningEffect,LightningEffectRenderer.State> {
    private static final float[][] OUTER={{.6f,.3f,.6f},{.6f,.6f,.1f},{.1f,.1f,.6f},{.1f,1,.1f},{.6f,.1f,.1f},{.6f,.2f,.6f}};
    private static final float[][] INNER={{1,.6f,1},{1,1,.1f},{.1f,.1f,1},{.1f,.6f,.1f},{1,.1f,.1f},{0,0,0}};
    public static final class State extends EntityRenderState {List<LegacyDraw.Batch> batches=List.of();}
    public LightningEffectRenderer(EntityRendererProvider.Context context){super(context);shadowRadius=0;}
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(LightningEffect bolt,State state,float partial){
        super.extractRenderState(bolt,state,partial);
        var camera=Minecraft.getInstance().gameRenderer.getMainCamera();Vec3 view=new Vec3(camera.rotation().transform(new Vector3f(0,0,-1))),relativeCamera=camera.position().subtract(bolt.position());
        int range=dev.thaumcraft.PortConfig.lowGfx?50:100;if(relativeCamera.lengthSqr()>range*range){state.batches=List.of();return;}
        var geometry=bolt.geometry();double reveal=Math.max(1,(int)(geometry.length*3));int visible=(int)((bolt.age()+partial+reveal)/reveal*geometry.mainSegments());float fraction=bolt.age()<0?0:Math.clamp((float)bolt.age()/bolt.duration(),0,1);
        state.batches=LegacyCompat.capture(new LegacyCompat.World(bolt.level(),bolt.getId()),BlockPos.ZERO,0xf000f0,bolt.age()+partial,()->{
            var draw=LegacyCompat.context().draw();draw.enable(3042,true);draw.blend(770,bolt.element()==5?771:1);draw.depthWrite(false);
            for(int pass=0;pass<2;pass++){
                draw.bind(Thaumcraft.id("textures/legacy/"+(pass==0?"p_large":"p_small")+".png"));float alpha=pass==0?(1-fraction)*.4f:1-fraction*.5f;draw.begin(7);
                for(var segment:geometry.segments()){
                    if(segment.number>visible)continue;float[] color=(pass==0?OUTER:INNER)[bolt.element()];draw.color(color[0],color[1],color[2],alpha*segment.light);
                    Vec3 start=segment.start.position(),end=segment.end.position();double width=.03*(relativeCamera.distanceTo(start)/5+1)*(1+segment.light)*.5;
                    Vec3 sideStart=view.cross(segment.previousTangent).scale(width/segment.previousSine),sideEnd=view.cross(segment.nextTangent).scale(width/segment.nextSine);
                    quad(draw,end,sideEnd,start,sideStart,.5,.5);
                    if(segment.next==null)quad(draw,end.add(segment.diff.normalize().scale(width)),sideEnd,end,sideEnd,0,.5);
                    if(segment.previous==null)quad(draw,start,sideStart,start.subtract(segment.diff.normalize().scale(width)),sideStart,.5,0);
                }
                draw.end();
            }
        });
    }
    private static void quad(LegacyDraw draw,Vec3 a,Vec3 sideA,Vec3 b,Vec3 sideB,double uA,double uB){
        vertex(draw,a.subtract(sideA),uA,0);vertex(draw,b.subtract(sideB),uB,0);vertex(draw,b.add(sideB),uB,1);vertex(draw,a.add(sideA),uA,1);
    }
    private static void vertex(LegacyDraw draw,Vec3 p,double u,double v){draw.vertex(p.x,p.y,p.z,u,v);}
    @Override public void submit(State state,PoseStack poses,SubmitNodeCollector collector,CameraRenderState camera){if(!state.isInvisible)LegacyDraw.submit(state.batches,poses,collector);}
}
