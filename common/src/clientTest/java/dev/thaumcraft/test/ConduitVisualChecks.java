package dev.thaumcraft.test;

import dev.thaumcraft.Thaumcraft;
import dev.thaumcraft.client.legacy.LegacyCompat;
import dev.thaumcraft.client.legacy.LegacyVisuals;
import dev.thaumcraft.content.Content;
import dev.thaumcraft.machine.MachineBlockEntity;
import dev.thaumcraft.machine.VisNetwork;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.BundlePacket;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Real packet, renderer, sound and powered-valve regressions, isolated by -Dthaumcraft.conduitSmoke=true. */
final class ConduitVisualChecks {
    private static final java.util.Queue<Object> packets=new java.util.concurrent.ConcurrentLinkedQueue<>();
    private static final boolean[] pulse=new boolean[5],cleared=new boolean[5];
    private static final String[] IDS={"vis_conduit","vis_filter","vis_valve","advanced_vis_valve","vis_conduit"};
    private static BlockPos origin;
    private static Channel channel;
    private static int ticks,stage,deadline;
    private static volatile Throwable serverFailure;
    private static void check(boolean value,String message){if(!value)throw new AssertionError("Vis parity: "+message);}
    private static void observe(Object message){
        if(message instanceof ClientboundBlockEntityDataPacket||message instanceof ClientboundSoundPacket)packets.add(message);
        else if(message instanceof BundlePacket<?> bundle)for(var packet:bundle.subPackets())observe(packet);
    }
    private static MachineBlockEntity place(ServerLevel level,BlockPos pos,String id){
        level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());level.setBlockAndUpdate(pos,Content.block(id).defaultBlockState());
        return (MachineBlockEntity)level.getBlockEntity(pos);
    }
    private static void tick(ServerLevel level,MachineBlockEntity machine){MachineBlockEntity.tick(level,machine.getBlockPos(),machine.getBlockState(),machine);}
    static boolean tick(){
        var mc=Minecraft.getInstance();var server=mc.getSingleplayerServer();
        if(serverFailure!=null)throw new AssertionError("Server vis regression",serverFailure);
        check(++ticks<300,"fixture timeout at stage "+stage);
        if(stage==0){
            try{
                var field=Connection.class.getDeclaredField("channel");field.setAccessible(true);channel=(Channel)field.get(mc.getConnection().getConnection());
                channel.pipeline().addBefore("packet_handler","conduit_checks",new ChannelInboundHandlerAdapter(){
                    @Override public void channelRead(ChannelHandlerContext ctx,Object message)throws Exception{observe(message);super.channelRead(ctx,message);}
                });
            }catch(ReflectiveOperationException e){throw new AssertionError(e);}
            origin=mc.player.blockPosition().offset(3,0,0);
            server.execute(()->{
                var level=server.overworld();for(int i=0;i<5;i++)place(level,origin.east(i*2),IDS[i]);
                place(level,origin.south(3),"vis_valve");place(level,origin.south(5),"advanced_vis_valve");
                for(int y=0;y<3;y++)place(level,origin.south(7).above(y),"vis_filter");
            });stage=1;return false;
        }
        if(stage==1){
            for(int i=0;i<5;i++)if(!(mc.level.getBlockEntity(origin.east(i*2)) instanceof MachineBlockEntity))return false;
            if(!(mc.level.getBlockEntity(origin.south(7).above(2)) instanceof MachineBlockEntity))return false;
            packets.clear();
            server.execute(()->{try{exerciseServer(server.overworld(),server.getPlayerList().getPlayers().getFirst());}catch(Throwable e){serverFailure=e;}});
            stage=2;deadline=ticks+30;return false;
        }
        if(ticks<deadline)return false;
        int openClicks=0,closedClicks=0;
        Object message;
        while((message=packets.poll())!=null){
            if(message instanceof ClientboundBlockEntityDataPacket packet)for(int i=0;i<5;i++)if(packet.getPos().equals(origin.east(i*2))){
                var tag=packet.getTag();
                if(tag.getFloatOr("display_pure_vis",0)==2&&tag.getFloatOr("display_tainted_vis",0)==1&&tag.getFloatOr("pure_vis",-1)==0&&tag.getFloatOr("tainted_vis",-1)==0)pulse[i]=true;
                else if(pulse[i]&&tag.getFloatOr("display_pure_vis",-1)==0&&tag.getFloatOr("display_tainted_vis",-1)==0)cleared[i]=true;
            }
            if(message instanceof ClientboundSoundPacket sound&&sound.getSound().value().equals(SoundEvents.LEVER_CLICK)){
                check(sound.getVolume()==.3f,"original click volume");
                if(sound.getPitch()==.6f)openClicks++;else if(sound.getPitch()==.5f)closedClicks++;
            }
        }
        for(int i=0;i<5;i++){check(pulse[i],"empty buffer still sends brief mixed flow at phase "+i);check(cleared[i],"display clears at phase "+i);}
        check(openClicks==1&&closedClicks==2,"normal open/close and advanced clicks reach client with original pitches");
        var filter=(MachineBlockEntity)mc.level.getBlockEntity(origin.south(7));
        check(filter.filterWisps()==1&&filter.filterStack()==2,"filter emission count and stack height reach client");
        checkFilterParticle(mc,filter);
        channel.pipeline().remove("conduit_checks");
        server.execute(()->{
            var level=server.overworld();for(int i=0;i<5;i++)level.removeBlock(origin.east(i*2),false);
            level.removeBlock(origin.south(3),false);level.removeBlock(origin.south(5),false);
            for(int y=0;y<3;y++)level.removeBlock(origin.south(7).above(y),false);
        });
        Thaumcraft.LOG.info("THAUMCRAFT_CONDUIT_PASS powered_mode_restore_and_reload brief_flow_and_clear_5_phases filter_exhaust valve_sound_packets");
        return true;
    }
    private static void exerciseServer(ServerLevel level,net.minecraft.server.level.ServerPlayer player)throws ReflectiveOperationException{
        var advanced=(MachineBlockEntity)level.getBlockEntity(origin.south(5));
        for(int mode=0;mode<3;mode++){
            advanced.setChannel(mode);level.setBlockAndUpdate(advanced.getBlockPos().above(2),Blocks.REDSTONE_BLOCK.defaultBlockState());tick(level,advanced);
            check(advanced.channel()==0&&!advanced.enabled(),"power above advanced valve closes mode "+mode);
            advanced.setChannel((advanced.channel()+1)%3);tick(level,advanced);
            check(advanced.channel()==0,"powered click does not reopen valve");
            var saved=advanced.saveWithFullMetadata(level.registryAccess());
            var restored=(MachineBlockEntity)BlockEntity.loadStatic(advanced.getBlockPos(),advanced.getBlockState(),saved,level.registryAccess());
            check(restored!=null,"powered valve reloads");restored.setLevel(level);
            level.setBlockAndUpdate(advanced.getBlockPos().above(2),Blocks.AIR.defaultBlockState());tick(level,advanced);tick(level,restored);
            check(advanced.channel()==mode&&restored.channel()==mode,"original mode survives powered clicks and reload: "+mode);
        }
        var counter=MachineBlockEntity.class.getDeclaredField("counter");counter.setAccessible(true);
        for(int i=0;i<5;i++){
            var pipe=(MachineBlockEntity)level.getBlockEntity(origin.east(i*2));counter.setInt(pipe,i);
            pipe.insertVis(2,false);pipe.insertVis(1,true);pipe.extractVis(2,false);pipe.extractVis(1,true);
        }
        var filter=(MachineBlockEntity)level.getBlockEntity(origin.south(7));filter.insertVis(1,true);
        for(int i=0;i<15;i++)VisNetwork.tick(level,filter);
        check(filter.filterWisps()==0,"no exhaust before sixteen removals");VisNetwork.tick(level,filter);
        check(filter.filterWisps()==1&&filter.filterStack()==2,"sixteenth removal emits above two upper filters");
        filter.extractVis(filter.taintedVis(),true);
        var valve=(MachineBlockEntity)level.getBlockEntity(origin.south(3));
        click(level,player,valve);click(level,player,valve);click(level,player,advanced);
    }
    private static void click(ServerLevel level,net.minecraft.server.level.ServerPlayer player,MachineBlockEntity machine){
        player.setShiftKeyDown(false);
        machine.getBlockState().useWithoutItem(level,player,new BlockHitResult(Vec3.atCenterOf(machine.getBlockPos()),Direction.UP,machine.getBlockPos(),false));
    }
    @SuppressWarnings("unchecked")
    private static void checkFilterParticle(Minecraft mc,MachineBlockEntity filter){
        try{
            var map=LegacyVisuals.class.getDeclaredField("FILTER_WISPS");map.setAccessible(true);((java.util.Map<BlockPos,Integer>)map.get(null)).put(filter.getBlockPos(),0);
            var method=LegacyVisuals.class.getDeclaredMethod("ambientParticles",LegacyCompat.World.class,BlockPos.class);method.setAccessible(true);
            var field=mc.particleEngine.getClass().getDeclaredField("particlesToAdd");field.setAccessible(true);var pending=(java.util.Queue<?>)field.get(mc.particleEngine);
            int before=pending.size();var world=new LegacyCompat.World(mc.level,0);world.effectsAllowed=true;
            LegacyCompat.capture(world,filter.getBlockPos(),0xf000f0,mc.level.getGameTime(),()->{
                try{method.invoke(null,world,filter.getBlockPos());method.invoke(null,world,filter.getBlockPos());}catch(ReflectiveOperationException e){throw new AssertionError(e);}
            });
            check(pending.size()==before+1,"one synchronized exhaust event produces exactly one legacy particle");
            Object particle=pending.toArray()[before];
            check(particle.getClass().getSimpleName().equals("LegacyParticle"),"filter uses original wisp renderer");
            Class<?> type=particle.getClass();while(type!=null){try{var y=type.getDeclaredField("y");y.setAccessible(true);check(Math.abs(y.getDouble(particle)-(filter.getBlockPos().getY()+2.8))<.001,"exhaust originates above stack");return;}catch(NoSuchFieldException e){type=type.getSuperclass();}}
            throw new AssertionError("Particle position unavailable");
        }catch(ReflectiveOperationException e){throw new AssertionError(e);}
    }
}
