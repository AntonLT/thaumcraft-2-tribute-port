package dev.alts.tc2tppatches;

import com.mojang.authlib.GameProfile;
import dev.thaumcraft.api.ThaumcraftApi;
import dev.thaumcraft.content.Content;
import dev.thaumcraft.gameplay.AddonData;
import dev.thaumcraft.gameplay.ArcaneWorldData;
import dev.thaumcraft.gameplay.GameData;
import dev.thaumcraft.gameplay.ResearchGate;
import dev.thaumcraft.gameplay.ResearchLogic;
import dev.thaumcraft.machine.MachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CrafterBlock;
import net.minecraft.world.level.block.entity.CrafterBlockEntity;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Checks the bundled research gates through loaded catalogs and actual crafting/discovery entry points. */
final class ResearchGatingSmokeTests {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError("Research gating: "+message);}
    private static Identifier research(String path){return Identifier.fromNamespaceAndPath("research_gating",path);}
    private static final Map<String,String> CRAFTS=Map.of("arcane_tinkering_tool","base_014","arcane_bore","base_026",
            "vis_filter","base_032","arcane_bellows","base_034","vis_pump","base_035");
    private static final List<String> PROJECTS=List.of("nitor","alumentum","enchanted_fabric","enchanted_silverwood",
            "arcane_singularity","portable_hole","runic_essence_air","runic_essence_water","runic_essence_earth",
            "runic_essence_fire","runic_essence_magic","runic_essence_dark","extract_of_lightest_air","extract_of_coolest_water",
            "extract_of_deepest_earth","extract_of_warmest_fire","extract_of_purest_magic","extract_of_foulest_taint",
            "arcane_seal","animated_piston","arcane_furnace","thaumometer","boots_of_striding","wand_of_lightning",
            "wand_of_water","wand_of_equal_trade","wand_of_fire","arcane_tinkering_tool","arcane_bellows",
            "arcane_bore","vis_filter","vis_pump");
    static int run(MinecraftServer server){
        checks=0;
        var level=server.overworld();var data=ArcaneWorldData.researchData(level);
        var player=new ServerPlayer(server,level,new GameProfile(UUID.randomUUID(),"GatingSmoke"),ClientInformation.createDefault());
        var result=new ResultContainer();var pos=new BlockPos(340,280,340);level.getChunkAt(pos);
        check(GameData.projects().stream().filter(p->p.key().getNamespace().equals("research_gating")).count()==32,"Jar supplies all 32 new research projects");
        check(ThaumcraftApi.server().research(research("arcane_focus")).isEmpty(),"Default Focus has no separate research");
        try{
            for(String name:PROJECTS){
                var id=research(name);var project=ThaumcraftApi.server().research(id).orElseThrow();
                String category=Set.of("runic_essence_dark","extract_of_foulest_taint").contains(name)?"tainted":"lost";
                check(project.difficulty()==0&&project.steps()==5&&!project.restricted()&&project.category().equals(Identifier.parse("thaumcraft2tp:"+category)),"Planned research settings for "+name);
                var recipe=Identifier.parse("thaumcraft2tp:"+(CRAFTS.containsKey(name)?CRAFTS.get(name):"infusion/"+(name.equals("arcane_seal")?"arcane_seal_item":name)));
                check(ThaumcraftApi.server().requiredResearch(recipe).orElseThrow().equals(id),"Recipe requires its own research: "+recipe);
                check(!ThaumcraftApi.canCraft(server,player.getUUID(),recipe)&&!ThaumcraftApi.canCraft(server,null,recipe),"Unresearched player and ownerless automation are blocked: "+recipe);
                check(ResearchGate.locked(Set.of(),recipe),"Client gate hides unlearned recipe: "+recipe);
                if(CRAFTS.containsKey(name)){
                    var holder=server.getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE,recipe)).orElseThrow();
                    check(!result.setRecipeUsed(player,holder),"Actual player crafting rejects "+recipe);
                    var advancement=server.getAdvancements().get(Identifier.parse("thaumcraft2tp:recipes/"+CRAFTS.get(name))).value();
                    check(advancement.criteria().size()==1&&advancement.criteria().containsKey("locked_by_research")
                            &&advancement.criteria().get("locked_by_research").trigger()==net.minecraft.advancements.CriteriaTriggers.IMPOSSIBLE,"Picking up ingredients cannot unlock the recipe book: "+recipe);
                    level.setBlockAndUpdate(pos,Blocks.CRAFTER.defaultBlockState());
                    Blocks.CRAFTER.setPlacedBy(level,pos,level.getBlockState(pos),player,new ItemStack(Blocks.CRAFTER));
                    var crafter=(CrafterBlockEntity)level.getBlockEntity(pos);
                    var craft=GameData.crafts().stream().filter(c->c.id().equals(recipe.toString())).findFirst().orElseThrow();
                    int slot=0;for(String row:craft.pattern())for(char symbol:row.toCharArray())crafter.setItem(slot++,symbol==' '?ItemStack.EMPTY:ingredient(craft.key().get(String.valueOf(symbol))));
                    int before=crafter.getItems().stream().mapToInt(ItemStack::getCount).sum();pulse(level,pos);
                    check(crafter.getItems().stream().mapToInt(ItemStack::getCount).sum()==before,"Actual Crafter preserves locked ingredients: "+recipe);
                    if(name.equals("arcane_bore")){
                        var focus=Identifier.parse("thaumcraft2tp:base_028");
                        check(ThaumcraftApi.server().requiredResearch(focus).orElseThrow().equals(id),"Default Focus shares Bore research");
                        check(!ThaumcraftApi.canCraft(server,player.getUUID(),focus)&&ResearchGate.locked(Set.of(),focus),"Default Focus is locked before Bore research");
                        var focusHolder=server.getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE,focus)).orElseThrow();
                        check(!result.setRecipeUsed(player,focusHolder),"Player crafting rejects Focus before Bore research");
                    }
                    data.unlock(player.getUUID(),id);
                    check(result.setRecipeUsed(player,holder),"Actual player crafting opens after learning: "+recipe);
                    if(name.equals("arcane_bore")){
                        var focus=Identifier.parse("thaumcraft2tp:base_028");
                        check(ThaumcraftApi.canCraft(server,player.getUUID(),focus)&&!ResearchGate.locked(Set.of(id),focus),"Bore research unlocks default Focus");
                        check(result.setRecipeUsed(player,server.getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE,focus)).orElseThrow()),"Player crafting permits Focus after Bore research");
                    }
                    pulse(level,pos);check(crafter.isEmpty(),"Actual Crafter crafts after its owner learns: "+recipe);
                }else{
                    var infusion=GameData.infusions().stream().filter(r->r.key().equals(recipe)).findFirst().orElseThrow();
                    level.setBlockAndUpdate(pos,Content.block("thaumic_infuser").defaultBlockState());
                    var machine=(MachineBlockEntity)level.getBlockEntity(pos);machine.setOwner(player.getUUID());
                    int slot=0;for(String item:infusion.ingredients())machine.setItem(slot++,ingredient(item));
                    machine.insertVis(infusion.cost(),false);
                    for(int tick=0;tick<5;tick++)machine.processes.tick(level);
                    check(machine.processes.pureWork==0&&machine.getItem(9).isEmpty()&&Math.abs(machine.totalVis()-infusion.cost())<.001F,"Locked infusion preserves vis and produces nothing: "+recipe);
                    data.unlock(player.getUUID(),id);
                    for(int tick=0;tick<infusion.cost()*4+5&&machine.getItem(9).isEmpty();tick++)machine.processes.tick(level);
                    var expected=infusion.result().create();
                    check(machine.getItem(9).is(expected.getItem())&&machine.getItem(9).getCount()==expected.getCount(),"Infusion completes after learning: "+recipe);
                }
                check(ThaumcraftApi.canCraft(server,player.getUUID(),recipe)&&!ThaumcraftApi.canCraft(server,null,recipe),"Only researched owner may craft: "+recipe);
                check(!ResearchGate.locked(Set.of(id),recipe),"Client gate reveals learned recipe: "+recipe);
                level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
            }
            elementalTools(server,pos);
            prerequisites(server,pos);
            sources(server,pos);
            check(ThaumcraftApi.server().requiredResearch(Identifier.parse("thaumcraft2tp:base_036")).isEmpty(),"Crystal Ball remains ungated");
        }finally{level.setBlockAndUpdate(pos.above(),Blocks.AIR.defaultBlockState());level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());}
        AltsPatches.LOG.info("ALTS_RESEARCH_GATING_PASS checks={}",checks);return checks;
    }
    private static ItemStack ingredient(String rule){return new ItemStack(BuiltInRegistries.ITEM.stream().filter(item->GameData.matches(rule,new ItemStack(item))).findFirst().orElseThrow());}
    private static void elementalTools(MinecraftServer server,BlockPos pos){
        var level=server.overworld();var data=ArcaneWorldData.researchData(level);
        for(int index:List.of(29,30,36,51,55)){
            var id=Identifier.parse("thaumcraft2tp:research_%03d".formatted(index));
            var holder=server.getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE,id)).orElseThrow();
            check(holder.value() instanceof dev.thaumcraft.recipe.InfusionRecipeData,"Elemental tool replaces workbench recipe: "+id);
            check(GameData.crafts().stream().noneMatch(c->c.id().equals(id.toString())),"Workbench catalog omits elemental tool: "+id);
            var recipe=GameData.infusions().stream().filter(r->r.key().equals(id)).findFirst().orElseThrow();
            check(!recipe.dark()&&recipe.cost()==50&&recipe.research()==index,"Normal infuser costs 50 vis and keeps tool research: "+id);
            var owner=UUID.randomUUID();
            level.setBlockAndUpdate(pos,Content.block("thaumic_infuser").defaultBlockState());
            var machine=(MachineBlockEntity)level.getBlockEntity(pos);machine.setOwner(owner);
            int slot=0;for(String item:recipe.ingredients())machine.setItem(slot++,ingredient(item));
            machine.insertVis(50,false);
            for(int tick=0;tick<5;tick++)machine.processes.tick(level);
            check(machine.getItem(9).isEmpty()&&machine.totalVis()==50,"Unlearned tool preserves vis: "+id);
            data.unlock(owner,Identifier.parse(recipe.result().id()));
            for(int tick=0;tick<205&&machine.getItem(9).isEmpty();tick++)machine.processes.tick(level);
            check(ItemStack.matches(machine.getItem(9),recipe.result().create())&&Math.abs(machine.totalVis())<.001F,"Learned tool completes using 50 vis: "+id);
            level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
        }
    }
    /** Every new project keeps its fragment chance and gains a material that finds it on each successful roll. */
    private static void sources(MinecraftServer server,BlockPos pos){
        var level=server.overworld();var data=ArcaneWorldData.researchData(level);
        var targeted=new java.util.HashSet<Identifier>();
        for(var source:AddonData.server().researchItems())if(source.special_chance()!=null&&source.special_chance()==100)source.special().forEach(p->targeted.add(GameData.project(p).key()));
        for(String name:PROJECTS)check(targeted.contains(research(name)),"A material targets "+name);
        level.setBlockAndUpdate(pos,Content.block("quaesitum").defaultBlockState());
        var machine=(MachineBlockEntity)level.getBlockEntity(pos);var owner=UUID.randomUUID();machine.setOwner(owner);
        var essences=Set.of("air","water","earth","fire","magic","dark").stream().map(e->research("runic_essence_"+e)).collect(java.util.stream.Collectors.toSet());
        var nitorUses=new java.util.HashSet<>(essences);nitorUses.add(research("arcane_singularity"));
        check(study(level,machine,"nitor").stream().noneMatch(nitorUses::contains),"Nitor reveals nothing whose prerequisites are unknown");
        check(study(level,machine,"minecraft:glowstone_dust").contains(research("nitor")),"Glowstone reveals Nitor");
        data.unlock(owner,research("nitor"));
        var found=study(level,machine,"nitor");
        check(!found.isEmpty()&&essences.containsAll(found),"Learned Nitor reveals runic essences but not Arcane Singularity before Alumentum");
        level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
    }
    /** One Quaesitum cycle; ancient artifacts as catalysts make the first roll succeed. */
    private static List<Identifier> study(net.minecraft.server.level.ServerLevel level,MachineBlockEntity machine,String item){
        var id=Identifier.parse(item.contains(":")?item:"thaumcraft2tp:"+item);
        machine.setItem(0,new ItemStack(BuiltInRegistries.ITEM.getValue(id)));
        machine.setItem(1,new ItemStack(Content.item("ancient_stone_tablet")));machine.setItem(2,new ItemStack(Content.item("ancient_seal")));
        machine.setItem(3,new ItemStack(net.minecraft.world.item.Items.PAPER,64));
        for(int slot=MachineBlockEntity.OUTPUT_START;slot<MachineBlockEntity.OUTPUT_END;slot++)machine.setItem(slot,ItemStack.EMPTY);
        check(ResearchLogic.odds(machine).success()>=100,"Catalysts guarantee a successful roll for "+item);
        machine.progress=10000;ResearchLogic.tick(level,machine);
        var theories=new java.util.ArrayList<Identifier>();
        for(int slot=MachineBlockEntity.OUTPUT_START;slot<MachineBlockEntity.OUTPUT_END;slot++)
            dev.thaumcraft.gameplay.ResearchItemData.project(machine.getItem(slot)).ifPresent(p->theories.add(p.key()));
        return theories;
    }
    private static void pulse(net.minecraft.server.level.ServerLevel level,BlockPos pos){
        level.setBlockAndUpdate(pos.above(),Blocks.REDSTONE_BLOCK.defaultBlockState());
        check(level.getBlockState(pos).getValue(CrafterBlock.TRIGGERED),"Redstone triggers test Crafter");
        level.getBlockState(pos).tick(level,pos,level.getRandom());level.setBlockAndUpdate(pos.above(),Blocks.AIR.defaultBlockState());
    }
    private static void prerequisites(MinecraftServer server,BlockPos pos){
        try{
            var select=ResearchLogic.class.getDeclaredMethod("selectProject",net.minecraft.server.level.ServerLevel.class,MachineBlockEntity.class,int.class,List.class);select.setAccessible(true);
            var level=server.overworld();var data=ArcaneWorldData.researchData(level);
            var bore=ThaumcraftApi.server().research(research("arcane_bore")).orElseThrow();
            check(bore.prerequisites().contains(research("arcane_singularity")),"Bore requires Arcane Singularity research");
            int overrides=0;
            for(var project:GameData.projects()){
                var gated=project.prerequisiteIds().stream().filter(p->p.getNamespace().equals("research_gating")).toList();if(gated.isEmpty())continue;
                if(!project.key().getNamespace().equals("research_gating"))overrides++;
                var owner=UUID.randomUUID();var machine=new MachineBlockEntity(pos,Content.block("quaesitum").defaultBlockState());machine.setOwner(owner);
                for(var prerequisite:project.prerequisiteIds())if(!gated.contains(prerequisite))data.unlock(owner,prerequisite);
                for(var prerequisite:gated){
                    check(select.invoke(null,level,machine,project.category(),List.of(project.index()))==null,"Quaesitum rejects "+project.id()+" before prerequisite "+prerequisite);
                    data.unlock(owner,prerequisite);
                }
                check(project.equals(select.invoke(null,level,machine,project.category(),List.of(project.index()))),"Quaesitum permits "+project.id()+" after all prerequisites");
            }
            check(overrides==25,"All 25 existing research prerequisite overrides loaded");
            var trunk=GameData.projects().stream().filter(p->p.id().equals("thaumcraft2tp:traveling_trunk")).findFirst().orElseThrow();
            var darkInfuser=net.minecraft.resources.Identifier.parse("thaumcraft2tp:dark_infuser");
            check(trunk.prerequisiteIds().contains(darkInfuser),"Traveling Trunk requires Dark Infuser");
            var owner=UUID.randomUUID();var machine=new MachineBlockEntity(pos,Content.block("quaesitum").defaultBlockState());machine.setOwner(owner);
            check(select.invoke(null,level,machine,trunk.category(),List.of(trunk.index()))==null,"Quaesitum rejects Traveling Trunk before Dark Infuser");
            data.unlock(owner,darkInfuser);
            check(trunk.equals(select.invoke(null,level,machine,trunk.category(),List.of(trunk.index()))),"Quaesitum permits Traveling Trunk after Dark Infuser");
            var generator=GameData.projects().stream().filter(p->p.id().equals("thaumcraft2tp:thaumic_generator")).findFirst().orElseThrow();
            var singularity=Identifier.parse("thaumcraft2tp:stabilized_singularity");
            check(generator.prerequisiteIds().contains(singularity),"Thaumic Generator requires Stabilized Singularity");
            var generatorOwner=UUID.randomUUID();machine.setOwner(generatorOwner);
            check(select.invoke(null,level,machine,generator.category(),List.of(generator.index()))==null,"Quaesitum rejects Thaumic Generator before Stabilized Singularity");
            data.unlock(generatorOwner,singularity);
            check(generator.equals(select.invoke(null,level,machine,generator.category(),List.of(generator.index()))),"Quaesitum permits Thaumic Generator after Stabilized Singularity");
        }catch(ReflectiveOperationException e){throw new AssertionError("Quaesitum prerequisite fixture",e);}
    }
}
