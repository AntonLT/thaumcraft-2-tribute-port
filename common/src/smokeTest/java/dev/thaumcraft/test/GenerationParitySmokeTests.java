package dev.thaumcraft.test;

import dev.thaumcraft.PortConfig;
import dev.thaumcraft.Thaumcraft;
import dev.thaumcraft.content.Content;
import dev.thaumcraft.content.CrystalBlock;
import dev.thaumcraft.content.TaintBlock;
import dev.thaumcraft.gameplay.ArcaneWorldData;
import dev.thaumcraft.world.ArcaneFeatures;
import dev.thaumcraft.world.EldritchIndex;
import dev.thaumcraft.world.WorldGenMonolith;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.lang.reflect.Proxy;
import java.util.*;

/** Controlled terrain isolates generation rules from changes to vanilla noise and caves. */
public final class GenerationParitySmokeTests {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError("Generation: "+message);}
    private static final class Terrain {
        final ServerLevel server;
        final Map<BlockPos,BlockState> blocks=new HashMap<>();
        final List<BlockPos> columns=new ArrayList<>();
        final List<BlockPos> scheduled=new ArrayList<>();
        final WorldGenLevel world;
        Holder<Biome> biome;
        long seed=216;
        int bottom=-64,height=101;
        boolean stone,failSilverwood,canopy,grassy;
        Terrain(ServerLevel server){
            this.server=server;biome=server.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.FOREST);
            world=(WorldGenLevel)Proxy.newProxyInstance(WorldGenLevel.class.getClassLoader(),new Class<?>[]{WorldGenLevel.class},(proxy,method,args)->switch(method.getName()){
                case "getLevel" -> server;
                case "getSeed" -> seed;
                case "getMinY" -> bottom;
                case "getMaxY" -> 320;
                case "getHeight" -> args==null||args.length==0?320-bottom:height;
                case "getSeaLevel" -> 63;
                case "getBiome" -> ((BlockPos)args[0]).getY()<0?server.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.DESERT):biome;
                case "isOutsideBuildHeight" -> {int y=args[0] instanceof BlockPos p?p.getY():(int)args[0];yield y<bottom||y>=320;}
                case "ensureCanWrite","hasChunkAt" -> true;
                case "getBlockEntity" -> null;
                case "scheduleTick" -> {scheduled.add(((BlockPos)args[0]).immutable());yield null;}
                case "getHeightmapPos" -> {BlockPos p=(BlockPos)args[1];columns.add(p);int y=canopy&&args[0]!=net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES?height+5:height;yield new BlockPos(p.getX(),y,p.getZ());}
                case "getBlockState" -> state((BlockPos)args[0]);
                case "setBlock" -> {blocks.put(((BlockPos)args[0]).immutable(),(BlockState)args[1]);yield true;}
                default -> throw new UnsupportedOperationException(method.toString());
            });
        }
        BlockState state(BlockPos p){
            if(blocks.containsKey(p))return blocks.get(p);
            if(stone)return Blocks.STONE.defaultBlockState();
            if(canopy&&p.getY()==height+4)return Blocks.OAK_LEAVES.defaultBlockState();
            if(grassy&&p.getY()==height)return Blocks.SHORT_GRASS.defaultBlockState();
            if(p.getY()>=height)return Blocks.AIR.defaultBlockState();
            if(failSilverwood&&(p.getX()&15)==1&&(p.getZ()&15)==1)return Blocks.STONE.defaultBlockState();
            return (p.getY()==height-1?Blocks.GRASS_BLOCK:Blocks.DIRT).defaultBlockState();
        }
        boolean place(String name,BlockPos origin,RandomSource random){
            if(name.equals("cinnabar_deposits"))return server.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE)
                    .getValue(Thaumcraft.id(name)).place(world,server.getChunkSource().getGenerator(),random,origin);
            @SuppressWarnings("unchecked") var feature=(Feature<NoneFeatureConfiguration>)BuiltInRegistries.FEATURE.getValue(Thaumcraft.id(name));
            return feature.place(new FeaturePlaceContext<>(Optional.empty(),world,server.getChunkSource().getGenerator(),random,origin,NoneFeatureConfiguration.INSTANCE));
        }
    }
    public static int run(MinecraftServer server){
        checks=0;boolean monoliths=PortConfig.monoliths,spread=PortConfig.taintSpread,generation=PortConfig.worldGeneration;int taint=PortConfig.taintSpawn;
        try {
            PortConfig.worldGeneration=true;PortConfig.monoliths=false;PortConfig.taintSpawn=0;
            deposits(server.overworld());vegetation(server.overworld());monoliths(server.overworld());
            corruption(server.overworld());patch(server.overworld());treasure(server.overworld());nether(server);nonOverworldMonoliths(server);
        } finally {PortConfig.monoliths=monoliths;PortConfig.taintSpread=spread;PortConfig.worldGeneration=generation;PortConfig.taintSpawn=taint;}
        Thaumcraft.LOG.info("WORLD_GENERATION_PARITY_PASS checks={}",checks);
        return checks;
    }
    private static void deposits(ServerLevel level){
        int[] crystals=new int[2],ores=new int[2];
        // Separate placed features receive separate seeds in vanilla generation.
        long[] seeds=new Random(216).longs(256).toArray();
        for(int variant=0;variant<2;variant++)for(int sample=0;sample<128;sample++){
            var terrain=new Terrain(level);terrain.stone=true;terrain.bottom=variant==0?0:-64;
            terrain.place("arcane_deposits",new BlockPos(4096+sample*16,0,4096),RandomSource.create(seeds[sample*2]));
            check(terrain.blocks.values().stream().allMatch(state->state.getBlock() instanceof CrystalBlock),"crystal feature generates no cinnabar");
            long before=terrain.blocks.values().stream().filter(state->state.getBlock() instanceof CrystalBlock).count();
            terrain.place("cinnabar_deposits",new BlockPos(4096+sample*16,0,4096),RandomSource.create(seeds[sample*2+1]));
            check(terrain.blocks.values().stream().filter(state->state.getBlock() instanceof CrystalBlock).count()==before,"cinnabar feature preserves crystals");
            for(var entry:terrain.blocks.entrySet()){
                if(entry.getValue().getBlock() instanceof CrystalBlock){crystals[variant]++;check(entry.getKey().getY()>=terrain.bottom&&entry.getKey().getY()<85,"crystal height range");}
                else if(entry.getValue().is(Content.block("cinnabar_ore"))){ores[variant]++;check(entry.getKey().getY()>=terrain.bottom&&entry.getKey().getY()<51,"cinnabar height range");}
            }
            check(ArcaneFeatures.surfaceBiome(terrain.world,BlockPos.ZERO).is(Biomes.FOREST),"underground desert does not replace surface biome");
        }
        double crystalRatio=(crystals[1]/149.0)/(crystals[0]/85.0),oreRatio=(ores[1]/115.0)/(ores[0]/51.0);
        Thaumcraft.LOG.info("WORLD_GENERATION_DENSITY crystals={}/{} cinnabar={}/{} ratios={},{}",crystals[0],crystals[1],ores[0],ores[1],crystalRatio,oreRatio);
        check(Math.abs(crystalRatio-1)<.02&&Math.abs(oreRatio-1)<.02,"actual placed deposit density remains within 2% of original on identical solid terrain");
        check(ArcaneFeatures.depositAttempts(25,85,100,85,RandomSource.create(1))==0,"world floor above ore ceiling is empty");
    }
    private static void vegetation(ServerLevel level){
        var terrain=new Terrain(level);terrain.failSilverwood=true;
        var origin=new BlockPos(8192,0,8192);
        while(ArcaneWorldData.initialAura(terrain.seed,origin.getX()>>4,origin.getZ()>>4,terrain.biome,false).vis()<=PortConfig.auraMax*.585f)terrain.seed++;
        var random=new LegacyRandomSource(216){int coordinates;@Override public int nextInt(int bound){if(bound==16)return coordinates++<2?1:12;return super.nextInt(bound);}};
        check(terrain.place("arcane_vegetation",origin,random),"failed silverwood location falls back to greatwood");
        check(terrain.columns.size()==2&&!terrain.columns.get(0).equals(terrain.columns.get(1)),"each tree gets an independent position");
        check(terrain.blocks.values().stream().anyMatch(s->s.is(Content.block("greatwood_log"))),"greatwood actually placed at fallback position");
        check(ArcaneFeatures.silverwoodBiome(level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.JUNGLE)),"modern jungle includes former jungle hills habitat");
        var covered=new Terrain(level);covered.seed=terrain.seed;covered.canopy=true;
        check(!covered.place("arcane_vegetation",origin,RandomSource.create(216))&&covered.blocks.isEmpty(),"existing canopy prevents natural trees from starting underneath leaves");
        covered.canopy=false;covered.grassy=true;
        check(!covered.place("arcane_vegetation",origin,RandomSource.create(216))&&covered.blocks.isEmpty(),"ground cover blocks natural trees like legacy tall grass");
        covered.grassy=false;
        check(covered.place("arcane_vegetation",origin,RandomSource.create(216)),"same high-aura site allows a tree with an open canopy");
    }
    private static void monoliths(ServerLevel level){
        var index=new EldritchIndex();var first=new BlockPos(0,100,0);
        check(index.reserve(first)&&!index.reserve(first.east(300)),"in-flight generation enforces original greater-than-300 spacing");
        index.cancel(first);check(index.reserve(first.east(16)),"failed placement leaves other chunks eligible");
        index.record(first.east(16));check(!index.reserve(first.east(32))&&index.reserve(first.east(320)),"committed spacing and distant retry");
        var concurrent=new EldritchIndex();
        long accepted=java.util.stream.IntStream.range(0,64).parallel().filter(i->concurrent.reserve(new BlockPos(i,100,0))).count();
        check(accepted==1,"parallel workers cannot reserve overlapping monoliths");
        var terrain=new Terrain(level);terrain.height=102;var origin=new BlockPos(12288,0,12288);var center=origin.offset(8,100,8);
        for(BlockPos p:BlockPos.betweenClosed(center.offset(-3,0,-3),center.offset(3,0,3)))terrain.blocks.put(p.immutable(),Blocks.GRASS_BLOCK.defaultBlockState());
        for(BlockPos p:BlockPos.betweenClosed(center.offset(-3,1,-3),center.offset(3,7,3)))terrain.blocks.put(p.immutable(),Blocks.AIR.defaultBlockState());
        terrain.blocks.put(center.above(),Blocks.SNOW.defaultBlockState());
        PortConfig.monoliths=true;
        var random=new LegacyRandomSource(216){@Override public int nextInt(int bound){return bound==50?0:super.nextInt(bound);}};
        check(terrain.place("arcane_vegetation",origin,random),"surface feature generates monolith through snow cover");
        check(terrain.state(center).is(Content.block("eldritch_core")),"snow normalization places buried core at ground");
        check(terrain.state(center.below()).is(Content.block("eldritch_structure_stone")),"monolith foundation uses original indestructible stone");
        for(String id:new String[]{"eldritch_structure_stone","eldritch_monolith","eldritch_core","eldritch_receptacle"}){
            var block=Content.block(id);
            check(block.defaultBlockState().getDestroySpeed(level,center)==-1&&block.getExplosionResistance()>=3600000,"generated "+id+" resists mining and explosions");
        }
        check(Content.block("eldritch_stone").defaultBlockState().getDestroySpeed(level,center)>0,"crafted Eldritch Stone remains breakable");
        check(terrain.scheduled.contains(center.above(3)),"generated monolith starts its ambient tick through generation tick storage");
        check(terrain.columns.size()==1&&terrain.blocks.values().stream().noneMatch(s->s.is(Content.block("greatwood_log"))||s.is(Content.block("silverwood_log"))),"successful monolith suppresses all vegetation attempts");
        var invalid=new Terrain(level);invalid.stone=false;invalid.height=101;
        var low=new Terrain(level);low.height=20;
        check(!WorldGenMonolith.generate(low.world,new BlockPos(16000,19,16000),RandomSource.create(1))&&low.blocks.isEmpty(),"original Y20 minimum prevents monoliths below the entrance chamber");
        var badCenter=origin.offset(1024+8,100,8);invalid.blocks.put(badCenter,Blocks.WATER.defaultBlockState());
        check(!WorldGenMonolith.generate(invalid.world,badCenter,RandomSource.create(1)),"unsuitable ground rejects a monolith");
        check(EldritchIndex.get(level).reserve(badCenter.east(16)),"terrain rejection does not reserve surrounding space");EldritchIndex.get(level).cancel(badCenter.east(16));
        PortConfig.monoliths=false;
    }
    private static void corruption(ServerLevel level){
        PortConfig.taintSpread=false;
        var terrain=new Terrain(level);terrain.height=-64;var pos=new BlockPos(16384,100,16384);
        terrain.blocks.put(pos,Blocks.OAK_LOG.defaultBlockState());
        check(TaintBlock.increase(terrain.world,pos,12000,RandomSource.create(1))&&terrain.state(pos).is(Content.block("tainted_log")),"initial corruption uses full wood conversion even with live spread disabled");
        check(dev.thaumcraft.world.TaintMemory.get(level).original(pos).orElseThrow().is(Blocks.OAK_LOG),"generated foliage retains purification memory");
        terrain.blocks.clear();terrain.blocks.put(pos,Content.block("vis_ore").defaultBlockState());
        check(TaintBlock.increase(terrain.world,pos,12000,RandomSource.create(1))&&terrain.state(pos).is(Content.block("tainted_vis_ore")),"initial corruption converts crystals");
        terrain.blocks.clear();terrain.blocks.put(pos,Blocks.DIRT.defaultBlockState());terrain.blocks.put(pos.east(2),Content.block("totem_of_dawn").defaultBlockState());
        check(!TaintBlock.increase(terrain.world,pos,12000,RandomSource.create(1))&&terrain.state(pos).is(Blocks.DIRT),"initial corruption respects totem protection");
        terrain.blocks.clear();terrain.blocks.put(pos,Content.block("tainted_grass").defaultBlockState());terrain.blocks.put(pos.east(2).above(),Content.block("taintweed").defaultBlockState());
        check(!TaintBlock.increase(terrain.world,pos,8000,RandomSource.create(1)),"taintweed generation retains original plant spacing");
        var data=new ArcaneWorldData();data.seedAura(pos,new ArcaneWorldData.Aura(5000,1000,5000));
        data.raiseExistingTaint(pos,6000,7000);data.raiseExistingTaint(pos,6000,7500);data.raiseExistingTaint(pos.east(32),6000,7000);
        var encoded=ArcaneWorldData.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,data).getOrThrow();
        var cells=encoded.getAsJsonObject().getAsJsonObject("aura");
        check(cells.size()==1&&cells.entrySet().iterator().next().getValue().getAsJsonObject().get("taint").getAsFloat()==7000,"neighbor taint boost applies once and never creates absent cells");
    }
    private static void treasure(ServerLevel level){
        var pos=new BlockPos(1440,290,1440);level.getChunkAt(pos);level.setBlockAndUpdate(pos,Blocks.CHEST.defaultBlockState());
        var chest=(net.minecraft.world.level.block.entity.ChestBlockEntity)level.getBlockEntity(pos);
        check(ArcaneFeatures.hasTreasureChest(level,pos)&&!ArcaneFeatures.treasure(level,pos,RandomSource.create(1)),"existing chest with empty slots suppresses additional treasure site");
        var expected=new net.minecraft.world.SimpleContainer(chest.getContainerSize());dev.thaumcraft.gameplay.ArtifactLoot.fillOriginalTreasure(expected,RandomSource.create(1));
        check(sameContents(chest,expected)&&!expected.isEmpty(),"generated chest without a loot table receives original treasure while scanned");
        chest.unpackLootTable(null);
        check(sameContents(chest,expected),"reopening a scanned chest does not augment it again");
        chest.clearContent();chest.setItem(0,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.EMERALD,9));
        expected=new net.minecraft.world.SimpleContainer(chest.getContainerSize());expected.setItem(0,chest.getItem(0).copy());dev.thaumcraft.gameplay.ArtifactLoot.fillOriginalTreasure(expected,RandomSource.create(2));
        check(!ArcaneFeatures.treasure(level,pos,RandomSource.create(2))&&sameContents(chest,expected),"partially populated generated chest keeps its items and fills only empty slots");
        chest.clearContent();chest.setLootTable(net.minecraft.world.level.storage.loot.BuiltInLootTables.SIMPLE_DUNGEON);
        check(!ArcaneFeatures.treasure(level,pos,RandomSource.create(3))&&chest.getLootTable()!=null,"deferred chest is left for the loot-table callback during the scan");
        chest.setLootTable(null);
        for(int slot=0;slot<chest.getContainerSize();slot++)chest.setItem(slot,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STONE));
        check(!ArcaneFeatures.hasTreasureChest(level,pos),"full chest does not suppress fallback site");
        chest.setLootTable(net.minecraft.world.level.storage.loot.BuiltInLootTables.SIMPLE_DUNGEON);
        check(ArcaneFeatures.hasTreasureChest(level,pos)&&chest.getLootTable()!=null,"deferred chest suppresses fallback without prematurely unpacking loot");
        chest.setLootTable(null);chest.clearContent();level.removeBlock(pos,false);
    }
    private static boolean sameContents(net.minecraft.world.Container actual,net.minecraft.world.Container expected){
        for(int slot=0;slot<expected.getContainerSize();slot++)if(!net.minecraft.world.item.ItemStack.matches(actual.getItem(slot),expected.getItem(slot)))return false;
        return true;
    }
    private static void patch(ServerLevel level){
        PortConfig.taintSpawn=2;var terrain=new Terrain(level);var origin=new BlockPos(24576,0,24576);
        while(ArcaneWorldData.initialAura(terrain.seed,origin.getX()>>4,origin.getZ()>>4,terrain.biome,false).taint()<14000)terrain.seed++;
        int[] podRolls={0};
        var sourceRandom=new LegacyRandomSource(1){@Override public int nextInt(int bound){if(bound==50)podRolls[0]++;return bound==31?0:super.nextInt(bound);}};
        var failure=new java.util.concurrent.atomic.AtomicReference<Throwable>();
        Thread worker=new Thread(()->{try{ArcaneFeatures.taintedArea(terrain.world,origin,sourceRandom);}catch(Throwable e){failure.set(e);}},"taint-generation-regression");
        worker.start();try{worker.join(10000);}catch(InterruptedException e){throw new AssertionError(e);}
        check(!worker.isAlive()&&failure.get()==null,"generation worker can read aura without accessing SavedDataStorage or blocking on server thread: "+failure.get());
        check(podRolls[0]==0&&terrain.blocks.values().stream().anyMatch(s->s.getBlock() instanceof TaintBlock),"initial source patch corrupts terrain without premature spore pods");
        var neighbor=origin.east(16);var data=ArcaneWorldData.get(level);data.seedAura(neighbor,new ArcaneWorldData.Aura(5000,1000,5000));
        var neighborRandom=new LegacyRandomSource(1){int sample;@Override public int nextInt(int bound){if(bound==50)podRolls[0]++;return bound==31?(sample++%4==0?16:0):super.nextInt(bound);}};
        ArcaneFeatures.taintedArea(terrain.world,origin,neighborRandom);
        check(data.knownAura(neighbor).taint()>=11200&&podRolls[0]>0,"existing neighbor aura is boosted and later samples can grow pods");
        PortConfig.taintSpawn=0;
    }
    private static void nether(MinecraftServer server){
        var level=server.getLevel(net.minecraft.world.level.Level.NETHER);check(level!=null,"Nether exists");
        var biome=level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.NETHER_WASTES);
        check(biome.value().getGenerationSettings().features().stream().flatMap(net.minecraft.core.HolderSet::stream).anyMatch(f->f.is(ArcaneFeatures.placed("arcane_vegetation"))),"Nether biomes actually receive aura generation feature");
        var terrain=new Terrain(level);terrain.biome=biome;terrain.bottom=0;terrain.height=129;
        PortConfig.monoliths=true;
        try {
            var random=new LegacyRandomSource(1){@Override public int nextInt(int bound){return bound==50?0:super.nextInt(bound);}};
            check(!terrain.place("arcane_vegetation",new BlockPos(20000,0,20000),random)&&terrain.blocks.isEmpty(),"Nether aura path does not generate overworld trees, ores or monoliths even with monoliths enabled");
        } finally {PortConfig.monoliths=false;}
    }
    private static void nonOverworldMonoliths(MinecraftServer server){
        PortConfig.monoliths=true;
        try {
            for(var level:server.getAllLevels()){
                if(level.dimension()==net.minecraft.world.level.Level.OVERWORLD)continue;
                var center=new BlockPos(24584,100,24584);var terrain=new Terrain(level);
                check(!WorldGenMonolith.generate(terrain.world,center,RandomSource.create(216))&&terrain.blocks.isEmpty()&&terrain.scheduled.isEmpty(),"direct monolith generation rejects "+level.dimension().identifier()+" on otherwise eligible terrain");
                var random=new LegacyRandomSource(216){@Override public int nextInt(int bound){return bound==50?0:super.nextInt(bound);}};
                terrain.place("arcane_vegetation",center.offset(-8,-100,-8),random);
                check(terrain.blocks.values().stream().noneMatch(s->s.is(Content.block("eldritch_core"))||s.is(Content.block("eldritch_monolith")))&&terrain.scheduled.isEmpty(),"surface monolith attempt rejects "+level.dimension().identifier()+" even with an Overworld biome");
                Thaumcraft.LOG.info("MONOLITH_DIMENSION_REJECTION_PASS dimension={}",level.dimension().identifier());
            }
        } finally {PortConfig.monoliths=false;}
    }
}
