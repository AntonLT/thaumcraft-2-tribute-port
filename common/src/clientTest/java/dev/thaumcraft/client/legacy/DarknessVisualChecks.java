package dev.thaumcraft.client.legacy;

import dev.thaumcraft.Thaumcraft;
import dev.thaumcraft.content.Content;
import dev.thaumcraft.machine.MachineBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import java.util.*;

/** Exercise the real once-per-tick lightning path and inspect its original shape. */
public final class DarknessVisualChecks {
    private static int stage,ticks,deadline;
    private static BlockPos pos,source;
    private static boolean finished;
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError("Darkness client: "+message);}
    private static Map field(Class<?> owner,String name){try{var f=owner.getDeclaredField(name);f.setAccessible(true);return (Map)f.get(null);}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
    private static Object value(Object record,String name){try{var m=record.getClass().getDeclaredMethod(name);m.setAccessible(true);return m.invoke(record);}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
    public static boolean tick(){
        if(finished)return true;var mc=Minecraft.getInstance();var server=mc.getSingleplayerServer();ticks++;check(ticks<240,"fixture timeout");
        if(stage==0){pos=mc.player.blockPosition().offset(2,-1,-3);source=pos.west(3);server.execute(()->{
            var level=server.overworld();level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());level.setBlockAndUpdate(pos,Content.block("darkness_generator").defaultBlockState());
            for(int y=0;y<4;y++)level.setBlockAndUpdate(source.above(y),Content.block("eldritch_monolith").defaultBlockState());
            ((MachineBlockEntity)level.getBlockEntity(pos)).setItem(0,new ItemStack(Items.WHEAT_SEEDS,64));
            for(var player:server.getPlayerList().getPlayers())player.teleportTo(level,pos.getX()+.5,pos.getY()+3,pos.getZ()+8,java.util.Set.of(),180,20,true);
        });stage=1;deadline=ticks+20;return false;}
        if(ticks<deadline)return false;
        var machine=mc.level.getBlockEntity(pos) instanceof MachineBlockEntity m?m:null;
        if(stage==1){
            if(machine==null||machine.visualDarknessMonolith()==null)return false;
            source=machine.visualDarknessMonolith();
            int moon=mc.level.environmentAttributes().getValue(net.minecraft.world.attribute.EnvironmentAttributes.MOON_PHASE,net.minecraft.world.phys.Vec3.ZERO).index();
            int bound=50+Math.abs(moon-4)*4+mc.level.getMaxLocalRawBrightness(pos);float partial=-1;
            for(int i=0;i<100;i++){float candidate=i/100f;float time=mc.level.getGameTime()+candidate;if(new Random(pos.asLong()^(long)(time*100)).nextInt(bound)==0){partial=candidate;break;}}
            if(partial<0)return false;
            LegacyVisuals.render(machine,partial,0xf000f0);field(LegacyVisuals.class,"EFFECT_TICKS").remove(pos);field(LegacyLightning.class,"BOLTS").remove(pos);
            LegacyVisuals.render(machine,partial,0xf000f0);var bolts=(List<?>)field(LegacyLightning.class,"BOLTS").get(pos);
            check(bolts!=null&&bolts.size()==1,"controlled seed creates one connecting bolt");var bolt=bolts.getFirst();var shape=(LightningShape)value(bolt,"shape");
            check((int)value(bolt,"type")==5&&shape.multiplier==2&&shape.particleMaxAge>=2&&shape.particleMaxAge<=4,"original dark type duration and multiplier");
            check(shape.start.x==source.getX()-pos.getX()+.5&&shape.start.z==source.getZ()-pos.getZ()+.5&&shape.start.y>=source.getY()-pos.getY()+.75&&shape.start.y<source.getY()-pos.getY()+4.75,"source-relative monolith endpoint");
            check(shape.end.x==.5&&shape.end.y==.75&&shape.end.z==.5,"generator endpoint");
            LegacyVisuals.render(machine,partial,0xf000f0);check(((List<?>)field(LegacyLightning.class,"BOLTS").get(pos)).size()==1,"second render same game tick cannot duplicate bolt");
            stage=2;deadline=ticks+Math.max(1,-shape.particleAge-2);return false;
        }
        if(stage==2){
            Screenshot.grab(mc.gameDirectory,"thaumcraft-darkness-lightning.png",mc.getMainRenderTarget(),1,r->{});
            server.execute(()->((MachineBlockEntity)server.overworld().getBlockEntity(pos)).setItem(0,ItemStack.EMPTY));stage=3;deadline=ticks+15;return false;
        }
        check(machine.visualDarknessMonolith()==null,"source clears on client after invalid input");field(LegacyLightning.class,"BOLTS").remove(pos);field(LegacyVisuals.class,"EFFECT_TICKS").remove(pos);
        LegacyVisuals.render(machine,.5f,0xf000f0);check(!field(LegacyLightning.class,"BOLTS").containsKey(pos),"inactive generator creates no bolt");
        server.execute(()->{var level=server.overworld();level.removeBlock(pos,false);for(int y=0;y<4;y++)level.removeBlock(source.above(y),false);});
        Thaumcraft.LOG.info("THAUMCRAFT_DARKNESS_VISUAL_PASS source_packets deterministic_endpoints once_per_tick idle_clear");finished=true;return true;
    }
}
