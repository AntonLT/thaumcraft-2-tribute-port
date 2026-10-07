package dev.thaumcraft.content;

import dev.thaumcraft.gameplay.ArcaneWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class CrystalBlock extends Block implements net.minecraft.world.level.block.EntityBlock {
    public static final net.minecraft.world.level.block.state.properties.IntegerProperty AMOUNT=net.minecraft.world.level.block.state.properties.IntegerProperty.create("amount",1,5);
    public static final net.minecraft.world.level.block.state.properties.EnumProperty<net.minecraft.core.Direction> FACING=net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING;
    private static final String[] CRYSTALS={"vis_crystal","vaporous_crystal","aqueous_crystal","earthen_crystal","fiery_crystal","tainted_crystal"};
    public CrystalBlock(Properties properties) {super(properties);registerDefaultState(stateDefinition.any().setValue(AMOUNT,2).setValue(FACING,net.minecraft.core.Direction.UP));}
    @Override public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new dev.thaumcraft.world.VisualBlockEntity(pos,state);}
    @Override protected net.minecraft.world.level.block.RenderShape getRenderShape(BlockState state){return net.minecraft.world.level.block.RenderShape.INVISIBLE;}
    @Override protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block,BlockState> builder){builder.add(AMOUNT,FACING);}
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {return defaultBlockState().setValue(AMOUNT,1).setValue(FACING,context.getClickedFace());}
    @Override protected boolean canSurvive(BlockState state,net.minecraft.world.level.LevelReader level,BlockPos pos){var face=state.getValue(FACING);return level.getBlockState(pos.relative(face.getOpposite())).isFaceSturdy(level,pos.relative(face.getOpposite()),face);}
    @Override protected BlockState updateShape(BlockState state,net.minecraft.world.level.LevelReader level,net.minecraft.world.level.ScheduledTickAccess ticks,BlockPos pos,net.minecraft.core.Direction direction,BlockPos neighborPos,BlockState neighbor,RandomSource random) {
        if(!state.canSurvive(level,pos))ticks.scheduleTick(pos,this,1);
        return super.updateShape(state,level,ticks,pos,direction,neighborPos,neighbor,random);
    }
    @Override protected void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){if(!state.canSurvive(level,pos))level.destroyBlock(pos,true);}
    @Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state,net.minecraft.world.level.BlockGetter level,BlockPos pos,net.minecraft.world.phys.shapes.CollisionContext context){return switch(state.getValue(FACING)){
        case DOWN->Block.box(4,4,4,12,16,12);
        case UP->Block.box(4,0,4,12,12,12);
        case NORTH->Block.box(4,4,4,12,12,16);
        case SOUTH->Block.box(4,4,0,12,12,12);
        case WEST->Block.box(4,4,4,16,12,12);
        case EAST->Block.box(0,4,4,12,12,12);
    };}
    @Override protected void affectNeighborsAfterRemoval(BlockState state,ServerLevel level,BlockPos pos,boolean moving){
        super.affectNeighborsAfterRemoval(state,level,pos,moving);
        boolean dark=crystal().is(Content.item("tainted_crystal"));int vibes=state.getValue(AMOUNT)*5;
        ArcaneWorldData.get(level).addVibes(level,pos,dark?vibes:0,dark?0:vibes);
    }
    public ItemStack crystal() {
        Content.Entry def=Content.DEFINITIONS.get(BuiltInRegistries.BLOCK.getKey(this).getPath());
        return new ItemStack(Content.item(CRYSTALS[def.meta()]));
    }
    @Override protected void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random) {
        int amount=state.getValue(AMOUNT);
        var data=ArcaneWorldData.get(level);var aura=data.aura(level,pos);
        boolean dark=crystal().is(Content.item("tainted_crystal"));
        boolean nourished=dark?aura.badVibes()>0:aura.goodVibes()>0;
        if((amount<5&&nourished&&random.nextInt(amount*75)==0)||(amount<3&&random.nextInt(amount*150)==0))
            level.setBlockAndUpdate(pos,state.setValue(AMOUNT,amount+1));
        if((dark?aura.taint():aura.vis())<dev.thaumcraft.PortConfig.auraMax/10){
            int vibes=random.nextInt(amount);
            data.addVibes(level,pos,dark?0:vibes,dark?vibes:0);
        }
    }
}
