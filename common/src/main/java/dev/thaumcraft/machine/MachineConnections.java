package dev.thaumcraft.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

/** Per-side attachment rules from the original IConnection implementations, plus addon vis containers. */
public final class MachineConnections {
    private MachineConnections() {}
    /** Original goggles reveal IConnection tiles even when their vis buffer is empty. */
    public static boolean revealsVis(BlockGetter level,BlockPos pos,BlockState state) {
        for(Direction side:Direction.values())if(accepts(level,pos,state,side))return true;
        return false;
    }
    public static boolean accepts(BlockGetter level,BlockPos pos,BlockState state,Direction side) {
        if(!(state.getBlock() instanceof MachineBlock block)){
            if(!state.hasBlockEntity())return false;
            var addon=VisNetwork.addon(level,pos);return addon!=null&&addon.connects(side);
        }
        Direction facing=state.getValue(MachineBlock.FACING);
        return switch(block.id()) {
            case "vis_conduit","vis_valve","advanced_vis_valve","vis_storage_tank","thaumium_reinforced_tank","thaumic_generator","thaumic_infuser","dark_infuser","thaumic_restorer" -> true;
            case "arcane_furnace","vis_condenser","crucible","crucible_of_eyes","thaumium_crucible","crucible_of_souls","thaumic_crystalizer","thaumic_enchanter","occultic_enchanter" -> side!=Direction.UP;
            case "thaumic_duplicator" -> side.getAxis().isHorizontal();
            case "arcane_bellows" -> side==facing;
            case "vis_pump","vis_purifier" -> side.getAxis()==facing.getAxis();
            case "vis_filter" -> side!=Direction.UP&&!level.getBlockState(pos.below()).is(state.getBlock());
            default -> false;
        };
    }
    public static boolean connected(BlockGetter level,BlockPos pos,BlockState state,Direction side) {
        return accepts(level,pos,state,side)&&accepts(level,pos.relative(side),level.getBlockState(pos.relative(side)),side.getOpposite());
    }
}
