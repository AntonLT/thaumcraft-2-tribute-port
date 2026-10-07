package dev.thaumcraft.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import java.util.EnumMap;
import java.util.Map;

public final class ConduitBlock extends MachineBlock {
    public static final Map<Direction,BooleanProperty> CONNECTIONS=new EnumMap<>(Direction.class);
    static {for(var direction:Direction.values())CONNECTIONS.put(direction,BooleanProperty.create(direction.getSerializedName()));}
    public ConduitBlock(Properties props){super(props);BlockState state=defaultBlockState();for(var property:CONNECTIONS.values())state=state.setValue(property,false);registerDefaultState(state);}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){super.createBlockStateDefinition(builder);CONNECTIONS.values().forEach(builder::add);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){BlockState state=super.getStateForPlacement(context);for(var direction:Direction.values())state=state.setValue(CONNECTIONS.get(direction),MachineConnections.connected(context.getLevel(),context.getClickedPos(),state,direction));return state;}
    @Override protected BlockState updateShape(BlockState state,LevelReader level,ScheduledTickAccess ticks,BlockPos pos,Direction direction,BlockPos neighborPos,BlockState neighbor,RandomSource random){return state.setValue(CONNECTIONS.get(direction),MachineConnections.connected(level,pos,state,direction));}
}
