package dev.thaumcraft.item;

import dev.thaumcraft.content.Content;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;


public final class ElementalTools {
    private static final ThreadLocal<Boolean> BREAKING=ThreadLocal.withInitial(()->false);
    private ElementalTools() {}
    public static boolean specialEnabled(Player player){return player.isShiftKeyDown()==dev.thaumcraft.PortConfig.toolShift;}
    public static void mined(ItemStack stack,ServerLevel level,BlockState original,BlockPos origin,ServerPlayer player,String type) {
        if(!dev.thaumcraft.PortConfig.areaMining||BREAKING.get()||!specialEnabled(player))return;
        if(!type.equals("ItemElementalCrusher")&&!type.equals("ItemElementalCutter")&&!type.equals("ItemElementalAxeWater")&&!type.equals("ItemElementalShovelEarth")&&!type.equals("ItemElementalHoeMagic"))return;
        boolean harvest=type.equals("ItemElementalHoeMagic");
        boolean tree=type.equals("ItemElementalCutter")||type.equals("ItemElementalAxeWater");
        boolean shovel=type.equals("ItemElementalShovelEarth");
        if(tree&&!wood(original))return;
        if(harvest&&!harvestable(original))return;
        if(!tree&&!harvest&&!original.is(BlockTags.MINEABLE_WITH_PICKAXE)&&!original.is(BlockTags.MINEABLE_WITH_SHOVEL))return;
        if(tree||harvest){
            ConnectedHarvest.start(level,player,stack,InteractionHand.MAIN_HAND,origin,original,false);return;
        }
        BREAKING.set(true);
        try {
            boolean boom=false;
            float hardness=original.getDestroySpeed(level,origin);
            for(BlockPos pos:BlockPos.betweenClosed(origin.offset(-1,shovel?0:-1,-1),origin.offset(1,shovel?0:1,1))) {
                if(stack.isEmpty())break;
                if(pos.equals(origin)||!level.hasChunkAt(pos)||level.getBlockEntity(pos)!=null)continue;
                BlockState state=level.getBlockState(pos);
                if(!state.equals(original)||state.getBlock() instanceof dev.thaumcraft.content.TaintBlock)continue;
                if(!shovel&&state.getBlock().getExplosionResistance()>=10)continue;
                if(shovel&&!original.is(BlockTags.MINEABLE_WITH_SHOVEL))continue;
                float speed=state.getDestroySpeed(level,pos);
                if(speed<0||speed>Math.max(3,hardness*2)||(!state.is(BlockTags.MINEABLE_WITH_PICKAXE)&&!state.is(BlockTags.MINEABLE_WITH_SHOVEL)))continue;
                if((!state.requiresCorrectToolForDrops()||stack.isCorrectToolForDrops(state))&&player.gameMode.destroyBlock(pos)){
                    dev.thaumcraft.network.EquipmentEffect.send(level,dev.thaumcraft.network.EquipmentEffect.POOF,Vec3.atLowerCornerOf(pos));boom=true;
                }
            }
            if(boom&&!shovel)level.playSound(null,origin,net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(),net.minecraft.sounds.SoundSource.BLOCKS,.2f,1+level.getRandom().nextFloat()*.2f);
        } finally {BREAKING.remove();}
    }
    private static boolean wood(BlockState state){return state.getSoundType()==net.minecraft.world.level.block.SoundType.WOOD||state.is(BlockTags.LOGS)||state.is(BlockTags.PLANKS);}
    private static boolean harvestable(BlockState state) {
        return state.getBlock() instanceof net.minecraft.world.level.block.CropBlock crop&&crop.isMaxAge(state)
            ||state.is(net.minecraft.world.level.block.Blocks.PUMPKIN)||state.is(net.minecraft.world.level.block.Blocks.MELON);
    }
    public static void tick(ServerLevel level){ConnectedHarvest.tick(level);}
    static boolean breakConnected(ServerLevel level,ServerPlayer player,BlockPos pos){
        BREAKING.set(true);
        try{return player.gameMode.destroyBlock(pos);}finally{BREAKING.remove();}
    }
    public static boolean till(ServerLevel level,Player player,net.minecraft.world.item.context.UseOnContext context) {
        BlockPos pos=context.getClickedPos();
        if(!(player instanceof ServerPlayer serverPlayer)||context.getClickedFace()==Direction.DOWN||!level.mayInteract(player,pos)||!ConnectedHarvest.tillable(level,pos))return false;
        if(!dev.thaumcraft.PortConfig.areaMining){
            if(!ConnectedHarvest.tillBlock(level,pos))return false;
            ArcanaItem.charge(context.getItemInHand(),player,context.getHand(),1);
        }else ConnectedHarvest.start(level,serverPlayer,context.getItemInHand(),context.getHand(),pos,level.getBlockState(pos),true);
        return true;
    }
    public static void vacuum(ServerLevel level,Player player,ItemStack stack) {
        Vec3 point=player.getEyePosition().add(player.getLookAngle().scale(2));boolean pulled=false;
        for(var entity:level.getEntities(player,player.getBoundingBox().inflate(10),e->!(e instanceof Player)&&!(e instanceof dev.thaumcraft.entity.TravelingTrunk)&&!(e instanceof dev.thaumcraft.entity.CarpetEntity))) {
            Vec3 delta=entity.position().subtract(player.getEyePosition());
            Vec3 destination=entity instanceof net.minecraft.world.entity.item.ItemEntity?player.getEyePosition().add(player.getLookAngle()):point;
            Vec3 velocity=entity.getDeltaMovement().add(destination.subtract(entity.position()).normalize().scale(.3));
            entity.setDeltaMovement(Math.clamp(velocity.x,-.3,.3),Math.clamp(velocity.y,-.3,.3),Math.clamp(velocity.z,-.3,.3));entity.hurtMarked=true;pulled=true;
            dev.thaumcraft.network.EquipmentEffect.send(level,dev.thaumcraft.network.EquipmentEffect.WISP,entity.position().add(0,.1+entity.getBbHeight()/2,0));
        }
        if(pulled&&level.getRandom().nextBoolean())ArcanaItem.charge(stack,player,player.getUsedItemHand(),1);
    }
    public static boolean grow(ServerLevel level,BlockPos pos,BlockState state){
        var block=state.getBlock();
        if(state.is(net.minecraft.world.level.block.Blocks.WHEAT)&&block instanceof net.minecraft.world.level.block.CropBlock crop){
            level.setBlockAndUpdate(pos,crop.getStateForAge(crop.getMaxAge()));return true;
        }
        if(block instanceof net.minecraft.world.level.block.StemBlock){
            level.setBlockAndUpdate(pos,state.setValue(net.minecraft.world.level.block.StemBlock.AGE,7));return true;
        }
        if(block instanceof net.minecraft.world.level.block.SaplingBlock sapling){
            sapling.advanceTree(level,pos,state.setValue(net.minecraft.world.level.block.SaplingBlock.STAGE,1),level.getRandom());return true;
        }
        if(block instanceof net.minecraft.world.level.block.MushroomBlock mushroom)return mushroom.growMushroom(level,pos,state,level.getRandom());
        if(block instanceof net.minecraft.world.level.block.BonemealableBlock plant&&plant.isValidBonemealTarget(level,pos,state)&&plant.isBonemealSuccess(level,level.getRandom(),pos,state)){
            plant.performBonemeal(level,level.getRandom(),pos,state);return true;
        }
        return false;
    }
    public static void prospect(ServerLevel level,Player player,ItemStack stack,InteractionHand hand) {
        BlockPos target=prospectTarget(level,player.blockPosition());
        if(target!=null) {
            dev.thaumcraft.entity.ArcaneMote.guide(level,player,target.getCenter());
        }
        level.playSound(null,player.blockPosition(),net.minecraft.sounds.SoundEvents.FIRECHARGE_USE,net.minecraft.sounds.SoundSource.PLAYERS,1,level.getRandom().nextFloat()*.4f+.8f);
        var entry=Content.entry(stack);ArcanaItem.charge(stack,player,hand,entry!=null&&entry.source_class().equals("ItemElementalPickFire")?8:10);
    }
    public static BlockPos prospectTarget(ServerLevel level,BlockPos origin) {
        BlockPos result=null;float greatest=1;double distance=Double.MAX_VALUE;
        for(BlockPos pos:BlockPos.betweenClosed(origin.offset(-8,-8,-8),origin.offset(8,8,8))) {
            if(!level.hasChunkAt(pos))continue;
            var block=level.getBlockState(pos).getBlock();float value=dev.thaumcraft.gameplay.GameData.vis(new ItemStack(block));
            if(value==0&&block instanceof dev.thaumcraft.content.CrystalBlock)value=50;
            double candidate=pos.distSqr(origin);
            if(value>greatest||value==greatest&&value>1&&candidate<distance){result=pos.immutable();greatest=value;distance=candidate;}
        }
        return result;
    }
}
