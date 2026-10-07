package dev.thaumcraft.test;

import dev.thaumcraft.Thaumcraft;
import dev.thaumcraft.content.Content;
import dev.thaumcraft.machine.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.sounds.SoundEventListener;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;

/** Observe normal menu and block packets plus the client's received sound events. */
final class QuaesitumParityChecks {
    private static int stage,ticks,started,lastProgress=-1,betweenBoundaries,cycles,soundCountAtStop;
    private static BlockPos pos;
    private static boolean finished;
    private static final java.util.List<Long> sounds=new java.util.ArrayList<>();
    private static final java.util.List<String> samples=new java.util.ArrayList<>();
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError("Quaesitum client: "+message);}
    private static final SoundEventListener listener=(sound,event,range)->{
        if(sound.getIdentifier().toString().equals("thaumcraft2tp:scribble")){
            check(Math.abs(sound.getVolume()-.1f)<.0001f&&sound.getPitch()==1,"scribble volume and pitch");
            sounds.add(Minecraft.getInstance().level.getGameTime());
        }
    };
    static boolean tick(){
        if(finished)return true;var mc=Minecraft.getInstance();var server=mc.getSingleplayerServer();ticks++;
        check(ticks<650,"fixture timeout at stage "+stage);
        if(stage==0){
            mc.getSoundManager().addListener(listener);pos=mc.player.blockPosition().offset(3,0,0);
            server.execute(()->{
                var level=server.overworld();level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());level.setBlockAndUpdate(pos,Content.block("quaesitum").defaultBlockState());
                var machine=(MachineBlockEntity)level.getBlockEntity(pos);machine.setItem(0,new ItemStack(Items.COBBLESTONE,64));machine.setItem(3,new ItemStack(Items.PAPER,64));
                for(var player:server.getPlayerList().getPlayers())player.openMenu(machine);
            });stage=1;return false;
        }
        if(stage==1){if(!(mc.player.containerMenu instanceof MachineMenu menu)||!menu.machineId().equals("quaesitum")||!(mc.level.getBlockEntity(pos) instanceof MachineBlockEntity))return false;stage=2;started=ticks;}
        if(stage==2){
            var machine=(MachineBlockEntity)mc.level.getBlockEntity(pos);check(machine!=null,"tracked machine");
            if(mc.player.containerMenu instanceof MachineMenu menu&&menu.machineId().equals("quaesitum")){
                int progress=menu.progress();samples.add(mc.level.getGameTime()+","+progress+","+machine.visualWorking());
                if(lastProgress>=0&&progress!=lastProgress&&progress%5!=0)betweenBoundaries++;
                if(lastProgress>50&&progress<lastProgress){cycles++;check(machine.visualWorking(),"working flag persists across completion");}
                lastProgress=progress;
            }
            if(ticks-started==80)Screenshot.grab(mc.gameDirectory,"thaumcraft-quaesitum-working.png",mc.getMainRenderTarget(),1,result->{});
            if(ticks-started==200)server.execute(()->{for(var player:server.getPlayerList().getPlayers())player.closeContainer();});
            if(ticks-started<340)return false;
            check(betweenBoundaries>20&&cycles>=1,"menu progress advances between old five-tick boundaries and across cycles");
            check(sounds.size()>=3,"working audio repeats with menu closed");
            for(int i=1;i<sounds.size();i++)check(Math.abs(sounds.get(i)-sounds.get(i-1)-110)<=4,"110-tick scribble spacing: "+sounds);
            server.execute(()->((MachineBlockEntity)server.overworld().getBlockEntity(pos)).setItem(3,ItemStack.EMPTY));
            stage=3;started=ticks;return false;
        }
        if(stage==3){
            if(ticks-started<15)return false;
            check(!((MachineBlockEntity)mc.level.getBlockEntity(pos)).visualWorking(),"paper removal stops client animation");soundCountAtStop=sounds.size();stage=4;started=ticks;return false;
        }
        if(ticks-started<115)return false;
        check(sounds.size()==soundCountAtStop,"idle machine sends no new scribble sounds");mc.getSoundManager().removeListener(listener);
        try{var output=mc.gameDirectory.toPath().resolve("screenshots/thaumcraft-quaesitum-series.csv");java.nio.file.Files.createDirectories(output.getParent());java.nio.file.Files.write(output,samples);}catch(java.io.IOException e){throw new AssertionError(e);}
        server.execute(()->server.overworld().removeBlock(pos,false));
        Thaumcraft.LOG.info("THAUMCRAFT_QUAESITUM_PARITY_PASS progress_samples={} intermediate={} cycles={} sound_ticks={}",samples.size(),betweenBoundaries,cycles,sounds);
        finished=true;return true;
    }
}
