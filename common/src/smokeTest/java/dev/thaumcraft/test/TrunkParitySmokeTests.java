package dev.thaumcraft.test;

import dev.thaumcraft.content.Content;
import dev.thaumcraft.entity.ModEntities;
import dev.thaumcraft.entity.TravelingTrunk;
import dev.thaumcraft.machine.TrunkMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

/** Behavior checks against the recovered trunk, including boundaries absent from the general smoke tests. */
final class TrunkParitySmokeTests {
    private static int checks;
    private static void check(boolean condition,String message){checks++;if(!condition)throw new AssertionError("Trunk parity: "+message);}
    private static ItemStack upgrade(int meta){return new ItemStack(Content.item(Content.ENTRIES.stream().filter(e->e.source_class().equals("ItemUpgrades")&&e.meta()==meta).findFirst().orElseThrow().id()));}
    private static TravelingTrunk trunk(ServerLevel level,ServerPlayer player){
        var trunk=ModEntities.TRUNK.create(level,EntitySpawnReason.COMMAND);trunk.setOwner(player.getUUID());trunk.setPos(player.position().add(2,0,0));trunk.setNoGravity(true);return trunk;
    }
    private static CompoundTag save(TravelingTrunk trunk,ServerLevel level){var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,level.registryAccess());trunk.saveWithoutId(out);return out.buildResult();}
    private static void load(TravelingTrunk trunk,ServerLevel level,CompoundTag tag){trunk.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),tag));}
    private static ItemEntity drop(ServerLevel level,Vec3 pos,ItemStack stack){var drop=new ItemEntity(level,pos.x,pos.y,pos.z,stack);drop.setDeltaMovement(Vec3.ZERO);level.addFreshEntity(drop);return drop;}
    private static void stepAt(TravelingTrunk trunk,Vec3 pos){trunk.setPos(pos);trunk.setDeltaMovement(Vec3.ZERO);trunk.setOnGround(true);trunk.aiStep();}

    static int run(ServerLevel level,ServerPlayer player){
        checks=0;Vec3 previous=player.position();BlockPos center=new BlockPos(320,280,320);
        ArcanaParitySmokeTests.prepareEntities(level,center);player.setPos(center.getBottomCenter());level.addNewPlayer(player);
        for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)level.setBlockAndUpdate(center.offset(x,-1,z),Blocks.STONE.defaultBlockState());
        var trunk=trunk(level,player);
        try{
            check(trunk.fireImmune()&&!trunk.hurtServer(level,trunk.damageSources().inFire(),10)&&trunk.getHealth()==50,"Fire cannot hurt the trunk");
            check(trunk.canBreatheUnderwater()&&!trunk.causeFallDamage(40,1,trunk.damageSources().fall()),"Water breathing and fall protection survive the port");
            check(trunk.getAttributeValue(Attributes.ARMOR)==1&&trunk.getExperienceReward(level,player)==7,"Original armor and experience");
            check(Math.abs(trunk.getBbWidth()-.8)<.001,"Normal collision width is 0.8");
            check(trunk.installUpgrade(upgrade(5))&&trunk.getContainerSize()==36&&Math.abs(trunk.getBbWidth()-.9)<.001,"Roomy changes both inventory and hitbox");
            check(trunk.installUpgrade(upgrade(0))&&!trunk.installUpgrade(upgrade(0))&&!trunk.installUpgrade(upgrade(2)),"Two distinct supported upgrades only");
            var menu=new TrunkMenu(81,player.getInventory(),trunk,4);
            check(menu.status(4)==5&&menu.status(5)==0,"GUI preserves installation order rather than sorting IDs");menu.removed(player);
            var named=new ItemStack(Items.EMERALD,19);named.set(DataComponents.CUSTOM_NAME,Component.literal("Trunk contents"));trunk.setItem(35,named.copy());
            var tag=save(trunk,level);var restored=trunk(level,player);load(restored,level,tag);
            check(restored.getContainerSize()==36&&restored.upgradeAt(0)==5&&restored.upgradeAt(1)==0&&ItemStack.isSameItemSameComponents(restored.getItem(35),named),"Save round trip retains capacity, order and item components");restored.discard();
            check(Content.entry(trunk.removeLatestUpgrade(level)).meta()==0&&trunk.hasUpgrade(5),"Reversal removes the last installed upgrade first");
            trunk.removeLatestUpgrade(level);
            check(trunk.getContainerSize()==27&&Math.abs(trunk.getBbWidth()-.8)<.001,"Removing Roomy restores normal capacity and hitbox");
            var spilled=level.getEntitiesOfClass(ItemEntity.class,trunk.getBoundingBox().inflate(2));
            check(spilled.stream().filter(e->ItemStack.isSameItemSameComponents(e.getItem(),named)).mapToInt(e->e.getItem().getCount()).sum()==19,"Overflow drops preserve all items and components");spilled.forEach(Entity::discard);

            trunk.installUpgrade(upgrade(1));trunk.toggleStay(player);
            var far=drop(level,trunk.position().add(2,0,0),new ItemStack(Items.DIAMOND));
            trunk.aiStep();check(far.isAlive()&&far.getDeltaMovement().x<0,"Greedy attracts distant items instead of collecting them remotely");far.discard();
            trunk.setItem(5,new ItemStack(Items.DIAMOND,60));var near=drop(level,trunk.position(),new ItemStack(Items.DIAMOND,10));near.setDefaultPickUpDelay();
            trunk.aiStep();check(near.isRemoved()&&trunk.getItem(5).getCount()==64&&trunk.getItem(0).getCount()==6,"Pickup ignores delay and fills existing stacks before empty slots");
            for(int i=0;i<trunk.getContainerSize();i++)trunk.setItem(i,new ItemStack(Items.STONE,64));trunk.setItem(5,new ItemStack(Items.DIAMOND,60));
            far=drop(level,trunk.position().add(2,0,0),new ItemStack(Items.DIAMOND));trunk.aiStep();
            check(far.getDeltaMovement().equals(Vec3.ZERO),"No attraction without a completely empty slot");far.discard();
            near=drop(level,trunk.position(),new ItemStack(Items.DIAMOND,10));trunk.aiStep();
            check(trunk.getItem(5).getCount()==64&&near.getItem().getCount()==6,"Nearby partial pickup works even without empty slots");near.discard();
            trunk.clearContent();trunk.setHealth(25);trunk.setItem(0,new ItemStack(Items.APPLE,2));trunk.tick();
            check(trunk.getHealth()==29&&trunk.lid(1)==.15f,"Automatic feeding heals and animates the lid");
            for(int i=0;i<30;i++)trunk.aiStep();check(trunk.getItem(0).getCount()==1,"Automatic eating stops above half health");
            trunk.discard();trunk=trunk(level,player);

            var victim=EntityType.COW.create(level,EntitySpawnReason.COMMAND);victim.setPos(trunk.position().add(1,0,0));
            trunk.hurtServer(level,trunk.damageSources().mobAttack(victim),1);
            check(save(trunk,level).getIntOr("anger",0)==300,"Retaliation starts the original 300-tick anger");
            var angrySave=save(trunk,level);restored=trunk(level,player);load(restored,level,angrySave);
            check(save(restored,level).getIntOr("anger",0)==300,"Anger persists across loading");restored.discard();
            trunk.toggleStay(player);Vec3 combatPos=trunk.position();float health=victim.getHealth();stepAt(trunk,combatPos);
            check(victim.getHealth()==health-1,"Unupgraded attack deals one health point");
            for(int i=0;i<9;i++){victim.invulnerableTime=0;stepAt(trunk,combatPos);}
            check(victim.getHealth()==health-1,"Attack cannot repeat before ten ticks");
            for(int i=0;i<5;i++){victim.invulnerableTime=0;stepAt(trunk,combatPos);}
            check(victim.getHealth()==health-2,"Attack repeats within the original 10-14 tick window");
            trunk.installUpgrade(upgrade(2));victim.invulnerableTime=0;health=victim.getHealth();trunk.doHurtTarget(level,victim);
            check(victim.getHealth()==health-2&&trunk.getAttributeValue(Attributes.ATTACK_DAMAGE)==2,"Rage adds one damage");
            health=player.getHealth();check(!trunk.doHurtTarget(level,player)&&player.getHealth()==health,"The owner is never a damage target");
            boolean hopped=false;
            for(int i=0;i<35;i++){stepAt(trunk,combatPos);hopped|=trunk.zza>1;}
            check(hopped&&trunk.staying(),"Stay prevents following but still allows combat hops");
            trunk.toggleStay(player);trunk.setPos(player.position().add(22,0,0));trunk.setDeltaMovement(Vec3.ZERO);trunk.aiStep();
            check(trunk.distanceToSqr(player)<9&&trunk.getTarget()==null&&save(trunk,level).getIntOr("anger",-1)==0,"Teleport interrupts combat and clears anger");victim.discard();
            trunk.removeLatestUpgrade(level);
            Vec3 close=player.position().add(4.5,0,0);boolean moved=false;
            for(int i=0;i<35;i++){stepAt(trunk,close);moved|=trunk.zza!=0;}
            check(!moved,"Following does not start within five blocks");
            Vec3 distant=player.position().add(7,0,0);float ordinaryInput=0;
            for(int i=0;i<35;i++){stepAt(trunk,distant);ordinaryInput=Math.max(ordinaryInput,trunk.zza);}
            check(ordinaryInput>5.8f&&ordinaryInput<=6,"Following uses the original forward input: "+ordinaryInput);
            trunk.installUpgrade(upgrade(0));float fastInput=0;
            for(int i=0;i<35;i++){stepAt(trunk,distant);fastInput=Math.max(fastInput,trunk.zza);}
            check(fastInput>7.8f&&fastInput<=8,"Quicksilver restores the stronger hop input: "+fastInput);
            trunk.setPos(player.position().add(2,0,0));trunk.setDeltaMovement(Vec3.ZERO);trunk.setOnGround(true);trunk.installUpgrade(upgrade(2));
            var monster=EntityType.ZOMBIE.create(level,EntitySpawnReason.COMMAND);monster.setPos(trunk.position().add(3,0,0));level.addFreshEntity(monster);trunk.getRandom().setSeed(216);
            for(int i=0;i<100&&trunk.getTarget()==null;i++)trunk.aiStep();
            check(trunk.getTarget()==monster&&save(trunk,level).getIntOr("anger",0)==600,"Rage acquires a visible monster with 600 ticks of anger");monster.discard();
            trunk.setItem(0,new ItemStack(Items.GOLD_INGOT,3));trunk.hurtServer(level,trunk.damageSources().genericKill(),1000);
            var deathDrops=level.getEntitiesOfClass(ItemEntity.class,trunk.getBoundingBox().inflate(3));
            check(deathDrops.stream().anyMatch(e->e.getItem().is(Items.GOLD_INGOT))&&deathDrops.stream().noneMatch(e->Content.entry(e.getItem())!=null&&Content.entry(e.getItem()).source_class().equals("ItemUpgrades")),"Death drops contents without refunding installed upgrades");deathDrops.forEach(Entity::discard);
        }finally{trunk.discard();level.removePlayerImmediately(player,Entity.RemovalReason.DISCARDED);player.setPos(previous);}
        return checks;
    }
}
