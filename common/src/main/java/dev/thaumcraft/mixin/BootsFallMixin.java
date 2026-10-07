package dev.thaumcraft.mixin;

import dev.thaumcraft.content.Content;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class BootsFallMixin {
    private String thaumcraft$boots() {
        if(!((Object)this instanceof net.minecraft.world.entity.player.Player player)||player.isPassenger())return "";
        var entry=Content.entry(dev.thaumcraft.gameplay.ArcaneEnchantments.wornBoots(player));return entry==null?"":entry.source_class();
    }
    @org.spongepowered.asm.mixin.injection.Inject(method="getJumpPower(F)F",at=@At("RETURN"),cancellable=true)
    private void thaumcraft$jumpPower(float factor,org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Float> callback) {
        if(!((Object)this instanceof net.minecraft.world.entity.player.Player player)||player.isShiftKeyDown())return;
        float extra=switch(thaumcraft$boots()){case "ItemStridingBoots"->.3f;case "ItemSevenBoots"->.75f;case "ItemStompBoots"->.8f;default->0;};
        float cap=switch(thaumcraft$boots()){case "ItemStridingBoots"->.85f;case "ItemSevenBoots"->1.55f;default->1.6f;};
        if(extra>0)callback.setReturnValue(Math.min(cap,callback.getReturnValue()+extra));
    }
    @org.spongepowered.asm.mixin.injection.Inject(method="jumpFromGround",at=@At("TAIL"))
    private void thaumcraft$rememberJump(org.spongepowered.asm.mixin.injection.callback.CallbackInfo callback) {
        if(!((Object)this instanceof net.minecraft.world.entity.player.Player player))return;
        String type=thaumcraft$boots();
        float exhaustion=switch(type){case "ItemStridingBoots"->.1f;case "ItemSevenBoots","ItemStompBoots"->.15f;default->0;};
        if(type.equals("ItemStompBoots")){
            var boots=dev.thaumcraft.gameplay.ArcaneEnchantments.wornBoots(player);
            dev.thaumcraft.item.ItemState.setInt(boots,"stomp_jump",player.isShiftKeyDown()?0:1);
            dev.thaumcraft.item.ItemState.setInt(boots,"stomp_fire",0);
        }
        if(exhaustion>0&&!player.isShiftKeyDown()&&player instanceof ServerPlayer)player.causeFoodExhaustion(exhaustion);
    }
    @org.spongepowered.asm.mixin.injection.Inject(method="getFrictionInfluencedSpeed",at=@At("RETURN"),cancellable=true)
    private void thaumcraft$groundSpeed(float friction,org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Float> callback) {
        if(!((Object)this instanceof net.minecraft.world.entity.player.Player player))return;
        float airSpeed=switch(thaumcraft$boots()){case "ItemStridingBoots"->.04f;case "ItemSevenBoots","ItemStompBoots"->.075f;default->0;};
        if(airSpeed>0)callback.setReturnValue(player.onGround()?callback.getReturnValue()*1.5f:airSpeed);
    }
    @ModifyVariable(method="causeFallDamage(DFLnet/minecraft/world/damagesource/DamageSource;)Z",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private double thaumcraft$enchantedBoots(double distance) {
        if(!((Object)this instanceof ServerPlayer player))return distance;
        var boots=dev.thaumcraft.gameplay.ArcaneEnchantments.wornBoots(player);var entry=Content.entry(boots);
        if(entry==null)return distance;
        String type=entry.source_class();
        if(type.equals("ItemStompBoots")&&dev.thaumcraft.item.ItemState.getInt(boots,"stomp_jump",0)>0&&distance>3&&player.isShiftKeyDown()&&!player.getCooldowns().isOnCooldown(boots)) {
            var level=player.level();
            level.explode(player,player.getX(),player.getY(),player.getZ(),2,false,Level.ExplosionInteraction.MOB);
            for(var target:level.getEntitiesOfClass(LivingEntity.class,player.getBoundingBox().inflate(3),e->e!=player))target.igniteForSeconds(3);
            boots.hurtAndBreak(1,player,EquipmentSlot.FEET);player.getCooldowns().addCooldown(boots,50);
        }
        return switch(type) {case "ItemStridingBoots"->distance/2;case "ItemSevenBoots","ItemStompBoots"->distance/3;default->distance;};
    }
}
