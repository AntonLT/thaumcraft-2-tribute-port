package dev.thaumcraft.mixin;

import dev.thaumcraft.gameplay.CrafterResearchOwner;
import dev.thaumcraft.gameplay.ResearchGate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CrafterBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.Optional;

@Mixin(CrafterBlock.class)
public abstract class CrafterResearchMixin {
    @Inject(method="setPlacedBy",at=@At("TAIL"))
    private void thaumcraft$assignOwner(Level level,BlockPos pos,BlockState state,LivingEntity placer,ItemStack stack,CallbackInfo ci){
        if(level instanceof ServerLevel&&level.getBlockEntity(pos) instanceof CrafterResearchOwner owner)
            owner.thaumcraft$setOwner(placer instanceof Player player?player.getUUID():null);
    }
    @Redirect(method="dispenseFrom",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/block/CrafterBlock;getPotentialResults(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/crafting/CraftingInput;)Ljava/util/Optional;"))
    private Optional<RecipeHolder<CraftingRecipe>> thaumcraft$authorizeRecipe(ServerLevel level,CraftingInput input,BlockState state,ServerLevel positionedLevel,BlockPos pos){
        var selected=CrafterBlock.getPotentialResults(level,input);
        if(selected.isEmpty())return selected;
        var owner=level.getBlockEntity(pos) instanceof CrafterResearchOwner crafter?crafter.thaumcraft$getOwner():null;
        // Returning no recipe takes vanilla's failure event before assembly or inventory mutation.
        return ResearchGate.locked(level,owner,selected.get().id().identifier())?Optional.empty():selected;
    }
}
