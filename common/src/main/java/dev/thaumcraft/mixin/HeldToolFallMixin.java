package dev.thaumcraft.mixin;

import dev.thaumcraft.content.Content;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class HeldToolFallMixin {
    // Modern item inventory ticks are server-only; falling must also affect local movement.
    @Inject(method="aiStep",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/player/Inventory;tick()V",shift=At.Shift.AFTER))
    private void thaumcraft$slowFall(CallbackInfo callback) {
        Player player=(Player)(Object)this;
        if(player.getDeltaMovement().y>=0)return;
        var entry=Content.entry(player.getMainHandItem());
        if(entry!=null&&(entry.source_class().equals("ItemElementalSwordAir")||entry.source_class().equals("ItemElementalCutter"))) {
            player.fallDistance*=.75;
            player.setDeltaMovement(player.getDeltaMovement().multiply(1,.9,1));
        }
    }
}
