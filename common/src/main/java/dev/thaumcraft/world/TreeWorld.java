package dev.thaumcraft.world;

import dev.thaumcraft.content.Content;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Narrow adapter retaining the original tree geometry calculations and block tests. */
public final class TreeWorld {
    private final LevelAccessor level;
    private final Set<BlockPos> placedLeaves=new HashSet<>();
    public TreeWorld(LevelAccessor level) {this.level=level;}
    public int getBlockId(int x,int y,int z) {
        BlockPos pos=new BlockPos(x,y,z);
        if(!canAccess(pos))return -1;
        BlockState state=level.getBlockState(pos);
        // Legacy tall grass, ferns, snow and vines were real blocks that stop trunks and leaves.
        if(state.isAir())return 0;
        if(state.is(Blocks.GRASS_BLOCK))return 2;
        if(state.is(Blocks.DIRT)||state.is(Blocks.PODZOL)||state.is(Blocks.COARSE_DIRT)||state.is(Blocks.MYCELIUM))return 3;
        if(state.is(BlockTags.LEAVES) || state.getBlock() instanceof LeavesBlock)return 18;
        if(state.is(BlockTags.LOGS))return 17;
        return -1;
    }
    public void setBlock(int x,int y,int z,int id,int meta,boolean notify) {
        BlockPos pos=new BlockPos(x,y,z);
        if(!canAccess(pos))return;
        BlockState state=switch(id) {
            case 0 -> Blocks.AIR.defaultBlockState();
            case 2 -> Blocks.GRASS_BLOCK.defaultBlockState();
            case 3 -> Blocks.DIRT.defaultBlockState();
            case 247 -> Content.block(meta==0?"silverwood_log":"greatwood_log").defaultBlockState().setValue(ArcaneLogBlock.BRANCH,meta>=6&&meta<=9?meta-5:0);
            case 248 -> Content.block(meta==1?"silverwood_leaves":"greatwood_leaves").defaultBlockState();
            default -> throw new IllegalArgumentException("Unexpected tree block "+id);
        };
        level.setBlock(pos,state,notify?3:2);
        if(id==248)placedLeaves.add(pos);
    }
    /** Resolve natural canopy distance after the original generator has placed its logs. */
    public void finishTree() {
        Map<BlockPos,Integer> distances=new HashMap<>();
        ArrayDeque<BlockPos> queue=new ArrayDeque<>();
        for(BlockPos pos:placedLeaves) {
            if(!(level.getBlockState(pos).getBlock() instanceof LeavesBlock))continue;
            for(Direction direction:Direction.values())if(canAccess(pos.relative(direction))&&level.getBlockState(pos.relative(direction)).is(BlockTags.LOGS)) {
                distances.put(pos,1);
                queue.add(pos);
                break;
            }
        }
        while(!queue.isEmpty()) {
            BlockPos pos=queue.removeFirst();
            int next=distances.get(pos)+1;
            if(next>4)continue;
            for(Direction direction:Direction.values()) {
                BlockPos neighbor=pos.relative(direction);
                if(!placedLeaves.contains(neighbor)||distances.getOrDefault(neighbor,7)<=next)continue;
                if(!(level.getBlockState(neighbor).getBlock() instanceof LeavesBlock))continue;
                distances.put(neighbor,next);
                queue.add(neighbor);
            }
        }
        for(BlockPos pos:placedLeaves) {
            BlockState state=level.getBlockState(pos);
            if(state.getBlock() instanceof LeavesBlock)level.setBlock(pos,state.setValue(LeavesBlock.DISTANCE,distances.getOrDefault(pos,7)),2);
        }
        placedLeaves.clear();
    }
    private boolean canAccess(BlockPos pos) {
        return !level.isOutsideBuildHeight(pos)&&(level instanceof net.minecraft.world.level.WorldGenLevel world?world.ensureCanWrite(pos):level.hasChunkAt(pos));
    }
}
