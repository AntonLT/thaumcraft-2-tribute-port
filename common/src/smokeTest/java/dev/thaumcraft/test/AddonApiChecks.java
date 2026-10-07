package dev.thaumcraft.test;

import dev.thaumcraft.api.ThaumcraftApi;
import dev.thaumcraft.api.ThaumcraftEvents;
import dev.thaumcraft.api.ThaumcraftVis;
import dev.thaumcraft.api.VisContainer;
import dev.thaumcraft.content.Content;
import dev.thaumcraft.gameplay.*;
import dev.thaumcraft.item.ItemState;
import dev.thaumcraft.machine.MachineBlockEntity;
import dev.thaumcraft.machine.MachineConnections;
import dev.thaumcraft.machine.VisNetwork;
import dev.thaumcraft.network.CatalogSync;
import dev.thaumcraft.recipe.InfusionRecipeData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.storage.LevelResource;
import java.nio.file.*;
import java.util.*;

/** Exercises the shipped example through real resource reloads, gates, machines, and addon removal. */
final class AddonApiChecks {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    static int runLocalization(MinecraftServer server){
        checks=0;Path root=Path.of("").toAbsolutePath();
        while(root!=null&&!Files.isRegularFile(root.resolve("examples/addon-api/pack.mcmeta")))root=root.getParent();
        if(root==null)throw new AssertionError("Cannot locate shipped addon example");
        Path source=root.resolve("examples/addon-api"),pack=server.getWorldPath(LevelResource.DATAPACK_DIR).resolve("addon-api-localization");
        var original=List.copyOf(server.getPackRepository().getSelectedIds());
        try{
            try(var paths=Files.walk(source)){for(Path path:paths.toList()){Path target=pack.resolve(source.relativize(path).toString());if(Files.isDirectory(path))Files.createDirectories(target);else Files.copy(path,target,StandardCopyOption.REPLACE_EXISTING);}}
            Path override=pack.resolve("data/thaumcraft2tp/thaumcraft2tp/research/thaumic_restorer.json");Files.createDirectories(override.getParent());
            server.getPackRepository().reload();var selected=new ArrayList<>(original);selected.add("file/addon-api-localization");reload(server,selected);
            localization(server,pack,selected,override);
        }catch(java.io.IOException error){throw new AssertionError("Localization pack fixture",error);}
        finally{reload(server,original);AddonData.clearClient();}
        return checks;
    }
    static int run(MinecraftServer server){return run(server,false);}
    static int run(MinecraftServer server,boolean accessOnly){
        checks=0;
        checks+=AddonAccessChecks.baseline(server);
        if(!accessOnly){
            crucibleEvent(server);
            visContainers(server);
            taintVeto(server);
            treasureIdentity();
            blockRemoving(server);
        }
        Path root=Path.of("").toAbsolutePath();
        while(root!=null&&!Files.isRegularFile(root.resolve("examples/addon-api/pack.mcmeta")))root=root.getParent();
        if(root==null)throw new AssertionError("Cannot locate shipped addon example");
        Path source=root.resolve("examples/addon-api"),pack=server.getWorldPath(LevelResource.DATAPACK_DIR).resolve("addon-api-example");
        var original=List.copyOf(server.getPackRepository().getSelectedIds());
        var owner=UUID.randomUUID();var project=Identifier.parse("example:resonance");
        var level=server.overworld();var pos=new BlockPos(304,280,304);
        var unlocked=new ArrayList<Identifier>();var revoked=new ArrayList<Identifier>();var infused=new ArrayList<Identifier>();int[] reloads={0};
        ThaumcraftEvents.ResearchChanged onUnlock=(s,player,id)->{if(player.equals(owner))unlocked.add(id);},onRevoke=(s,player,id)->{if(player.equals(owner))revoked.add(id);};
        ThaumcraftEvents.CatalogReloaded onReload=(s,catalog)->reloads[0]++;
        ThaumcraftEvents.InfusionCompleted onInfusion=(l,at,machineOwner,recipe,result)->{if(at.equals(pos))infused.add(recipe);};
        try{
            try(var paths=Files.walk(source)){for(Path path:paths.toList()){
                Path target=pack.resolve(source.relativize(path).toString());
                if(Files.isDirectory(path))Files.createDirectories(target);else Files.copy(path,target,StandardCopyOption.REPLACE_EXISTING);
            }}
            Files.writeString(pack.resolve("data/example/thaumcraft2tp/vis/absent_mod.json"),"{\"ingredient\":\"minecraft:diamond\",\"value\":999,\"required_mods\":[\"definitely_absent_mod\"]}");
            Files.writeString(pack.resolve("data/example/thaumcraft2tp/vis/present_mod.json"),"{\"ingredient\":\"minecraft:emerald\",\"value\":77,\"required_mods\":[\"minecraft\"]}");
            Files.writeString(pack.resolve("data/example/thaumcraft2tp/vis/broken.json"),"{\"ingredient\":");
            Files.writeString(pack.resolve("data/example/recipe/absent_item.json"),"{\"type\":\"thaumcraft2tp:infusion\",\"cost\":5,\"ingredients\":[\"minecraft:glass\"],\"result\":{\"id\":\"definitely_absent_mod:gem\"}}");
            Files.writeString(pack.resolve("data/example/thaumcraft2tp/boosters/missing.json"),"{\"block\":\"example:missing_block\",\"enchanting\":1}");
            Files.writeString(pack.resolve("data/example/thaumcraft2tp/taint_blocks/bad_material.json"),"{\"block\":\"minecraft:bricks\",\"result\":\"minecraft:stone\",\"material\":3}");
            Files.writeString(pack.resolve("data/example/thaumcraft2tp/treasure/unknown_pool.json"),"{\"pool\":\"example:nowhere\",\"entries\":[{\"items\":[\"minecraft:stone\"]}]}");
            Files.writeString(pack.resolve("data/example/thaumcraft2tp/treasure/missing_item.json"),"{\"pool\":\"thaumcraft2tp:chest\",\"entries\":[{\"items\":[\"example:missing\"]}]}");
            Files.createDirectories(pack.resolve("data/thaumcraft2tp/thaumcraft2tp/treasure/legacy"));
            Files.writeString(pack.resolve("data/thaumcraft2tp/thaumcraft2tp/treasure/legacy/eldritch.json"),"{\"remove\":true}");
            Files.writeString(pack.resolve("data/example/recipe/bad_components.json"),"{\"type\":\"thaumcraft2tp:infusion\",\"cost\":5,\"ingredients\":[\"minecraft:glass\"],\"result\":{\"id\":\"minecraft:book\",\"components\":{\"minecraft:enchantments\":{\"example:missing\":1}}}}");
            Path override=pack.resolve("data/thaumcraft2tp/thaumcraft2tp/research/thaumic_restorer.json");Files.createDirectories(override.getParent());
            Files.writeString(override,"{\"name\":\"Broken override\"}");
            ThaumcraftEvents.RESEARCH_UNLOCKED.register(onUnlock);ThaumcraftEvents.RESEARCH_REVOKED.register(onRevoke);
            ThaumcraftEvents.CATALOG_RELOADED.register(onReload);ThaumcraftEvents.INFUSION_COMPLETED.register(onInfusion);
            server.getPackRepository().reload();var selected=new ArrayList<>(original);selected.add("file/addon-api-example");reload(server,selected);
            check(ThaumcraftApi.apiVersion()==1,"Runtime API version is 1");
            check(reloads[0]>0,"Accepted catalog posts the reload event");
            check(ThaumcraftApi.server().vis(new ItemStack(Items.DIAMOND))!=999,"Rule for an absent mod is ignored");
            check(ThaumcraftApi.server().vis(new ItemStack(Items.EMERALD))==77,"Rule for a present mod applies");
            check(ThaumcraftApi.server().infusion(Identifier.parse("example:absent_item")).isEmpty(),"Recipe for an absent item is skipped");
            var restorer=ThaumcraftApi.server().research(Identifier.parse("thaumcraft2tp:thaumic_restorer")).orElseThrow();
            check(!restorer.name().getString().equals("Broken override")&&restorer.category().equals(Identifier.parse("thaumcraft2tp:lost")),"Invalid built-in override keeps the built-in definition");
            specialChance(server,pack,selected,project,pos.above());
            if(accessOnly){
                checks+=AddonAccessChecks.example(server);
                accessRuleReloads(server,pack,selected);
                reload(server,original);
                check(ThaumcraftApi.server().restorerCost(new ItemStack(Items.DIAMOND_SWORD))==.25f&&!new ItemStack(Items.DIAMOND).is(dev.thaumcraft.content.ModTags.DUPLICATOR_FORBIDDEN),"Removing addon restores cost defaults and duplicator permission");
                return checks;
            }
            var overlap=new GameData.Infusion(new GameData.StackDef("minecraft:stone",1),1,List.of("#minecraft:planks","minecraft:oak_planks"),false,-1);
            check(GameData.allocateNormal(overlap,List.of(new ItemStack(Items.OAK_PLANKS),new ItemStack(Items.BIRCH_PLANKS)),2)!=null,"Overlapping tag and exact ingredients match independently of slot order");
            check(ThaumcraftApi.server().research(project).isPresent(),"Example project loads through loader reload hook");
            check(ThaumcraftApi.server().research(project).orElseThrow().category().equals(Identifier.parse("example:resonance")),"Addon category resolves by ID");
            check(GameData.infusions().size()==76,"Native recipe manager supplies the three addon infusions");
            catalogRules(server);
            exclusionTags(server);
            checks+=AddonAccessChecks.example(server);
            check(ThaumcraftApi.server().vis(new ItemStack(Items.AMETHYST_SHARD))==12,"Data pack vis rule is effective");
            var wornChestplate=new ItemStack(Items.IRON_CHESTPLATE);wornChestplate.setDamageValue(1);
            check(ThaumcraftApi.server().vis(wornChestplate)==0,"Damaged ordinary equipment has no recipe-derived vis");
            check(ThaumcraftApi.server().vis(new ItemStack(Items.IRON_CHESTPLATE))==40,"Pristine equipment retains original ingredient value");
            check(ThaumcraftApi.server().vis(wornChestplate)==0,"Pristine cached value cannot make damaged equipment meltable");
            check(ThaumcraftApi.server().researchValue(new ItemStack(Items.QUARTZ))==30,"Data pack research source is effective");
            var theory=ThaumcraftApi.server().theory(project).orElseThrow();var discovery=ThaumcraftApi.server().discovery(project).orElseThrow();
            check(theory.is(Content.item("theory_generic"))&&ResearchItemData.project(theory).orElseThrow().key().equals(project),"Generic theory carries addon identity");
            check(discovery.is(Content.item("discovery_generic"))&&ResearchItemData.project(discovery).orElseThrow().key().equals(project),"Generic discovery carries addon identity");
            var asked=new ArrayList<Identifier>();
            ThaumcraftEvents.ResearchLearning denyLearning=(reader,id,item)->{asked.add(id);return !reader.getUUID().equals(owner);};
            ThaumcraftEvents.RESEARCH_LEARNING.register(denyLearning);
            try{
                var reader=new net.minecraft.server.level.ServerPlayer(server,level,new com.mojang.authlib.GameProfile(owner,"AddonReader"),net.minecraft.server.level.ClientInformation.createDefault());
                reader.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,discovery.copy());
                reader.getMainHandItem().use(level,reader,net.minecraft.world.InteractionHand.MAIN_HAND);
                check(asked.equals(List.of(project))&&!ThaumcraftApi.knows(server,owner,project)&&unlocked.isEmpty(),"Denied discovery leaves research unlearned");
            }finally{ThaumcraftEvents.RESEARCH_LEARNING.unregister(denyLearning);}
            check(!ThaumcraftApi.canCraft(server,owner,Identifier.parse("example:resonant_glass")),"Addon craft lock denies unknown research");
            level.getChunkAt(pos);level.setBlockAndUpdate(pos,Content.block("thaumic_infuser").defaultBlockState());
            var machine=(MachineBlockEntity)level.getBlockEntity(pos);machine.setOwner(owner);
            machine.setItem(0,new ItemStack(Items.AMETHYST_SHARD));machine.setItem(1,new ItemStack(Items.GLASS));machine.insertVis(40,false);
            machine.processes.tick(level);check(machine.processes.pureCost==0,"Addon infusion uses the same research gate");
            check(ThaumcraftApi.server().requiredResearch(Identifier.parse("example:resonant_glass")).orElseThrow().equals(project),"Catalog reports the craft lock");
            check(ThaumcraftApi.server().allResearch().stream().anyMatch(r->r.id().equals(project))&&ThaumcraftApi.server().category(project).isPresent(),"Catalog lists addon research and categories");
            check(ThaumcraftApi.unlock(server,owner,project),"API grants addon research");
            check(unlocked.equals(List.of(project)),"Unlock posts one event");
            check(!ThaumcraftApi.unlock(server,owner,project),"Repeated unlock is idempotent");
            check(unlocked.size()==1,"Repeated unlock posts no event");
            check(ThaumcraftApi.canCraft(server,owner,Identifier.parse("example:resonant_glass")),"Unlock opens addon craft");
            check(!ThaumcraftApi.canCraft(server,null,Identifier.parse("example:echo_shard")),"Ownerless infusion stays locked even when someone knows it");
            var askedInfusion=new ArrayList<Identifier>();
            ThaumcraftEvents.InfusionAllowed denyInfusion=(l,at,machineOwner,recipe)->{if(!at.equals(pos))return true;askedInfusion.add(recipe);return false;};
            ThaumcraftEvents.INFUSION_ALLOWED.register(denyInfusion);
            try{
                machine.processes.tick(level);machine.processes.tick(level);
                check(machine.processes.pureCost==0&&machine.processes.pureWork==0&&askedInfusion.contains(Identifier.parse("example:echo_shard")),"Denied infusion does not start");
            }finally{ThaumcraftEvents.INFUSION_ALLOWED.unregister(denyInfusion);}
            machine.processes.tick(level);machine.processes.tick(level);
            check(machine.processes.pureWork>0&&machine.getItem(9).isEmpty(),"Addon infusion begins partial work");
            float oldValue=ThaumcraftApi.server().vis(new ItemStack(Items.ECHO_SHARD));
            var oldCatalog=AddonData.server();
            Path recipe=pack.resolve("data/example/recipe/echo_shard.json");
            String originalRecipe=Files.readString(recipe);Files.writeString(recipe,originalRecipe.replace("\"cost\": 5","\"cost\": 7"));reload(server,selected);
            check(AddonData.server()!=oldCatalog,"Reload publishes a new snapshot");
            check(ThaumcraftApi.server().vis(new ItemStack(Items.ECHO_SHARD))==oldValue+2,"Recipe reload invalidates derived vis cache");
            machine.processes.tick(level);
            check(machine.processes.pureWork==0&&machine.processes.pureCost==7&&machine.getItem(9).isEmpty(),"Changed recipe clears stale work before producing anything");
            for(int i=0;i<40&&machine.getItem(9).isEmpty();i++)machine.processes.tick(level);
            check(machine.getItem(9).is(Items.ECHO_SHARD)&&machine.getItem(0).isEmpty()&&machine.getItem(1).isEmpty(),"Addon infusion completes and consumes exact inputs");
            check(infused.equals(List.of(Identifier.parse("example:echo_shard"))),"Completed infusion posts its recipe");
            machine.setItem(9,ItemStack.EMPTY);machine.setItem(0,new ItemStack(Items.AMETHYST_SHARD));machine.setItem(1,new ItemStack(Items.GLASS));machine.processes.tick(level);machine.processes.tick(level);
            Path tag=pack.resolve("data/example/tags/item/resonant_crystals.json");String originalTag=Files.readString(tag);
            Files.writeString(tag,originalTag.replace("minecraft:amethyst_shard","minecraft:quartz"));reload(server,selected);machine.processes.tick(level);
            check(machine.processes.pureWork==0&&machine.getItem(0).is(Items.AMETHYST_SHARD)&&machine.getItem(9).isEmpty(),"Tag membership reload cancels stale infusion without consuming inputs");
            Files.writeString(tag,originalTag);reload(server,selected);
            var validRecipeCatalog=AddonData.server();Files.writeString(recipe,originalRecipe.replace("\"cost\": 5","\"cost\": 0"));reload(server,selected);
            check(AddonData.server()!=validRecipeCatalog&&ThaumcraftApi.server().infusion(Identifier.parse("example:echo_shard")).isEmpty()&&ThaumcraftApi.server().research(project).isPresent(),"Malformed infusion is skipped without rejecting other definitions");
            Files.writeString(recipe,originalRecipe.replace("\"cost\": 5","\"cost\": 7"));reload(server,selected);
            var good=AddonData.server();Path research=pack.resolve("data/example/thaumcraft2tp/research/resonance.json");
            String originalResearch=Files.readString(research);Files.writeString(research,originalResearch.replace("thaumcraft2tp:thaumic_restorer","example:resonance"));reload(server,selected);
            check(AddonData.server()!=good&&ThaumcraftApi.server().research(project).isEmpty(),"Cyclic research is skipped");
            check(ThaumcraftApi.server().infusion(Identifier.parse("example:echo_shard")).isEmpty(),"Infusion gated by skipped research is skipped");
            check(ThaumcraftApi.server().requiredResearch(Identifier.parse("example:resonant_glass")).orElseThrow().equals(dev.thaumcraft.gameplay.AddonData.UNAVAILABLE_ID)&&!ThaumcraftApi.canCraft(server,owner,Identifier.parse("example:resonant_glass")),"Craft lock with skipped research stays locked for everyone");
            check(ThaumcraftApi.server().vis(new ItemStack(Items.AMETHYST_SHARD))==12&&ThaumcraftApi.server().research(Identifier.parse("thaumcraft2tp:thaumic_restorer")).isPresent(),"Unrelated definitions survive the skip");
            Files.writeString(research,originalResearch);reload(server,selected);
            check(ThaumcraftApi.server().research(project).orElseThrow().steps()==5,"Omitted steps preserves five-step research");
            Files.writeString(research,originalResearch.replace("{","{\"steps\":8,"));reload(server,selected);
            check(ThaumcraftApi.server().research(project).orElseThrow().steps()==8,"Custom steps reaches the public API");
            var researchPos=pos.above();level.setBlockAndUpdate(researchPos,Content.block("quaesitum").defaultBlockState());
            try{
                var researcher=(MachineBlockEntity)level.getBlockEntity(researchPos);
                var customTheory=ThaumcraftApi.server().theory(project).orElseThrow();ItemState.setInt(customTheory,"research_progress",4);
                researcher.setItem(0,customTheory);researcher.setItem(1,new ItemStack(Items.QUARTZ,64));researcher.setItem(2,new ItemStack(Items.QUARTZ,64));researcher.setItem(3,new ItemStack(Items.PAPER,64));
                check(ResearchLogic.odds(researcher).success()>=100,"Supporting items make completion check deterministic");
                researcher.progress=10000;ResearchLogic.tick(level,researcher);
                check(ItemState.getInt(customTheory,"research_progress",0)==5&&!researcher.getItem(0).isEmpty()&&researcher.getItem(9).isEmpty(),"Eight-step theory remains unfinished at five");
                for(int i=0;i<3;i++){researcher.progress=10000;ResearchLogic.tick(level,researcher);}
                check(researcher.getItem(0).isEmpty()&&ResearchItemData.project(researcher.getItem(9)).orElseThrow().key().equals(project),"Eight-step theory completes at eight");
            }finally{level.removeBlock(researchPos,false);}
            var validSteps=AddonData.server();
            for(String invalid:List.of("0","-1","32768","1.5")){
                Files.writeString(research,originalResearch.replace("{","{\"steps\":"+invalid+","));reload(server,selected);
                check(ThaumcraftApi.server().research(project).isEmpty(),"Invalid steps skips the research: "+invalid);
            }
            Files.writeString(research,originalResearch.replace("{","{\"steps\":8,"));reload(server,selected);
            var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),server.registryAccess());
            try{
                var payload=new CatalogSync(AddonData.encode(AddonData.server()));CatalogSync.CODEC.encode(buffer,payload);
                var decoded=CatalogSync.CODEC.decode(buffer);check(decoded.equals(payload),"Complete catalog crosses the network codec");
                AddonData.receive(decoded.json());
                check(ThaumcraftApi.client().research(project).orElseThrow().steps()==8,"Custom steps survives client catalog synchronization");
                check(ThaumcraftApi.client().vis(new ItemStack(Items.AMETHYST_SHARD))==12&&ThaumcraftApi.client().theory(project).isPresent(),"Client view reads the received catalog from any thread");
                check(sharpness(server,ThaumcraftApi.client().infusion(Identifier.parse("example:resonant_blade")).orElseThrow().result())==3,"Result components survive client catalog synchronization");
                check(ThaumcraftApi.client().booster(Blocks.AMETHYST_BLOCK.defaultBlockState()).orElseThrow().researchSpeed()==3,"Booster rules survive client catalog synchronization");
            }finally{buffer.release();}
            check(ThaumcraftApi.revoke(server,owner,project)&&!ThaumcraftApi.knows(server,owner,project)&&!ThaumcraftApi.canCraft(server,owner,Identifier.parse("example:resonant_glass")),"Revoke removes knowledge and relocks its craft");
            check(revoked.equals(List.of(project))&&!ThaumcraftApi.revoke(server,owner,project),"Revoke posts once and is idempotent");
            ThaumcraftApi.unlock(server,owner,project);
            reload(server,original);
            check(ThaumcraftApi.server().research(project).isEmpty()&&GameData.infusions().size()==73,"Removing addon removes its definitions");
            check(ThaumcraftApi.server().restorerCost(new ItemStack(Items.DIAMOND_SWORD))==.25f&&!new ItemStack(Items.DIAMOND).is(dev.thaumcraft.content.ModTags.DUPLICATOR_FORBIDDEN),"Removing addon restores cost defaults and duplicator permission");
            check(ThaumcraftApi.knows(server,owner,project),"Addon removal preserves known research IDs");
            check(ResearchItemData.project(theory).isEmpty(),"Dormant addon theory does not fall back to a built-in project");
            reload(server,selected);check(ResearchItemData.project(theory).isPresent()&&ThaumcraftApi.knows(server,owner,project),"Reinstalling addon restores theory identity and progress");
        }catch(java.io.IOException e){throw new AssertionError(e);}
        finally{
            ThaumcraftEvents.RESEARCH_UNLOCKED.unregister(onUnlock);ThaumcraftEvents.RESEARCH_REVOKED.unregister(onRevoke);
            ThaumcraftEvents.CATALOG_RELOADED.unregister(onReload);ThaumcraftEvents.INFUSION_COMPLETED.unregister(onInfusion);
            AddonData.clearClient();
            reload(server,original);level.removeBlock(pos,false);
            try{if(Files.exists(pack))try(var paths=Files.walk(pack)){for(Path path:paths.sorted(Comparator.reverseOrder()).toList())Files.delete(path);}}catch(java.io.IOException e){throw new AssertionError(e);}
        }
        return checks;
    }
    /** An unrestricted special project normally rolls 1 in 33; a source's special_chance replaces that roll. */
    private static void specialChance(MinecraftServer server,Path pack,List<String> selected,Identifier project,BlockPos pos) throws java.io.IOException {
        Path rule=pack.resolve("data/example/thaumcraft2tp/research_sources/quartz.json");String original=Files.readString(rule);
        var level=server.overworld();var seeker=UUID.randomUUID();
        try{
            check(GameData.researchItem(new ItemStack(Items.QUARTZ)).special_chance()==null,"Omitted special_chance keeps the original odds");
            Files.writeString(rule,"{\"ingredient\":\"minecraft:quartz\",\"value\":100,\"category\":\"example:resonance\",\"special\":[\""+project+"\"],\"special_chance\":100}");reload(server,selected);
            check(GameData.researchItem(new ItemStack(Items.QUARTZ)).special_chance()==100,"special_chance loads from a research source");
            check(!ThaumcraftApi.server().research(project).orElseThrow().restricted(),"Special project stays in the fragment pool");
            ThaumcraftApi.unlock(server,seeker,Identifier.parse("thaumcraft2tp:thaumic_restorer"));
            level.setBlockAndUpdate(pos,Content.block("quaesitum").defaultBlockState());
            var researcher=(MachineBlockEntity)level.getBlockEntity(pos);researcher.setOwner(seeker);
            researcher.setItem(0,new ItemStack(Items.QUARTZ));researcher.setItem(3,new ItemStack(Items.PAPER,64));
            researcher.progress=10000;ResearchLogic.tick(level,researcher);
            boolean found=false;
            for(int slot=MachineBlockEntity.OUTPUT_START;slot<MachineBlockEntity.OUTPUT_END;slot++)found|=ResearchItemData.project(researcher.getItem(slot)).filter(p->p.key().equals(project)).isPresent();
            check(found,"A certain special_chance turns a successful roll into the unrestricted project's theory");
            for(String invalid:List.of("-1","101","1.5","\"often\"")){
                Files.writeString(rule,"{\"ingredient\":\"minecraft:quartz\",\"value\":100,\"special\":[\""+project+"\"],\"special_chance\":"+invalid+"}");reload(server,selected);
                check(ThaumcraftApi.server().researchValue(new ItemStack(Items.QUARTZ))!=100,"Invalid special_chance skips the source: "+invalid);
            }
            Files.writeString(rule,"{\"ingredient\":\"minecraft:quartz\",\"value\":100,\"special_chance\":50}");reload(server,selected);
            check(ThaumcraftApi.server().researchValue(new ItemStack(Items.QUARTZ))!=100,"special_chance without a special list skips the source");
        }finally{level.removeBlock(pos,false);Files.writeString(rule,original);reload(server,selected);}
    }
    private static void accessRuleReloads(MinecraftServer server,Path pack,List<String> selected) throws java.io.IOException {
        Path rule=pack.resolve("data/example/thaumcraft2tp/restorer_costs/diamond_sword.json");String original=Files.readString(rule);
        Path legacy=pack.resolve("data/thaumcraft2tp/thaumcraft2tp/restorer_costs/legacy/flying_carpet.json");Files.createDirectories(legacy.getParent());
        try{
            Files.writeString(rule,"{\"ingredient\":\"minecraft:diamond_sword\",\"cost\":0.75}");reload(server,selected);
            check(ThaumcraftApi.server().restorerCost(new ItemStack(Items.DIAMOND_SWORD))==.75f,"Restorer cost changes on reload");
            for(String invalid:List.of("-1","1e999","\"cheap\"","100001")){
                Files.writeString(legacy,"{\"ingredient\":\"thaumcraft2tp:flying_carpet\",\"cost\":"+invalid+"}");reload(server,selected);
                check(ThaumcraftApi.server().restorerCost(new ItemStack(Content.item("flying_carpet")))==1.25f,"Invalid cost override retains builtin: "+invalid);
            }
            Files.writeString(legacy,"{\"remove\":true}");reload(server,selected);
            check(ThaumcraftApi.server().restorerCost(new ItemStack(Content.item("flying_carpet")))==.25f,"Removing seed uses unmatched default cost");
            Files.writeString(rule,"{\"ingredient\":\"minecraft:diamond_sword\",\"cost\":0}");reload(server,selected);
            check(ThaumcraftApi.server().restorerCost(new ItemStack(Items.DIAMOND_SWORD))==0,"Zero repair cost is valid");
            Files.writeString(rule,"{\"ingredient\":\"minecraft:diamond_sword\",\"cost\":0,\"required_mods\":[\"definitely_absent_mod\"]}");reload(server,selected);
            check(ThaumcraftApi.server().restorerCost(new ItemStack(Items.DIAMOND_SWORD))==.6f,"Absent-mod exact rule is ignored and tag applies");
        }finally{Files.writeString(rule,original);Files.deleteIfExists(legacy);reload(server,selected);}
    }
    private static void localization(MinecraftServer server,Path pack,List<String> selected,Path override) throws java.io.IOException {
        var id=Identifier.parse("example:resonance");
        var project=GameData.findProject(id).orElseThrow();
        check(project.name_key().equals("research.example.resonance.name")&&project.text_key().equals("research.example.resonance.text"),"Explicit addon translation metadata loads");
        check(project.nameComponent().getString().equals(project.name()),"Missing dedicated-server translation uses English fallback");
        check(((net.minecraft.network.chat.contents.TranslatableContents)project.nameComponent().getContents()).getKey().equals(project.name_key()),"API retains a translation key instead of server-rendered text");
        AddonData.receive(AddonData.encode(AddonData.server()));
        check(ThaumcraftApi.client().research(id).orElseThrow().name().equals(project.nameComponent()),"Key and fallback survive catalog serialization");
        var before=AddonData.catalog(true);
        for(String invalid:List.of("\"\"","\"two words\"","\""+"a".repeat(257)+"\"","42","null","true")){
            String json=AddonData.encode(AddonData.server()).replace("\"name_key\":\"research.example.resonance.name\"","\"name_key\":"+invalid);
            try{AddonData.receive(json);check(false,"Malformed received key rejected: "+invalid);}catch(RuntimeException expected){checks++;}
            check(AddonData.catalog(true)==before,"Rejected translation metadata leaves client snapshot intact");
        }
        var oldItem=ThaumcraftApi.server().discovery(id).orElseThrow();
        oldItem.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.translatableWithFallback("item.thaumcraft2tp.addon_discovery","Discovery: %s",net.minecraft.network.chat.Component.translatableWithFallback(project.name(),project.name())));
        ResearchItemData.refreshName(oldItem);
        check(oldItem.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME).equals(ThaumcraftApi.server().discovery(id).orElseThrow().get(net.minecraft.core.component.DataComponents.CUSTOM_NAME)),"Old generated item name picks up stable research key");
        var renamed=oldItem.copy();var custom=net.minecraft.network.chat.Component.literal("My discovery");renamed.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,custom);
        ResearchItemData.refreshName(renamed);check(renamed.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME).equals(custom),"Player-renamed research item remains untouched");
        Path research=pack.resolve("data/example/thaumcraft2tp/research/resonance.json");String original=Files.readString(research);
        try{
            var legacy=com.google.gson.JsonParser.parseString(original).getAsJsonObject();legacy.remove("name_key");legacy.remove("text_key");
            Files.writeString(research,legacy.toString());reload(server,selected);
            check(GameData.findProject(id).orElseThrow().name_key()==null&&ThaumcraftApi.server().research(id).orElseThrow().name().getString().equals(project.name()),"Legacy literal addon remains readable");
            legacy.addProperty("name","research.example.legacy_key");Files.writeString(research,legacy.toString());reload(server,selected);
            check(((net.minecraft.network.chat.contents.TranslatableContents)ThaumcraftApi.server().research(id).orElseThrow().name().getContents()).getKey().equals("research.example.legacy_key"),"Legacy key-in-name addon keeps its translation contract");
            Files.writeString(override,"{\"name\":\"Pack Restorer\",\"category\":\"thaumcraft2tp:lost\"}");reload(server,selected);
            var restorer=GameData.findProject(Identifier.parse("thaumcraft2tp:thaumic_restorer")).orElseThrow();
            check(restorer.name_key()==null&&restorer.nameComponent().getString().equals("Pack Restorer"),"Literal built-in override does not inherit the built-in translation key");
            for(String invalid:List.of("\"\"","\"two words\"","42","null")){
                var broken=com.google.gson.JsonParser.parseString(original).getAsJsonObject();broken.add("name_key",com.google.gson.JsonParser.parseString(invalid));
                Files.writeString(research,broken.toString());reload(server,selected);
                check(ThaumcraftApi.server().research(id).isEmpty(),"Invalid data-pack translation key skips its project: "+invalid);
            }
        }finally{Files.writeString(research,original);Files.deleteIfExists(override);reload(server,selected);}
    }
    private static void crucibleEvent(MinecraftServer server){
        var level=server.overworld();var pos=new BlockPos(310,280,310);var owner=UUID.randomUUID();
        var seen=new ArrayList<Object[]>();
        ThaumcraftEvents.CrucibleDissolved onDissolved=(l,at,player,dissolved,vis)->{if(at.equals(pos))seen.add(new Object[]{player,dissolved,vis});};
        var thrower=new net.minecraft.server.level.ServerPlayer(server,level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"CrucibleThrower"),net.minecraft.server.level.ClientInformation.createDefault());
        ThaumcraftEvents.CrucibleDissolving doubling=(l,at,player,item,vis)->at.equals(pos)?vis*2:vis,refusing=(l,at,player,item,vis)->at.equals(pos)?0:vis;
        ThaumcraftEvents.CRUCIBLE_DISSOLVED.register(onDissolved);
        try{
            level.getChunkAt(pos);level.setBlockAndUpdate(pos,Content.block("crucible").defaultBlockState());
            var crucible=(MachineBlockEntity)level.getBlockEntity(pos);
            java.util.function.Function<net.minecraft.world.entity.Entity,net.minecraft.world.entity.item.ItemEntity> drop=by->{
                var item=new net.minecraft.world.entity.item.ItemEntity(level,pos.getX()+.5,pos.getY()+.6,pos.getZ()+.5,new ItemStack(Items.COBBLESTONE,2));
                if(by!=null)item.setThrower(by);level.addFreshEntity(item);return item;
            };
            // A dissolved item delays the next one, so tick until something happens or the delay must have lapsed.
            Runnable tick=()->{int before=seen.size();for(int i=0;i<200&&seen.size()==before;i++)dev.thaumcraft.machine.MachineLogic.crucible(level,crucible);};
            float value=ThaumcraftApi.server().vis(new ItemStack(Items.COBBLESTONE));
            var ownerless=drop.apply(null);tick.run();ownerless.discard();
            check(seen.size()==1&&seen.get(0)[0]==null,"Ownerless crucible credits nobody for hopper-style input");
            check(((ItemStack)seen.get(0)[1]).is(Items.COBBLESTONE)&&((ItemStack)seen.get(0)[1]).getCount()==1&&(float)seen.get(0)[2]==value,"Dissolve event reports one item and its full vis value");
            crucible.setOwner(owner);
            var unthrown=drop.apply(null);tick.run();unthrown.discard();
            check(seen.size()==2&&owner.equals(seen.get(1)[0]),"Unthrown item credits the crucible owner");
            var thrown=drop.apply(thrower);tick.run();thrown.discard();
            check(seen.size()==3&&thrower.getUUID().equals(seen.get(2)[0]),"Thrown item credits its thrower over the owner");
            var rejected=new net.minecraft.world.entity.item.ItemEntity(level,pos.getX()+.5,pos.getY()+.6,pos.getZ()+.5,new ItemStack(Items.BEDROCK));level.addFreshEntity(rejected);
            tick.run();rejected.discard();
            check(seen.size()==3,"Rejected item posts no dissolve event");
            float before=crucible.totalVis();
            ThaumcraftEvents.CRUCIBLE_DISSOLVING.register(doubling);
            var doubled=drop.apply(null);tick.run();doubled.discard();
            check(seen.size()==4&&(float)seen.get(3)[2]==value*2&&Math.abs(crucible.totalVis()-before-value*2)<.001f,"Dissolving listener changes the dissolved value");
            ThaumcraftEvents.CRUCIBLE_DISSOLVING.register(refusing);
            var refused=drop.apply(null);tick.run();
            check(seen.size()==4&&refused.isAlive()&&refused.getItem().getCount()==2,"Zero value from a listener rejects the item");
            refused.discard();
        }finally{
            ThaumcraftEvents.CRUCIBLE_DISSOLVED.unregister(onDissolved);ThaumcraftEvents.CRUCIBLE_DISSOLVING.unregister(doubling);ThaumcraftEvents.CRUCIBLE_DISSOLVING.unregister(refusing);
            level.removeBlock(pos,false);
        }
    }
    /** A minimal pure-vis container adapted onto jukeboxes at test positions. */
    private static final class TestContainer implements VisContainer {
        float pure;final int suction;
        TestContainer(float pure,int suction){this.pure=pure;this.suction=suction;}
        @Override public boolean connects(Direction side){return true;}
        @Override public float vis(boolean tainted){return tainted?0:pure;}
        @Override public float capacity(){return 100;}
        @Override public int suction(boolean tainted){return tainted?0:suction;}
        @Override public float extract(float amount,boolean tainted){if(tainted)return 0;float taken=Math.min(amount,pure);pure-=taken;return taken;}
        @Override public float insert(float amount,boolean tainted){if(tainted)return 0;float accepted=Math.min(amount,100-pure);pure+=accepted;return accepted;}
    }
    private static final Map<BlockPos,TestContainer> CONTAINERS=new java.util.concurrent.ConcurrentHashMap<>();
    private static boolean adapterRegistered;
    private static MachineBlockEntity machine(net.minecraft.server.level.ServerLevel level,BlockPos pos,String id){
        level.getChunkAt(pos);level.setBlockAndUpdate(pos,Content.block(id).defaultBlockState());return (MachineBlockEntity)level.getBlockEntity(pos);
    }
    private static void visContainers(MinecraftServer server){
        var level=server.overworld();var base=new BlockPos(320,280,320);
        if(!adapterRegistered){ThaumcraftVis.register(BlockEntityType.JUKEBOX,jukebox->CONTAINERS.get(jukebox.getBlockPos()));adapterRegistered=true;}
        boolean duplicate=false;
        try{ThaumcraftVis.register(BlockEntityType.JUKEBOX,jukebox->null);}catch(IllegalArgumentException expected){duplicate=true;}
        check(duplicate,"A block entity type takes one vis adapter");
        var placed=new ArrayList<BlockPos>();
        try{
            // Source, conduit, tank: the tank's suction draws addon vis through the conduit.
            var source=new TestContainer(10,0);CONTAINERS.put(base,source);level.getChunkAt(base);level.setBlockAndUpdate(base,Blocks.JUKEBOX.defaultBlockState());placed.add(base);
            var conduit=machine(level,base.east(),"vis_conduit");var tank=machine(level,base.east(2),"vis_storage_tank");placed.add(base.east());placed.add(base.east(2));
            check(MachineConnections.connected(level,conduit.getBlockPos(),conduit.getBlockState(),Direction.WEST),"Conduit connects to an adapted addon container");
            check(!MachineConnections.connected(level,conduit.getBlockPos(),conduit.getBlockState(),Direction.UP),"Conduit ignores blocks without a container");
            for(int i=0;i<20;i++){VisNetwork.tick(level,conduit);VisNetwork.tick(level,tank);}
            check(source.pure<10&&tank.pureVis()>0&&Math.abs(source.pure+conduit.pureVis()+tank.pureVis()-10)<.001f,"Network extracts addon source vis without creating or losing any");
            // Tank, conduit, consumer: the consumer's suction brings vis to its conduit, then it pulls.
            var row=base.south(3);
            var tank2=machine(level,row,"vis_storage_tank");var conduit2=machine(level,row.east(),"vis_conduit");placed.add(row);placed.add(row.east());
            var consumer=new TestContainer(0,50);CONTAINERS.put(row.east(2),consumer);level.setBlockAndUpdate(row.east(2),Blocks.JUKEBOX.defaultBlockState());placed.add(row.east(2));
            tank2.insertVis(20,false);VisNetwork.tick(level,conduit2);
            check(conduit2.visSuction()==49&&Math.abs(conduit2.pureVis()-4)<.001f,"Addon consumer suction propagates through conduits");
            float pulled=ThaumcraftVis.pull(level,row.east(2),3,false);
            check(Math.abs(pulled-3)<.001f&&Math.abs(conduit2.pureVis()-1)<.001f,"Addon consumer pulls from its connected conduit");
            check(ThaumcraftVis.pull(level,row.east(2),3,true)==0,"Pull of an absent kind gives nothing");
            check(ThaumcraftVis.pull(level,row.above(5),3,false)==0,"Pull without a container at the position gives nothing");
            var view=ThaumcraftVis.container(level,row).orElseThrow();
            check(Math.abs(view.insert(5,false)-5)<.001f&&Math.abs(view.extract(2,false)-2)<.001f&&view.capacity()==500,"Thaumcraft tanks accept addon insert and extract");
            var infuser=machine(level,row.south(3),"thaumic_infuser");placed.add(row.south(3));
            var infuserView=ThaumcraftVis.container(level,infuser.getBlockPos()).orElseThrow();
            check(infuserView.insert(5,false)==0&&infuser.pureVis()==0,"Consumer machines refuse direct insertion");
            level.setBlockAndUpdate(row.south(6),Blocks.JUKEBOX.defaultBlockState());placed.add(row.south(6));
            check(ThaumcraftVis.container(level,row.south(6)).isEmpty(),"An adapter may decline an entity");
        }finally{for(var pos:placed)level.removeBlock(pos,false);CONTAINERS.clear();}
    }
    private static void taintVeto(MinecraftServer server){
        var level=server.overworld();var pos=new BlockPos(330,280,330);
        ThaumcraftEvents.TaintSpreading deny=(l,at)->!at.equals(pos);
        ThaumcraftEvents.TAINT_SPREADING.register(deny);
        try{
            level.getChunkAt(pos);level.setBlockAndUpdate(pos,Blocks.DIRT.defaultBlockState());
            check(!dev.thaumcraft.content.TaintBlock.taint(level,pos)&&level.getBlockState(pos).is(Blocks.DIRT),"Denied taint leaves the block unchanged");
            ThaumcraftEvents.TAINT_SPREADING.unregister(deny);
            check(dev.thaumcraft.content.TaintBlock.taint(level,pos)&&level.getBlockState(pos).is(Content.block("tainted_soil")),"Allowed taint converts the block");
        }finally{ThaumcraftEvents.TAINT_SPREADING.unregister(deny);level.removeBlock(pos,false);dev.thaumcraft.world.TaintMemory.get(level).forget(pos);}
    }
    private static int sharpness(MinecraftServer server,ItemStack stack){
        var enchantment=server.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS);
        return net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(enchantment,stack);
    }
    /** Booster, taint, component, goggles, and aura rules from the example pack. Runs while the pack is enabled. */
    private static void catalogRules(MinecraftServer server){
        var level=server.overworld();var catalog=ThaumcraftApi.server();
        var amethyst=catalog.booster(Blocks.AMETHYST_BLOCK.defaultBlockState()).orElseThrow();
        check(amethyst.enchanting()==2&&amethyst.researchSpeed()==3&&amethyst.researchBonus()==1&&amethyst.failureProtection()==0,"Data pack booster reaches the catalog with defaults for omitted fields");
        var shelf=catalog.booster(Blocks.BOOKSHELF.defaultBlockState()).orElseThrow();var brain=catalog.booster(Content.block("brain_in_a_jar").defaultBlockState()).orElseThrow();
        check(shelf.enchanting()==1&&shelf.researchSpeed()==2&&shelf.researchBonus()==1&&shelf.failureProtection()==.25,"Bookshelf keeps its original weights with a quarter of a brain's protection");
        check(brain.enchanting()==4&&brain.researchSpeed()==4&&brain.researchBonus()==2&&brain.failureProtection()==1,"Brain in a jar keeps its original weights");
        check(catalog.booster(Blocks.STONE.defaultBlockState()).isEmpty(),"Ordinary blocks do not boost");
        var origin=new BlockPos(340,280,340);var placed=List.of(origin.east(2),origin.east(2).above(),origin.west(2));
        try{
            level.getChunkAt(origin);
            level.setBlockAndUpdate(placed.get(0),Blocks.AMETHYST_BLOCK.defaultBlockState());level.setBlockAndUpdate(placed.get(1),Blocks.BOOKSHELF.defaultBlockState());level.setBlockAndUpdate(placed.get(2),Content.block("brain_in_a_jar").defaultBlockState());
            check(dev.thaumcraft.machine.EnchantingBoosters.count(level,origin).equals(new dev.thaumcraft.machine.EnchantingBoosters.Totals(7,9,4,1)),"Booster totals sum every rule in the ring");
            level.setBlockAndUpdate(origin.east(),Blocks.GLASS.defaultBlockState());
            check(dev.thaumcraft.machine.EnchantingBoosters.count(level,origin).equals(new dev.thaumcraft.machine.EnchantingBoosters.Totals(4,4,2,1)),"A blocked gap hides addon boosters like bookshelves");
        }finally{for(var pos:placed)level.removeBlock(pos,false);level.removeBlock(origin.east(),false);}

        var pos=new BlockPos(345,280,345);level.getChunkAt(pos);
        try{
            level.setBlockAndUpdate(pos,Blocks.TERRACOTTA.defaultBlockState());
            check(dev.thaumcraft.api.ThaumcraftTaint.taint(level,pos)&&level.getBlockState(pos).is(Content.block("tainted_clay"))&&level.getBlockState(pos).getValue(dev.thaumcraft.content.TaintBlock.MATERIAL)==15,"Addon taint rule converts a tagged block with its material");
            check(dev.thaumcraft.world.TaintMemory.get(level).original(pos).orElseThrow().is(Blocks.TERRACOTTA),"Addon taint conversion remembers the original");
            check(dev.thaumcraft.api.ThaumcraftTaint.purify(level,pos)&&level.getBlockState(pos).is(Blocks.TERRACOTTA),"Purifying an addon conversion restores the exact original");
            level.setBlockAndUpdate(pos,Blocks.MYCELIUM.defaultBlockState());
            check(!dev.thaumcraft.api.ThaumcraftTaint.taint(level,pos)&&level.getBlockState(pos).is(Blocks.MYCELIUM),"Taint-immune tag blocks a built-in conversion");
            check(dev.thaumcraft.content.TaintBlock.taint(Blocks.BRICKS.defaultBlockState())==null,"Taint rule with an invalid material is skipped");
            level.setBlockAndUpdate(pos,Blocks.GLASS.defaultBlockState());
            check(!dev.thaumcraft.api.ThaumcraftTaint.purify(level,pos),"Purify ignores untainted blocks");
            var mooshroom=net.minecraft.world.entity.EntityType.MOOSHROOM.create(level,net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            mooshroom.snapTo(pos.getX()+.5,pos.getY()+1,pos.getZ()+.5,0,0);level.addFreshEntity(mooshroom);
            check(dev.thaumcraft.api.ThaumcraftTaint.taintEntity(level,mooshroom)&&mooshroom.isRemoved(),"Addon entity rule converts its mob");
            var converted=level.getEntities(dev.thaumcraft.entity.ModEntities.TYPES.get("tainted_cow"),new net.minecraft.world.phys.AABB(pos).inflate(2),entity->true);
            check(converted.size()==1,"Addon entity rule spawns its tainted form");converted.forEach(net.minecraft.world.entity.Entity::discard);
            var player=new net.minecraft.server.level.ServerPlayer(server,level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"TaintTarget"),net.minecraft.server.level.ClientInformation.createDefault());
            check(!dev.thaumcraft.api.ThaumcraftTaint.taintEntity(level,player),"Players are never converted");
            var helmet=new ItemStack(Items.TURTLE_HELMET);
            check(helmet.is(dev.thaumcraft.item.ArcanaItem.REVEALS_AURA)&&new ItemStack(Content.item("goggles_of_revealing")).is(dev.thaumcraft.item.ArcanaItem.REVEALS_AURA),"Aura-revealing tag holds the goggles and pack additions");
            dev.thaumcraft.item.ArcanaItem.recordAura(helmet,level,player);
            check(ItemState.tag(helmet).contains("thaumcraft_aura_vis"),"Tagged helmets carry the aura for the HUD");
        }finally{level.removeBlock(pos,false);dev.thaumcraft.world.TaintMemory.get(level).forget(pos);}

        var auraPos=new BlockPos(350,280,350);var before=ThaumcraftVis.aura(level,auraPos);
        float added=ThaumcraftVis.addAura(level,auraPos,5,true);
        check(Math.abs(added-Math.min(5,before.max()-before.taint()))<.001f&&Math.abs(ThaumcraftVis.aura(level,auraPos).taint()-before.taint()-added)<.001f,"Adding aura taint reports the capped amount");
        check(Math.abs(ThaumcraftVis.drainAura(level,auraPos,added,true)-added)<.001f&&Math.abs(ThaumcraftVis.aura(level,auraPos).taint()-before.taint())<.001f,"Draining returns the aura to its prior taint");
        check(ThaumcraftVis.addAura(level,auraPos,-1,false)==0&&ThaumcraftVis.addAura(level,auraPos,Float.NaN,false)==0,"Invalid aura amounts change nothing");

        // Addon entries come first (priority 0 before the built-in -100,000), so an always-zero random picks them.
        var zero=new net.minecraft.world.level.levelgen.LegacyRandomSource(0){@Override public int nextInt(int bound){return 0;}};
        var treasure=new net.minecraft.world.SimpleContainer(3);dev.thaumcraft.gameplay.ArtifactLoot.fillOriginalTreasure(treasure,zero);
        check(treasure.getItem(0).is(Items.AMETHYST_SHARD)&&treasure.getItem(0).getCount()==2,"Treasure contribution joins the chest pool with its count range");
        var pool=AddonData.server().treasure().get("thaumcraft2tp:chest");
        check(pool.fillOneIn()==3&&pool.entries().size()==9&&pool.entries().stream().noneMatch(e->e.items().stream().anyMatch(i->i.oneOf().contains("minecraft:stone"))),"Invalid treasure files are skipped and the pool keeps its odds");
        var eldritch=new net.minecraft.world.SimpleContainer(27);dev.thaumcraft.gameplay.ArtifactLoot.fillOriginalEldritch(eldritch,zero);
        check(eldritch.isEmpty()&&AddonData.server().treasure().get("thaumcraft2tp:eldritch").entries().isEmpty(),"Removing the built-in eldritch pool leaves void chests empty");
        var blade=catalog.infusion(Identifier.parse("example:resonant_blade")).orElseThrow().result();
        check(blade.is(Items.GOLDEN_SWORD)&&sharpness(server,blade)==3&&blade.get(net.minecraft.core.component.DataComponents.RARITY)==net.minecraft.world.item.Rarity.RARE,"Infusion result carries its data components");
        check(sharpness(server,catalog.infusion(Identifier.parse("example:resonant_blade")).orElseThrow().result())==3,"Each result is a fresh copy with components");
        check(catalog.infusion(Identifier.parse("example:bad_components")).isEmpty(),"Recipe with invalid components is skipped");
    }
    /** The hardcoded implementation that the data-driven pools replaced, kept verbatim as the reference. */
    private static final class OriginalTreasure {
        static void treasure(net.minecraft.world.Container chest,net.minecraft.util.RandomSource random){
            var choices=new ArrayList<ItemStack>();
            for(String type:List.of("ItemElementalSwordAir","ItemElementalAxeWater","ItemElementalPickFire","ItemElementalShovelEarth","ItemElementalHoeMagic","ItemElementalBowBone"))choices.add(legacy(type,0,1));
            for(int i=0;i<15;i++)choices.add(legacy("ItemComponents",6,1+random.nextInt(3)));
            for(int repeats=0;repeats<50;repeats++)for(int meta=0;meta<2;meta++){choices.add(legacy("ItemArtifactLost",meta,1));choices.add(legacy("ItemArtifactForbidden",meta,1));}
            for(int repeats=0;repeats<25;repeats++)for(int meta=2;meta<4;meta++){choices.add(legacy("ItemArtifactLost",meta,1));choices.add(legacy("ItemArtifactForbidden",meta,1));}
            for(int repeats=0;repeats<12;repeats++){choices.add(legacy("ItemArtifactLost",4,1));choices.add(legacy("ItemArtifactForbidden",4,1));}
            for(int repeats=0;repeats<6;repeats++){choices.add(legacy("ItemArtifactLost",5,1));choices.add(legacy("ItemArtifactForbidden",5,1));}
            for(int i=0;i<3;i++)choices.add(legacy("ItemArtifactEldritch",random.nextInt(2),1));
            choices.add(legacy("ItemArtifactEldritch",2+random.nextInt(2),1));
            for(int slot=0;slot<chest.getContainerSize();slot++)if(chest.getItem(slot).isEmpty()){
                int roll=random.nextInt(choices.size()*3);if(roll<choices.size())chest.setItem(slot,choices.get(roll).copy());
            }
        }
        static void eldritch(net.minecraft.world.Container chest,net.minecraft.util.RandomSource random){
            var choices=new ArrayList<ItemStack>();
            for(int i=0;i<100;i++)for(int meta=0;meta<2;meta++)choices.add(legacy("ItemArtifactEldritch",meta,1));
            for(int i=0;i<33;i++)for(int meta=2;meta<4;meta++)choices.add(legacy("ItemArtifactEldritch",meta,1));
            for(int i=0;i<11;i++)choices.add(legacy("ItemArtifactEldritch",4,1));
            for(int i=0;i<5;i++)choices.add(legacy("ItemArtifactEldritch",5,1));
            for(int i=0;i<17;i++)choices.add(legacy("ItemComponents",8,1+random.nextInt(2)));
            for(int i=0;i<17;i++)choices.add(legacy("ItemComponents",11,1+random.nextInt(2)));
            for(int i=0;i<25;i++)choices.add(new ItemStack(Content.item("eldritch_stone"),1+random.nextInt(4)));
            for(int slot=0;slot<chest.getContainerSize();slot++)if(chest.getItem(slot).isEmpty()){
                int roll=random.nextInt(choices.size()*6);if(roll<choices.size())chest.setItem(slot,choices.get(roll).copy());
            }
        }
        private static ItemStack legacy(String type,int meta,int count){
            var entry=Content.ENTRIES.stream().filter(e->e.source_class().equals(type)&&e.meta()==meta).findFirst().orElseThrow();
            return new ItemStack(Content.item(entry.id()),count);
        }
    }
    /** The built-in pools must fill exactly as the hardcoded lists did, slot for slot, for every seed. */
    private static void treasureIdentity(){
        var prefill=new ItemStack(Items.EMERALD,9);
        for(int size:new int[]{1,27,54})for(long seed=0;seed<400;seed++)for(boolean eldritch:new boolean[]{false,true}){
            var expected=new net.minecraft.world.SimpleContainer(size);var actual=new net.minecraft.world.SimpleContainer(size);
            if(seed%3==0){expected.setItem(0,prefill.copy());actual.setItem(0,prefill.copy());}
            var expectedRandom=net.minecraft.util.RandomSource.create(seed);var actualRandom=net.minecraft.util.RandomSource.create(seed);
            if(eldritch){OriginalTreasure.eldritch(expected,expectedRandom);dev.thaumcraft.gameplay.ArtifactLoot.fillOriginalEldritch(actual,actualRandom);}
            else{OriginalTreasure.treasure(expected,expectedRandom);dev.thaumcraft.gameplay.ArtifactLoot.fillOriginalTreasure(actual,actualRandom);}
            boolean same=expectedRandom.nextLong()==actualRandom.nextLong();
            for(int slot=0;slot<size;slot++)same&=ItemStack.matches(expected.getItem(slot),actual.getItem(slot));
            if(!same)throw new AssertionError("Treasure differs from the original for seed "+seed+", size "+size+(eldritch?", eldritch":""));
        }
        check(true,"Built-in treasure pools reproduce the original contents and random sequence");
    }
    private static void blockRemoving(MinecraftServer server){
        var level=server.overworld();var pos=new BlockPos(360,280,360);level.getChunkAt(pos);
        var asked=new ArrayList<UUID>();var player=UUID.randomUUID();
        ThaumcraftEvents.BlockRemoving deny=(l,at,state,actor)->{asked.add(actor);return false;};
        ThaumcraftEvents.BLOCK_REMOVING.register(deny);
        try{
            for(int y=0;y<2;y++)for(int x=0;x<3;x++)level.setBlockAndUpdate(pos.offset(x,y,0),Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(pos.offset(3,0,0),Blocks.AIR.defaultBlockState());level.setBlockAndUpdate(pos.offset(3,1,0),Blocks.AIR.defaultBlockState());
            var space=dev.thaumcraft.world.TemporarySpace.get(level);
            check(space.open(level,pos,Direction.EAST,32,player)==0&&level.getBlockState(pos).is(Blocks.STONE)&&asked.equals(List.of(player)),"Denied portable hole leaves the wall and reports its user");
            ThaumcraftEvents.BLOCK_REMOVING.unregister(deny);
            check(space.open(level,pos,Direction.EAST,32,player)==3,"Allowed portable hole opens");
        }finally{
            ThaumcraftEvents.BLOCK_REMOVING.unregister(deny);
            for(int y=0;y<2;y++)for(int x=0;x<4;x++)level.removeBlock(pos.offset(x,y,0),false);
        }
        // Bore: a veto skips every candidate, so a full wall survives a mining cycle.
        var borePos=new BlockPos(370,280,370);level.getChunkAt(borePos);
        level.setBlockAndUpdate(borePos,Content.block("arcane_bore").defaultBlockState().setValue(dev.thaumcraft.machine.MachineBlock.FACING,Direction.EAST));
        var bore=(MachineBlockEntity)level.getBlockEntity(borePos);level.setBlockAndUpdate(borePos.below(),Blocks.REDSTONE_BLOCK.defaultBlockState());
        Runnable wall=()->{for(int y=-4;y<=4;y++)for(int z=-4;z<=4;z++)level.setBlockAndUpdate(borePos.offset(2,y,z),Blocks.STONE.defaultBlockState());};
        java.util.function.IntSupplier stone=()->{int n=0;for(int y=-4;y<=4;y++)for(int z=-4;z<=4;z++)if(level.getBlockState(borePos.offset(2,y,z)).is(Blocks.STONE))n++;return n;};
        asked.clear();ThaumcraftEvents.BLOCK_REMOVING.register(deny);
        try{
            bore.setItem(0,new ItemStack(Content.item("arcane_focus")));bore.restoreEnergy(100);bore.progress=4;wall.run();
            dev.thaumcraft.machine.MachineLogic.bore(level,bore);
            check(stone.getAsInt()==81&&!asked.isEmpty()&&asked.stream().allMatch(java.util.Objects::isNull),"Denied bore mines nothing and reports its missing owner");
            ThaumcraftEvents.BLOCK_REMOVING.unregister(deny);
            bore.restoreEnergy(100);bore.progress=4;dev.thaumcraft.machine.MachineLogic.bore(level,bore);
            check(stone.getAsInt()==80,"Allowed bore mines one block");
        }finally{
            ThaumcraftEvents.BLOCK_REMOVING.unregister(deny);
            for(int y=-4;y<=4;y++)for(int z=-4;z<=4;z++)level.removeBlock(borePos.offset(2,y,z),false);
            level.removeBlock(borePos,false);level.removeBlock(borePos.below(),false);
            level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(borePos).inflate(16)).forEach(net.minecraft.world.entity.Entity::discard);
        }
    }
    /** Exclusion tags from the example pack. Runs while the pack is enabled. */
    private static void exclusionTags(MinecraftServer server){
        var level=server.overworld();var pos=new BlockPos(380,280,380);level.getChunkAt(pos);
        try{
            for(int x=0;x<2;x++)for(int y=0;y<2;y++)level.setBlockAndUpdate(pos.offset(x,y,0),x==0?Blocks.SPAWNER.defaultBlockState():Blocks.STONE.defaultBlockState());
            check(dev.thaumcraft.world.TemporarySpace.get(level).open(level,pos,Direction.EAST,32)==0&&level.getBlockState(pos).is(Blocks.SPAWNER),"Portable hole refuses tagged blocks");
        }finally{for(int x=0;x<2;x++)for(int y=0;y<2;y++)level.removeBlock(pos.offset(x,y,0),false);}
        var borePos=pos.south(20);
        level.setBlockAndUpdate(borePos,Content.block("arcane_bore").defaultBlockState().setValue(dev.thaumcraft.machine.MachineBlock.FACING,Direction.EAST));
        var bore=(MachineBlockEntity)level.getBlockEntity(borePos);level.setBlockAndUpdate(borePos.below(),Blocks.REDSTONE_BLOCK.defaultBlockState());
        try{
            for(int y=-4;y<=4;y++)for(int z=-4;z<=4;z++)level.setBlockAndUpdate(borePos.offset(2,y,z),Blocks.SPAWNER.defaultBlockState());
            bore.setItem(0,new ItemStack(Content.item("arcane_focus")));
            for(int i=0;i<5;i++){bore.restoreEnergy(100);bore.progress=4;dev.thaumcraft.machine.MachineLogic.bore(level,bore);}
            boolean intact=true;for(int y=-4;y<=4;y++)for(int z=-4;z<=4;z++)intact&=level.getBlockState(borePos.offset(2,y,z)).is(Blocks.SPAWNER);
            check(intact,"Bore skips tagged blocks");
        }finally{
            for(int y=-4;y<=4;y++)for(int z=-4;z<=4;z++)level.removeBlock(borePos.offset(2,y,z),false);
            level.removeBlock(borePos,false);level.removeBlock(borePos.below(),false);
        }
        var soulsPos=pos.south(40);level.setBlockAndUpdate(soulsPos,Content.block("crucible_of_souls").defaultBlockState());
        var souls=(MachineBlockEntity)level.getBlockEntity(soulsPos);
        var villager=net.minecraft.world.entity.EntityType.VILLAGER.create(level,net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        var zombie=net.minecraft.world.entity.EntityType.ZOMBIE.create(level,net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        try{
            villager.snapTo(soulsPos.getX()+2.5,soulsPos.getY(),soulsPos.getZ()+.5,0,0);zombie.snapTo(soulsPos.getX()-1.5,soulsPos.getY(),soulsPos.getZ()+.5,0,0);
            villager.setNoAi(true);zombie.setNoAi(true);level.addFreshEntity(villager);level.addFreshEntity(zombie);
            for(int i=0;i<40&&!zombie.hasEffect(net.minecraft.world.effect.MobEffects.HUNGER);i++)dev.thaumcraft.machine.MachineLogic.crucible(level,souls);
            check(zombie.hasEffect(net.minecraft.world.effect.MobEffects.HUNGER)&&!villager.hasEffect(net.minecraft.world.effect.MobEffects.HUNGER)&&villager.getHealth()==villager.getMaxHealth(),"Crucible of Souls ignores tagged mobs");
        }finally{villager.discard();zombie.discard();level.removeBlock(soulsPos,false);}
        var trunk=dev.thaumcraft.entity.ModEntities.TRUNK.create(level,net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        check(!trunk.canPlaceItem(0,new ItemStack(Items.SHULKER_BOX))&&trunk.canPlaceItem(0,new ItemStack(Items.STONE)),"Trunk refuses tagged items");
        var menu=new dev.thaumcraft.machine.TrunkMenu(0,new net.minecraft.server.level.ServerPlayer(server,level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"TrunkUser"),net.minecraft.server.level.ClientInformation.createDefault()).getInventory(),trunk,3);
        check(!menu.getSlot(0).mayPlace(new ItemStack(Items.SHULKER_BOX))&&menu.getSlot(0).mayPlace(new ItemStack(Items.STONE)),"Trunk slots refuse tagged items");
        trunk.discard();
    }
    private static void reload(MinecraftServer server,List<String> selected){var future=server.reloadResources(selected);server.managedBlock(future::isDone);future.join();}
}
