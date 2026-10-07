package dev.thaumcraft.fabric;

import dev.thaumcraft.api.IntegrationHooks;
import dev.thaumcraft.content.Content;
import dev.thaumcraft.machine.MachineBlockEntity;
import net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import java.util.function.Predicate;

/** Transactional item access through the Fabric transfer API, matching the NeoForge item capability. */
final class FabricIntegration {
    private FabricIntegration() {}
    static void register(){
        ItemStorage.SIDED.registerForBlockEntity((machine,side)->items(machine,side),Content.MACHINE_ENTITY);
        // Containers keep the built-in rules; this reaches storage-only inventories such as storage-network interfaces.
        IntegrationHooks.registerInventory((level,pos,face)->{
            if(level.getBlockEntity(pos) instanceof Container)return null;
            var storage=ItemStorage.SIDED.find(level,pos,face);
            return storage==null?null:new StorageAccess(storage);
        });
    }

    /**
     * Machine faces with the same slot, face and reservation rules as hoppers. Unsided access would reach upgrades, and
     * void interfaces delegate to linked chests the wrapper cannot journal. Fabric's container fallback would expose both,
     * so refused faces answer with an empty storage rather than null.
     */
    private static Storage<ItemVariant> items(MachineBlockEntity machine,Direction side){
        if(side==null||machine.machineId().equals("void_interface")||machine.getSlotsForFace(side).length==0)return Storage.empty();
        return ContainerStorage.of(machine,side);
    }

    private record StorageAccess(Storage<ItemVariant> storage) implements IntegrationHooks.ItemAccess {
        @Override public int insert(ItemStack offered,boolean simulate){
            if(offered.isEmpty())return 0;
            try(var transaction=Transaction.openOuter()){
                int accepted=(int)storage.insert(ItemVariant.of(offered),offered.getCount(),transaction);
                if(!simulate)transaction.commit();
                return accepted;
            }
        }
        @Override public int insertWhole(ItemStack offered){
            if(offered.isEmpty())return 0;
            try(var transaction=Transaction.openOuter()){
                int accepted=(int)storage.insert(ItemVariant.of(offered),offered.getCount(),transaction);
                if(accepted!=offered.getCount())return 0;
                transaction.commit();
                return accepted;
            }
        }
        @Override public ItemStack extract(Predicate<ItemStack> filter,int maximum,boolean simulate){
            if(maximum<=0)return ItemStack.EMPTY;
            try(var transaction=Transaction.openOuter()){
                for(var view:storage.nonEmptyViews()){
                    var variant=view.getResource();
                    if(!filter.test(variant.toStack()))continue;
                    int extracted=(int)storage.extract(variant,Math.min(maximum,variant.getItem().getDefaultMaxStackSize()),transaction);
                    if(extracted<=0)continue;
                    if(!simulate)transaction.commit();
                    return variant.toStack(extracted);
                }
                return ItemStack.EMPTY;
            }
        }
    }
}
