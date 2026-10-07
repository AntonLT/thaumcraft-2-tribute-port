package dev.thaumcraft.entity;

import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;

/** 1.2.5 collide-attack timing and width-based reach, using the modern pathfinder. */
final class LegacyMeleeGoal extends Goal {
    private final PathfinderMob mob;
    private final Class<? extends LivingEntity> targetType;
    private final double speed;
    private final boolean pursue;
    private LivingEntity target;
    private Path path;
    private int attackDelay,pathDelay;
    LegacyMeleeGoal(PathfinderMob mob,Class<? extends LivingEntity> targetType,double speed,boolean pursue){
        this.mob=mob;this.targetType=targetType;this.speed=speed;this.pursue=pursue;
        setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));
    }
    @Override public boolean canUse(){
        target=mob.getTarget();
        if(!targetType.isInstance(target)||!target.isAlive())return false;
        path=mob.getNavigation().createPath(target,0);return path!=null;
    }
    @Override public boolean canContinueToUse(){return target!=null&&target==mob.getTarget()&&target.isAlive()&&(pursue?mob.isWithinHome(target.blockPosition()):!mob.getNavigation().isDone());}
    @Override public void start(){mob.getNavigation().moveTo(path,speed);mob.setAggressive(true);pathDelay=0;}
    @Override public void stop(){target=null;mob.getNavigation().stop();mob.setAggressive(false);}
    @Override public boolean requiresUpdateEveryTick(){return true;}
    @Override public void tick(){
        mob.getLookControl().setLookAt(target,30,30);
        if((pursue||mob.getSensing().hasLineOfSight(target))&&--pathDelay<=0){
            pathDelay=4+mob.getRandom().nextInt(7);mob.getNavigation().moveTo(target,speed);
        }
        if(attackDelay>0)attackDelay--;
        double reach=mob.getBbWidth()*2;
        if(attackDelay==0&&mob.distanceToSqr(target.getX(),target.getBoundingBox().minY,target.getZ())<=reach*reach){
            attackDelay=20;mob.doHurtTarget(getServerLevel(mob),target);
        }
    }
}
