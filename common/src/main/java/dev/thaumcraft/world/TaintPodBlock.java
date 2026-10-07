package dev.thaumcraft.world;

import dev.thaumcraft.content.Content;
import dev.thaumcraft.content.TaintBlock;
import dev.thaumcraft.gameplay.ArcaneWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A repeating 1,000-tick growth / 20-tick spore burst cycle from TileTaintSeed. */
public final class TaintPodBlock extends Block implements net.minecraft.world.level.block.EntityBlock {
    public static final IntegerProperty AGE=IntegerProperty.create("age",0,20);
    public static final BooleanProperty BURSTING=BooleanProperty.create("bursting");
    public TaintPodBlock(Properties properties){super(properties);registerDefaultState(stateDefinition.any().setValue(AGE,0).setValue(BURSTING,false));}
    @Override public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new VisualBlockEntity(pos,state);}
    @Override protected net.minecraft.world.level.block.RenderShape getRenderShape(BlockState state){return net.minecraft.world.level.block.RenderShape.INVISIBLE;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(AGE,BURSTING);}
    @Override protected java.util.List<net.minecraft.world.item.ItemStack> getDrops(BlockState state,net.minecraft.world.level.storage.loot.LootParams.Builder params){
        var tool=params.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.TOOL);
        int fortune=tool==null?0:net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(params.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.FORTUNE),tool);
        return params.getLevel().getRandom().nextInt(15)<=fortune?java.util.List.of(new net.minecraft.world.item.ItemStack(Content.item("taint_spores"))):java.util.List.of();
    }
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return Block.box(2,0,2,14,6+state.getValue(AGE)*0.4,14);}
    @Override protected boolean canSurvive(BlockState state,LevelReader level,BlockPos pos){return level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),Direction.UP);}
    @Override protected BlockState updateShape(BlockState state,LevelReader level,ScheduledTickAccess ticks,BlockPos pos,Direction direction,BlockPos neighbor,BlockState neighborState,RandomSource random) {
        return canSurvive(state,level,pos)?super.updateShape(state,level,ticks,pos,direction,neighbor,neighborState,random):Blocks.AIR.defaultBlockState();
    }
    @Override protected void onPlace(BlockState state,Level level,BlockPos pos,BlockState oldState,boolean moving){if(!oldState.is(this))level.scheduleTick(pos,this,50);}
    @Override protected void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random) {
        var aura=ArcaneWorldData.get(level);var cell=aura.aura(level,pos);
        if(!canSurvive(state,level,pos)||!state.getValue(BURSTING)&&TaintBlock.shouldHeal(cell,random)&&random.nextInt(3)==0){level.removeBlock(pos,false);return;}
        int age=state.getValue(AGE);
        if(state.getValue(BURSTING)&&age>0) {
            for(var entity:level.getEntitiesOfClass(LivingEntity.class,new AABB(pos).inflate(1))) {
                entity.addEffect(new MobEffectInstance(MobEffects.POISON,120,1));
                entity.addEffect(new MobEffectInstance(MobEffects.NAUSEA,120,1));
            }
            if(dev.thaumcraft.PortConfig.taintSpread) {
                if(cell.goodVibes()>0)aura.addVibes(level,pos,-1,0);
                else if(random.nextInt(3)==0)aura.addVibes(level,pos,0,1);
            }
            state=state.setValue(AGE,age-1).setValue(BURSTING,age>1);
        } else if(age<20)state=state.setValue(AGE,age+1);
        if(state.getValue(AGE)==20&&!state.getValue(BURSTING))state=startBurst(state,level,pos,false);
        if(state!=level.getBlockState(pos))level.setBlock(pos,state,3);
        level.scheduleTick(pos,this,state.getValue(BURSTING)?1:50);
    }
    private BlockState startBurst(BlockState state,ServerLevel level,BlockPos pos,boolean disturbed) {
        level.playSound(null,pos,dev.thaumcraft.content.ModSounds.event("podburst"),SoundSource.BLOCKS,disturbed?.25f:.35f,disturbed?1.2f:.9f);
        return state.setValue(BURSTING,true);
    }
    private void disturb(BlockState state,Level level,BlockPos pos) {
        if(level instanceof ServerLevel server&&state.getValue(AGE)>0&&!state.getValue(BURSTING)) {
            level.setBlock(pos,startBurst(state,server,pos,true),3);level.scheduleTick(pos,this,1);
        }
    }
    @Override protected void entityInside(BlockState state,Level level,BlockPos pos,Entity entity,InsideBlockEffectApplier effects,boolean precise){if(entity instanceof LivingEntity)disturb(state,level,pos);}
    @Override protected void attack(BlockState state,Level level,BlockPos pos,Player player){disturb(state,level,pos);}
    public static boolean tryGrow(ServerLevel level,BlockPos ground,float taint,RandomSource random) {
        return dev.thaumcraft.PortConfig.taintSpread&&tryGrow((net.minecraft.world.level.WorldGenLevel)level,ground,taint,random);
    }
    public static boolean tryGrow(net.minecraft.world.level.WorldGenLevel level,BlockPos ground,float taint,RandomSource random) {
        if(!TaintBlock.canAccess(level,ground.above())||taint<=dev.thaumcraft.PortConfig.auraMax*.66f||random.nextInt(50)!=0||!(level.getBlockState(ground).getBlock() instanceof TaintBlock)||!level.getBlockState(ground.above()).isAir())return false;
        var pod=Content.block("taint_spore_pod");
        for(BlockPos nearby:BlockPos.betweenClosed(ground.offset(-10,-10,-10),ground.offset(10,10,10)))
            if(TaintBlock.canAccess(level,nearby)&&level.getBlockState(nearby).is(pod))return false;
        return level.setBlock(ground.above(),pod.defaultBlockState(),level instanceof ServerLevel?3:2);
    }
}
