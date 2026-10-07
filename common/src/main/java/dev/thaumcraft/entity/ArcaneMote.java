package dev.thaumcraft.entity;

import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.*;
import java.util.UUID;

/** Original moving effect simulation, with damage performed on the authoritative server. */
public final class ArcaneMote extends Entity {
    public static final int SCORCH=0,BEAM=1,FREEZE=2,WIND=3,GUIDE=4;
    private static final EntityDataAccessor<Integer> KIND=SynchedEntityData.defineId(ArcaneMote.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> AGE=SynchedEntityData.defineId(ArcaneMote.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> LIFE=SynchedEntityData.defineId(ArcaneMote.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FLAGS=SynchedEntityData.defineId(ArcaneMote.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SCALE=SynchedEntityData.defineId(ArcaneMote.class,EntityDataSerializers.FLOAT);
    public static final int LANCE=1,HOMING=2,MOTES=4,CORE=8,PUSH=16;
    private Vec3 destination=Vec3.ZERO;
    private UUID target,owner;
    private int damage,filter=7,age;
    private float speed=3;
    private boolean guideArrived;
    public ArcaneMote(EntityType<? extends ArcaneMote> type,Level level){super(type,level);noPhysics=true;}
    @Override protected void defineSynchedData(SynchedEntityData.Builder data){data.define(KIND,0);data.define(AGE,0);data.define(LIFE,100);data.define(FLAGS,0);data.define(SCALE,2f);}
    public int kind(){return entityData.get(KIND);}
    /** Both sides count age locally; the server only syncs jumps (scorch hitting a wall) rather than every tick. */
    public int age(){return age;}
    public int lifetime(){return entityData.get(LIFE);}
    public float scale(){return entityData.get(SCALE);}
    public boolean flag(int mask){return (entityData.get(FLAGS)&mask)!=0;}
    public boolean guideArrived(){return guideArrived;}
    @Override public EntityDimensions getDimensions(Pose pose){return kind()==GUIDE?EntityDimensions.fixed(.2f,.2f):super.getDimensions(pose);}
    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> field){super.onSyncedDataUpdated(field);if(field.equals(KIND))refreshDimensions();if(field.equals(AGE))age=entityData.get(AGE);}
    public static void guide(ServerLevel level,Player player,Vec3 destination){
        var mote=create(level,GUIDE,player.getEyePosition().add(player.getLookAngle().scale(.3)),destination,1000,.3f,0);
        if(mote!=null){mote.noPhysics=false;level.addFreshEntity(mote);}
    }
    private static ArcaneMote create(ServerLevel level,int kind,Vec3 origin,Vec3 destination,int life,float size,int flags){
        ArcaneMote mote=ModEntities.ARCANE_MOTE.create(level,EntitySpawnReason.MOB_SUMMONED);
        if(mote==null)return null;
        mote.setPos(origin);mote.destination=destination;mote.entityData.set(KIND,kind);mote.entityData.set(LIFE,Math.max(1,life));mote.entityData.set(SCALE,size);mote.entityData.set(FLAGS,flags);
        return mote;
    }
    private static Vec3 jitter(Level level,double size){var r=level.getRandom();return new Vec3((r.nextFloat()-r.nextFloat())*size,(r.nextFloat()-r.nextFloat())*size,(r.nextFloat()-r.nextFloat())*size);}
    private static Vec3 uniformJitter(Level level,double size){var r=level.getRandom();return new Vec3((r.nextFloat()*2-1)*size,(r.nextFloat()*2-1)*size,(r.nextFloat()*2-1)*size);}
    public static void fireWand(ServerLevel level,Player player,int potency){
        Vec3 look=player.getLookAngle(),origin=player.getEyePosition().add(-Math.cos(Math.toRadians(player.getYRot()))*.16,0,-Math.sin(Math.toRadians(player.getYRot()))*.16).add(look.scale(.3));
        for(int i=0;i<3;i++){
            var mote=create(level,SCORCH,origin,origin.add(look.scale(100)).add(jitter(level,10)),100,2+level.getRandom().nextFloat()*.5f,0);
            if(mote!=null){mote.owner=player.getUUID();mote.damage=3+potency;level.addFreshEntity(mote);}
        }
    }
    public static void scorch(ServerLevel level,Vec3 origin,LivingEntity target,int amount,boolean lance,boolean mobs,boolean animals,boolean players){
        Vec3 end=target.position().add(0,target.getBbHeight()/2,0);double distance=origin.distanceTo(target.position());
        for(int i=0;i<amount;i++){
            var mote=create(level,SCORCH,origin,end.add(lance?Vec3.ZERO:jitter(level,amount*.5)),(int)(distance*(lance?3:5)),2+level.getRandom().nextFloat()*.5f,lance?LANCE:0);
            if(mote!=null){mote.damage=lance?3:2;mote.filter=(mobs?1:0)|(animals?2:0)|(players?4:0);level.addFreshEntity(mote);}
        }
    }
    public static void beam(ServerLevel level,Vec3 origin,LivingEntity target,int damage,boolean homing,boolean core,float speed){
        Vec3 end=target.position().add(0,target.getBbHeight()/2,0);
        // Legacy constructor sampled initial speed=3 before the seal applied its speed setting.
        var mote=create(level,BEAM,origin,end,(int)(origin.distanceTo(end)*3.5),.9f,MOTES|PUSH|(homing?HOMING:0)|(core?CORE:0));
        if(mote!=null){mote.target=target.getUUID();mote.damage=damage;mote.speed=speed;mote.setDeltaMovement(end.subtract(origin).normalize().scale(1.0/3));level.addFreshEntity(mote);}
    }
    public static void freeze(ServerLevel level,Vec3 origin,LivingEntity target){
        double distance=origin.distanceTo(target.position());
        for(int i=0;i<3;i++){var mote=create(level,FREEZE,origin,target.position().add(uniformJitter(level,.3)),(int)(distance*8),.5f+level.getRandom().nextFloat()*.2f,0);if(mote!=null)level.addFreshEntity(mote);}
    }
    public static void wind(ServerLevel level,Vec3 origin,Vec3 end,boolean pull){
        int life=Math.max(1,(int)(origin.distanceTo(end)*10));Vec3 velocity=end.subtract(origin).normalize().scale(.1);
        var mote=create(level,WIND,pull?origin.add(velocity.scale(life)):origin,end,life,.75f*(1+level.getRandom().nextFloat()),0);
        if(mote!=null){mote.setDeltaMovement(pull?velocity.scale(-1):velocity);level.addFreshEntity(mote);}
    }
    @Override public void tick(){
        super.tick();if(!(level() instanceof ServerLevel level)){age++;dev.thaumcraft.content.VisualEffects.entity.accept(this);return;}
        if(age>=lifetime()){discard();return;}
        if(kind()==GUIDE){
            if(position().distanceTo(destination)<.75){
                level.broadcastEntityEvent(this,(byte)60);
                level.playSound(null,blockPosition(),net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP,net.minecraft.sounds.SoundSource.PLAYERS,.5f,level.getRandom().nextFloat()*.4f+.8f);
                discard();return;
            }
            var motion=destination.subtract(position()).scale(.02);setDeltaMovement(motion);move(MoverType.SELF,motion);
            setDeltaMovement(getDeltaMovement().multiply(onGround()?.98*.7:.98,.98,onGround()?.98*.7:.98));
            age++;return;
        }
        Vec3 velocity=getDeltaMovement();
        if(kind()==BEAM){
            if(flag(HOMING)&&target!=null&&level.getEntity(target) instanceof LivingEntity living)destination=living.position().add(0,living.getBbHeight()/2,0);
            if(flag(HOMING))velocity=destination.subtract(position()).normalize().scale(1/speed);
        }else if(kind()==SCORCH||kind()==FREEZE){
            velocity=destination.subtract(position()).normalize().scale(kind()==FREEZE?.125:.4);
            if(kind()==SCORCH&&!flag(LANCE))velocity=velocity.scale((lifetime()-age)/(double)lifetime());
            if(kind()==SCORCH&&age>5&&level.getBlockState(blockPosition()).isSolidRender()){
                var r=level.getRandom();velocity=velocity.multiply(r.nextFloat()*.2,r.nextFloat()*.2,r.nextFloat()*.2);age+=10;entityData.set(AGE,age);
            }
        }
        setDeltaMovement(velocity);Vec3 next=position().add(velocity);
        if(kind()==SCORCH)next=next.add(uniformJitter(level,.035));else if(kind()==FREEZE)next=next.add(uniformJitter(level,.1));
        setPos(next);
        if(kind()==SCORCH||kind()==BEAM){
            AABB collision=new AABB(kind()==BEAM?next:next.subtract(.1,.1,.1),next.add(.1,.1,.1));
            // Legacy particles were absent from the entity list; their modern visual entities must not intercept one another.
            var hits=level.getEntities(this,collision,e->!e.isSpectator()&&!(e instanceof ArcaneMote)&&!(e instanceof LightningEffect)&&!(e instanceof RelicProjectile projectile&&projectile.mode()==4));
            if(!hits.isEmpty()&&hits.getFirst() instanceof LivingEntity hit&&hit.isAlive()&&eligible(hit)){
                Player player=owner==null?null:level.getPlayerByUUID(owner);
                if(kind()==SCORCH)hit.igniteForSeconds(2);
                hit.hurtServer(level,player==null?level.damageSources().lava():level.damageSources().playerAttack(player),damage);
                if(kind()==SCORCH){level.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE,next.x,next.y,next.z,1,0,0,0,0);discard();}
                else {if(flag(PUSH))hit.move(MoverType.SELF,new Vec3(velocity.x/5,0,velocity.z/5));if(scale()<4)entityData.set(SCALE,scale()*1.3f);}
            }
        }
        age++;
        if(kind()!=WIND&&age>=lifetime())discard();
    }
    private boolean eligible(LivingEntity entity){
        if(kind()==BEAM)return !(entity instanceof Player)&&!(entity instanceof TravelingTrunk);
        if(owner!=null)return !(entity instanceof Player);
        return (filter&4)!=0&&(entity instanceof Player||entity instanceof TravelingTrunk||entity instanceof TamableAnimal)||(filter&2)!=0&&entity instanceof Animal||(filter&1)!=0&&entity instanceof net.minecraft.world.entity.monster.Enemy;
    }
    @Override public boolean hurtServer(ServerLevel level,net.minecraft.world.damagesource.DamageSource source,float damage){return false;}
    @Override public void handleEntityEvent(byte event){if(event==60){guideArrived=true;dev.thaumcraft.content.VisualEffects.entity.accept(this);discard();}else super.handleEntityEvent(event);}
    @Override protected void readAdditionalSaveData(ValueInput input){discard();}
    @Override protected void addAdditionalSaveData(ValueOutput output){}
}
