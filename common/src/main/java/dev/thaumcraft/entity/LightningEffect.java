package dev.thaumcraft.entity;

import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** Visual-only synchronized bolt. Damage stays in the invoking wand/seal exactly once. */
public final class LightningEffect extends Entity {
    private static final EntityDataAccessor<Vector3fc> END=SynchedEntityData.defineId(LightningEffect.class,EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Long> SEED=SynchedEntityData.defineId(LightningEffect.class,EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> ELEMENT=SynchedEntityData.defineId(LightningEffect.class,EntityDataSerializers.INT),DURATION=SynchedEntityData.defineId(LightningEffect.class,EntityDataSerializers.INT),BASE_DURATION=SynchedEntityData.defineId(LightningEffect.class,EntityDataSerializers.INT),SPEED=SynchedEntityData.defineId(LightningEffect.class,EntityDataSerializers.INT),AGE=SynchedEntityData.defineId(LightningEffect.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> MULTIPLIER=SynchedEntityData.defineId(LightningEffect.class,EntityDataSerializers.FLOAT);
    private LightningGeometry geometry;private long geometrySeed;private Vec3 geometryEnd;private int age;
    public LightningEffect(EntityType<? extends LightningEffect> type,Level level){super(type,level);setNoGravity(true);noPhysics=true;}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){b.define(END,new Vector3f());b.define(SEED,0L);b.define(ELEMENT,2);b.define(DURATION,6);b.define(BASE_DURATION,6);b.define(SPEED,4);b.define(AGE,0);b.define(MULTIPLIER,.3f);}
    public static LightningEffect spawn(ServerLevel level,Vec3 start,Vec3 end,int duration,float multiplier,int speed){
        return spawn(level,start,end,duration,multiplier,speed,2);
    }
    public static LightningEffect spawn(ServerLevel level,Vec3 start,Vec3 end,int duration,float multiplier,int speed,int element){
        var bolt=ModEntities.LIGHTNING.create(level,EntitySpawnReason.TRIGGERED);if(bolt==null)return null;
        bolt.setPos(start);Vec3 delta=end.subtract(start);long seed=level.getRandom().nextLong();var random=new java.util.Random(seed);random.nextInt(3);
        bolt.entityData.set(BASE_DURATION,duration);bolt.entityData.set(END,new Vector3f((float)delta.x,(float)delta.y,(float)delta.z));bolt.entityData.set(SEED,seed);bolt.entityData.set(DURATION,duration+random.nextInt(duration)-duration/2);bolt.entityData.set(SPEED,speed);bolt.entityData.set(MULTIPLIER,multiplier);
        bolt.entityData.set(ELEMENT,Math.clamp(element,0,5));bolt.age=-(int)(delta.length()*3);bolt.entityData.set(AGE,bolt.age);level.addFreshEntity(bolt);
        return bolt;
    }
    public int element(){return entityData.get(ELEMENT);}
    /** Original contracted hitboxes and segment endpoints, shared by wand and wisp bolts. */
    public void strike(ServerLevel level,LivingEntity caster,int damage){
        var shape=geometry();
        var bounds=new net.minecraft.world.phys.AABB(position(),position().add(new Vec3(entityData.get(END)))).inflate(shape.length/2);
        for(var victim:level.getEntitiesOfClass(LivingEntity.class,bounds,e->e!=caster&&e.isAlive())){
            var box=victim.getBoundingBox();box=box.deflate(box.getXsize()/2.6,box.getYsize()/2.6,box.getZsize()/2.6);
            for(var segment:shape.segments())if(box.contains(position().add(segment.start.position()))||box.contains(position().add(segment.end.position()))){
                victim.hurtServer(level,level.damageSources().indirectMagic(caster,caster),damage);
                WispEntity.applyBoltEffects(level,victim,element());break;
            }
        }
    }
    public LightningGeometry geometry(){Vec3 end=new Vec3(entityData.get(END));long seed=entityData.get(SEED);if(geometry==null||seed!=geometrySeed||!end.equals(geometryEnd)){geometry=new LightningGeometry(end,seed,entityData.get(MULTIPLIER),entityData.get(BASE_DURATION));geometrySeed=seed;geometryEnd=end;}return geometry;}
    public int age(){return age;}public int duration(){return entityData.get(DURATION);}public int speed(){return entityData.get(SPEED);}
    @Override public boolean hurtServer(ServerLevel level,net.minecraft.world.damagesource.DamageSource source,float damage){return false;}
    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> field){super.onSyncedDataUpdated(field);if(field.equals(AGE))age=entityData.get(AGE);if(field.equals(MULTIPLIER))geometry=null;}
    @Override public void tick(){super.tick();age+=speed();if(!level().isClientSide()&&age>=duration())discard();}
    @Override protected void readAdditionalSaveData(ValueInput input){discard();}
    @Override protected void addAdditionalSaveData(ValueOutput output){}
}
