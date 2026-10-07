package dev.thaumcraft.neoforge;

import dev.thaumcraft.api.IntegrationHooks;
import dev.thaumcraft.content.Content;
import dev.thaumcraft.machine.MachineBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.furnace.FurnaceFuelBurnTimeEvent;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import java.util.function.Predicate;

/** Loader fuel handling, energy reception for the shared generator bridge, and transactional item access. */
final class NeoForgeIntegration {
    private NeoForgeIntegration() {}
    static void register(){
        NeoForge.EVENT_BUS.addListener(NeoForgeIntegration::fuelBurnTime);
        IntegrationHooks.registerEnergy((level,pos,face,offered)->{
            var receiver=level.getCapability(Capabilities.Energy.BLOCK,pos,face);
            if(receiver==null)return 0;
            try(var transaction=Transaction.openRoot()){
                int accepted=receiver.insert(offered,transaction);
                if(accepted<0||accepted>offered)throw new IllegalStateException("Energy handler accepted an invalid amount");
                if(accepted>0)transaction.commit();
                return accepted;
            }
        });
        // Containers keep the built-in rules; this reaches capability-only inventories such as storage-network interfaces.
        IntegrationHooks.registerInventory((level,pos,face)->{
            if(level.getBlockEntity(pos) instanceof Container)return null;
            var handler=level.getCapability(Capabilities.Item.BLOCK,pos,face);
            return handler==null?null:new HandlerAccess(handler);
        });
    }

    /**
     * Machine faces through the loader item API, with the same slot, face and reservation rules as hoppers. Unsided
     * access would reach upgrades, and void interfaces delegate to linked chests the wrapper cannot journal.
     */
    static ResourceHandler<ItemResource> items(MachineBlockEntity machine,Direction side){
        if(side==null||machine.machineId().equals("void_interface")||machine.getSlotsForFace(side).length==0)return null;
        return new WorldlyContainerWrapper(machine,side);
    }

    private record HandlerAccess(ResourceHandler<ItemResource> handler) implements IntegrationHooks.ItemAccess {
        @Override public int insert(ItemStack offered,boolean simulate){
            if(offered.isEmpty())return 0;
            try(var transaction=Transaction.openRoot()){
                int accepted=ResourceHandlerUtil.insertStacking(handler,ItemResource.of(offered),offered.getCount(),transaction);
                if(!simulate)transaction.commit();
                return accepted;
            }
        }
        @Override public int insertWhole(ItemStack offered){
            if(offered.isEmpty())return 0;
            try(var transaction=Transaction.openRoot()){
                int accepted=ResourceHandlerUtil.insertStacking(handler,ItemResource.of(offered),offered.getCount(),transaction);
                if(accepted!=offered.getCount())return 0;
                transaction.commit();
                return accepted;
            }
        }
        @Override public ItemStack extract(Predicate<ItemStack> filter,int maximum,boolean simulate){
            if(maximum<=0)return ItemStack.EMPTY;
            try(var transaction=Transaction.openRoot()){
                for(int index=0;index<handler.size();index++){
                    var resource=handler.getResource(index);
                    if(resource.isEmpty()||!filter.test(resource.toStack()))continue;
                    int extracted=handler.extract(index,resource,Math.min(maximum,resource.getMaxStackSize()),transaction);
                    if(extracted<=0)continue;
                    if(!simulate)transaction.commit();
                    return resource.toStack(extracted);
                }
                return ItemStack.EMPTY;
            }
        }
    }

    private static void fuelBurnTime(FurnaceFuelBurnTimeEvent event){
        // The generic logs data map may load after this mod's explicit entries.
        var stack=event.getItemStack();
        if(stack.is(Content.item("alumentum")))event.setBurnTime(16000);
        else if(stack.is(Content.item("silverwood_log")))event.setBurnTime(600);
        else if(stack.is(Content.item("greatwood_log")))event.setBurnTime(400);
    }
}
