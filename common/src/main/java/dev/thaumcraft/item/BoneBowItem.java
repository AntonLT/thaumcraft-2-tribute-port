package dev.thaumcraft.item;

import dev.thaumcraft.entity.BoneArrow;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Original ten-tick full draw and automatic release after eighteen ticks. */
public final class BoneBowItem extends BowItem {
    public BoneBowItem(Properties properties) {super(properties);}
    @Override public boolean releaseUsing(ItemStack stack,Level level,LivingEntity owner,int remaining) {
        int held=getUseDuration(stack,owner)-remaining;
        return super.releaseUsing(stack,level,owner,getUseDuration(stack,owner)-held*2);
    }
    @Override public void onUseTick(Level level,LivingEntity owner,ItemStack stack,int remaining) {
        if(!level.isClientSide()&&getUseDuration(stack,owner)-remaining>18)owner.releaseUsingItem();
    }
    @Override protected Projectile createProjectile(Level level,LivingEntity owner,ItemStack weapon,ItemStack ammo,boolean critical) {
        BoneArrow arrow=new BoneArrow(level,owner,ammo,weapon);arrow.setCritArrow(critical);return arrow;
    }
    @Override protected void shootProjectile(LivingEntity shooter,Projectile arrow,int index,float power,float uncertainty,float angle,LivingEntity target) {
        super.shootProjectile(shooter,arrow,index,power*1.25f,uncertainty,angle,target);
    }
}
