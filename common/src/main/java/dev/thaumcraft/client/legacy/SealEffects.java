package dev.thaumcraft.client.legacy;

import dev.thaumcraft.network.SealEffect;
import net.minecraft.core.BlockPos;
import static dev.thaumcraft.client.legacy.LegacyCompat.*;

/** Seal-specific use of the existing recovered wisp and sparkle renderers. */
public final class SealEffects {
    private SealEffects(){}
    public static void receive(SealEffect event){
        var level=net.minecraft.client.Minecraft.getInstance().level;if(level==null)return;
        World world=new World(level,level.getRandom().nextLong());world.effectsAllowed=true;
        LegacyCompat.capture(world,BlockPos.ZERO,0xf000f0,level.getGameTime(),()->{
            var r=world.rand;var p=event.origin();var t=event.target();var effects=new Effects();
            boolean low=dev.thaumcraft.PortConfig.lowGfx||!ModLoader.getMinecraftInstance().gameSettings.fancyGraphics;
            switch(event.kind()){
                case SealEffect.BOOST,SealEffect.DETECT,SealEffect.ANCHOR->{
                    int count=event.kind()==SealEffect.DETECT?(low?3:6):1;
                    for(int i=0;i<count;i++){
                        double x=t.x-.5+r.nextFloat(),y=t.y-.5+r.nextFloat(),z=t.z-.5+r.nextFloat();
                        if(event.kind()==SealEffect.ANCHOR)effects.addEffect(new FXSparkle(world,p.x,p.y,p.z,x,y,z,1,r.nextBoolean()?0:3,4));
                        else effects.addEffect(new FXWisp(world,p.x,p.y,p.z,x,y,z,.1,event.kind()==SealEffect.DETECT?4:r.nextInt(5)));
                    }
                }
                case SealEffect.NULLIFY->effects.addEffect(new FXSparkle(world,t.x-.5+r.nextFloat(),t.y-.5+r.nextFloat(),t.z-.5+r.nextFloat(),2,5,5));
                case SealEffect.HEAL->{for(int i=0;i<5;i++)effects.addEffect(new FXWisp(world,p.x,p.y,p.z,t.x+(r.nextFloat()-r.nextFloat())*.5,t.y+(r.nextFloat()-r.nextFloat())*.5,t.z+(r.nextFloat()-r.nextFloat())*.5,.2,r.nextBoolean()?0:2));}
                case SealEffect.HYDRATE,SealEffect.TILL->{
                    boolean down=event.kind()==SealEffect.HYDRATE;
                    for(int i=0;i<(low?5:16);i++){
                        double x=p.x+r.nextFloat(),z=p.z+r.nextFloat(),y=p.y+1,high=y+r.nextFloat()*.5;
                        var sparkle=new FXSparkle(world,x,down?high:y,z,x,down?y:high,z,2,down?2:3,5);sparkle.tinkle=true;effects.addEffect(sparkle);
                    }
                }
                case SealEffect.GROW,SealEffect.REPLANT->{
                    for(int i=0;i<(low?5:10);i++){
                        double x=p.x+r.nextFloat(),z=p.z+r.nextFloat();
                        var wisp=new FXWisp(world,x,p.y,z,x,p.y+.5+r.nextFloat(),z,.3,event.kind()==SealEffect.GROW?3:0);wisp.shrink=true;effects.addEffect(wisp);
                    }
                }
                case SealEffect.HARVEST,SealEffect.POOF->{for(int i=0;i<(low?3:6);i++)level.addParticle(net.minecraft.core.particles.ParticleTypes.POOF,p.x+r.nextFloat(),p.y+r.nextFloat(),p.z+r.nextFloat(),0,0,0);}
                case SealEffect.PICKUP->{
                    for(int i=0;i<5;i++){
                        effects.addEffect(new FXSparkle(world,p.x,p.y,p.z,p.x+(r.nextFloat()-r.nextFloat())*.5,p.y+(r.nextFloat()-r.nextFloat())*.5,p.z+(r.nextFloat()-r.nextFloat())*.5,2,1,3));
                        effects.addEffect(new FXSparkle(world,p.x+(r.nextFloat()-r.nextFloat())*.2,p.y+(r.nextFloat()-r.nextFloat())*.2,p.z+(r.nextFloat()-r.nextFloat())*.2,t.x,t.y,t.z,1,1,3));
                    }
                }
                case SealEffect.SEED->{for(int i=0;i<(low?5:10);i++)effects.addEffect(new FXWisp(world,p.x+.5+r.nextFloat()-r.nextFloat(),p.y+.5+r.nextFloat()-r.nextFloat(),p.z+.5+r.nextFloat()-r.nextFloat(),.3,0));}
            }
        });
    }
}
