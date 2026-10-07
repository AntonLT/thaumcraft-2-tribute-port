package dev.thaumcraft.entity;

import dev.thaumcraft.content.Content;
import dev.thaumcraft.gameplay.ArcaneWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public final class WispEntity extends Monster implements ItemSupplier {
    private static final EntityDataAccessor<Integer> ELEMENT=SynchedEntityData.defineId(WispEntity.class,EntityDataSerializers.INT);
    private static final String[] CRYSTALS={"vis_crystal","vaporous_crystal","aqueous_crystal","earthen_crystal","fiery_crystal","tainted_crystal"};
    private Vec3 destination;
    private int attackCounter,courseChangeCooldown,aggroCooldown;
    public WispEntity(EntityType<? extends WispEntity> type,Level level){super(type,level);setNoGravity(true);xpReward=5;}
    public static AttributeSupplier.Builder attributes(){return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,22).add(Attributes.MOVEMENT_SPEED,0.3).add(Attributes.FOLLOW_RANGE,16).add(Attributes.ATTACK_DAMAGE,1);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){super.defineSynchedData(builder);builder.define(ELEMENT,0);}
    public int element(){return entityData.get(ELEMENT);}
    public void setElement(int element){entityData.set(ELEMENT,Math.clamp(element,0,5));}
    @Override public ItemStack getItem(){return new ItemStack(Content.item(CRYSTALS[element()]));}
    @Override public void aiStep(){super.aiStep();if(level().isClientSide())dev.thaumcraft.content.VisualEffects.entity.accept(this);}
    @Override public boolean fireImmune(){return element()==4||super.fireImmune();}
    @Override public void travel(Vec3 input){travelFlying(input,0.02f);}
    @Override protected void checkFallDamage(double y,boolean onGround,net.minecraft.world.level.block.state.BlockState state,BlockPos pos){}
    @Override public boolean hurtServer(ServerLevel level,DamageSource source,float amount) {
        boolean hurt=super.hurtServer(level,source,amount);
        if(hurt&&source.getEntity() instanceof LivingEntity attacker){setTarget(attacker);aggroCooldown=200;}
        return hurt;
    }
    @Override protected net.minecraft.sounds.SoundEvent getAmbientSound(){return net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP;}
    @Override protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource source){return net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH;}
    @Override protected net.minecraft.sounds.SoundEvent getDeathSound(){return net.minecraft.sounds.SoundEvents.PLAYER_BREATH;}
    @Override protected float getSoundVolume(){return .25f;}
    @Override public int getMaxSpawnClusterSize(){return 1;}
    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor access,DifficultyInstance difficulty,EntitySpawnReason reason,SpawnGroupData group) {
        var cell=ArcaneWorldData.get(access.getLevel()).aura(access.getLevel(),blockPosition());
        var biome=access.getBiome(blockPosition());
        int type=0;
        if(cell.taint()>dev.thaumcraft.PortConfig.auraMax*.5f)type=5;
        else if(cell.vis()<=dev.thaumcraft.PortConfig.auraMax*.5f) {
            if(biome.is(net.minecraft.world.level.biome.Biomes.DESERT)||biome.is(BiomeTags.IS_NETHER)||biome.is(BiomeTags.IS_MOUNTAIN)||nearLava())type=4;
            else if(biome.is(BiomeTags.IS_FOREST)||biome.is(BiomeTags.IS_TAIGA)||biome.is(BiomeTags.IS_JUNGLE))type=3;
            else if(biome.is(BiomeTags.IS_OCEAN)||biome.is(BiomeTags.IS_RIVER)||biome.is(net.minecraft.world.level.biome.Biomes.SWAMP)||biome.is(net.minecraft.world.level.biome.Biomes.MANGROVE_SWAMP)||biome.value().getBaseTemperature()<0.15||access.getLevel().isRaining())type=2;
            else if(biome.is(net.minecraft.world.level.biome.Biomes.PLAINS))type=1;
        }
        setElement(type);
        return super.finalizeSpawn(access,difficulty,reason,group);
    }
    private boolean nearLava() {
        for(BlockPos pos:BlockPos.betweenClosed(blockPosition().offset(-5,-5,-5),blockPosition().offset(5,5,5)))
            if(level().hasChunkAt(pos)&&level().getFluidState(pos).is(FluidTags.LAVA))return true;
        return false;
    }
    @Override protected void customServerAiStep(ServerLevel server) {
        super.customServerAiStep(server);
        var target=getTarget();
        if(target!=null&&(!target.isAlive()||target instanceof net.minecraft.world.entity.player.Player player&&(player.isCreative()||player.isSpectator()))) {setTarget(null);target=null;}
        aggroCooldown--;
        if(element()==5&&(target==null||aggroCooldown--<=0)) {
            target=server.getNearestPlayer(getX(),getY(),getZ(),16,entity->entity instanceof net.minecraft.world.entity.player.Player player&&!player.isCreative()&&!player.isSpectator()&&player.isAlive());
            setTarget(target);
            if(target!=null)aggroCooldown=50;
        }
        if(target==null){
            if(destination==null||position().distanceToSqr(destination)<1||position().distanceToSqr(destination)>3600)destination=randomDestination(16);
        }else if(!hasLineOfSight(target))destination=randomDestination(4);
        else {
            destination=distanceToSqr(target)>108?target.position().add(0,1,0):position();
        }
        if(courseChangeCooldown--<=0){
            courseChangeCooldown+=random.nextInt(5)+2;
            Vec3 direction=destination.subtract(position());
            if(direction.lengthSqr()>1.0e-6&&canTravelTo(destination))setDeltaMovement(getDeltaMovement().add(direction.normalize().scale(.1)));
            else destination=position();
        }
        if(target!=null&&distanceToSqr(target)<144&&hasLineOfSight(target)){
            if(++attackCounter==20) {
                Vec3 end=target.position().add(0,target.getEyeHeight()-.7,0);
                var bolt=LightningEffect.spawn(server,position(),end,3,.4f,4,element());
                if(bolt!=null)bolt.strike(server,this,1);
                server.playSound(null,blockPosition(),dev.thaumcraft.content.ModSounds.event("zap"),net.minecraft.sounds.SoundSource.HOSTILE,1,1.1f);
                attackCounter=-30+random.nextInt(30);
            }
        }else if(attackCounter>0)attackCounter--;
    }
    private Vec3 randomDestination(float radius){return position().add((random.nextFloat()*2-1)*radius,(random.nextFloat()*2-1)*radius,(random.nextFloat()*2-1)*radius);}
    public static void applyBoltEffects(ServerLevel level,LivingEntity target,int element){
        if(element==4){target.igniteForSeconds(2);return;}
        int duration=switch(level.getDifficulty()){case NORMAL->60;case HARD->120;default->0;};
        if(duration==0)return;
        var effect=switch(element){
            case 0->net.minecraft.world.effect.MobEffects.NAUSEA;
            case 2->net.minecraft.world.effect.MobEffects.SLOWNESS;
            case 3->net.minecraft.world.effect.MobEffects.POISON;
            case 5->net.minecraft.world.effect.MobEffects.BLINDNESS;
            default->null;
        };
        if(effect!=null)target.addEffect(new net.minecraft.world.effect.MobEffectInstance(effect,duration*(element==0||element==2?2:1),0));
    }
    private boolean canTravelTo(Vec3 target) {
        BlockPos pos=BlockPos.containing(target);
        if(level().isOutsideBuildHeight(pos)||!level().hasChunkAt(pos)||!level().getFluidState(pos).isEmpty())return false;
        Vec3 step=target.subtract(position()).normalize();
        int length=(int)Math.ceil(target.distanceTo(position()));
        for(int i=1;i<=length;i++) {
            Vec3 offset=step.scale(i);BlockPos next=BlockPos.containing(position().add(offset));
            var box=getBoundingBox().move(offset);
            if(!level().hasChunksAt(BlockPos.containing(box.minX,box.minY,box.minZ),BlockPos.containing(box.maxX,box.maxY,box.maxZ))||!level().noCollision(this,box))return false;
        }
        for(int y=0;y<=10;y++)if(!level().getBlockState(pos.below(y)).isAir())return true;
        return false;
    }
    @Override protected void dropFromLootTable(ServerLevel level,DamageSource source,boolean playerKilled) {
        int looting=LegacyMobLoot.looting(level,source);
        getLootTable().ifPresent(table->dropFromLootTable(level,source,playerKilled,table,stack->{
            for(String crystal:CRYSTALS)if(stack.is(Content.item(crystal))){stack=new ItemStack(Content.item(CRYSTALS[element()]),stack.getCount()+random.nextInt(Math.max(0,looting)+1));break;}
            spawnAtLocation(level,stack);
        }));
    }
    @Override protected void readAdditionalSaveData(ValueInput input){super.readAdditionalSaveData(input);setElement(input.getIntOr("element",0));}
    @Override protected void addAdditionalSaveData(ValueOutput output){super.addAdditionalSaveData(output);output.putInt("element",element());}
}
