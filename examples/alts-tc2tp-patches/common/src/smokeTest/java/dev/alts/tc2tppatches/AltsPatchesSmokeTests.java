package dev.alts.tc2tppatches;

import dev.thaumcraft.api.ThaumcraftApi;
import dev.thaumcraft.content.Content;
import dev.thaumcraft.machine.MachineBlockEntity;
import dev.thaumcraft.machine.MachineLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.ShapelessRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Opt-in (-PsmokeTest) checks: a real crucible teaches the owner, the sync round-trips, and tooltips follow it. */
public final class AltsPatchesSmokeTests {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static void brainyLoot(MinecraftServer server){
        var level=server.overworld();
        var zombie=dev.thaumcraft.entity.ModEntities.BRAINY_ZOMBIE.create(level,net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        var params=new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY,zombie)
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,zombie.position())
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.DAMAGE_SOURCE,level.damageSources().generic())
                .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.ENTITY);
        var table=server.reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,Identifier.parse("thaumcraft2tp:entities/brainy_zombie")));
        var random=net.minecraft.util.RandomSource.create(216);int brains=0,skulls=0,both=0;boolean valid=true;
        for(int i=0;i<4096;i++){
            int brain=0,skull=0;
            for(var stack:table.getRandomItems(params,random)){
                if(stack.is(Content.item("zombie_brain")))brain+=stack.getCount();
                if(stack.is(Content.item("distorted_skull")))skull+=stack.getCount();
            }
            brains+=brain;skulls+=skull;if(brain>0&&skull>0)both++;
            valid&=brain<=1&&skull<=1;
        }
        AltsPatches.LOG.info("ALTS_BRAINY_LOOT brains={} skulls={} both={} rolls=4096",brains,skulls,both);
        check(valid,"Each brain and skull drop is limited to one");
        check(brains>4096*.45&&brains<4096*.55,"Bundled brain drop rate is 50%: "+brains+"/4096");
        check(skulls>4096*.45&&skulls<4096*.55,"Bundled skull drop rate is 50%: "+skulls+"/4096");
        check(both>4096*.20&&both<4096*.30,"Independent brain and skull rolls drop both about 25% of the time: "+both+"/4096");
    }
    public static void run(MinecraftServer server) {
        checks=0;
        generator(server);
        checks+=ResearchGatingSmokeTests.run(server);
        thaumonomicon(server);
        deepslateCinnabar(server);
        brainyLoot(server);
        researchSources();
        var level=server.overworld();var pos=new BlockPos(330,280,330);var owner=UUID.randomUUID();
        var cobblestone=BuiltInRegistries.ITEM.getKey(Items.COBBLESTONE);
        check(ThaumcraftApi.server().vis(new ItemStack(Items.STONE))==.04f,"Bundled datapack rules load from the mod jar");
        // Right after startup the chunk's entities are not loaded yet, and dropped items would be silently ignored.
        var chunk=net.minecraft.world.level.ChunkPos.containing(pos);level.setChunkForced(chunk.x(),chunk.z(),true);level.getChunk(chunk.x(),chunk.z());
        level.getChunkSource().tick(()->true,false);level.waitForEntities(chunk,0);
        level.setBlockAndUpdate(pos,Content.block("crucible").defaultBlockState());
        try{
            var crucible=(MachineBlockEntity)level.getBlockEntity(pos);crucible.setOwner(owner);
            var knowledge=VisKnowledge.get(server);
            check(!knowledge.knows(owner,cobblestone),"Nothing is known before dissolving");
            var item=new ItemEntity(level,pos.getX()+.5,pos.getY()+.6,pos.getZ()+.5,new ItemStack(Items.COBBLESTONE,2));level.addFreshEntity(item);
            for(int i=0;i<200&&item.getItem().getCount()==2;i++)MachineLogic.crucible(level,crucible);
            int left=item.getItem().getCount();boolean alive=item.isAlive();item.discard();
            check(knowledge.knows(owner,cobblestone)&&knowledge.known(owner).equals(java.util.List.of(cobblestone)),"Dissolving teaches the credited player: left="+left+" alive="+alive+" vis="+crucible.totalVis()+" known="+knowledge.known(owner));
            check(!knowledge.learn(owner,cobblestone),"Learning is idempotent");
            check(knowledge.isDirty(),"Learned items are saved");

            float vis=ThaumcraftApi.server().vis(new ItemStack(Items.COBBLESTONE));
            var payload=new VisKnowledgeSync(java.util.Map.of(cobblestone,vis));
            var buffer=new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),server.registryAccess());
            try{VisKnowledgeSync.CODEC.encode(buffer,payload);check(VisKnowledgeSync.CODEC.decode(buffer).equals(payload),"Sync payload round-trips");}
            finally{buffer.release();}

            ClientVisKnowledge.receive(payload);
            var lines=new ArrayList<Component>();
            ClientVisKnowledge.appendTooltip(new ItemStack(Items.COBBLESTONE),lines::add);
            check(lines.size()==1&&lines.get(0).getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents text
                    &&text.getKey().equals("tooltip.alts_tc2tp_patches.vis")&&java.util.Arrays.equals(text.getArgs(),new Object[]{VisKnowledgeSync.format(vis)}),"Known item shows its vis");
            lines.clear();ClientVisKnowledge.appendTooltip(new ItemStack(Items.DIRT),lines::add);
            check(lines.isEmpty(),"Unknown item shows nothing");
            var worn=new ItemStack(Items.COBBLESTONE);worn.set(net.minecraft.core.component.DataComponents.MAX_DAMAGE,100);worn.setDamageValue(1);
            lines.clear();ClientVisKnowledge.appendTooltip(worn,lines::add);
            check(lines.isEmpty(),"Worn item shows nothing");
            ClientVisKnowledge.clear();lines.clear();ClientVisKnowledge.appendTooltip(new ItemStack(Items.COBBLESTONE),lines::add);
            check(lines.isEmpty(),"Disconnect clears client knowledge");
            check(VisKnowledgeSync.format(1f).equals("1")&&VisKnowledgeSync.format(.04f).equals("0.04")&&VisKnowledgeSync.format(2.5f).equals("2.5"),"Values format without trailing zeros");
            knowledge.forget(owner,cobblestone);
        }finally{level.removeBlock(pos,false);level.setChunkForced(chunk.x(),chunk.z(),false);}
        AltsPatches.LOG.info("ALTS_SMOKE_TESTS_PASS checks={}",checks);
    }

    private static void generator(MinecraftServer server) {
        var level=server.overworld();var pos=new BlockPos(340,280,340);var clock=level.dimensionType().defaultClock().orElseThrow();long day=level.clockManager().getTotalTicks(clock);
        level.getChunkAt(pos);level.setBlockAndUpdate(pos,Content.block("thaumic_generator").defaultBlockState());
        level.setBlockAndUpdate(pos.east(),Content.block("vis_storage_tank").defaultBlockState());
        var machine=(MachineBlockEntity)level.getBlockEntity(pos);var source=(MachineBlockEntity)level.getBlockEntity(pos.east());source.insertVis(100,false);
        try{
            check(machine.energyCapacity()==2000000,"Generator stores two million FE");
            for(int phase=0;phase<8;phase++){
                level.clockManager().setTotalTicks(clock,phase*24000L);machine.restoreEnergy(0);float before=source.pureVis();
                check(dev.thaumcraft.machine.MachineProcesses.moon(level)==phase,"Generator fixture advances moon phase "+phase);
                for(int tick=0;tick<20;tick++)MachineLogic.generator(level,machine);
                float consumed=before-source.pureVis();
                check(consumed>0&&Math.abs(machine.energy()-consumed*60000)<10,"Generator yields 60000 FE per vis at moon phase "+phase);
            }
            machine.restoreEnergy(1500000);
            var saved=machine.saveWithFullMetadata(level.registryAccess());
            var restored=(MachineBlockEntity)net.minecraft.world.level.block.entity.BlockEntity.loadStatic(pos,machine.getBlockState(),saved,level.registryAccess());
            check(restored!=null&&restored.energy()==1500000,"Million-FE storage survives save and reload");
            var player=new net.minecraft.server.level.ServerPlayer(server,level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"Generator"),net.minecraft.server.level.ClientInformation.createDefault());
            var data=new net.minecraft.world.inventory.SimpleContainerData(dev.thaumcraft.machine.MachineMenu.DATA_COUNT);
            for(int i=0;i<data.getCount();i++)data.set(i,(short)machine.data.get(i));
            var menu=new dev.thaumcraft.machine.MachineMenu(1,player.getInventory(),machine,data);
            check(menu.storedEnergy()==1500000&&menu.status(31)==2000000,"Menu transmits full FE storage and capacity through 16-bit data slots");
            check(machine.generatorOutput.extract(4000)==4000&&machine.generatorOutput.extract(4000)==2000&&machine.generatorOutput.available(1)==0,"Generator shares a 6000 FE per tick output budget");
            var upgrade=Content.ENTRIES.stream().filter(e->e.source_class().equals("ItemUpgrades")&&e.meta()==5).findFirst().orElseThrow();
            machine.setItem(18,new ItemStack(Content.item(upgrade.id())));
            check(machine.energyCapacity()==4000000,"Storage upgrade raises generator capacity to four million FE");
            machine.restoreEnergy(machine.energyCapacity());float before=source.pureVis();MachineLogic.generator(level,machine);
            check(source.pureVis()==before&&machine.energy()==4000000,"Full generator does not consume vis");
        }finally{level.clockManager().setTotalTicks(clock,day);level.removeBlock(pos,false);level.removeBlock(pos.east(),false);}
    }

    /** Addon research sources replace the built-in rules for the same item and keep the original hints. */
    private static void researchSources(){
        check(dev.thaumcraft.gameplay.ResearchLogic.researchValue(new ItemStack(Content.item("zombie_brain")))==20,"Zombie Brain research value is 20");
        check(dev.thaumcraft.gameplay.ResearchLogic.researchValue(new ItemStack(Items.PAPER))==5,"Paper is a farmable research source");
        var book=dev.thaumcraft.gameplay.GameData.researchItem(new ItemStack(Items.BOOK));
        check(book.value()==20&&book.special().equals(List.of(dev.thaumcraft.gameplay.GameData.project(Identifier.parse("thaumcraft2tp:thaumic_enchanter")).index())),"Book is worth 20 and keeps its Thaumic Enchanter hint");
        var bookshelf=dev.thaumcraft.gameplay.GameData.researchItem(new ItemStack(Items.BOOKSHELF));
        check(bookshelf.value()==30&&bookshelf.special().equals(book.special()),"Bookshelf is worth 30 and keeps its Thaumic Enchanter hint");
        var bone=dev.thaumcraft.gameplay.GameData.researchItem(new ItemStack(Items.BONE));
        check(bone.value()==4&&bone.special().size()==2,"Bone is worth 4 and keeps both original hints");
        var feather=dev.thaumcraft.gameplay.GameData.researchItem(new ItemStack(Items.FEATHER));
        check(feather.value()==3&&feather.special_chance()==100,"Feather keeps its study material hint");
        check(dev.thaumcraft.gameplay.ResearchLogic.researchValue(new ItemStack(Content.item("ancient_stone_tablet")))==160,"Artifact research values are doubled");
    }
    private static void deepslateCinnabar(MinecraftServer server) {
        var ore=new ItemStack(AltsPatches.deepslateCinnabarOreItem);
        check(ThaumcraftApi.server().vis(ore)==ThaumcraftApi.server().vis(new ItemStack(Content.block("cinnabar_ore"))),"Deepslate cinnabar has cinnabar's vis");
        var smelted=server.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.SMELTING,new net.minecraft.world.item.crafting.SingleRecipeInput(ore),server.overworld());
        check(smelted.isPresent()&&smelted.get().value().assemble(new net.minecraft.world.item.crafting.SingleRecipeInput(ore)).is(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("thaumcraft2tp","quicksilver"))),"Deepslate cinnabar smelts into quicksilver");
        check(AltsPatches.deepslateCinnabarOre.defaultBlockState().is(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE),"Deepslate cinnabar is mined with a pickaxe");
        check(AltsPatches.deepslateCinnabarOre.defaultBlockState().getDestroySpeed(server.overworld(),BlockPos.ZERO)==Content.block("cinnabar_ore").defaultBlockState().getDestroySpeed(server.overworld(),BlockPos.ZERO),"Deepslate cinnabar keeps regular cinnabar's hardness");
        rawCinnabar(server);
        // The builtin pack selects the same generator with only the deepslate state changed.
        var placed=server.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE).getValue(Identifier.fromNamespaceAndPath("thaumcraft2tp","cinnabar_deposits"));
        check(placed!=null&&placed.feature().is(AltsPatches.id("cinnabar_ore")),"Cinnabar generation is replaced by the addon's ore feature");
        check(placed.placement().size()==1&&placed.placement().getFirst() instanceof net.minecraft.world.level.levelgen.placement.BiomeFilter,"Cinnabar has no extra vein count or height placement");
        cinnabarParity(server);
        int regular=0,deepslate=0;
        var level=server.overworld();
        // Generate terrain through the loader's biome hooks, rather than calling the ore feature directly.
        for(int cx=900;cx<902;cx++)for(int cz=900;cz<902;cz++) {
            var chunk=level.getChunk(cx,cz);
            for(int y=level.getMinY();y<56;y++)for(int x=0;x<16;x++)for(int z=0;z<16;z++) {
                var state=chunk.getBlockState(new BlockPos(cx*16+x,y,cz*16+z));
                if(state.is(Content.block("cinnabar_ore")))regular++;
                if(state.is(AltsPatches.deepslateCinnabarOre))deepslate++;
            }
        }
        check(regular>0&&deepslate>0,"Natural generation places both cinnabar variants: regular="+regular+" deepslate="+deepslate);
        AltsPatches.LOG.info("ALTS_CINNABAR_GENERATION regular={} deepslate={} chunks=4",regular,deepslate);
    }

    private static void rawCinnabar(MinecraftServer server) {
        var level=server.overworld();var raw=new ItemStack(AltsPatches.rawCinnabar);
        var quicksilver=BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("thaumcraft2tp","quicksilver"));
        check(ThaumcraftApi.server().vis(raw)==ThaumcraftApi.server().vis(new ItemStack(Content.block("cinnabar_ore"))),"Raw cinnabar has cinnabar's vis");
        for(var type:List.of(net.minecraft.world.item.crafting.RecipeType.SMELTING,net.minecraft.world.item.crafting.RecipeType.BLASTING)){
            var cooked=server.getRecipeManager().getRecipeFor(type,new net.minecraft.world.item.crafting.SingleRecipeInput(raw),level);
            check(cooked.isPresent()&&cooked.get().value().assemble(new net.minecraft.world.item.crafting.SingleRecipeInput(raw)).is(quicksilver),"Raw cinnabar cooks into quicksilver: "+type);
        }
        var pickaxe=new ItemStack(Items.IRON_PICKAXE);var silk=new ItemStack(Items.IRON_PICKAXE);
        silk.enchant(server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH),1);
        for(var ore:List.of(Content.block("cinnabar_ore"),AltsPatches.deepslateCinnabarOre)){
            var state=ore.defaultBlockState();
            var drops=net.minecraft.world.level.block.Block.getDrops(state,level,BlockPos.ZERO,null,null,pickaxe);
            check(drops.size()==1&&drops.getFirst().is(AltsPatches.rawCinnabar)&&drops.getFirst().getCount()==1,"Pickaxe mines raw cinnabar from "+ore+": "+drops);
            drops=net.minecraft.world.level.block.Block.getDrops(state,level,BlockPos.ZERO,null,null,silk);
            check(drops.size()==1&&drops.getFirst().is(ore.asItem()),"Silk touch keeps "+ore+": "+drops);
        }
    }

    private static void cinnabarParity(MinecraftServer server){
        var features=server.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE);
        var original=features.getValue(Identifier.fromNamespaceAndPath("thaumcraft2tp","cinnabar_deposits"));
        var patched=features.getValue(AltsPatches.id("cinnabar_ore"));
        check(original!=null&&patched!=null&&original.feature()==patched.feature(),"Addon reuses the original cinnabar generator");
        var level=server.overworld();var generator=level.getChunkSource().getGenerator();
        var origin=new BlockPos(4096,0,4096);
        var states=new java.util.HashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
        boolean enabled=dev.thaumcraft.PortConfig.worldGeneration;
        int regular=0,deepslate=0;
        try{
            dev.thaumcraft.PortConfig.worldGeneration=true;
            for(int bottom:new int[]{0,-64,-128,80}){
                var world=(net.minecraft.world.level.WorldGenLevel)java.lang.reflect.Proxy.newProxyInstance(
                        net.minecraft.world.level.WorldGenLevel.class.getClassLoader(),new Class<?>[]{net.minecraft.world.level.WorldGenLevel.class},(proxy,method,args)->switch(method.getName()){
                            case "getLevel" -> level;
                            case "getSeed" -> 216L;
                            case "getMinY" -> bottom;
                            case "getMaxY" -> 320;
                            case "getSeaLevel","getHeight" -> 63;
                            case "getBiome" -> level.getBiome(origin);
                            case "ensureCanWrite" -> args[0].equals(origin)||(((BlockPos)args[0]).getZ()&3)!=0;
                            case "getBlockState" -> {
                                var pos=(BlockPos)args[0];
                                yield states.getOrDefault(pos,cinnabarHost(pos));
                            }
                            case "setBlock" -> {states.put(((BlockPos)args[0]).immutable(),(net.minecraft.world.level.block.state.BlockState)args[1]);yield true;}
                            default -> throw new UnsupportedOperationException(method.toString());
                        });
                for(int seed=0;seed<32;seed++){
                    states.clear();var baselineRandom=net.minecraft.util.RandomSource.create(seed);
                    boolean baselinePlaced=original.place(world,generator,baselineRandom,origin);
                    var expected=new java.util.HashMap<>(states);long next=baselineRandom.nextLong();
                    expected.replaceAll((pos,state)->cinnabarHost(pos).is(net.minecraft.tags.BlockTags.DEEPSLATE_ORE_REPLACEABLES)?AltsPatches.deepslateCinnabarOre.defaultBlockState():state);
                    states.clear();var patchedRandom=net.minecraft.util.RandomSource.create(seed);
                    check(patched.place(world,generator,patchedRandom,origin)==baselinePlaced&&states.equals(expected),"Cinnabar keeps original positions and count: bottom="+bottom+" seed="+seed);
                    check(patchedRandom.nextLong()==next,"Cinnabar keeps original random consumption");
                    for(var entry:states.entrySet()){
                        check(entry.getKey().getY()>=bottom&&entry.getKey().getY()<=50,"Cinnabar stays in original height range");
                        if(entry.getValue().is(AltsPatches.deepslateCinnabarOre))deepslate++;else regular++;
                    }
                }
                states.clear();dev.thaumcraft.PortConfig.worldGeneration=false;
                check(!patched.place(world,generator,net.minecraft.util.RandomSource.create(1),origin)&&states.isEmpty(),"Addon respects disabled world generation");
                dev.thaumcraft.PortConfig.worldGeneration=true;
            }
            check(regular>0&&deepslate>0,"Parity terrain exercises stone and deepslate hosts");
        }finally{dev.thaumcraft.PortConfig.worldGeneration=enabled;}
        AltsPatches.LOG.info("ALTS_CINNABAR_PARITY regular={} deepslate={} samples=128",regular,deepslate);
    }

    private static net.minecraft.world.level.block.state.BlockState cinnabarHost(BlockPos pos){
        return ((pos.getX()&3)==0?net.minecraft.world.level.block.Blocks.DIRT:pos.getY()<0?net.minecraft.world.level.block.Blocks.DEEPSLATE:net.minecraft.world.level.block.Blocks.STONE).defaultBlockState();
    }

    private static void thaumonomicon(MinecraftServer server) {
        var tag=TagKey.create(Registries.ITEM,AltsPatches.id("knowledge_fragments"));
        var fragments=List.of("fragment_of_eldritch_knowledge","fragment_of_forbidden_knowledge",
                "fragment_of_lost_knowledge","fragment_of_tainted_knowledge");
        check(BuiltInRegistries.ITEM.get(tag).orElseThrow().size()==4,"Knowledge fragment tag has four variants");
        for(String id:List.of("base_000","base_001")) {
            var holder=server.getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE,Identifier.parse("thaumcraft2tp:"+id))).orElseThrow();
            check(holder.value() instanceof ShapelessRecipe,"Original Thaumonomicon recipe is replaced: "+id);
            var recipe=(ShapelessRecipe)holder.value();
            for(String fragment:fragments) {
                var stack=new ItemStack(Content.item(fragment));
                check(stack.is(tag),"Fragment is tagged: "+fragment);
                for(int size:List.of(2,3))for(int book=0;book<size*size;book++)for(int slot=0;slot<size*size;slot++) {
                    if(book==slot)continue;
                    var items=new ArrayList<>(java.util.Collections.nCopies(size*size,ItemStack.EMPTY));
                    items.set(book,new ItemStack(Items.BOOK));items.set(slot,stack);
                    var input=CraftingInput.of(size,size,items);
                    check(recipe.matches(input,server.overworld()),"Book and "+fragment+" match in any grid positions: "+id);
                    var output=recipe.assemble(input);
                    check(output.is(Content.item("thaumonomicon"))&&output.getCount()==1,"Recipe makes one Thaumonomicon");
                }
            }
            check(!recipe.matches(CraftingInput.of(1,1,List.of(new ItemStack(Items.BOOK))),server.overworld()),"Book alone is rejected");
            check(!recipe.matches(CraftingInput.of(2,1,List.of(new ItemStack(Items.BOOK),new ItemStack(Content.item("soul_fragment")))),server.overworld()),"Unrelated fragment is rejected");
            check(!recipe.matches(CraftingInput.of(3,1,List.of(new ItemStack(Items.BOOK),new ItemStack(Content.item(fragments.getFirst())),new ItemStack(Content.item(fragments.getFirst())))),server.overworld()),"Extra fragment is rejected");
        }
    }
}
