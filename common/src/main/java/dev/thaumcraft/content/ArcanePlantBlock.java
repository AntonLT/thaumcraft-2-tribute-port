package dev.thaumcraft.content;

import com.mojang.serialization.MapCodec;
import dev.thaumcraft.world.TreeWorld;
import dev.thaumcraft.world.WorldGenGreatwood;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Random;

public final class ArcanePlantBlock extends VegetationBlock implements BonemealableBlock {
    public static final MapCodec<ArcanePlantBlock> CODEC=simpleCodec(ArcanePlantBlock::new);
    public ArcanePlantBlock(Properties properties) {super(properties);}
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,RandomSource random){VisualEffects.ambient.accept(level,pos);}
    @Override protected MapCodec<? extends VegetationBlock> codec() {return CODEC;}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) {return Block.box(2,0,2,14,12,14);}
    @Override protected boolean mayPlaceOn(BlockState state,BlockGetter level,BlockPos pos) {
        return super.mayPlaceOn(state,level,pos) || state.is(Blocks.SAND) || state.getBlock() instanceof TaintBlock;
    }
    @Override protected java.util.List<net.minecraft.world.item.ItemStack> getDrops(BlockState state,net.minecraft.world.level.storage.loot.LootParams.Builder params){
        var tool=params.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.TOOL);
        if(tool!=null&&tool.is(net.minecraft.world.item.Items.SHEARS))return java.util.List.of(new net.minecraft.world.item.ItemStack(this));
        if(state.is(Content.block("cinderpearl")))return java.util.List.of(new net.minecraft.world.item.ItemStack(Content.item("cinderpearl_pod")));
        if(state.is(Content.block("shimmerleaf"))||state.is(Content.block("greatwood_sapling")))return java.util.List.of(new net.minecraft.world.item.ItemStack(this));
        if(params.getLevel().getRandom().nextInt(10)!=0)return java.util.List.of();
        var artifact=Content.ENTRIES.stream().filter(entry->entry.source_class().equals("ItemArtifactTainted")&&entry.meta()==0).findFirst().orElseThrow();
        return java.util.List.of(new net.minecraft.world.item.ItemStack(Content.item(artifact.id())));
    }
    @Override protected void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random) {
        if(state.is(Content.block("greatwood_sapling")) && level.getMaxLocalRawBrightness(pos.above())>=9 && random.nextInt(25)==0 && dev.thaumcraft.gameplay.ArcaneWorldData.get(level).aura(level,pos).vis()>dev.thaumcraft.PortConfig.auraMax*.5f)grow(level,pos,random);
        else if(state.is(Content.block("taintweed"))||state.is(Content.block("glowing_taintweed"))) {
            var aura=dev.thaumcraft.gameplay.ArcaneWorldData.get(level).aura(level,pos);
            if(TaintBlock.shouldHeal(aura,random))level.removeBlock(pos,false);
            else TaintBlock.spreadFrom(level,pos,aura,random);
        }
    }
    private void grow(ServerLevel level,BlockPos pos,RandomSource random) {
        BlockState original=level.getBlockState(pos);
        level.removeBlock(pos,false);
        if(!new WorldGenGreatwood(true).generate(new TreeWorld(level),new Random(random.nextLong()),pos.getX(),pos.getY(),pos.getZ()))level.setBlock(pos,original,3);
    }
    @Override public boolean isValidBonemealTarget(LevelReader level,BlockPos pos,BlockState state) {return state.is(Content.block("greatwood_sapling"));}
    @Override public boolean isBonemealSuccess(Level level,RandomSource random,BlockPos pos,BlockState state) {return random.nextFloat()<0.45f;}
    @Override public void performBonemeal(ServerLevel level,RandomSource random,BlockPos pos,BlockState state) {grow(level,pos,random);}
}
