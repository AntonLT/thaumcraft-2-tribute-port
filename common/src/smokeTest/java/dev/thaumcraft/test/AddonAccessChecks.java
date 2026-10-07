package dev.thaumcraft.test;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.thaumcraft.api.*;
import dev.thaumcraft.content.Content;
import dev.thaumcraft.content.ModTags;
import dev.thaumcraft.gameplay.*;
import dev.thaumcraft.machine.MachineBlockEntity;
import dev.thaumcraft.machine.MachineLogic;
import dev.thaumcraft.network.ResearchSync;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import java.util.*;

/** Exercises datapack registrations, team ownership, client packets, and actual machine behavior. */
final class AddonAccessChecks {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError("Addon access: "+message);}
    static int baseline(MinecraftServer server){
        checks=0;
        check(ThaumcraftApi.server().restorerCost(new ItemStack(Items.DIAMOND_SWORD))==.25f,"Default addon repair cost");
        check(ThaumcraftApi.server().restorerCost(new ItemStack(Content.item("flying_carpet")))==1.25f,"Legacy repair cost remains identical");
        for(var biome:server.registryAccess().lookupOrThrow(Registries.BIOME).listElements().toList()){
            for(String feature:dev.thaumcraft.world.ArcaneFeatures.NAMES)
                check(biome.is(ModTags.featureBiomes(feature))==biome.is(net.minecraft.tags.BiomeTags.IS_OVERWORLD),"Default feature biome tag "+feature);
            check(biome.is(ModTags.SPAWNS_WISPS)==biome.is(net.minecraft.tags.BiomeTags.IS_OVERWORLD),"Default wisp biome tag");
            check(biome.is(ModTags.NETHER_ARCANE_VEGETATION)==biome.is(net.minecraft.tags.BiomeTags.IS_NETHER),"Default Nether vegetation tag");
        }
        var plains=server.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS).value();
        for(String feature:dev.thaumcraft.world.ArcaneFeatures.NAMES)
            check(plains.getGenerationSettings().features().stream().flatMap(set->set.stream()).anyMatch(holder->holder.is(dev.thaumcraft.world.ArcaneFeatures.placed(feature))),"Loader adds tagged feature "+feature);
        check(plains.getMobSettings().getMobs(net.minecraft.world.entity.MobCategory.MONSTER).unwrap().stream().anyMatch(entry->entry.value().type()==dev.thaumcraft.entity.ModEntities.TYPES.get("wisp")),"Loader adds tagged wisp spawn");
        return checks;
    }
    static int example(MinecraftServer server){
        checks=0;
        costsAndDuplicator(server);
        teamsAndPredicates(server);
        return checks;
    }
    static int biomeOverrides(MinecraftServer server){
        checks=0;
        for(var biome:server.registryAccess().lookupOrThrow(Registries.BIOME).listElements().toList()){
            boolean plains=biome.is(Biomes.PLAINS),nether=biome.is(Biomes.NETHER_WASTES),desert=biome.is(Biomes.DESERT);
            for(String feature:dev.thaumcraft.world.ArcaneFeatures.NAMES){
                check(biome.is(ModTags.featureBiomes(feature))==plains,"Startup pack overrides feature tag "+feature);
                boolean present=biome.value().getGenerationSettings().features().stream().flatMap(set->set.stream()).anyMatch(holder->holder.is(dev.thaumcraft.world.ArcaneFeatures.placed(feature)));
                check(present==(plains||feature.equals("arcane_vegetation")&&nether),"Loader honors feature biome inclusion and exclusion "+feature);
            }
            check(biome.is(ModTags.NETHER_ARCANE_VEGETATION)==nether,"Startup pack overrides Nether tag");
            for(String entity:dev.thaumcraft.entity.ModSpawns.NATURAL){
                boolean present=biome.value().getMobSettings().getMobs(net.minecraft.world.entity.MobCategory.MONSTER).unwrap().stream().anyMatch(entry->entry.value().type()==dev.thaumcraft.entity.ModEntities.TYPES.get(entity));
                check(present==(entity.equals("wisp")?plains:desert),"Loader honors spawn biome inclusion and exclusion "+entity);
            }
        }
        return checks;
    }
    private static MachineBlockEntity machine(MinecraftServer server,BlockPos pos,String id){
        var level=server.overworld();level.getChunkAt(pos);level.removeBlock(pos,false);
        level.setBlockAndUpdate(pos,Content.block(id).defaultBlockState());return (MachineBlockEntity)level.getBlockEntity(pos);
    }
    private static void costsAndDuplicator(MinecraftServer server){
        check(ThaumcraftApi.server().restorerCost(new ItemStack(Items.DIAMOND_SWORD))==1.5f,"Exact restorer rule beats higher priority tag");
        check(ThaumcraftApi.server().restorerCost(new ItemStack(Items.IRON_SWORD))==.6f,"Restorer tag covers other swords");
        AddonData.receive(AddonData.encode(AddonData.server()));
        check(ThaumcraftApi.client().restorerCost(new ItemStack(Items.DIAMOND_SWORD))==1.5f,"Restorer rules survive client transport");
        var pos=new BlockPos(392,280,392);var level=server.overworld();
        try{
            var repair=machine(server,pos,"thaumic_restorer");var sword=new ItemStack(Items.DIAMOND_SWORD);sword.setDamageValue(2);
            repair.setItem(0,sword);repair.insertVis(4,false);MachineLogic.tickVisProcess(level,repair);
            check(sword.getDamageValue()==1&&Math.abs(repair.pureVis()-2.5f)<.0001f,"Restorer charges configured cost per durability point");
            var duplicate=machine(server,pos,"thaumic_duplicator");duplicate.setItem(0,new ItemStack(Items.DIAMOND));
            duplicate.processes.recipe("minecraft:diamond",5,0);duplicate.processes.pureWork=5;
            MachineLogic.tickVisProcess(level,duplicate);
            check(duplicate.getItem(0).getCount()==1&&duplicate.getItem(9).isEmpty()&&duplicate.processes.pureWork==0,"Forbidden template cancels paid work without consuming input or producing output");
            check(ThaumcraftApi.server().vis(new ItemStack(Items.DIAMOND))>0,"Duplicator exclusion preserves crucible value");
            duplicate.setItem(0,new ItemStack(Items.COBBLESTONE));duplicate.processes.recipe("minecraft:cobblestone",2,0);duplicate.processes.pureWork=2;
            MachineLogic.tickVisProcess(level,duplicate);
            check(duplicate.getItem(0).isEmpty()&&duplicate.getItem(9).is(Items.COBBLESTONE)&&duplicate.getItem(9).getCount()==2,"Allowed duplicator template still completes");
        }finally{level.removeBlock(pos,false);}
    }
    private static ServerPlayer player(MinecraftServer server,String name){
        var player=new ServerPlayer(server,server.overworld(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),name),net.minecraft.server.level.ClientInformation.createDefault());
        new net.minecraft.server.network.ServerGamePacketListenerImpl(server,new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),player,net.minecraft.server.network.CommonListenerCookie.createInitial(player.getGameProfile(),false));
        return player;
    }
    private static void teamsAndPredicates(MinecraftServer server){
        var first=player(server,"AccessOne");var second=player(server,"AccessTwo");var outsider=player(server,"AccessOther");
        var research=Identifier.parse("example:resonance");var personal=GameData.legacyResearchId(0);
        var group=Identifier.parse("example:team_"+UUID.randomUUID());var other=Identifier.parse("example:team_"+UUID.randomUUID());
        var membership=new HashMap<UUID,Identifier>();membership.put(first.getUUID(),group);membership.put(second.getUUID(),group);membership.put(outsider.getUUID(),other);
        var previous=ThaumcraftKnowledge.setGroupResolver((s,id)->null);var oldSender=ResearchSync.sender;
        var packets=new HashMap<UUID,List<Identifier>>();ResearchSync.sender=(p,payload)->packets.put(p.getUUID(),payload.known());
        var pos=new BlockPos(396,280,396);Map<UUID,ServerPlayer> online=null;List<ServerPlayer> players=null;
        try{
            // Keep synthetic players visible to the normal owner and online-member lookup paths.
            var field=net.minecraft.server.players.PlayerList.class.getDeclaredField("playersByUUID");field.setAccessible(true);
            @SuppressWarnings("unchecked") var map=(Map<UUID,ServerPlayer>)field.get(server.getPlayerList());online=map;
            var listField=net.minecraft.server.players.PlayerList.class.getDeclaredField("players");listField.setAccessible(true);
            @SuppressWarnings("unchecked") var list=(List<ServerPlayer>)listField.get(server.getPlayerList());players=list;
            for(var player:List.of(first,second,outsider)){players.add(player);online.put(player.getUUID(),player);}
            check(ThaumcraftApi.unlock(server,first.getUUID(),personal),"Personal progress exists before team integration");
            ThaumcraftKnowledge.setGroupResolver((s,id)->membership.get(id));
            check(!ThaumcraftApi.knows(server,first.getUUID(),personal),"Group progress is separate from retained personal progress");
            var learned=server.getAdvancements().get(Identifier.parse("example:research_learned"));
            var infused=server.getAdvancements().get(Identifier.parse("example:infusion_completed"));
            check(learned!=null&&infused!=null,"Example advancements load through registered trigger codecs");
            var predicate=LootItemCondition.DIRECT_CODEC.parse(JsonOps.INSTANCE,JsonParser.parseString("{\"condition\":\"thaumcraft2tp:knows_research\",\"research\":\"example:resonance\"}")).getOrThrow();
            var params=new LootParams.Builder(server.overworld()).withParameter(LootContextParams.THIS_ENTITY,second).withParameter(LootContextParams.ORIGIN,second.position()).create(LootContextParamSets.COMMAND);
            var context=new LootContext.Builder(params).create(Optional.empty());
            check(!predicate.test(context),"Unknown research predicate is false");
            packets.clear();check(ThaumcraftApi.unlock(server,first.getUUID(),research),"Group unlock succeeds");
            check(ThaumcraftApi.knows(server,second.getUUID(),research)&&!ThaumcraftApi.knows(server,outsider.getUUID(),research),"Knowledge belongs to one group");
            check(packets.keySet().equals(Set.of(first.getUUID(),second.getUUID()))&&packets.get(second.getUUID()).contains(research),"Unlock synchronizes both online members only");
            check(first.getAdvancements().getOrStartProgress(learned).isDone()&&second.getAdvancements().getOrStartProgress(learned).isDone()&&!outsider.getAdvancements().getOrStartProgress(learned).isDone(),"Research trigger awards newly learning group members only");
            check(predicate.test(context),"Predicate reads shared knowledge");
            var recipe=ResourceKey.create(Registries.RECIPE,Identifier.parse("example:resonant_glass"));
            check(second.getRecipeBook().contains(recipe)&&ThaumcraftApi.canCraft(server,second.getUUID(),recipe.identifier()),"Peer receives recipes and passes crafting gate");
            packets.clear();check(!ThaumcraftApi.unlock(server,second.getUUID(),research)&&packets.isEmpty(),"Repeated shared grant is idempotent");
            var infuser=machine(server,pos,"thaumic_infuser");infuser.setOwner(first.getUUID());infuser.setItem(0,new ItemStack(Items.AMETHYST_SHARD));infuser.setItem(1,new ItemStack(Items.GLASS));infuser.insertVis(10,false);
            for(int tick=0;tick<20;tick++)MachineLogic.tickVisProcess(server.overworld(),infuser);
            check(infuser.getItem(9).is(Items.ECHO_SHARD)&&first.getAdvancements().getOrStartProgress(infused).isDone()&&!second.getAdvancements().getOrStartProgress(infused).isDone(),"Completed infusion awards online machine owner");
            var mob=new net.minecraft.world.entity.monster.zombie.Zombie(net.minecraft.world.entity.EntityType.ZOMBIE,server.overworld());
            var killParams=new LootParams.Builder(server.overworld()).withParameter(LootContextParams.THIS_ENTITY,mob).withParameter(LootContextParams.LAST_DAMAGE_PLAYER,second).withParameter(LootContextParams.ORIGIN,mob.position()).withParameter(LootContextParams.DAMAGE_SOURCE,mob.damageSources().generic()).create(LootContextParamSets.ENTITY);
            var killContext=new LootContext.Builder(killParams).create(Optional.empty());
            check(!predicate.test(killContext)&&new KnowsResearchCondition(research,LootContext.EntityTarget.ATTACKING_PLAYER).test(killContext),"Mob drops can select killing player instead of mob");
            check(!new KnowsResearchCondition(research,LootContext.EntityTarget.ATTACKER).test(context),"Missing player parameter fails closed");
            membership.put(second.getUUID(),other);ThaumcraftKnowledge.synchronize(second);
            check(!packets.get(second.getUUID()).contains(research)&&!second.getRecipeBook().contains(recipe),"Membership refresh removes stale client knowledge and recipe entries");
            membership.put(second.getUUID(),group);ThaumcraftKnowledge.synchronize(server);
            check(second.getRecipeBook().contains(recipe)&&packets.get(second.getUUID()).contains(research),"Rejoining restores saved group knowledge");
            check(ThaumcraftApi.revoke(server,second.getUUID(),research)&&!predicate.test(context)&&!first.getRecipeBook().contains(recipe),"Shared revoke relocks predicates and removes peer recipes");
            var offline=UUID.randomUUID();membership.put(offline,group);
            check(ThaumcraftApi.unlock(server,offline,research)&&ThaumcraftApi.knows(server,first.getUUID(),research),"Offline grants resolve group and update peers");
            var encoded=ArcaneWorldData.CODEC.encodeStart(JsonOps.INSTANCE,ArcaneWorldData.researchData(server.overworld())).getOrThrow();
            check(encoded.getAsJsonObject().getAsJsonObject("research").has("group:"+group),"Group bucket persists with a distinct stable key");
            var restored=ArcaneWorldData.CODEC.parse(JsonOps.INSTANCE,encoded).getOrThrow();
            check(ArcaneWorldData.CODEC.encodeStart(JsonOps.INSTANCE,restored).getOrThrow().equals(encoded),"Group and personal knowledge survive saved-data round trip");
            var generic=ProgressTriggers.RESEARCH_LEARNED.codec().parse(JsonOps.INSTANCE,JsonParser.parseString("{} ")).getOrThrow();
            var filtered=ProgressTriggers.RESEARCH_LEARNED.codec().parse(JsonOps.INSTANCE,JsonParser.parseString("{\"research\":\"example:resonance\"}")).getOrThrow();
            check(generic.matches(personal)&&filtered.matches(research)&&!filtered.matches(personal),"Optional trigger filter accepts only matching IDs");
            membership.remove(first.getUUID());ThaumcraftKnowledge.synchronize(server);
            check(ThaumcraftApi.knows(server,first.getUUID(),personal)&&!ThaumcraftApi.knows(server,first.getUUID(),research),"Leaving team restores personal progress");
        }catch(ReflectiveOperationException error){throw new AssertionError(error);}
        finally{
            for(var player:List.of(first,second,outsider)){
                player.getAdvancements().stopListening();if(players!=null)players.remove(player);
                if(online!=null)online.remove(player.getUUID());
            }
            ResearchSync.sender=oldSender;ThaumcraftKnowledge.setGroupResolver(previous);server.overworld().removeBlock(pos,false);
        }
    }
}
