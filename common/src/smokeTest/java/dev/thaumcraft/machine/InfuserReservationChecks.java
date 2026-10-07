package dev.thaumcraft.machine;

import dev.thaumcraft.api.ThaumcraftInfusers;
import dev.thaumcraft.api.ThaumcraftInfusers.Status;
import dev.thaumcraft.content.Content;
import dev.thaumcraft.gameplay.GameData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** The public whole-batch infuser contract shared by storage-network addons. */
public final class InfuserReservationChecks {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError("Infuser reservation: "+message);}
    public static int run(MinecraftServer server){
        checks=0;ServerLevel level=server.overworld();var pos=new BlockPos(-1024,290,-1024); // Infusion adds aura vibes; keep them out of chunks other suites assert on.level.getChunkAt(pos);
        var recipe=GameData.infusions().stream().filter(r->!r.dark()&&r.requiredResearch()==null&&r.ingredients().size()>=2).findFirst().orElseThrow();
        var units=new ArrayList<ItemStack>();for(String ingredient:recipe.ingredients())units.add(representative(ingredient));
        units.set(0,units.get(0).copy());units.get(0).set(DataComponents.CUSTOM_NAME,Component.literal("Named input"));
        level.setBlockAndUpdate(pos,Content.block("thaumic_infuser").defaultBlockState());
        var infuser=(MachineBlockEntity)level.getBlockEntity(pos);
        var token=UUID.randomUUID();var other=UUID.randomUUID();

        var matched=ThaumcraftInfusers.match(level,pos,units).orElseThrow();
        var selected=matched.id();
        check(ThaumcraftInfusers.find(level,pos).map(found->!found.dark()&&found.inputSlots()==6).orElse(false),"Finds a normal infuser");
        check(ThaumcraftInfusers.match(level,pos,List.of(units.get(0).copyWithCount(2))).isEmpty(),"Merged repeated units are rejected");
        check(ThaumcraftInfusers.reserve(level,pos,token,selected,units,true)&&infuser.getItem(0).isEmpty()&&infuser.reservation()==null,"Simulation changes nothing");
        check(!ThaumcraftInfusers.reserve(level,pos,token,Identifier.parse("thaumcraft2tp:not_selected"),units,false)&&infuser.getItem(0).isEmpty(),"A batch selecting another recipe changes nothing");
        infuser.setItem(9,new ItemStack(Blocks.STONE));
        check(!ThaumcraftInfusers.reserve(level,pos,token,selected,units,false)&&infuser.getItem(0).isEmpty(),"Occupied output refuses a batch");
        infuser.setItem(9,ItemStack.EMPTY);

        check(ThaumcraftInfusers.reserve(level,pos,token,selected,units,false),"Accepts a complete batch");
        for(int slot=0;slot<units.size();slot++)check(ItemStack.matches(infuser.getItem(slot),units.get(slot)),"Places one unit per slot "+slot);
        check(!ThaumcraftInfusers.reserve(level,pos,other,selected,units,true),"A second bridge cannot reserve the infuser");
        check(ThaumcraftInfusers.status(level,pos,token)==Status.RUNNING&&ThaumcraftInfusers.status(level,pos,other)==Status.NONE,"Status belongs to the token");
        for(Direction face:Direction.values())for(int slot:infuser.getSlotsForFace(face)){
            check(!infuser.canTakeItemThroughFace(slot,infuser.getItem(slot),face)&&!infuser.canPlaceItemThroughFace(slot,units.get(0),face),"Faces refuse reserved slot "+slot);
        }
        check(infuser.reservedSlot(0)&&infuser.reservedSlot(9)&&!infuser.reservedSlot(MachineBlockEntity.UPGRADE_START),"Menus refuse reserved slots but keep upgrades");
        var saved=infuser.saveWithFullMetadata(level.registryAccess());
        var restored=(MachineBlockEntity)BlockEntity.loadStatic(pos,infuser.getBlockState(),saved,level.registryAccess());
        check(restored!=null&&infuser.reservation().equals(restored.reservation()),"Reservation survives save and load");

        var finished=ThaumcraftInfusers.finish(level,pos,token);
        check(finished.size()==units.size()&&ItemStack.matches(finished.get(0),units.get(0))&&finished.get(0).has(DataComponents.CUSTOM_NAME),"Cancellation returns unconsumed inputs with components");
        check(infuser.reservation()==null&&infuser.getItem(0).isEmpty()&&ThaumcraftInfusers.status(level,pos,token)==Status.NONE,"Cancellation releases the infuser");
        check(ThaumcraftInfusers.finish(level,pos,token).isEmpty(),"Finishing twice returns nothing");

        check(ThaumcraftInfusers.reserve(level,pos,token,selected,units,false),"Reserves again");
        var pinned=infuser.reservation();
        infuser.setReservation(new MachineBlockEntity.Reservation(token,pinned.recipe(),"changed by reload",pinned.sequence()));
        infuser.insertVis(500,false);
        for(int tick=0;tick<200;tick++)MachineLogic.tickVisProcess(level,infuser);
        check(ThaumcraftInfusers.status(level,pos,token)==Status.BLOCKED&&infuser.getItem(9).isEmpty(),"A changed pinned definition blocks without producing");
        infuser.setReservation(pinned);
        float before=infuser.pureVis();
        for(int tick=0;tick<recipe.cost()*4+40&&ThaumcraftInfusers.status(level,pos,token)!=Status.DONE;tick++)MachineLogic.tickVisProcess(level,infuser);
        check(ThaumcraftInfusers.status(level,pos,token)==Status.DONE&&infuser.pureVis()<before,"Pinned recipe runs with ordinary vis cost");
        var outputs=ThaumcraftInfusers.finish(level,pos,token);
        var expected=ThaumcraftInfusers.match(level,pos,units).orElseThrow().result();
        check(outputs.stream().anyMatch(stack->ItemStack.isSameItemSameComponents(stack,expected)&&stack.getCount()==expected.getCount()),"Completion hands over the actual output");
        for(int slot=0;slot<18;slot++)check(infuser.getItem(slot).isEmpty(),"Completion leaves no reserved stack in slot "+slot);
        level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
        return checks;
    }
    private static ItemStack representative(String ingredient){
        if(ingredient.startsWith("#"))return new ItemStack(BuiltInRegistries.ITEM.get(TagKey.create(Registries.ITEM,Identifier.parse(ingredient.substring(1)))).orElseThrow().iterator().next().value());
        return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(ingredient)));
    }
}
