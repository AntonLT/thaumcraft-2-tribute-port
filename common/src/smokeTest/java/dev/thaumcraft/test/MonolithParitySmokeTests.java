package dev.thaumcraft.test;

import com.mojang.authlib.GameProfile;
import dev.thaumcraft.content.Content;
import dev.thaumcraft.world.EldritchBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.*;

/** Exercises the public puzzle interactions, rather than its internal transition helpers. */
public final class MonolithParitySmokeTests {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError("Monolith: "+message);}
    private static final class Player extends ServerPlayer {
        final java.util.List<String> messages=new java.util.ArrayList<>();
        Player(MinecraftServer server,ServerLevel level){super(server,level,new GameProfile(java.util.UUID.randomUUID(),"MonolithParity"),ClientInformation.createDefault());}
        @Override public void sendSystemMessage(net.minecraft.network.chat.Component message,boolean overlay){messages.add(message.getString());}
    }
    private static ItemStack crystal(int rune){return new ItemStack(Content.item(Content.ENTRIES.stream().filter(e->e.source_class().equals("ItemCrystals")&&e.meta()==rune).findFirst().orElseThrow().id()),2);}
    private static void use(ServerLevel level,BlockPos pos,Player player,ItemStack held){level.getBlockState(pos).useItemOn(held,level,player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));}
    public static int run(MinecraftServer server){
        checks=0;var level=server.overworld();var player=new Player(server,level);var core=new BlockPos(960,100,960);level.getChunkAt(core);
        level.setBlockAndUpdate(core,Content.block("eldritch_core").defaultBlockState());
        var held=crystal(EldritchBlock.rune(core,0));use(level,core,player,held);
        check(held.getCount()==2&&level.getBlockState(core).getValue(EldritchBlock.PROGRESS)==0,"core rejects direct crystal insertion");
        level.getBlockState(core).useWithoutItem(level,player,new BlockHitResult(Vec3.atCenterOf(core),Direction.UP,core,false));
        check(player.messages.stream().noneMatch(m->m.contains("sequence")||m.contains("next rune")),"interactions never disclose rune answers in chat");
        level.removeBlock(core,false);
        for(int progress=0;progress<4;progress++){
            var position=core.east(32+progress*32);level.getChunkAt(position);
            for(Direction direction:new Direction[]{Direction.EAST,Direction.WEST,Direction.SOUTH,Direction.NORTH})level.setBlockAndUpdate(position.relative(direction),Content.block("eldritch_stone").defaultBlockState());
            level.setBlockAndUpdate(position,Content.block("eldritch_core").defaultBlockState().setValue(EldritchBlock.PROGRESS,progress));
            var hit=new BlockHitResult(Vec3.atCenterOf(position),Direction.UP,position,false);
            level.getBlockState(position).useWithoutItem(level,player,hit);
            var saved=level.getBlockEntity(position).saveWithoutMetadata(level.registryAccess());
            check(saved.getIntOr("puzzle_version",0)==1,"legacy progress "+progress+" converts once");
            var targets=saved.getIntArray("targets").orElseThrow();var inserted=saved.getIntArray("inserted").orElseThrow();
            for(int i=0;i<4;i++)check(inserted[i]==(i<progress?targets[i]:-1),"legacy paid crystal "+i+" retained at progress "+progress);
            level.removeBlock(position,false);
            for(Direction direction:new Direction[]{Direction.EAST,Direction.WEST,Direction.SOUTH,Direction.NORTH})level.removeBlock(position.relative(direction),false);
        }
        var blocked=core.south(32);level.getChunkAt(blocked);
        for(Direction direction:Direction.Plane.HORIZONTAL)level.setBlockAndUpdate(blocked.relative(direction),Content.block("eldritch_stone").defaultBlockState());
        level.setBlockAndUpdate(blocked,Content.block("eldritch_core").defaultBlockState().setValue(EldritchBlock.PROGRESS,2));
        level.setBlockAndUpdate(blocked.east(),net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState());
        var chest=(net.minecraft.world.level.block.entity.ChestBlockEntity)level.getBlockEntity(blocked.east());
        chest.setItem(0,new ItemStack(net.minecraft.world.item.Items.DIAMOND,7));
        level.getBlockState(blocked).useWithoutItem(level,player,new BlockHitResult(Vec3.atCenterOf(blocked),Direction.UP,blocked,false));
        check(chest.getItem(0).getCount()==7&&level.getBlockState(blocked).getValue(EldritchBlock.PROGRESS)==2,"obstruction preserves inventory and paid legacy progress");
        check(level.getBlockEntity(blocked).saveWithoutMetadata(level.registryAccess()).getIntOr("puzzle_version",0)==0,"obstruction defers migration atomically");
        chest.clearContent();level.removeBlock(blocked.east(),false);
        level.setBlockAndUpdate(blocked.east(),Content.block("eldritch_stone").defaultBlockState());
        level.getBlockState(blocked).useWithoutItem(level,player,new BlockHitResult(Vec3.atCenterOf(blocked),Direction.UP,blocked,false));
        var saved=level.getBlockEntity(blocked).saveWithFullMetadata(level.registryAccess());
        var restored=net.minecraft.world.level.block.entity.BlockEntity.loadStatic(blocked,level.getBlockState(blocked),saved,level.registryAccess());
        check(restored!=null&&java.util.Arrays.equals(saved.getIntArray("inserted").orElseThrow(),restored.saveWithoutMetadata(level.registryAccess()).getIntArray("inserted").orElseThrow()),"paid migration state survives reload");
        level.removeBlock(blocked,false);for(Direction direction:Direction.Plane.HORIZONTAL)level.removeBlock(blocked.relative(direction),false);
        var oldPos=core.south(64);level.getChunkAt(oldPos);
        for(var direction:Direction.Plane.HORIZONTAL)level.setBlockAndUpdate(oldPos.relative(direction),Content.block("eldritch_stone").defaultBlockState());
        level.setBlockAndUpdate(oldPos,Content.block("eldritch_core").defaultBlockState().setValue(EldritchBlock.PROGRESS,3));
        level.setBlockEntity(new dev.thaumcraft.world.VisualBlockEntity(oldPos,level.getBlockState(oldPos)));
        level.getChunkAt(oldPos).registerAllBlockEntitiesAfterLevelLoad();
        check(level.getBlockEntity(oldPos) instanceof dev.thaumcraft.world.MonolithBlockEntity,"legacy visual record migrates to dedicated core during chunk registration");
        var migrated=(dev.thaumcraft.world.MonolithBlockEntity)level.getBlockEntity(oldPos);check(migrated.migrate(),"old visual core converts neighbors");
        for(int i=0;i<4;i++)check(migrated.inserted(i)==(i<3?migrated.target(i):-1),"old visual record retains paid progress");
        level.removeBlock(oldPos,false);for(var direction:Direction.Plane.HORIZONTAL)level.removeBlock(oldPos.relative(direction),false);
        var puzzlePos=core.south(96);level.getChunkAt(puzzlePos);
        level.setBlockAndUpdate(puzzlePos,Content.block("eldritch_core").defaultBlockState());
        for(var side:dev.thaumcraft.world.MonolithBlockEntity.SIDES)level.setBlockAndUpdate(puzzlePos.relative(side),Content.block("eldritch_receptacle").defaultBlockState());
        var puzzle=(dev.thaumcraft.world.MonolithBlockEntity)level.getBlockEntity(puzzlePos);puzzle.initialize(net.minecraft.util.RandomSource.create(216));
        var wrong=crystal((puzzle.target(0)+1)%6);use(level,puzzlePos.east(),player,wrong);
        check(wrong.getCount()==1&&puzzle.inserted(0)==-1,"wrong crystal consumed and receptacle cleared");
        // A player-owned inventory in the shaft must block opening, retaining all four payments.
        var obstruction=puzzlePos.below(12);level.setBlockAndUpdate(obstruction,net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState());
        var protectedChest=(net.minecraft.world.level.block.entity.ChestBlockEntity)level.getBlockEntity(obstruction);protectedChest.setItem(0,new ItemStack(net.minecraft.world.item.Items.DIAMOND,7));
        for(int index:new int[]{2,0,3,1}){
            var crystal=crystal(puzzle.target(index));use(level,puzzlePos.relative(dev.thaumcraft.world.MonolithBlockEntity.SIDES[index]),player,crystal);
            check(crystal.getCount()==1&&puzzle.inserted(index)==puzzle.target(index),"out-of-order receptacle insertion "+index);
            var duplicate=crystal(puzzle.target(index));use(level,puzzlePos.relative(dev.thaumcraft.world.MonolithBlockEntity.SIDES[index]),player,duplicate);
            check(duplicate.getCount()==2,"occupied receptacle cannot consume another player's crystal");
        }
        check(level.getBlockEntity(puzzlePos)==puzzle&&protectedChest.getItem(0).getCount()==7,"blocked opening retains puzzle and neighboring inventory");
        var puzzleSaved=puzzle.saveWithFullMetadata(level.registryAccess());
        var reloaded=(dev.thaumcraft.world.MonolithBlockEntity)net.minecraft.world.level.block.entity.BlockEntity.loadStatic(puzzlePos,puzzle.getBlockState(),puzzleSaved,level.registryAccess());
        reloaded.setLevel(level);level.setBlockEntity(reloaded);
        for(int i=0;i<4;i++)check(reloaded.target(i)==puzzle.target(i)&&reloaded.inserted(i)==puzzle.inserted(i),"all puzzle targets and payments persist");
        protectedChest.clearContent();level.removeBlock(obstruction,false);
        level.getBlockState(puzzlePos).useWithoutItem(level,player,new BlockHitResult(Vec3.atCenterOf(puzzlePos),Direction.UP,puzzlePos,false));
        check(level.getBlockState(puzzlePos).isAir(),"empty-hand retry opens paid puzzle after obstruction removal");
        for(int y=14;y<=puzzlePos.getY();y++)check(level.getBlockState(new BlockPos(puzzlePos.getX(),y,puzzlePos.getZ())).isAir(),"original hollow shaft at "+y);
        check(level.getBlockState(new BlockPos(puzzlePos.getX(),7,puzzlePos.getZ())).is(Content.block("eldritch_structure_stone")),"original chamber center is stone, without added Nitor");
        check(!reloaded.tryOpen(),"old core cannot generate chamber twice");
        return checks;
    }
}
