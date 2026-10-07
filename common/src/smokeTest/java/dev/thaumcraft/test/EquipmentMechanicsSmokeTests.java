package dev.thaumcraft.test;

import dev.thaumcraft.content.Content;
import dev.thaumcraft.item.ArcanaItem;
import dev.thaumcraft.item.ElementalTools;
import dev.thaumcraft.item.ItemState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Behavior checks against the recovered equipment mechanics, not attack balance. */
final class EquipmentMechanicsSmokeTests {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError("Equipment mechanics: "+message);}
    private static ItemStack equip(ServerPlayer player,String id){var stack=new ItemStack(Content.item(id));player.setItemInHand(InteractionHand.MAIN_HAND,stack);return stack;}
    private static UseOnContext context(ServerPlayer player,BlockPos pos){return new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(pos.getCenter(),Direction.UP,pos,false));}
    private static void ticks(ServerLevel level,int count){for(int i=0;i<count;i++)ElementalTools.tick(level);}
    private static int blocks(ServerLevel level,BlockPos from,BlockPos to,net.minecraft.world.level.block.Block block){int count=0;for(var pos:BlockPos.betweenClosed(from,to))if(level.getBlockState(pos).is(block))count++;return count;}
    private static float exhaustion(ServerPlayer player){
        var out=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,player.registryAccess());
        player.getFoodData().addAdditionalSaveData(out);return out.buildResult().getFloatOr("foodExhaustionLevel",0);
    }
    static int run(ServerLevel level,ServerPlayer player){
        checks=0;
        BlockPos origin=new BlockPos(240,296,240);ArcanaParitySmokeTests.prepareEntities(level,origin);player.setPos(origin.getBottomCenter());
        boolean area=dev.thaumcraft.PortConfig.areaMining,shift=dev.thaumcraft.PortConfig.toolShift;
        dev.thaumcraft.PortConfig.areaMining=true;dev.thaumcraft.PortConfig.toolShift=false;player.setShiftKeyDown(false);
        var axe=new ItemStack(Content.item("axe_of_the_stream"));player.setItemInHand(InteractionHand.MAIN_HAND,axe);
        var drop=new ItemEntity(level,player.getX()+3,player.getY(),player.getZ(),new ItemStack(Items.STICK));drop.setDeltaMovement(Vec3.ZERO);level.addFreshEntity(drop);
        axe.getItem().use(level,player,InteractionHand.MAIN_HAND);
        check(Math.abs(drop.getDeltaMovement().length()-.3)<1e-6,"Stream axe adds a 0.3 impulse to stationary items");
        Vec3 previous=new Vec3(.2,.1,.25);drop.setDeltaMovement(previous);
        Vec3 expected=previous.add(player.getEyePosition().add(0,-player.getBbHeight()/2,0).subtract(drop.position()).normalize().scale(.3));
        axe.getItem().use(level,player,InteractionHand.MAIN_HAND);
        check(drop.getDeltaMovement().distanceTo(new Vec3(Math.clamp(expected.x,-.35,.35),Math.clamp(expected.y,-.35,.35),Math.clamp(expected.z,-.35,.35)))<1e-6,"Attraction preserves existing sideways momentum and clamps each component");drop.discard();
        for(String id:new String[]{"void_crusher","elemental_crusher"}){
            var crusher=new ItemStack(Content.item(id));
            check(crusher.getDestroySpeed(Blocks.DIRT.defaultBlockState())==8,id+" digs dirt at original speed");
            check(crusher.getDestroySpeed(Blocks.SAND.defaultBlockState())==8&&crusher.getDestroySpeed(Blocks.GRAVEL.defaultBlockState())==8,id+" digs sand and gravel efficiently");
            check(crusher.isCorrectToolForDrops(Blocks.SNOW.defaultBlockState()),id+" harvests snow as well as mining it");
        }
        for(String id:new String[]{"void_cutter","elemental_cutter"}){
            var cutter=new ItemStack(Content.item(id));
            check(cutter.getDestroySpeed(Blocks.COBWEB.defaultBlockState())==15&&cutter.isCorrectToolForDrops(Blocks.COBWEB.defaultBlockState()),id+" cuts cobwebs quickly and keeps their drops");
            check(cutter.has(net.minecraft.core.component.DataComponents.BLOCKS_ATTACKS),id+" blocks attacks while its use action is held");
        }
        fire();
        check(new ItemStack(Content.item("pickaxe_of_the_core")).getDestroySpeed(Blocks.STONE.defaultBlockState())==8,"Core pick retains its extra mining speed");
        check(new ItemStack(Content.item("thaumium_pickaxe")).getDestroySpeed(Blocks.STONE.defaultBlockState())==7,"Ordinary Thaumium mining speed is unchanged");
        connected(level,player,origin);
        growth(level,player,origin);
        targeting(level,player,origin);
        boots(level,player);
        dev.thaumcraft.PortConfig.areaMining=area;dev.thaumcraft.PortConfig.toolShift=shift;player.setShiftKeyDown(false);
        player.setItemSlot(EquipmentSlot.FEET,ItemStack.EMPTY);player.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);player.setDeltaMovement(Vec3.ZERO);
        return checks;
    }
    private static void connected(ServerLevel level,ServerPlayer player,BlockPos origin){
        BlockPos from=origin.offset(-6,0,-6),to=from.offset(11,0,13);
        var logs=Blocks.OAK_LOG.defaultBlockState();var axe=equip(player,"axe_of_the_stream");
        for(var pos:BlockPos.betweenClosed(from,to))level.setBlockAndUpdate(pos,logs);
        level.setBlockAndUpdate(from,Blocks.AIR.defaultBlockState());ElementalTools.mined(axe,level,logs,from,player,"ItemElementalAxeWater");
        check(blocks(level,from,to,Blocks.OAK_LOG)==167,"Connected chopping starts a continuing operation without instant deletion");
        ticks(level,1);check(blocks(level,from,to,Blocks.OAK_LOG)==166,"First chopper update removes one neighbor");
        ticks(level,1);check(blocks(level,from,to,Blocks.OAK_LOG)==166,"Child and parent delays prevent immediate follow-up chopping");
        ticks(level,600);check(blocks(level,from,to,Blocks.OAK_LOG)==0,"Connected chopping completes beyond the former 128-block limit");
        check(axe.getDamageValue()==167,"Each extra chopped block spends one durability");
        check(player.getInventory().countItem(Items.OAK_LOG)>=167,"Connected chopping collects its drops");
        for(int i=0;i<4;i++)level.setBlockAndUpdate(from.offset(i,0,0),logs);
        level.setBlockAndUpdate(from,Blocks.AIR.defaultBlockState());ElementalTools.mined(axe,level,logs,from,player,"ItemElementalAxeWater");ticks(level,1);
        equip(player,"thaumium_axe");int left=blocks(level,from,to,Blocks.OAK_LOG);ticks(level,40);
        check(left>0&&blocks(level,from,to,Blocks.OAK_LOG)==left,"Switching tools cancels pending work instead of using a replacement tool");
        for(var pos:BlockPos.betweenClosed(from,to))level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
        var hoe=equip(player,"hoe_of_the_mystic");var wheat=Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE,7);
        for(var pos:BlockPos.betweenClosed(from,from.offset(4,0,4))){level.setBlockAndUpdate(pos.below(),Blocks.FARMLAND.defaultBlockState());level.setBlockAndUpdate(pos,wheat);}
        level.setBlockAndUpdate(from,Blocks.AIR.defaultBlockState());ElementalTools.mined(hoe,level,wheat,from,player,"ItemElementalHoeMagic");ticks(level,300);
        check(blocks(level,from,from.offset(4,0,4),Blocks.WHEAT)==0&&hoe.getDamageValue()==24,"Connected crop harvest pays one durability even for zero-hardness crops");
        for(var pos:BlockPos.betweenClosed(from.below(),to))level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
        axe=equip(player,"axe_of_the_stream");axe.setDamageValue(axe.getMaxDamage()-1);
        for(int i=1;i<=3;i++)level.setBlockAndUpdate(from.offset(i,0,0),logs);
        ElementalTools.mined(axe,level,logs,from,player,"ItemElementalAxeWater");ticks(level,50);
        check(axe.isEmpty()&&blocks(level,from,to,Blocks.OAK_LOG)==2,"Breaking the bound tool stops the continuing operation");
        for(var pos:BlockPos.betweenClosed(from,to))level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
        for(boolean configured:new boolean[]{false,true})for(boolean crouched:new boolean[]{false,true}){
            dev.thaumcraft.PortConfig.toolShift=configured;player.setShiftKeyDown(crouched);
            for(var pos:BlockPos.betweenClosed(from,from.offset(6,0,6)))level.setBlockAndUpdate(pos,Blocks.DIRT.defaultBlockState());
            hoe=equip(player,"hoe_of_the_mystic");hoe.getItem().useOn(context(player,from.offset(3,0,3)));
            check(blocks(level,from,from.offset(6,0,6),Blocks.FARMLAND)==0,"Tilling is scheduled rather than completed in the use callback");
            ticks(level,1);check(blocks(level,from,from.offset(6,0,6),Blocks.FARMLAND)==1,"First tiller step converts one neighbor");
            ticks(level,200);check(blocks(level,from,from.offset(6,0,6),Blocks.FARMLAND)==32,"Mass tilling stays at 32 blocks independently of modifier settings");
            check(hoe.getDamageValue()==32,"Mass tilling charges each converted block");
        }
        dev.thaumcraft.PortConfig.toolShift=false;player.setShiftKeyDown(false);
        for(var pos:BlockPos.betweenClosed(from,to))level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
    }
    private static void growth(ServerLevel level,ServerPlayer player,BlockPos origin){
        var shovel=equip(player,"shovel_of_renewal");BlockPos plant=origin.above();
        level.setBlockAndUpdate(origin,Blocks.FARMLAND.defaultBlockState());level.setBlockAndUpdate(plant,Blocks.WHEAT.defaultBlockState());
        shovel.getItem().useOn(context(player,plant));
        check(level.getBlockState(plant).getValue(CropBlock.AGE)==7&&shovel.getDamageValue()==8,"Renewal fully fertilizes young wheat for eight durability");
        level.setBlockAndUpdate(plant,Blocks.MELON_STEM.defaultBlockState());shovel.getItem().useOn(context(player,plant));
        check(level.getBlockState(plant).getValue(StemBlock.AGE)==7&&shovel.getDamageValue()==16,"Renewal fully fertilizes a young stem");
        level.setBlockAndUpdate(origin,Blocks.MYCELIUM.defaultBlockState());level.setBlockAndUpdate(plant,Blocks.BROWN_MUSHROOM.defaultBlockState());
        for(var pos:BlockPos.betweenClosed(plant.offset(-3,1,-3),plant.offset(3,5,3)))level.setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());
        for(int i=0;i<10;i++)shovel.getItem().useOn(context(player,plant));
        check(shovel.getDamageValue()==16&&level.getBlockState(plant).is(Blocks.BROWN_MUSHROOM),"Failed mushroom growth neither consumes charge nor replaces the plant");
        for(var pos:BlockPos.betweenClosed(plant.offset(-3,1,-3),plant.offset(3,5,3)))level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(origin,Blocks.DIRT.defaultBlockState());level.setBlockAndUpdate(plant,Blocks.OAK_SAPLING.defaultBlockState());
        shovel.getItem().useOn(context(player,plant));
        check(level.getBlockState(plant).is(Blocks.OAK_LOG)&&shovel.getDamageValue()==24,"A fresh sapling immediately attempts tree growth without a bonemeal chance or stage delay");
        for(var pos:BlockPos.betweenClosed(origin.offset(-5,0,-5),origin.offset(5,12,5)))level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
    }
    private static void fire(){
        try{
            var fire=(net.minecraft.world.level.block.FireBlock)Blocks.FIRE;
            var burn=net.minecraft.world.level.block.FireBlock.class.getDeclaredMethod("getBurnOdds",net.minecraft.world.level.block.state.BlockState.class);burn.setAccessible(true);
            var ignite=net.minecraft.world.level.block.FireBlock.class.getDeclaredMethod("getIgniteOdds",net.minecraft.world.level.block.state.BlockState.class);ignite.setAccessible(true);
            for(String id:new String[]{"greatwood_log","silverwood_log","tainted_log","greatwood_leaves","silverwood_leaves","tainted_leaves","petrified_log"}){
                var state=Content.block(id).defaultBlockState();boolean leaves=id.endsWith("leaves"),inert=id.equals("petrified_log");
                check((int)burn.invoke(fire,state)==(inert?0:leaves?100:75)&&(int)ignite.invoke(fire,state)==(inert?0:leaves?3:2),id+" keeps the original flammability and fire spread");
            }
        }catch(ReflectiveOperationException e){throw new AssertionError("Equipment mechanics: fire odds unavailable",e);}
    }
    private static void targeting(ServerLevel level,ServerPlayer player,BlockPos origin){
        player.setPos(origin.getBottomCenter());player.setYRot(0);player.setXRot(0);player.setShiftKeyDown(false);player.setDeltaMovement(Vec3.ZERO);
        var victim=EntityType.ZOMBIE.create(level,EntitySpawnReason.COMMAND);victim.setPos(player.position().add(0,0,7));level.addFreshEntity(victim);
        var mask=new ItemStack(Content.item("mask_of_cruelty"));player.setItemSlot(EquipmentSlot.HEAD,mask);
        ((ArcanaItem)mask.getItem()).tickSecond(mask,level,player,EquipmentSlot.HEAD);
        check(mask.getDamageValue()==1,"The mask activates while standing normally");
        BlockPos wall=origin.offset(0,1,3);level.setBlockAndUpdate(wall,Blocks.STONE.defaultBlockState());
        ((ArcanaItem)mask.getItem()).tickSecond(mask,level,player,EquipmentSlot.HEAD);
        check(mask.getDamageValue()==2,"The original gaze targeting is not clipped by terrain");
        var obstruction=EntityType.END_CRYSTAL.create(level,EntitySpawnReason.COMMAND);obstruction.setPos(player.position().add(0,0,3));level.addFreshEntity(obstruction);
        ((ArcanaItem)mask.getItem()).tickSecond(mask,level,player,EquipmentSlot.HEAD);
        check(mask.getDamageValue()==2,"A nearer nonliving collidable target intercepts the mask's gaze");obstruction.discard();
        var sword=equip(player,"sword_of_the_zephyr");victim.setDeltaMovement(Vec3.ZERO);sword.getItem().use(level,player,InteractionHand.MAIN_HAND);
        check(victim.getDeltaMovement().z<0&&victim.getDeltaMovement().y>0&&player.getDeltaMovement().equals(Vec3.ZERO),"Zephyr pulls the entity behind a block instead of unexpectedly dashing");
        level.setBlockAndUpdate(wall,Blocks.AIR.defaultBlockState());victim.discard();
        var primary=EntityType.ENDERMAN.create(level,EntitySpawnReason.COMMAND);primary.setPos(player.position().add(0,0,6));level.addFreshEntity(primary);
        var above=EntityType.COW.create(level,EntitySpawnReason.COMMAND);above.setPos(primary.position().add(1,2,0));level.addFreshEntity(above);
        var near=EntityType.COW.create(level,EntitySpawnReason.COMMAND);near.setPos(primary.position().add(1,0,0));level.addFreshEntity(near);
        float health=above.getHealth();sword.getItem().hurtEnemy(sword,primary,player);
        check(above.getHealth()==health&&near.getHealth()<near.getMaxHealth(),"Secondary targeting uses the original position box independently of victim height");
        check(near.getHealth()==near.getMaxHealth()-4,"The secondary strike deals its four damage exactly once");
        var difficulty=level.getDifficulty();
        for(var mode:new net.minecraft.world.Difficulty[]{net.minecraft.world.Difficulty.EASY,net.minecraft.world.Difficulty.NORMAL,net.minecraft.world.Difficulty.HARD}){
            level.getServer().setDifficulty(mode,true);near.removeAllEffects();sword.getItem().hurtEnemy(sword,primary,player);
            var slow=near.getEffect(net.minecraft.world.effect.MobEffects.SLOWNESS);int expected=mode==net.minecraft.world.Difficulty.HARD?240:mode==net.minecraft.world.Difficulty.NORMAL?120:0;
            check(expected==0?slow==null:slow!=null&&slow.getDuration()==expected,"Zephyr secondary strike applies the type-2 bolt Slowness on "+mode);
        }
        level.getServer().setDifficulty(difficulty,true);
        var bolts=level.getEntitiesOfClass(dev.thaumcraft.entity.LightningEffect.class,primary.getBoundingBox().inflate(8));
        check(bolts.size()==4,"A real synchronized lightning effect accompanies each secondary strike");bolts.forEach(Entity::discard);
        primary.discard();above.discard();near.discard();player.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);
    }
    private static void boots(ServerLevel level,ServerPlayer player){
        player.setSprinting(false);player.setShiftKeyDown(false);player.setItemSlot(EquipmentSlot.FEET,ItemStack.EMPTY);
        float before=exhaustion(player);player.jumpFromGround();float normal=exhaustion(player)-before;
        for(String id:new String[]{"boots_of_striding","seven_league_boots","boots_of_the_meteor"}){
            var boots=new ItemStack(Content.item(id));player.setItemSlot(EquipmentSlot.FEET,boots);
            before=exhaustion(player);player.jumpFromGround();
            check(Math.abs(exhaustion(player)-before-normal-(id.equals("boots_of_striding")?.1f:.15f))<1e-5,id+" applies its extra jump exhaustion exactly once");
        }
        var meteor=new ItemStack(Content.item("boots_of_the_meteor"));player.setItemSlot(EquipmentSlot.FEET,meteor);player.setShiftKeyDown(true);
        before=exhaustion(player);player.jumpFromGround();
        check(ItemState.getInt(meteor,"stomp_jump",0)==0&&Math.abs(exhaustion(player)-before-normal)<1e-5,"Sneaking jump neither arms Meteor stomp nor pays for a special boost");
        player.setShiftKeyDown(false);player.jumpFromGround();check(ItemState.getInt(meteor,"stomp_jump",0)==1,"A boosted Meteor jump arms the stomp");
        player.setOnGround(false);player.setShiftKeyDown(true);player.setDeltaMovement(0,-1,0);
        ArcanaItem.stompTick(meteor,level,player);
        check(ItemState.getInt(meteor,"stomp_fire",0)==1&&Math.abs(player.getDeltaMovement().y+1.09)<1e-5,"Accelerated descent begins the ignition feedback and retains the original multiplier");
        player.setOnGround(true);ArcanaItem.stompTick(meteor,level,player);
        check(ItemState.getInt(meteor,"stomp_jump",0)==0&&ItemState.getInt(meteor,"stomp_fire",0)==0,"Landing clears Meteor activation and ignition state");
    }
}
