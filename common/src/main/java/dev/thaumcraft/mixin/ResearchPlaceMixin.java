package dev.thaumcraft.mixin;

import dev.thaumcraft.gameplay.ResearchGate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Recipe-book placement must not move ingredients for locked research; slotChanged would only clear the result. */
@Mixin(AbstractCraftingMenu.class)
public abstract class ResearchPlaceMixin {
    @Inject(method = "handlePlacement", at = @At("HEAD"), cancellable = true)
    private void thaumcraft$checkPlacement(boolean craftSingle, boolean creative, RecipeHolder<?> recipe,
            ServerLevel level, Inventory inventory,
            CallbackInfoReturnable<RecipeBookMenu.PostPlaceAction> cir) {
        if (inventory.player instanceof ServerPlayer player
                && ResearchGate.locked(player, recipe.id().identifier())) {
            cir.setReturnValue(RecipeBookMenu.PostPlaceAction.NOTHING);
        }
    }
}
