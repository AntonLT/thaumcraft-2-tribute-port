package dev.thaumcraft.api;

import dev.thaumcraft.machine.MachineBlockEntity;
import dev.thaumcraft.machine.MachineLayout;
import dev.thaumcraft.machine.MachineLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Whole-batch infusion for storage-network bridges. A bridge reserves an empty infuser with one complete batch; the
 * infuser then runs only that recipe with its ordinary vis cost, speed, upgrades and redstone pause, and no player,
 * container or loader path may touch its input or output slots until the bridge finishes the reservation.
 *
 * <p>Every method must run on the server thread. Each stack exists in exactly one place: the caller owns a batch until
 * {@link #reserve} succeeds, the infuser owns it while reserved, and {@link #finish} hands back what remains.
 */
public final class ThaumcraftInfusers {
    private ThaumcraftInfusers() {}

    /** A loaded thaumic or dark infuser. {@code owner} is the research authority; null denies locked recipes. */
    public record Infuser(boolean dark,UUID owner,int inputSlots) {}

    public enum Status {
        /** The infuser's chunk is not loaded; ask again later. */
        UNLOADED,
        /** No infuser holds a reservation with this token: it was broken, replaced or already finished. */
        NONE,
        /** Inputs are reserved and the infuser is working or waiting for vis or redstone. */
        RUNNING,
        /** The pinned recipe can no longer run for the owner: removed, changed by a reload, research lost or vetoed. */
        BLOCKED,
        /** The infusion completed; {@link #finish} returns its actual outputs. */
        DONE
    }

    public static Optional<Infuser> find(ServerLevel level,BlockPos pos){
        var machine=infuser(level,pos);
        return machine==null?Optional.empty():Optional.of(new Infuser(machine.machineId().equals("dark_infuser"),machine.owner(),MachineLogic.infusionSlots(machine)));
    }

    /**
     * The recipe this infuser would run if it held exactly {@code units}, one item per input slot, by its ordinary
     * catalog order and owner research. Repeated ingredients need one unit each. Nothing changes.
     */
    public static Optional<InfusionRecipe> match(ServerLevel level,BlockPos pos,List<ItemStack> units){
        var machine=infuser(level,pos);
        if(machine==null||!validUnits(machine,units))return Optional.empty();
        var recipe=MachineLogic.selectInfusion(level,machine,placed(machine,units),null);
        return recipe==null?Optional.empty():ThaumcraftApi.server().infusion(recipe.key());
    }

    /**
     * Places every unit, one per input slot, and pins {@code recipe}, or changes nothing. Fails while the infuser is
     * reserved, holds any input or output, or when the batch would not select exactly {@code recipe}.
     */
    public static boolean reserve(ServerLevel level,BlockPos pos,UUID token,Identifier recipe,List<ItemStack> units,boolean simulate){
        var machine=infuser(level,pos);
        if(machine==null||token==null||machine.reservation()!=null||!validUnits(machine,units))return false;
        for(int slot=0;slot<outputEnd(machine);slot++)if(!machine.getItem(slot).isEmpty())return false;
        for(int slot=0;slot<units.size();slot++)if(!machine.canPlaceItem(slot,units.get(slot)))return false;
        var selected=MachineLogic.selectInfusion(level,machine,placed(machine,units),null);
        if(selected==null||!selected.key().equals(recipe))return false;
        if(simulate)return true;
        for(int slot=0;slot<units.size();slot++)machine.setItem(slot,units.get(slot).copy());
        String definition=java.util.Objects.requireNonNullElse(dev.thaumcraft.gameplay.AddonData.infusionSignature(selected.id()),"");
        machine.setReservation(new MachineBlockEntity.Reservation(token,recipe,definition,machine.sequence));
        return true;
    }

    public static Status status(ServerLevel level,BlockPos pos,UUID token){
        if(!level.getServer().isSameThread())throw new IllegalStateException("Thaumcraft infuser API requires the server thread");
        if(!level.hasChunkAt(pos))return Status.UNLOADED;
        var machine=infuser(level,pos);var reservation=machine==null?null:machine.reservation();
        if(reservation==null||!reservation.token().equals(token))return Status.NONE;
        if(machine.sequence!=reservation.sequence())return Status.DONE;
        return MachineLogic.selectInfusion(level,machine,machine.inventory(),reservation)==null?Status.BLOCKED:Status.RUNNING;
    }

    /**
     * Ends the reservation and returns every stack it owned, which the caller now owns: the actual outputs and
     * byproducts after completion, otherwise the unconsumed inputs. Vis already absorbed is not refunded. Returns an
     * empty list when this token holds no reservation there. Unlike the other methods it loads the infuser's chunk,
     * so a bridge removed while its target is unloaded still ends the reservation instead of stranding the batch.
     */
    public static List<ItemStack> finish(ServerLevel level,BlockPos pos,UUID token){
        var machine=infuser(level,pos,true);
        if(machine==null||machine.reservation()==null||!machine.reservation().token().equals(token))return List.of();
        var owned=new ArrayList<ItemStack>();
        for(int slot=0;slot<outputEnd(machine);slot++){
            ItemStack stack=machine.getItem(slot);
            if(!stack.isEmpty()){owned.add(stack.copy());machine.setItem(slot,ItemStack.EMPTY);}
        }
        machine.setReservation(null);
        return owned;
    }

    private static MachineBlockEntity infuser(ServerLevel level,BlockPos pos){return infuser(level,pos,false);}
    /** With {@code load}, an unloaded chunk is loaded instead of reported absent. */
    private static MachineBlockEntity infuser(ServerLevel level,BlockPos pos,boolean load){
        if(!level.getServer().isSameThread())throw new IllegalStateException("Thaumcraft infuser API requires the server thread");
        if(!load&&!level.hasChunkAt(pos)||!(level.getBlockEntity(pos) instanceof MachineBlockEntity machine))return null;
        String id=machine.machineId();
        return id.equals("thaumic_infuser")||id.equals("dark_infuser")?machine:null;
    }
    private static int outputEnd(MachineBlockEntity machine){return MachineLayout.get(machine.machineId()).outputEnd();}
    private static boolean validUnits(MachineBlockEntity machine,List<ItemStack> units){
        return !units.isEmpty()&&units.size()<=MachineLogic.infusionSlots(machine)&&units.stream().allMatch(unit->unit.getCount()==1);
    }
    private static List<ItemStack> placed(MachineBlockEntity machine,List<ItemStack> units){
        var inventory=new ArrayList<ItemStack>();
        for(int slot=0;slot<MachineBlockEntity.INPUTS;slot++)inventory.add(slot<units.size()?units.get(slot).copy():ItemStack.EMPTY);
        return inventory;
    }
}
