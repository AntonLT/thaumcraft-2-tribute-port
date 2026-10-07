package dev.thaumcraft.mixin;

import dev.thaumcraft.machine.MachineBlock;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntityRenderer.class)
public abstract class CrucibleItemAnimationMixin {
    @Unique private final java.util.Set<ItemEntity> thaumcraft$boiledItems=java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());

    @Inject(method="extractRenderState(Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;F)V",at=@At("TAIL"))
    private void thaumcraft$boilingClock(ItemEntity item,ItemEntityRenderState state,float partialTick,CallbackInfo callback) {
        if(item.getAge()<=1&&item.level().getBlockState(item.blockPosition()).getBlock() instanceof MachineBlock block
                &&block.id().contains("crucible")&&!block.id().equals("crucible_of_souls"))thaumcraft$boiledItems.add(item);
        // Original RenderItem used age, which boiling resets. Keep that clock after ejection too.
        if(thaumcraft$boiledItems.contains(item))state.ageInTicks=item.getAge()+partialTick;
    }
}
