package dev.thaumcraft.test;

import com.mojang.serialization.JsonOps;
import dev.thaumcraft.Thaumcraft;
import dev.thaumcraft.content.Content;
import dev.thaumcraft.entity.ModEntities;
import dev.thaumcraft.gameplay.ArcaneWorldData;
import dev.thaumcraft.gameplay.GameData;
import dev.thaumcraft.machine.MachineBlockEntity;
import dev.thaumcraft.machine.MachineLogic;
import dev.thaumcraft.machine.VisNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.TagValueOutput;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Runs only with -PsmokeTest in an isolated development server. */
public final class ServerSmokeTests {
    private static int checks;
    private static final UUID OWNER=UUID.fromString("f3bdaf20-94ae-4a9b-99a1-c54412814662");
    private static void check(boolean condition,String message) {checks++;if(!condition)throw new AssertionError(message);}
    public static void run(MinecraftServer server) {
        checks=0;
        ServerLevel level=server.overworld();
        GameData.validate();
        checks+=ResearchIdentityChecks.run(server);
        if(Boolean.getBoolean("thaumcraft.guiSmoke")){
            checks+=GuiParitySmokeTests.run(server);
            Thaumcraft.LOG.info("THAUMCRAFT_SMOKE_TESTS_PASS checks={} suite=gui-parity",checks);return;
        }
        if(Boolean.getBoolean("thaumcraft.infuserSmoke")){
            checks+=dev.thaumcraft.machine.InfuserReservationChecks.run(server);
            checks+=dev.thaumcraft.machine.DuplicatorReservationChecks.run(server);
            Thaumcraft.LOG.info("THAUMCRAFT_SMOKE_TESTS_PASS checks={} suite=infuser-reservations",checks);return;
        }
        if(Boolean.getBoolean("thaumcraft.biomeAccessSmoke")){
            checks+=AddonAccessChecks.biomeOverrides(server);
            Thaumcraft.LOG.info("THAUMCRAFT_SMOKE_TESTS_PASS checks={} suite=biome-access",checks);return;
        }
        if(Boolean.getBoolean("thaumcraft.addonAccessSmoke")){
            checks+=AddonApiChecks.run(server,true)+IntegrationSmokeTests.run(server);
            Thaumcraft.LOG.info("THAUMCRAFT_SMOKE_TESTS_PASS checks={} suite=addon-access",checks);return;
        }
        if(Boolean.getBoolean("thaumcraft.localizationSmoke")){
            checks+=CommandChecks.run(server);checks+=AddonApiChecks.runLocalization(server);
            Thaumcraft.LOG.info("THAUMCRAFT_SMOKE_TESTS_PASS checks={} suite=localization",checks);return;
        }
        if(Boolean.getBoolean("thaumcraft.conventionBiomeSmoke")){
            checks=GenerationParitySmokeTests.conventionBiome(server);
            Thaumcraft.LOG.info("THAUMCRAFT_SMOKE_TESTS_PASS checks={} suite=convention-biome",checks);return;
        }
        if(TreeCensus.requested()){
            // Off the server thread so ticks keep unloading scanned chunks.
            new Thread(()->{
                TreeCensus.run(server);
                Thaumcraft.LOG.info("THAUMCRAFT_SMOKE_TESTS_PASS checks=0 suite=tree-census");
                // The census world is discarded; halting skips minutes of saving generated chunks.
                try{Thread.sleep(1000);}catch(InterruptedException ignored){}
                Runtime.getRuntime().halt(0);
            },"tree-census").start();return;
        }
        if(Boolean.getBoolean("thaumcraft.generationSmoke")){
            checks=GenerationParitySmokeTests.run(server);
            Thaumcraft.LOG.info("THAUMCRAFT_SMOKE_TESTS_PASS checks={} suite=generation",checks);return;
        }
        if(Boolean.getBoolean("thaumcraft.basicEnchantingSmoke")){
            checks=dev.thaumcraft.machine.BasicEnchantingChecks.run(server);
            Thaumcraft.LOG.info("THAUMCRAFT_SMOKE_TESTS_PASS checks={} suite=thaumic-enchanter",checks);return;
        }
        if(Boolean.getBoolean("thaumcraft.enchantingTableSmoke")){
            checks=ProgressionSmokeTests.runEnchantingTable(server);
            Thaumcraft.LOG.info("THAUMCRAFT_SMOKE_TESTS_PASS checks={} suite=enchanting-table",checks);return;
        }
        if(Boolean.getBoolean("thaumcraft.crafterSmoke")){
            checks=CrafterTreasureSmokeTests.run(server);
            Thaumcraft.LOG.info("THAUMCRAFT_SMOKE_TESTS_PASS checks={} suite=crafter",checks);return;
        }
        if(Boolean.getBoolean("thaumcraft.monolithSmoke")){
            checks=MonolithParitySmokeTests.run(server)+WorldParitySmokeTests.eldritch(server.overworld())+GenerationParitySmokeTests.run(server);
            Thaumcraft.LOG.info("THAUMCRAFT_SMOKE_TESTS_PASS checks={} suite=monolith",checks);return;
        }
        if(Boolean.getBoolean("thaumcraft.potionSmoke")){
            checks=PotionParitySmokeTests.run(server);
            Thaumcraft.LOG.info("THAUMCRAFT_SMOKE_TESTS_PASS checks={} suite=potions",checks);return;
        }
        if(Boolean.getBoolean("thaumcraft.crystalSmoke")){
            checks=ProgressionSmokeTests.runCrystals(server);
            Thaumcraft.LOG.info("THAUMCRAFT_SMOKE_TESTS_PASS checks={} suite=crystals",checks);return;
        }
        if(Boolean.getBoolean("thaumcraft.braceletSmoke")){
            checks=BraceletParitySmokeTests.run(server);
            Thaumcraft.LOG.info("THAUMCRAFT_SMOKE_TESTS_PASS checks={} suite=bracelet",checks);return;
        }
        if(Boolean.getBoolean("thaumcraft.sealSmoke")){
            checks=BraceletParitySmokeTests.run(server)+SealParitySmokeTests.run(server)+SourceParitySmokeTests.run(server)+EffectsParitySmokeTests.run(server)+IntegrationSmokeTests.run(server);
            Thaumcraft.LOG.info("THAUMCRAFT_SMOKE_TESTS_PASS checks={} suite=seals",checks);return;
        }
        if(Boolean.getBoolean("thaumcraft.carpetSmoke")){
            checks=CarpetParitySmokeTests.run(server);
            Thaumcraft.LOG.info("THAUMCRAFT_SMOKE_TESTS_PASS checks={} suite=carpet",checks);return;
        }
        if(Boolean.getBoolean("thaumcraft.voidSmoke")){
            checks=VoidNetworkSmokeTests.run(server);
            Thaumcraft.LOG.info("THAUMCRAFT_SMOKE_TESTS_PASS checks={} suite=void-networks",checks);return;
        }
        if(Boolean.getBoolean("thaumcraft.equipmentSmoke")){
            checks=ArcanaParitySmokeTests.run(server);
            Thaumcraft.LOG.info("THAUMCRAFT_SMOKE_TESTS_PASS checks={} suite=equipment-and-arcana",checks);return;
        }
        if(Boolean.getBoolean("thaumcraft.mobSmoke")){
            checks=dev.thaumcraft.entity.MobParitySmokeTests.run(server);
            Thaumcraft.LOG.info("THAUMCRAFT_SMOKE_TESTS_PASS checks={} suite=mobs",checks);return;
        }
        checks+=PotionParitySmokeTests.run(server);
        checks+=BraceletParitySmokeTests.run(server);
        checks+=CarpetParitySmokeTests.run(server);
        checks+=VoidNetworkSmokeTests.run(server);
        checks+=BoreParitySmokeTests.run(server);
        checks+=dev.thaumcraft.machine.BasicEnchantingChecks.run(server);
        checks+=dev.thaumcraft.machine.InfuserReservationChecks.run(server);
        checks+=dev.thaumcraft.machine.DuplicatorReservationChecks.run(server);
        recipes();
        BlockPos start=new BlockPos(64,290,64);
        level.getChunkAt(start);
        for(int i=0;i<24;i++)level.setBlockAndUpdate(start.east(i),Blocks.AIR.defaultBlockState());
        MachineBlockEntity source=place(level,start,"crucible");
        MachineBlockEntity pipe=place(level,start.east(),"vis_conduit");
        MachineBlockEntity tank=place(level,start.east(2),"vis_storage_tank");
        source.insertVis(80,false);source.insertVis(20,true);
        for(int i=0;i<20;i++){VisNetwork.tick(level,tank);VisNetwork.tick(level,pipe);}
        check(tank.pureVis()>0&&tank.taintedVis()>0,"Tank suction transports both components through a local conduit buffer");
        check(Math.abs(source.pureVis()+pipe.pureVis()+tank.pureVis()-80)<.001f&&Math.abs(source.taintedVis()+pipe.taintedVis()+tank.taintedVis()-20)<.001f,"Buffered vis is conserved across all network nodes");
        MachineBlockEntity filter=place(level,start.east(),"vis_filter");
        float oldPure=tank.pureVis(),oldTaint=tank.taintedVis(),sourceTaint=source.taintedVis();
        VisNetwork.tick(level,filter);VisNetwork.tick(level,tank);
        check(tank.taintedVis()<oldTaint&&source.taintedVis()<sourceTaint&&filter.taintedVis()>0,"Filter's stronger taint suction draws contamination from both neighboring buffers");
        check(Math.abs(tank.taintedVis()+source.taintedVis()+filter.taintedVis()+.025f-oldTaint-sourceTaint)<.001f,"Filter retains extracted taint apart from its exact .025 collection step");
        check(tank.pureVis()>oldPure,"Filter passes pure vis down its independent suction gradient");
        check(source.insertVis(Float.NaN,false)==0&&source.extractVis(-1,false)==0,"Reject invalid vis requests");

        source.setOwner(OWNER);source.setItem(0,new ItemStack(Items.IRON_INGOT,7));source.progress=35;
        var saved=source.saveWithFullMetadata(level.registryAccess());
        var restored=(MachineBlockEntity)BlockEntity.loadStatic(start,source.getBlockState(),saved,level.registryAccess());
        check(restored!=null&&OWNER.equals(restored.owner())&&restored.getItem(0).getCount()==7&&restored.progress==35&&restored.pureVis()==source.pureVis(),"Machine NBT round trip");
        source.setItem(0,ItemStack.EMPTY);

        MachineBlockEntity voidA=place(level,start.east(4),"void_chest"),voidB=place(level,start.east(5),"void_chest");
        voidA.setOwner(OWNER);voidB.setOwner(OWNER);voidA.setChannel(2);voidB.setChannel(2);
        voidA.setItem(0,new ItemStack(Items.DIAMOND,13));
        check(voidA.getContainerSize()==72&&voidB.getContainerSize()==72,"Void chests retain the original 72 slots");
        check(voidB.getItem(0).isEmpty(),"Void chests keep independent local contents");
        level.setBlockAndUpdate(start.east(4),Blocks.AIR.defaultBlockState());
        check(voidB.getItem(0).isEmpty(),"Breaking a void chest cannot copy contents to another chest");

        var research=ArcaneWorldData.researchData(level);research.unlock(OWNER,0);
        var encoded=ArcaneWorldData.CODEC.encodeStart(JsonOps.INSTANCE,research).getOrThrow();
        check(ArcaneWorldData.CODEC.parse(JsonOps.INSTANCE,encoded).getOrThrow().knows(OWNER,0),"Research save round trip");
        var aura=research.aura(level,start);float taken=research.drainAura(level,start,17,false);
        check(taken==17&&research.aura(level,start).vis()==aura.vis()-17,"Aura depletion");

        for(var entry:Content.ENTRIES)if(entry.kind().equals("machine")) {
            MachineBlockEntity machine=place(level,start.east(8),entry.id());
            MachineBlockEntity.tick(level,machine.getBlockPos(),machine.getBlockState(),machine);
            check(machine.getContainerSize()==(entry.id().equals("void_chest")||entry.id().equals("void_interface")?72:MachineBlockEntity.SIZE),"Container for "+entry.id());
        }
        for(var type:ModEntities.TYPES.entrySet()) {
            var entity=type.getValue().create(level,EntitySpawnReason.COMMAND);
            check(entity!=null,"Entity factory "+type.getKey());
            entity.setPos(68,292,68);
            var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,level.registryAccess());
            entity.saveWithoutId(output);
            check(!output.buildResult().isEmpty(),"Entity saves "+type.getKey());
            entity.discard();
        }
        for(String feature:dev.thaumcraft.world.ArcaneFeatures.NAMES)
            check(level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE).get(dev.thaumcraft.world.ArcaneFeatures.placed(feature)).isPresent(),"Placed feature "+feature);

        // Real machine processing: known recipe, exact vis charge and one output.
        GameData.Infusion recipe=GameData.infusions().stream().filter(r->!r.dark()&&r.research()<0).findFirst().orElseThrow();
        MachineBlockEntity infuser=place(level,start.east(10),"thaumic_infuser");
        int slot=0;for(String ingredient:recipe.ingredients())infuser.setItem(slot++,representative(ingredient));
        infuser.insertVis(recipe.cost(),false);
        for(int i=0;i<recipe.cost()*2+2;i++)infuser.processes.tick(level);
        check(infuser.getItem(9).is(recipe.result().create().getItem()),"Infuser produces "+recipe.result().id());
        check(infuser.pureVis()<.001f,"Infuser charges the full vis cost");
        checks+=ProgressionSmokeTests.run(server);
        checks+=GuiParitySmokeTests.run(server);
        checks+=MachineParitySmokeTests.run(server);
        checks+=ArcanaParitySmokeTests.run(server);
        checks+=WorldParitySmokeTests.run(server);
        checks+=dev.thaumcraft.entity.MobParitySmokeTests.run(server);
        checks+=GenerationParitySmokeTests.run(server);
        checks+=SourceParitySmokeTests.run(server);
        checks+=LightningParitySmokeTests.run(server);
        checks+=EffectsParitySmokeTests.run(server);
        for(int i=0;i<24;i++)level.setBlockAndUpdate(start.east(i),Blocks.AIR.defaultBlockState());
        int parityFailures=0;
        try {checks+=MonolithParitySmokeTests.run(server);} catch(AssertionError error){System.err.println(error);parityFailures++;}
        try {checks+=dev.thaumcraft.machine.OcculticEnchantingChecks.run(server);} catch(AssertionError error){System.err.println(error);parityFailures++;}
        try {checks+=dev.thaumcraft.machine.CombinedMachineChecks.run(server);} catch(AssertionError error){System.err.println(error);parityFailures++;}
        try {checks+=CrafterTreasureSmokeTests.run(server);} catch(AssertionError error){System.err.println(error);parityFailures++;}
        if(parityFailures>0)throw new AssertionError("Combined parity regressions: "+parityFailures);
        checks+=CommandChecks.run(server);
        checks+=AddonApiChecks.run(server);
        Thaumcraft.LOG.info("THAUMCRAFT_SMOKE_TESTS_PASS checks={}",checks);
    }
    private static MachineBlockEntity place(ServerLevel level,BlockPos pos,String id) {
        level.setBlockAndUpdate(pos,Content.block(id).defaultBlockState());
        var machine=(MachineBlockEntity)level.getBlockEntity(pos);check(machine!=null,"Block entity for "+id);return machine;
    }
    private static ItemStack representative(String ingredient) {
        if(ingredient.startsWith("#")) {
            var values=BuiltInRegistries.ITEM.get(TagKey.create(Registries.ITEM,Identifier.parse(ingredient.substring(1)))).orElseThrow();
            check(values.size()>0,"Nonempty ingredient tag "+ingredient);
            return new ItemStack(values.iterator().next().value());
        }
        var item=BuiltInRegistries.ITEM.getValue(Identifier.parse(ingredient));
        check(item!=null&&item!=Items.AIR,"Ingredient exists "+ingredient);return new ItemStack(item);
    }
    private static void recipes() {
        String crystals="#thaumcraft2tp:infusion_base_crystals";
        var ingot=GameData.infusions().stream().filter(r->r.result().id().equals("thaumcraft2tp:thaumium_ingot")&&r.ingredients().contains(crystals)).findFirst().orElseThrow();
        for(String name:List.of("vis_crystal","vaporous_crystal","aqueous_crystal","earthen_crystal","fiery_crystal"))
            check(GameData.allocateNormal(ingot,List.of(new ItemStack(Content.item(name)),new ItemStack(Items.IRON_INGOT)),2)!=null,"Base infusion accepts "+name);
        for(String name:List.of("tainted_crystal","depleted_crystal"))
            check(GameData.allocateNormal(ingot,List.of(new ItemStack(Content.item(name)),new ItemStack(Items.IRON_INGOT)),2)==null,"Base infusion rejects "+name);
        var wood=GameData.infusions().stream().filter(r->r.result().id().equals("thaumcraft2tp:enchanted_wood")&&r.ingredients().contains(crystals)).toList();
        check(wood.size()==2&&wood.stream().anyMatch(r->r.result().count()==4&&GameData.allocateNormal(r,List.of(new ItemStack(Content.item("vis_crystal")),new ItemStack(Items.OAK_LOG)),2)!=null)&&wood.stream().anyMatch(r->r.result().count()==5&&GameData.allocateNormal(r,List.of(new ItemStack(Content.item("vis_crystal")),new ItemStack(Content.item("greatwood_log"))),2)!=null),"Ordinary and greatwood infusion yields remain distinct");
        check(GameData.matches("#minecraft:planks",new ItemStack(Items.SPRUCE_PLANKS)),"Vis conduit accepts other planks");
        check(GameData.matches("#c:ingots/iron",new ItemStack(Items.IRON_INGOT))&&GameData.matches("#c:ingots/gold",new ItemStack(Items.GOLD_INGOT))&&GameData.matches("#c:nuggets/gold",new ItemStack(Items.GOLD_NUGGET)),"Seeded material tags load");
        for(GameData.Infusion recipe:GameData.infusions()) {
            var inputs=new ArrayList<ItemStack>();for(String ingredient:recipe.ingredients())inputs.add(representative(ingredient));
            while(inputs.size()<9)inputs.add(ItemStack.EMPTY);
            check(GameData.allocate(recipe,inputs,9)!=null,"Recipe can match "+recipe.result().id());
            inputs.set(0,ItemStack.EMPTY);
            check(GameData.allocate(recipe,inputs,9)==null,"Missing ingredient rejected for "+recipe.result().id());
        }
        var repeated=new GameData.Infusion(new GameData.StackDef("minecraft:stone",1),1,List.of("minecraft:water_bucket","minecraft:water_bucket"),false,-1);
        check(GameData.allocate(repeated,List.of(new ItemStack(Items.WATER_BUCKET)),1)==null,"One bucket cannot satisfy two ingredients");
    }
}
