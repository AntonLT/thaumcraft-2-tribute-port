package dev.thaumcraft.machine;

import dev.thaumcraft.api.ThaumcraftDuplicators;
import dev.thaumcraft.api.ThaumcraftInfusers.Status;
import dev.thaumcraft.content.Content;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import java.util.UUID;

/** The public one-cycle duplicator contract shared by storage-network addons. */
public final class DuplicatorReservationChecks {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError("Duplicator reservation: "+message);}
    public static int run(MinecraftServer server){
        checks=0;ServerLevel level=server.overworld();var pos=new BlockPos(-1024,290,-1020);level.getChunkAt(pos);
        level.setBlockAndUpdate(pos,Content.block("thaumic_duplicator").defaultBlockState());
        var machine=(MachineBlockEntity)level.getBlockEntity(pos);
        var dirt=new ItemStack(Items.DIRT);var token=UUID.randomUUID();var other=UUID.randomUUID();

        check(ThaumcraftDuplicators.find(level,pos).map(found->!found.repeat()&&found.template().isEmpty()).orElse(false),"Finds an empty duplicator in normal mode");
        check(ThaumcraftDuplicators.cost(dirt)==5&&ThaumcraftDuplicators.cost(new ItemStack(Items.COBBLESTONE))==2,"Cost is the vis value times five, two for cobblestone");
        check(ThaumcraftDuplicators.cost(new ItemStack(Items.NETHER_STAR))==0,"Refused items cost nothing");
        check(ItemStack.isSameItemSameComponents(ThaumcraftDuplicators.output(dirt,false),dirt)&&ThaumcraftDuplicators.output(dirt,false).getCount()==2&&ThaumcraftDuplicators.output(dirt,true).getCount()==1,"Normal mode makes two, repeat mode one");
        check(!ThaumcraftDuplicators.reserve(level,pos,token,new ItemStack(Items.NETHER_STAR),false)&&machine.getItem(0).isEmpty(),"A refused item changes nothing");
        check(ThaumcraftDuplicators.reserve(level,pos,token,dirt,true)&&machine.getItem(0).isEmpty()&&machine.reservation()==null,"Simulation changes nothing");

        // Normal mode: the item is consumed for two copies, and the reserved duplicator stops after that cycle.
        check(ThaumcraftDuplicators.reserve(level,pos,token,dirt,false)&&machine.getItem(0).is(Items.DIRT),"Accepts one item");
        check(!ThaumcraftDuplicators.reserve(level,pos,other,dirt,true),"A second bridge cannot reserve the duplicator");
        for(Direction face:Direction.values())for(int slot:machine.getSlotsForFace(face))check(!machine.canTakeItemThroughFace(slot,machine.getItem(slot),face),"Faces refuse reserved slot "+slot);
        var saved=machine.saveWithFullMetadata(level.registryAccess());
        var restored=(MachineBlockEntity)BlockEntity.loadStatic(pos,machine.getBlockState(),saved,level.registryAccess());
        check(restored!=null&&machine.reservation().equals(restored.reservation()),"Reservation survives save and load");
        check(ThaumcraftDuplicators.status(level,pos,token)==Status.RUNNING&&ThaumcraftDuplicators.status(level,pos,other)==Status.NONE,"Status belongs to the token");
        machine.insertVis(50,false);
        for(int tick=0;tick<200&&ThaumcraftDuplicators.status(level,pos,token)!=Status.DONE;tick++)MachineLogic.tickVisProcess(level,machine);
        check(ThaumcraftDuplicators.status(level,pos,token)==Status.DONE&&Math.abs(machine.pureVis()-45)<.01,"One cycle runs with the ordinary cost: "+machine.pureVis());
        var copies=ThaumcraftDuplicators.finish(level,pos,token);
        check(copies.stream().filter(stack->stack.is(Items.DIRT)).mapToInt(ItemStack::getCount).sum()==2,"Completion hands over both copies");
        for(int slot=0;slot<18;slot++)check(machine.getItem(slot).isEmpty(),"Completion leaves nothing in slot "+slot);

        // Repeat mode: the template stays and one reservation buys exactly one copy.
        machine.processes.repeat=true;machine.setItem(0,dirt.copy());
        check(!ThaumcraftDuplicators.reserve(level,pos,token,new ItemStack(Items.SAND),true),"Repeat mode needs the template's item");
        check(ThaumcraftDuplicators.reserve(level,pos,token,dirt,false)&&machine.getItem(0).getCount()==1,"Repeat mode reserves without adding an item");
        for(int tick=0;tick<200;tick++)MachineLogic.tickVisProcess(level,machine);
        check(ThaumcraftDuplicators.status(level,pos,token)==Status.DONE&&Math.abs(machine.pureVis()-40)<.01,"A reserved repeat duplicator stops after one copy: "+machine.pureVis());
        copies=ThaumcraftDuplicators.finish(level,pos,token);
        check(copies.size()==1&&copies.getFirst().is(Items.DIRT)&&copies.getFirst().getCount()==1&&machine.getItem(0).is(Items.DIRT),"Completion hands over one copy and keeps the template");

        // Cancelling a normal-mode job before it runs returns the item.
        machine.processes.repeat=false;machine.setItem(0,ItemStack.EMPTY);machine.extractVis(machine.pureVis(),false);
        check(ThaumcraftDuplicators.reserve(level,pos,token,dirt,false),"Reserves again");
        var returned=ThaumcraftDuplicators.finish(level,pos,token);
        check(returned.size()==1&&returned.getFirst().is(Items.DIRT)&&machine.reservation()==null&&machine.getItem(0).isEmpty(),"Cancellation returns the unconsumed item");
        level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
        return checks;
    }
}
