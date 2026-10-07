package dev.thaumcraft.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.level.Level;

public final class Grub extends Silverfish {
    public Grub(EntityType<? extends Silverfish> type,Level level){super(type,level);xpReward=0;}
    @Override public boolean isWithinMeleeAttackRange(net.minecraft.world.entity.LivingEntity target){return distanceToSqr(target)<4&&target.getBoundingBox().maxY>getBoundingBox().minY&&target.getBoundingBox().minY<getBoundingBox().maxY;}
    @Override public void aiStep(){if(tickCount<10)resetFallDistance();super.aiStep();}
}
