package dev.thaumcraft.neoforge;

import dev.thaumcraft.Thaumcraft;
import dev.thaumcraft.client.ClientRegistration;
import dev.thaumcraft.client.MachineScreen;
import dev.thaumcraft.content.Content;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@Mod(value=Thaumcraft.MOD_ID,dist=Dist.CLIENT)
public final class ThaumcraftNeoForgeClient {
    public ThaumcraftNeoForgeClient(IEventBus bus) {
        ClientRegistration.init();
        dev.thaumcraft.network.SoulAbsorption.receiver=dev.thaumcraft.client.legacy.LegacyVisuals::soulAbsorption;
        dev.thaumcraft.network.GeneratorArc.receiver=dev.thaumcraft.client.legacy.LegacyVisuals::generatorArc;
        dev.thaumcraft.network.EquipmentEffect.receiver=dev.thaumcraft.client.legacy.LegacyVisuals::equipmentEffect;
        dev.thaumcraft.network.SealEffect.receiver=dev.thaumcraft.client.legacy.SealEffects::receive;
        dev.thaumcraft.network.BoreEffect.receiver=dev.thaumcraft.client.legacy.BoreEffects::receive;
        dev.thaumcraft.network.VoidCompassTarget.requestSender=net.neoforged.neoforge.client.network.ClientPacketDistributor::sendToServer;
        dev.thaumcraft.network.VoidCompassTarget.receiver=dev.thaumcraft.client.VoidCompassTexture.INSTANCE::receive;
        dev.thaumcraft.network.PortalScenes.requestSender=net.neoforged.neoforge.client.network.ClientPacketDistributor::sendToServer;
        dev.thaumcraft.network.PortalScenes.entityReceiver=dev.thaumcraft.client.legacy.PortalViews::receiveEntities;
        dev.thaumcraft.network.PortalScenes.receiver=dev.thaumcraft.client.legacy.PortalViews::receive;
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.ClientTickEvent.Post event)->ClientRegistration.tick());
        bus.addListener((RegisterMenuScreensEvent event)->{event.register(Content.MACHINE_MENU,MachineScreen::new);Content.MACHINE_MENUS.values().forEach(type->event.register(type,MachineScreen::new));});
        bus.addListener((RegisterMenuScreensEvent event)->event.register(Content.VOID_MENU,dev.thaumcraft.client.VoidScreen::new));
        bus.addListener((RegisterMenuScreensEvent event)->{event.register(Content.TRUNK_MENU,dev.thaumcraft.client.TrunkScreen::new);event.register(Content.ROOMY_TRUNK_MENU,dev.thaumcraft.client.TrunkScreen::new);});
        bus.addListener((EntityRenderersEvent.RegisterRenderers event)->ClientRegistration.entities(event::registerEntityRenderer));
        bus.addListener((EntityRenderersEvent.RegisterRenderers event)->{
            event.registerBlockEntityRenderer(Content.MACHINE_ENTITY,dev.thaumcraft.client.LegacyBlockRenderer::new);
            event.registerBlockEntityRenderer(Content.VISUAL_ENTITY,dev.thaumcraft.client.LegacyBlockRenderer::new);
            event.registerBlockEntityRenderer(Content.MONOLITH_ENTITY,dev.thaumcraft.client.LegacyBlockRenderer::new);
        });
    }
}
