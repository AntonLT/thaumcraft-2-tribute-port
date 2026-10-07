package dev.thaumcraft.world;

import dev.thaumcraft.Thaumcraft;
import dev.thaumcraft.content.Content;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import java.util.Random;
import java.util.function.BiConsumer;

public final class ArcaneFeatures extends Feature<NoneFeatureConfiguration> {
    public static final String[] NAMES={"arcane_deposits","cinnabar_deposits","arcane_vegetation","monolith","arcane_treasure"};
    private final String kind;
    private ArcaneFeatures(String kind) {super(NoneFeatureConfiguration.CODEC);this.kind=kind;}
    public static void register(BiConsumer<String,Feature<?>> register) {
        for(String name:NAMES)register.accept(name,name.equals("cinnabar_deposits")?new CinnabarFeature():new ArcaneFeatures(name));
    }
    public static ResourceKey<PlacedFeature> placed(String name) {return ResourceKey.create(Registries.PLACED_FEATURE,Thaumcraft.id(name));}
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if(!dev.thaumcraft.PortConfig.worldGeneration)return false;
        // Kept registered for existing datapacks; surface generation now owns the priority chain.
        if(kind.equals("monolith"))return false;
        var world=context.level();var origin=context.origin();
        var cell=seedAura(world,origin);
        return switch(kind) {
            case "arcane_deposits" -> deposits(context);
            case "arcane_vegetation" -> surface(context,cell);
            case "arcane_treasure" -> treasure(world,origin,context.random());
            default -> false;
        };
    }
    static dev.thaumcraft.gameplay.ArcaneWorldData.Aura seedAura(WorldGenLevel world,BlockPos origin){
        var cell=dev.thaumcraft.gameplay.ArcaneWorldData.initialAura(world.getSeed(),origin.getX()>>4,origin.getZ()>>4,surfaceBiome(world,origin),world.getLevel().dimension()==net.minecraft.world.level.Level.NETHER);
        world.getLevel().getServer().execute(()->dev.thaumcraft.gameplay.ArcaneWorldData.get(world.getLevel()).seedAura(origin,cell));
        return cell;
    }
    private static boolean oreReplaceable(net.minecraft.world.level.block.state.BlockState state){
        return state.is(BlockTags.STONE_ORE_REPLACEABLES)||state.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES);
    }
    private boolean deposits(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        var level=context.level();var random=context.random();BlockPos origin=context.origin();boolean placed=false;
        String[] crystals={"vis_ore","vaporous_vis_ore","aqueous_vis_ore","earthen_vis_ore","fiery_vis_ore","tainted_vis_ore"};
        var biome=surfaceBiome(level,origin);
        int top=Math.min(85,level.getMaxY());
        int attempts=depositAttempts(25,85,level.getMinY(),top,random);
        for(int i=0;i<attempts;i++) {
            BlockPos pos=new BlockPos(origin.getX()+random.nextInt(16),level.getMinY()+random.nextInt(top-level.getMinY()),origin.getZ()+random.nextInt(16));
            if(!level.ensureCanWrite(pos)||!oreReplaceable(level.getBlockState(pos)))continue;
            int crystal=crystalByBiome(biome,random,1);
            var supports=new java.util.ArrayList<Direction>();
            for(Direction direction:Direction.values()){
                BlockPos support=pos.relative(direction.getOpposite());
                if(level.ensureCanWrite(support)&&oreReplaceable(level.getBlockState(support)))supports.add(direction);
            }
            if(!supports.isEmpty()){
                Direction facing=supports.get(random.nextInt(supports.size()));
                level.setBlock(pos,Content.block(crystals[crystal]).defaultBlockState().setValue(dev.thaumcraft.content.CrystalBlock.FACING,facing).setValue(dev.thaumcraft.content.CrystalBlock.AMOUNT,1+random.nextInt(4)),2);placed=true;
            }
        }
        return placed;
    }
    /** Preserve the original attempts per vertical layer, including nonstandard world floors. */
    public static int depositAttempts(int originalAttempts,int originalLayers,int bottom,int top,net.minecraft.util.RandomSource random){
        long scaled=(long)originalAttempts*Math.max(0,top-bottom);
        int attempts=(int)(scaled/originalLayers),remainder=(int)(scaled%originalLayers);
        return attempts+(remainder>0&&random.nextInt(originalLayers)<remainder?1:0);
    }
    public static net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> surfaceBiome(WorldGenLevel level,BlockPos column){
        var heightmap=level instanceof net.minecraft.server.level.ServerLevel?Heightmap.Types.WORLD_SURFACE:Heightmap.Types.WORLD_SURFACE_WG;
        int y=Math.max(level.getSeaLevel(),level.getHeight(heightmap,column.getX(),column.getZ())-1);
        return level.getBiome(new BlockPos(column.getX(),Math.min(y,level.getMaxY()-1),column.getZ()));
    }
    private boolean surface(FeaturePlaceContext<NoneFeatureConfiguration> context,dev.thaumcraft.gameplay.ArcaneWorldData.Aura aura){
        var level=context.level();var origin=context.origin();var random=context.random();
        boolean changed=false;
        if(dev.thaumcraft.PortConfig.taintSpawn>0&&aura.taint()>=dev.thaumcraft.PortConfig.auraMax*.5f)changed=taintedArea(level,origin,random);
        if(level.getLevel().dimension()==net.minecraft.world.level.Level.NETHER)return changed;
        if(dev.thaumcraft.PortConfig.monoliths&&monolith(context))return true;
        return vegetation(context,aura.vis())||changed;
    }
    /** Jungle hills were merged into the modern jungle biome's terrain variation. */
    public static boolean silverwoodBiome(net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> biome){
        return biome.is(BiomeTags.IS_FOREST)||biome.is(BiomeTags.IS_TAIGA)||biome.is(BiomeTags.IS_JUNGLE);
    }
    private boolean vegetation(FeaturePlaceContext<NoneFeatureConfiguration> context,float vis) {
        var level=context.level();var random=context.random();BlockPos origin=context.origin();
        for(int attempt=0;attempt<3;attempt++){
            BlockPos column=origin.offset(random.nextInt(16),0,random.nextInt(16));
            if(!level.ensureCanWrite(column))continue;
            // Legacy getHeightValue includes canopies, so trees cannot start beneath leaves.
            BlockPos pos=level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING,column);
            var biome=surfaceBiome(level,column);
            if(attempt==0&&vis>dev.thaumcraft.PortConfig.auraMax*.585f&&silverwoodBiome(biome)){
                if(new WorldGenSilverwood(false).generate(new TreeWorld(level),new Random(random.nextLong()),pos.getX(),pos.getY(),pos.getZ())){
                    flowers(level,pos,"shimmerleaf",random);return true;
                }
            } else if(attempt==1&&vis>dev.thaumcraft.PortConfig.auraMax*.53f&&(biome.is(BiomeTags.IS_FOREST)||biome.is(BiomeTags.IS_TAIGA)||biome.is(net.minecraft.world.level.biome.Biomes.PLAINS))){
                if(new WorldGenGreatwood(false).generate(new TreeWorld(level),new Random(random.nextLong()),pos.getX(),pos.getY(),pos.getZ()))return true;
            } else if(attempt==2&&vis>dev.thaumcraft.PortConfig.auraMax/9&&biome.is(net.minecraft.world.level.biome.Biomes.DESERT)&&random.nextInt(4)==0){
                return flowers(level,pos,"cinderpearl",random);
            }
        }
        return false;
    }
    public static boolean flowers(WorldGenLevel level,BlockPos origin,String id,net.minecraft.util.RandomSource random) {
        boolean placed=false;var flower=Content.block(id).defaultBlockState();
        for(int i=0;i<18;i++) {
            BlockPos pos=origin.offset(random.nextInt(8)-random.nextInt(8),random.nextInt(4)-random.nextInt(4),random.nextInt(8)-random.nextInt(8));
            if(!level.ensureCanWrite(pos)||!level.ensureCanWrite(pos.below()))continue;
            var ground=level.getBlockState(pos.below());
            if(level.getBlockState(pos).isAir()&&(ground.is(Blocks.GRASS_BLOCK)||ground.is(Blocks.SAND))){level.setBlock(pos,flower,2);placed=true;}
        }
        return placed;
    }
    /** The six original element weights use modern biome keys and tags. */
    public static int crystalByBiome(net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> biome,net.minecraft.util.RandomSource random,int darkAmount){
        boolean plains=biome.is(net.minecraft.world.level.biome.Biomes.PLAINS),mushroom=biome.is(net.minecraft.world.level.biome.Biomes.MUSHROOM_FIELDS);
        boolean jungle=biome.is(BiomeTags.IS_JUNGLE),mountain=biome.is(BiomeTags.IS_MOUNTAIN),desert=biome.is(net.minecraft.world.level.biome.Biomes.DESERT);
        boolean swamp=biome.is(net.minecraft.world.level.biome.Biomes.SWAMP)||biome.is(net.minecraft.world.level.biome.Biomes.MANGROVE_SWAMP),snowy=biome.value().getBaseTemperature()<.15f;
        int[] weights={1+(plains||mushroom||jungle?1:0),1+(desert||mountain||plains?1:0),1+(biome.is(BiomeTags.IS_OCEAN)||biome.is(BiomeTags.IS_RIVER)||snowy||swamp?1:0),1+(mountain||biome.is(BiomeTags.IS_TAIGA)||biome.is(BiomeTags.IS_FOREST)||jungle?1:0),1+(desert||biome.is(BiomeTags.IS_NETHER)||mountain?1:0),Math.max(0,darkAmount)+(darkAmount>0&&(mushroom||swamp)?1:0)};
        int roll=random.nextInt(java.util.Arrays.stream(weights).sum());
        for(int element=0;element<weights.length;element++){roll-=weights[element];if(roll<0)return element;}
        throw new AssertionError("Invalid crystal weights");
    }
    /** Original 500 triangular samples, using the same ordered corruption as live spread. */
    public static boolean taintedArea(WorldGenLevel level,BlockPos origin,net.minecraft.util.RandomSource random){
        var biome=surfaceBiome(level,origin);
        boolean nether=level.getLevel().dimension()==net.minecraft.world.level.Level.NETHER;
        var initial=dev.thaumcraft.gameplay.ArcaneWorldData.initialAura(level.getSeed(),origin.getX()>>4,origin.getZ()>>4,biome,nether);
        float sourceTaint=initial.taint();
        if(nether||biome.is(net.minecraft.world.level.biome.Biomes.SWAMP)||biome.is(net.minecraft.world.level.biome.Biomes.MANGROVE_SWAMP))sourceTaint/=1.5f;
        final float patchTaint=sourceTaint;
        var boosts=new java.util.LinkedHashMap<BlockPos,Float>();
        var data=dev.thaumcraft.gameplay.ArcaneWorldData.get(level.getLevel());
        BlockPos sourceChunk=new BlockPos((origin.getX()>>4)*16,0,(origin.getZ()>>4)*16);
        boolean changed=false;
        for(int i=0;i<500;i++){
            BlockPos column=origin.offset(8+random.nextInt(31)-random.nextInt(31),0,8+random.nextInt(31)-random.nextInt(31));
            BlockPos chunk=new BlockPos((column.getX()>>4)*16,0,(column.getZ()>>4)*16);
            // The original inserts the source aura after painting the patch. Other cells
            // use their existing taint, then the boost from their previous sample, for pods.
            var previous=chunk.equals(sourceChunk)?null:data.knownAura(chunk);
            float current=boosts.getOrDefault(chunk,previous==null?0:previous.taint());
            if(dev.thaumcraft.content.TaintBlock.canAccess(level,column)){
                BlockPos pos=level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG,column);
                changed|=dev.thaumcraft.content.TaintBlock.increase(level,pos,current,random);
            }
            if(previous!=null&&current<patchTaint*.8f)boosts.put(chunk,patchTaint*(.8f+random.nextFloat()*.25f));
        }
        var server=level.getLevel();
        server.getServer().execute(()->{
            boosts.forEach((pos,value)->data.raiseExistingTaint(pos,patchTaint*.8f,value));
        });
        return changed;
    }
    public static boolean hasTreasureChest(WorldGenLevel level,BlockPos origin){return treasureChests(level,origin,null);}
    /**
     * Original GenerateTreasure: every chest in the chunk with an empty slot counts as treasure, even when no roll hits.
     * Deferred loot-table chests are left packed; ChestTreasureMixin augments them when the table fills. Other chests
     * are augmented here when {@code random} is given, which happens once as the chunk generates.
     */
    private static boolean treasureChests(WorldGenLevel level,BlockPos origin,net.minecraft.util.RandomSource random){
        boolean found=false;
        for(BlockPos pos:level.getChunk(origin).getBlockEntitiesPos()){
            if(!(level.getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest))continue;
            boolean empty=chest.getLootTable()!=null;
            for(int slot=0;!empty&&slot<chest.getContainerSize();slot++)empty=chest.getItem(slot).isEmpty();
            if(!empty)continue;
            if(random==null)return true;
            found=true;
            if(chest.getLootTable()==null){dev.thaumcraft.gameplay.ArtifactLoot.fillOriginalTreasure(chest,random);chest.setChanged();}
        }
        return found;
    }
    public static boolean treasure(WorldGenLevel level,BlockPos origin,net.minecraft.util.RandomSource random){
        if(treasureChests(level,origin,random))return false;
        int x=origin.getX()+random.nextInt(16),z=origin.getZ()+random.nextInt(16),y=level.getMinY()+5;
        while(y<level.getMaxY()-2){
            BlockPos candidate=new BlockPos(x,y+1,z);
            if(!level.ensureCanWrite(candidate)||!level.ensureCanWrite(candidate.above()))return false;
            if(level.getBlockState(candidate).isAir()&&level.getBlockState(candidate.above()).isAir())break;
            y++;
        }
        y++;if(y>=level.getMaxY()-1)return false;
        boolean covered=false;
        for(int above=y+1;above<level.getMaxY();above++){
            BlockPos candidate=new BlockPos(x,above,z);if(!level.ensureCanWrite(candidate))return false;
            if(level.getBlockState(candidate).is(BlockTags.STONE_ORE_REPLACEABLES)||level.getBlockState(candidate).is(BlockTags.DEEPSLATE_ORE_REPLACEABLES)){covered=true;break;}
        }
        if(!covered||random.nextInt(10)!=0)return false;
        BlockPos pos=new BlockPos(x,y,z);
        for(BlockPos target:BlockPos.betweenClosed(pos.offset(-1,-1,-1),pos.offset(1,1,1)))if(!level.ensureCanWrite(target)||level.getBlockEntity(target)!=null)return false;
        for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++){
            level.setBlock(pos.offset(dx,-1,dz),(random.nextBoolean()?Blocks.COBBLESTONE:Blocks.MOSSY_COBBLESTONE).defaultBlockState(),2);
            level.setBlock(pos.offset(dx,0,dz),Blocks.AIR.defaultBlockState(),2);
            level.setBlock(pos.offset(dx,1,dz),Blocks.AIR.defaultBlockState(),2);
        }
        level.setBlock(pos,Blocks.CHEST.defaultBlockState(),2);
        if(level.getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest){
            var params=new net.minecraft.world.level.storage.loot.LootParams.Builder(level.getLevel()).withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,net.minecraft.world.phys.Vec3.atCenterOf(pos)).create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST);
            level.getLevel().getServer().reloadableRegistries().getLootTable(net.minecraft.world.level.storage.loot.BuiltInLootTables.SIMPLE_DUNGEON).fill(chest,params,random.nextLong());
        }
        return true;
    }
    private boolean monolith(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        var level=context.level();BlockPos origin=context.origin();
        if(context.random().nextInt(50)!=0)return false;
        BlockPos center=level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,origin.offset(8,0,8)).below();
        return WorldGenMonolith.generate(level,center,context.random());
    }
}
