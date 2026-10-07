package dev.thaumcraft.client.legacy;

import dev.thaumcraft.content.Content;
import dev.thaumcraft.entity.RelicProjectile;
import dev.thaumcraft.network.EquipmentEffect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.SoundEventListener;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import java.util.*;

/** Real server effect delivery, client audio and the recovered water billboard. */
public final class WandVisualChecks {
    private static boolean started,trade,water;
    private static int checks;
    private static long start;
    private static java.util.function.Consumer<EquipmentEffect> receiver;
    private static final Set<String> heard=new HashSet<>();
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError("Wand client: "+message);}
    private static final SoundEventListener listener=(sound,event,range)->heard.add(sound.getIdentifier().toString());
    public static boolean tick(){
        var mc=Minecraft.getInstance();
        if(!started){
            started=true;start=mc.level.getGameTime();mc.getSoundManager().addListener(listener);receiver=EquipmentEffect.receiver;
            EquipmentEffect.receiver=event->{
                if(event.kind()!=EquipmentEffect.TRADE){receiver.accept(event);return;}
                try{
                    var field=mc.particleEngine.getClass().getDeclaredField("particlesToAdd");field.setAccessible(true);var pending=(Queue<?>)field.get(mc.particleEngine);int before=pending.size();
                    receiver.accept(event);
                    boolean low=dev.thaumcraft.PortConfig.lowGfx||!new LegacyCompat.GameSettings().fancyGraphics;
                    check(pending.size()==before+(low?30:60),"Trade emits original six-face sparkle count");
                    for(int i=before;i<pending.size();i++)check(pending.toArray()[i] instanceof LegacyParticle,"Trade uses the recovered sparkle renderer");trade=true;
                }catch(ReflectiveOperationException e){throw new AssertionError(e);}
            };
            mc.getSingleplayerServer().execute(()->{
                var player=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());var level=player.level();player.stopRiding();player.setXRot(-60);var pos=player.blockPosition().offset(2,0,0);
                var tool=new ItemStack(Content.item("wand_of_equal_trade"));player.setItemInHand(InteractionHand.MAIN_HAND,tool);
                var use=new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(pos.getCenter(),Direction.UP,pos,false));
                level.setBlockAndUpdate(pos,Blocks.DIRT.defaultBlockState());player.setShiftKeyDown(true);tool.getItem().useOn(use);player.setShiftKeyDown(false);
                level.setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());level.setBlockAndUpdate(pos.above(),Blocks.AIR.defaultBlockState());player.getInventory().add(new ItemStack(Items.DIRT));tool.getItem().useOn(use);
                dev.thaumcraft.item.EqualTrade.tick(level);
                check(level.getBlockState(pos).is(Blocks.DIRT),"Server trade exchanged its block; state="+level.getBlockState(pos)+", held="+player.getMainHandItem()+", context="+use.getItemInHand());
                for(String name:List.of("fire","water")){
                    var wand=new ItemStack(Content.item("wand_of_"+name));player.setItemInHand(InteractionHand.MAIN_HAND,wand);player.startUsingItem(InteractionHand.MAIN_HAND);wand.getItem().onUseTick(level,player,wand,71999);player.stopUsingItem();
                }
                for(var entity:level.getEntitiesOfClass(RelicProjectile.class,player.getBoundingBox().inflate(4),e->e.mode()==4)){
                    entity.setPos(player.getEyePosition().add(0,3,0));entity.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
                    for(var clear:net.minecraft.core.BlockPos.betweenClosed(entity.blockPosition().offset(-1,-1,-1),entity.blockPosition().offset(1,1,1)))level.setBlockAndUpdate(clear,Blocks.AIR.defaultBlockState());
                }
                var bone=new ItemStack(Content.item("wand_of_bone"));player.setItemInHand(InteractionHand.MAIN_HAND,bone);bone.getItem().useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(pos.getCenter(),Direction.UP,pos,false)));
                level.setBlockAndUpdate(pos,Blocks.LAVA.defaultBlockState());RelicProjectile.waterImpact(level,new BlockHitResult(pos.getCenter(),Direction.UP,pos,false));
            });
        }
        for(var entity:mc.level.entitiesForRendering())if(entity instanceof RelicProjectile orb&&orb.mode()==4&&!water){
            var batches=LegacyEntityVisuals.effects(orb,0);
            check(orb.waterLifetime()>=48&&orb.waterLifetime()<=68&&orb.waterBlue()>=.7f&&orb.waterBlue()<=1,"Water visual parameters synchronize to client");
            check(batches.size()==1&&batches.getFirst().texture().getPath().endsWith("p_large.png"),"Water uses original texture");
            var batch=batches.getFirst();check(!batch.depthWrite()&&batch.sourceBlend()==770&&batch.blend()==1&&batch.vertices().size()==4,"Water retains additive transparent billboard");water=true;
        }
        if(mc.level.getGameTime()-start<60)return false;
        check(trade&&water,"Trade packet and water entity reached the client: trade="+trade+", water="+water);
        for(String sound:List.of("thaumcraft2tp:fireloop","minecraft:weather.rain","thaumcraft2tp:zap","minecraft:entity.skeleton.hurt","minecraft:entity.experience_orb.pickup","minecraft:block.lava.extinguish"))check(heard.contains(sound),"Received wand sound "+sound+" in "+heard);
        EquipmentEffect.receiver=receiver;mc.getSoundManager().removeListener(listener);
        dev.thaumcraft.Thaumcraft.LOG.info("THAUMCRAFT_WAND_CLIENT_PASS checks={}",checks);return true;
    }
}
