package dev.thaumcraft.entity;

import dev.thaumcraft.gameplay.ArcaneWorldData;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class ThaumSlime extends Slime {
    private static final EntityDataAccessor<Boolean> TAINTED=SynchedEntityData.defineId(ThaumSlime.class,EntityDataSerializers.BOOLEAN);
    private float absorbedVis,absorbedTaint;
    private int jumpDelay,anger;
    public ThaumSlime(EntityType<? extends ThaumSlime> type,Level level) {
        super(type,level);setSize(2,true);xpReward=1;jumpDelay=random.nextInt(20)+10;
        moveControl=new net.minecraft.world.entity.ai.control.MoveControl(this){@Override public void tick(){hop();}};
    }
    @Override protected void registerGoals(){}
    private void hop(){
        if(anger>0)anger--;else setTarget(null);
        var player=level().getNearestPlayer(getX(),getY(),getZ(),16,p->!p.isSpectator());
        if(anger>0&&getTarget()!=null&&getTarget()!=player)lookAt(getTarget(),10,20);
        if(player!=null)lookAt(player,10,20);
        if(onGround()&&jumpDelay--<=0){
            jumpDelay=random.nextInt(20)+10;if(player!=null)jumpDelay/=3;
            getJumpControl().jump();setSpeed(.1f);xxa=1-random.nextFloat()*2;zza=getSize();
            if(getSize()>2)playSound(net.minecraft.sounds.SoundEvents.SLIME_JUMP,getSoundVolume(),((random.nextFloat()-random.nextFloat())*.2f+1)*.8f);
        }else if(onGround())xxa=zza=0;
    }
    @Override public boolean hurtServer(ServerLevel level,net.minecraft.world.damagesource.DamageSource source,float amount){
        if(source.getDirectEntity() instanceof net.minecraft.world.entity.LivingEntity attacker){setTarget(attacker);anger=300;}
        return super.hurtServer(level,source,amount);
    }
    @Override protected void dealDamage(net.minecraft.world.entity.LivingEntity target){
        if(target instanceof net.minecraft.world.entity.player.Player&&level() instanceof ServerLevel level&&isAlive()&&getSize()>1&&distanceToSqr(target)<.36*getSize()*getSize()&&hasLineOfSight(target))
            if(target.hurtServer(level,damageSources().mobAttack(this),getAttackDamage()))playSound(net.minecraft.sounds.SoundEvents.SLIME_ATTACK,1,(random.nextFloat()-random.nextFloat())*.2f+1);
    }
    @Override protected float getSoundVolume(){return .3f;}
    @Override public void playSound(net.minecraft.sounds.SoundEvent sound,float volume,float pitch){
        if(getSize()<=5&&(sound==net.minecraft.sounds.SoundEvents.SLIME_SQUISH||sound==net.minecraft.sounds.SoundEvents.SLIME_SQUISH_SMALL))return;
        super.playSound(sound,volume,pitch);
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {super.defineSynchedData(builder);builder.define(TAINTED,false);}
    public boolean tainted() {return entityData.get(TAINTED);}
    @Override public void setSize(int size,boolean updateHealth){super.setSize(Math.clamp(size,1,15),updateHealth);xpReward=getSize();}
    @Override protected float getAttackDamage(){return Math.min(10,getSize());}
    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor level,DifficultyInstance difficulty,EntitySpawnReason reason,SpawnGroupData group) {
        var result=super.finalizeSpawn(level,difficulty,reason,group);setSize(2,true);xpReward=1;return result;
    }
    @Override public void die(net.minecraft.world.damagesource.DamageSource source){
        if(isDeadOrDying()&&dead)return;
        super.die(source);
        if(level() instanceof ServerLevel level&&getSize()>1)for(int i=0;i<2;i++)if(random.nextInt(4)<3){
            var child=ModEntities.THAUM_SLIME.create(level,EntitySpawnReason.TRIGGERED);
            if(child==null)continue;
            child.setSize(Math.max(1,getSize()/2),true);child.entityData.set(TAINTED,absorbedTaint>absorbedVis);
            child.snapTo(getX()+(i%2-.5)*getSize()/4,getY()+.5,getZ()+(i/2-.5)*getSize()/4,random.nextFloat()*360,0);
            level.addFreshEntity(child);
        }
    }
    @Override public void remove(net.minecraft.world.entity.Entity.RemovalReason reason) {
        // Children were created in die(). Skip vanilla splitting on both loaders;
        // NeoForge assumes every conversion result is a real child entity.
        if(isDeadOrDying()&&getSize()>1)setSize(1,false);
        super.remove(reason);
    }
    @Override protected void dropFromLootTable(ServerLevel level,net.minecraft.world.damagesource.DamageSource source,boolean playerKilled) {
        if(getSize()<3)return;
        int looting=LegacyMobLoot.looting(level,source);
        getLootTable().ifPresent(table->dropFromLootTable(level,source,playerKilled,table,stack->{
            if(stack.is(dev.thaumcraft.content.Content.item("vis_crystal")))stack.grow(random.nextInt(Math.max(0,looting)+1));
            spawnAtLocation(level,stack);
        }));
    }
    @Override public void tick() {
        boolean wasOnGround=onGround();super.tick();
        if(!(level() instanceof ServerLevel level))return;
        if(!isAlive())return;
        if(onGround()&&!wasOnGround&&getSize()>14)level.explode(this,getX(),getY()+.5,getZ(),1,Level.ExplosionInteraction.MOB);
        var aura=ArcaneWorldData.get(level);
        absorbedVis+=aura.drainVibes(level,blockPosition(),1,false);
        absorbedTaint+=aura.drainVibes(level,blockPosition(),1,true);
        if(getSize()<15&&Math.max(absorbedVis,absorbedTaint)>=50*getSize()) {
            entityData.set(TAINTED,absorbedTaint>=absorbedVis);absorbedVis/=2;absorbedTaint/=2;setSize(getSize()+1,true);
        }
    }
    @Override protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);entityData.set(TAINTED,input.getBooleanOr("tainted",false));
        float legacy=input.getFloatOr("absorbed",0);absorbedVis=input.getFloatOr("absorbed_vis",tainted()?0:legacy);absorbedTaint=input.getFloatOr("absorbed_taint",tainted()?legacy:0);
        anger=input.getIntOr("anger",0);
    }
    @Override protected void addAdditionalSaveData(ValueOutput output) {super.addAdditionalSaveData(output);output.putFloat("absorbed_vis",absorbedVis);output.putFloat("absorbed_taint",absorbedTaint);output.putBoolean("tainted",tainted());output.putInt("anger",anger);}
}
