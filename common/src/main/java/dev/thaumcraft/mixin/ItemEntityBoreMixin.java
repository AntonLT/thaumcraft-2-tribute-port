package dev.thaumcraft.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.objectweb.asm.Opcodes;

/** TileBore's temporary fireResistance=50 delays ignition; it does not prevent fire damage. */
@Mixin(ItemEntity.class)
public abstract class ItemEntityBoreMixin extends Entity {
    @Unique private boolean thaumcraft$boreAttracted;

    protected ItemEntityBoreMixin(EntityType<?> type,Level level){super(type,level);}

    @Override protected int getFireImmuneTicks(){return thaumcraft$boreAttracted?50:super.getFireImmuneTicks();}

    // ItemEntity recalculates noPhysics every tick. Keep suction's wall traversal through movement.
    @Redirect(method="tick",at=@At(value="FIELD",target="Lnet/minecraft/world/entity/item/ItemEntity;noPhysics:Z",opcode=Opcodes.PUTFIELD))
    private void thaumcraft$keepBoreMovement(ItemEntity item,boolean value){item.noPhysics=value||thaumcraft$boreAttracted;}

    @Redirect(method="tick",at=@At(value="FIELD",target="Lnet/minecraft/world/entity/item/ItemEntity;noPhysics:Z",opcode=Opcodes.GETFIELD))
    private boolean thaumcraft$skipBorePushOut(ItemEntity item){return !thaumcraft$boreAttracted&&item.noPhysics;}
}
