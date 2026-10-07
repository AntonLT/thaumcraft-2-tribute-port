package dev.thaumcraft.entity;

import dev.thaumcraft.content.Content;
import dev.thaumcraft.gameplay.ArcaneWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import java.util.ArrayList;
import java.util.List;

/** Exercises the encounter contract against live entities, loaded spawn tables and real loot. */
public final class MobParitySmokeTests {
    private static int checks;
    private static final BlockPos CENTER=new BlockPos(1536,290,1536);
    private static final List<Entity> spawned=new ArrayList<>();
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError("Mob parity: "+message);}
    private static <T extends Entity> T spawn(ServerLevel level,EntityType<T> type,int x,int z){
        T entity=type.create(level,EntitySpawnReason.COMMAND);
        entity.setPos(CENTER.offset(x,0,z).getBottomCenter());entity.setNoGravity(true);entity.setOnGround(true);
        check(level.addFreshEntity(entity),"Fixture entity enters server: "+type);
        spawned.add(entity);return entity;
    }
    private static void clear(ServerLevel level){
        spawned.forEach(Entity::discard);spawned.clear();
        for(var entity:level.getEntitiesOfClass(Entity.class,new AABB(CENTER).inflate(24)))entity.discard();
    }
    public static int run(MinecraftServer server){
        checks=0;ServerLevel level=server.overworld();var chunk=ChunkPos.containing(CENTER);
        ChunkPos.rangeClosed(chunk,1).forEach(c->{level.setChunkForced(c.x(),c.z(),true);level.getChunk(c.x(),c.z());});
        level.getChunkSource().tick(()->true,false);level.waitForEntities(chunk,1);
        for(BlockPos pos:BlockPos.betweenClosed(CENTER.offset(-20,-1,-20),CENTER.offset(20,0,20)))
            level.setBlockAndUpdate(pos,pos.getY()<CENTER.getY()?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState());
        Difficulty difficulty=level.getDifficulty();server.setDifficulty(Difficulty.HARD,true);
        try{
            spawns(level);zombie(level);clear(level);undead(level);clear(level);livestock(level);clear(level);melee(level);clear(level);
            wisps(level);clear(level);slimes(level);clear(level);allies(level);clear(level);trees(level);clear(level);
        }finally{
            clear(level);server.setDifficulty(difficulty,true);
            ChunkPos.rangeClosed(chunk,1).forEach(c->level.setChunkForced(c.x(),c.z(),false));
        }
        return checks;
    }
    private static void spawns(ServerLevel level){
        var entries=level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS).value().getMobSettings().getMobs(MobCategory.MONSTER).unwrap();
        for(String id:ModSpawns.NATURAL){
            var entry=entries.stream().filter(e->e.value().type()==ModEntities.TYPES.get(id)).findFirst().orElseThrow();
            check(entry.weight()==(id.equals("brainy_zombie")?6:id.equals("tainted_tree")?3:4),"Loaded original spawn weight: "+id);
            check(entry.value().minCount()==(id.equals("brainy_zombie")?4:1),"Loaded natural spawn attempt minimum: "+id);
            check(entry.value().maxCount()==(id.equals("brainy_zombie")||id.equals("thaum_slime")?4:1),"Loaded natural spawn attempt maximum: "+id);
        }
        check(entries.stream().noneMatch(e->e.value().type()==ModEntities.TYPES.get("tainted_cow")||e.value().type()==ModEntities.TYPES.get("tainted_villager")||e.value().type()==ModEntities.TYPES.get("tainted_creeper")),"Corrupted creatures enter through conversion, not ambient spawn entries");
    }
    private static void zombie(ServerLevel level){
        for(int i=0;i<40;i++){
            var zombie=(BrainyZombie)spawn(level,ModEntities.BRAINY_ZOMBIE,0,0);
            zombie.finalizeSpawn(level,level.getCurrentDifficultyAt(CENTER),EntitySpawnReason.NATURAL,null);
            check(zombie.getMaxHealth()==25&&zombie.getArmorValue()==3&&zombie.getAttributeValue(Attributes.ATTACK_DAMAGE)==5,"Brainy stats survive hard-difficulty spawn finalization");
            check(!zombie.isBaby()&&!zombie.isPassenger()&&!zombie.canPickUpLoot()&&zombie.getMainHandItem().isEmpty(),"Brainy spawns stay adult and unarmed without jockeys or item pickup");
            check(zombie.getAttributeValue(Attributes.SPAWN_REINFORCEMENTS_CHANCE)==0&&!zombie.convertsInWater(),"No reinforcement chance or drowned conversion");
            zombie.discard();
        }
        var zombie=(BrainyZombie)spawn(level,ModEntities.BRAINY_ZOMBIE,0,0);
        var cow=spawn(level,EntityType.COW,1,0);zombie.igniteForSeconds(10);zombie.doHurtTarget(level,cow);
        check(!cow.isOnFire(),"Burning brainy zombies do not ignite victims");
        var grub=spawn(level,ModEntities.TYPES.get("grub"),3,0);
        check(((LivingEntity)grub).getAttributeValue(Attributes.ATTACK_DAMAGE)==2,"Grubs deal two damage rather than modern silverfish one");
        check(grub.getBbHeight()==.7f&&grub.is(net.minecraft.tags.EntityTypeTags.SENSITIVE_TO_BANE_OF_ARTHROPODS),"Grubs retain original height and arthropod vulnerability");
    }
    private static void undead(ServerLevel level){
        var clock=level.dimensionType().defaultClock().orElseThrow();long dayTime=level.clockManager().getTotalTicks(clock);level.clockManager().setTotalTicks(clock,6000);
        try{
            for(var type:List.of(ModEntities.BRAINY_ZOMBIE,ModEntities.SKELETON_ALLY)){
                var mob=spawn(level,type,0,0);mob.setNoAi(true);
                check(mob.isInvertedHealAndHarm()&&mob.is(net.minecraft.tags.EntityTypeTags.UNDEAD)&&mob.is(net.minecraft.tags.EntityTypeTags.SENSITIVE_TO_SMITE),"Custom undead participate in healing inversion, Smite and relic checks");
                check(!mob.canBeAffected(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON,100)),"Custom undead reject poison");
                for(int i=0;i<500&&!mob.isOnFire();i++)mob.aiStep();
                check(mob.isOnFire(),"Custom undead burn in exposed daylight");
                check(mob.getRemainingFireTicks()==(mob instanceof SkeletonAlly?40:160),"Original ally/zombie daylight burn durations are retained");mob.discard();
            }
        }finally{level.clockManager().setTotalTicks(clock,dayTime);}
    }
    private static void livestock(ServerLevel level){
        var sheep=(TaintedCreature)spawn(level,ModEntities.TYPES.get("tainted_sheep"),0,0);
        check(sheep.getArmorValue()==1&&sheep.getAttributeValue(Attributes.FOLLOW_RANGE)==12,"Sheep retain one armor and twelve-block detection");
        var cow=spawn(level,EntityType.COW,3,0);cow.setNoAi(true);
        for(int i=0;i<40;i++)sheep.tick();
        check(sheep.getTarget()==null,"Sheep leave ordinary livestock alone");
        var villager=spawn(level,EntityType.VILLAGER,3,0);villager.setNoAi(true);
        for(int i=0;i<40&&sheep.getTarget()==null;i++)sheep.tick();
        check(sheep.getTarget()==villager,"Sheep still acquire villagers");
        var tainted=(TaintedCreature)spawn(level,ModEntities.TYPES.get("tainted_villager"),8,0);
        var data=ArcaneWorldData.get(level);int before=data.aura(level,tainted.blockPosition()).badVibes();
        for(int i=0;i<40;i++)tainted.playAmbientSound();
        check(data.aura(level,tainted.blockPosition()).badVibes()>before,"Villager ambient pulses produce bad vibes");
        boolean spreading=dev.thaumcraft.PortConfig.taintSpread;dev.thaumcraft.PortConfig.taintSpread=false;
        try{before=data.aura(level,tainted.blockPosition()).badVibes();tainted.playAmbientSound();check(data.aura(level,tainted.blockPosition()).badVibes()==before,"Disabling taint spread also disables villager corruption pulses");}
        finally{dev.thaumcraft.PortConfig.taintSpread=spreading;}
    }
    private static void wisps(ServerLevel level){
        var wisp=spawn(level,ModEntities.WISP,0,0);
        var victim=spawn(level,EntityType.VILLAGER,4,0);victim.setNoAi(true);
        var bystander=spawn(level,EntityType.VILLAGER,4,0);bystander.setNoAi(true);
        wisp.setElement(3);wisp.setTarget(victim);float health=victim.getHealth();
        for(int i=0;i<19;i++)wisp.customServerAiStep(level);
        check(victim.getHealth()==health,"Wisp bolt waits for twenty visible in-range ticks");
        wisp.customServerAiStep(level);
        check(victim.getHealth()==health-1&&victim.hasEffect(net.minecraft.world.effect.MobEffects.POISON),"Wisp bolt deals one damage and the elemental effect");
        check(bystander.getHealth()==bystander.getMaxHealth()-1,"Other creatures intersecting bolt endpoints also take damage");
        var bolts=level.getEntitiesOfClass(LightningEffect.class,new AABB(CENTER).inflate(10));
        check(bolts.size()==1&&bolts.getFirst().element()==3,"Wisp attack broadcasts one bolt with its element");
        for(int i=0;i<20;i++)bolts.getFirst().tick();
        check(victim.getHealth()==health-1,"Visual bolt does not apply damage a second time");
        check(wisp.getAmbientSound()==net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP&&wisp.getSoundVolume()==.25f,"Wisp ambient sound and volume match original mappings");
        wisp.setPos(victim.position());for(int i=0;i<5;i++)wisp.customServerAiStep(level);
        check(Double.isFinite(wisp.getDeltaMovement().lengthSqr()),"Coincident target cannot create non-finite flight velocity");
        var killer=spawn(level,EntityType.ZOMBIE,10,0);killer.setNoAi(true);
        var sword=new ItemStack(Items.DIAMOND_SWORD);sword.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.LOOTING),3);killer.setItemSlot(EquipmentSlot.MAINHAND,sword);
        boolean multiple=false;
        for(int i=0;i<50;i++){
            wisp.dropFromLootTable(level,level.damageSources().mobAttack(killer),true);
            for(var drop:level.getEntitiesOfClass(ItemEntity.class,wisp.getBoundingBox().inflate(3))){
                var stack=drop.getItem();
                check(!stack.is(Content.item("vis_crystal")),"Earth wisps drop earth crystals");
                if(stack.is(Content.item("earthen_crystal"))){check(stack.getCount()>=1&&stack.getCount()<=4,"Looting III crystal quantity remains 1..4");multiple|=stack.getCount()>1;}
                drop.discard();
            }
        }
        check(multiple,"Looting increases wisp crystal quantities through actual entity loot");
    }
    private static void melee(ServerLevel level){
        var pig=(TaintedCreature)spawn(level,ModEntities.TYPES.get("tainted_pig"),0,0);
        var victim=spawn(level,EntityType.COW,1,0);victim.setNoAi(true);victim.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);victim.setHealth(100);
        pig.setTarget(victim);var goal=new LegacyMeleeGoal(pig,net.minecraft.world.entity.animal.Animal.class,1.6,false);
        check(goal.canUse(),"Legacy attack can obtain a real path");goal.start();goal.tick();
        check(victim.getHealth()==96,"Attack begins without a modern twenty-tick start throttle");
        for(int i=0;i<19;i++){victim.invulnerableTime=0;goal.tick();}
        check(victim.getHealth()==96,"Melee cooldown lasts twenty ticks");victim.invulnerableTime=0;goal.tick();
        check(victim.getHealth()==92,"Melee attacks again on tick twenty");
        victim.setPos(CENTER.getBottomCenter().add(2,0,0));
        for(int i=0;i<25;i++){victim.invulnerableTime=0;goal.tick();}
        check(victim.getHealth()==92,"A .9-wide pig cannot attack beyond its original 1.8-block reach");goal.stop();
    }
    private static void slimes(ServerLevel level){
        var slime=spawn(level,ModEntities.THAUM_SLIME,0,0);
        var data=ArcaneWorldData.get(level);data.drainVibes(level,CENTER,10000,false);data.drainVibes(level,CENTER,10000,true);
        data.addVibes(level,CENTER,0,100);
        slime.setNoAi(true);for(int i=0;i<100;i++)slime.tick();
        check(slime.getSize()==3&&slime.getMaxHealth()==9&&slime.tainted(),"One hundred bad vibes grow a size-two slime into a tainted size-three slime");
        check(data.aura(level,CENTER).badVibes()==0,"Growth consumes vibes rather than raw taint");
        var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,level.registryAccess());slime.saveWithoutId(out);
        var restored=spawn(level,ModEntities.THAUM_SLIME,8,0);restored.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),out.buildResult()));
        check(restored.getSize()==3&&restored.tainted(),"Slime growth and skin survive a save roundtrip");restored.discard();
        slime.discard();int totalChildren=0;
        for(int n=0;n<24;n++){
            var parent=spawn(level,ModEntities.THAUM_SLIME,0,0);parent.setSize(6,true);
            var before=level.getEntitiesOfClass(ThaumSlime.class,new AABB(CENTER).inflate(8));
            parent.hurtServer(level,level.damageSources().genericKill(),10000);
            var children=level.getEntitiesOfClass(ThaumSlime.class,new AABB(CENTER).inflate(8));children.removeAll(before);
            check(children.size()<=2,"Death creates at most two children immediately");totalChildren+=children.size();
            for(var child:children){check(child.getSize()==3,"Child size is half the parent's size");child.discard();}
            parent.discard();
            check(level.getEntitiesOfClass(ThaumSlime.class,new AABB(CENTER).inflate(8)).isEmpty(),"Removal cannot split a dead slime a second time");
        }
        check(totalChildren>0&&totalChildren<48,"The two death splits remain probabilistic");
    }
    private static void allies(ServerLevel level){
        var ally=spawn(level,ModEntities.SKELETON_ALLY,0,0);
        check(ally.getMainHandItem().is(Items.BOW),"Unfinalized summoned ally already carries its bow");
        var slime=spawn(level,ModEntities.THAUM_SLIME,5,0);slime.setNoAi(true);slime.setSize(3,true);
        level.setBlockAndUpdate(CENTER.above(3),Blocks.STONE.defaultBlockState());
        for(int i=0;i<100;i++){ally.setPos(CENTER.getBottomCenter());ally.clearFire();ally.tick();}
        check(ally.getTarget()==slime,"Allies recognize slime enemies outside the Monster superclass");
        int arrows=level.getEntitiesOfClass(Arrow.class,new AABB(CENTER).inflate(20)).size();
        check(arrows==3,"Allied bow fires immediately then every forty ticks even on hard difficulty");
        ally.setLifetime(1);ally.aiStep();check(ally.isRemoved(),"Allied lifetime expires without death loot");
        level.removeBlock(CENTER.above(3),false);
    }
    private static void trees(ServerLevel level){
        var tree=(TaintedTree)spawn(level,ModEntities.TYPES.get("tainted_tree"),0,0);
        var attacker=spawn(level,EntityType.COW,4,0);attacker.setNoAi(true);
        tree.hurtServer(level,level.damageSources().mobAttack(attacker),1);
        check(tree.getTarget()==attacker&&tree.isAngry(),"Tree remembers its attacker immediately");
        var arrow=spawn(level,EntityType.ARROW,3,0);arrow.setOwner(attacker);arrow.setDeltaMovement(1,0,0);
        tree.customServerAiStep(level);check(arrow.getDeltaMovement().x==-1,"Tree reverses nearby outgoing as well as incoming arrows like the original");
        for(var grub:level.getEntitiesOfClass(Grub.class,new AABB(CENTER).inflate(12)))grub.discard();
        arrow.discard();attacker.setPos(CENTER.getBottomCenter().add(0,8,0));
        for(int i=0;i<300;i++)tree.customServerAiStep(level);
        check(level.getEntitiesOfClass(Grub.class,new AABB(CENTER).inflate(12)).isEmpty(),"Tree cannot shake out grubs at targets above its vertical attack bounds");
        var sword=new ItemStack(Items.DIAMOND_SWORD);sword.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.LOOTING),3);attacker.setItemSlot(EquipmentSlot.MAINHAND,sword);
        boolean extra=false;
        for(int i=0;i<30;i++){
            tree.dropFromLootTable(level,level.damageSources().mobAttack(attacker),true);int branches=0;
            for(var drop:level.getEntitiesOfClass(ItemEntity.class,tree.getBoundingBox().inflate(3))){if(drop.getItem().is(Content.item("tainted_branch")))branches+=drop.getItem().getCount();drop.discard();}
            check(branches<=5,"Looting III tree drops stay within five branch trials");extra|=branches>2;
        }
        check(extra,"Looting III adds branch trials beyond the base two");
    }
}
