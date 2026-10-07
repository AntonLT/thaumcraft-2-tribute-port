package dev.thaumcraft.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public final class BoneArrow extends AbstractArrow {
    public BoneArrow(EntityType<? extends BoneArrow> type,Level level) {super(type,level);}
    public BoneArrow(Level level,LivingEntity owner,ItemStack ammo,ItemStack bow) {super(ModEntities.BONE_ARROW,owner,level,ammo,bow);}
    @Override protected ItemStack getDefaultPickupItem() {return new ItemStack(Items.ARROW);}
    @Override protected void doPostHurtEffects(LivingEntity target) {
        super.doPostHurtEffects(target);
        dev.thaumcraft.gameplay.ArcaneEnchantments.markBone(target,getOwner());
    }
}
