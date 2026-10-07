package dev.thaumcraft.world;

import dev.thaumcraft.content.Content;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

public final class EldritchBlock extends Block implements net.minecraft.world.level.block.EntityBlock {
    public static final EnumProperty<Direction> FACING=BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty PROGRESS=IntegerProperty.create("runes",0,4);
    public EldritchBlock(Properties properties) {super(properties);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(PROGRESS,0));}
    @Override public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos,BlockState state){return id().equals("eldritch_core")?new MonolithBlockEntity(pos,state):new VisualBlockEntity(pos,state);}
    @Override protected net.minecraft.world.level.block.RenderShape getRenderShape(BlockState state){return net.minecraft.world.level.block.RenderShape.INVISIBLE;}
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,net.minecraft.util.RandomSource random){dev.thaumcraft.content.VisualEffects.ambient.accept(level,pos);}
    @Override protected void randomTick(BlockState state,ServerLevel level,BlockPos pos,net.minecraft.util.RandomSource random){if(id().equals("eldritch_monolith"))dev.thaumcraft.gameplay.ArcaneWorldData.get(level).addVibes(level,pos,0,1);}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) {builder.add(FACING,PROGRESS);}
    @Override protected void onPlace(BlockState state,Level level,BlockPos pos,BlockState oldState,boolean moving){
        if(!oldState.is(this)&&(id().equals("eldritch_monolith")||id().equals("eldritch_core")))level.scheduleTick(pos,this,1);
    }
    @Override protected void tick(BlockState state,ServerLevel level,BlockPos pos,net.minecraft.util.RandomSource random){
        if(id().equals("eldritch_core")&&level.getBlockEntity(pos) instanceof MonolithBlockEntity core)core.migrate();
        if(id().equals("eldritch_monolith")){
            if(!(level.getBlockState(pos.below()).getBlock() instanceof EldritchBlock))
                dev.thaumcraft.content.ModSounds.play(level,pos,"monolith",net.minecraft.sounds.SoundSource.BLOCKS,.4f,1);
            level.scheduleTick(pos,this,450+random.nextInt(150));
        }
    }
    private String id() {return BuiltInRegistries.BLOCK.getKey(this).getPath();}
    public static int rune(BlockPos pos,int step) {return new java.util.Random(pos.asLong()^(step*104729L)).nextInt(6);}
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit) {
        if(!level.isClientSide()) {
            if(id().equals("eldritch_core")) {
                if(level.getBlockEntity(pos) instanceof MonolithBlockEntity core){
                    if(!core.migrate())player.sendOverlayMessage(Component.translatableWithFallback("message.thaumcraft2tp.monolith.obstructed", "The monolith receptacles are obstructed."));
                    else core.tryOpen();
                }
            } else if(id().equals("eldritch_lock"))player.sendOverlayMessage(Component.translatableWithFallback("message.thaumcraft2tp.lock.keystone", "This lock requires an Eldritch Keystone: Tlhutlh."));
            else return InteractionResult.PASS;
        }
        return InteractionResult.SUCCESS;
    }
    @Override protected InteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
        var entry=Content.entry(stack);
        if(entry==null)return InteractionResult.TRY_WITH_EMPTY_HAND;
        if(id().equals("eldritch_receptacle")&&entry.source_class().equals("ItemCrystals")&&entry.meta()>=0&&entry.meta()<6) {
            if(level.isClientSide())return InteractionResult.SUCCESS;
            var core=MonolithBlockEntity.coreAt(level,pos);
            return core==null?InteractionResult.FAIL:core.insert(pos,stack,player);
        }
        if(id().equals("eldritch_lock")&&entry.source_class().equals("ItemEldritchKeystone")&&entry.meta()>0) {
            if(level instanceof ServerLevel server) {
                if(!EldritchStructures.unlockRoom(server,pos,state.getValue(FACING)))return InteractionResult.FAIL;
                stack.consume(1,player);dev.thaumcraft.content.ModSounds.play(server,pos,"place",net.minecraft.sounds.SoundSource.BLOCKS,.5f,1);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }
}
