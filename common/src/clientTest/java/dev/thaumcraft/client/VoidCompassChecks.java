package dev.thaumcraft.client;

import com.mojang.blaze3d.platform.NativeImage;
import dev.thaumcraft.Thaumcraft;
import dev.thaumcraft.content.Content;
import dev.thaumcraft.world.EldritchIndex;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Golden pixels/equations plus real server lookup, packet, shared sound and resource-reload checks. */
public final class VoidCompassChecks {
    private static int checks,ticks,stage,sounds;
    private static volatile boolean ready;
    private static java.util.concurrent.CompletableFuture<Void> reload;
    private static final BlockPos CORE=new BlockPos(12001,280,12001);
    private static final net.minecraft.client.sounds.SoundEventListener LISTENER=(sound,event,range)->{if(sound.getIdentifier().equals(Thaumcraft.id("monolithfound")))sounds++;};
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError("Void Compass: "+message);}
    private static void near(double actual,double expected){check(Math.abs(actual-expected)<1e-12,"expected "+expected+", got "+actual);}
    public static void verifyEquations(){
        var compass=new VoidCompassTexture();
        check(compass.updateTarget(true,new BlockPos(10,50,20)),"first target sounds");
        check(!compass.updateTarget(true,new BlockPos(11,50,20)),"X-only change is silent");
        check(!compass.updateTarget(true,new BlockPos(11,60,21)),"Z-only change is silent");
        check(compass.updateTarget(true,new BlockPos(12,60,22)),"both axes changing sounds");
        check(compass.updateTarget(true,null),"losing target sounds");
        check(!compass.updateTarget(true,null),"continued absence is silent");
        compass.updateTarget(true,CORE);check(!compass.updateTarget(false,null),"removing compass is silent");
        near(compass.direction(Vec3.ZERO,0,true,()->.25),Math.PI/2);
        compass.updateTarget(true,new BlockPos(0,0,0));
        near(compass.direction(new Vec3(.5,0,-10),90,true,()->.9),-Math.PI/2);
        near(compass.direction(Vec3.ZERO,90,false,()->.75),Math.PI*1.5);
        compass.advance(Math.PI);near(compass.angle,-.08);near(compass.velocity,-.08);
        compass=new VoidCompassTexture();
        for(int i=0;i<200;i++)compass.advance(Math.sin(i*.17)*20);
        near(compass.angle,-.11351863477239427);near(compass.velocity,.0401588526326254);
        // Golden ARGB hashes from the original TextureVoidCompassFX rasterizer, not 32 angle bins.
        int[][] golden={{-1364515512,1377558378,1495413219,2022311695},{1996396799,1402306433,-1149605270,1745132154}};
        double[] angles={0,.37,2.4,5.8};
        for(int scale=0;scale<2;scale++)for(int i=0;i<angles.length;i++)try(var image=new NativeImage(16<<scale,16<<scale,true)){
            VoidCompassTexture.drawNeedle(image,angles[i]);check(java.util.Arrays.hashCode(image.getPixels())==golden[scale][i],"original needle pixels at size/angle "+scale+"/"+angles[i]);
        }
    }
    public static boolean tick(){
        var mc=Minecraft.getInstance();check(++ticks<500,"client fixture timeout");
        if(stage==0){
            verifyEquations();mc.getSoundManager().addListener(LISTENER);
            mc.getSingleplayerServer().execute(()->{
                var level=mc.getSingleplayerServer().overworld();
                var a=CORE.offset(-1,0,1);var b=CORE.offset(1,0,-1);var absent=CORE.above();
                for(var pos:java.util.List.of(a,b,CORE))level.setBlockAndUpdate(pos,Content.block("eldritch_core").defaultBlockState());
                level.setBlockAndUpdate(absent,Blocks.AIR.defaultBlockState());
                var index=new EldritchIndex();index.record(b);index.record(a);index.record(absent);
                Vec3 between=new Vec3(12000.2,280.5,12000.8);
                check(index.compassTarget(level,between).orElseThrow().equals(a),"exact player coordinates choose nearest center, including Y");
                level.setBlockAndUpdate(a,Blocks.AIR.defaultBlockState());
                check(index.compassTarget(level,between).orElseThrow().equals(b),"missing tile skipped");
                level.setBlockAndUpdate(b,Blocks.AIR.defaultBlockState());
                check(index.compassTarget(level,between).isEmpty(),"no surviving tiles means no target");
                EldritchIndex.get(level).record(CORE);
                var player=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);player.getInventory().clearContent();
                player.teleportTo(level,12000.2,280.5,12000.8,java.util.Set.of(),0,0,true);
                player.getAbilities().flying=true;player.onUpdateAbilities();
                var stack=new ItemStack(Content.item("void_compass"));
                stack.set(DataComponents.LODESTONE_TRACKER,new LodestoneTracker(java.util.Optional.of(GlobalPos.of(level.dimension(),BlockPos.ZERO)),false));
                player.setItemInHand(InteractionHand.MAIN_HAND,stack);player.setItemInHand(InteractionHand.OFF_HAND,stack.copy());
                check(stack.getItem().use(level,player,InteractionHand.MAIN_HAND)==InteractionResult.PASS,"right-click has no compass action");
                ready=true;
            });stage=1;return false;
        }
        if(!ready)return false;
        if(stage==1){
            if(VoidCompassTexture.INSTANCE.lastX!=CORE.getX()+.5||VoidCompassTexture.INSTANCE.lastZ!=CORE.getZ()+.5)return false;
            check(sounds==1,"two compasses produce one shared discovery sound");
            var state=new ItemStackRenderState();new ItemModelResolver(mc.getModelManager()).updateForTopItem(state,mc.player.getMainHandItem(),ItemDisplayContext.GUI,mc.level,mc.player,0);
            check(state.pickParticleMaterial(net.minecraft.util.RandomSource.create(0)).sprite().contents().name().equals(Thaumcraft.id("item/atlas_61")),"saved lodestone component cannot select vanilla compass frames");
            reload=mc.reloadResourcePacks();stage=2;return false;
        }
        if(!reload.isDone())return false;
        reload.join();
        if(stage++==2){ticks=0;return false;}
        if(ticks<70)return false;
        check(sounds==1,"unchanged target and resource reload do not repeat sound");
        check(mc.getConnection()!=null,"target packets retain connection");mc.getSoundManager().removeListener(LISTENER);
        Thaumcraft.LOG.info("THAUMCRAFT_COMPASS_PASS checks={} shared_sound target_packets original_pixels reload",checks);return true;
    }
    public static void main(String[] args){verifyEquations();System.out.println("THAUMCRAFT_COMPASS_EQUATIONS_PASS checks="+checks);}
}
