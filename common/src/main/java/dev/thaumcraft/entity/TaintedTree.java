package dev.thaumcraft.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public final class TaintedTree extends Monster {
    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> ANGRY=net.minecraft.network.syncher.SynchedEntityData.defineId(TaintedTree.class,net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> SEED=net.minecraft.network.syncher.SynchedEntityData.defineId(TaintedTree.class,net.minecraft.network.syncher.EntityDataSerializers.INT);
    private int anger,shudder,attackDelay;
    @Override protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder){super.defineSynchedData(builder);builder.define(ANGRY,false);builder.define(SEED,0);}
    public boolean isAngry(){return entityData.get(ANGRY);}
    public int modelSeed(){return entityData.get(SEED);}
    public int shudder(){return shudder;}
    @Override public void tick(){super.tick();if(shudder>0)shudder--;}
    @Override public void handleEntityEvent(byte event){if(event<=-6&&event>=-10)shudder=-event;else super.handleEntityEvent(event);}
    public TaintedTree(EntityType<? extends TaintedTree> type,Level level) {super(type,level);setPersistenceRequired();xpReward=15;if(!level.isClientSide())entityData.set(SEED,random.nextInt(10));}
    @Override protected void registerGoals() {targetSelector.addGoal(1,new HurtByTargetGoal(this));}
    @Override protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        setDeltaMovement(0,Math.min(0,getDeltaMovement().y),0);
        setYRot(0);
        if(attackDelay>0)attackDelay--;
        if(anger>0)anger--;else setTarget(null);
        if(getTarget()!=null&&distanceToSqr(getTarget())<100&&getTarget().getBoundingBox().maxY>getBoundingBox().minY&&getTarget().getBoundingBox().minY<getBoundingBox().maxY&&attackDelay==0){
            attackDelay=25;if(random.nextInt(3)==0)spawnGrub(level);
        }
        for(var arrow:level.getEntitiesOfClass(net.minecraft.world.entity.projectile.arrow.AbstractArrow.class,new net.minecraft.world.phys.AABB(getX()-6,getY()-6,getZ()-6,getX()+7,getY()+7,getZ()+7))) {
            arrow.setDeltaMovement(arrow.getDeltaMovement().scale(-1));
            arrow.setXRot(-arrow.getXRot());arrow.setYRot(-arrow.getYRot());arrow.needsSync=true;
            level.playSound(null,arrow.getX(),arrow.getY(),arrow.getZ(),net.minecraft.sounds.SoundEvents.DISPENSER_FAIL,net.minecraft.sounds.SoundSource.HOSTILE,1,1);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF,arrow.getX(),arrow.getY(),arrow.getZ(),1,0,0,0,.01);
            if(arrow.getOwner() instanceof net.minecraft.world.entity.LivingEntity attacker){setTarget(attacker);anger=500;}
        }
        if(getTarget()!=null&&!getTarget().isAlive()){setTarget(null);anger=5;}
        entityData.set(ANGRY,anger>0&&getTarget()!=null);
        if(dev.thaumcraft.PortConfig.taintSpread&&random.nextInt(100)==0) {
            var pos=blockPosition();var aura=dev.thaumcraft.gameplay.ArcaneWorldData.get(level).aura(level,pos);
            if(!dev.thaumcraft.content.TaintBlock.shouldHeal(aura,random))dev.thaumcraft.content.TaintBlock.spreadFrom(level,pos,aura,random);
        }
    }
    @Override public boolean hurtServer(ServerLevel level,DamageSource source,float amount) {
        boolean hurt=super.hurtServer(level,source,amount);
        if(hurt){if(source.getEntity() instanceof net.minecraft.world.entity.LivingEntity attacker)setTarget(attacker);anger=1000;entityData.set(ANGRY,true);if(random.nextInt(3)==0)spawnGrub(level);}
        return hurt;
    }
    private void spawnGrub(ServerLevel level) {
        // Positive vanilla events include client-side casts to unrelated entity types.
        level.broadcastEntityEvent(this,(byte)(-6-random.nextInt(5)));
        var entity=ModEntities.TYPES.get("grub").create(level,EntitySpawnReason.MOB_SUMMONED);
        if(entity==null)return;
        entity.setPos(getX()+(random.nextFloat()-random.nextFloat())*2,getY()+4,getZ()+(random.nextFloat()-random.nextFloat())*2);
        level.addFreshEntity(entity);
        dev.thaumcraft.content.ModSounds.playAt(level,entity.getX(),entity.getY(),entity.getZ(),"gore",net.minecraft.sounds.SoundSource.HOSTILE,1,(random.nextFloat()-random.nextFloat())*.2f+1);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF,entity.getX(),entity.getY(),entity.getZ(),8,.2,.2,.2,.01);
    }
    @Override protected net.minecraft.sounds.SoundEvent getAmbientSound(){return anger>0?net.minecraft.sounds.SoundEvents.BLAZE_AMBIENT:null;}
    @Override protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource source){return net.minecraft.sounds.SoundEvents.PLAYER_HURT;}
    @Override protected net.minecraft.sounds.SoundEvent getDeathSound(){return net.minecraft.sounds.SoundEvents.BLAZE_DEATH;}
    @Override protected float getSoundVolume(){return .5f;}
    @Override protected void dropFromLootTable(ServerLevel level,DamageSource source,boolean playerKilled){
        var table=getLootTable();
        if(!table.equals(getType().getDefaultLootTable())){super.dropFromLootTable(level,source,playerKilled);return;}
        table.ifPresent(key->{for(int i=0;i<2+LegacyMobLoot.looting(level,source);i++)dropFromLootTable(level,source,playerKilled,key);});
    }
    @Override protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output){super.addAdditionalSaveData(output);output.putInt("anger",anger);output.putInt("model_seed",modelSeed());}
    @Override protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input){super.readAdditionalSaveData(input);anger=input.getIntOr("anger",0);entityData.set(SEED,Math.floorMod(input.getIntOr("model_seed",modelSeed()),10));entityData.set(ANGRY,anger>0);}
}
