package dev.thaumcraft.client.legacy;

import dev.thaumcraft.entity.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3f;
import java.util.List;
import static dev.thaumcraft.client.legacy.LegacyCompat.*;

/** Extracts original effects at local coordinates, preserving precision far from spawn. */
public final class LegacyEntityVisuals {
    private static final EntityWispRenderer WISP=new EntityWispRenderer();
    private static final EntitySingularityRenderer SINGULARITY=new EntitySingularityRenderer();
    private static final ModelCarpet CARPET=new ModelCarpet();
    private LegacyEntityVisuals() {}
    public static List<LegacyDraw.Batch> effects(Entity entity,float partial){
        var world=new World(entity.level(),entity.getId());
        return capture(world,BlockPos.ZERO,0xf000f0,entity.level().getGameTime()+partial,()->{
            var rotation=net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera().rotation();
            var right=rotation.transform(new Vector3f(1,0,0));var up=rotation.transform(new Vector3f(0,1,0));
            ActiveRenderInfo.rotationX=right.x;ActiveRenderInfo.rotationZ=right.z;ActiveRenderInfo.rotationXZ=up.y;ActiveRenderInfo.rotationYZ=up.x;ActiveRenderInfo.rotationXY=up.z;
            if(entity instanceof WispEntity wisp){var legacy=new EntityWisp();legacy.type=wisp.element();legacy.health=(int)Math.ceil(wisp.getHealth());WISP.renderEntityAt(legacy,0,0,0,partial);}
            else if(entity instanceof RelicProjectile relic){
                if(relic.mode()==0){var legacy=new EntitySingularity();legacy.fuse=60-relic.visualAge();SINGULARITY.renderEntityAt(legacy,0,0,0,partial);}
                else if(relic.mode()==4)waterBillboard(relic.visualAge()+partial,relic.waterLifetime(),relic.waterBlue());
                else billboard(159+16*(relic.mode()-1),.2f);
            }
        });
    }
    private static void waterBillboard(float age,int lifetime,float blue){
        var draw=context().draw();draw.bind(texture("/thaumcraft/resources/p_large.png"));draw.enable(3042,true);draw.blend(770,1);draw.depthWrite(false);
        draw.color(.2f,.2f,blue,.5f);
        float size=.5f*(age/lifetime<.2f?.1f:age/lifetime);
        float rx=ActiveRenderInfo.rotationX*size,rz=ActiveRenderInfo.rotationZ*size,ux=ActiveRenderInfo.rotationYZ*size,uy=ActiveRenderInfo.rotationXZ*size,uz=ActiveRenderInfo.rotationXY*size;
        draw.begin(7);draw.vertex(-rx-ux,-uy,-rz-uz,0,1);draw.vertex(-rx+ux,uy,-rz+uz,1,1);draw.vertex(rx+ux,uy,rz+uz,1,0);draw.vertex(rx-ux,-uy,rz-uz,0,0);draw.end();
    }
    private static void billboard(int icon,float size){
        var draw=context().draw();draw.bind(texture("/thaumcraft/resources/items.png"));draw.enable(3042,true);draw.blend(770,771);draw.depthWrite(false);
        float u=(icon%16)/16f,v=(icon/16)/16f,extent=15.99f/256;
        float rx=ActiveRenderInfo.rotationX*size,rz=ActiveRenderInfo.rotationZ*size,ux=ActiveRenderInfo.rotationYZ*size,uy=ActiveRenderInfo.rotationXZ*size,uz=ActiveRenderInfo.rotationXY*size;
        draw.begin(7);draw.vertex(-rx-ux,-uy,-rz-uz,u+extent,v+extent);draw.vertex(-rx+ux,uy,-rz+uz,u+extent,v);draw.vertex(rx+ux,uy,rz+uz,u,v);draw.vertex(rx-ux,-uy,rz-uz,u,v+extent);draw.end();
    }
    public static List<LegacyDraw.Batch> carpet(CarpetEntity entity,float partial,int light){
        int age=entity.getFirstPassenger()!=null?entity.getFirstPassenger().tickCount:entity.tickCount;
        return capture(new World(entity.level(),entity.getId()),BlockPos.ZERO,light,age+partial,()->{
            GL11.glTranslatef(0,.4f,0);
            // Modern movement yaw is player yaw; legacy carpetYaw was player yaw minus 90.
            GL11.glRotatef(180-net.minecraft.util.Mth.rotLerp(partial,entity.yRotO,entity.getYRot()),0,1,0);
            float hit=entity.timeSinceHit()-partial,damage=Math.max(0,entity.damageTaken()-partial);
            if(hit>0)GL11.glRotatef((float)Math.sin(hit)*hit*damage/10*entity.forwardDirection(),1,0,0);
            MinecraftForgeClient.bindTexture("/thaumcraft/resources/carpet.png");GL11.glScalef(-.575f,-.25f,.575f);CARPET.render();
        });
    }
    public static void ambient(Entity entity){
        var world=new World(entity.level(),entity.tickCount*31L+entity.getId());world.effectsAllowed=true;
        capture(world,BlockPos.ZERO,0xf000f0,entity.level().getGameTime(),()->{
            if(entity instanceof CarpetEntity){carpetSparkles(world,entity);return;}
            double x=entity.getX(),y=entity.getY()+entity.getBbHeight()/2,z=entity.getZ();
            if(entity instanceof RelicProjectile relic&&relic.mode()==0){var trail=new FXWisp(world,x,entity.getY()+.1,z,.4,0);trail.shrink=true;new Effects().addEffect(trail);return;}
            if(entity instanceof RelicProjectile relic&&relic.mode()==4){
                if(world.rand.nextInt(5)==0){
                    var r=world.rand;double xx=x+(r.nextFloat()-r.nextFloat())*.1,yy=entity.getY()+(r.nextFloat()-r.nextFloat())*.1,zz=z+(r.nextFloat()-r.nextFloat())*.1;
                    var motion=relic.getDeltaMovement();
                    var sparkle=new FXSparkle(world,xx,yy,zz,xx+motion.x,yy+motion.y,zz+motion.z,.5f,r.nextBoolean()?2:6,2+r.nextInt(2));
                    sparkle.setGravity(.05f);new Effects().addEffect(sparkle);
                }
                return;
            }
            if(entity instanceof WispEntity wisp){new Effects().addEffect(new FXWisp(world,entity.getX()+world.rand.nextFloat()-world.rand.nextFloat(),entity.getY()+world.rand.nextFloat()-world.rand.nextFloat(),entity.getZ()+world.rand.nextFloat()-world.rand.nextFloat(),.3,wisp.element()));return;}
            var effect=new FXWisp(world,x,y,z,x+(world.rand.nextFloat()-world.rand.nextFloat())*2,y+(world.rand.nextFloat()-world.rand.nextFloat())*2,z+(world.rand.nextFloat()-world.rand.nextFloat())*2,.3,5);
            new Effects().addEffect(effect);
        });
    }
    private static void carpetSparkles(World world,Entity carpet){
        double yaw=Math.toRadians(carpet.getYRot()-90),c=Math.cos(yaw),s=Math.sin(yaw);
        double x=carpet.getX(),y=carpet.getY()+.275,z=carpet.getZ();
        double speed=carpet.getDeltaMovement().horizontalDistance();
        var r=world.rand;var effects=new Effects();
        if(speed>.15){
            for(int i=0;i<1+speed*60;i++){
                double along=r.nextFloat()*2-1,side=(r.nextInt(2)*2-1)*.7;
                if(r.nextBoolean())effects.addEffect(new FXSparkle(world,x-c*along*.8+s*side,y,z-s*along*.8-c*side,1,1,2));
                else effects.addEffect(new FXSparkle(world,x+c+s*along*.7,y,z+s-c*along*.7,1,1,2));
            }
        }else{
            double along=r.nextFloat()*2-1,side=(r.nextInt(2)*2-1)*.7;
            if(r.nextBoolean())effects.addEffect(new FXSparkle(world,x-c*along*.8+s*side,y,z-s*along*.8-c*side,1,1,2));
            if(r.nextBoolean())effects.addEffect(new FXSparkle(world,x+c+s*along*.8,y,z+s-c*along*.8,1,1,2));
            if(r.nextBoolean())effects.addEffect(new FXSparkle(world,x-c+s*along*.8,y,z-s-c*along*.8,1,1,2));
        }
    }
}
