package dev.thaumcraft.client.legacy;

import dev.thaumcraft.Thaumcraft;
import dev.thaumcraft.content.Content;
import dev.thaumcraft.machine.MachineBlockEntity;
import dev.thaumcraft.network.EquipmentEffect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import java.util.*;

/** Real effect packets and particle output for combat marks, doorway wisps and upgraded machines. */
public final class CombatMachineVisualChecks {
    private static final Set<Integer> received=new HashSet<>();
    private static java.util.function.Consumer<EquipmentEffect> receiver;
    private static int ticks,checks,startTick;
    private static boolean finished,respawnRequested;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError("Combat/machine visuals: "+message);}
    private static double field(Object object,Class<?> type,String name){
        try{var field=type.getDeclaredField(name);field.setAccessible(true);return ((Number)field.get(object)).doubleValue();}
        catch(ReflectiveOperationException e){throw new AssertionError(e);}
    }
    private static Queue<?> pending(){
        try{var engine=Minecraft.getInstance().particleEngine;var field=engine.getClass().getDeclaredField("particlesToAdd");field.setAccessible(true);return (Queue<?>)field.get(engine);}
        catch(ReflectiveOperationException e){throw new AssertionError(e);}
    }
    public static boolean tick(){
        if(finished)return true;
        var mc=Minecraft.getInstance();
        if(++ticks>200)throw new AssertionError("Visual effect packets timed out: "+received);
        if(mc.player.isDeadOrDying()){
            if(!respawnRequested){mc.player.respawn();respawnRequested=true;}
            startTick=ticks;return false;
        }
        if(receiver==null){
            startTick=ticks;
            mc.setScreen(null);
            receiver=EquipmentEffect.receiver;
            EquipmentEffect.receiver=effect->{
                int kind=effect.kind();
                if(kind<EquipmentEffect.VAMPIRIC||kind>EquipmentEffect.VOID_POOF){receiver.accept(effect);return;}
                var pending=pending();int before=pending.size();receiver.accept(effect);
                boolean low=dev.thaumcraft.PortConfig.lowGfx||!new LegacyCompat.GameSettings().fancyGraphics;
                int count=kind==EquipmentEffect.VOID_POOF?(low?5:10):1;
                check(pending.size()==before+count,"Packet kind "+kind+" creates its original particle count");
                for(int i=before;i<pending.size();i++){
                    var particle=pending.toArray()[i];check(particle instanceof LegacyParticle,"Packet uses recovered particle renderer");
                    double gravity=kind==EquipmentEffect.VAMPIRIC?.1:kind==EquipmentEffect.SOUL_MARK?-.1:kind==EquipmentEffect.BONE_MARK?.05:0;
                    check(Math.abs(field(particle,Particle.class,"gravity")-gravity)<1e-5,"Mark gravity matches the original");
                    if(kind==EquipmentEffect.VOID_POOF)try{var silent=LegacyParticle.class.getDeclaredField("tinkle");silent.setAccessible(true);check(!silent.getBoolean(particle),"Doorway wisps remain silent");}catch(ReflectiveOperationException e){throw new AssertionError(e);}
                }
                received.add(kind);
            };
            return false;
        }
        // Allow the integrated player to finish joining the level before broadcasting to its player list.
        if(ticks==startTick+20){
            mc.getSingleplayerServer().execute(()->{
                var player=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                check(player!=null&&player.level().players().contains(player),"Packet observer has joined the server level: player="+player+", removed="+player.isRemoved()+", level="+player.level().dimension()+", levelPlayers="+player.level().players()+", listed="+mc.getSingleplayerServer().getPlayerList().getPlayers());
                for(int kind=EquipmentEffect.VAMPIRIC;kind<=EquipmentEffect.VOID_POOF;kind++)EquipmentEffect.send(player.level(),kind,player.position().add(0,1,0));
            });
            return false;
        }
        if(received.size()!=4)return false;
        try{
            var ambient=LegacyVisuals.class.getDeclaredMethod("ambientParticles",LegacyCompat.World.class,BlockPos.class);ambient.setAccessible(true);
            var pos=mc.player.blockPosition().above(3);var saved=mc.level.getBlockState(pos);
            check(saved.isAir(),"Temporary machine fixture uses an empty client block");
            try{
                for(String id:List.of("thaumic_infuser","dark_infuser","thaumic_restorer")){
                    mc.level.setBlock(pos,Content.block(id).defaultBlockState(),3);
                    var machine=(MachineBlockEntity)mc.level.getBlockEntity(pos);
                    for(int state=0;state<3;state++){
                        var tag=new CompoundTag();tag.putBoolean("visual_only",true);tag.putBoolean("working",state!=0);tag.putBoolean("infuser_processing",state!=0);tag.putInt("upgrades",state==1?0:1);
                        machine.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING,mc.level.registryAccess(),tag));
                        var pending=pending();int before=pending.size();var world=new LegacyCompat.World(mc.level,0);world.effectsAllowed=true;
                        LegacyCompat.capture(world,pos,0xf000f0,mc.level.getGameTime(),()->{
                            try{ambient.invoke(null,world,pos);}catch(ReflectiveOperationException e){throw new AssertionError(e);}
                            if(id.equals("thaumic_infuser"))new TileInfuserRenderer().renderEntityAt((LegacyCompat.TileInfuser)LegacyVisuals.snapshot(world,pos),0,0,0,0);
                        });
                        check(pending.size()==before+(state==2?1:0),id+" emits exactly one flame only while active and upgraded");
                        if(state==2){
                            var flame=pending.toArray()[before];check(flame.getClass().getSimpleName().equals("FlameParticle"),id+" uses a flame instead of a wisp");
                            check(field(flame,SingleQuadParticle.class,"rCol")==0&&field(flame,SingleQuadParticle.class,"gCol")==1&&field(flame,SingleQuadParticle.class,"bCol")==1,"Small flame retains the original green tint");
                            check(((SingleQuadParticle)flame).getQuadSize(0)<.03,"Small flame is scaled down");
                            double inset=id.equals("thaumic_restorer")?.25:.1,x=field(flame,Particle.class,"x")-pos.getX(),z=field(flame,Particle.class,"z")-pos.getZ();
                            // Vanilla RisingParticle adds up to .05 of position jitter during construction.
                            check(Math.abs(field(flame,Particle.class,"y")-pos.getY()-1.15)<.051&&(Math.abs(x-inset)<.051||Math.abs(x-1+inset)<.051)&&(Math.abs(z-inset)<.051||Math.abs(z-1+inset)<.051),"Small flame starts at an original top corner");
                        }
                    }
                }
            }finally{mc.level.setBlock(pos,saved,3);}
        }catch(ReflectiveOperationException e){throw new AssertionError(e);}
        finished=true;EquipmentEffect.receiver=receiver;Thaumcraft.LOG.info("THAUMCRAFT_COMBAT_MACHINE_VISUAL_PASS checks={}",checks);return true;
    }
}
