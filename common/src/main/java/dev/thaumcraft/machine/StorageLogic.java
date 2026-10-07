package dev.thaumcraft.machine;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

public final class StorageLogic {
    private StorageLogic() {}
    public static void tick(ServerLevel level,MachineBlockEntity machine) {
        if(machine.machineId().equals("void_interface")){dev.thaumcraft.gameplay.VoidNetworks.get(level).register(level,machine.getBlockPos());dev.thaumcraft.world.ChunkAnchors.register(level,machine.getBlockPos());return;}
        if(machine.machineId().equals("void_chest")){machine.getItem(0);return;}
        if(!machine.machineId().equals("traveling_trunk"))return;
        for(ItemEntity entity:level.getEntitiesOfClass(ItemEntity.class,new AABB(machine.getBlockPos()).inflate(3))) {
            if(entity.hasPickUpDelay())continue;
            ItemStack stack=entity.getItem();
            for(int i=0;i<machine.getContainerSize() && !stack.isEmpty();i++) {
                ItemStack existing=machine.getItem(i);
                if(existing.isEmpty()){machine.setItem(i,stack.copy());stack.setCount(0);}
                else if(ItemStack.isSameItemSameComponents(existing,stack)) {
                    int moved=Math.min(stack.getCount(),existing.getMaxStackSize()-existing.getCount());
                    if(moved>0){existing.grow(moved);stack.shrink(moved);machine.setChanged();}
                }
            }
            if(stack.isEmpty())entity.discard();else entity.setItem(stack);
        }
    }
}
