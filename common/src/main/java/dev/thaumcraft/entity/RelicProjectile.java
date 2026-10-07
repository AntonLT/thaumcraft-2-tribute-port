package dev.thaumcraft.entity;

import dev.thaumcraft.content.Content;
import dev.thaumcraft.content.TaintBlock;
import dev.thaumcraft.gameplay.ArcaneWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.*;

import java.util.UUID;

public final class RelicProjectile extends Entity implements ItemSupplier {
    private static final EntityDataAccessor<Integer> MODE=SynchedEntityData.defineId(RelicProjectile.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> VISUAL_AGE=SynchedEntityData.defineId(RelicProjectile.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> WATER_LIFE=SynchedEntityData.defineId(RelicProjectile.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> WATER_BLUE=SynchedEntityData.defineId(RelicProjectile.class,EntityDataSerializers.FLOAT);
    private UUID owner;
    private int age;
    private float absorbedVis;
    private boolean exploded;
    private static final java.util.List<BlockPos> SUCTION_BLOCKS=BlockPos.betweenClosedStream(-10,-10,-10,10,10,10)
        .map(BlockPos::immutable).filter(pos->pos.distSqr(BlockPos.ZERO)<=100)
        .sorted(java.util.Comparator.comparingDouble(pos->pos.distSqr(BlockPos.ZERO))).toList();
    public RelicProjectile(EntityType<? extends RelicProjectile> type,Level level){super(type,level);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){builder.define(MODE,0);builder.define(VISUAL_AGE,0);builder.define(WATER_LIFE,60);builder.define(WATER_BLUE,.85f);}
    public int mode(){return entityData.get(MODE);}
    public int visualAge(){return age;}
    public int waterLifetime(){return entityData.get(WATER_LIFE);}
    public float waterBlue(){return entityData.get(WATER_BLUE);}
    public static void waterWand(ServerLevel level,net.minecraft.world.entity.player.Player player){
        var orb=ModEntities.RELIC.create(level,EntitySpawnReason.MOB_SUMMONED);if(orb==null)return;
        var r=level.getRandom();var look=player.getLookAngle();double yaw=Math.toRadians(player.getYRot());
        orb.configure(4,player.getUUID());
        orb.entityData.set(WATER_LIFE,(int)(48/(r.nextDouble()*.3+.7)));orb.entityData.set(WATER_BLUE,.7f+r.nextFloat()*.3f);
        orb.setPos(player.getEyePosition().add(-Math.cos(yaw)*.16,0,-Math.sin(yaw)*.16).add(look.scale(.3)));
        orb.setDeltaMovement(look.scale(.5).add((r.nextFloat()-r.nextFloat())*.033,(r.nextFloat()-r.nextFloat())*.033,(r.nextFloat()-r.nextFloat())*.033));
        level.addFreshEntity(orb);
    }
    @Override public EntityDimensions getDimensions(Pose pose){return mode()==0?EntityDimensions.fixed(.5f,.5f):super.getDimensions(pose);}
    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> field){super.onSyncedDataUpdated(field);if(field.equals(VISUAL_AGE))age=mode()==0?Math.max(age,entityData.get(VISUAL_AGE)):entityData.get(VISUAL_AGE);else if(field.equals(MODE))refreshDimensions();}
    public void configure(int mode,UUID owner){entityData.set(MODE,mode);this.owner=owner;refreshDimensions();}
    @Override public ItemStack getItem(){return new ItemStack(Content.item(switch(entityData.get(MODE)){case 1->"concentrated_vis";case 2->"concentrated_taint";case 3->"potion_of_purity";default->"arcane_singularity";}));}
    @Override public boolean isPickable(){return mode()==0&&!isRemoved();}
    @Override public boolean hurtServer(ServerLevel level,DamageSource source,float damage){if(entityData.get(MODE)==0)age=Math.max(age,60);return false;}
    @Override public void tick() {
        super.tick();age++;
        if(!(level() instanceof ServerLevel level)){
            if(mode()==0&&age<60||mode()==4)dev.thaumcraft.content.VisualEffects.entity.accept(this);
            return;
        }
        if(age%5==0)entityData.set(VISUAL_AGE,age);
        int mode=entityData.get(MODE);
        if(mode==4){waterTick(level);return;}
        if(mode==0 && age>=60) {
            setDeltaMovement(Vec3.ZERO);
            if(!exploded){exploded=true;level.explode(this,getX(),getY(),getZ(),4,Level.ExplosionInteraction.TNT);dev.thaumcraft.content.ModSounds.playAt(level,getX(),getY(),getZ(),"singularity",net.minecraft.sounds.SoundSource.NEUTRAL,2,1);}
            for(BlockPos offset:SUCTION_BLOCKS) {
                BlockPos pos=blockPosition().offset(offset);if(!level.hasChunkAt(pos))continue;
                var state=level.getBlockState(pos);if(state.isAir())continue;
                if(state.getDestroySpeed(level,pos)>=0&&state.getBlock().getExplosionResistance()<100)level.destroyBlock(pos,true,this);
                break;
            }
            for(Entity entity:level.getEntities(this,getBoundingBox().inflate(10),e->e.isAlive() && !(e instanceof RelicProjectile))) {
                Vec3 pull=position().subtract(entity.position());
                entity.setDeltaMovement(entity.getDeltaMovement().add(pull.normalize().scale(Math.max(0,1-pull.length()/10)*0.1)));entity.hurtMarked=true;
                if(pull.lengthSqr()<1.5) {
                    if(entity instanceof net.minecraft.world.entity.item.ItemEntity item){absorbedVis+=dev.thaumcraft.gameplay.GameData.vis(item.getItem())*item.getItem().getCount();item.discard();}
                    else if(entity instanceof LivingEntity living&&living.hurtServer(level,damageSources().magic(),3))absorbedVis++;
                }
            }
            level.sendParticles(ParticleTypes.PORTAL,getX(),getY(),getZ(),8,0.5,0.5,0.5,0.5);
            if(age>=230){ArcaneWorldData.get(level).addVibes(level,blockPosition(),(int)(absorbedVis/3),(int)(absorbedVis/4));discard();}
            return;
        }
        if(mode==0) {
            Vec3 velocity=getDeltaMovement(),before=position();
            move(MoverType.SELF,velocity);
            Vec3 traveled=position().subtract(before);
            setDeltaMovement(
                Math.abs(traveled.x-velocity.x)>1.0e-5?-velocity.x*.25:velocity.x,
                Math.abs(traveled.y-velocity.y)>1.0e-5?-velocity.y*.25:velocity.y-.04,
                Math.abs(traveled.z-velocity.z)>1.0e-5?-velocity.z*.25:velocity.z
            );
            setDeltaMovement(getDeltaMovement().scale(.98));
            return;
        }
        Vec3 start=position(),end=start.add(getDeltaMovement());
        var hit=level.clip(new ClipContext(start,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this));
        Vec3 clipped=hit.getType()==HitResult.Type.MISS?end:hit.getLocation();
        var entityHit=ProjectileUtil.getEntityHitResult(this,start,clipped,getBoundingBox().expandTowards(getDeltaMovement()).inflate(0.5),e->e instanceof LivingEntity && !e.getUUID().equals(owner),start.distanceToSqr(clipped));
        if(hit.getType()!=HitResult.Type.MISS || entityHit!=null) {
            setPos(entityHit!=null?entityHit.getLocation():hit.getLocation());
            splash(level,mode);discard();return;
        }
        setPos(end);setDeltaMovement(getDeltaMovement().add(0,-.05,0).scale(.99));
        if(age>240)discard();
    }
    private void waterTick(ServerLevel level) {
        setDeltaMovement(getDeltaMovement().add(0,-.004,0));
        Vec3 start=position(),end=start.add(getDeltaMovement());
        var block=level.clip(new ClipContext(start,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.ANY,this));
        Vec3 clipped=block.getType()==HitResult.Type.MISS?end:block.getLocation();
        var hit=ProjectileUtil.getEntityHitResult(this,start,clipped,getBoundingBox().expandTowards(getDeltaMovement()).inflate(.15),e->e instanceof LivingEntity&&!(e instanceof net.minecraft.world.entity.player.Player)&&!(e instanceof TamableAnimal)&&!(e instanceof TravelingTrunk),start.distanceToSqr(clipped));
        if(hit!=null) {
            var victim=(LivingEntity)hit.getEntity();var shooter=owner==null?null:level.getEntity(owner);
            float damage=victim.getType()==EntityType.BLAZE||victim.getType()==EntityType.ENDERMAN?5:1;
            if(victim.getType()==EntityType.ENDERMAN){
                for(var goal:((dev.thaumcraft.mixin.MobGoalsAccess)victim).thaumcraft$targetGoals().getAvailableGoals())
                    if(goal.getGoal() instanceof dev.thaumcraft.mixin.EndermanTeleportAccess teleport)teleport.thaumcraft$setTeleportTime(0);
            }
            victim.hurtServer(level,shooter instanceof net.minecraft.world.entity.player.Player player?level.damageSources().playerAttack(player):level.damageSources().magic(),damage);
            victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,80,1));discard();return;
        }
        if(block.getType()!=HitResult.Type.MISS) {
            waterImpact(level,block);discard();return;
        }
        setPos(end);
        if(age>waterLifetime())discard();
    }
    /** Authoritative counterpart of the original water particle's terrain interactions. */
    public static void waterImpact(ServerLevel level,BlockHitResult hit) {
        BlockPos pos=hit.getBlockPos();if(!level.hasChunkAt(pos))return;
        var state=level.getBlockState(pos);var fluid=state.getFluidState();boolean splashed=false;
        if(fluid.is(net.minecraft.tags.FluidTags.LAVA)) {
            if(fluid.isSource())level.setBlockAndUpdate(pos,net.minecraft.world.level.block.Blocks.OBSIDIAN.defaultBlockState());
            else if(fluid.getAmount()>=4)level.setBlockAndUpdate(pos,net.minecraft.world.level.block.Blocks.COBBLESTONE.defaultBlockState());
        } else if(fluid.is(net.minecraft.tags.FluidTags.WATER)&&!state.hasBlockEntity()&&state.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock) {
            level.setBlockAndUpdate(pos,fluid.isSource()?net.minecraft.world.level.block.Blocks.ICE.defaultBlockState():net.minecraft.world.level.block.Blocks.WATER.defaultBlockState().setValue(net.minecraft.world.level.block.LiquidBlock.LEVEL,9));
        } else if(state.getBlock() instanceof net.minecraft.world.level.block.FarmlandBlock) {
            level.setBlockAndUpdate(pos,state.setValue(net.minecraft.world.level.block.FarmlandBlock.MOISTURE,7));
        } else if(state.is(net.minecraft.tags.BlockTags.SAND)) {
            level.setBlockAndUpdate(pos,net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState());
        } else if(!state.is(net.minecraft.world.level.block.Blocks.ICE)) {
            BlockPos outside=pos.relative(hit.getDirection());var adjacent=level.getBlockState(outside);
            if(adjacent.is(net.minecraft.world.level.block.Blocks.FIRE))splashed=level.removeBlock(outside,false);
            else if(adjacent.isAir())splashed=level.setBlockAndUpdate(outside,net.minecraft.world.level.block.Blocks.WATER.defaultBlockState().setValue(net.minecraft.world.level.block.LiquidBlock.LEVEL,9));
        }
        if(fluid.is(net.minecraft.tags.FluidTags.LAVA)){
            level.playSound(null,pos,net.minecraft.sounds.SoundEvents.LAVA_EXTINGUISH,net.minecraft.sounds.SoundSource.BLOCKS,.5f,2.6f+(level.getRandom().nextFloat()-level.getRandom().nextFloat())*.8f);
            level.sendParticles(ParticleTypes.LARGE_SMOKE,pos.getX()+.5,pos.getY()+1.2,pos.getZ()+.5,8,.5,0,.5,0);
        }else if(fluid.isEmpty()&&(state.getBlock() instanceof net.minecraft.world.level.block.FarmlandBlock||state.is(net.minecraft.tags.BlockTags.SAND))){
            level.sendParticles(ParticleTypes.SPLASH,pos.getX()+.5,pos.getY()+1,pos.getZ()+.5,1,.5,0,.5,0);
        }else if(splashed){
            level.playSound(null,pos.relative(hit.getDirection()),net.minecraft.sounds.SoundEvents.GENERIC_SPLASH,net.minecraft.sounds.SoundSource.BLOCKS,.3f,1+(level.getRandom().nextFloat()-level.getRandom().nextFloat())*.4f);
        }
    }
    private void splash(ServerLevel level,int mode) {
        var data=ArcaneWorldData.get(level);
        // The original truncates impact coordinates, including at negative positions.
        BlockPos center=new BlockPos((int)getX(),(int)getY(),(int)getZ());
        if(mode==1)data.changeAura(level,center,150,0);
        else data.addVibes(level,center,mode==3?25:0,mode==2?25:0);
        int radius=mode==2?3:4;
        if(mode==2||mode==3)for(int x=-radius;x<=radius;x++)for(int y=-radius;y<=radius;y++)for(int z=-radius;z<=radius;z++) {
            BlockPos pos=center.offset(x,y,z);
            if(!TaintBlock.canAccess(level,pos)||position().distanceToSqr(Vec3.atLowerCornerOf(pos))>radius*radius)continue;
            if(mode==2)TaintBlock.increase(level,pos,data.aura(level,pos).taint(),level.getRandom());
            else TaintBlock.purifyPotion(level,pos);
        }
        // Original PotionHelper colors for metadata 0, 1 and 2, respectively.
        int color=switch(mode){case 2->13458603;case 3->8171462;default->3694022;};
        level.levelEvent(2002,new BlockPos((int)Math.round(getX()),(int)Math.round(getY()),(int)Math.round(getZ())),color);
    }
    @Override protected void readAdditionalSaveData(ValueInput input){entityData.set(MODE,input.getIntOr("mode",0));age=input.getIntOr("age",0);entityData.set(WATER_LIFE,input.getIntOr("water_life",60));entityData.set(WATER_BLUE,input.getFloatOr("water_blue",.85f));entityData.set(VISUAL_AGE,age);absorbedVis=input.getFloatOr("absorbedVis",0);exploded=input.getBooleanOr("exploded",false);String id=input.getStringOr("owner","");try{owner=id.isEmpty()?null:UUID.fromString(id);}catch(IllegalArgumentException ignored){owner=null;}}
    @Override protected void addAdditionalSaveData(ValueOutput output){output.putInt("mode",entityData.get(MODE));output.putInt("age",age);output.putInt("water_life",waterLifetime());output.putFloat("water_blue",waterBlue());output.putFloat("absorbedVis",absorbedVis);output.putBoolean("exploded",exploded);if(owner!=null)output.putString("owner",owner.toString());}
}
