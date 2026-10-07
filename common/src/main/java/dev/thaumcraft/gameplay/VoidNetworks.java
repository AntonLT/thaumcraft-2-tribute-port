package dev.thaumcraft.gameplay;

import com.mojang.serialization.Codec;
import dev.thaumcraft.Thaumcraft;
import dev.thaumcraft.machine.MachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.*;

/** Loaded interfaces link their owner's chests across dimensions. The saved map retains legacy inventories until migrated. */
public final class VoidNetworks extends SavedData {
    private static final Codec<VoidNetworks> CODEC=Codec.unboundedMap(Codec.STRING,ItemStack.OPTIONAL_CODEC.listOf()).xmap(VoidNetworks::new,VoidNetworks::encode);
    private static final SavedDataType<VoidNetworks> TYPE=new SavedDataType<>(Thaumcraft.id("void_networks"),VoidNetworks::new,CODEC,null);
    private final Map<String,NonNullList<ItemStack>> networks=new HashMap<>();
    private final Map<String,Set<BlockPos>> interfaces=new HashMap<>();
    private int revision;
    public VoidNetworks() {}
    private VoidNetworks(Map<String,List<ItemStack>> saved) {
        saved.forEach((key,stacks)->{
            var inventory=NonNullList.withSize(27,ItemStack.EMPTY);
            for(int i=0;i<Math.min(27,stacks.size());i++)inventory.set(i,stacks.get(i));
            networks.put(key,inventory);
        });
    }
    private Map<String,List<ItemStack>> encode() {
        Map<String,List<ItemStack>> saved=new HashMap<>();networks.forEach((key,inventory)->saved.put(key,new ArrayList<>(inventory)));return saved;
    }
    public static VoidNetworks get(ServerLevel level) {return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);}
    public NonNullList<ItemStack> inventory(UUID owner,int channel) {
        String key=owner+":"+Math.clamp(channel,0,5);
        return networks.computeIfAbsent(key,ignored->{setDirty();return NonNullList.withSize(27,ItemStack.EMPTY);});
    }
    public int revision() {return revision;}
    public void register(ServerLevel level,BlockPos pos) {
        if(interfaces.computeIfAbsent(level.dimension().identifier().toString(),ignored->new HashSet<>()).add(pos.immutable()))revision++;
    }
    public void invalidate() {revision++;}
    public void remove(ServerLevel level,BlockPos pos) {
        var locations=interfaces.get(level.dimension().identifier().toString());
        if(locations!=null&&locations.remove(pos))revision++;
    }
    public List<MachineBlockEntity> linkedChests(ServerLevel level,MachineBlockEntity source) {
        register(level,source.getBlockPos());
        var result=new ArrayList<MachineBlockEntity>();
        for(ServerLevel targetLevel:level.getServer().getAllLevels()) {
            var locations=interfaces.get(targetLevel.dimension().identifier().toString());
            if(locations==null)continue;
            for(BlockPos pos:locations) {
                if(!targetLevel.hasChunkAt(pos)||!(targetLevel.getBlockEntity(pos) instanceof MachineBlockEntity portal)||!portal.machineId().equals("void_interface"))continue;
                if(portal.channel()!=source.channel())continue;
                // Unowned world-generation interfaces expose only their own local chest.
                if(portal!=source&&(source.owner()==null||!source.owner().equals(portal.owner())))continue;
                if(targetLevel.getBlockEntity(pos.below()) instanceof MachineBlockEntity chest&&chest.machineId().equals("void_chest"))result.add(chest);
            }
        }
        result.sort(Comparator.comparing((MachineBlockEntity chest)->chest.getLevel()!=level)
                .thenComparing(chest->chest.getLevel().dimension().identifier().toString())
                .thenComparingDouble(chest->chest.getBlockPos().distSqr(source.getBlockPos()))
                .thenComparingLong(chest->chest.getBlockPos().asLong()));
        return List.copyOf(result);
    }
    /** Move, never copy, old channel contents into the first available local chest. */
    public boolean migrate(UUID owner,int channel,NonNullList<ItemStack> destination) {
        // Runs on every void chest slot access; only saves from before per-chest storage hold legacy inventories.
        if(owner==null||networks.isEmpty())return false;
        String key=owner+":"+Math.clamp(channel,0,5);
        var legacy=networks.get(key);if(legacy==null)return false;
        boolean moved=false;
        for(int old=0;old<legacy.size();old++) {
            ItemStack stack=legacy.get(old);
            for(int slot=0;slot<destination.size()&&!stack.isEmpty();slot++) {
                ItemStack existing=destination.get(slot);
                if(existing.isEmpty()){destination.set(slot,stack.copy());legacy.set(old,ItemStack.EMPTY);moved=true;break;}
                if(ItemStack.isSameItemSameComponents(existing,stack)) {
                    int amount=Math.min(stack.getCount(),existing.getMaxStackSize()-existing.getCount());
                    if(amount>0){existing.grow(amount);stack.shrink(amount);moved=true;}
                }
            }
        }
        if(legacy.stream().allMatch(ItemStack::isEmpty))networks.remove(key);
        if(moved)setDirty();
        return moved;
    }
}
