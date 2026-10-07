package dev.thaumcraft.test;

import dev.thaumcraft.Thaumcraft;
import dev.thaumcraft.content.VisualEffects;
import dev.thaumcraft.entity.CarpetEntity;
import dev.thaumcraft.entity.ModEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.Vec3;

/** Real client/server input, pose, particles and dismount interaction. */
public final class CarpetClientChecks {
    private static int ticks,checks;
    private static Vec3 start;
    private static void check(boolean condition,String message){checks++;if(!condition)throw new AssertionError("Carpet client: "+message);}
    public static boolean tick(){
        var mc=Minecraft.getInstance();ticks++;
        if(ticks==1){
            mc.setScreen(null);mc.options.pauseOnLostFocus=false;mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
            mc.getSingleplayerServer().execute(()->{
                var player=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
                player.stopRiding();player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
                player.teleportTo(player.level(),8,180,8,java.util.Set.of(),0,0,true);
                var carpet=ModEntities.CARPET.create(player.level(),EntitySpawnReason.COMMAND);
                carpet.setPos(player.position());player.level().addFreshEntity(carpet);player.startRiding(carpet);
            });
        }
        if(ticks==60){
            check(mc.player.getVehicle() instanceof CarpetEntity,"Server mount reaches the client");
            var carpet=(CarpetEntity)mc.player.getVehicle();start=carpet.position();
            check(!carpet.isLocalInstanceAuthoritative(),"Client never claims movement authority");
            var state=new HumanoidRenderState();
            HumanoidMobRenderer.extractHumanoidRenderState(mc.player,state,0,mc.getItemModelResolver());
            check(!state.isPassenger,"Actual player render extraction uses standing legs");
            try{
                var field=mc.particleEngine.getClass().getDeclaredField("particlesToAdd");field.setAccessible(true);
                var pending=(java.util.Queue<?>)field.get(mc.particleEngine);int before=pending.size();
                carpet.setDeltaMovement(.3,0,0);VisualEffects.entity.accept(carpet);
                check(pending.size()-before==19,"Moving carpet emits the original speed-dependent sparkle count");
                for(int i=before;i<pending.size();i++)check(pending.toArray()[i].getClass().getName().equals("dev.thaumcraft.client.legacy.LegacyParticle"),"Carpet uses the recovered sparkle renderer");
                carpet.setDeltaMovement(Vec3.ZERO);before=pending.size();
                for(int i=0;i<20;i++){carpet.tickCount++;VisualEffects.entity.accept(carpet);}
                check(pending.size()>before&&pending.size()<=before+60,"Idle carpet emits intermittent sparkles");
            }catch(ReflectiveOperationException e){throw new AssertionError(e);}
            mc.options.keyUp.setDown(true);mc.options.keyJump.setDown(true);
        }
        if(ticks==100){
            var carpet=mc.player.getVehicle();check(carpet!=null&&carpet.getY()>start.y+.5&&carpet.position().distanceTo(start)>2,"Real input packets fly the carpet forward and up");
            mc.options.keyUp.setDown(false);mc.options.keyJump.setDown(false);mc.options.keyShift.setDown(true);start=carpet.position();
        }
        if(ticks==150){
            check(mc.player.getVehicle() instanceof CarpetEntity&&mc.player.getVehicle().getY()<start.y,"Sneak descends without client/server dismount");
            mc.options.keyShift.setDown(false);mc.player.setXRot(0);
            net.minecraft.client.Screenshot.grab(mc.gameDirectory,"thaumcraft-carpet-standing.png",mc.getMainRenderTarget(),1,result->{});
        }
        if(ticks==170){
            mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);mc.player.setXRot(90);
            try{var method=Minecraft.class.getDeclaredMethod("startUseItem");method.setAccessible(true);method.invoke(mc);}
            catch(ReflectiveOperationException e){throw new AssertionError(e);}
        }
        if(ticks<190)return false;
        check(!mc.player.isPassenger(),"Looking down and using the carpet sends the real dismount interaction");
        Thaumcraft.LOG.info("THAUMCRAFT_CARPET_CLIENT_PASS checks={}",checks);return true;
    }
}
