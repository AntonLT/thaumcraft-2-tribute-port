package dev.thaumcraft.entity;

import dev.thaumcraft.PortConfig;
import dev.thaumcraft.gameplay.ArcaneWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.List;

public final class ModSpawns {
    public static final List<String> NATURAL=List.of("brainy_zombie","thaum_slime","wisp","tainted_tree");
    @FunctionalInterface public interface Registrar {<T extends Mob> void register(EntityType<T> type,SpawnPlacementType placement,Heightmap.Types height,SpawnPlacements.SpawnPredicate<T> predicate);}
    private ModSpawns() {}
    public static int weight(String id) {return id.equals("brainy_zombie")?6:id.equals("tainted_tree")?3:4;}
    // 1.2.5 natural spawning ignored the registered brainy group size and tried four positions.
    public static int minCount(String id){return id.equals("brainy_zombie")?4:1;}
    public static int maxCount(String id){return id.equals("brainy_zombie")||id.equals("thaum_slime")?4:1;}
    @SuppressWarnings("unchecked") public static void register(Registrar registrar) {
        for(String id:NATURAL)registrar.register((EntityType<Mob>)ModEntities.TYPES.get(id),id.equals("wisp")?SpawnPlacementTypes.NO_RESTRICTIONS:SpawnPlacementTypes.ON_GROUND,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,ModSpawns::canSpawn);
    }
    private static <T extends Mob> boolean canSpawn(EntityType<T> type,ServerLevelAccessor access,EntitySpawnReason reason,BlockPos pos,RandomSource random) {
        if(!PortConfig.naturalSpawns||access.getDifficulty()==Difficulty.PEACEFUL)return false;
        String id=BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath();
        if(id.startsWith("tainted_")) {
            var cell=ArcaneWorldData.get(access.getLevel()).aura(access.getLevel(),pos);
            if(cell.taint()<PortConfig.auraMax*.4f||!Mob.checkMobSpawnRules(type,access,reason,pos,random))return false;
            if(id.equals("tainted_tree")) {
                if(!access.getLevel().getEntitiesOfClass(TaintedTree.class,new net.minecraft.world.phys.AABB(pos).inflate(64,12,64)).isEmpty())return false;
                for(BlockPos check:BlockPos.betweenClosed(pos.offset(-1,0,-1),pos.offset(1,4,1)))
                    if(!access.hasChunkAt(check)||!(access.getBlockState(check).isAir()||access.getBlockState(check).is(net.minecraft.world.level.block.Blocks.SHORT_GRASS)))return false;
                return darkEnough(access,pos,random);
            }
            return random.nextBoolean();
        }
        if(id.equals("wisp")||id.equals("thaum_slime")) {
            int x=pos.getX()>>4,z=pos.getZ()>>4;
            boolean arcaneChunk=net.minecraft.world.level.levelgen.WorldgenRandom.seedSlimeChunk(x,z,access.getLevel().getSeed(),987234911L).nextInt(10)==0;
            if(!arcaneChunk||!access.getFluidState(pos).isEmpty())return false;
            if(id.equals("wisp"))return random.nextBoolean()&&access.canSeeSky(pos.above())&&access.getBlockState(pos).isAir();
            return random.nextInt(8)==0&&Mob.checkMobSpawnRules(type,access,reason,pos,random);
        }
        if(id.equals("brainy_zombie")&&(access.getLevel().dimension()==net.minecraft.world.level.Level.NETHER||access.getBiome(pos).is(net.minecraft.world.level.biome.Biomes.MUSHROOM_FIELDS)))return false;
        return (EntitySpawnReason.ignoresLightRequirements(reason)||darkEnough(access,pos,random))&&Mob.checkMobSpawnRules(type,access,reason,pos,random);
    }
    public static boolean darkEnough(ServerLevelAccessor level,BlockPos pos,RandomSource random){
        // 1.2.5 monsters accepted block light 0..7 probabilistically. Modern Overworld monsters require zero.
        if(level.getBrightness(net.minecraft.world.level.LightLayer.SKY,pos)>random.nextInt(32))return false;
        int light=level.getLevel().isThundering()?level.getMaxLocalRawBrightness(pos,10):level.getMaxLocalRawBrightness(pos);
        return light<=random.nextInt(8);
    }
}
