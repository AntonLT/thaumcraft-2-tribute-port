package dev.thaumcraft.test;

import dev.thaumcraft.Thaumcraft;
import dev.thaumcraft.client.legacy.LegacyVisuals;
import dev.thaumcraft.content.Content;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import java.util.Set;

/** Opt-in visual regression at both sides of the legacy void detail threshold. */
final class VoidChestRenderChecks {
    private static final BlockPos CHEST=new BlockPos(10,120,0);
    private static final int[] DISTANCES={3,6,10,24};
    private static int ticks;
    private static int checkedImages;
    static void tick(){
        var mc=Minecraft.getInstance();
        if(++ticks==1){
            mc.options.renderDistance().set(4);mc.options.hideGui=true;mc.options.pauseOnLostFocus=false;mc.setScreen(null);
            mc.getSingleplayerServer().execute(()->{
                var server=mc.getSingleplayerServer();var level=server.overworld();
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"time set 6000");
                server.setWeatherParameters(6000,0,false,false);
                for(BlockPos pos:BlockPos.betweenClosed(CHEST.offset(-3,0,-3),CHEST.offset(3,4,26)))level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
                for(BlockPos pos:BlockPos.betweenClosed(CHEST.offset(-3,-1,-3),CHEST.offset(3,-1,26)))level.setBlockAndUpdate(pos,Blocks.GRASS_BLOCK.defaultBlockState());
                level.setBlockAndUpdate(CHEST,Content.block("void_chest").defaultBlockState());
                level.setBlockAndUpdate(CHEST.above(),Content.block("void_interface").defaultBlockState());
            });
        }
        for(int i=0;i<DISTANCES.length;i++){
            int distance=DISTANCES[i];
            if(ticks==20+i*60)mc.getSingleplayerServer().execute(()->{
                var level=mc.getSingleplayerServer().overworld();
                for(var player:mc.getSingleplayerServer().getPlayerList().getPlayers()){
                    player.setGameMode(GameType.CREATIVE);player.getAbilities().flying=true;player.onUpdateAbilities();
                    player.teleportTo(level,10.5,120,.5+distance,Set.of(),180,8,true);
                }
            });
            if(ticks==65+i*60){
                Screenshot.grab(mc.gameDirectory,"void-chest-"+distance+".png",mc.getMainRenderTarget(),1,message->Thaumcraft.LOG.info("Void chest: {}",message.getString()));
                var sample=mc.gameRenderer.projectPointToScreen(new net.minecraft.world.phys.Vec3(10.22,120.25,1.001));
                Screenshot.takeScreenshot(mc.getMainRenderTarget(),image->{
                    int color;
                    try(image){color=image.getPixel((int)((sample.x+1)*image.getWidth()/2),(int)((1-sample.y)*image.getHeight()/2));}
                    mc.execute(()->{
                        int r=color>>16&255,g=color>>8&255,b=color&255;
                        if(g>r+10&&g>b+10)throw new AssertionError("Grass visible through void chest at distance "+distance);
                        checkedImages++;
                    });
                });
                var batches=LegacyVisuals.render(mc.level.getBlockEntity(CHEST),.5f,0xf000f0);
                RenderParityChecks.verifyVoidSubmissions(batches);
                var texture=Thaumcraft.id("textures/legacy/"+(distance==3?"tunnel.png":"particlefield32.png"));
                var faces=batches.stream().filter(b->b.texture().equals(texture)).toList();
                if(faces.isEmpty())throw new AssertionError("Missing void surface at distance "+distance);
                for(var face:faces){
                    var vertices=face.vertices();
                    for(int v=0;v<vertices.size();v+=4){
                        var quad=vertices.subList(v,v+4);
                        if(quad.stream().map(p->p.u()+","+p.v()).distinct().count()<4)
                            throw new AssertionError("Void texture collapses to one sample at distance "+distance);
                    }
                }
            }
        }
        if(ticks==260){
            if(checkedImages!=DISTANCES.length)throw new AssertionError("Missing void chest framebuffer checks");
            Thaumcraft.LOG.info("THAUMCRAFT_VOID_CHEST_PASS distances=3,6,10,24");mc.stop();
        }
    }
}
