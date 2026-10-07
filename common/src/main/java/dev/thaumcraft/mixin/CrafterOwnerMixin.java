package dev.thaumcraft.mixin;

import dev.thaumcraft.gameplay.CrafterResearchOwner;
import net.minecraft.world.level.block.entity.CrafterBlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.UUID;

@Mixin(CrafterBlockEntity.class)
public abstract class CrafterOwnerMixin implements CrafterResearchOwner {
    @Unique private UUID thaumcraft$owner;
    @Override public UUID thaumcraft$getOwner(){return thaumcraft$owner;}
    @Override public void thaumcraft$setOwner(UUID owner){thaumcraft$owner=owner;((CrafterBlockEntity)(Object)this).setChanged();}
    @Inject(method="loadAdditional",at=@At("TAIL"))
    private void thaumcraft$loadOwner(ValueInput input,CallbackInfo ci){
        thaumcraft$owner=null;
        String owner=input.getStringOr("thaumcraft2tp:owner","");
        if(!owner.isEmpty())try{thaumcraft$owner=UUID.fromString(owner);}catch(IllegalArgumentException ignored){}
    }
    @Inject(method="saveAdditional",at=@At("TAIL"))
    private void thaumcraft$saveOwner(ValueOutput output,CallbackInfo ci){
        if(thaumcraft$owner!=null)output.putString("thaumcraft2tp:owner",thaumcraft$owner.toString());
    }
}
