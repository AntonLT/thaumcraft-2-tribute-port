package dev.thaumcraft.neoforge;

import dev.thaumcraft.Thaumcraft;
import dev.thaumcraft.api.IntegrationHooks;
import dev.thaumcraft.content.Content;
import dev.thaumcraft.machine.MachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import java.util.UUID;

/** Loader item access for TC2 machines and capability-only inventories. Runs only with -PsmokeTest. */
public final class ItemAccessSmokeTests {
    private static int checks;
    private static void check(boolean condition,String message){checks++;if(!condition)throw new AssertionError("Item access: "+message);}
    public static void run(MinecraftServer server){
        checks=0;var level=server.overworld();var pos=new BlockPos(100,300,96);level.getChunkAt(pos);
        level.setBlockAndUpdate(pos,Content.block("thaumic_infuser").defaultBlockState());
        var infuser=(MachineBlockEntity)level.getBlockEntity(pos);
        var top=level.getCapability(Capabilities.Item.BLOCK,pos,Direction.UP);
        var side=level.getCapability(Capabilities.Item.BLOCK,pos,Direction.NORTH);
        check(top!=null&&top.size()==6&&side!=null&&side.size()==2,"Infuser exposes inputs above and outputs on the side");
        check(level.getCapability(Capabilities.Item.BLOCK,pos,null)==null,"Unsided access is not exposed");
        var named=new ItemStack(Items.IRON_INGOT,64);named.set(DataComponents.CUSTOM_NAME,Component.literal("Marked"));
        var resource=ItemResource.of(named);
        try(var transaction=Transaction.openRoot()){
            check(ResourceHandlerUtil.insertStacking(top,resource,500,transaction)==384,"Full inputs leave the remainder at the source");
        }
        check(infuser.getItem(0).isEmpty(),"An aborted transaction changes nothing");
        try(var outer=Transaction.openRoot()){
            try(var inner=Transaction.open(outer)){check(top.insert(0,resource,10,inner)==10,"Nested insertion");inner.commit();}
            outer.commit();
        }
        check(infuser.getItem(0).getCount()==10&&infuser.getItem(0).has(DataComponents.CUSTOM_NAME),"A committed transfer keeps components");
        check(side.insert(0,resource,1,null)==0||infuser.getItem(9).isEmpty(),"Outputs cannot be filled from the side");
        infuser.setItem(9,named.copyWithCount(3));
        try(var transaction=Transaction.openRoot()){check(side.extract(0,resource,64,transaction)==3,"Side extracts outputs");transaction.commit();}
        check(infuser.getItem(9).isEmpty(),"Extraction commits");
        infuser.installUpgrade(new ItemStack(Content.item("quicksilver_core")));
        for(Direction face:Direction.values()){
            var handler=level.getCapability(Capabilities.Item.BLOCK,pos,face);
            for(int index=0;index<handler.size();index++)check(!handler.getResource(index).is(Content.item("quicksilver_core")),"Upgrades are hidden from "+face);
        }
        infuser.setItem(0,ItemStack.EMPTY);
        infuser.setReservation(new MachineBlockEntity.Reservation(UUID.randomUUID(),Thaumcraft.id("reserved"),"",infuser.sequence));
        infuser.setItem(0,named.copyWithCount(1));
        try(var transaction=Transaction.openRoot()){
            check(top.insert(1,resource,1,transaction)==0&&top.extract(0,resource,1,transaction)==0,"A reservation refuses loader access");
        }
        infuser.setReservation(null);infuser.setItem(0,ItemStack.EMPTY);
        var interfacePos=pos.east(2);
        level.setBlockAndUpdate(interfacePos,Content.block("void_interface").defaultBlockState());
        check(level.getCapability(Capabilities.Item.BLOCK,interfacePos,Direction.UP)==null,"Void interfaces are not exposed");
        var composter=pos.east(4);
        level.setBlockAndUpdate(composter,Blocks.COMPOSTER.defaultBlockState());
        var access=IntegrationHooks.inventory(level,composter,Direction.UP);
        check(access!=null&&access.insert(new ItemStack(Items.WHEAT_SEEDS,5),true)==1,"Capability-only inventories are reachable");
        check(level.getBlockState(composter).getValue(net.minecraft.world.level.block.ComposterBlock.LEVEL)==0,"Simulated adapter insertion changes nothing");
        check(IntegrationHooks.insert(level,composter,Direction.UP,new ItemStack(Items.WHEAT_SEEDS,5)).getCount()==4,"Adapter insertion returns the remainder");
        for(var at:new BlockPos[]{pos,interfacePos,composter})level.setBlockAndUpdate(at,Blocks.AIR.defaultBlockState());
        Thaumcraft.LOG.info("THAUMCRAFT_NEOFORGE_ITEM_ACCESS_TESTS_PASS checks={}",checks);
    }
}
