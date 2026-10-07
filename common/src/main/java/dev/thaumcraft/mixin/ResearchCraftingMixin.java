package dev.thaumcraft.mixin;

import dev.thaumcraft.gameplay.ResearchGate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.RecipeCraftingHolder;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RecipeCraftingHolder.class)
public interface ResearchCraftingMixin {
    @Inject(method="setRecipeUsed(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/item/crafting/RecipeHolder;)Z",at=@At("HEAD"),cancellable=true)
    private void thaumcraft$checkResearch(ServerPlayer player,RecipeHolder<?> recipe,CallbackInfoReturnable<Boolean> cir) {
        if(ResearchGate.locked(player,recipe.id().identifier()))cir.setReturnValue(false);
    }
}
