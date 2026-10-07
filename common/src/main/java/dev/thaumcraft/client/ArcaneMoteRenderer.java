package dev.thaumcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.thaumcraft.entity.ArcaneMote;
import dev.thaumcraft.client.legacy.*;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import org.joml.Vector3f;
import java.util.List;

/** Original effect colors, size curves and textures on server-driven moving effects. */
public final class ArcaneMoteRenderer extends EntityRenderer<ArcaneMote,ArcaneMoteRenderer.State> {
    public static final class State extends EntityRenderState {List<LegacyDraw.Batch> batches=List.of();}
    public ArcaneMoteRenderer(EntityRendererProvider.Context context){super(context);shadowRadius=0;}
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(ArcaneMote mote,State state,float partial){
        super.extractRenderState(mote,state,partial);
        var player=net.minecraft.client.Minecraft.getInstance().player;int visibleDistance=dev.thaumcraft.PortConfig.lowGfx?50:100;
        if((mote.kind()==ArcaneMote.GUIDE||mote.kind()==ArcaneMote.BEAM)&&player!=null&&player.distanceToSqr(mote)>visibleDistance*visibleDistance){state.batches=List.of();return;}
        int light=state.lightCoords;
        if(mote.kind()==ArcaneMote.FREEZE){float fraction=Math.clamp((float)mote.age()/mote.lifetime(),0,1),bright=fraction*fraction*fraction*fraction;light=Math.round(((light>>4)&15)*(1-bright)+15*bright)<<4|Math.round(((light>>20)&15)*(1-bright)+15*bright)<<20;}
        else if(mote.kind()!=ArcaneMote.WIND)light=0xf000f0;
        state.batches=LegacyCompat.capture(new LegacyCompat.World(mote.level(),mote.getId()),BlockPos.ZERO,light,mote.age()+partial,()->{
            int kind=mote.kind();if(kind==ArcaneMote.BEAM&&!mote.flag(ArcaneMote.CORE))return;
            float age=mote.age()+partial,fraction=age/mote.lifetime(),size;
            var palette=new java.util.Random(mote.getId());
            Identifier texture;float red=1,green=1,blue=1,alpha=1;
            if(kind==ArcaneMote.SCORCH){texture=Identifier.withDefaultNamespace("textures/particle/flame.png");size=.1f*mote.scale()*(fraction+.5f);red=green=Math.min(1,age*9/mote.lifetime());}
            else if(kind==ArcaneMote.BEAM){texture=dev.thaumcraft.Thaumcraft.id("textures/legacy/s_1_4.png");size=.5f*mote.scale()*(age+3)/mote.lifetime();red=.7f+palette.nextFloat()*.3f;green=blue=.25f;alpha=.5f;}
            else if(kind==ArcaneMote.GUIDE){var r=new java.util.Random(mote.getId());texture=dev.thaumcraft.Thaumcraft.id("textures/legacy/p_large.png");size=.5f*mote.scale()*(float)Math.sin((age+r.nextInt(100000))/5);red=.75f+r.nextFloat()*.25f;green=.25f+r.nextFloat()*.25f;blue=.75f+r.nextFloat()*.25f;alpha=.5f;}
            else {int frame=kind==ArcaneMote.FREEZE?Math.clamp(Math.round(fraction*8),0,7):Math.clamp(7-(int)(fraction*8),0,7);texture=Identifier.withDefaultNamespace("textures/particle/generic_"+frame+".png");size=.1f*mote.scale()*(kind==ArcaneMote.WIND?Math.clamp(fraction*32,0,1):1);red=green=blue=kind==ArcaneMote.WIND?.25f+palette.nextFloat()*.3f:.8f+palette.nextFloat()*.2f;}
            var draw=LegacyCompat.context().draw();draw.bind(texture);draw.enable(3042,true);draw.blend(770,kind==ArcaneMote.BEAM||kind==ArcaneMote.GUIDE?1:771);draw.depthWrite(false);draw.color(red,green,blue,alpha);
            var rotation=net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera().rotation();var right=rotation.transform(new Vector3f(size,0,0));var up=rotation.transform(new Vector3f(0,size,0));
            draw.begin(7);draw.vertex(-right.x-up.x,-right.y-up.y,-right.z-up.z,0,1);draw.vertex(-right.x+up.x,-right.y+up.y,-right.z+up.z,0,0);draw.vertex(right.x+up.x,right.y+up.y,right.z+up.z,1,0);draw.vertex(right.x-up.x,right.y-up.y,right.z-up.z,1,1);draw.end();
        });
    }
    public static void ambient(ArcaneMote mote){
        if(mote.kind()!=ArcaneMote.GUIDE&&(mote.kind()!=ArcaneMote.BEAM||!mote.flag(ArcaneMote.MOTES)||dev.thaumcraft.PortConfig.lowGfx))return;
        var world=new LegacyCompat.World(mote.level(),mote.tickCount*31L+mote.getId());world.effectsAllowed=true;
        LegacyCompat.capture(world,BlockPos.ZERO,0xf000f0,mote.age(),()->{
            var motion=mote.getDeltaMovement();double x=mote.getX(),y=mote.getY(),z=mote.getZ();
            if(mote.kind()==ArcaneMote.GUIDE){
                int count=mote.guideArrived()?30:1;
                for(int i=0;i<count;i++){
                    var effect=mote.guideArrived()?new LegacyCompat.FXWisp(world,x,y,z,x+world.rand.nextFloat()-world.rand.nextFloat(),y+world.rand.nextFloat()-world.rand.nextFloat(),z+world.rand.nextFloat()-world.rand.nextFloat(),.1,0):new LegacyCompat.FXWisp(world,x+(world.rand.nextFloat()-world.rand.nextFloat())*.1,y+(world.rand.nextFloat()-world.rand.nextFloat())*.1,z+(world.rand.nextFloat()-world.rand.nextFloat())*.1,.1,4);
                    effect.shrink=true;new LegacyCompat.Effects().addEffect(effect);
                }
                return;
            }
            x+=(world.rand.nextFloat()-world.rand.nextFloat())*.2;y+=(world.rand.nextFloat()-world.rand.nextFloat())*.2;z+=(world.rand.nextFloat()-world.rand.nextFloat())*.2;
            new LegacyCompat.Effects().addEffect(new LegacyCompat.FXSparkle(world,x,y,z,x+motion.x/(1+world.rand.nextFloat()),y+motion.y/(1+world.rand.nextFloat()),z+motion.z/(1+world.rand.nextFloat()),1+mote.age()/(double)mote.lifetime(),world.rand.nextBoolean()?4:1,2+world.rand.nextInt(3)));
        });
    }
    @Override public void submit(State state,PoseStack poses,SubmitNodeCollector collector,CameraRenderState camera){if(!state.isInvisible)LegacyDraw.submit(state.batches,poses,collector);}
}
