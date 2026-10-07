package dev.thaumcraft.mixin;

import dev.thaumcraft.gameplay.ArcaneEnchantments;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.stream.Stream;

@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentMenuMixin {
    @Unique private ServerPlayer thaumcraft$player;

    @Inject(method="<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",at=@At("RETURN"))
    private void thaumcraft$rememberPlayer(int id,Inventory inventory,ContainerLevelAccess access,CallbackInfo ci) {
        if(inventory.player instanceof ServerPlayer player)thaumcraft$player=player;
    }

    @ModifyArg(method="getEnchantmentList",at=@At(value="INVOKE",target="Lnet/minecraft/world/item/enchantment/EnchantmentHelper;selectEnchantment(Lnet/minecraft/util/RandomSource;Lnet/minecraft/world/item/ItemStack;ILjava/util/stream/Stream;)Ljava/util/List;"),index=3)
    private Stream<Holder<Enchantment>> thaumcraft$filterResearch(Stream<Holder<Enchantment>> candidates) {
        return thaumcraft$player==null?candidates:candidates.filter(enchantment->ArcaneEnchantments.available(thaumcraft$player,enchantment));
    }
}
