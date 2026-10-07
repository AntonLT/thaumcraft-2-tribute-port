package dev.thaumcraft.api;

import dev.thaumcraft.api.ThaumcraftInfusers.Status;
import dev.thaumcraft.machine.MachineBlockEntity;
import dev.thaumcraft.machine.MachineLayout;
import dev.thaumcraft.machine.MachineLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * One duplication cycle for storage-network bridges, with the same ownership contract as {@link ThaumcraftInfusers}.
 * In normal mode a bridge reserves an empty duplicator with one item, which is consumed for two copies. In repeat mode
 * the duplicator keeps its template and a reservation buys one copy. The duplicator runs with its ordinary vis cost,
 * speed and upgrades, stops after that one cycle, and no player, container or loader path may touch its slots or switch
 * its mode until the bridge finishes the reservation.
 *
 * <p>Every method must run on the server thread.
 */
public final class ThaumcraftDuplicators {
    private ThaumcraftDuplicators() {}

    /** A loaded duplicator. {@code template} is a copy of its template slot, empty when there is none. */
    public record Duplicator(boolean repeat,ItemStack template) {}

    public static Optional<Duplicator> find(ServerLevel level,BlockPos pos){
        var machine=duplicator(level,pos,false);
        return machine==null?Optional.empty():Optional.of(new Duplicator(machine.processes.repeat,machine.getItem(0).copy()));
    }
    /**
     * Vis one cycle can cost for {@code item}, without the efficiency upgrade that lowers it, or 0 when duplicators
     * refuse the item.
     */
    public static float cost(ItemStack item){return MachineLogic.duplicationCost(item,false);}
    /** What one cycle hands over: two copies in normal mode, one in repeat mode, never stored contents. */
    public static ItemStack output(ItemStack item,boolean repeat){return MachineLogic.duplicationOutput(item,repeat);}

    /**
     * Reserves one cycle, or changes nothing. Normal mode places {@code item} into an empty duplicator; repeat mode
     * requires a template of the same item and room for its copy. Fails while reserved or when the item is refused.
     */
    public static boolean reserve(ServerLevel level,BlockPos pos,UUID token,ItemStack item,boolean simulate){
        var machine=duplicator(level,pos,false);
        if(machine==null||token==null||machine.reservation()!=null||item.getCount()!=1||cost(item)<=0)return false;
        boolean repeat=machine.processes.repeat;
        if(repeat){
            if(!ItemStack.isSameItemSameComponents(machine.getItem(0),item)||!machine.output(output(item,true),true))return false;
        }else for(int slot=0;slot<outputEnd(machine);slot++)if(!machine.getItem(slot).isEmpty())return false;
        if(simulate)return true;
        if(!repeat)machine.setItem(0,item.copy());
        machine.setReservation(new MachineBlockEntity.Reservation(token,BuiltInRegistries.ITEM.getKey(item.getItem()),repeat?"repeat":"normal",machine.sequence));
        return true;
    }

    /** {@link Status#BLOCKED} means the template is now refused, for example by a changed vis value or tag. */
    public static Status status(ServerLevel level,BlockPos pos,UUID token){
        if(!level.getServer().isSameThread())throw new IllegalStateException("Thaumcraft duplicator API requires the server thread");
        if(!level.hasChunkAt(pos))return Status.UNLOADED;
        var machine=duplicator(level,pos,false);var reservation=machine==null?null:machine.reservation();
        if(reservation==null||!reservation.token().equals(token))return Status.NONE;
        if(machine.sequence!=reservation.sequence())return Status.DONE;
        return cost(machine.getItem(0))<=0?Status.BLOCKED:Status.RUNNING;
    }

    /**
     * Ends the reservation and returns every stack it owned: after completion, the copies (and, in repeat mode, any
     * copies made before it from conduit vis); otherwise the unconsumed item. A repeat-mode template stays in the
     * duplicator. Vis already absorbed is not refunded. Loads the chunk, like {@link ThaumcraftInfusers#finish}.
     */
    public static List<ItemStack> finish(ServerLevel level,BlockPos pos,UUID token){
        var machine=duplicator(level,pos,true);
        if(machine==null||machine.reservation()==null||!machine.reservation().token().equals(token))return List.of();
        var owned=new ArrayList<ItemStack>();
        for(int slot=machine.processes.repeat?1:0;slot<outputEnd(machine);slot++){
            ItemStack stack=machine.getItem(slot);
            if(!stack.isEmpty()){owned.add(stack.copy());machine.setItem(slot,ItemStack.EMPTY);}
        }
        machine.setReservation(null);
        return owned;
    }

    private static MachineBlockEntity duplicator(ServerLevel level,BlockPos pos,boolean load){
        if(!level.getServer().isSameThread())throw new IllegalStateException("Thaumcraft duplicator API requires the server thread");
        if(!load&&!level.hasChunkAt(pos)||!(level.getBlockEntity(pos) instanceof MachineBlockEntity machine))return null;
        return machine.machineId().equals("thaumic_duplicator")?machine:null;
    }
    private static int outputEnd(MachineBlockEntity machine){return MachineLayout.get(machine.machineId()).outputEnd();}
}
