package dev.thaumcraft.api;

import dev.thaumcraft.machine.MachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.function.Predicate;

/** Register adapters during mod initialization. Mutations run on the server; water conversion is also queried by clients. */
public final class IntegrationHooks {
    private IntegrationHooks() {}
    @FunctionalInterface public interface Registration extends AutoCloseable {@Override void close();}
    public enum CropAction {GROW,HYDRATE,HARVEST}
    public enum BackpackCategory {MINER,FORESTER,BUILDER}
    /** Exact source Forestry categories, expressed against the modern registered item identities. */
    public static boolean backpackAllows(BackpackCategory category,ItemStack stack){
        var entry=dev.thaumcraft.content.Content.entry(stack);if(entry==null)return false;
        String legacy=entry.legacy();int meta=entry.meta();
        return switch(category){
            case MINER -> legacy.endsWith("blockCustomOre")&&meta==0||legacy.endsWith("itemComponents")&&java.util.Set.of(0,6,11,12).contains(meta)||legacy.endsWith("itemCrystals")&&meta>=0&&meta<6;
            case FORESTER -> legacy.endsWith("itemComponents")&&meta==2||legacy.endsWith("itemPlants")&&meta>=0&&meta<5||legacy.endsWith("blockCustomWood")&&meta>=0&&meta<15||legacy.endsWith("blockCustomLeaves")&&meta>=0&&meta<3;
            case BUILDER -> legacy.endsWith("blockAppStone")&&meta==5;
        };
    }
    /** Optional quarry adapters can honor the original softBlocks registration without numeric block IDs. */
    public static boolean softForQuarry(net.minecraft.world.level.block.state.BlockState state){return state.getBlock() instanceof dev.thaumcraft.world.EldritchBlock;}
    /** Apply one action only when this adapter owns the crop; report true only after changing it. */
    @FunctionalInterface public interface CropAdapter {boolean apply(ServerLevel level,BlockPos pos,CropAction action);}
    /** Insert never mutates the offered stack. Simulation must not change the destination. */
    public interface ItemAccess {
        int insert(ItemStack offered,boolean simulate);
        ItemStack extract(Predicate<ItemStack> filter,int maximum,boolean simulate);
        /** Whole-stack delivery; slotless adapters retain their own atomic capacity rules. */
        default int insertWhole(ItemStack offered){return insert(offered.copy(),true)>=offered.getCount()?insert(offered.copy(),false):0;}
    }
    @FunctionalInterface public interface InventoryAdapter {ItemAccess find(ServerLevel level,BlockPos pos,Direction receivingFace);}
    /** Atomically accept 0..offered internal generator units; conversion belongs to the adapter. */
    @FunctionalInterface public interface EnergyBridge {int receive(ServerLevel level,BlockPos pos,Direction receivingFace,int offered);}
    private static final List<CropAdapter> CROPS=new CopyOnWriteArrayList<>();
    private static final List<InventoryAdapter> INVENTORIES=new CopyOnWriteArrayList<>();
    private static final List<Function<ItemStack,ItemStack>> WATER=new CopyOnWriteArrayList<>();
    private static final List<EnergyBridge> ENERGY=new CopyOnWriteArrayList<>();
    private static final List<Function<Player,List<ItemStack>>> WORN=new CopyOnWriteArrayList<>();
    private static <T> Registration add(List<T> list,T adapter){java.util.Objects.requireNonNull(adapter);list.add(adapter);return ()->list.remove(adapter);}
    public static Registration registerCrop(CropAdapter adapter){return add(CROPS,adapter);}
    public static boolean hasCropAdapters(){return !CROPS.isEmpty();}
    public static Registration registerInventory(InventoryAdapter adapter){return add(INVENTORIES,adapter);}
    /** Pure conversion of one empty container; return EMPTY when unsupported, preserving relevant components. */
    public static Registration registerWaterContainer(Function<ItemStack,ItemStack> adapter){return add(WATER,adapter);}
    public static Registration registerEnergy(EnergyBridge adapter){return add(ENERGY,adapter);}
    /** Stacks a player wears outside the vanilla armor slots, such as slot-mod accessories. Queried by clients and servers; stacks are read, never copied, so the item may keep its own state. */
    public static Registration registerWornItems(Function<Player,List<ItemStack>> provider){return add(WORN,provider);}
    /** The first worn stack matching {@code filter}: the helmet slot, then registered providers. */
    public static ItemStack worn(Player player,Predicate<ItemStack> filter){
        var head=player.getItemBySlot(EquipmentSlot.HEAD);if(filter.test(head))return head;
        for(var provider:WORN)for(var stack:provider.apply(player))if(filter.test(stack))return stack;
        return ItemStack.EMPTY;
    }
    public static boolean crop(ServerLevel level,BlockPos pos,CropAction action){
        if(!level.hasChunkAt(pos))return false;
        for(var adapter:CROPS)if(adapter.apply(level,pos,action))return true;
        return false;
    }
    public static ItemStack fillWater(ItemStack empty){
        if(empty.isEmpty())return ItemStack.EMPTY;
        for(var adapter:WATER){ItemStack filled=adapter.apply(empty.copyWithCount(1));if(filled!=null&&!filled.isEmpty())return filled.copyWithCount(1);}
        if(empty.is(Items.BUCKET))return new ItemStack(Items.WATER_BUCKET);
        if(empty.is(Items.GLASS_BOTTLE))return PotionContents.createItemStack(Items.POTION,Potions.WATER);
        return ItemStack.EMPTY;
    }
    public static ItemAccess inventory(ServerLevel level,BlockPos pos,Direction face){
        if(!level.hasChunkAt(pos))return null;
        for(var adapter:INVENTORIES){ItemAccess access=adapter.find(level,pos,face);if(access!=null)return access;}
        if(!(level.getBlockEntity(pos) instanceof Container container))return null;
        // Like hoppers, use a double chest's combined inventory; loader adapters leave vanilla containers to this path.
        if(container instanceof net.minecraft.world.level.block.entity.ChestBlockEntity&&level.getBlockState(pos).getBlock() instanceof net.minecraft.world.level.block.ChestBlock chest){
            var combined=net.minecraft.world.level.block.ChestBlock.getContainer(chest,level.getBlockState(pos),level,pos,true);
            if(combined!=null)container=combined;
        }
        return new ContainerAccess(container,face);
    }
    public static ItemStack insert(ServerLevel level,BlockPos pos,Direction face,ItemStack stack){
        var access=inventory(level,pos,face);if(access==null||stack.isEmpty())return stack.copy();
        int accepted=access.insert(stack.copy(),false);
        if(accepted<0||accepted>stack.getCount())throw new IllegalStateException("Inventory adapter accepted an invalid item count");
        return stack.copyWithCount(stack.getCount()-accepted);
    }
    public static ItemStack insertWhole(ServerLevel level,BlockPos pos,Direction face,ItemStack stack){
        var access=inventory(level,pos,face);if(access==null||stack.isEmpty())return stack.copy();
        int accepted=access.insertWhole(stack.copy());
        if(accepted<0||accepted>stack.getCount())throw new IllegalStateException("Inventory adapter accepted an invalid item count");
        return stack.copyWithCount(stack.getCount()-accepted);
    }
    public static ItemStack extract(ServerLevel level,BlockPos pos,Direction face,Predicate<ItemStack> filter,int maximum){
        if(maximum<=0)return ItemStack.EMPTY;
        var access=inventory(level,pos,face);if(access==null)return ItemStack.EMPTY;
        ItemStack result=access.extract(filter,maximum,false);
        if(result==null||result.getCount()>maximum||!result.isEmpty()&&!filter.test(result.copy()))throw new IllegalStateException("Inventory adapter extracted an invalid stack");
        return result;
    }
    /** Called by Thaumic Generators each tick to offer energy to registered bridges. Not part of the addon API. */
    @org.jetbrains.annotations.ApiStatus.Internal
    public static void pushGenerator(ServerLevel level,MachineBlockEntity machine){
        for(Direction face:Direction.values()){
            BlockPos target=machine.getBlockPos().relative(face);if(!level.hasChunkAt(target))continue;
            for(var bridge:ENERGY){
                int offered=machine.generatorOutput.available(Integer.MAX_VALUE);if(offered==0)return;
                int accepted=bridge.receive(level,target,face.getOpposite(),offered);
                if(accepted<0||accepted>offered)throw new IllegalStateException("Energy adapter accepted an invalid amount");
                if(accepted>0){machine.generatorOutput.extract(accepted);machine.generatorOutput.committed();machine.generatorOutput.transferred(target,accepted);break;}
            }
        }
    }
    private record ContainerAccess(Container container,Direction face) implements ItemAccess {
        private int[] slots(){return container instanceof WorldlyContainer sided?sided.getSlotsForFace(face):java.util.stream.IntStream.range(0,container.getContainerSize()).toArray();}
        @Override public int insertWhole(ItemStack offered){
            for(int slot:slots()){
                if(!container.canPlaceItem(slot,offered)||container instanceof WorldlyContainer sided&&!sided.canPlaceItemThroughFace(slot,offered,face))continue;
                ItemStack current=container.getItem(slot);
                if(!current.isEmpty()&&!ItemStack.isSameItemSameComponents(current,offered))continue;
                if(current.getCount()+offered.getCount()>Math.min(container.getMaxStackSize(),offered.getMaxStackSize()))continue;
                container.setItem(slot,offered.copyWithCount(current.getCount()+offered.getCount()));container.setChanged();return offered.getCount();
            }
            return 0;
        }
        @Override public int insert(ItemStack offered,boolean simulate){
            int remaining=offered.getCount();
            for(int slot:slots()){
                if(!container.canPlaceItem(slot,offered)||container instanceof WorldlyContainer sided&&!sided.canPlaceItemThroughFace(slot,offered,face))continue;
                ItemStack current=container.getItem(slot);
                if(!current.isEmpty()&&!ItemStack.isSameItemSameComponents(current,offered))continue;
                int moved=Math.min(remaining,Math.max(0,Math.min(container.getMaxStackSize(),offered.getMaxStackSize())-current.getCount()));
                if(moved==0)continue;
                if(!simulate)container.setItem(slot,offered.copyWithCount(current.getCount()+moved));
                remaining-=moved;if(remaining==0)break;
            }
            if(!simulate&&remaining<offered.getCount())container.setChanged();
            return offered.getCount()-remaining;
        }
        @Override public ItemStack extract(Predicate<ItemStack> filter,int maximum,boolean simulate){
            if(maximum<=0)return ItemStack.EMPTY;
            for(int slot:slots()){
                ItemStack current=container.getItem(slot);
                if(current.isEmpty()||!filter.test(current.copy())||container instanceof WorldlyContainer sided&&!sided.canTakeItemThroughFace(slot,current,face))continue;
                int amount=Math.min(maximum,current.getCount());
                if(simulate)return current.copyWithCount(amount);
                ItemStack result=container.removeItem(slot,amount);container.setChanged();return result;
            }
            return ItemStack.EMPTY;
        }
    }
}
