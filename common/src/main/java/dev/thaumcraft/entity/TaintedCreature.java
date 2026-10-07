package dev.thaumcraft.entity;

import dev.thaumcraft.content.TaintBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Tainted livestock are hostile creatures, as in the original mod. */
public final class TaintedCreature extends Monster {
    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> SHEARED=net.minecraft.network.syncher.SynchedEntityData.defineId(TaintedCreature.class,net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);
    private int grazeTicks;
    private float wing,oldWing,wingHeight,oldWingHeight,wingSpeed=1;
    @Override protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder){super.defineSynchedData(builder);builder.define(SHEARED,false);}
    public boolean isSheared(){return entityData.get(SHEARED);}
    public float wingRotation(float partial){return ((float)Math.sin(net.minecraft.util.Mth.lerp(partial,oldWing,wing))+1)*net.minecraft.util.Mth.lerp(partial,oldWingHeight,wingHeight);}
    public float grazePosition(float partial){return grazeTicks<=0?0:grazeTicks>=4&&grazeTicks<=36?1:grazeTicks<4?(grazeTicks-partial)/4:-(grazeTicks-40-partial)/4;}
    public float grazeAngle(float partial){return grazeTicks>4&&grazeTicks<=36?.62831855f+.2199115f*(float)Math.sin((grazeTicks-4-partial)/32*28.7f):grazeTicks>0?.62831855f:getXRot()/(180/(float)Math.PI);}
    @Override public void handleEntityEvent(byte event){if(event==10)grazeTicks=40;else super.handleEntityEvent(event);}
    @Override public void aiStep(){
        super.aiStep();if(grazeTicks>0)grazeTicks--;
        if(kind().equals("tainted_chicken")) {
            oldWing=wing;oldWingHeight=wingHeight;wingHeight=Math.clamp(wingHeight+(onGround()?-1:4)*.3f,0,1);
            if(!onGround()&&wingSpeed<1)wingSpeed=1;wingSpeed*=.9f;wing+=wingSpeed*2;
        }
        if(level().isClientSide()&&random.nextInt(5)==0)dev.thaumcraft.content.VisualEffects.entity.accept(this);
    }
    @Override public net.minecraft.world.InteractionResult mobInteract(Player player,net.minecraft.world.InteractionHand hand){
        var stack=player.getItemInHand(hand);
        if(kind().equals("tainted_sheep")&&!isSheared()&&stack.is(net.minecraft.world.item.Items.SHEARS)) {
            if(level() instanceof ServerLevel server){entityData.set(SHEARED,true);spawnAtLocation(server,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.PURPLE_WOOL,1+random.nextInt(3)));stack.hurtAndBreak(1,player,hand.asEquipmentSlot());}
            return net.minecraft.world.InteractionResult.SUCCESS;
        }
        return super.mobInteract(player,hand);
    }
    @Override protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output){super.addAdditionalSaveData(output);output.putBoolean("Sheared",isSheared());}
    @Override protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input){super.readAdditionalSaveData(input);entityData.set(SHEARED,input.getBooleanOr("Sheared",false));}
    public TaintedCreature(EntityType<? extends TaintedCreature> type,Level level) {
        super(type,level);
        if(!kind().equals("tainted_cow"))setPersistenceRequired();
    }
    @Override public void playAmbientSound(){
        if(kind().equals("tainted_villager")&&level() instanceof ServerLevel server&&dev.thaumcraft.PortConfig.taintSpread){
            var data=dev.thaumcraft.gameplay.ArcaneWorldData.get(server);
            data.addVibes(server,blockPosition(),0,random.nextInt(4));
            TaintBlock.increase(server,blockPosition(),data.aura(server,blockPosition()).taint(),random);
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.WITCH,getX(),getY()+getBbHeight()/2,getZ(),50,1,1.5,1,.1);
        }
        super.playAmbientSound();
    }
    @Override protected net.minecraft.sounds.SoundEvent getAmbientSound(){return switch(kind()){
        case "tainted_cow"->registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.COW_SOUND_VARIANT).getOrThrow(net.minecraft.world.entity.animal.cow.CowSoundVariants.CLASSIC).value().ambientSound().value();
        case "tainted_pig"->registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.PIG_SOUND_VARIANT).getOrThrow(net.minecraft.world.entity.animal.pig.PigSoundVariants.CLASSIC).value().adultSounds().ambientSound().value();
        case "tainted_chicken"->registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.CHICKEN_SOUND_VARIANT).getOrThrow(net.minecraft.world.entity.animal.chicken.ChickenSoundVariants.CLASSIC).value().adultSounds().ambientSound().value();
        case "tainted_sheep"->net.minecraft.sounds.SoundEvents.SHEEP_AMBIENT;
        default->net.minecraft.sounds.SoundEvents.VILLAGER_AMBIENT;
    };}
    @Override protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source){return switch(kind()){
        case "tainted_cow"->registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.COW_SOUND_VARIANT).getOrThrow(net.minecraft.world.entity.animal.cow.CowSoundVariants.CLASSIC).value().hurtSound().value();
        case "tainted_pig"->registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.PIG_SOUND_VARIANT).getOrThrow(net.minecraft.world.entity.animal.pig.PigSoundVariants.CLASSIC).value().adultSounds().hurtSound().value();
        case "tainted_chicken"->registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.CHICKEN_SOUND_VARIANT).getOrThrow(net.minecraft.world.entity.animal.chicken.ChickenSoundVariants.CLASSIC).value().adultSounds().hurtSound().value();
        case "tainted_sheep"->net.minecraft.sounds.SoundEvents.SHEEP_HURT;
        default->net.minecraft.sounds.SoundEvents.VILLAGER_HURT;
    };}
    @Override protected net.minecraft.sounds.SoundEvent getDeathSound(){return switch(kind()){
        case "tainted_cow"->registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.COW_SOUND_VARIANT).getOrThrow(net.minecraft.world.entity.animal.cow.CowSoundVariants.CLASSIC).value().deathSound().value();
        case "tainted_pig"->registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.PIG_SOUND_VARIANT).getOrThrow(net.minecraft.world.entity.animal.pig.PigSoundVariants.CLASSIC).value().adultSounds().deathSound().value();
        case "tainted_chicken"->registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.CHICKEN_SOUND_VARIANT).getOrThrow(net.minecraft.world.entity.animal.chicken.ChickenSoundVariants.CLASSIC).value().adultSounds().deathSound().value();
        case "tainted_sheep"->net.minecraft.sounds.SoundEvents.SHEEP_DEATH;
        default->net.minecraft.sounds.SoundEvents.VILLAGER_DEATH;
    };}
    @Override protected float getSoundVolume(){return kind().equals("tainted_cow")?.4f:super.getSoundVolume();}
    @Override protected void registerGoals() {
        goalSelector.addGoal(0,new FloatGoal(this));
        goalSelector.addGoal(2,new Goal() {
            {setFlags(java.util.EnumSet.of(Flag.MOVE,Flag.LOOK,Flag.JUMP));}
            @Override public boolean canUse(){return kind().equals("tainted_sheep")&&random.nextInt(250)==0&&(level().getBlockState(blockPosition().below()).is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)||level().getBlockState(blockPosition()).is(net.minecraft.world.level.block.Blocks.SHORT_GRASS));}
            @Override public boolean canContinueToUse(){return grazeTicks>0;}
            @Override public void start(){grazeTicks=40;level().broadcastEntityEvent(TaintedCreature.this,(byte)10);getNavigation().stop();}
            @Override public void stop(){grazeTicks=0;}
            @Override public boolean requiresUpdateEveryTick(){return true;}
            @Override public void tick(){if(grazeTicks==4&&level() instanceof ServerLevel server){if(level().getBlockState(blockPosition()).is(net.minecraft.world.level.block.Blocks.SHORT_GRASS))server.destroyBlock(blockPosition(),false);TaintBlock.taint(server,blockPosition().below());}}
        });
        boolean sheep=kind().equals("tainted_sheep"),chicken=kind().equals("tainted_chicken"),villager=kind().equals("tainted_villager");
        // Old path speeds were absolute; modern goals multiply the .25 movement attribute.
        double attackSpeed=sheep?1.4:1.6;
        goalSelector.addGoal(sheep?3:2,attackGoal(Player.class,attackSpeed,false));
        if(chicken)goalSelector.addGoal(2,new LeapAtTargetGoal(this,.3f));
        if(kind().equals("tainted_villager")){
            if(getNavigation() instanceof net.minecraft.world.entity.ai.navigation.GroundPathNavigation navigation)navigation.setCanOpenDoors(true);
            goalSelector.addGoal(3,new OpenDoorGoal(this,true));
            goalSelector.addGoal(4,new MoveThroughVillageGoal(this,1.2,true,4,()->true));
            goalSelector.addGoal(5,new MoveBackToVillageGoal(this,1.2,false));
        }
        if(!villager)goalSelector.addGoal(3,attackGoal(Villager.class,attackSpeed,true));
        if(!villager&&!sheep)goalSelector.addGoal(chicken?3:8,attackGoal(Animal.class,attackSpeed,chicken));
        goalSelector.addGoal(sheep?6:chicken?3:villager?9:5,new WaterAvoidingRandomStrollGoal(this,sheep?.92:chicken||villager?1.2:.8));
        goalSelector.addGoal(6,new LookAtPlayerGoal(this,Player.class,6));
        goalSelector.addGoal(7,new RandomLookAroundGoal(this));
        targetSelector.addGoal(0,new HurtByTargetGoal(this));
        targetSelector.addGoal(sheep?3:2,new NearestAttackableTargetGoal<>(this,Player.class,0,true,false,null));
        if(!villager){
            targetSelector.addGoal(sheep?3:2,new NearestAttackableTargetGoal<>(this,Villager.class,0,false,false,null));
            if(!sheep)targetSelector.addGoal(chicken?3:8,new NearestAttackableTargetGoal<>(this,Animal.class,0,false,false,null));
        }
    }
    private Goal attackGoal(Class<? extends net.minecraft.world.entity.LivingEntity> target,double speed,boolean pursue){
        return new LegacyMeleeGoal(this,target,speed,pursue);
    }
    public String kind() {return BuiltInRegistries.ENTITY_TYPE.getKey(getType()).getPath();}
    @Override protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if(kind().equals("tainted_chicken")&&getDeltaMovement().y<0) {setDeltaMovement(getDeltaMovement().multiply(1,0.6,1));resetFallDistance();}
    }
}
