package dev.thaumcraft.world;

import dev.thaumcraft.content.Content;
import dev.thaumcraft.content.TaintBlock;
import dev.thaumcraft.gameplay.ArcaneWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class ArcaneLogBlock extends RotatedPillarBlock {
    public static final net.minecraft.world.level.block.state.properties.IntegerProperty BRANCH=net.minecraft.world.level.block.state.properties.IntegerProperty.create("branch",0,4);
    public ArcaneLogBlock(Properties properties){super(properties);registerDefaultState(defaultBlockState().setValue(BRANCH,0));}
    @Override public void animateTick(BlockState state,net.minecraft.world.level.Level level,BlockPos pos,RandomSource random){dev.thaumcraft.content.VisualEffects.ambient.accept(level,pos);}
    @Override protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<net.minecraft.world.level.block.Block,BlockState> builder){super.createBlockStateDefinition(builder);builder.add(BRANCH);}
    @Override protected void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random) {
        if(!state.is(Content.block("silverwood_log"))&&!state.is(Content.block("tainted_log")))return;
        var data=ArcaneWorldData.get(level);var aura=data.aura(level,pos);
        if(state.is(Content.block("silverwood_log"))) {
            if(aura.badVibes()>0)data.addVibes(level,pos,0,-1);else data.addVibes(level,pos,1,0);
        } else {
            if(TaintBlock.shouldHeal(aura,random)){TaintBlock.purify(level,pos);return;}
            TaintBlock.spreadFrom(level,pos,aura,random);
            if(dev.thaumcraft.PortConfig.taintSpread&&random.nextInt(10)==0) {
                if(aura.goodVibes()>0)data.addVibes(level,pos,-1,0);else data.addVibes(level,pos,0,1);
            }
        }
    }
    @Override protected void affectNeighborsAfterRemoval(BlockState state,ServerLevel level,BlockPos pos,boolean moving) {
        super.affectNeighborsAfterRemoval(state,level,pos,moving);
        if(state.is(Content.block("tainted_log")))TaintMemory.get(level).forget(pos);
    }
}
