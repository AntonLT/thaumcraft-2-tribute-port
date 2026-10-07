package dev.thaumcraft.test;

import dev.thaumcraft.content.Content;
import dev.thaumcraft.machine.MachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import java.util.UUID;

final class VoidNetworkSmokeTests {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError("Void network: "+message);}
    private static MachineBlockEntity place(ServerLevel level,BlockPos pos,String id,UUID owner){
        level.getChunkAt(pos);level.setBlockAndUpdate(pos,Content.block(id).defaultBlockState());
        var machine=(MachineBlockEntity)level.getBlockEntity(pos);machine.setOwner(owner);machine.setChannel(2);return machine;
    }
    static int run(MinecraftServer server){
        checks=0;
        var level=server.overworld();var nether=server.getLevel(Level.NETHER);
        check(nether!=null,"Nether exists");
        var owner=UUID.fromString("b8c92bde-b985-4232-9814-2982a9f30011");
        var other=UUID.fromString("b8c92bde-b985-4232-9814-2982a9f30012");
        var pos=new BlockPos(512,100,512);
        var local=place(level,pos,"void_chest",owner);
        var source=place(level,pos.above(),"void_interface",owner);
        // Identical coordinates exercise dimension-aware lookup and page ordering.
        var remote=place(nether,pos,"void_chest",owner);
        var portal=place(nether,pos.above(),"void_interface",owner);
        var foreign=place(level,pos.east(3),"void_chest",other);
        var foreignPortal=place(level,pos.east(3).above(),"void_interface",other);
        var unowned=place(level,pos.east(6),"void_chest",null);
        var unownedPortal=place(level,pos.east(6).above(),"void_interface",null);
        try {
            remote.setItem(0,new ItemStack(Items.DIAMOND,13));
            foreign.setItem(0,new ItemStack(Items.EMERALD,7));
            check(source.linkedVoidChests().equals(java.util.List.of(local,remote)),"Only matching owner/channel links, local dimension first");
            check(source.getItem(72).is(Items.DIAMOND)&&source.getItem(72).getCount()==13,"Cross-dimension inventory reads");
            check(source.removeItem(72,4).getCount()==4&&remote.getItem(0).getCount()==9,"Cross-dimension removal reaches actual chest");
            source.setItem(73,new ItemStack(Items.GOLD_INGOT,5));
            check(remote.getItem(1).getCount()==5&&local.getItem(1).isEmpty(),"Cross-dimension insertion does not hit same coordinates locally");
            check(foreignPortal.linkedVoidChests().equals(java.util.List.of(foreign)),"Second player's network remains separate");
            check(unownedPortal.linkedVoidChests().equals(java.util.List.of(unowned)),"Ownerless interface remains local");
            portal.setChannel(3);
            check(source.voidPages()==1&&source.getItem(72).isEmpty(),"Channel change invalidates cached remote access");
            portal.setChannel(2);
            check(source.voidPages()==2,"Matching channel reconnects");
            portal.setOwner(other);
            check(source.voidPages()==1,"Ownership change invalidates cached links");
            portal.setOwner(owner);
            check(source.voidPages()==2,"Restored ownership reconnects");
            source.clearContent();
            check(remote.isEmpty()&&foreign.getItem(0).getCount()==7,"Clear reaches remote chest but not another player's storage");
            level.setBlockAndUpdate(pos.above(),Blocks.AIR.defaultBlockState());
            check(portal.linkedVoidChests().equals(java.util.List.of(remote)),"Removed interface disappears across dimensions");
        } finally {
            for(var dimension:java.util.List.of(level,nether))for(int offset:new int[]{0,3,6}){
                dimension.setBlockAndUpdate(pos.east(offset).above(),Blocks.AIR.defaultBlockState());
                dimension.setBlockAndUpdate(pos.east(offset),Blocks.AIR.defaultBlockState());
            }
        }
        return checks;
    }
}
