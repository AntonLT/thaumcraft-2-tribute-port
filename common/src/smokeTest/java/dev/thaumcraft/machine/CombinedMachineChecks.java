package dev.thaumcraft.machine;

import dev.thaumcraft.content.Content;
import dev.thaumcraft.gameplay.ResearchLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;

/** Behavioral checks for the combined machine parity repairs. */
public final class CombinedMachineChecks {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError("Combined machines: "+message);}
    private static MachineBlockEntity place(ServerLevel level,BlockPos pos,String id){
        level.getChunkAt(pos);level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos,Content.block(id).defaultBlockState());return (MachineBlockEntity)level.getBlockEntity(pos);
    }
    public static int run(MinecraftServer server){
        checks=0;int failures=0;
        try {research(server);}catch(AssertionError e){System.err.println(e);failures++;}
        try {darkInfuser(server);}catch(AssertionError e){System.err.println(e);failures++;}
        try {darkness(server);}catch(AssertionError e){System.err.println(e);failures++;}
        if(failures>0)throw new AssertionError("Machine groups failed: "+failures);
        return checks;
    }
    private static void research(MinecraftServer server){
        var level=server.overworld();var pos=new BlockPos(1200,280,1200);
        var research=place(level,pos,"quaesitum");research.setItem(0,new ItemStack(Items.COBBLESTONE,64));research.setItem(3,new ItemStack(Items.PAPER,64));
        for(int n=1;n<=5;n++){MachineBlockEntity.tick(level,pos,research.getBlockState(),research);check(research.progress==n,"research advances exactly once on game tick "+n);}
        check(research.visualWorking(),"research reports working between five-tick boundaries");
        for(int n=6;n<130;n++)MachineBlockEntity.tick(level,pos,research.getBlockState(),research);
        check(research.sequence==0&&research.progress==129,"unboosted research waits 130 ticks");
        MachineBlockEntity.tick(level,pos,research.getBlockState(),research);check(research.sequence==1&&research.progress==0&&research.visualWorking(),"completion remains working at cycle boundary");
        research.setItem(3,ItemStack.EMPTY);MachineBlockEntity.tick(level,pos,research.getBlockState(),research);check(!research.visualWorking()&&research.progress==0,"paper removal stops research next tick");
        research.setItem(0,ResearchLogic.theory(0));research.setItem(1,new ItemStack(Items.WATER_BUCKET));research.setItem(3,new ItemStack(Items.PAPER,64));research.progress=10000;ResearchLogic.tick(level,research);
        check(research.getItem(1).isEmpty(),"research consumes filled reagent");for(int slot=9;slot<18;slot++)check(!research.getItem(slot).is(Items.BUCKET),"research creates no crafting remainder");
        for(int slot=0;slot<18;slot++)research.setItem(slot,ItemStack.EMPTY);
        research.setItem(0,new ItemStack(Items.COBBLESTONE,64));research.setItem(3,new ItemStack(Items.PAPER,64));research.progress=0;
        level.setBlockAndUpdate(pos.east(2),Blocks.BOOKSHELF.defaultBlockState());long sequence=research.sequence;
        for(int n=0;n<123;n++)MachineBlockEntity.tick(level,pos,research.getBlockState(),research);
        check(research.sequence==sequence&&research.progress==123,"one bookshelf requires 124 ticks");
        MachineBlockEntity.tick(level,pos,research.getBlockState(),research);check(research.sequence==sequence+1,"bookshelf cycle completes at 124");
        level.setBlockAndUpdate(pos.east(),Blocks.GLASS.defaultBlockState());
        check(EnchantingBoosters.research(level,pos).equals(new EnchantingBoosters.Totals(0,0,0,0)),"research ignores a bookshelf behind glass");
        level.removeBlock(pos.east(),false);
        var ring=net.minecraft.world.level.block.EnchantingTableBlock.BOOKSHELF_OFFSETS.stream().map(pos::offset).toList();
        for(var shelf:ring)level.setBlockAndUpdate(shelf,Blocks.BOOKSHELF.defaultBlockState());
        check(EnchantingBoosters.research(level,pos).equals(new EnchantingBoosters.Totals(16,96,32,8)),"research counts sixteen boosted bookshelves at most");
        for(var shelf:ring)if(!shelf.equals(pos.east(2)))level.removeBlock(shelf,false);
        research.progress=17;var saved=research.saveWithFullMetadata(level.registryAccess());
        var restored=(MachineBlockEntity)net.minecraft.world.level.block.entity.BlockEntity.loadStatic(pos,research.getBlockState(),saved,level.registryAccess());restored.setLevel(level);
        restored.processes.tick(level);check(restored.progress==18,"saved research resumes one unit per tick");
        research.processes.soundDelay=0;research.processes.tick(level);check(research.processes.soundDelay==110,"first working tick starts 110-tick sound cooldown");
        for(int n=0;n<109;n++)research.processes.tick(level);check(research.processes.soundDelay==1,"sound cooldown continues across cycles");
        research.processes.tick(level);check(research.processes.soundDelay==110,"sound repeats on tick 110");
        research.setItem(3,ItemStack.EMPTY);for(int n=0;n<110;n++)research.processes.tick(level);check(research.processes.soundDelay==0&&!research.processes.researchWorked,"idle cooldown expires without new playback");
        level.removeBlock(pos.east(2),false);level.removeBlock(pos,false);
        }
    private static void darkInfuser(MinecraftServer server){
        var level=server.overworld();var pos=new BlockPos(1200,280,1200);
        var dark=place(level,pos.east(16),"dark_infuser");dark.setItem(0,new ItemStack(Items.SOUL_SAND,5));dark.insertVis(100,false);dark.insertVis(100,true);
        dark.processes.tick(level);check(dark.processes.pureCost==0&&dark.processes.darkCost==0&&dark.totalVis()==200,"dark recipe rejects repeated ingredients in one slot");
        for(int i=0;i<5;i++)dark.setItem(i,new ItemStack(Items.SOUL_SAND,2));dark.processes.tick(level);
        check(dark.processes.pureWork==0&&dark.processes.darkWork==0&&dark.totalVis()==200,"new dark recipe initializes without absorbing");
        check(dark.processes.pureCost==60*.6666667f&&dark.processes.darkCost==60*.33333334f,"original independent float split");
        dark.processes.tick(level);check(dark.processes.pureWork>0&&dark.processes.darkWork>0,"next dark tick absorbs both resources");
        level.setBlockAndUpdate(dark.getBlockPos().above(),Blocks.REDSTONE_BLOCK.defaultBlockState());dark.setItem(0,ItemStack.EMPTY);dark.processes.tick(level);
        check(dark.processes.pureWork==0&&dark.processes.darkWork==0&&dark.processes.pureCost==0,"powered dark infuser still invalidates missing input");
        level.removeBlock(dark.getBlockPos().above(),false);
        for(int i=0;i<5;i++)dark.setItem(i,new ItemStack(Items.SOUL_SAND,2));dark.processes.tick(level);dark.processes.tick(level);
        float pureCost=dark.processes.pureCost,darkCost=dark.processes.darkCost;
        var splitUpgrade=Content.ENTRIES.stream().filter(e->e.source_class().equals("ItemUpgrades")&&e.meta()==3).findFirst().orElseThrow();
        dark.setItem(18,new ItemStack(Content.item(splitUpgrade.id())));dark.processes.tick(level);
        check(dark.processes.pureCost==pureCost&&dark.processes.darkCost==darkCost,"mid-process split upgrade preserves accepted split");
        var saved=dark.saveWithFullMetadata(level.registryAccess());
        var restored=(MachineBlockEntity)net.minecraft.world.level.block.entity.BlockEntity.loadStatic(dark.getBlockPos(),dark.getBlockState(),saved,level.registryAccess());restored.setLevel(level);level.setBlockEntity(restored);dark=restored;
        check(dark.processes.pureWork>0&&dark.processes.darkWork>0&&dark.processes.pureCost==pureCost&&dark.processes.darkCost==darkCost,"independent partial work persists");
        dark.processes.pureWork=pureCost-.005f;dark.processes.darkWork=darkCost-.005f;float before=dark.totalVis();dark.processes.tick(level);
        check(dark.sequence==1&&dark.getItem(9).is(Content.item("soul_fragment")),"near endpoint completes exactly once");
        check(Math.abs(before-dark.totalVis()-.03f)<.001f,"both endpoint requests include original .01 overshoot");
        for(int i=0;i<5;i++)check(dark.getItem(i).getCount()==1,"dark infuser consumes one per ingredient slot");
        check(dark.processes.pureCost==30&&dark.processes.darkCost==30,"next process uses newly installed equal split");
        dark.setItem(9,new ItemStack(Items.DIRT,64));dark.processes.pureWork=30;dark.processes.darkWork=30;dark.processes.tick(level);
        check(dark.sequence==1&&dark.processes.pureCost==0&&dark.getItem(0).getCount()==1,"blocked completion clears costs without consuming ingredients");
        level.removeBlock(dark.getBlockPos(),false);
        var previousCatalog=dev.thaumcraft.gameplay.AddonData.server();
        var recipes=new java.util.ArrayList<>(previousCatalog.infusions());
        var bucket=new dev.thaumcraft.gameplay.GameData.Infusion(new dev.thaumcraft.gameplay.GameData.StackDef("minecraft:stone",1),10,java.util.List.of("minecraft:water_bucket"),true,-1);
        var sameCost=new dev.thaumcraft.gameplay.GameData.Infusion(new dev.thaumcraft.gameplay.GameData.StackDef("minecraft:dirt",1),10,java.util.List.of("minecraft:lava_bucket"),true,-1);
        var differentCost=new dev.thaumcraft.gameplay.GameData.Infusion(new dev.thaumcraft.gameplay.GameData.StackDef("minecraft:gravel",1),11,java.util.List.of("minecraft:milk_bucket"),true,-1);
        recipes.addFirst(bucket);recipes.addFirst(sameCost);recipes.addFirst(differentCost);
        replaceCatalog(new dev.thaumcraft.gameplay.AddonData.Snapshot(previousCatalog.projects(),recipes,previousCatalog.vis(),previousCatalog.researchItems(),previousCatalog.locks(),previousCatalog.categories()));
        try{
            var fixture=place(level,pos.east(16),"dark_infuser");fixture.setItem(0,new ItemStack(Items.WATER_BUCKET));fixture.insertVis(30,false);fixture.insertVis(30,true);
            fixture.processes.tick(level);fixture.processes.tick(level);float paidPure=fixture.processes.pureWork,paidDark=fixture.processes.darkWork;
            level.setBlockAndUpdate(fixture.getBlockPos().above(),Blocks.REDSTONE_BLOCK.defaultBlockState());fixture.setItem(0,new ItemStack(Items.LAVA_BUCKET));fixture.processes.tick(level);
            check(fixture.processes.pureWork==paidPure&&fixture.processes.darkWork==paidDark,"equal-cost output swap retains both work counters while powered");
            fixture.setItem(0,new ItemStack(Items.MILK_BUCKET));fixture.processes.tick(level);check(fixture.processes.pureWork==0&&fixture.processes.darkWork==0&&Math.round(fixture.processes.pureCost+fixture.processes.darkCost)==11,"changed rounded cost resets and reinitializes while powered");
            fixture.setItem(0,new ItemStack(Items.WATER_BUCKET));fixture.processes.tick(level);level.removeBlock(fixture.getBlockPos().above(),false);
            fixture.extractVis(fixture.taintedVis(),true);for(int i=0;i<30;i++)fixture.processes.tick(level);
            check(fixture.getItem(9).isEmpty()&&fixture.processes.darkWork==0&&fixture.processes.pureWork>=fixture.processes.pureCost,"taint starvation cannot complete pure-paid recipe");
            fixture.insertVis(10,true);for(int i=0;i<40&&fixture.getItem(9).isEmpty();i++)fixture.processes.tick(level);
            check(fixture.getItem(9).is(Items.STONE)&&fixture.getItem(0).isEmpty(),"container ingredient consumed on dark completion");
            for(int slot=0;slot<fixture.getContainerSize();slot++)check(!fixture.getItem(slot).is(Items.BUCKET),"dark infusion emits no container remainder");
            level.removeBlock(fixture.getBlockPos(),false);
        }finally{replaceCatalog(previousCatalog);}
        }
    private static void darkness(MinecraftServer server){
        var level=server.overworld();var pos=new BlockPos(1200,280,1200);
        var generator=place(level,pos.east(32),"darkness_generator");
        check(generator.getBlockState().getCollisionShape(level,generator.getBlockPos()).bounds().maxY==1,"darkness generator has full collision");
        check(generator.getBlockState().getShape(level,generator.getBlockPos()).bounds().maxY==7/16d,"darkness selection remains low");
        generator.setItem(0,new ItemStack(Items.WHEAT_SEEDS));MachineLogic.darkness(level,generator);
        level.setBlockAndUpdate(generator.getBlockPos().east(),Content.block("eldritch_monolith").defaultBlockState());MachineLogic.darkness(level,generator);
        check(generator.progress==0,"negative monolith lookup waits retry deadline");
        long now=level.getGameTime();var data=(net.minecraft.world.level.storage.ServerLevelData)level.getLevelData();
        data.setGameTime(now+19);MachineLogic.darkness(level,generator);check(generator.progress==0,"negative cache holds through tick 19");
        data.setGameTime(now+20);MachineLogic.darkness(level,generator);check(generator.visualDarknessMonolith().equals(generator.getBlockPos().east()),"retry publishes actual monolith");
        int work=generator.progress;level.setBlockAndUpdate(generator.getBlockPos().west(),Content.block("eldritch_monolith").defaultBlockState());level.removeBlock(generator.getBlockPos().east(),false);
        MachineLogic.darkness(level,generator);check(generator.progress>work&&generator.visualDarknessMonolith().equals(generator.getBlockPos().west()),"replacement monolith found immediately without lost work");
        check(generator.getUpdateTag(level.registryAccess()).contains("darkness_monolith"),"initial tracking includes source");
        generator.setItem(9,new ItemStack(Items.DIRT));generator.progress=89999;MachineLogic.darkness(level,generator);check(generator.progress==89999&&generator.getItem(0).getCount()==1&&generator.visualDarknessMonolith()!=null,"blocked output retains seed and visual link");
        generator.toggle();MachineLogic.darkness(level,generator);check(generator.visualDarknessMonolith()==null&&!generator.getUpdateTag(level.registryAccess()).contains("darkness_monolith"),"disabled source clears initial tracking");generator.toggle();
        level.removeBlock(generator.getBlockPos().west(),false);MachineLogic.darkness(level,generator);check(generator.progress==0&&generator.visualDarknessMonolith()==null,"lost target resets immediately");
        data.setGameTime(now);level.removeBlock(generator.getBlockPos(),false);
        var below=place(level,new BlockPos(1232,-40,1200),"darkness_generator");
        var boundary=below.getBlockPos().offset(-5,-5,-5);level.getChunkAt(boundary);level.setBlockAndUpdate(boundary,Content.block("eldritch_monolith").defaultBlockState());below.setItem(0,new ItemStack(Items.WHEAT_SEEDS));MachineLogic.darkness(level,below);
        check(boundary.equals(below.visualDarknessMonolith()),"inclusive negative-height search boundary");level.removeBlock(boundary,false);level.removeBlock(below.getBlockPos(),false);
    }
    private static void replaceCatalog(dev.thaumcraft.gameplay.AddonData.Snapshot snapshot){
        try{
            var field=dev.thaumcraft.gameplay.AddonData.class.getDeclaredField("server");field.setAccessible(true);
            var constructor=field.getType().getDeclaredConstructor(dev.thaumcraft.gameplay.AddonData.Snapshot.class);constructor.setAccessible(true);
            field.set(null,constructor.newInstance(snapshot));
        }catch(ReflectiveOperationException e){throw new AssertionError(e);}
    }
}
