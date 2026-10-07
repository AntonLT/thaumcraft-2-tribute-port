package dev.thaumcraft.test;

import dev.thaumcraft.Thaumcraft;
import dev.thaumcraft.client.legacy.LegacyDraw;
import dev.thaumcraft.client.legacy.LegacyVisuals;
import dev.thaumcraft.content.Content;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import java.util.List;
import java.util.Set;

/** Enclosed brains must draw before the depth-writing glass shell. */
final class BrainJarRenderChecks {
    private static final BlockPos JAR=new BlockPos(10,160,0);
    private static int ticks;
    static void verify(List<LegacyDraw.Batch> batches,String id){
        var texture=Thaumcraft.id("textures/legacy/"+(id.equals("brain_in_a_jar")?"brain.png":"brain2.png"));
        int brain=-1,glass=-1;
        for(int i=0;i<batches.size();i++){
            var batch=batches.get(i);
            if(batch.texture().equals(texture)){
                brain=i;
                if(batch.vertices().size()!=72)throw new AssertionError(id+": incomplete brain model");
                for(var v:batch.vertices())if(v.x()<0||v.x()>1||v.y()<0||v.y()>1||v.z()<0||v.z()>1)
                    throw new AssertionError(id+": brain outside its block");
            }
            if(batch.texture().equals(Thaumcraft.id("textures/legacy/blocks.png"))&&batch.blend()!=0)glass=i;
        }
        if(brain<0||glass<0||brain>=glass)throw new AssertionError(id+": brain must precede glass, brain="+brain+", glass="+glass);
    }
    static void tick(){
        var mc=Minecraft.getInstance();
        if(++ticks==1){
            mc.options.renderDistance().set(4);mc.options.hideGui=true;mc.options.pauseOnLostFocus=false;mc.setScreen(null);
            mc.getSingleplayerServer().execute(()->{
                var server=mc.getSingleplayerServer();var level=server.overworld();
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"time set 6000");
                server.setWeatherParameters(6000,0,false,false);
                for(var pos:BlockPos.betweenClosed(JAR.offset(-3,0,-3),JAR.offset(5,4,5)))level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
                for(var pos:BlockPos.betweenClosed(JAR.offset(-3,-1,-3),JAR.offset(5,-1,5)))level.setBlockAndUpdate(pos,Blocks.SMOOTH_STONE.defaultBlockState());
                level.setBlockAndUpdate(JAR,Content.block("brain_in_a_jar").defaultBlockState());
                level.setBlockAndUpdate(JAR.east(2),Content.block("occultic_enchanter").defaultBlockState());
                for(var player:server.getPlayerList().getPlayers()){
                    player.setGameMode(GameType.CREATIVE);player.getAbilities().flying=true;player.onUpdateAbilities();
                    player.teleportTo(level,11.5,160,4,Set.of(),180,14,true);
                }
            });
        }
        if(ticks==60){
            Screenshot.grab(mc.gameDirectory,"brain-jars.png",mc.getMainRenderTarget(),1,message->{});
            verify(LegacyVisuals.render(mc.level.getBlockEntity(JAR),.5f,0xf000f0),"brain_in_a_jar");
            verify(LegacyVisuals.render(mc.level.getBlockEntity(JAR.east(2)),.5f,0xf000f0),"occultic_enchanter");
            Thaumcraft.LOG.info("THAUMCRAFT_BRAIN_JAR_PASS brain_geometry_before_glass");
        }
        if(ticks==65)mc.stop();
    }
}
