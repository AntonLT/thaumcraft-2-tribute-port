package dev.thaumcraft.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class SkeletonAlly extends Skeleton {
    private int life=1200;
    public SkeletonAlly(EntityType<? extends SkeletonAlly> type,Level level){
        super(type,level);setPersistenceRequired();xpReward=0;
        setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BOW));
        setDropChance(net.minecraft.world.entity.EquipmentSlot.MAINHAND,0);
    }
    @Override protected void registerGoals() {
        goalSelector.addGoal(1,new FloatGoal(this));
        goalSelector.addGoal(2,new RestrictSunGoal(this));
        goalSelector.addGoal(3,new FleeSunGoal(this,1));
        goalSelector.addGoal(4,new Goal(){
            private net.minecraft.world.entity.LivingEntity target;
            private int attackDelay,visibleTicks;
            {setFlags(java.util.EnumSet.of(Flag.MOVE,Flag.LOOK));}
            @Override public boolean canUse(){if(getTarget()==null||!getTarget().isAlive())return false;target=getTarget();return true;}
            @Override public boolean canContinueToUse(){return canUse()||target!=null&&target.isAlive()&&!getNavigation().isDone();}
            @Override public void stop(){target=null;}
            @Override public boolean requiresUpdateEveryTick(){return true;}
            @Override public void tick(){
                boolean visible=getSensing().hasLineOfSight(target);visibleTicks=visible?visibleTicks+1:0;
                double distance=distanceToSqr(target.getX(),target.getBoundingBox().minY,target.getZ());
                if(distance<=100&&visibleTicks>=20)getNavigation().stop();else getNavigation().moveTo(target,1);
                getLookControl().setLookAt(target,30,30);
                if(attackDelay>0)attackDelay--;
                if(attackDelay==0&&distance<=100&&visible){performRangedAttack(target,1);attackDelay=40;}
            }
        });
        goalSelector.addGoal(6,new LookAtPlayerGoal(this,Monster.class,8));
        goalSelector.addGoal(6,new RandomLookAroundGoal(this));
        targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,net.minecraft.world.entity.Mob.class,0,true,false,(living,server)->living instanceof net.minecraft.world.entity.monster.Enemy&&!(living instanceof SkeletonAlly)));
    }
    // AbstractSkeleton normally switches to strafing bow AI, or melee when summoned without a bow.
    @Override public void reassessWeaponGoal(){}
    @Override public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(net.minecraft.world.level.ServerLevelAccessor level,net.minecraft.world.DifficultyInstance difficulty,net.minecraft.world.entity.EntitySpawnReason reason,net.minecraft.world.entity.SpawnGroupData group){return group;}
    @Override protected void doFreezeConversion(){setFreezeConverting(false);}
    @Override public void performRangedAttack(net.minecraft.world.entity.LivingEntity target,float power){
        if(!(level() instanceof ServerLevel level))return;
        var arrow=new net.minecraft.world.entity.projectile.arrow.Arrow(level,this,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ARROW),getMainHandItem());
        double x=target.getX()-getX(),z=target.getZ()-getZ(),y=target.getEyeY()-.7-arrow.getY();
        arrow.shoot(x,y+Math.sqrt(x*x+z*z)*.2,z,1.6f,12);
        level.addFreshEntity(arrow);playSound(net.minecraft.sounds.SoundEvents.SKELETON_SHOOT,1,1/(random.nextFloat()*.4f+.8f));
    }
    public void setLifetime(int ticks){life=Math.max(1,ticks);}
    @Override public void aiStep() {
        // The original ally burns for two seconds; modern Mob's daylight tag always uses eight.
        if(level() instanceof ServerLevel&&isAlive()&&level().environmentAttributes().getValue(net.minecraft.world.attribute.EnvironmentAttributes.MONSTERS_BURN,position())){
            float brightness=getLightLevelDependentMagicValue();
            if(brightness>.5f&&level().canSeeSky(blockPosition())&&random.nextFloat()*30<(brightness-.4f)*2)igniteForSeconds(2);
        }
        super.aiStep();
        if(level() instanceof ServerLevel server) {
            if(--life<=0){
                server.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF,getX(),getY()+getBbHeight()/2,getZ(),20,.3,.5,.3,.02);
                playSound(net.minecraft.sounds.SoundEvents.SKELETON_DEATH,1,1);discard();
            }
        }
    }
    @Override protected void addAdditionalSaveData(ValueOutput output){super.addAdditionalSaveData(output);output.putInt("ally_life",life);}
    @Override protected void readAdditionalSaveData(ValueInput input){super.readAdditionalSaveData(input);life=input.getIntOr("ally_life",1200);}
}
