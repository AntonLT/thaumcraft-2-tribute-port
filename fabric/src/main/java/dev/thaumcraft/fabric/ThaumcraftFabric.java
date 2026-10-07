package dev.thaumcraft.fabric;

import dev.thaumcraft.Thaumcraft;
import dev.thaumcraft.content.Content;
import dev.thaumcraft.entity.ModEntities;
import dev.thaumcraft.machine.MachineBlockEntity;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.Set;

public final class ThaumcraftFabric implements ModInitializer {
    @Override public void onInitialize() {
        dev.thaumcraft.PortConfig.load(net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir());
        Thaumcraft.modLoaded=net.fabricmc.loader.api.FabricLoader.getInstance()::isModLoaded;
        Thaumcraft.ANCHOR_TICKET=Registry.register(BuiltInRegistries.TICKET_TYPE,Thaumcraft.id("seal_anchor"),new net.minecraft.server.level.TicketType(60,15));
        Thaumcraft.PORTAL_VIEW_TICKET=Registry.register(BuiltInRegistries.TICKET_TYPE,Thaumcraft.id("portal_view"),Thaumcraft.createPortalViewTicket());
        dev.thaumcraft.content.ModSounds.register((id,sound)->Registry.register(BuiltInRegistries.SOUND_EVENT,Thaumcraft.id(id),sound));
        Content.registerBlocks((id,block)->Registry.register(BuiltInRegistries.BLOCK,Thaumcraft.id(id),block));
        Content.registerItems((id,item)->Registry.register(BuiltInRegistries.ITEM,Thaumcraft.id(id),item));
        Registry.register(BuiltInRegistries.RECIPE_TYPE,Thaumcraft.id("infusion"),dev.thaumcraft.recipe.InfusionRecipeData.TYPE);
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,Thaumcraft.id("infusion"),dev.thaumcraft.recipe.InfusionRecipeData.SERIALIZER);
        Registry.register(BuiltInRegistries.TRIGGER_TYPES,Thaumcraft.id("research_learned"),dev.thaumcraft.gameplay.ProgressTriggers.RESEARCH_LEARNED);
        Registry.register(BuiltInRegistries.TRIGGER_TYPES,Thaumcraft.id("infusion_completed"),dev.thaumcraft.gameplay.ProgressTriggers.INFUSION_COMPLETED);
        Registry.register(BuiltInRegistries.LOOT_CONDITION_TYPE,Thaumcraft.id("knows_research"),dev.thaumcraft.gameplay.KnowsResearchCondition.CODEC);
        net.fabricmc.fabric.api.registry.FuelValueEvents.BUILD.register((builder,context)->{builder.add(Content.item("alumentum"),16000);builder.add(Content.item("silverwood_log"),600);builder.add(Content.item("greatwood_log"),400);});
        Content.MACHINE_ENTITY=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,Thaumcraft.id("machine"),new BlockEntityType<>(MachineBlockEntity::new,Set.of(Content.machineBlocks())));
        Content.VISUAL_ENTITY=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,Thaumcraft.id("visual"),new BlockEntityType<>(dev.thaumcraft.world.VisualBlockEntity::new,Content.visualBlocks()));
        Content.MONOLITH_ENTITY=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,Thaumcraft.id("monolith"),new BlockEntityType<>(dev.thaumcraft.world.MonolithBlockEntity::new,java.util.Set.of(Content.block("eldritch_core"))));
        FabricIntegration.register();
        Registry.register(BuiltInRegistries.MENU,Thaumcraft.id("machine"),Content.createMachineMenu());
        Content.registerMachineMenus((id,type)->Registry.register(BuiltInRegistries.MENU,Thaumcraft.id(id),type));
        Registry.register(BuiltInRegistries.MENU,Thaumcraft.id("void_storage"),Content.createVoidMenu());
        Registry.register(BuiltInRegistries.MENU,Thaumcraft.id("trunk"),Content.createTrunkMenu(false));
        Registry.register(BuiltInRegistries.MENU,Thaumcraft.id("roomy_trunk"),Content.createTrunkMenu(true));
        ModEntities.register((id,type)->Registry.register(BuiltInRegistries.ENTITY_TYPE,Thaumcraft.id(id),type));
        dev.thaumcraft.entity.ModSpawns.register(dev.thaumcraft.mixin.SpawnPlacementsInvoker::thaumcraft$register);
        for(String id:dev.thaumcraft.entity.ModSpawns.NATURAL)
            net.fabricmc.fabric.api.biome.v1.BiomeModifications.addSpawn(net.fabricmc.fabric.api.biome.v1.BiomeSelectors.tag(id.equals("wisp")?dev.thaumcraft.content.ModTags.SPAWNS_WISPS:dev.thaumcraft.content.ModTags.SPAWNS_ARCANE_MOBS),net.minecraft.world.entity.MobCategory.MONSTER,ModEntities.TYPES.get(id),dev.thaumcraft.entity.ModSpawns.weight(id),dev.thaumcraft.entity.ModSpawns.minCount(id),dev.thaumcraft.entity.ModSpawns.maxCount(id));
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_LEVEL_TICK.register(level->{dev.thaumcraft.world.AuraTicker.tick(level);dev.thaumcraft.world.ChunkAnchors.tick(level);});
        ModEntities.ATTRIBUTES.forEach(FabricDefaultAttributeRegistry::register);
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,Thaumcraft.id("main"),Content.createTab());
        dev.thaumcraft.world.ArcaneFeatures.register((id,feature)->Registry.register(BuiltInRegistries.FEATURE,Thaumcraft.id(id),feature));
        for(String name:dev.thaumcraft.world.ArcaneFeatures.NAMES)
            net.fabricmc.fabric.api.biome.v1.BiomeModifications.addFeature(net.fabricmc.fabric.api.biome.v1.BiomeSelectors.tag(dev.thaumcraft.content.ModTags.featureBiomes(name)),
                    name.equals("arcane_deposits")||name.equals("cinnabar_deposits")?net.minecraft.world.level.levelgen.GenerationStep.Decoration.UNDERGROUND_ORES:name.equals("arcane_treasure")?net.minecraft.world.level.levelgen.GenerationStep.Decoration.TOP_LAYER_MODIFICATION:net.minecraft.world.level.levelgen.GenerationStep.Decoration.VEGETAL_DECORATION,
                    dev.thaumcraft.world.ArcaneFeatures.placed(name));
        net.fabricmc.fabric.api.biome.v1.BiomeModifications.addFeature(net.fabricmc.fabric.api.biome.v1.BiomeSelectors.tag(dev.thaumcraft.content.ModTags.NETHER_ARCANE_VEGETATION),
                net.minecraft.world.level.levelgen.GenerationStep.Decoration.VEGETAL_DECORATION,dev.thaumcraft.world.ArcaneFeatures.placed("arcane_vegetation"));
        if(Boolean.getBoolean("thaumcraft.smokeTest"))ServerLifecycleEvents.SERVER_STARTED.register(server->{
            try {Class.forName("dev.thaumcraft.fabric.ItemAccessSmokeTests").getMethod("run",net.minecraft.server.MinecraftServer.class).invoke(null,server);}
            catch(ReflectiveOperationException exception) {throw new IllegalStateException("Thaumcraft Fabric smoke test failed",exception);}
        });
        ServerLifecycleEvents.SERVER_STARTED.register(Thaumcraft::serverStarted);
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((dispatcher,registries,environment)->dev.thaumcraft.command.ThaumcraftCommand.register(dispatcher));
        net.fabricmc.fabric.api.loot.v3.LootTableEvents.MODIFY_DROPS.register((table,context,loot)->table.unwrapKey().ifPresent(key->dev.thaumcraft.gameplay.ArtifactLoot.add(key.identifier(),context,loot)));
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay().register(dev.thaumcraft.network.SoulAbsorption.TYPE,dev.thaumcraft.network.SoulAbsorption.CODEC);
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay().register(dev.thaumcraft.network.GeneratorArc.TYPE,dev.thaumcraft.network.GeneratorArc.CODEC);
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay().register(dev.thaumcraft.network.EquipmentEffect.TYPE,dev.thaumcraft.network.EquipmentEffect.CODEC);
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay().register(dev.thaumcraft.network.SealEffect.TYPE,dev.thaumcraft.network.SealEffect.CODEC);
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay().register(dev.thaumcraft.network.BoreEffect.TYPE,dev.thaumcraft.network.BoreEffect.CODEC);
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay().register(dev.thaumcraft.network.ResearchSync.TYPE,dev.thaumcraft.network.ResearchSync.CODEC);
        dev.thaumcraft.network.SoulAbsorption.sender=net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking::send;
        dev.thaumcraft.network.GeneratorArc.sender=net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking::send;
        dev.thaumcraft.network.EquipmentEffect.sender=net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking::send;
        dev.thaumcraft.network.SealEffect.sender=net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking::send;
        dev.thaumcraft.network.BoreEffect.sender=net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking::send;
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay().register(dev.thaumcraft.network.CatalogSync.TYPE,dev.thaumcraft.network.CatalogSync.CODEC);
        dev.thaumcraft.network.CatalogSync.sender=net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking::send;
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server->dev.thaumcraft.gameplay.AddonData.clearServer());
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server,manager,success)->{if(success)dev.thaumcraft.gameplay.AddonData.reload(server,false);});
        dev.thaumcraft.network.ResearchSync.sender=net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking::send;
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((handler,sender,server)->{dev.thaumcraft.gameplay.AddonData.sync(handler.player);dev.thaumcraft.api.ThaumcraftKnowledge.synchronize(handler.player);});
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.serverboundPlay().register(dev.thaumcraft.network.VoidCompassTarget.Request.TYPE,dev.thaumcraft.network.VoidCompassTarget.Request.CODEC);
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay().register(dev.thaumcraft.network.VoidCompassTarget.TYPE,dev.thaumcraft.network.VoidCompassTarget.CODEC);
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(dev.thaumcraft.network.VoidCompassTarget.Request.TYPE,(payload,context)->dev.thaumcraft.network.VoidCompassTarget.handle(context.player(),payload,target->net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(context.player(),target)));
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.serverboundPlay().register(dev.thaumcraft.network.PortalScenes.Request.TYPE,dev.thaumcraft.network.PortalScenes.Request.CODEC);
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay().register(dev.thaumcraft.network.PortalScenes.Snapshot.TYPE,dev.thaumcraft.network.PortalScenes.Snapshot.CODEC);
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay().register(dev.thaumcraft.network.PortalScenes.Entities.TYPE,dev.thaumcraft.network.PortalScenes.Entities.CODEC);
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(dev.thaumcraft.network.PortalScenes.Request.TYPE,(payload,context)->dev.thaumcraft.network.PortalScenes.handle(context.player(),payload,snapshot->net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(context.player(),snapshot)));
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_LEVEL_TICK.register(dev.thaumcraft.network.PortalScenes::tick);
        Thaumcraft.LOG.info("Thaumcraft 2 Tribute Port initialized for Fabric");
    }
}
