package dev.thaumcraft.client.legacy;

import dev.thaumcraft.network.SealEffect;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import static dev.thaumcraft.client.legacy.LegacyCompat.*;

/** Exercises actual effect packets/renderers and the portal's client animation clock. */
public final class SealVisualChecks {
    private static TileSeal tile;
    private static long previousTick=Long.MIN_VALUE;
    private static int ticks,received,checks;
    private static float expected;
    private static java.util.function.Consumer<SealEffect> receiver;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError("Seal visual: "+message);}
    public static boolean tick(){
        var mc=Minecraft.getInstance();
        if(tile==null){
            tile=new TileSeal();tile.worldObj=new World(mc.level,0);BlockPos pos=mc.player.blockPosition().above(10);tile.xCoord=pos.getX();tile.yCoord=pos.getY();tile.zCoord=pos.getZ();
            receiver=SealEffect.receiver;
            SealEffect.receiver=event->{
                try{
                    var field=mc.particleEngine.getClass().getDeclaredField("particlesToAdd");field.setAccessible(true);var pending=(java.util.Queue<?>)field.get(mc.particleEngine);int before=pending.size();
                    receiver.accept(event);
                    boolean low=dev.thaumcraft.PortConfig.lowGfx||!new GameSettings().fancyGraphics;
                    int expected=switch(event.kind()){case SealEffect.BOOST,SealEffect.ANCHOR,SealEffect.NULLIFY->1;case SealEffect.DETECT->low?3:6;case SealEffect.HEAL->5;case SealEffect.HYDRATE,SealEffect.TILL->low?5:16;case SealEffect.GROW,SealEffect.REPLANT,SealEffect.SEED->low?5:10;case SealEffect.PICKUP->10;default->low?3:6;};
                    check(pending.size()==before+expected,"Original particle count for effect "+event.kind());
                    if(event.kind()!=SealEffect.HARVEST&&event.kind()!=SealEffect.POOF)for(int i=before;i<pending.size();i++)check(pending.toArray()[i] instanceof LegacyParticle,"Seal uses recovered particle renderer");
                    received|=1<<event.kind();
                }catch(ReflectiveOperationException e){throw new AssertionError(e);}
            };
            mc.getSingleplayerServer().execute(()->{
                var player=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());var effectPos=player.getEyePosition().add(player.getLookAngle().scale(2));
                for(int kind=0;kind<=SealEffect.SEED;kind++)SealEffect.send(player.level(),kind,effectPos,effectPos.add(.5,.5,.5));
            });
        }
        long now=mc.level.getGameTime();if(now==previousTick)return false;
        int steps=previousTick==Long.MIN_VALUE?1:(int)Math.clamp(now-previousTick,0,20);previousTick=now;ticks++;
        tile.portalOpen=ticks<25;tile.duplicatorCopyTime=ticks>=12&&ticks<25?40:0;
        for(int step=0;step<steps;step++){
            if(tile.portalOpen&&expected<1.4f)expected+=.15f;
            if((!tile.portalOpen||tile.duplicatorCopyTime>0)&&expected>0)expected-=.25f;
            expected=Math.clamp(expected,0,1.4f);
        }
        LegacyAnimation.apply(tile);check(Math.abs(tile.pSize-expected)<.0001f,"Portal growth/cooldown shrink at tick "+ticks);
        if(ticks==11)check(tile.pSize==1.4f,"Open portal reaches full size");
        if(ticks==24)check(tile.pSize==0,"Open portal shrinks shut during travel cooldown");
        if(ticks<30)return false;
        check(received==(1<<(SealEffect.SEED+1))-1,"All thirteen server effect packets reached the client");
        SealEffect.receiver=receiver;
        dev.thaumcraft.Thaumcraft.LOG.info("THAUMCRAFT_SEAL_CLIENT_PASS checks={} packets=13",checks);return true;
    }
}
