package dev.thaumcraft.test;

import dev.thaumcraft.content.Content;
import dev.thaumcraft.entity.*;
import dev.thaumcraft.item.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.*;
import java.util.*;

/** Regression checks for the reported wand experience differences. */
final class WandParitySmokeTests {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError("Wand parity: "+message);}
    private static ItemStack wand(String name){return new ItemStack(Content.item("wand_of_"+name));}
    private static UseOnContext context(ServerPlayer player,BlockPos pos){return new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(pos.getCenter(),Direction.UP,pos,false));}
    private static void select(ServerLevel level,ServerPlayer player,BlockPos pos,ItemStack wand){player.setItemInHand(InteractionHand.MAIN_HAND,wand);player.setShiftKeyDown(true);wand.getItem().useOn(context(player,pos));player.setShiftKeyDown(false);}
    static int run(ServerLevel level,ServerPlayer player){
        checks=0;var previous=player.position();var pos=new BlockPos(400,280,400);ArcanaParitySmokeTests.prepareEntities(level,pos);player.setPos(pos.getBottomCenter());
        player.stopUsingItem();player.getInventory().clearContent();
        var lightning=wand("lightning");
        check(lightning.getItem().getUseDuration(lightning,player)==2&&lightning.getItem().getUseAnimation(lightning)==ItemUseAnimation.BOW,"Lightning has the two-tick bow pose");
        var start=pos.getCenter().add(0,3,0);var bolt=LightningEffect.spawn(level,start,start.add(0,0,20),6,.3f,4);
        var middle=bolt.geometry().segments().stream().filter(s->s.branch==0&&s.number==64).findFirst().orElseThrow().start.position().add(start);
        var victim=EntityType.COW.create(level,EntitySpawnReason.COMMAND);victim.setPos(middle.add(0,-victim.getBbHeight()/2,0));level.addFreshEntity(victim);
        var bystander=EntityType.COW.create(level,EntitySpawnReason.COMMAND);bystander.setPos(middle.add(4,0,0));level.addFreshEntity(bystander);
        bolt.strike(level,player,5);
        check(victim.getHealth()==5&&bystander.getHealth()==10,"Lightning damages a segment intersection but not a nearby bystander");
        victim.discard();bystander.discard();bolt.discard();

        var first=wand("equal_trade");level.setBlockAndUpdate(pos,Blocks.DIRT.defaultBlockState());
        var beforeRespawn=new ServerPlayer(level.getServer(),level,player.getGameProfile(),ClientInformation.createDefault());
        new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(),new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),beforeRespawn,net.minecraft.server.network.CommonListenerCookie.createInitial(beforeRespawn.getGameProfile(),false));
        beforeRespawn.setPos(player.position());select(level,beforeRespawn,pos,first);
        var second=wand("equal_trade");player.setItemInHand(InteractionHand.MAIN_HAND,second);player.getInventory().add(new ItemStack(Items.DIRT,64));
        for(int x=0;x<20;x++)level.setBlockAndUpdate(pos.east(x),Blocks.STONE.defaultBlockState());
        second.getItem().useOn(context(player,pos));
        check(level.getBlockState(pos).is(Blocks.STONE),"A second wand shares the selection after player replacement, without immediate exchanges");
        EqualTrade.tick(level);
        check(level.getBlockState(pos).is(Blocks.DIRT)&&level.getBlockState(pos.east()).is(Blocks.STONE)&&second.getDamageValue()==1,"First helper tick exchanges exactly one block");
        EqualTrade.tick(level);EqualTrade.tick(level);
        check(second.getDamageValue()==1,"Child trader waits two ticks");
        EqualTrade.tick(level);check(second.getDamageValue()==2,"Child trader propagates after its delay");
        for(int i=0;i<100;i++)EqualTrade.tick(level);
        check(second.getDamageValue()==16&&java.util.stream.IntStream.range(0,20).filter(x->level.getBlockState(pos.east(x)).is(Blocks.DIRT)).count()==16,"Unenchanted operation stops at sixteen blocks");
        for(int x=0;x<20;x++)level.setBlockAndUpdate(pos.east(x),Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos,Blocks.CHEST.defaultBlockState());((ChestBlockEntity)level.getBlockEntity(pos)).setItem(0,new ItemStack(Items.EMERALD,7));
        second.getItem().useOn(context(player,pos));EqualTrade.tick(level);
        check(level.getBlockState(pos).is(Blocks.DIRT),"Equal Trade accepts a container source");
        check(level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(2)).stream().filter(e->e.getItem().is(Items.EMERALD)).mapToInt(e->e.getItem().getCount()).sum()==7,"Container exchange preserves its contents exactly once");
        level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(2)).forEach(Entity::discard);
        for(int i=0;i<8;i++)EqualTrade.tick(level);
        level.setBlockAndUpdate(pos,Blocks.CHEST.defaultBlockState());select(level,player,pos,second);
        level.setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());player.getInventory().add(new ItemStack(Items.CHEST));second.getItem().useOn(context(player,pos));EqualTrade.tick(level);
        check(level.getBlockEntity(pos) instanceof ChestBlockEntity,"Equal Trade can place a container replacement");
        for(int i=0;i<8;i++)EqualTrade.tick(level);
        var doubleSlab=Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE,SlabType.DOUBLE);var inventory=player.getInventory();
        level.setBlockAndUpdate(pos,doubleSlab);select(level,player,pos,second);level.setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());
        inventory.setItem(20,new ItemStack(Items.OAK_SLAB));second.getItem().useOn(context(player,pos));EqualTrade.tick(level);
        check(!player.isCreative()&&level.getBlockState(pos).is(Blocks.STONE)&&inventory.getItem(20).getCount()==1,"One slab cannot pay for a double slab and is kept");
        inventory.setItem(21,new ItemStack(Items.OAK_SLAB,2));second.getItem().useOn(context(player,pos));EqualTrade.tick(level);
        check(level.getBlockState(pos).equals(doubleSlab)&&!inventory.getItem(20).is(Items.OAK_SLAB)&&inventory.getItem(21).getCount()==1,"A double slab costs exactly two slabs across stacks");
        for(int i=0;i<8;i++)EqualTrade.tick(level);
        var topSlab=Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE,SlabType.TOP);
        level.setBlockAndUpdate(pos,topSlab);select(level,player,pos,second);level.setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());
        second.getItem().useOn(context(player,pos));EqualTrade.tick(level);
        check(level.getBlockState(pos).equals(topSlab)&&!inventory.getItem(21).is(Items.OAK_SLAB),"A single slab state keeps its half and costs one slab");
        for(int i=0;i<8;i++)EqualTrade.tick(level);
        level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());

        var bone=wand("bone");player.setItemInHand(InteractionHand.MAIN_HAND,bone);
        level.setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());level.setBlockAndUpdate(pos.above(),Blocks.STONE.defaultBlockState());level.setBlockAndUpdate(pos.above(2),Blocks.STONE.defaultBlockState());
        bone.getItem().useOn(context(player,pos));var allies=level.getEntitiesOfClass(SkeletonAlly.class,new AABB(pos).inflate(3));
        check(allies.size()==1&&bone.getDamageValue()==1,"Bone summons in occupied space and spends one charge");allies.forEach(Entity::discard);
        for(int y=0;y<=2;y++)level.setBlockAndUpdate(pos.above(y),Blocks.AIR.defaultBlockState());

        player.setYRot(0);player.setXRot(0);var water=wand("water");player.setItemInHand(InteractionHand.MAIN_HAND,water);player.startUsingItem(InteractionHand.MAIN_HAND);
        for(int i=0;i<20;i++)water.getItem().onUseTick(level,player,water,71999-i);
        check(water.getDamageValue()==1,"Water charges by elapsed time rather than the supplied tick counter");
        var orbs=level.getEntitiesOfClass(RelicProjectile.class,new AABB(pos).inflate(4),e->e.mode()==4);
        check(orbs.size()==20,"Water emits one orb per use tick");
        check(orbs.stream().allMatch(e->e.waterLifetime()>=48&&e.waterLifetime()<=68)&&orbs.stream().map(RelicProjectile::waterLifetime).distinct().count()>1,"Water has original randomized lifetime");
        check(orbs.stream().map(RelicProjectile::getDeltaMovement).distinct().count()>1&&orbs.stream().allMatch(e->Math.abs(e.getDeltaMovement().x)<.0331&&Math.abs(e.getDeltaMovement().z-.5)<.0331),"Water has original directional spread");
        orbs.forEach(Entity::discard);player.stopUsingItem();
        var fire=wand("fire");player.setItemInHand(InteractionHand.MAIN_HAND,fire);player.startUsingItem(InteractionHand.MAIN_HAND);
        for(int i=0;i<3;i++)fire.getItem().onUseTick(level,player,fire,71999-i);
        var flames=level.getEntitiesOfClass(ArcaneMote.class,new AABB(pos).inflate(4));
        check(fire.getDamageValue()==3&&flames.size()==9,"Fire emits three flames and spends one charge per tick");flames.forEach(Entity::discard);player.stopUsingItem();
        level.setBlockAndUpdate(pos,Content.block("arcane_seal").defaultBlockState());
        var seal=(dev.thaumcraft.machine.MachineBlockEntity)level.getBlockEntity(pos);
        var rune=Content.ENTRIES.stream().filter(e->e.source_class().equals("ItemRunicEssence")&&e.meta()==0).findFirst().orElseThrow();
        seal.setItem(18,new ItemStack(Content.item(rune.id())));var reversal=wand("reversal");player.setItemInHand(InteractionHand.MAIN_HAND,reversal);reversal.getItem().useOn(context(player,pos));
        check(seal.getItem(18).isEmpty()&&seal.progress==60&&reversal.getDamageValue()==1,"Reversal rune removal waits sixty ticks and spends one charge");
        var upgrade=Content.ENTRIES.stream().filter(e->e.source_class().equals("ItemUpgrades")&&e.meta()==0).findFirst().orElseThrow();
        level.setBlockAndUpdate(pos,Content.block("void_chest").defaultBlockState());level.setBlockAndUpdate(pos.above(),Content.block("void_interface").defaultBlockState());
        var chest=(dev.thaumcraft.machine.MachineBlockEntity)level.getBlockEntity(pos);var linkedInterface=(dev.thaumcraft.machine.MachineBlockEntity)level.getBlockEntity(pos.above());
        chest.setItem(20,new ItemStack(Content.item(upgrade.id())));chest.setItem(21,new ItemStack(Content.item(rune.id())));
        check(linkedInterface.getItem(20).is(Content.item(upgrade.id())),"Void interface exposes the chest's stored upgrade");
        check(reversal.getItem().useOn(context(player,pos))==InteractionResult.PASS&&reversal.getItem().useOn(context(player,pos.above()))==InteractionResult.PASS,"Reversal finds nothing installed in Void storage");
        check(chest.getItem(20).is(Content.item(upgrade.id()))&&chest.getItem(21).is(Content.item(rune.id()))&&reversal.getDamageValue()==1,"Reversal leaves stored upgrades and runes in Void storage without spending charge");
        level.setBlockAndUpdate(pos.above(),Blocks.AIR.defaultBlockState());level.setBlockAndUpdate(pos,Content.block("vis_condenser").defaultBlockState());
        var condenser=(dev.thaumcraft.machine.MachineBlockEntity)level.getBlockEntity(pos);
        check(condenser.installUpgrade(new ItemStack(Content.item(upgrade.id())))&&reversal.getItem().useOn(context(player,pos))==InteractionResult.SUCCESS,"Reversal removes an installed machine upgrade");
        check(condenser.getItem(18).isEmpty()&&reversal.getDamageValue()==2,"Installed upgrade removal spends one charge");
        level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(3)).forEach(Entity::discard);
        level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
        var enderman=EntityType.ENDERMAN.create(level,EntitySpawnReason.COMMAND);enderman.setPos(pos.getBottomCenter().add(0,0,2));level.addFreshEntity(enderman);
        var goals=((dev.thaumcraft.mixin.MobGoalsAccess)enderman).thaumcraft$targetGoals().getAvailableGoals();
        var teleport=goals.stream().map(g->g.getGoal()).filter(g->g instanceof dev.thaumcraft.mixin.EndermanTeleportAccess).findFirst().orElseThrow();
        ((dev.thaumcraft.mixin.EndermanTeleportAccess)teleport).thaumcraft$setTeleportTime(29);
        var orb=ModEntities.RELIC.create(level,EntitySpawnReason.COMMAND);orb.configure(4,player.getUUID());orb.setPos(enderman.position().add(0,1,-.3));orb.setDeltaMovement(0,0,.5);level.addFreshEntity(orb);orb.tick();
        check(orb.isRemoved()&&enderman.getHealth()==35,"Water deals five damage to Endermen");
        try{var field=teleport.getClass().getDeclaredField("teleportTime");field.setAccessible(true);check(field.getInt(teleport)==0,"Water resets the Enderman teleport countdown");}catch(ReflectiveOperationException e){throw new AssertionError(e);}
        enderman.discard();orb.discard();
        var nether=level.getServer().getLevel(net.minecraft.world.level.Level.NETHER);var netherPos=pos.below(100);nether.getChunkAt(netherPos);nether.setBlockAndUpdate(netherPos,Blocks.STONE.defaultBlockState());nether.setBlockAndUpdate(netherPos.above(),Blocks.AIR.defaultBlockState());
        RelicProjectile.waterImpact(nether,new BlockHitResult(netherPos.getCenter(),Direction.UP,netherPos,false));
        check(nether.getBlockState(netherPos.above()).is(Blocks.WATER),"Water orb places flowing water even in an evaporating dimension");
        nether.setBlockAndUpdate(netherPos.above(),Blocks.AIR.defaultBlockState());nether.setBlockAndUpdate(netherPos,Blocks.AIR.defaultBlockState());
        player.setPos(previous);player.getInventory().clearContent();return checks;
    }
}
