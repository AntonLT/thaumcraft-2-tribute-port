package dev.thaumcraft.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

public final class TemporarySpaceBlock extends Block implements net.minecraft.world.level.block.EntityBlock {
    public TemporarySpaceBlock(Properties properties){super(properties);}
    @Override public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new VisualBlockEntity(pos,state);}
    @Override protected void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){TemporarySpace.get(level).restore(level,pos);}
    @Override protected void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){dev.thaumcraft.gameplay.ArcaneWorldData.get(level).addVibes(level,pos,0,1);}
    @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.INVISIBLE;}
}
