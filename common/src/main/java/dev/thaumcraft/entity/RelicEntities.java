package dev.thaumcraft.entity;

import dev.thaumcraft.content.Content;
import dev.thaumcraft.item.ArcanaItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;

public final class RelicEntities {
    private RelicEntities() {}
    public static InteractionResult use(ServerLevel level,ServerPlayer player,InteractionHand hand,ItemStack stack,Content.Entry entry) {
        String cls=entry.source_class();
        if(cls.equals("ItemCarpet")) {
            if(stack.getDamageValue()>=600)return InteractionResult.FAIL;
            var hit=player.pick(5,0,true);if(!(hit instanceof net.minecraft.world.phys.BlockHitResult blockHit)||hit.getType()==net.minecraft.world.phys.HitResult.Type.MISS)return InteractionResult.PASS;
            var eye=player.getEyePosition();
            for(var entity:level.getEntities(player,player.getBoundingBox().expandTowards(player.getLookAngle().scale(5)).inflate(1)))
                if(entity.isPickable()&&entity.getBoundingBox().inflate(entity.getPickRadius()).contains(eye))return InteractionResult.FAIL;
            CarpetEntity carpet=ModEntities.CARPET.create(level,EntitySpawnReason.SPAWN_ITEM_USE);
            if(carpet==null)return InteractionResult.FAIL;
            var ground=blockHit.getBlockPos();
            if(level.getBlockState(ground).is(net.minecraft.world.level.block.Blocks.SNOW))ground=ground.below();
            carpet.setPos(ground.above().getBottomCenter());carpet.setEndurance(600-stack.getDamageValue());
            if(!level.addFreshEntity(carpet))return InteractionResult.FAIL;
            stack.shrink(1);return InteractionResult.SUCCESS;
        }
        if(cls.equals("ItemSingularity") || cls.equals("ItemCustomPotion")) {
            RelicProjectile relic=ModEntities.RELIC.create(level,EntitySpawnReason.SPAWN_ITEM_USE);
            if(relic==null)return InteractionResult.FAIL;
            relic.configure(cls.equals("ItemSingularity")?0:entry.meta()+1,player.getUUID());
            if(cls.equals("ItemSingularity")){
                double yaw=Math.toRadians(player.getYRot()),pitch=Math.toRadians(player.getXRot());
                double headingX=-Math.sin(yaw),headingZ=Math.cos(yaw);
                relic.setPos(player.getX()+headingX*.8,player.getY(),player.getZ()+headingZ*.8);
                relic.setDeltaMovement(headingX*Math.cos(pitch)*.75,-Math.sin(pitch)*.5,headingZ*Math.cos(pitch)*.75);
            } else {
                relic.setPos(player.getEyePosition().add(player.getLookAngle().scale(0.3)));
                double pitch=Math.toRadians(player.getXRot()-20),yaw=Math.toRadians(player.getYRot());
                relic.setDeltaMovement(-Math.sin(yaw)*Math.cos(pitch)*.5,-Math.sin(pitch)*.5,Math.cos(yaw)*Math.cos(pitch)*.5);
            }
            if(!level.addFreshEntity(relic))return InteractionResult.FAIL;
            if(cls.equals("ItemCustomPotion"))level.playSound(null,player.getX(),player.getY(),player.getZ(),net.minecraft.sounds.SoundEvents.ARROW_SHOOT,net.minecraft.sounds.SoundSource.PLAYERS,.5f,.4f/(level.getRandom().nextFloat()*.4f+.8f));
            stack.shrink(1);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
    public static InteractionResult useOn(ServerLevel level,Player player,UseOnContext context,Content.Entry entry) {
        if(entry.source_class().equals("ItemWandBone")) {
            if(context.getClickedFace()!=net.minecraft.core.Direction.UP)return InteractionResult.PASS;
            SkeletonAlly ally=ModEntities.SKELETON_ALLY.create(level,EntitySpawnReason.MOB_SUMMONED);
            if(ally==null)return InteractionResult.FAIL;
            ally.setPos(context.getClickedPos().above().getBottomCenter());
            ally.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.BOW));
            if(!level.addFreshEntity(ally))return InteractionResult.FAIL;
            ArcanaItem.charge(context.getItemInHand(),player,context.getHand(),1);
            level.broadcastEntityEvent(ally,(byte)20);
            dev.thaumcraft.content.ModSounds.playAt(level,ally.getX(),ally.getY(),ally.getZ(),"zap",net.minecraft.sounds.SoundSource.NEUTRAL,1,1);
            level.playSound(null,ally.blockPosition(),net.minecraft.sounds.SoundEvents.SKELETON_HURT,net.minecraft.sounds.SoundSource.NEUTRAL,1,1);
            return InteractionResult.SUCCESS;
        }
        if(entry.id().equals("traveling_trunk")) {
            var trunk=ModEntities.TRUNK.create(level,EntitySpawnReason.SPAWN_ITEM_USE);
            if(trunk==null)return InteractionResult.FAIL;
            trunk.setPos(context.getClickedPos().above().getBottomCenter());trunk.setYRot(level.getRandom().nextFloat()*360);trunk.setOwner(player.getUUID());
            if(!level.noCollision(trunk))return InteractionResult.FAIL;
            level.addFreshEntity(trunk);context.getItemInHand().consume(1,player);return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
