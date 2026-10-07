package dev.thaumcraft.machine;

import com.mojang.serialization.MapCodec;
import dev.thaumcraft.content.Content;
import dev.thaumcraft.item.ArcanaItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class MachineBlock extends BaseEntityBlock {
    public static final MapCodec<MachineBlock> CODEC=simpleCodec(MachineBlock::new);
    public static final EnumProperty<Direction> FACING=BlockStateProperties.FACING;
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty LIT=BlockStateProperties.LIT;
    // Original metadata variants share one BlockApparatusMetal collision counter.
    private static int crucibleCollisionDelay;

    public MachineBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(LIT,false));
    }
    @Override protected void randomTick(BlockState state,ServerLevel level,BlockPos pos,net.minecraft.util.RandomSource random){
        if(level.getBlockEntity(pos) instanceof MachineBlockEntity machine&&machine.isTotem())MachineLogic.totem(level,machine);
        if(id().equals("arcane_seal")&&level.getBlockEntity(pos) instanceof MachineBlockEntity machine)SealLogic.randomTick(level,machine);
    }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() {return CODEC;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) {builder.add(FACING,LIT);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        if(id().equals("void_interface")&&!context.getLevel().getBlockState(context.getClickedPos().below()).is(Content.block("void_chest")))return null;
        if(id().equals("arcane_bellows")) {
            // BlockApparatusFragile checks south, north, east, west in that order;
            // each match replaces the previous orientation.
            Direction facing=context.getHorizontalDirection().getOpposite();
            for(Direction side:java.util.List.of(Direction.SOUTH,Direction.NORTH,Direction.EAST,Direction.WEST)) {
                var neighbor=context.getLevel().getBlockEntity(context.getClickedPos().relative(side));
                if(neighbor instanceof MachineBlockEntity machine && (machine.machineId().contains("crucible")
                        ||machine.machineId().equals("arcane_furnace")||java.util.Set.of("vis_conduit","vis_pump","vis_storage_tank","thaumium_reinforced_tank").contains(machine.machineId())))facing=side;
            }
            return defaultBlockState().setValue(FACING,facing);
        }
        return defaultBlockState().setValue(FACING,sixDirections()?context.getClickedFace():context.getHorizontalDirection().getOpposite());
    }
    private boolean sixDirections(){return id().equals("arcane_bore")||id().equals("vis_pump")||id().equals("arcane_seal");}
    @Override protected RenderShape getRenderShape(BlockState state) {return RenderShape.INVISIBLE;}
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,net.minecraft.util.RandomSource random){dev.thaumcraft.content.VisualEffects.ambient.accept(level,pos);}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) {return new MachineBlockEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type) {
        return level.isClientSide()?null:createTickerHelper(type,Content.MACHINE_ENTITY,MachineBlockEntity::tick);
    }
    private String id;
    /** Registry path, cached once registered: machine ticks compare it many times per tick. */
    public String id() {
        String cached=id;
        if(cached!=null)return cached;
        var key=BuiltInRegistries.BLOCK.getResourceKey(this);
        if(key.isEmpty())return BuiltInRegistries.BLOCK.getKey(this).getPath();
        return id=key.get().identifier().getPath();
    }

    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,LivingEntity placer,ItemStack stack) {
        super.setPlacedBy(level,pos,state,placer,stack);
        if(level.getBlockEntity(pos) instanceof MachineBlockEntity machine) {
            if(placer instanceof Player player)machine.setOwner(player.getUUID());
            if(machine.machineId().equals("void_interface")&&!level.isClientSide()){
                machine.setChannel(level.getRandom().nextInt(6));
                if(placer instanceof Player player)dev.thaumcraft.content.ModSounds.playAt(level,player.getX(),player.getY(),player.getZ(),"attach",net.minecraft.sounds.SoundSource.PLAYERS,1,1);
            }
        }
    }

    @Override protected InteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
        if(id().equals("totem_of_dawn")||id().equals("totem_of_dusk"))return InteractionResult.PASS;
        if(id().equals("void_chest")&&stack.is(Content.item("void_interface"))
                ||id().contains("crucible")&&stack.is(Content.item("arcane_bellows")))return InteractionResult.PASS;
        ItemStack filledWater=id().equals("everfull_urn")?dev.thaumcraft.api.IntegrationHooks.fillWater(stack):ItemStack.EMPTY;
        if(!filledWater.isEmpty()) {
            if(!level.isClientSide()) {
                player.setItemInHand(hand,ItemUtils.createFilledResult(stack,player,filledWater));
                dev.thaumcraft.gameplay.ArcaneWorldData.get((ServerLevel)level).addVibes((ServerLevel)level,pos,0,1);
                level.playSound(null,pos,net.minecraft.sounds.SoundEvents.BUCKET_FILL,net.minecraft.sounds.SoundSource.BLOCKS,.5f,1);
            }
            return InteractionResult.SUCCESS;
        }
        if(stack.getItem() instanceof ArcanaItem arcana) {
            if(id().equals("arcane_seal")&&arcana.entry().source_class().equals("ItemCrystalBall")){
                if(player instanceof net.minecraft.server.level.ServerPlayer server&&level.getBlockEntity(pos) instanceof MachineBlockEntity machine){
                    dev.thaumcraft.content.ModSounds.playAt(level,player.getX(),player.getY(),player.getZ(),"rune_set",net.minecraft.sounds.SoundSource.PLAYERS,.3f,1);
                    dev.thaumcraft.gameplay.ResearchBook.openCrystalBall(server,stack,hand,SealLogic.runes(machine),true);
                }
                return InteractionResult.SUCCESS;
            }
            if(arcana.entry().source_class().equals("ItemVisDetector")||arcana.entry().source_class().equals("ItemWandReversal")||arcana.entry().source_class().equals("ItemVoidBracelet"))return InteractionResult.PASS;
            if(arcana.entry().source_class().equals("ItemTool")) {
                if((id().equals("arcane_seal")||id().equals("void_interface"))) {
                    if(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)dev.thaumcraft.world.ChunkAnchors.toggleOverlay(serverPlayer,pos);
                    return InteractionResult.SUCCESS;
                }
                if(id().contains("crucible")) {
                    if(level instanceof ServerLevel server&&level.getBlockEntity(pos) instanceof MachineBlockEntity machine&&MachineLogic.ejectCrucible(server,machine,player)) {
                        ArcanaItem.charge(stack,player,hand,1);dev.thaumcraft.content.ModSounds.play(level,pos,"tool",net.minecraft.sounds.SoundSource.BLOCKS,.5f,1);
                    }
                    return InteractionResult.SUCCESS;
                }
                if(!java.util.Set.of("arcane_bore","vis_pump","vis_purifier","arcane_bellows","thaumic_duplicator").contains(id()))return InteractionResult.PASS;
                if(!level.isClientSide()) {
                    level.setBlock(pos,sixDirections()?state.cycle(FACING):state.setValue(FACING,state.getValue(FACING).getAxis().isHorizontal()?state.getValue(FACING).getClockWise():Direction.NORTH),3);
                    ArcanaItem.charge(stack,player,hand,1);
                    dev.thaumcraft.content.ModSounds.play(level,pos,"tool",net.minecraft.sounds.SoundSource.BLOCKS,.5f,1);
                }
                return InteractionResult.SUCCESS;
            }
            if(arcana.entry().source_class().equals("ItemRunicEssence") && level.getBlockEntity(pos) instanceof MachineBlockEntity machine
                    &&(machine.isVoidStorage()||id().equals("arcane_seal"))) {
                if(!level.isClientSide()) {
                    if(machine.isVoidStorage()) {
                        machine.setChannel(arcana.entry().meta());
                        if(machine.machineId().equals("void_chest")&&level.getBlockEntity(pos.above()) instanceof MachineBlockEntity portal&&portal.machineId().equals("void_interface"))portal.setChannel(machine.channel());
                        player.sendOverlayMessage(net.minecraft.network.chat.Component.translatableWithFallback("message.thaumcraft2tp.void.channel", "Void channel: %s", machine.channel()+1));
                    } else if(id().equals("arcane_seal")) {
                        for(int slot=18;slot<21;slot++)if(machine.getItem(slot).isEmpty()) {
                            machine.setItem(slot,stack.copyWithCount(1));SealLogic.runesChanged(machine);
                            dev.thaumcraft.content.ModSounds.play(level,pos,"rune_set",net.minecraft.sounds.SoundSource.BLOCKS,.5f,1.2f-arcana.entry().meta()*.075f);
                            if(!player.isCreative())stack.shrink(1);break;
                        }
                    }
                }
                return InteractionResult.SUCCESS;
            }
            if(arcana.entry().source_class().equals("ItemUpgrades") && level.getBlockEntity(pos) instanceof MachineBlockEntity machine
                    &&MachineBlockEntity.supportsUpgrade(id(),arcana.entry().meta())) {
                if(!level.isClientSide() && machine.installUpgrade(stack)) {
                    stack.shrink(1);
                    dev.thaumcraft.content.ModSounds.play(level,pos,"upgrade",net.minecraft.sounds.SoundSource.BLOCKS,.4f,1);
                }
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit) {
        if(id().equals("totem_of_dawn")||id().equals("totem_of_dusk"))return InteractionResult.PASS;
        if(VisNetwork.isTank(id()))return InteractionResult.PASS;
        if(id().contains("crucible")&&(player.getMainHandItem().is(Content.item("arcane_bellows"))
                ||player.getOffhandItem().is(Content.item("arcane_bellows"))))return InteractionResult.PASS;
        if(player.isShiftKeyDown())return InteractionResult.PASS;
        // Original BlockApparatusStone.blockActivated: right-clicking a portal seal cycles its previewed destination.
        if(id().equals("arcane_seal")&&level.getBlockEntity(pos) instanceof MachineBlockEntity seal){
            if(!(level instanceof ServerLevel server))return seal.visualRune(0)==0&&seal.visualRune(1)==1?InteractionResult.SUCCESS:InteractionResult.PASS;
            return dev.thaumcraft.world.SealPortals.cycle(server,seal)?InteractionResult.SUCCESS:InteractionResult.PASS;
        }
        if(level.getBlockEntity(pos) instanceof MachineBlockEntity machine) {
            boolean valve=id().equals("advanced_vis_valve")||id().equals("vis_valve");
            boolean menu=MachineLayout.ALL.containsKey(id())||machine.isVoidStorage()||id().equals("traveling_trunk");
            if(!valve&&!menu)return InteractionResult.PASS;
            if(!level.isClientSide()) {
                boolean opened=false;
                if(id().equals("advanced_vis_valve"))machine.setChannel((machine.channel()+1)%3);
                else if(id().equals("vis_valve"))opened=machine.toggle();
                else player.openMenu(machine);
                if(valve)level.playSound(null,pos,net.minecraft.sounds.SoundEvents.LEVER_CLICK,net.minecraft.sounds.SoundSource.BLOCKS,.3f,opened?.6f:.5f);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) {
        String id=id();
        if(id.contains("crucible"))return Shapes.block();
        if(id.equals("arcane_seal"))return switch(state.getValue(FACING)) {
            case UP -> Block.box(4.8,0,4.8,11.2,1,11.2);case DOWN -> Block.box(4.8,15,4.8,11.2,16,11.2);
            case NORTH -> Block.box(4.8,4.8,15,11.2,11.2,16);case SOUTH -> Block.box(4.8,4.8,0,11.2,11.2,1);
            case EAST -> Block.box(0,4.8,4.8,1,11.2,11.2);case WEST -> Block.box(15,4.8,4.8,16,11.2,11.2);
        };
        if(java.util.Set.of("vis_conduit","vis_valve","advanced_vis_valve","glowing_nitor").contains(id))return Block.box(4,4,4,12,12,12);
        if(java.util.Set.of("vis_filter","arcane_bellows","brain_in_a_jar").contains(id))return Block.box(2,0,2,14,16,14);
        if(java.util.Set.of("vis_storage_tank","thaumium_reinforced_tank").contains(id))return Block.box(1,0,1,15,16,15);
        if(java.util.Set.of("thaumic_generator","arcane_bore").contains(id))return Block.box(2,2,2,14,14,14);
        if(id.equals("brazier_of_souls"))return Block.box(4,0,4,12,12,12);
        if(id.equals("void_interface")||id.equals("darkness_generator"))return Block.box(0,0,0,16,7,16);
        if(id.equals("thaumic_enchanter"))return Block.box(0,0,0,16,12,16);
        if(id.equals("thaumic_infuser")||id.equals("dark_infuser"))return Block.box(0,0,0,16,15,16);
        if(id.equals("quaesitum"))return Block.box(0,6,0,16,10,16);
        if(id.equals("everfull_urn"))return Block.box(2,0,2,14,9,14);
        if(id.equals("vis_condenser"))return Block.box(3,0,3,13,16,13);
        return super.getShape(state,level,pos,context);
    }

    @Override protected VoxelShape getBlockSupportShape(BlockState state,BlockGetter level,BlockPos pos) {
        return id().equals("arcane_bore")?Shapes.block():super.getBlockSupportShape(state,level,pos);
    }

    @Override protected VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) {
        if(id().contains("crucible")&&!id().equals("crucible_of_souls"))return Shapes.or(Block.box(0,0,0,16,5,16),Block.box(0,0,0,2,16,16),Block.box(0,0,0,16,16,2),Block.box(14,0,0,16,16,16),Block.box(0,0,14,16,16,16));
        if(id().equals("glowing_nitor"))return Shapes.empty();
        if(id().equals("darkness_generator"))return Shapes.block();
        if(id().equals("quaesitum"))return Shapes.or(getShape(state,level,pos,context),Block.box(3.2,0,3.2,12.8,6,12.8));
        if(id().equals("everfull_urn"))return Shapes.or(getShape(state,level,pos,context),Block.box(5,9,5,11,16,11));
        return getShape(state,level,pos,context);
    }
    @Override protected void entityInside(BlockState state,Level level,BlockPos pos,net.minecraft.world.entity.Entity entity,net.minecraft.world.entity.InsideBlockEffectApplier effects,boolean precise) {
        if(!id().contains("crucible")||id().equals("crucible_of_souls"))return;
        if(entity instanceof net.minecraft.world.entity.item.ItemEntity item&&entity.getY()<=pos.getY()+.7) {
            var random=level.getRandom();item.setDeltaMovement(item.getDeltaMovement().add((random.nextFloat()-random.nextFloat())*.05,random.nextFloat()*.1,(random.nextFloat()-random.nextFloat())*.05));item.setPickUpDelay(10);((dev.thaumcraft.mixin.ItemEntityAccess)item).thaumcraft$setAge(0);
        }
        if(!(level instanceof ServerLevel server))return;
        if(++crucibleCollisionDelay>=5){
            crucibleCollisionDelay=0;
            if(entity instanceof LivingEntity living&&!net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(living.getType()).getPath().equals("thaum_slime")){
                living.hurtServer(server,level.damageSources().magic(),1);
                level.playSound(null,pos.getX(),pos.getY(),pos.getZ(),net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH,net.minecraft.sounds.SoundSource.BLOCKS,.4f,2+level.getRandom().nextFloat()*.4f);
            }
        }
    }
    @Override protected boolean canSurvive(BlockState state,net.minecraft.world.level.LevelReader level,BlockPos pos) {
        if(id().equals("arcane_seal")){Direction face=state.getValue(FACING);BlockPos support=pos.relative(face.getOpposite());return level.getBlockState(support).isFaceSturdy(level,support,face);}
        if(id().equals("vis_condenser"))return level.isEmptyBlock(pos.above());
        return super.canSurvive(state,level,pos);
    }
    @Override protected BlockState updateShape(BlockState state,net.minecraft.world.level.LevelReader level,net.minecraft.world.level.ScheduledTickAccess ticks,BlockPos pos,Direction direction,BlockPos neighbor,BlockState neighborState,net.minecraft.util.RandomSource random) {
        if(!canSurvive(state,level,pos))ticks.scheduleTick(pos,this,1);
        return super.updateShape(state,level,ticks,pos,direction,neighbor,neighborState,random);
    }
    @Override protected void tick(BlockState state,ServerLevel level,BlockPos pos,net.minecraft.util.RandomSource random) {
        if(!canSurvive(state,level,pos))level.destroyBlock(pos,!id().equals("arcane_seal"));
    }

    @Override protected boolean isSignalSource(BlockState state) {return id().equals("arcane_seal")||id().equals("crucible_of_eyes")||id().equals("thaumium_crucible");}
    @Override protected int getSignal(BlockState state,BlockGetter level,BlockPos pos,Direction direction) {
        if(id().equals("crucible_of_eyes")||id().equals("thaumium_crucible"))return level.getBlockEntity(pos) instanceof MachineBlockEntity machine&&machine.totalVis()>=machine.capacity()*.9f?15:0;
        return id().equals("arcane_seal")&&level.getBlockEntity(pos) instanceof MachineBlockEntity machine && machine.energy()>0 ? 15:0;
    }
    @Override protected int getDirectSignal(BlockState state,BlockGetter level,BlockPos pos,Direction direction) {
        return id().equals("arcane_seal")?getSignal(state,level,pos,direction):super.getDirectSignal(state,level,pos,direction);
    }
    @Override protected boolean hasAnalogOutputSignal(BlockState state) {return true;}
    @Override protected int getAnalogOutputSignal(BlockState state,Level level,BlockPos pos,Direction direction) {
        return level.getBlockEntity(pos) instanceof MachineBlockEntity machine ? machine.comparatorSignal():0;
    }
    @Override protected void affectNeighborsAfterRemoval(BlockState state,ServerLevel level,BlockPos pos,boolean movedByPiston) {
        level.updateNeighbourForOutputSignal(pos,this);
    }
}
