package dev.thaumcraft.content;

import dev.thaumcraft.gameplay.ArcaneWorldData;
import dev.thaumcraft.entity.ModEntities;
import dev.thaumcraft.entity.WispEntity;
import dev.thaumcraft.world.TaintPodBlock;
import dev.thaumcraft.world.TaintMemory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class TaintBlock extends Block {
    /** Blocks and entity types taint never converts. */
    public static final net.minecraft.tags.TagKey<Block> IMMUNE=net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK,dev.thaumcraft.Thaumcraft.id("taint_immune"));
    public static final net.minecraft.tags.TagKey<net.minecraft.world.entity.EntityType<?>> IMMUNE_ENTITIES=net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE,dev.thaumcraft.Thaumcraft.id("taint_immune"));
    public static final net.minecraft.world.level.block.state.properties.IntegerProperty MATERIAL=net.minecraft.world.level.block.state.properties.IntegerProperty.create("material",0,15);
    public TaintBlock(Properties properties){this("tainted_grass",properties);}
    public TaintBlock(String id,Properties properties){super(properties);registerDefaultState(stateDefinition.any().setValue(MATERIAL,switch(id){
        case "tainted_sand","tainted_sandstone"->1;
        case "tainted_stone","tainted_cobblestone"->2;
        case "tainted_gravel"->3;
        case "tainted_clay"->15;
        default->0;
    }));}
    @Override protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block,BlockState> builder){builder.add(MATERIAL);}
    @Override protected java.util.List<net.minecraft.world.item.ItemStack> getDrops(BlockState state,net.minecraft.world.level.storage.loot.LootParams.Builder params){
        int material=state.getValue(MATERIAL);if(material<=4||material==15)return java.util.List.of();
        var tool=params.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.TOOL);
        int fortune=tool==null?0:net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(params.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.FORTUNE),tool);
        return params.getLevel().getRandom().nextInt(16)<material+fortune?java.util.List.of(new net.minecraft.world.item.ItemStack(Content.item("congealed_taint"))):java.util.List.of();
    }
    @Override protected void affectNeighborsAfterRemoval(BlockState state,ServerLevel level,BlockPos pos,boolean moving){super.affectNeighborsAfterRemoval(state,level,pos,moving);TaintMemory.get(level).forget(pos);}
    @Override protected void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random) {
        var data=ArcaneWorldData.get(level);
        var aura=data.aura(level,pos);
        if(shouldHeal(aura,random)){purify(level,pos);return;}
        spreadFrom(level,pos,aura,random);
    }
    public static void spreadFrom(ServerLevel level,BlockPos pos,ArcaneWorldData.Aura aura,RandomSource random) {
        if(!dev.thaumcraft.PortConfig.taintSpread||aura.taint()<dev.thaumcraft.PortConfig.auraMax*.25f||aura.taint()<dev.thaumcraft.PortConfig.auraMax*.375f&&random.nextInt(6)!=0||aura.taint()<dev.thaumcraft.PortConfig.auraMax*.5f&&random.nextInt(3)!=0)return;
        increase(level,pos,aura.taint(),random); // Checks silverwood protection itself.
    }
    /** Original ordered 3x3x3 scan converts at most one target in each update. */
    public static boolean increase(ServerLevel level,BlockPos pos,float taint,RandomSource random){
        if(!dev.thaumcraft.PortConfig.taintSpread)return false;
        return increase((WorldGenLevel)level,pos,taint,random);
    }
    public static boolean increase(WorldGenLevel level,BlockPos pos,float taint,RandomSource random){
        // Silverwood prevents every effect below. The scan runs only once a block could change: the world is untouched
        // until then, so the answer matches a check made up front.
        int protection=0; // 0 unknown, 1 open, 2 protected
        Block taintweed=Content.block("taintweed"),glowingTaintweed=Content.block("glowing_taintweed");
        for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++){
            BlockPos target=pos.offset(x,y,z);if(!canAccess(level,target))continue;
            BlockState state=level.getBlockState(target),converted=taint(state);
            if(converted!=null){
                boolean foliage=converted.is(Content.block("tainted_log"))||converted.is(Content.block("tainted_leaves"));
                boolean crystal=converted.getBlock() instanceof CrystalBlock;
                if(converted.is(Content.block("tainted_leaves"))&&!nearArcaneLog(level,target,2))continue;
                boolean exposed=false;
                for(var direction:net.minecraft.core.Direction.values()){
                    BlockPos neighbor=target.relative(direction);if(canAccess(level,neighbor)&&!level.getBlockState(neighbor).isSolidRender()){exposed=true;break;}
                }
                if(foliage||crystal||exposed){
                    if(protection==0)protection=protectedBySilverwood(level,pos)?2:1;
                    if(protection==2)return false;
                    if(taint(level,target)){roots(level,target,random);return true;}
                }
            }
            if(state.getBlock() instanceof TaintBlock&&canAccess(level,target.above())&&level.getBlockState(target.above()).isAir()){
                if(protection==0)protection=protectedBySilverwood(level,pos)?2:1;
                if(protection==2)return false;
                if(!spreadAllowed(level,target.above()))continue;
                if(TaintPodBlock.tryGrow(level,target,taint,random)){roots(level,target.above(),random);return true;}
                boolean nearbyPlant=false;
                for(BlockPos nearby:BlockPos.betweenClosed(target.offset(-3,-2,-3),target.offset(3,4,3))){
                    if(!canAccess(level,nearby))continue;
                    BlockState plant=level.getBlockState(nearby);
                    if(plant.is(taintweed)||plant.is(glowingTaintweed)){nearbyPlant=true;break;}
                }
                if(!nearbyPlant){
                    boolean planted=level.setBlock(target.above(),Content.block(random.nextInt(4)==0?"glowing_taintweed":"taintweed").defaultBlockState(),level instanceof ServerLevel?3:2);
                    if(planted)roots(level,target.above(),random);
                    return planted;
                }
            }
        }
        return false;
    }
    private static void roots(WorldGenLevel level,BlockPos pos,RandomSource random){
        if(level instanceof ServerLevel server)ModSounds.play(server,pos,"roots",net.minecraft.sounds.SoundSource.BLOCKS,.05f,1.1f+random.nextFloat()*.2f);
    }
    private static boolean nearArcaneLog(WorldGenLevel level,BlockPos pos,int radius){
        for(BlockPos nearby:BlockPos.betweenClosed(pos.offset(-radius,-radius,-radius),pos.offset(radius,radius,radius)))
            if(canAccess(level,nearby)&&level.getBlockState(nearby).getBlock() instanceof dev.thaumcraft.world.ArcaneLogBlock)return true;
        return false;
    }
    @Override public void stepOn(Level level,BlockPos pos,BlockState state,Entity entity) {
        super.stepOn(level,pos,state,entity);
        if(level instanceof ServerLevel server)tryConvert(server,pos,entity);
    }
    public static boolean tryConvert(ServerLevel level,BlockPos pos,Entity entity) {
        if(!dev.thaumcraft.PortConfig.taintSpread||!(entity instanceof LivingEntity living)||!living.isAlive()||entity.isPassenger()||entity.isVehicle())return false;
        float taint=ArcaneWorldData.get(level).aura(level,pos).taint();
        if(level.getRandom().nextInt(100)>(int)(taint/(dev.thaumcraft.PortConfig.auraMax/30)))return false;
        return convert(level,entity);
    }
    public static boolean convert(ServerLevel level,Entity entity) {
        if(!(entity instanceof LivingEntity living)||entity instanceof net.minecraft.world.entity.player.Player||!living.isAlive()||entity.isPassenger()||entity.isVehicle())return false;
        if(!spreadAllowed(level,entity.blockPosition()))return false;
        if(entity instanceof WispEntity wisp){wisp.setElement(5);return true;}
        if(BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(entity.getType()).is(IMMUNE_ENTITIES))return false;
        var rule=dev.thaumcraft.gameplay.GameData.taintEntityRule(entity.getType());
        String kind=BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath();
        var type=rule!=null?BuiltInRegistries.ENTITY_TYPE.getValue(net.minecraft.resources.Identifier.parse(rule.result())):ModEntities.TYPES.get("tainted_"+kind);
        if(type==null)return false;
        var replacement=type.create(level,EntitySpawnReason.CONVERSION);
        if(!(replacement instanceof LivingEntity tainted))return false;
        tainted.snapTo(entity.getX(),entity.getY(),entity.getZ(),entity.getYRot(),entity.getXRot());
        tainted.setHealth(Math.max(1,living.getHealth()/living.getMaxHealth()*tainted.getMaxHealth()));
        tainted.setCustomName(entity.getCustomName());tainted.setCustomNameVisible(entity.isCustomNameVisible());
        if(entity instanceof net.minecraft.world.entity.Mob mob&&mob.isPersistenceRequired()&&tainted instanceof net.minecraft.world.entity.Mob target)target.setPersistenceRequired();
        if(!level.addFreshEntity(tainted))return false;
        entity.discard();return true;
    }
    public static void explosion(ServerLevel level,BlockPos pos){
        level.explode(null,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,1,Level.ExplosionInteraction.BLOCK);
        for(BlockPos target:BlockPos.betweenClosed(pos.offset(-2,-2,-2),pos.offset(2,2,2)))
            if(level.hasChunkAt(target))increase(level,target,ArcaneWorldData.get(level).aura(level,target).taint(),level.getRandom());
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.WITCH,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,100,1,1,1,.02);
    }
    public static boolean protectedBySilverwood(WorldGenLevel level,BlockPos pos) {
        Block silverwood=Content.block("silverwood_log"),totem=Content.block("totem_of_dawn");
        for(BlockPos nearby:BlockPos.betweenClosed(pos.offset(-3,-3,-3),pos.offset(3,3,3))){
            if(!canAccess(level,nearby))continue;
            BlockState state=level.getBlockState(nearby);
            if(state.is(silverwood)||state.is(totem))return true;
        }
        return false;
    }
    public static boolean shouldHeal(ArcaneWorldData.Aura aura,RandomSource random) {
        return aura.taint()<dev.thaumcraft.PortConfig.auraMax*.25f&&(aura.vis()>dev.thaumcraft.PortConfig.auraMax*.3f||aura.vis()>dev.thaumcraft.PortConfig.auraMax*.2f&&random.nextInt(5)==0||aura.vis()>dev.thaumcraft.PortConfig.auraMax*.1f&&random.nextInt(10)==0);
    }
    /** Generation scans must not request unavailable chunks or mutate saved data off-thread. */
    public static boolean canAccess(WorldGenLevel level,BlockPos pos){
        if(level.isOutsideBuildHeight(pos))return false;
        if(level instanceof ServerLevel server)return server.hasChunkAt(pos);
        if(level instanceof net.minecraft.server.level.WorldGenRegion region){
            var center=region.getCenter();
            if(Math.abs((pos.getX()>>4)-center.x())>1||Math.abs((pos.getZ()>>4)-center.z())>1)return false;
        }
        return level.ensureCanWrite(pos);
    }
    public static boolean taint(WorldGenLevel level,BlockPos pos) {
        if(!canAccess(level,pos))return false;
        var entity=level.getBlockEntity(pos);
        if(entity!=null&&!(entity instanceof dev.thaumcraft.world.VisualBlockEntity))return false;
        BlockState original=level.getBlockState(pos),corrupted=taint(original);
        if(corrupted==null||!spreadAllowed(level,pos))return false;
        // Addon rules have no material to restore from, so their originals are always remembered.
        boolean remember=dev.thaumcraft.gameplay.GameData.taintRule(original)!=null||corrupted.is(Content.block("tainted_log"))||corrupted.is(Content.block("tainted_leaves"))||BuiltInRegistries.BLOCK.getKey(original.getBlock()).getPath().startsWith("deepslate");
        if(!level.setBlock(pos,corrupted,level instanceof ServerLevel?3:2))return false;
        if(remember){
            BlockPos saved=pos.immutable();var server=level.getLevel();
            server.getServer().execute(()->TaintMemory.get(server).remember(saved,original));
        }
        return true;
    }
    /** Addons are not asked during world generation, which may run off the server thread. */
    private static boolean spreadAllowed(WorldGenLevel level,BlockPos pos){
        return !(level instanceof ServerLevel server)||dev.thaumcraft.api.ThaumcraftEvents.TAINT_SPREADING.allows(listener->listener.allow(server,pos.immutable()));
    }
    public static BlockState restore(ServerLevel level,BlockPos pos) {
        return TaintMemory.get(level).consume(pos).orElseGet(()->restore(level.getBlockState(pos)));
    }
    /** Potion cleanup retains the original material limit and one-in-three pod chance. */
    public static boolean purifyPotion(ServerLevel level,BlockPos pos) {
        if(!level.hasChunkAt(pos))return false;
        BlockState state=level.getBlockState(pos);
        if(state.getBlock() instanceof TaintBlock&&state.getValue(MATERIAL)>=10)return false;
        if(state.is(Content.block("taint_spore_pod"))&&level.getRandom().nextInt(3)!=0)return false;
        return purify(level,pos);
    }
    public static boolean purify(ServerLevel level,BlockPos pos) {
        if(!level.hasChunkAt(pos))return false;
        BlockState state=level.getBlockState(pos);
        boolean foliage=state.is(Content.block("tainted_log"))||state.is(Content.block("tainted_leaves"));
        boolean plant=state.is(Content.block("taintweed"))||state.is(Content.block("glowing_taintweed"))||state.is(Content.block("taint_spore_pod"));
        boolean addon=!(state.getBlock() instanceof TaintBlock)&&!foliage&&!plant;
        // An addon result is only cleansed where taint converted it, so that placed blocks stay.
        if(addon&&(!dev.thaumcraft.gameplay.GameData.taintResult(state)||TaintMemory.get(level).original(pos).isEmpty()))return false;
        BlockState restored=plant?Blocks.AIR.defaultBlockState():TaintMemory.get(level).original(pos).orElseGet(()->restore(state));
        if(!level.setBlockAndUpdate(pos,restored))return false;
        TaintMemory.get(level).forget(pos);return true;
    }
    public static BlockState taint(BlockState state) {
        if(state.is(IMMUNE))return null;
        var rule=dev.thaumcraft.gameplay.GameData.taintRule(state);
        if(rule!=null){
            var block=BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.parse(rule.result()));
            if(state.is(block))return null;
            var result=block.withPropertiesOf(state);
            return rule.material()!=null&&result.hasProperty(MATERIAL)?result.setValue(MATERIAL,rule.material()):result;
        }
        if(state.getBlock() instanceof CrystalBlock&&!state.is(Content.block("tainted_vis_ore")))return Content.block("tainted_vis_ore").withPropertiesOf(state);
        if(state.is(net.minecraft.tags.BlockTags.LOGS)&&!state.is(Content.block("silverwood_log"))&&!state.is(Content.block("tainted_log"))&&!state.is(Content.block("petrified_log")))return Content.block("tainted_log").withPropertiesOf(state);
        if(state.is(net.minecraft.tags.BlockTags.LEAVES)&&!state.is(Content.block("silverwood_leaves"))&&!state.is(Content.block("tainted_leaves")))return Content.block("tainted_leaves").withPropertiesOf(state);
        String result="tainted_stone";int material;
        if(state.is(Blocks.GRASS_BLOCK)||state.is(Blocks.DIRT)||state.is(Blocks.COARSE_DIRT)||state.is(Blocks.PODZOL)||state.is(Blocks.FARMLAND)){result="tainted_soil";material=0;}
        else if(state.is(Blocks.SAND)||state.is(Blocks.SANDSTONE)){result="tainted_sand";material=1;}
        else if(state.is(Blocks.STONE)||state.is(Blocks.DEEPSLATE))material=2;
        else if(state.is(Blocks.GRAVEL)){result="tainted_gravel";material=3;}
        else if(state.is(Blocks.MYCELIUM)){result="tainted_soil";material=4;}
        else if(state.is(Blocks.COAL_ORE)||state.is(Blocks.DEEPSLATE_COAL_ORE))material=5;
        else if(state.is(Blocks.DIAMOND_ORE)||state.is(Blocks.DEEPSLATE_DIAMOND_ORE))material=6;
        else if(state.is(Blocks.GOLD_ORE)||state.is(Blocks.DEEPSLATE_GOLD_ORE))material=7;
        else if(state.is(Blocks.IRON_ORE)||state.is(Blocks.DEEPSLATE_IRON_ORE))material=8;
        else if(state.is(Blocks.REDSTONE_ORE)||state.is(Blocks.DEEPSLATE_REDSTONE_ORE))material=9;
        else if(state.is(Blocks.LAPIS_ORE)||state.is(Blocks.DEEPSLATE_LAPIS_ORE))material=11;
        else if(state.is(Content.block("cinnabar_ore")))material=12;
        else if(state.is(Blocks.NETHERRACK)||state.is(Blocks.SOUL_SAND)||state.is(Blocks.GLOWSTONE)||state.is(Blocks.CLAY)||state.is(Blocks.PUMPKIN)||state.is(Blocks.MELON)||state.is(Blocks.CACTUS)||state.is(Blocks.SPONGE)||state.is(net.minecraft.tags.BlockTags.WOOL)){result="tainted_clay";material=15;}
        else return null;
        return Content.block(result).defaultBlockState().setValue(MATERIAL,material);
    }
    public static BlockState restore(BlockState state) {
        String path=net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        if(path.equals("tainted_log")||path.equals("tainted_leaves"))return Blocks.AIR.defaultBlockState();
        if(path.equals("tainted_cobblestone"))return Blocks.COBBLESTONE.defaultBlockState();
        if(!(state.getBlock() instanceof TaintBlock))return state;
        return switch(state.getValue(MATERIAL)){
            case 0->Blocks.DIRT.defaultBlockState();case 1->Blocks.SAND.defaultBlockState();case 2->Blocks.STONE.defaultBlockState();case 3->Blocks.GRAVEL.defaultBlockState();case 4->Blocks.MYCELIUM.defaultBlockState();
            case 5->Blocks.COAL_ORE.defaultBlockState();case 6->Blocks.DIAMOND_ORE.defaultBlockState();case 7->Blocks.GOLD_ORE.defaultBlockState();case 8->Blocks.IRON_ORE.defaultBlockState();case 9->Blocks.REDSTONE_ORE.defaultBlockState();
            case 11->Blocks.LAPIS_ORE.defaultBlockState();case 12->Content.block("cinnabar_ore").defaultBlockState();default->Blocks.AIR.defaultBlockState();
        };
    }
}
