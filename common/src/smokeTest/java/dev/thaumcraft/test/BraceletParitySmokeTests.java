package dev.thaumcraft.test;

import com.mojang.authlib.GameProfile;
import dev.thaumcraft.content.Content;
import dev.thaumcraft.gameplay.ArcaneWorldData;
import dev.thaumcraft.item.ItemState;
import dev.thaumcraft.machine.MachineBlock;
import dev.thaumcraft.machine.MachineBlockEntity;
import dev.thaumcraft.network.SealEffect;
import dev.thaumcraft.world.SealPortals;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import java.util.*;

/** Bracelet regressions against the original ItemVoidBracelet behavior. */
public final class BraceletParitySmokeTests {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError("Bracelet parity: "+message);}
    private static final class Player extends ServerPlayer {
        final List<String> chat=new ArrayList<>();
        Player(MinecraftServer server,ServerLevel level){super(server,level,new GameProfile(UUID.randomUUID(),"BraceletParity"),ClientInformation.createDefault());
            new net.minecraft.server.network.ServerGamePacketListenerImpl(server,new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),this,net.minecraft.server.network.CommonListenerCookie.createInitial(getGameProfile(),false));
        }
        @Override public void sendSystemMessage(Component message,boolean overlay){check(!overlay,"Feedback appears in chat");chat.add(message.getString());}
    }
    private static MachineBlockEntity seal(ServerLevel level,BlockPos pos,Direction facing,int channel){
        level.getChunkAt(pos);level.setBlockAndUpdate(pos.relative(facing.getOpposite()),Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(pos,Content.block("arcane_seal").defaultBlockState().setValue(MachineBlock.FACING,facing));
        var seal=(MachineBlockEntity)level.getBlockEntity(pos);
        for(int i=0;i<3;i++){int rune=new int[]{0,1,channel}[i];if(rune<0)continue;
            var entry=Content.ENTRIES.stream().filter(e->e.source_class().equals("ItemRunicEssence")&&e.meta()==rune).findFirst().orElseThrow();
            seal.setItem(18+i,new ItemStack(Content.item(entry.id())));
        }
        SealPortals.updateRunes(level,seal);return seal;
    }
    public static int run(MinecraftServer server){
        checks=0;var level=server.overworld();var pos=new BlockPos(720,280,720);var source=pos.west(96).getCenter();
        level.getChunkAt(BlockPos.containing(source));var player=new Player(server,level);player.setPos(source);level.addNewPlayer(player);
        var bracelet=new ItemStack(Content.item("void_bracelet"));player.setItemInHand(InteractionHand.MAIN_HAND,bracelet);
        var effects=new ArrayList<SealEffect>();var sender=SealEffect.sender;SealEffect.sender=(recipient,effect)->{if(recipient==player)effects.add(effect);};
        try{
            var target=seal(level,pos,Direction.EAST,5);
            var context=new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(pos.getCenter(),Direction.EAST,pos,false));
            check(bracelet.getItem().useOn(context)==InteractionResult.SUCCESS&&ItemState.getInt(bracelet,"seal_rune",-1)==5,"Link selects the third rune");
            check(player.chat.equals(List.of("You've linked the bracelet to a new network.")),"Original link message");player.chat.clear();
            check(bracelet.getItem().useOn(context)==InteractionResult.PASS&&player.chat.isEmpty(),"Unchanged network falls through without feedback");
            target.toggle();level.setBlockAndUpdate(pos.east(),Blocks.STONE.defaultBlockState());level.setBlockAndUpdate(pos.east().below(),Blocks.STONE.defaultBlockState());
            check(!SealPortals.travelByBracelet(player,5)&&player.position().equals(source),"Occupied landing refuses travel instead of embedding the player");
            level.setBlockAndUpdate(pos.east(),Blocks.AIR.defaultBlockState());
            player.setKnownMovement(new Vec3(.4,-.6,.8));player.fallDistance=7;player.setXRot(23);
            var aura=ArcaneWorldData.get(level);aura.addVibes(level,pos,0,-100);aura.addVibes(level,BlockPos.containing(source),0,-100);
            check(bracelet.getItem().use(level,player,InteractionHand.MAIN_HAND)==InteractionResult.SUCCESS,"Disabled seal remains a valid destination");
            check(player.position().equals(Vec3.atBottomCenterOf(pos.east()))&&player.getYRot()==270&&player.getXRot()==23,"Wall arrival stands on the landing block with original rotation");
            check(player.getDeltaMovement().equals(new Vec3(0,-.6,0))&&player.fallDistance==7,"Vertical motion and fall distance survive teleport");
            check(target.progress==40&&!player.getCooldowns().isOnCooldown(bracelet)&&!player.getCooldowns().isOnCooldown(new ItemStack(Content.item("arcane_seal_item"))),"Only destination seal receives the forty-tick delay");
            check(aura.aura(level,pos).badVibes()==25&&aura.aura(level,BlockPos.containing(source)).badVibes()==25,"Both ends receive 25 bad vibes");
            check(effects.size()==2&&effects.stream().allMatch(e->e.kind()==SealEffect.POOF)&&effects.get(0).origin().equals(source.add(-.5,-.5,-.5))&&effects.get(1).origin().equals(player.position().add(-.5,-.5,-.5)),"Departure and arrival poofs use original coordinates");
            check(bracelet.getItem().use(level,player,InteractionHand.MAIN_HAND)==InteractionResult.SUCCESS,"Bracelet can be reused immediately");
            level.setBlockAndUpdate(pos.east().below(),Blocks.AIR.defaultBlockState());
            check(SealPortals.travelByBracelet(player,5)&&player.position().equals(Vec3.atBottomCenterOf(pos.east().below())),"Wall arrival drops one block over air, without needing ground");
            level.setBlockAndUpdate(pos.east().below(),Blocks.WATER.defaultBlockState());
            check(SealPortals.travelByBracelet(player,5)&&player.position().equals(Vec3.atBottomCenterOf(pos.east())),"Water below does not count as air");
            level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
            for(var facing:Direction.values()){
                target=seal(level,pos,facing,5);level.setBlockAndUpdate(pos.relative(facing),Blocks.AIR.defaultBlockState());
                if(facing.getAxis().isHorizontal())level.setBlockAndUpdate(pos.relative(facing).below(),Blocks.STONE.defaultBlockState());
                check(SealPortals.travelByBracelet(player,5)&&player.position().equals(Vec3.atBottomCenterOf(pos.relative(facing)))&&player.getYRot()==(facing.getAxis().isHorizontal()?facing.toYRot():0),"Original arrival for "+facing);
                level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
            }
            check(!SealPortals.travelByBracelet(player,5),"Empty network fails");
            bracelet.getItem().use(level,player,InteractionHand.MAIN_HAND);
            check(player.chat.equals(List.of("No valid destinations found.")),"Original failure message");
            target=seal(level,pos,Direction.EAST,5);var invalid=pos.east(16);
            // Register a stale entry without a block entity, as can occur in saved network data.
            var stale=new MachineBlockEntity(invalid,target.getBlockState());
            stale.setItem(18,target.getItem(18).copy());stale.setItem(19,target.getItem(19).copy());stale.setItem(20,target.getItem(20).copy());
            SealPortals.updateRunes(level,stale);
            level.getRandom().setSeed(216);int successes=0;for(int i=0;i<40;i++)if(SealPortals.travelByBracelet(player,5))successes++;
            check(successes>0&&successes<40,"One random candidate per use, with no retry after stale selection");SealPortals.remove(level,invalid);
            var nether=server.getLevel(net.minecraft.world.level.Level.NETHER);seal(nether,pos.below(100),Direction.EAST,4);
            check(!SealPortals.travelByBracelet(player,4),"Other dimensions are excluded");nether.setBlockAndUpdate(pos.below(100),Blocks.AIR.defaultBlockState());
        }finally{
            SealEffect.sender=sender;player.discard();
            for(BlockPos p:BlockPos.betweenClosed(pos.offset(-1,-2,-1),pos.offset(1,2,1)))level.setBlockAndUpdate(p,Blocks.AIR.defaultBlockState());
            SealPortals.remove(level,pos);SealPortals.remove(level,pos.east(16));
        }
        return checks;
    }
}
