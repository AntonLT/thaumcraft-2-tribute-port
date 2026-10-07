package dev.thaumcraft.mixin;

import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Transient item state used by crucibles and the arcane bore. */
@Mixin(ItemEntity.class)
public interface ItemEntityAccess {
    @Accessor("age") void thaumcraft$setAge(int age);
    @Accessor("thaumcraft$boreAttracted") void thaumcraft$setBoreAttracted(boolean attracted);
}
