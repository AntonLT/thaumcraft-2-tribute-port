package dev.thaumcraft.test;

import dev.thaumcraft.Thaumcraft;
import dev.thaumcraft.content.Content;
import dev.thaumcraft.world.ArcaneLogBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** Measures modern biome shares and natural greatwood/silverwood yield per chunk; run through scripts/tree_census.py. */
public final class TreeCensus {
    static final List<ResourceKey<Biome>> TARGETS=List.of(Biomes.FOREST,Biomes.FLOWER_FOREST,Biomes.BIRCH_FOREST,Biomes.OLD_GROWTH_BIRCH_FOREST,Biomes.DARK_FOREST,Biomes.PALE_GARDEN,Biomes.GROVE,
            Biomes.TAIGA,Biomes.SNOWY_TAIGA,Biomes.OLD_GROWTH_PINE_TAIGA,Biomes.OLD_GROWTH_SPRUCE_TAIGA,Biomes.JUNGLE,Biomes.SPARSE_JUNGLE,Biomes.BAMBOO_JUNGLE,Biomes.PLAINS);
    static final long[] SEEDS={216L,1L,42L,-7310214513957226497L,8675309L};

    /** Settings come from tree-census.properties in the server directory, so Gradle can keep its daemon. */
    static final java.nio.file.Path SETTINGS=java.nio.file.Path.of("tree-census.properties");
    static boolean requested(){return java.nio.file.Files.exists(SETTINGS);}
    private static java.util.Properties settings(){
        var properties=new java.util.Properties();
        try(var in=java.nio.file.Files.newInputStream(SETTINGS)){properties.load(in);}catch(java.io.IOException ignored){}
        return properties;
    }
    public static void run(MinecraftServer server){
        var settings=settings();
        if(Boolean.parseBoolean(settings.getProperty("shares","true")))shares(server);
        yields(server.overworld(),Integer.parseInt(settings.getProperty("radius","5")),Integer.parseInt(settings.getProperty("threads","8")));
    }

    private static String name(Holder<Biome> biome){return biome.unwrapKey().map(key->key.identifier().getPath()).orElse("?");}

    private static void shares(MinecraftServer server){
        ServerLevel level=server.overworld();var generator=level.getChunkSource().getGenerator();
        Map<String,Long> counts=new TreeMap<>();long total=0,land=0;
        for(long seed:SEEDS){
            var state=RandomState.create(server.registryAccess(),NoiseGeneratorSettings.OVERWORLD,seed);
            for(int x=-16384;x<16384;x+=256)for(int z=-16384;z<16384;z+=256){
                int y=Math.max(level.getSeaLevel(),generator.getBaseHeight(x,z,Heightmap.Types.WORLD_SURFACE_WG,level,state)-1);
                var biome=generator.getBiomeSource().getNoiseBiome(QuartPos.fromBlock(x),QuartPos.fromBlock(y),QuartPos.fromBlock(z),state.sampler());
                counts.merge(name(biome),1L,Long::sum);total++;
                if(!biome.is(BiomeTags.IS_OCEAN)&&!biome.is(BiomeTags.IS_RIVER))land++;
            }
        }
        Thaumcraft.LOG.info("TREE_CENSUS shares samples={} land={}",total,String.format("%.4f",(double)land/total));
        for(var entry:counts.entrySet())Thaumcraft.LOG.info("TREE_CENSUS share {} all={} land={}",entry.getKey(),String.format("%.4f",(double)entry.getValue()/total),String.format("%.4f",(double)entry.getValue()/land));
    }

    private static void yields(ServerLevel level,int radius,int threads){
        Map<String,double[]> stats=new TreeMap<>();
        BlockPos[] origins={new BlockPos(0,64,0),new BlockPos(20000,64,-20000),new BlockPos(-20000,64,20000),
                new BlockPos(20000,64,20000),new BlockPos(-20000,64,-20000),new BlockPos(0,64,40000)};
        List<ChunkPos> centers=new java.util.ArrayList<>();
        for(var target:TARGETS)for(BlockPos origin:origins){
            var found=level.findClosestBiome3d(biome->biome.is(target),origin,6400,64,64);
            if(found==null){Thaumcraft.LOG.info("TREE_CENSUS patch {} near {} not found",target.identifier().getPath(),origin);continue;}
            centers.add(new ChunkPos(found.getFirst().getX()>>4,found.getFirst().getZ()>>4));
            Thaumcraft.LOG.info("TREE_CENSUS patch {} at chunk {},{}",target.identifier().getPath(),centers.getLast().x(),centers.getLast().z());
        }
        // Load tickets let the worldgen workers build several patches at once, like spawn preparation.
        var ticket=new TicketType(TicketType.NO_TIMEOUT,TicketType.FLAG_LOADING);
        var source=level.getChunkSource();var server=level.getServer();
        // Patches around nearby targets overlap; each chunk is scanned once so no area counts twice.
        Set<ChunkPos> chunks=new java.util.HashSet<>();
        for(int first=0;first<centers.size();first+=threads){
            var batch=centers.subList(first,Math.min(centers.size(),first+threads));
            var loads=server.submit(()->batch.stream().map(center->source.addTicketAndLoadWithRadius(ticket,center,radius)).toList()).join();
            java.util.concurrent.CompletableFuture.allOf(loads.toArray(java.util.concurrent.CompletableFuture[]::new)).join();
            var loaded=server.submit(()->{
                List<LevelChunk> found=new java.util.ArrayList<>();
                for(ChunkPos center:batch)for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++){
                    var pos=new ChunkPos(center.x()+dx,center.z()+dz);
                    if(chunks.add(pos))found.add(java.util.Objects.requireNonNull(source.getChunkNow(pos.x(),pos.z()),"census chunk not loaded"));
                }
                return found;
            }).join();
            for(LevelChunk chunk:loaded)scan(level,chunk,stats);
            server.submit(()->batch.forEach(center->source.removeTicketWithRadius(ticket,center,radius))).join();
            Thaumcraft.LOG.info("TREE_CENSUS scanned {} chunks",chunks.size());
        }
        Thaumcraft.LOG.info("TREE_CENSUS chunks {}",chunks.size());
        for(var entry:stats.entrySet()){
            double[] s=entry.getValue();
            Thaumcraft.LOG.info("TREE_CENSUS yield {} chunks={} greatwood={} silverwood={} greatwoodPerChunk={} silverwoodPerChunk={}",entry.getKey(),String.format("%.1f",s[0]),(int)s[1],(int)s[2],String.format("%.4f",s[1]/s[0]),String.format("%.4f",s[2]/s[0]));
        }
    }

    private static Holder<Biome> surfaceBiome(ServerLevel level,LevelChunk chunk,int x,int z){
        int y=Math.max(level.getSeaLevel(),chunk.getHeight(Heightmap.Types.WORLD_SURFACE,x&15,z&15)-1);
        return chunk.getNoiseBiome(QuartPos.fromBlock(x),QuartPos.fromBlock(Math.min(y,level.getMaxY()-1)),QuartPos.fromBlock(z));
    }

    private static void scan(ServerLevel level,LevelChunk chunk,Map<String,double[]> stats){
        int baseX=chunk.getPos().getMinBlockX(),baseZ=chunk.getPos().getMinBlockZ();
        for(int sx=2;sx<16;sx+=4)for(int sz=2;sz<16;sz+=4)stats.computeIfAbsent(name(surfaceBiome(level,chunk,baseX+sx,baseZ+sz)),k->new double[3])[0]+=1/16.0;
        var greatwood=Content.block("greatwood_log");var silverwood=Content.block("silverwood_log");
        var pos=new BlockPos.MutableBlockPos();
        for(int x=baseX;x<baseX+16;x++)for(int z=baseZ;z<baseZ+16;z++){
            int top=chunk.getHeight(Heightmap.Types.WORLD_SURFACE,x&15,z&15);
            for(int y=top;y>top-64&&y>level.getMinY();y--){
                var state=chunk.getBlockState(pos.set(x,y,z));var below=chunk.getBlockState(pos.set(x,y-1,z));
                int species=-1;
                if(state.is(greatwood)&&state.getValue(ArcaneLogBlock.BRANCH)==4&&!below.is(greatwood))species=1;
                else if(state.is(silverwood)&&!below.is(silverwood)&&!below.isAir()&&!below.is(BlockTags.LEAVES))species=2;
                if(species>0){stats.computeIfAbsent(name(surfaceBiome(level,chunk,x,z)),k->new double[3])[species]++;break;}
                if(!(state.isAir()||state.is(BlockTags.LEAVES)||state.is(BlockTags.LOGS)||!state.blocksMotion()))break;
            }
        }
    }
}
