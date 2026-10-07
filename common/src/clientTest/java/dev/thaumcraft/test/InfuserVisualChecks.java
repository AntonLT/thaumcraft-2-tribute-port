package dev.thaumcraft.test;

import dev.thaumcraft.Thaumcraft;
import dev.thaumcraft.client.legacy.LegacyCompat;
import dev.thaumcraft.client.legacy.LegacyVisuals;
import dev.thaumcraft.client.legacy.TileInfuserRenderer;
import dev.thaumcraft.content.Content;
import dev.thaumcraft.machine.MachineBlockEntity;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.BundlePacket;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/** Observe real block-entity packets, then exercise the renderer's actual particle output. */
final class InfuserVisualChecks {
    private static final java.util.Queue<ClientboundBlockEntityDataPacket> packets=new java.util.concurrent.ConcurrentLinkedQueue<>();
    private static final java.util.List<String> failures=new java.util.ArrayList<>();
    private static final boolean[] pulse=new boolean[5],stopped=new boolean[5];
    private static BlockPos origin;
    private static Channel channel;
    private static int ticks,stage,deadline;
    private static boolean finished;

    private static void check(boolean ok,String message){if(!ok)failures.add(message);}
    private static void observe(Object message){
        if(message instanceof ClientboundBlockEntityDataPacket packet)packets.add(packet);
        else if(message instanceof BundlePacket<?> bundle)for(var packet:bundle.subPackets())observe(packet);
    }
    static boolean tick(){
        if(finished)return true;
        var mc=Minecraft.getInstance();var server=mc.getSingleplayerServer();
        if(++ticks>300)throw new AssertionError("Infuser visual fixture timed out at stage "+stage);
        if(stage==0){
            try {
                var field=Connection.class.getDeclaredField("channel");field.setAccessible(true);
                channel=(Channel)field.get(mc.getConnection().getConnection());
                channel.pipeline().addBefore("packet_handler","infuser_visual_checks",new ChannelInboundHandlerAdapter(){
                    @Override public void channelRead(ChannelHandlerContext ctx,Object message)throws Exception{observe(message);super.channelRead(ctx,message);}
                });
            }catch(ReflectiveOperationException e){throw new AssertionError(e);}
            origin=mc.player.blockPosition().offset(4,0,0);
            server.execute(()->{
                var level=server.overworld();
                for(int i=0;i<5;i++){
                    var pos=origin.east(i*2);level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
                    level.setBlockAndUpdate(pos,Content.block("thaumic_infuser").defaultBlockState());
                    var machine=(MachineBlockEntity)level.getBlockEntity(pos);
                    machine.toggle(); // Wait for the client to track all machines before emitting one-tick pulses.
                }
            });
            stage=1;return false;
        }
        if(stage==1){
            for(int i=0;i<5;i++)if(!(mc.level.getBlockEntity(origin.east(i*2)) instanceof MachineBlockEntity))return false;
            packets.clear();
            server.execute(()->{
                var level=server.overworld();
                for(int i=0;i<5;i++){
                    var machine=(MachineBlockEntity)level.getBlockEntity(origin.east(i*2));
                    machine.setItem(0,new ItemStack(Items.GLOWSTONE_DUST));machine.setItem(1,new ItemStack(Items.REDSTONE));
                    machine.processes.pureCost=10;machine.processes.pureWork=9.5f;
                    machine.processes.boost=0;machine.processes.infuserBoostDelay=20;machine.insertVis(.5f,false);
                    // Exercise all five positions relative to the old shared synchronization interval.
                    try {var counter=MachineBlockEntity.class.getDeclaredField("counter");counter.setAccessible(true);counter.setInt(machine,i);}
                    catch(ReflectiveOperationException e){throw new AssertionError(e);}
                    machine.toggle();
                }
            });
            stage=2;deadline=ticks+40;return false;
        }
        if(stage==2){
            ClientboundBlockEntityDataPacket packet;
            while((packet=packets.poll())!=null)for(int i=0;i<5;i++)if(packet.getPos().equals(origin.east(i*2))){
                var tag=packet.getTag();
                if(tag.getFloatOr("infuser_sucked",0)>0&&tag.getIntOr("progress",-1)==0)pulse[i]=true;
                else if(pulse[i]&&tag.getFloatOr("infuser_sucked",-1)==0)stopped[i]=true;
            }
            if(ticks<deadline)return false;
            for(int i=0;i<5;i++){
                check(pulse[i],"Client receives final absorption pulse at phase "+i);
                check(stopped[i],"Client receives absorption stop after final pulse at phase "+i);
            }
            server.execute(()->{
                var machine=(MachineBlockEntity)server.overworld().getBlockEntity(origin);
                machine.setItem(0,new ItemStack(Items.GLOWSTONE_DUST,2));machine.setItem(1,new ItemStack(Items.REDSTONE,2));
                machine.setItem(9,ItemStack.EMPTY);machine.setItem(18,new ItemStack(Content.item("quicksilver_core")));
                machine.processes.pureCost=10;machine.processes.pureWork=5;machine.extractVis(machine.pureVis(),false);machine.setChanged();
            });
            stage=3;deadline=ticks+15;return false;
        }
        if(ticks<deadline)return false;
        if(stage==3){
            check(flames(mc,false)==1,"Starved valid upgraded infusion emits a corner flame");
            check(flames(mc,true)==0,"Normal infuser flame change does not affect the Dark Infuser");
            server.execute(()->((MachineBlockEntity)server.overworld().getBlockEntity(origin)).setItem(9,new ItemStack(Items.DIRT)));
            stage=4;deadline=ticks+15;return false;
        }
        if(stage==4){
            check(flames(mc,false)==0,"Output-blocked upgraded infusion emits no corner flame with retained progress");
            server.execute(()->{
                var level=server.overworld();((MachineBlockEntity)level.getBlockEntity(origin)).setItem(9,ItemStack.EMPTY);
                level.setBlockAndUpdate(origin.above(),Blocks.REDSTONE_BLOCK.defaultBlockState());
            });
            stage=5;deadline=ticks+15;return false;
        }
        check(flames(mc,false)==0,"Powered upgraded infusion emits no corner flame");
        channel.pipeline().remove("infuser_visual_checks");
        server.execute(()->{
            var level=server.overworld();level.setBlockAndUpdate(origin.above(),Blocks.AIR.defaultBlockState());
            for(int i=0;i<5;i++)level.setBlockAndUpdate(origin.east(i*2),Blocks.AIR.defaultBlockState());
        });
        if(!failures.isEmpty())throw new AssertionError(String.join("; ",failures));
        finished=true;Thaumcraft.LOG.info("THAUMCRAFT_INFUSER_VISUAL_PASS final_pulse_and_stop_all_5_phases flame_conditions dark_unchanged");
        return true;
    }
    private static int flames(Minecraft mc,boolean dark){
        try {
            var field=mc.particleEngine.getClass().getDeclaredField("particlesToAdd");field.setAccessible(true);
            var pending=(java.util.Queue<?>)field.get(mc.particleEngine);
            var world=new LegacyCompat.World(mc.level,0);world.effectsAllowed=true;
            int before=pending.size();
            LegacyCompat.capture(world,origin,0xf000f0,mc.level.getGameTime(),()->{
                var tile=(LegacyCompat.TileInfuser)LegacyVisuals.snapshot(world,origin);
                if(tile.sucked!=0)throw new AssertionError("Flame fixture must be vis-starved");
                if(dark){tile.id="dark_infuser";tile.metadata=2;tile.worked=true;}
                new TileInfuserRenderer().renderEntityAt(tile,0,0,0,0);
            });
            return pending.size()-before;
        }catch(ReflectiveOperationException e){throw new AssertionError(e);}
    }
}
