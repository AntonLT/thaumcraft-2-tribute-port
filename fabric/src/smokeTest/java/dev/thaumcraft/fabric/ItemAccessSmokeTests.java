package dev.thaumcraft.fabric;

import dev.thaumcraft.Thaumcraft;
import dev.thaumcraft.api.IntegrationHooks;
import dev.thaumcraft.content.Content;
import dev.thaumcraft.machine.MachineBlockEntity;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import java.util.UUID;

/** Loader item access for TC2 machines and storage-only inventories. Runs only with -PsmokeTest. */
public final class ItemAccessSmokeTests {
    private static int checks;
    private static void check(boolean condition,String message){checks++;if(!condition)throw new AssertionError("Item access: "+message);}
    private static int size(net.fabricmc.fabric.api.transfer.v1.storage.Storage<ItemVariant> storage){int count=0;for(var view:storage)count++;return count;}
    public static void run(MinecraftServer server){
        checks=0;var level=server.overworld();var pos=new BlockPos(100,300,96);level.getChunkAt(pos);
        level.setBlockAndUpdate(pos,Content.block("thaumic_infuser").defaultBlockState());
        var infuser=(MachineBlockEntity)level.getBlockEntity(pos);
        var top=ItemStorage.SIDED.find(level,pos,Direction.UP);
        var side=ItemStorage.SIDED.find(level,pos,Direction.NORTH);
        check(top!=null&&size(top)==6&&side!=null&&size(side)==2,"Infuser exposes inputs above and outputs on the side");
        var unsided=ItemStorage.SIDED.find(level,pos,null);
        check(unsided==null||size(unsided)==0,"Unsided access is not exposed");
        var named=new ItemStack(Items.IRON_INGOT,64);named.set(DataComponents.CUSTOM_NAME,Component.literal("Marked"));
        var variant=ItemVariant.of(named);
        try(var transaction=Transaction.openOuter()){
            check(top.insert(variant,500,transaction)==384,"Full inputs leave the remainder at the source");
        }
        check(infuser.getItem(0).isEmpty(),"An aborted transaction changes nothing");
        try(var outer=Transaction.openOuter()){
            try(var inner=outer.openNested()){check(top.insert(variant,10,inner)==10,"Nested insertion");inner.commit();}
            outer.commit();
        }
        check(infuser.getItem(0).getCount()==10&&infuser.getItem(0).has(DataComponents.CUSTOM_NAME),"A committed transfer keeps components");
        try(var transaction=Transaction.openOuter()){check(side.insert(variant,1,transaction)==0,"Outputs cannot be filled from the side");}
        infuser.setItem(9,named.copyWithCount(3));
        try(var transaction=Transaction.openOuter()){check(side.extract(variant,64,transaction)==3,"Side extracts outputs");transaction.commit();}
        check(infuser.getItem(9).isEmpty(),"Extraction commits");
        infuser.installUpgrade(new ItemStack(Content.item("quicksilver_core")));
        for(Direction face:Direction.values()){
            var storage=ItemStorage.SIDED.find(level,pos,face);
            for(var view:storage)check(!view.getResource().getItem().equals(Content.item("quicksilver_core")),"Upgrades are hidden from "+face);
        }
        infuser.setItem(0,ItemStack.EMPTY);
        infuser.setReservation(new MachineBlockEntity.Reservation(UUID.randomUUID(),Thaumcraft.id("reserved"),"",infuser.sequence));
        infuser.setItem(0,named.copyWithCount(1));
        try(var transaction=Transaction.openOuter()){
            check(top.insert(variant,1,transaction)==0&&top.extract(variant,1,transaction)==0,"A reservation refuses loader access");
        }
        infuser.setReservation(null);infuser.setItem(0,ItemStack.EMPTY);
        var interfacePos=pos.east(2);
        level.setBlockAndUpdate(interfacePos,Content.block("void_interface").defaultBlockState());
        var exposed=ItemStorage.SIDED.find(level,interfacePos,Direction.UP);
        check(exposed==null||size(exposed)==0,"Void interfaces are not exposed");
        var composter=pos.east(4);
        level.setBlockAndUpdate(composter,Blocks.COMPOSTER.defaultBlockState());
        var access=IntegrationHooks.inventory(level,composter,Direction.UP);
        check(access!=null&&access.insert(new ItemStack(Items.WHEAT_SEEDS,5),true)==1,"Storage-only inventories are reachable");
        check(level.getBlockState(composter).getValue(net.minecraft.world.level.block.ComposterBlock.LEVEL)==0,"Simulated adapter insertion changes nothing");
        check(IntegrationHooks.insert(level,composter,Direction.UP,new ItemStack(Items.WHEAT_SEEDS,5)).getCount()==4,"Adapter insertion returns the remainder");
        for(var at:new BlockPos[]{pos,interfacePos,composter})level.setBlockAndUpdate(at,Blocks.AIR.defaultBlockState());
        Thaumcraft.LOG.info("THAUMCRAFT_FABRIC_ITEM_ACCESS_TESTS_PASS checks={}",checks);
    }
}
