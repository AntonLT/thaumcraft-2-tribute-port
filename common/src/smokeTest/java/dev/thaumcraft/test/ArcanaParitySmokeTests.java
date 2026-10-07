package dev.thaumcraft.test;

import com.mojang.authlib.GameProfile;
import dev.thaumcraft.content.Content;
import dev.thaumcraft.entity.*;
import dev.thaumcraft.gameplay.*;
import dev.thaumcraft.item.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.BlockHitResult;
import java.util.UUID;

/** Original arcana behavior, exercised against registered items and server entities. */
public final class ArcanaParitySmokeTests {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError("Arcana parity: "+message);}
    private static final class Player extends ServerPlayer {
        Player(MinecraftServer server,ServerLevel level){this(server,level,new GameProfile(UUID.fromString("edb472b9-1f9d-40b8-96d0-cc3357baf146"),"ArcanaParity"));}
        Player(MinecraftServer server,ServerLevel level,GameProfile profile){super(server,level,profile,ClientInformation.createDefault());
            new net.minecraft.server.network.ServerGamePacketListenerImpl(server,new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),this,net.minecraft.server.network.CommonListenerCookie.createInitial(getGameProfile(),false));
        }
        @Override public void sendSystemMessage(Component message,boolean overlay){}
        @Override public void openItemGui(ItemStack stack,InteractionHand hand){}
    }
    /** Server-picked combat particles reach a nearby viewer only while the victim is alive and marked. */
    private static void checkMarkEffects(MinecraftServer server,ServerLevel level,Player player,BlockPos pos){
        var observer=new ServerPlayer(server,level,new GameProfile(UUID.randomUUID(),"MarkObserver"),ClientInformation.createDefault());
        new net.minecraft.server.network.ServerGamePacketListenerImpl(server,new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),observer,net.minecraft.server.network.CommonListenerCookie.createInitial(observer.getGameProfile(),false));
        observer.setPos(pos.getBottomCenter());level.addNewPlayer(observer);
        var effects=new java.util.ArrayList<dev.thaumcraft.network.EquipmentEffect>();var sender=dev.thaumcraft.network.EquipmentEffect.sender;
        dev.thaumcraft.network.EquipmentEffect.sender=(recipient,effect)->{if(recipient==observer)effects.add(effect);};
        var held=player.getMainHandItem();
        var clock=(net.minecraft.world.level.storage.ServerLevelData)level.getLevelData();long time=level.getGameTime();
        try{
            var weapon=new ItemStack(Items.IRON_SWORD);
            for(String id:new String[]{"soulstealer","vampiric"})weapon.enchant(level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENCHANTMENT,dev.thaumcraft.Thaumcraft.id(id))),3);
            player.setItemInHand(InteractionHand.MAIN_HAND,weapon);var attack=level.damageSources().playerAttack(player);
            var victim=EntityType.COW.create(level,EntitySpawnReason.COMMAND);victim.setPos(pos.getBottomCenter());level.addFreshEntity(victim);
            java.util.function.IntPredicate near=kind->effects.stream().anyMatch(e->e.kind()==kind&&Math.abs(e.origin().x-victim.getX())<=1&&Math.abs(e.origin().z-victim.getZ())<=1&&e.origin().y>=victim.getY()&&e.origin().y<=victim.getY()+victim.getBbHeight()*1.2);
            ArcaneEnchantments.markAttack(level,victim,attack);ArcaneEnchantments.markBone(victim,player);ArcaneEnchantments.tick(level);
            check(near.test(dev.thaumcraft.network.EquipmentEffect.SOUL_MARK)&&near.test(dev.thaumcraft.network.EquipmentEffect.BONE_MARK),"Live Soulstealer and bone marks emit their particles around the victim to nearby viewers");
            for(int i=0;i<200&&!near.test(dev.thaumcraft.network.EquipmentEffect.VAMPIRIC);i++)ArcaneEnchantments.onHit(level,victim,attack);
            check(effects.stream().filter(e->e.kind()==dev.thaumcraft.network.EquipmentEffect.VAMPIRIC).count()%10==0&&near.test(dev.thaumcraft.network.EquipmentEffect.VAMPIRIC),"Vampiric heals burst ten sparkles around the victim");
            effects.clear();clock.setGameTime(time+41);ArcaneEnchantments.tick(level);
            check(effects.stream().noneMatch(e->e.kind()==dev.thaumcraft.network.EquipmentEffect.SOUL_MARK||e.kind()==dev.thaumcraft.network.EquipmentEffect.BONE_MARK),"Expired marks stop their particles while the victim is alive");
            clock.setGameTime(time);ArcaneEnchantments.markAttack(level,victim,attack);ArcaneEnchantments.markBone(victim,player);
            victim.discard();effects.clear();ArcaneEnchantments.tick(level);
            check(effects.stream().noneMatch(e->e.kind()==dev.thaumcraft.network.EquipmentEffect.SOUL_MARK||e.kind()==dev.thaumcraft.network.EquipmentEffect.BONE_MARK),"Removed victims stop their mark particles");
        }finally{
            clock.setGameTime(time);dev.thaumcraft.network.EquipmentEffect.sender=sender;player.setItemInHand(InteractionHand.MAIN_HAND,held);observer.discard();
        }
    }
    private static ItemStack stack(String source){return stack(source,0);}
    private static ItemStack stack(String source,int meta){var entry=Content.ENTRIES.stream().filter(e->e.source_class().equals(source)&&e.meta()==meta).findFirst().orElseThrow();return new ItemStack(Content.item(entry.id()));}
    private static void second(ItemStack stack,ServerLevel level,Player player){((ArcanaItem)stack.getItem()).tickSecond(stack,level,player,EquipmentSlot.MAINHAND);}
    @SuppressWarnings("unchecked")
    private static void expireCharm(ArcanaItem item,Player player){
        try{var timing=ArcanaItem.class.getDeclaredField("charmTimings");timing.setAccessible(true);((java.util.Map<net.minecraft.world.entity.player.Player,Long>)timing.get(item)).put(player,System.currentTimeMillis()-1);}catch(ReflectiveOperationException e){throw new AssertionError(e);}
    }
    private static void charmChecks(ServerLevel level,Player player){
        var life=stack("ItemCharmLife");var item=(ArcanaItem)life.getItem();player.setHealth(15);
        item.inventoryTick(life,level,player,null);
        var other=stack("ItemCharmLife");item.inventoryTick(other,level,player,null);
        check(player.getHealth()==16&&life.getDamageValue()==1&&other.getDamageValue()==0,"Life shares its wall-clock cooldown across copies");
        expireCharm(item,player);
        item.inventoryTick(other,level,player,null);
        check(player.getHealth()==17&&other.getDamageValue()==1,"Life activates after real time passes even in the same game tick");
        expireCharm(item,player);player.setHealth(player.getMaxHealth());var healthy=stack("ItemCharmLife");item.inventoryTick(healthy,level,player,null);
        var injured=new Player(level.getServer(),level,new GameProfile(UUID.fromString("0b6a3f52-9a51-4c1e-8d6f-3f2f6a1c7d20"),"ArcanaParityB"));injured.setHealth(10);
        var injuredLife=stack("ItemCharmLife");item.inventoryTick(injuredLife,level,injured,null);
        check(healthy.getDamageValue()==0&&injured.getHealth()==11&&injuredLife.getDamageValue()==1,"A healthy owner's Life cooldown does not suppress another owner's healing");
        var injuredCopy=stack("ItemCharmLife");item.inventoryTick(injuredCopy,level,injured,null);
        check(injured.getHealth()==11&&injuredCopy.getDamageValue()==0,"The second owner's copies still share one cooldown");
        var souls=stack("ItemCharmSouls");var soulItem=(ArcanaItem)souls.getItem();
        check(!souls.isDamageableItem(),"Souls cannot take durability damage or be repaired");
        var first=new ExperienceOrb(level,player.getX(),player.getY(),player.getZ(),1);
        var second=new ExperienceOrb(level,player.getX()+1,player.getY(),player.getZ(),1);
        level.addFreshEntity(first);level.addFreshEntity(second);
        int before=player.getInventory().countItem(Content.item("soul_fragment"));
        soulItem.inventoryTick(souls,level,player,null);
        check(first.isRemoved()!=second.isRemoved(),"Souls consumes only the first nearby orb each tick");
        check(player.getInventory().countItem(Content.item("soul_fragment"))==before+1&&ItemState.getInt(souls,"soul_charge",-1)==49,"Fresh Souls immediately produces a fragment and retains the original remaining-XP counter");
        check(soulItem.isBarVisible(souls)&&soulItem.getBarWidth(souls)==1,"Souls shows its charge on the original 51-point bar");
        first.discard();second.discard();
        var outside=new ExperienceOrb(level,player.getX()+2.4,player.getY(),player.getZ(),1);level.addFreshEntity(outside);
        soulItem.inventoryTick(souls,level,player,null);check(!outside.isRemoved(),"Souls range starts at player position rather than expanded player bounds");outside.discard();
        var large=new ExperienceOrb(level,player.getX(),player.getY(),player.getZ(),100);level.addFreshEntity(large);
        soulItem.inventoryTick(souls,level,player,null);
        check(player.getInventory().countItem(Content.item("soul_fragment"))==before+2&&ItemState.getInt(souls,"soul_charge",0)==-1,"Large orbs produce at most one fragment and retain negative charge like original item damage");
        var merged=new ExperienceOrb(level,player.getX(),player.getY(),player.getZ(),1);level.addFreshEntity(merged);
        ((dev.thaumcraft.mixin.ExperienceOrbAccess)merged).thaumcraft$count(3);soulItem.inventoryTick(souls,level,player,null);
        check(!merged.isRemoved()&&((dev.thaumcraft.mixin.ExperienceOrbAccess)merged).thaumcraft$count()==2,"Merged modern orbs spend one original XP award per tick without deleting the remaining awards");merged.discard();
        var migrated=stack("ItemCharmSouls");ItemState.setInt(migrated,"souls",17);soulItem.inventoryTick(migrated,level,player,null);
        check(ItemState.getInt(migrated,"soul_charge",-1)==33,"Existing port saves preserve accumulated XP when migrating to the original counter");
        var inventory=player.getInventory();var saved=new java.util.ArrayList<ItemStack>();
        for(int slot=0;slot<inventory.getContainerSize();slot++){saved.add(inventory.getItem(slot));inventory.setItem(slot,new ItemStack(Items.STONE,64));}
        var fullOrb=new ExperienceOrb(level,player.getX(),player.getY(),player.getZ(),1);level.addFreshEntity(fullOrb);
        var fresh=stack("ItemCharmSouls");soulItem.inventoryTick(fresh,level,player,null);
        check(fullOrb.isRemoved()&&ItemState.getInt(fresh,"soul_charge",-1)==49&&level.getEntitiesOfClass(ItemEntity.class,player.getBoundingBox().inflate(3),e->e.getItem().is(Content.item("soul_fragment"))).isEmpty(),"Full inventory still consumes XP without dropping a fragment, matching original insertion behavior");
        for(int slot=0;slot<inventory.getContainerSize();slot++)inventory.setItem(slot,saved.get(slot));
        var zombie=EntityType.ZOMBIE.create(level,EntitySpawnReason.COMMAND);zombie.setPos(player.position().add(2,0,0));zombie.setTarget(player);level.addFreshEntity(zombie);
        var enemy=EntityType.CREEPER.create(level,EntitySpawnReason.COMMAND);enemy.setPos(player.position().add(3,0,0));level.addFreshEntity(enemy);
        var dead=stack("ItemCharmNecro");second(dead,level,player);
        check(dead.getDamageValue()==1&&zombie.getTarget()==enemy,"Dead rewrites undead targeting to attack hostile mobs immediately");
        check(zombie.hasEffect(MobEffects.NAUSEA)&&!zombie.hasEffect(MobEffects.SLOWNESS),"Dead uses original confusion instead of slowness");
        zombie.removeAllEffects();zombie.setTarget(null);((dev.thaumcraft.mixin.MobGoalsAccess)zombie).thaumcraft$targetGoals().getAvailableGoals().forEach(goal->goal.stop());
        ((dev.thaumcraft.mixin.MobGoalsAccess)zombie).thaumcraft$targetGoals().tick();
        check(zombie.getTarget()==enemy,"Undead hostile targeting survives the temporary confusion effect");zombie.discard();
        var skeleton=EntityType.SKELETON.create(level,EntitySpawnReason.COMMAND);skeleton.setPos(player.position().add(2,0,0));skeleton.setTarget(player);level.addFreshEntity(skeleton);
        // Mask of Cruelty's Slowness VII leaves no effective movement speed while confusion builds the new goal.
        skeleton.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,100,6));
        check(skeleton.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)==0,"Slowness VII immobilizes the skeleton");
        second(dead,level,player);
        double ranged=((dev.thaumcraft.mixin.MobGoalsAccess)skeleton).thaumcraft$goals().getAvailableGoals().stream().filter(goal->goal.getGoal() instanceof net.minecraft.world.entity.ai.goal.RangedAttackGoal).mapToDouble(goal->{
            try{var field=net.minecraft.world.entity.ai.goal.RangedAttackGoal.class.getDeclaredField("speedModifier");field.setAccessible(true);return field.getDouble(goal.getGoal());}catch(ReflectiveOperationException e){throw new AssertionError(e);}
        }).findFirst().orElse(Double.NaN);
        check(Double.isFinite(ranged)&&ranged>0,"Dead keeps a finite ranged speed while the skeleton is slowed (speed="+ranged+")");
        check(skeleton.getTarget()==enemy&&((dev.thaumcraft.mixin.MobGoalsAccess)skeleton).thaumcraft$goals().getAvailableGoals().stream().anyMatch(goal->goal.getGoal() instanceof net.minecraft.world.entity.ai.goal.RangedAttackGoal),"Dead converts skeletons to the original ranged attack goal");
        skeleton.discard();enemy.discard();
    }
    private static void selfRepairChecks(ServerLevel level,Player player){
        var data=ArcaneWorldData.get(level);var at=new BlockPos(240,280,240);player.setPos(at.getBottomCenter());
        var levelData=(net.minecraft.world.level.storage.ServerLevelData)level.getLevelData();long time=level.getGameTime();float vis=data.aura(level,at).vis();
        var sword=new ItemStack(Items.IRON_SWORD);sword.setDamageValue(5);
        sword.enchant(level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENCHANTMENT,dev.thaumcraft.Thaumcraft.id("repair"))),1);
        player.getInventory().setItem(0,sword);level.addNewPlayer(player);
        try{
            data.changeAura(level,at,.25f-data.aura(level,at).vis(),0);levelData.setGameTime(time-time%40+40);ArcaneEnchantments.tick(level);
            check(sword.getDamageValue()==5&&Math.abs(data.aura(level,at).vis()-.25f)<1e-4f,"Self Repair keeps a partial aura remainder when it cannot repair");
            data.changeAura(level,at,.75f,0);levelData.setGameTime(time-time%40+80);ArcaneEnchantments.tick(level);
            check(sword.getDamageValue()==4&&Math.abs(data.aura(level,at).vis()-.5f)<1e-4f,"Self Repair spends half a vis for each repaired point");
        }finally{level.removePlayerImmediately(player,Entity.RemovalReason.DISCARDED);data.changeAura(level,at,vis-data.aura(level,at).vis(),0);levelData.setGameTime(time);}
    }
    private static void water(ServerLevel level,BlockPos pos){RelicProjectile.waterImpact(level,new BlockHitResult(pos.getCenter(),Direction.UP,pos,false));}
    static void prepareEntities(ServerLevel level,BlockPos pos){
        var center=new net.minecraft.world.level.ChunkPos(pos.getX()>>4,pos.getZ()>>4);
        var chunks=net.minecraft.world.level.ChunkPos.rangeClosed(center,1).toList();
        chunks.forEach(chunk->{level.setChunkForced(chunk.x(),chunk.z(),true);level.getChunk(chunk.x(),chunk.z());});
        // Entity data can be loaded before the chunk becomes queryable.
        level.getServer().managedBlock(()->{
            level.getChunkSource().pollTask();
            return chunks.stream().allMatch(chunk->level.isPositionEntityTicking(new BlockPos(chunk.x()<<4,pos.getY(),chunk.z()<<4)));
        });
        level.waitForEntities(center,1);
    }
    private static net.minecraft.world.InteractionResult useOn(ItemStack stack,Player player,BlockPos clicked,Direction face){
        player.setItemInHand(InteractionHand.MAIN_HAND,stack);
        return stack.getItem().useOn(new net.minecraft.world.item.context.UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(clicked.getCenter(),face,clicked,false)));
    }
    private static void placementChecks(ServerLevel level,Player player,BlockPos pos){
        check(!player.hasInfiniteMaterials(),"Placement checks run in survival");
        BlockPos support=pos.offset(4,0,0);var seal=Content.block("arcane_seal");
        level.setBlockAndUpdate(support,Blocks.STONE.defaultBlockState());
        for(Direction face:Direction.values()) {
            var seals=stack("ItemSeal");seals.setCount(2);BlockPos target=support.relative(face);level.setBlockAndUpdate(target,Blocks.AIR.defaultBlockState());
            var result=useOn(seals,player,support,face);var state=level.getBlockState(target);
            check(result.consumesAction()&&state.is(seal)&&state.getValue(dev.thaumcraft.machine.MachineBlock.FACING)==face&&state.canSurvive(level,target),"Seal placed on the "+face+" face faces it and is supported by the clicked block");
            check(seals.getCount()==1&&level.getBlockEntity(target) instanceof dev.thaumcraft.machine.MachineBlockEntity machine&&player.getUUID().equals(machine.owner()),"Successful "+face+" seal placement consumes one item and records its owner");
            level.setBlockAndUpdate(target,Blocks.AIR.defaultBlockState());
        }
        level.setBlockAndUpdate(support,Blocks.SMOOTH_STONE_SLAB.defaultBlockState());
        var unsupported=stack("ItemSeal");useOn(unsupported,player,support,Direction.UP);
        check(level.getBlockState(support.above()).isAir()&&unsupported.getCount()==1,"Seals need a sturdy clicked face and keep the item otherwise");
        level.setBlockAndUpdate(support,Blocks.AIR.defaultBlockState());
        BlockPos top=new BlockPos(support.getX(),level.getMaxY(),support.getZ());level.setBlockAndUpdate(top,Blocks.STONE.defaultBlockState());
        var highSeal=stack("ItemSeal");var highNitor=stack("ItemComponents",10);
        check(!useOn(highSeal,player,top,Direction.UP).consumesAction()&&highSeal.getCount()==1,"Seal above build height is rejected without consuming the item");
        check(!useOn(highNitor,player,top,Direction.UP).consumesAction()&&highNitor.getCount()==1,"Nitor above build height is rejected without consuming the item");
        var nitor=stack("ItemComponents",10);nitor.setCount(2);
        check(useOn(nitor,player,top,Direction.EAST).consumesAction()&&level.getBlockState(top.east()).is(Content.block("glowing_nitor"))&&nitor.getCount()==1,"Nitor placement inside build height places one light and consumes one item");
        level.setBlockAndUpdate(top.east(),Blocks.AIR.defaultBlockState());level.setBlockAndUpdate(top,Blocks.AIR.defaultBlockState());
    }
    static int run(MinecraftServer server){
        checks=0;ServerLevel level=server.overworld();var player=new Player(server,level);BlockPos pos=new BlockPos(240,280,240);prepareEntities(level,pos);player.setPos(pos.getBottomCenter());
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,200));player.addEffect(new MobEffectInstance(MobEffects.POISON,200));player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,200));player.igniteForSeconds(10);
        check(ArcanaItem.cleanse(player)&&!player.isOnFire()&&player.hasEffect(MobEffects.BLINDNESS),"Cleansing extinguishes fire before curing effects");
        check(ArcanaItem.cleanse(player)&&!player.hasEffect(MobEffects.BLINDNESS)&&player.hasEffect(MobEffects.POISON),"Cleansing removes exactly one effect in priority order");
        check(ArcanaItem.cleanse(player)&&!player.hasEffect(MobEffects.POISON)&&player.hasEffect(MobEffects.SLOWNESS),"Cleansing retains effects outside the original cure list");
        check(!ArcanaItem.cleanse(player),"Cleansing with no eligible condition costs nothing");player.removeAllEffects();
        var vigor=stack("ItemCharmVigor");player.getFoodData().setFoodLevel(19);player.setAirSupply(149);float saturation=player.getFoodData().getSaturationLevel();
        second(vigor,level,player);check(player.getFoodData().getFoodLevel()==20&&player.getFoodData().getSaturationLevel()==saturation,"Vigor restores one food without invented saturation");
        check(player.getAirSupply()==player.getMaxAirSupply()&&vigor.getDamageValue()==2,"Vigor refills low air and charges each action");second(vigor,level,player);check(vigor.getDamageValue()==2,"Full hunger and air do not spend Vigor");
        var life=stack("ItemCharmLife");player.setHealth(17);second(life,level,player);check(player.getHealth()==18&&life.getDamageValue()==1,"Life charm heals one health per second");
        charmChecks(level,player);
        selfRepairChecks(level,new Player(level.getServer(),level));
        var voidTool=stack("ItemVoidCutter");voidTool.setDamageValue(10);second(voidTool,level,player);check(voidTool.getDamageValue()==10,"Void material does not grant an invented passive repair");
        level.setBlockAndUpdate(pos,Blocks.LAVA.defaultBlockState());water(level,pos);check(level.getBlockState(pos).is(Blocks.OBSIDIAN),"Water orb solidifies source lava");
        level.setBlockAndUpdate(pos,Blocks.LAVA.defaultBlockState().setValue(LiquidBlock.LEVEL,2));water(level,pos);check(level.getBlockState(pos).is(Blocks.COBBLESTONE),"Water orb solidifies shallow flowing lava");
        level.setBlockAndUpdate(pos,Blocks.WATER.defaultBlockState());water(level,pos);check(level.getBlockState(pos).is(Blocks.ICE),"Water orb freezes source water");
        level.setBlockAndUpdate(pos,Blocks.FARMLAND.defaultBlockState());water(level,pos);check(level.getBlockState(pos).getValue(FarmlandBlock.MOISTURE)==7,"Water orb irrigates farmland");
        level.setBlockAndUpdate(pos,Blocks.SAND.defaultBlockState());water(level,pos);check(level.getBlockState(pos).is(Blocks.DIRT),"Water orb converts sand to dirt");
        level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
        var lightning=stack("ItemWandLightning");player.setItemInHand(InteractionHand.MAIN_HAND,lightning);player.setYRot(0);player.setXRot(0);
        lightning.getItem().use(level,player,InteractionHand.MAIN_HAND);check(lightning.getDamageValue()==0&&!player.isUsingItem(),"Lightning misses neither spend charge nor start continuous use");
        var victim=EntityType.ZOMBIE.create(level,EntitySpawnReason.COMMAND);victim.setPos(player.position().add(0,0,20));level.getChunkAt(victim.blockPosition());level.addFreshEntity(victim);
        lightning.getItem().use(level,player,InteractionHand.MAIN_HAND);check(victim.getHealth()==15&&lightning.getDamageValue()==1,"Lightning reaches twenty blocks and deals original five damage (health="+victim.getHealth()+", charge="+lightning.getDamageValue()+")");victim.discard();
        var trunk=ModEntities.TRUNK.create(level,EntitySpawnReason.COMMAND);trunk.setPos(player.position().add(2,0,0));trunk.setOwner(player.getUUID());check(trunk.getMaxHealth()==50,"Traveling trunk has original fifty health");
        trunk.setHealth(20);player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.APPLE,2));trunk.mobInteract(player,InteractionHand.MAIN_HAND);check(trunk.getHealth()==24&&player.getMainHandItem().getCount()==1,"Owner can feed the injured trunk");
        trunk.setItem(0,new ItemStack(Items.APPLE,2));trunk.aiStep();check(trunk.getHealth()==28&&trunk.getItem(0).getCount()==1,"Trunk eats stored food at half health");
        var item=new ItemEntity(level,trunk.getX(),trunk.getY(),trunk.getZ(),new ItemStack(Items.DIAMOND));item.setNoPickUpDelay();level.addFreshEntity(item);trunk.tickCount=10;trunk.aiStep();check(item.isAlive(),"Trunk without Greedy does not vacuum items");
        trunk.installUpgrade(stack("ItemUpgrades",1));trunk.toggleStay(player);trunk.tickCount=20;trunk.aiStep();check(item.isRemoved()&&java.util.stream.IntStream.range(0,trunk.getContainerSize()).anyMatch(i->trunk.getItem(i).is(Items.DIAMOND)),"Greedy upgrade collects items while staying");trunk.discard();item.discard();
        var trade=stack("ItemWandEqualTrade");player.setItemInHand(InteractionHand.MAIN_HAND,trade);
        var context=new net.minecraft.world.item.context.UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(pos.getCenter(),Direction.UP,pos,false));
        var selected=Blocks.OAK_LOG.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS,Direction.Axis.X);
        level.setBlockAndUpdate(pos,selected);player.setShiftKeyDown(true);trade.getItem().useOn(context);player.setShiftKeyDown(false);
        level.setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());level.setBlockAndUpdate(pos.offset(1,1,1),Blocks.STONE.defaultBlockState());
        player.getInventory().add(new ItemStack(Items.OAK_LOG,2));trade.getItem().useOn(context);
        check(level.getBlockState(pos).is(Blocks.STONE),"Equal Trade waits for its helper tick instead of replacing immediately");
        EqualTrade.tick(level);
        check(level.getBlockState(pos).equals(selected)&&level.getBlockState(pos.offset(1,1,1)).is(Blocks.STONE),"Equal Trade starts with one block");
        for(int tick=0;tick<8;tick++)EqualTrade.tick(level);
        check(level.getBlockState(pos).equals(selected)&&level.getBlockState(pos.offset(1,1,1)).equals(selected),"Equal Trade traverses diagonal neighbors and preserves selected state");
        check(trade.getDamageValue()==2&&java.util.stream.IntStream.range(0,player.getInventory().getContainerSize()).noneMatch(i->player.getInventory().getItem(i).is(Items.OAK_LOG)),"Equal Trade charges each block and consumes matching inventory");
        level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());level.setBlockAndUpdate(pos.offset(1,1,1),Blocks.AIR.defaultBlockState());
        var striding=stack("ItemStridingBoots");player.setItemSlot(EquipmentSlot.FEET,striding);player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);player.jumpFromGround();
        check(Math.abs(player.getDeltaMovement().y-.72)<.01,"Striding boots add original jump impulse");player.setItemSlot(EquipmentSlot.FEET,ItemStack.EMPTY);
        var boneVictim=EntityType.COW.create(level,EntitySpawnReason.COMMAND);boneVictim.setPos(pos.getBottomCenter());level.addFreshEntity(boneVictim);ArcaneEnchantments.markBone(boneVictim,player);boneVictim.setHealth(0);ArcaneEnchantments.tick(level);
        var allies=level.getEntitiesOfClass(SkeletonAlly.class,boneVictim.getBoundingBox().inflate(2));check(allies.size()==1&&allies.getFirst().getMainHandItem().is(Items.BOW),"Bone mark raises a bow-wielding ally after death");allies.forEach(Entity::discard);boneVictim.discard();
        checkMarkEffects(server,level,player,pos);
        var chest=new net.minecraft.world.SimpleContainer(27);chest.setItem(0,new ItemStack(Items.EMERALD,9));ArtifactLoot.fillOriginalTreasure(chest,net.minecraft.util.RandomSource.create(125));check(chest.getItem(0).is(Items.EMERALD)&&chest.getItem(0).getCount()==9,"Treasure augmentation preserves occupied slots");
        check(java.util.stream.IntStream.range(1,27).anyMatch(i->!chest.getItem(i).isEmpty()),"Original weighted treasure fills empty slots");
        placementChecks(level,player,pos);
        checks+=WandParitySmokeTests.run(level,player);
        checks+=EquipmentMechanicsSmokeTests.run(level,player);
        checks+=TrunkParitySmokeTests.run(level,player);
        return checks;
    }
}
