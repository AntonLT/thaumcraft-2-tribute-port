package dev.thaumcraft.item;

import dev.thaumcraft.Thaumcraft;
import dev.thaumcraft.content.Content;
import dev.thaumcraft.content.CrystalBlock;
import dev.thaumcraft.content.TaintBlock;
import dev.thaumcraft.gameplay.ArcaneWorldData;
import dev.thaumcraft.gameplay.GameData;
import dev.thaumcraft.gameplay.ResearchBook;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;

import java.util.Locale;
import java.util.function.Consumer;

public class ArcanaItem extends Item {
    private final Content.Entry entry;
    // Original charms timed per item instance in singleplayer: keep that timer per owner, shared by their copies.
    private final java.util.Map<Player,Long> charmTimings=new java.util.WeakHashMap<>();
    private final java.util.Map<Player,Long> soundDelays=new java.util.WeakHashMap<>();
    public ArcanaItem(Properties properties,Content.Entry entry) {super(properties);this.entry=entry;}
    public Content.Entry entry() {return entry;}
    private boolean continuous() {return entry.source_class().equals("ItemWandFire") || entry.source_class().equals("ItemWandWater") || entry.source_class().equals("ItemElementalCutter") || entry.source_class().equals("ItemVoidCutter");}

    @Override public InteractionResult use(Level level,Player player,InteractionHand hand) {
        if(entry.id().equals("infinite_sadness")) {
            if(!level.isClientSide())player.sendSystemMessage(Component.translatableWithFallback("message.thaumcraft2tp.sadness", "Our sadness is infinite, but your work will thrive, Azanor"));
            return InteractionResult.SUCCESS;
        }
        ItemStack stack=player.getItemInHand(hand);
        String cls=entry.source_class();
        if(continuous()) {player.startUsingItem(hand);return InteractionResult.CONSUME;}
        if(level instanceof ServerLevel server && player instanceof ServerPlayer sp) {
            if(cls.equals("itemDiscovery")) {
                var definition=dev.thaumcraft.gameplay.ResearchItemData.project(stack);
                if(definition.isEmpty()){player.sendSystemMessage(Component.translatableWithFallback("message.thaumcraft2tp.research.unavailable", "Research unavailable"));return InteractionResult.SUCCESS;}
                var research=definition.get();
                dev.thaumcraft.gameplay.ResearchItemData.setProject(stack,research.key());
                int project=research.index();
                var discovery=stack.copy();
                if(!dev.thaumcraft.api.ThaumcraftApi.knows(server.getServer(),player.getUUID(),research.key())
                        &&!dev.thaumcraft.api.ThaumcraftEvents.RESEARCH_LEARNING.allows(listener->listener.allow(sp,research.key(),discovery.copy())))return InteractionResult.SUCCESS;
                if(dev.thaumcraft.api.ThaumcraftApi.unlock(server.getServer(),player.getUUID(),research.key())) {
                    player.sendSystemMessage(Component.translatableWithFallback("message.thaumcraft2tp.discovery.learned", "Discovery learned: %s", research.nameComponent()));sound(server,player,"learn");
                }
                ResearchBook.openDiscovery(sp,stack,hand,project);return InteractionResult.SUCCESS;
            }
            if(cls.equals("ItemDiscoveryTome")) {ResearchBook.openTome(sp,stack,hand);return InteractionResult.SUCCESS;}
            if(cls.equals("itemTheory")) {
                dev.thaumcraft.gameplay.ResearchItemData.migrate(stack);
                player.sendOverlayMessage(Component.translatableWithFallback("message.thaumcraft2tp.theory.study", "Study this theory in a Quaesitum with paper in slot 4."));return InteractionResult.SUCCESS;
            }
            if(cls.equals("ItemCrystalBall")){
                dev.thaumcraft.content.ModSounds.playAt(server,player.getX(),player.getY(),player.getZ(),"rune_set",SoundSource.PLAYERS,.3f,1);
                ResearchBook.openCrystalBall(sp,stack,hand,new int[]{0,-1,-1},false);return InteractionResult.SUCCESS;
            }
            if(cls.equals("ItemVisDetector") || cls.equals("ItemVisGoggles")) {
                var aura=ArcaneWorldData.get(server).aura(server,player.blockPosition());
                player.sendOverlayMessage(Component.translatableWithFallback("message.thaumcraft2tp.aura", "Aura: %s vis  |  %s taint", String.format(Locale.ROOT,"%.1f",aura.vis()), String.format(Locale.ROOT,"%.1f",aura.taint())));
                return InteractionResult.SUCCESS;
            }
            if(cls.equals("ItemWandLightning"))return lightning(server,player,stack,hand);
            if(cls.equals("ItemDawnStone")) {
                var clock=server.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.WORLD_CLOCK).getOrThrow(net.minecraft.world.clock.WorldClocks.OVERWORLD);
                var clocks=server.getServer().clockManager();long current=clocks.getTotalTicks(clock);if(Math.floorMod(current,24000)<6000)return InteractionResult.PASS;clocks.setTotalTicks(clock,current+(24000-Math.floorMod(current,24000)));
                stack.consume(1,player);dev.thaumcraft.content.ModSounds.playAt(server,player.getX(),player.getY(),player.getZ(),"recover",SoundSource.PLAYERS,1,1);return InteractionResult.SUCCESS;
            }
            if(cls.equals("ItemElementalCrusher")||cls.equals("ItemElementalPickFire")) {ElementalTools.prospect(server,player,stack,hand);return InteractionResult.SUCCESS;}
            if(cls.equals("ItemElementalAxeWater")) {
                Vec3 center=player.getEyePosition(),destination=center.add(0,-player.getBbHeight()/2,0);
                for(var dropped:server.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(center,center).inflate(8))) {
                    Vec3 motion=dropped.getDeltaMovement().add(destination.subtract(dropped.position()).normalize().scale(.3));
                    dropped.setDeltaMovement(Math.clamp(motion.x,-.35,.35),Math.clamp(motion.y,-.35,.35),Math.clamp(motion.z,-.35,.35));dropped.hurtMarked=true;
                    dev.thaumcraft.network.EquipmentEffect.send(server,dev.thaumcraft.network.EquipmentEffect.WISP,dropped.position().add(0,.1,0));
                }
                if(server.getRandom().nextInt(3)==0)charge(stack,player,hand,1);
                return InteractionResult.SUCCESS;
            }
            if(cls.equals("ItemElementalSwordAir")) {
                Vec3 look=player.getLookAngle();
                var hit=pointedEntity(player,25);
                if(hit!=null) {
                    Entity target=hit.getEntity();double distance=player.distanceTo(target)/8.0;
                    target.setDeltaMovement(target.getDeltaMovement().add(-look.x*distance,distance/2,-look.z*distance));target.hurtMarked=true;
                    dev.thaumcraft.network.EquipmentEffect.send(server,dev.thaumcraft.network.EquipmentEffect.POOF,target.position().add(-.5,target.getBbHeight()/2-.5,-.5));
                    dev.thaumcraft.content.ModSounds.playAt(server,player.getX(),player.getY(),player.getZ(),"swing",SoundSource.PLAYERS,.5f,1);
                } else if(!player.getCooldowns().isOnCooldown(stack)) {
                    player.setDeltaMovement(look.x*3,.5+look.y,look.z*3);player.hurtMarked=true;player.resetFallDistance();player.getCooldowns().addCooldown(stack,30);
                    Vec3 soundPos=player.position().add(player.getDeltaMovement().scale(2));
                    dev.thaumcraft.content.ModSounds.playAt(server,soundPos.x,soundPos.y,soundPos.z,"swing",SoundSource.PLAYERS,.25f,.5f);
                    dev.thaumcraft.network.EquipmentEffect.send(server,dev.thaumcraft.network.EquipmentEffect.POOF,player.getEyePosition().add(-.5,-1,-.5));
                }
                charge(stack,player,hand,1);return InteractionResult.SUCCESS;
            }
            return RelicActions.use(server,sp,hand,stack,entry);
        }
        return InteractionResult.PASS;
    }

    @Override public int getUseDuration(ItemStack stack,LivingEntity user) {return continuous()?72000:entry.source_class().equals("ItemWandLightning")?2:super.getUseDuration(stack,user);}
    @Override public ItemUseAnimation getUseAnimation(ItemStack stack) {return entry.source_class().contains("Cutter")?ItemUseAnimation.BLOCK:continuous()||entry.source_class().equals("ItemWandLightning")?ItemUseAnimation.BOW:super.getUseAnimation(stack);}
    @Override public void onUseTick(Level level,LivingEntity entity,ItemStack stack,int remaining) {
        if(!(level instanceof ServerLevel server) || !(entity instanceof Player player))return;
        String cls=entry.source_class();
        if(!continuous())return;
        if(cls.equals("ItemElementalCutter")) {ElementalTools.vacuum(server,player,stack);return;}
        if(cls.equals("ItemVoidCutter"))return;
        long now=System.currentTimeMillis();
        boolean play=soundDelays.getOrDefault(player,0L)<now;
        if(play)soundDelays.put(player,now+500);
        if(cls.equals("ItemWandWater")) {
            if(play){server.playSound(null,player.blockPosition(),net.minecraft.sounds.SoundEvents.WEATHER_RAIN,SoundSource.PLAYERS,.15f,1);charge(stack,player,player.getUsedItemHand(),1);}
            dev.thaumcraft.entity.RelicProjectile.waterWand(server,player);
            return;
        }
        if(cls.equals("ItemWandFire")){
            dev.thaumcraft.entity.ArcaneMote.fireWand(server,player,dev.thaumcraft.gameplay.ArcaneEnchantments.level(server,stack,"potency"));
            charge(stack,player,player.getUsedItemHand(),1);
            if(play)dev.thaumcraft.content.ModSounds.playAt(server,player.getX(),player.getY(),player.getZ(),"fireloop",SoundSource.PLAYERS,.33f,1);
        }
    }

    @Override public InteractionResult useOn(UseOnContext context) {
        Player player=context.getPlayer();
        if(player==null || !player.mayUseItemAt(context.getClickedPos(),context.getClickedFace(),context.getItemInHand()))return InteractionResult.FAIL;
        if(entry.id().equals("infinite_sadness"))return use(context.getLevel(),player,context.getHand());
        if(!(context.getLevel() instanceof ServerLevel level))return InteractionResult.PASS;
        BlockPos pos=context.getClickedPos();var state=level.getBlockState(pos);String cls=entry.source_class();
        ItemStack stack=context.getItemInHand();
        if(cls.equals("ItemVisDetector")&&level.getBlockEntity(pos) instanceof dev.thaumcraft.machine.MachineBlockEntity machine&&!machine.isTotem()) {
            var message=switch(entry.meta()) {
                case 0->Component.translatableWithFallback("message.thaumcraft2tp.detector.vis", "Detected %s Vis. %s Vis TCB", Math.round(machine.pureVis()), machine.visSuction());
                case 1->Component.translatableWithFallback("message.thaumcraft2tp.detector.taint", "Detected %s Taint. %s Taint TCB", Math.round(machine.taintedVis()), machine.taintSuction());
                default->Component.translatableWithFallback("message.thaumcraft2tp.detector.both", "Detected %s Vis and %s Taint. %s%% capacity. %s Vis TCB, %s Taint TCB", Math.round(machine.pureVis()), Math.round(machine.taintedVis()), Math.round(machine.totalVis()/Math.max(1,machine.capacity())*100), machine.visSuction(), machine.taintSuction());
            };
            int capacity=Math.round(Math.round(machine.totalVis())/Math.max(1,machine.capacity())*100);
            level.playSound(null,pos.getX(),pos.getY(),pos.getZ(),net.minecraft.sounds.SoundEvents.NOTE_BLOCK_HARP.value(),SoundSource.BLOCKS,.8f,entry.meta()==2?1+capacity/100f:1);
            player.sendSystemMessage(message);return InteractionResult.SUCCESS;
        }
        if(cls.equals("ItemComponents") && entry.meta()==10 || cls.equals("ItemSeal")) {
            var place=new net.minecraft.world.item.context.BlockPlaceContext(context);
            BlockPos target=place.getClickedPos();
            Block block=Content.block(cls.equals("ItemSeal")?"arcane_seal":"glowing_nitor");
            var placed=place.canPlace()?block.getStateForPlacement(place):null;
            // Original ItemSeal/ItemComponents only spend the item inside a successful world write.
            if(placed==null||!level.mayInteract(player,target)||!player.mayUseItemAt(target,context.getClickedFace(),stack)
                    ||!placed.canSurvive(level,target)||!level.setBlockAndUpdate(target,placed))return InteractionResult.FAIL;
            block.setPlacedBy(level,target,placed,player,stack);
            stack.consume(1,player);return InteractionResult.SUCCESS;
        }
        if(cls.equals("ItemCrystallineBell") && state.getBlock() instanceof CrystalBlock crystal) {
            int amount=state.getValue(CrystalBlock.AMOUNT);if(amount<=1)return InteractionResult.FAIL;
            level.playSound(null,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP,SoundSource.BLOCKS,.5f,.8f+amount*.1f);
            level.setBlockAndUpdate(pos,state.setValue(CrystalBlock.AMOUNT,amount-1));
            var drop=new net.minecraft.world.entity.item.ItemEntity(level,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,crystal.crystal());
            drop.setDeltaMovement(player.position().subtract(drop.position()).scale((double).1f));
            level.addFreshEntity(drop);charge(stack,player,context.getHand(),1);
            return InteractionResult.SUCCESS;
        }
        if(cls.equals("ItemElementalShovelEarth")&&state.getBlock() instanceof BonemealableBlock) {
            if(!level.mayInteract(player,pos)||!ElementalTools.grow(level,pos,state))return InteractionResult.PASS;
            charge(stack,player,context.getHand(),8);dev.thaumcraft.content.ModSounds.playAt(level,player.getX(),player.getY(),player.getZ(),"recover",SoundSource.PLAYERS,1,1);return InteractionResult.SUCCESS;
        }
        if(cls.equals("ItemElementalHoeMagic")) {
            return ElementalTools.till(level,player,context)?InteractionResult.SUCCESS:InteractionResult.PASS;
        }
        if(cls.contains("Hoe") && (state.is(Blocks.GRASS_BLOCK)||state.is(Blocks.DIRT)) && level.getBlockState(pos.above()).isAir()) {
            level.setBlockAndUpdate(pos,Blocks.FARMLAND.defaultBlockState());charge(stack,player,context.getHand(),1);return InteractionResult.SUCCESS;
        }
        if(cls.contains("Shovel") && (state.is(Blocks.GRASS_BLOCK)||state.is(Blocks.DIRT)) && level.getBlockState(pos.above()).isAir()) {
            level.setBlockAndUpdate(pos,Blocks.DIRT_PATH.defaultBlockState());charge(stack,player,context.getHand(),1);return InteractionResult.SUCCESS;
        }
        return RelicActions.useOn(level,player,context,entry);
    }

    /** Ticked by ArcaneEnchantments for the worn boots, whether in the feet slot or an accessory slot. */
    public static void stompTick(ItemStack stack,ServerLevel level,net.minecraft.world.entity.player.Player player) {
        if(player.onGround()){ItemState.setInt(stack,"stomp_jump",0);ItemState.setInt(stack,"stomp_fire",0);}
        else if(ItemState.getInt(stack,"stomp_jump",0)>0&&player.isShiftKeyDown()&&player.getDeltaMovement().y<0&&!player.getCooldowns().isOnCooldown(stack)) {
            player.setDeltaMovement(player.getDeltaMovement().multiply(1,1.09,1));player.hurtMarked=true;
            if(ItemState.getInt(stack,"stomp_fire",0)==0){
                level.playSound(null,player.blockPosition(),net.minecraft.sounds.SoundEvents.FLINTANDSTEEL_USE,SoundSource.PLAYERS,1,level.getRandom().nextFloat()*.4f+.8f);ItemState.setInt(stack,"stomp_fire",1);
            }
            level.sendParticles(ParticleTypes.FLAME,player.getX()+(level.getRandom().nextFloat()-level.getRandom().nextFloat())*.4,player.getY()+.12,player.getZ()+(level.getRandom().nextFloat()-level.getRandom().nextFloat())*.4,1,0,0,0,0);
        }
    }

    @Override public void inventoryTick(ItemStack stack,ServerLevel level,Entity owner,EquipmentSlot slot) {
        if(!(owner instanceof ServerPlayer player))return;
        dev.thaumcraft.gameplay.ResearchItemData.refreshName(stack);
        dev.thaumcraft.gameplay.ResearchItemData.refreshModel(stack);
        String cls=entry.source_class();
        if(cls.equals("ItemCharmSouls")) {
            // The original loop repeatedly read index zero: only the first orb is consumed.
            int charge=soulCharge(stack);
            var orbs=level.getEntitiesOfClass(ExperienceOrb.class,new AABB(player.position(),player.position()).inflate(2));
            if(!orbs.isEmpty()) {
                var orb=orbs.getFirst();charge-=orb.getValue();
                dev.thaumcraft.network.EquipmentEffect.send(level,dev.thaumcraft.network.EquipmentEffect.SOUL,orb.position());
                // A merged modern entity represents several separate original orbs.
                var merged=(dev.thaumcraft.mixin.ExperienceOrbAccess)orb;
                if(merged.thaumcraft$count()>1)merged.thaumcraft$count(merged.thaumcraft$count()-1);else orb.discard();
                if(charge<=0){player.getInventory().add(new ItemStack(Content.item("soul_fragment")));charge+=50;}
            }
            ItemState.setInt(stack,"soul_charge",charge);
            return;
        }
        if(cls.startsWith("ItemCharm")) {
            long now=System.currentTimeMillis();
            if(charmTimings.getOrDefault(player,0L)>=now)return;
            charmTimings.put(player,now+1000);
        } else if(level.getGameTime()%20!=0)return;
        tickSecond(stack,level,player,slot);
    }
    public void tickSecond(ItemStack stack,ServerLevel level,ServerPlayer player,EquipmentSlot slot) {
        String cls=entry.source_class();
        if(cls.equals("ItemCharmNecro")) {
            for(var mob:level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class,new AABB(player.position(),player.position()).inflate(8,3,8),m->m instanceof net.minecraft.world.entity.monster.Enemy&&m.typeHolder().is(net.minecraft.tags.EntityTypeTags.UNDEAD)&&m.getTarget() instanceof Player)) {
                mob.addEffect(new MobEffectInstance(MobEffects.NAUSEA,40,0));mob.setTarget(null);
                var selectors=(dev.thaumcraft.mixin.MobGoalsAccess)mob;
                var targets=selectors.thaumcraft$targetGoals();targets.removeAllGoals(goal->true);
                targets.addGoal(2,new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(mob,Mob.class,0,true,false,(target,world)->target instanceof net.minecraft.world.entity.monster.Enemy){
                    @Override protected double getFollowDistance(){return 16;}
                });
                // The original absolute speed is converted against base speed: temporary Slowness can make the effective value zero.
                double base=mob.getAttributeBaseValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED),speed=base>0?.23/base:1;
                if(mob instanceof net.minecraft.world.entity.monster.skeleton.AbstractSkeleton skeleton){
                    selectors.thaumcraft$goals().removeAllGoals(goal->goal instanceof net.minecraft.world.entity.ai.goal.RangedBowAttackGoal<?>);
                    selectors.thaumcraft$goals().addGoal(4,new net.minecraft.world.entity.ai.goal.RangedAttackGoal(skeleton,speed,60,10));
                }else if(mob instanceof PathfinderMob pathfinder)
                    selectors.thaumcraft$goals().addGoal(2,new net.minecraft.world.entity.ai.goal.MeleeAttackGoal(pathfinder,speed,false));
                selectors.thaumcraft$goals().tick();targets.tick();
                stack.hurtAndBreak(1,level,player,item->{});break;
            }
        }
        if(cls.equals("ItemMaskCruelty")&&slot==EquipmentSlot.HEAD) {
            var hit=pointedEntity(player,24);
            if(hit!=null&&hit.getEntity() instanceof LivingEntity target) {
                switch(level.getRandom().nextInt(4)) {
                    case 0->target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,100,6));
                    case 1->target.addEffect(new MobEffectInstance(MobEffects.POISON,100,0));
                    case 2->target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,100,6));
                    default->target.hurtServer(level,level.damageSources().playerAttack(player),1);
                }
                stack.hurtAndBreak(1,player,EquipmentSlot.HEAD);
            }
        }
        if(cls.equals("ItemCharmLife")&&player.getHealth()<player.getMaxHealth()) {
            player.heal(1);dev.thaumcraft.content.ModSounds.playAt(level,player.getX(),player.getY(),player.getZ(),"heal",SoundSource.PLAYERS,1,1);stack.hurtAndBreak(1,level,player,item->{});
        }
        if(cls.equals("ItemVisDetector")||cls.equals("ItemVisGoggles"))recordAura(stack,level,player);
        if(cls.equals("ItemCharmCleansing") && cleanse(player)) {
            dev.thaumcraft.content.ModSounds.playAt(level,player.getX(),player.getY(),player.getZ(),"heal",SoundSource.PLAYERS,1,1);stack.hurtAndBreak(1,level,player,item->{});
        }
        if(cls.equals("ItemCharmVigor")) {
            if(player.getFoodData().needsFood()) {
                player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel()+1);
                level.playSound(null,player.getX(),player.getY(),player.getZ(),net.minecraft.sounds.SoundEvents.GENERIC_EAT,SoundSource.PLAYERS,.25f,level.getRandom().nextFloat()*.5f+.5f);
                stack.hurtAndBreak(1,level,player,item->{});
            }
            if(!stack.isEmpty()&&player.getAirSupply()<150) {
                player.setAirSupply(300);
                level.playSound(null,player.getX(),player.getY(),player.getZ(),net.minecraft.sounds.SoundEvents.PLAYER_BREATH,SoundSource.PLAYERS,.8f,.5f*((level.getRandom().nextFloat()-level.getRandom().nextFloat())*.6f+2));stack.hurtAndBreak(1,level,player,item->{});
            }
        }
    }
    private static int soulCharge(ItemStack stack) {
        // Preserve accumulated XP from existing port saves when first converting the counter.
        var tag=ItemState.tag(stack);
        return tag.getIntOr("thaumcraft_soul_charge",tag.contains("thaumcraft_souls")?50-tag.getIntOr("thaumcraft_souls",0):0);
    }
    @Override public boolean isBarVisible(ItemStack stack){return entry.source_class().equals("ItemCharmSouls")?soulCharge(stack)>0:super.isBarVisible(stack);}
    @Override public int getBarWidth(ItemStack stack){return entry.source_class().equals("ItemCharmSouls")?Math.round(13-13*soulCharge(stack)/51f):super.getBarWidth(stack);}
    @Override public int getBarColor(ItemStack stack){
        if(!entry.source_class().equals("ItemCharmSouls"))return super.getBarColor(stack);
        int green=Math.round(255-soulCharge(stack)*255/51f);return (255-green)<<16|green<<8;
    }
    @Override public boolean mineBlock(ItemStack stack,Level level,net.minecraft.world.level.block.state.BlockState state,BlockPos pos,LivingEntity owner) {
        boolean mined=super.mineBlock(stack,level,state,pos,owner);
        if(level instanceof ServerLevel server&&owner instanceof ServerPlayer player)ElementalTools.mined(stack,server,state,pos,player,entry.source_class());
        return mined;
    }
    @Override public float getDestroySpeed(ItemStack stack,net.minecraft.world.level.block.state.BlockState state) {
        if(entry.source_class().contains("Crusher")&&state.is(Blocks.OBSIDIAN))return 40;
        if(entry.source_class().contains("Cutter")&&state.is(Blocks.COBWEB))return 15;
        if((entry.source_class().contains("Crusher")||entry.source_class().contains("Cutter")||entry.source_class().equals("ItemElementalShovelEarth"))&&state.getBlock() instanceof TaintBlock)return entry.source_class().equals("ItemElementalCrusher")||entry.source_class().equals("ItemElementalShovelEarth")?20:10;
        if(entry.source_class().contains("Crusher")&&(state.is(net.minecraft.tags.BlockTags.MINEABLE_WITH_SHOVEL)||state.is(Blocks.FARMLAND)||state.is(Blocks.ICE)))return Content.VOID.speed();
        if(entry.source_class().equals("ItemElementalPickFire")&&state.is(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE))return Content.THAUMIUM.speed()+1;
        return super.getDestroySpeed(stack,state);
    }
    @Override public boolean isCorrectToolForDrops(ItemStack stack,net.minecraft.world.level.block.state.BlockState state){
        if(entry.source_class().contains("Crusher")&&(state.is(Blocks.SNOW)||state.is(Blocks.SNOW_BLOCK)))return true;
        if(entry.source_class().contains("Cutter")&&state.is(Blocks.COBWEB))return true;
        return super.isCorrectToolForDrops(stack,state);
    }
    private static EntityHitResult pointedEntity(Player player,double range){
        Vec3 eye=player.getEyePosition(),end=eye.add(player.getLookAngle().scale(range));
        return ProjectileUtil.getEntityHitResult(player,eye,end,player.getBoundingBox().expandTowards(end.subtract(eye)).inflate(1),e->e!=player&&e.isPickable(),range*range);
    }
    @Override public void hurtEnemy(ItemStack stack,LivingEntity target,LivingEntity attacker) {
        if(entry.source_class().equals("ItemElementalPickFire")){
            target.igniteForSeconds(3);
            attacker.level().playSound(null,target.blockPosition(),net.minecraft.sounds.SoundEvents.FLINTANDSTEEL_USE,SoundSource.PLAYERS,1,attacker.getRandom().nextFloat()*.4f+.8f);
        }
        if((entry.source_class().equals("ItemElementalSwordAir")||entry.source_class().equals("ItemElementalCutter"))&&attacker.level() instanceof ServerLevel server) {
            int radius=entry.source_class().equals("ItemElementalCutter")?4:2;
            dev.thaumcraft.content.ModSounds.playAt(server,attacker.getX(),attacker.getY(),attacker.getZ(),"swing",SoundSource.PLAYERS,.5f,1);
            for(var nearby:server.getEntitiesOfClass(LivingEntity.class,new AABB(target.position(),target.position()).inflate(radius,radius/2.0,radius),e->e!=target&&e!=attacker&&!(e instanceof Player)&&!(e instanceof dev.thaumcraft.entity.TravelingTrunk))){
                dev.thaumcraft.entity.LightningEffect.spawn(server,target.position().add(0,target.getBbHeight()/2,0),nearby.position().add(0,nearby.getBbHeight()/2,0),6,5,4);
                dev.thaumcraft.content.ModSounds.playAt(server,nearby.getX(),nearby.getY(),nearby.getZ(),"zap",SoundSource.PLAYERS,.5f,1);
                dev.thaumcraft.network.EquipmentEffect.send(server,dev.thaumcraft.network.EquipmentEffect.POOF,nearby.position().add(-.5,nearby.getBbHeight()/2-.5,-.5));
                nearby.hurtServer(server,server.damageSources().indirectMagic(attacker,attacker),4);
                dev.thaumcraft.entity.WispEntity.applyBoltEffects(server,nearby,2);
            }
        }
        if(entry.source_class().contains("Crusher")) {
            Vec3 delta=target.position().subtract(attacker.position()).normalize();target.push(delta.x*.5,.4,delta.z*.5);
        }
        if(entry.source_class().equals("ItemVoidCutter"))target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,60,1));
        super.hurtEnemy(stack,target,attacker);
    }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,TooltipDisplay display,Consumer<Component> lines,TooltipFlag flag) {
        super.appendHoverText(stack,context,display,lines,flag);
        if(entry.id().equals("infinite_sadness"))lines.accept(Component.translatableWithFallback("tooltip.thaumcraft2tp.upgrades", "Upgrades ?"));
        if(entry.research()!=null||entry.source_class().equals("itemTheory")||entry.source_class().equals("itemDiscovery")) {
            if(entry.legacy().endsWith("itemTheory"))lines.accept(Component.translatableWithFallback("tooltip.thaumcraft2tp.theory.progress", "Progress: %s / %s", ItemState.getInt(stack,"research_progress",0), dev.thaumcraft.gameplay.ResearchItemData.project(stack).map(GameData.Project::steps).orElse(5)));
        }
        if(entry.source_class().startsWith("ItemArtifact")) {
            String rarity=switch(entry.meta()){case 2,3->"uncommon";case 4->"rare";case 5->"exceptional";default->"common";};
            String category=entry.source_class().substring("ItemArtifact".length());
            lines.accept(Component.translatable("tooltip.thaumcraft2tp.artifact."+category.toLowerCase(Locale.ROOT)+"."+rarity));
        }
        if(entry.source_class().equals("itemTheory"))lines.accept(Component.translatableWithFallback("tooltip.thaumcraft2tp.theory.difficulty", "Difficulty: %s", Component.translatable("tooltip.thaumcraft2tp.difficulty."+Math.clamp(ItemState.getInt(stack,"difficulty",dev.thaumcraft.gameplay.ResearchItemData.project(stack).map(GameData.Project::difficulty).orElse(0)),0,5))));
        if(entry.source_class().equals("ItemCarpetInert"))lines.accept(Component.translatableWithFallback("tooltip.thaumcraft2tp.carpet.imbue", "Imbue in an Infuser with an Extract of Lightest Air."));
        if(entry.source_class().equals("ItemVoidBracelet"))lines.accept(Component.translatable("tooltip.thaumcraft2tp.network."+Math.clamp(ItemState.getInt(stack,"seal_rune",-1)+1,0,6)));
    }
    public static void charge(ItemStack stack,Player player,InteractionHand hand,int amount) {if(!player.isCreative())stack.hurtAndBreak(amount,player,hand);}
    /** Helmets in this tag show the goggles' aura HUD. */
    public static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> REVEALS_AURA=net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,dev.thaumcraft.Thaumcraft.id("reveals_aura"));
    /** Stores the wearer's aura on the item for the client HUD. */
    public static void recordAura(ItemStack stack,ServerLevel level,Player player){
        var aura=ArcaneWorldData.get(level).aura(level,player.blockPosition());
        net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA,stack,tag->{
            tag.putInt("thaumcraft_aura_previous_vis",tag.getIntOr("thaumcraft_aura_vis",Math.round(aura.vis())));
            tag.putInt("thaumcraft_aura_previous_taint",tag.getIntOr("thaumcraft_aura_taint",Math.round(aura.taint())));
            tag.putInt("thaumcraft_aura_vis",Math.round(aura.vis()));tag.putInt("thaumcraft_aura_taint",Math.round(aura.taint()));
            tag.putInt("thaumcraft_aura_max",dev.thaumcraft.PortConfig.auraMax);
            tag.putInt("thaumcraft_aura_good",aura.goodVibes());tag.putInt("thaumcraft_aura_bad",aura.badVibes());
        });
    }
    /** The worn stack the aura HUD reads: a tagged helmet, or tagged gear an addon reports as worn. */
    public static ItemStack auraRevealer(Player player){return dev.thaumcraft.api.IntegrationHooks.worn(player,stack->stack.is(REVEALS_AURA));}
    /** Thaumcraft's own goggles record from their inventory tick; other tagged helmets are handled here. */
    public static void recordTaggedHelmets(ServerLevel level){
        for(var player:level.players()){
            var worn=auraRevealer(player);
            if(!worn.isEmpty()&&!worn.is(dev.thaumcraft.content.Content.item("goggles_of_revealing")))recordAura(worn,level,player);
        }
    }
    public static void sound(ServerLevel level,Player player,String sound) {level.playSound(null,player.blockPosition(),dev.thaumcraft.content.ModSounds.event(sound),SoundSource.PLAYERS,0.6f,1);}
    /** The charm cures one condition per activation, in the original priority order. */
    public static boolean cleanse(Player player) {
        if(player.isOnFire()){player.clearFire();return true;}
        for(var effect:java.util.List.of(MobEffects.BLINDNESS,MobEffects.NAUSEA,MobEffects.POISON,MobEffects.WEAKNESS))
            if(player.hasEffect(effect)){player.removeEffect(effect);return true;}
        return false;
    }
    private static InteractionResult lightning(ServerLevel level,Player player,ItemStack stack,InteractionHand hand) {
        Vec3 eye=player.getEyePosition(),end=eye.add(player.getLookAngle().scale(25));
        var block=player.pick(25,1,false);if(block.getType()!=HitResult.Type.MISS)end=block.getLocation();
        var hit=ProjectileUtil.getEntityHitResult(player,eye,end,player.getBoundingBox().expandTowards(end.subtract(eye)).inflate(1),e->e instanceof LivingEntity&&e!=player&&e.isPickable(),625);
        if(hit==null)return InteractionResult.PASS;
        var target=(LivingEntity)hit.getEntity();
        float yaw=player.getYRot()*(float)Math.PI/180;
        Vec3 start=eye.add(-Math.cos(yaw)*.16,-.1,-Math.sin(yaw)*.16).add(player.getLookAngle().scale(.25));
        var bolt=dev.thaumcraft.entity.LightningEffect.spawn(level,start,target.getBoundingBox().getCenter(),6,.3f,4);
        if(bolt==null)return InteractionResult.FAIL;
        bolt.strike(level,player,5+dev.thaumcraft.gameplay.ArcaneEnchantments.level(level,stack,"potency"));
        player.startUsingItem(hand);
        charge(stack,player,hand,1);dev.thaumcraft.content.ModSounds.playAt(level,player.getX(),player.getY(),player.getZ(),"shock",SoundSource.PLAYERS,.33f,1);return InteractionResult.SUCCESS;
    }
}
