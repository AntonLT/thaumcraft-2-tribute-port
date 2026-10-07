package dev.thaumcraft.test;

import com.mojang.authlib.GameProfile;
import dev.thaumcraft.content.Content;
import dev.thaumcraft.gameplay.ArcaneWorldData;
import dev.thaumcraft.gameplay.ArtifactLoot;
import dev.thaumcraft.gameplay.GameData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CrafterBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.CrafterBlockEntity;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.phys.Vec3;
import java.util.UUID;

final class CrafterTreasureSmokeTests {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    static int run(MinecraftServer server){
        checks=0;AssertionError failure=null;
        try{crafting(server.overworld());}catch(AssertionError e){failure=e;}
        try{treasure(server.overworld());}catch(AssertionError e){if(failure==null)failure=e;else failure.addSuppressed(e);}
        if(failure!=null)throw failure;return checks;
    }
    private static ServerPlayer player(ServerLevel level){return new ServerPlayer(level.getServer(),level,new GameProfile(UUID.randomUUID(),"CrafterOwner"),ClientInformation.createDefault());}
    private static CrafterBlockEntity crafter(ServerLevel level,BlockPos pos,ServerPlayer owner){
        level.getChunkAt(pos);level.setBlockAndUpdate(pos,Blocks.CRAFTER.defaultBlockState());
        if(owner!=null)Blocks.CRAFTER.setPlacedBy(level,pos,level.getBlockState(pos),owner,new ItemStack(Items.CRAFTER));
        return (CrafterBlockEntity)level.getBlockEntity(pos);
    }
    private static void loadRecipe(CrafterBlockEntity crafter,String id){
        var recipe=GameData.crafts().stream().filter(r->r.id().equals(id)).findFirst().orElseThrow();
        int slot=0;for(String row:recipe.pattern())for(char symbol:row.toCharArray()){
            String item=recipe.key().get(String.valueOf(symbol));
            crafter.setItem(slot++,symbol==' '?ItemStack.EMPTY:new ItemStack(BuiltInRegistries.ITEM.stream().filter(candidate->GameData.matches(item,new ItemStack(candidate))).findFirst().orElseThrow(),2));
        }
    }
    private static int count(CrafterBlockEntity crafter){return crafter.getItems().stream().mapToInt(ItemStack::getCount).sum();}
    private static void pulse(ServerLevel level,BlockPos pos){
        level.setBlockAndUpdate(pos.above(),Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos.above(),Blocks.REDSTONE_BLOCK.defaultBlockState());
        check(level.getBlockState(pos).getValue(CrafterBlock.TRIGGERED),"Real redstone signal triggers Crafter");
        level.getBlockState(pos).tick(level,pos,level.getRandom());
        level.setBlockAndUpdate(pos.above(),Blocks.AIR.defaultBlockState());
    }
    private static void crafting(ServerLevel level){
        var owner=player(level);var other=player(level);BlockPos pos=new BlockPos(220,280,220);
        ArcaneWorldData.researchData(level).unlock(other.getUUID(),0);
        var unowned=crafter(level,pos,null);loadRecipe(unowned,"thaumcraft2tp:research_000");pulse(level,pos);
        check(count(unowned)==18,"Ownerless Crafter rejects researched recipe without ingredient consumption even when another player knows it");
        var owned=crafter(level,pos.east(4),owner);loadRecipe(owned,"thaumcraft2tp:research_000");pulse(level,pos.east(4));
        check(count(owned)==18,"Player-placed Crafter rejects recipe unknown to its owner");
        ArcaneWorldData.researchData(level).unlock(owner.getUUID(),0);pulse(level,pos.east(4));
        check(count(owned)==9,"Same cached recipe crafts exactly once after offline owner learns research");
        pulse(level,pos);check(count(unowned)==18,"Ownerless Crafter remains blocked after owner learns research");
        var saved=owned.saveWithFullMetadata(level.registryAccess());
        check(saved.getStringOr("thaumcraft2tp:owner","").equals(owner.getUUID().toString()),"Crafter persists placer UUID");
        var restored=(CrafterBlockEntity)BlockEntity.loadStatic(owned.getBlockPos(),owned.getBlockState(),saved,level.registryAccess());
        check(restored!=null&&restored.saveWithFullMetadata(level.registryAccess()).getStringOr("thaumcraft2tp:owner","").equals(owner.getUUID().toString()),"Owner survives save reload");
        saved.putString("thaumcraft2tp:owner","malformed");restored=(CrafterBlockEntity)BlockEntity.loadStatic(owned.getBlockPos(),owned.getBlockState(),saved,level.registryAccess());
        check(restored!=null&&!restored.saveWithFullMetadata(level.registryAccess()).contains("thaumcraft2tp:owner"),"Malformed owner loads as ownerless");
        for(var candidate:new CrafterBlockEntity[]{owned,unowned}){
            candidate.clearContent();candidate.setItem(0,new ItemStack(Items.OAK_LOG));pulse(level,candidate.getBlockPos());
            check(candidate.isEmpty(),"Ungated recipe works with and without owner");
        }
        Blocks.CRAFTER.setPlacedBy(level,owned.getBlockPos(),owned.getBlockState(),other,new ItemStack(Items.CRAFTER));
        check(owned.saveWithFullMetadata(level.registryAccess()).getStringOr("thaumcraft2tp:owner","").equals(other.getUUID().toString()),"Replacement placement assigns new owner");
    }
    private static void treasure(ServerLevel level){
        var params=new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN,new Vec3(230,280,230)).create(LootContextParamSets.CHEST);
        for(var block:new net.minecraft.world.level.block.Block[]{Blocks.CHEST,Blocks.TRAPPED_CHEST}){
            ChestBlockEntity chest=block==Blocks.TRAPPED_CHEST?new net.minecraft.world.level.block.entity.TrappedChestBlockEntity(new BlockPos(230,280,230),block.defaultBlockState()):new ChestBlockEntity(new BlockPos(230,280,230),block.defaultBlockState());
            var expected=new SimpleContainer(27);chest.setItem(0,new ItemStack(Items.DIAMOND));expected.setItem(0,new ItemStack(Items.DIAMOND));
            ArtifactLoot.fillOriginalTreasure(expected,RandomSource.create(843));LootTable.EMPTY.fill(chest,params,843);
            for(int i=0;i<27;i++)check(ItemStack.matches(chest.getItem(i),expected.getItem(i)),"Actual loot fill augments remaining chest slots deterministically "+i);
        }
        var full=new ChestBlockEntity(BlockPos.ZERO,Blocks.CHEST.defaultBlockState());for(int i=0;i<27;i++)full.setItem(i,new ItemStack(Items.DIAMOND,64));
        LootTable.EMPTY.fill(full,params,843);for(int i=0;i<27;i++)check(full.getItem(i).is(Items.DIAMOND)&&full.getItem(i).getCount()==64,"Full chest preserved");
        // Exact original pool ranges, including both no-loot boundaries, without sampling noise.
        for(int roll=0;roll<1083;roll++){
            final int selected=roll;
            var random=new net.minecraft.world.level.levelgen.LegacyRandomSource(0){@Override public int nextInt(int bound){return bound==1083?selected:0;}};
            var sample=new SimpleContainer(1);ArtifactLoot.fillOriginalTreasure(sample,random);var stack=sample.getItem(0);
            if(roll>=361){check(stack.isEmpty(),"No-loot range "+roll);continue;}
            var entry=Content.entry(stack);check(entry!=null,"Pool entry exists "+roll);
            if(roll<6)check(entry.source_class().startsWith("ItemElemental"),"Six elemental tools");
            else if(roll<21)check(entry.source_class().equals("ItemComponents")&&entry.meta()==6&&stack.getCount()==1,"Fifteen thaumium entries");
            else if(roll<357){int offset=roll-21;int meta=offset<200?(offset%4)/2:offset<300?2+((offset-200)%4)/2:offset<324?4:5;
                check(entry.meta()==meta&&entry.source_class().equals(offset%2==0?"ItemArtifactLost":"ItemArtifactForbidden"),"Weighted artifact category "+roll);}
            else check(entry.source_class().equals("ItemArtifactEldritch")&&entry.meta()==(roll==360?2:0),"Four Eldritch entries");
        }
        var copies=new SimpleContainer(2);ArtifactLoot.fillOriginalTreasure(copies,new net.minecraft.world.level.levelgen.LegacyRandomSource(0){@Override public int nextInt(int bound){return 0;}});
        copies.getItem(0).shrink(1);check(copies.getItem(1).getCount()==1,"Generated stacks are independent copies");
        var lazyPos=new BlockPos(236,280,236);level.getChunkAt(lazyPos);level.setBlockAndUpdate(lazyPos,Blocks.CHEST.defaultBlockState());
        var lazy=(ChestBlockEntity)level.getBlockEntity(lazyPos);lazy.setLootTable(net.minecraft.world.level.storage.loot.BuiltInLootTables.SIMPLE_DUNGEON);lazy.setLootTableSeed(843);
        lazy.getItem(0);check(lazy.getLootTable()==null,"hopper-style slot access clears lazy loot table before fill");
        var contents=new java.util.ArrayList<ItemStack>();for(int i=0;i<27;i++)contents.add(lazy.getItem(i).copy());
        lazy.unpackLootTable(null);for(int i=0;i<27;i++)check(ItemStack.matches(contents.get(i),lazy.getItem(i)),"repeated access cannot augment twice");
        lazy.clearContent();level.removeBlock(lazyPos,false);
        var generic=new SimpleContainer(27);LootTable.EMPTY.fill(generic,params,843);check(generic.isEmpty(),"Non-chest containers receive no ordinary treasure");
    }
}
