package dev.thaumcraft.mixin;

import dev.thaumcraft.world.EldritchIndex;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class WorldGenerationStateMixin {
    @Inject(method="<init>",at=@At("RETURN"))
    private void thaumcraft$prepareGenerationState(CallbackInfo ci){
        EldritchIndex.get((ServerLevel)(Object)this);
        dev.thaumcraft.gameplay.ArcaneWorldData.get((ServerLevel)(Object)this);
    }
}
