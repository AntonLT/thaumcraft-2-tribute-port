package dev.thaumcraft.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/** Keeps the original adult, unarmed zombie encounter without later zombie spawn mutations. */
public final class BrainyZombie extends Zombie {
    public BrainyZombie(EntityType<? extends Zombie> type,Level level){super(type,level);setCanBreakDoors(true);}
    @Override protected void registerGoals(){
        goalSelector.addGoal(0,new FloatGoal(this));
        goalSelector.addGoal(2,new LegacyMeleeGoal(this,Player.class,1,false));
        goalSelector.addGoal(3,new LegacyMeleeGoal(this,Villager.class,1,true));
        goalSelector.addGoal(4,new MoveTowardsRestrictionGoal(this,1));
        goalSelector.addGoal(5,new MoveThroughVillageGoal(this,1,false,4,this::canBreakDoors));
        goalSelector.addGoal(6,new WaterAvoidingRandomStrollGoal(this,1));
        goalSelector.addGoal(7,new LookAtPlayerGoal(this,Player.class,8));
        goalSelector.addGoal(7,new RandomLookAroundGoal(this));
        targetSelector.addGoal(1,new HurtByTargetGoal(this));
        targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,0,true,false,null));
        targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Villager.class,0,false,false,null));
    }
    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor level,DifficultyInstance difficulty,EntitySpawnReason reason,SpawnGroupData group){
        // Zombie.finalizeSpawn adds babies, jockeys, equipment and random health/reinforcement bonuses.
        setCanPickUpLoot(false);setBaby(false);setCanBreakDoors(true);
        return group;
    }
    @Override protected boolean convertsInWater(){return false;}
    @Override public boolean isUnderWaterConverting(){return false;}
    @Override public boolean killedEntity(ServerLevel level,LivingEntity victim,DamageSource source){return true;}
    @Override public boolean doHurtTarget(ServerLevel level,Entity target){
        // Burning zombies did not ignite their victims in 1.2.5.
        boolean hurt=target.hurtServer(level,damageSources().mobAttack(this),(float)getAttributeValue(Attributes.ATTACK_DAMAGE));
        if(hurt)setLastHurtMob(target);
        return hurt;
    }
}
