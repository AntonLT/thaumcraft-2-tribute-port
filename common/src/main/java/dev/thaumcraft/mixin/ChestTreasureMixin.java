package dev.thaumcraft.mixin;

import dev.thaumcraft.gameplay.ArtifactLoot;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LootTable.class)
public abstract class ChestTreasureMixin {
    @Inject(method="fill",at=@At("RETURN"))
    private void thaumcraft$fillTreasure(Container container,LootParams params,long seed,CallbackInfo ci){
        if(container instanceof ChestBlockEntity)
            ArtifactLoot.fillOriginalTreasure(container,seed==0?params.getLevel().getRandom():RandomSource.create(seed));
    }
}
