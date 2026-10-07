package dev.thaumcraft.neoforge;

import dev.thaumcraft.Thaumcraft;
import dev.thaumcraft.content.Content;
import dev.thaumcraft.entity.ModEntities;
import dev.thaumcraft.machine.MachineBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.Set;

@Mod(Thaumcraft.MOD_ID)
public final class ThaumcraftNeoForge {
    public ThaumcraftNeoForge(IEventBus bus) {
        NeoForgeIntegration.register();
        dev.thaumcraft.network.SoulAbsorption.sender=(player,payload)->net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,payload);
        dev.thaumcraft.network.GeneratorArc.sender=(player,payload)->net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,payload);
        dev.thaumcraft.network.EquipmentEffect.sender=(player,payload)->net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,payload);
        dev.thaumcraft.network.SealEffect.sender=(player,payload)->net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,payload);
        dev.thaumcraft.network.BoreEffect.sender=(player,payload)->net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,payload);
        dev.thaumcraft.network.CatalogSync.sender=(player,payload)->net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,payload);
        dev.thaumcraft.network.ResearchSync.sender=(player,payload)->net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,payload);
        dev.thaumcraft.PortConfig.load(net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get());
        Thaumcraft.modLoaded=id->net.neoforged.fml.ModList.get().isLoaded(id);
        bus.addListener(ThaumcraftNeoForge::register);
        bus.addListener((net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent event)->{
            var registrar=event.registrar("1");
            registrar.playToClient(dev.thaumcraft.network.SoulAbsorption.TYPE,dev.thaumcraft.network.SoulAbsorption.CODEC,(payload,context)->dev.thaumcraft.network.SoulAbsorption.receiver.accept(payload));
            registrar.playToClient(dev.thaumcraft.network.GeneratorArc.TYPE,dev.thaumcraft.network.GeneratorArc.CODEC,(payload,context)->dev.thaumcraft.network.GeneratorArc.receiver.accept(payload));
            registrar.playToClient(dev.thaumcraft.network.EquipmentEffect.TYPE,dev.thaumcraft.network.EquipmentEffect.CODEC,(payload,context)->dev.thaumcraft.network.EquipmentEffect.receiver.accept(payload));
            registrar.playToClient(dev.thaumcraft.network.SealEffect.TYPE,dev.thaumcraft.network.SealEffect.CODEC,(payload,context)->dev.thaumcraft.network.SealEffect.receiver.accept(payload));
            registrar.playToClient(dev.thaumcraft.network.BoreEffect.TYPE,dev.thaumcraft.network.BoreEffect.CODEC,(payload,context)->dev.thaumcraft.network.BoreEffect.receiver.accept(payload));
            registrar.playToClient(dev.thaumcraft.network.CatalogSync.TYPE,dev.thaumcraft.network.CatalogSync.CODEC,(payload,context)->context.enqueueWork(()->dev.thaumcraft.network.CatalogSync.handle(payload)));
            registrar.playToClient(dev.thaumcraft.network.ResearchSync.TYPE,dev.thaumcraft.network.ResearchSync.CODEC,(payload,context)->context.enqueueWork(()->dev.thaumcraft.network.ResearchSync.handleClient(payload)));
            registrar.playToServer(dev.thaumcraft.network.VoidCompassTarget.Request.TYPE,dev.thaumcraft.network.VoidCompassTarget.Request.CODEC,(payload,context)->context.enqueueWork(()->dev.thaumcraft.network.VoidCompassTarget.handle((net.minecraft.server.level.ServerPlayer)context.player(),payload,context::reply)));
            registrar.playToClient(dev.thaumcraft.network.VoidCompassTarget.TYPE,dev.thaumcraft.network.VoidCompassTarget.CODEC,(payload,context)->context.enqueueWork(()->dev.thaumcraft.network.VoidCompassTarget.receiver.accept(payload)));
            registrar.playToServer(dev.thaumcraft.network.PortalScenes.Request.TYPE,dev.thaumcraft.network.PortalScenes.Request.CODEC,(payload,context)->dev.thaumcraft.network.PortalScenes.handle((net.minecraft.server.level.ServerPlayer)context.player(),payload,snapshot->net.neoforged.neoforge.network.PacketDistributor.sendToPlayer((net.minecraft.server.level.ServerPlayer)context.player(),snapshot)));
            registrar.playToClient(dev.thaumcraft.network.PortalScenes.Entities.TYPE,dev.thaumcraft.network.PortalScenes.Entities.CODEC,(payload,context)->dev.thaumcraft.network.PortalScenes.entityReceiver.accept(payload));
            registrar.playToClient(dev.thaumcraft.network.PortalScenes.Snapshot.TYPE,dev.thaumcraft.network.PortalScenes.Snapshot.CODEC,(payload,context)->dev.thaumcraft.network.PortalScenes.receiver.accept(payload));
        });
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.tick.LevelTickEvent.Post event)->{if(event.getLevel() instanceof net.minecraft.server.level.ServerLevel level)dev.thaumcraft.network.PortalScenes.tick(level);});
        bus.addListener((net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event)->{
            event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.Energy.BLOCK,Content.MACHINE_ENTITY,(machine,side)->GeneratorEnergy.of(machine,side));
            event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK,Content.MACHINE_ENTITY,NeoForgeIntegration::items);
        });
        bus.addListener((net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent event)->dev.thaumcraft.entity.ModSpawns.register(new dev.thaumcraft.entity.ModSpawns.Registrar() {
            @Override public <T extends net.minecraft.world.entity.Mob> void register(net.minecraft.world.entity.EntityType<T> type,net.minecraft.world.entity.SpawnPlacementType placement,net.minecraft.world.level.levelgen.Heightmap.Types height,net.minecraft.world.entity.SpawnPlacements.SpawnPredicate<T> predicate) {
                event.register(type,placement,height,predicate,net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
            }
        }));
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.tick.LevelTickEvent.Post event)->{if(event.getLevel() instanceof net.minecraft.server.level.ServerLevel level){dev.thaumcraft.world.AuraTicker.tick(level);dev.thaumcraft.world.ChunkAnchors.tick(level);}});
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStoppedEvent event)->dev.thaumcraft.gameplay.AddonData.clearServer());
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.RegisterCommandsEvent event)->dev.thaumcraft.command.ThaumcraftCommand.register(event.getDispatcher()));
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.OnDatapackSyncEvent event)->{dev.thaumcraft.gameplay.AddonData.reload(event.getPlayerList().getServer(),false);if(event.getPlayer()!=null)dev.thaumcraft.gameplay.AddonData.sync(event.getPlayer());});
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event)->{if(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)dev.thaumcraft.api.ThaumcraftKnowledge.synchronize(player);});
        bus.addListener((EntityAttributeCreationEvent event)->ModEntities.ATTRIBUTES.forEach(event::put));
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent event)->{
            if(Boolean.getBoolean("thaumcraft.smokeTest")) {
                try {
                    for(String suite:new String[]{"GeneratorEnergySmokeTests","ItemAccessSmokeTests"})
                        Class.forName("dev.thaumcraft.neoforge."+suite).getMethod("run",net.minecraft.server.MinecraftServer.class).invoke(null,event.getServer());
                }
                catch(ReflectiveOperationException exception) {throw new IllegalStateException("Thaumcraft NeoForge smoke test failed",exception);}
            }
            Thaumcraft.serverStarted(event.getServer());
        });
    }
    private static void register(RegisterEvent event) {
        event.register(Registries.RECIPE_TYPE,helper->helper.register(Thaumcraft.id("infusion"),dev.thaumcraft.recipe.InfusionRecipeData.TYPE));
        event.register(Registries.RECIPE_SERIALIZER,helper->helper.register(Thaumcraft.id("infusion"),dev.thaumcraft.recipe.InfusionRecipeData.SERIALIZER));
        event.register(Registries.TRIGGER_TYPE,helper->{
            helper.register(Thaumcraft.id("research_learned"),dev.thaumcraft.gameplay.ProgressTriggers.RESEARCH_LEARNED);
            helper.register(Thaumcraft.id("infusion_completed"),dev.thaumcraft.gameplay.ProgressTriggers.INFUSION_COMPLETED);
        });
        event.register(Registries.LOOT_CONDITION_TYPE,helper->helper.register(Thaumcraft.id("knows_research"),dev.thaumcraft.gameplay.KnowsResearchCondition.CODEC));
        event.register(net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS,helper->helper.register(Thaumcraft.id("artifacts"),ArtifactLootModifier.CODEC));
        event.register(Registries.TICKET_TYPE,helper->{Thaumcraft.ANCHOR_TICKET=new net.minecraft.server.level.TicketType(60,15);helper.register(Thaumcraft.id("seal_anchor"),Thaumcraft.ANCHOR_TICKET);Thaumcraft.PORTAL_VIEW_TICKET=Thaumcraft.createPortalViewTicket();helper.register(Thaumcraft.id("portal_view"),Thaumcraft.PORTAL_VIEW_TICKET);});
        event.register(Registries.SOUND_EVENT,helper->dev.thaumcraft.content.ModSounds.register((id,sound)->helper.register(Thaumcraft.id(id),sound)));
        event.register(Registries.BLOCK,helper->Content.registerBlocks((id,value)->helper.register(Thaumcraft.id(id),value)));
        event.register(Registries.ITEM,helper->Content.registerItems((id,value)->helper.register(Thaumcraft.id(id),value)));
        event.register(Registries.BLOCK_ENTITY_TYPE,helper->{
            Content.MACHINE_ENTITY=new BlockEntityType<>(MachineBlockEntity::new,Set.of(Content.machineBlocks()));
            helper.register(Thaumcraft.id("machine"),Content.MACHINE_ENTITY);
            Content.VISUAL_ENTITY=new BlockEntityType<>(dev.thaumcraft.world.VisualBlockEntity::new,Content.visualBlocks());
            helper.register(Thaumcraft.id("visual"),Content.VISUAL_ENTITY);
            Content.MONOLITH_ENTITY=new BlockEntityType<>(dev.thaumcraft.world.MonolithBlockEntity::new,java.util.Set.of(Content.block("eldritch_core")));
            helper.register(Thaumcraft.id("monolith"),Content.MONOLITH_ENTITY);
        });
        event.register(Registries.FEATURE,helper->dev.thaumcraft.world.ArcaneFeatures.register((id,value)->helper.register(Thaumcraft.id(id),value)));
        event.register(Registries.MENU,helper->{helper.register(Thaumcraft.id("machine"),Content.createMachineMenu());Content.registerMachineMenus((id,type)->helper.register(Thaumcraft.id(id),type));});
        event.register(Registries.MENU,helper->helper.register(Thaumcraft.id("void_storage"),Content.createVoidMenu()));
        event.register(Registries.MENU,helper->{helper.register(Thaumcraft.id("trunk"),Content.createTrunkMenu(false));helper.register(Thaumcraft.id("roomy_trunk"),Content.createTrunkMenu(true));});
        event.register(Registries.ENTITY_TYPE,helper->ModEntities.register((id,value)->helper.register(Thaumcraft.id(id),value)));
        event.register(Registries.CREATIVE_MODE_TAB,helper->helper.register(Thaumcraft.id("main"),Content.createTab()));
    }
}
