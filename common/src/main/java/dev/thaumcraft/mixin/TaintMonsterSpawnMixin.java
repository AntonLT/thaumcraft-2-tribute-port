package dev.thaumcraft.mixin;

import dev.thaumcraft.content.TaintBlock;
import dev.thaumcraft.entity.ModSpawns;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Taint glows at light 2 like 1.2.5, which modern zero-block-light monster rules would turn into a spawn ward. Near taint, monsters use the 1.2.5 light rule instead. */
@Mixin(Monster.class)
public abstract class TaintMonsterSpawnMixin {
    @Inject(method="isDarkEnoughToSpawn",at=@At("HEAD"),cancellable=true)
    private static void thaumcraft$taintGlow(ServerLevelAccessor level,BlockPos pos,RandomSource random,CallbackInfoReturnable<Boolean> callback) {
        int light=level.getBrightness(LightLayer.BLOCK,pos);
        if(light==0||light>2||light<=level.dimensionType().monsterSpawnBlockLightLimit())return;
        for(Direction direction:Direction.values())
            if(level.getBlockState(pos.relative(direction)).getBlock() instanceof TaintBlock) {callback.setReturnValue(ModSpawns.darkEnough(level,pos,random));return;}
    }
}
