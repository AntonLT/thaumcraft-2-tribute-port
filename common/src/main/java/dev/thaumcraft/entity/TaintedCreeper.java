package dev.thaumcraft.entity;

import dev.thaumcraft.content.TaintBlock;
import dev.thaumcraft.gameplay.ArcaneWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;

/** The original smaller explosion also seeds corruption in its surroundings. */
public final class TaintedCreeper extends Creeper {
    public TaintedCreeper(EntityType<? extends Creeper> type,Level level){super(type,level);setPersistenceRequired();}
    @Override public void tick(){
        if(isAlive()&&level() instanceof ServerLevel server&&(isIgnited()||getSwellDir()>0)&&getSwelling(1)>=29f/28f){
            int strength=isPowered()?2:1;
            ArcaneWorldData.get(server).changeAura(server,blockPosition(),0,100*strength+random.nextInt(100*strength));
            server.explode(this,getX(),getY(),getZ(),2f*strength,Level.ExplosionInteraction.MOB);
            BlockPos center=blockPosition();
            for(BlockPos pos:BlockPos.betweenClosed(center.offset(-2,-2,-2),center.offset(2,2,2)))
                if(pos.distSqr(center)<=4&&server.hasChunkAt(pos))TaintBlock.increase(server,pos,ArcaneWorldData.get(server).aura(server,pos).taint(),random);
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.WITCH,getX(),getY()+getBbHeight()/2,getZ(),100,2,2,2,.1);
            discard();return;
        }
        super.tick();
        if(level().isClientSide()&&random.nextInt(5)==0)dev.thaumcraft.content.VisualEffects.entity.accept(this);
    }
}
