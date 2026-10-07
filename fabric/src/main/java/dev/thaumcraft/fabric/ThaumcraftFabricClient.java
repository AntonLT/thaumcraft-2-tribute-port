package dev.thaumcraft.fabric;

import dev.thaumcraft.client.ClientRegistration;
import dev.thaumcraft.client.MachineScreen;
import dev.thaumcraft.content.Content;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.gui.screens.MenuScreens;

public final class ThaumcraftFabricClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        ClientRegistration.init();
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(dev.thaumcraft.network.SoulAbsorption.TYPE,(payload,context)->dev.thaumcraft.client.legacy.LegacyVisuals.soulAbsorption(payload));
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(dev.thaumcraft.network.GeneratorArc.TYPE,(payload,context)->dev.thaumcraft.client.legacy.LegacyVisuals.generatorArc(payload));
        dev.thaumcraft.network.EquipmentEffect.receiver=dev.thaumcraft.client.legacy.LegacyVisuals::equipmentEffect;
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(dev.thaumcraft.network.EquipmentEffect.TYPE,(payload,context)->dev.thaumcraft.network.EquipmentEffect.receiver.accept(payload));
        dev.thaumcraft.network.SealEffect.receiver=dev.thaumcraft.client.legacy.SealEffects::receive;
        dev.thaumcraft.network.BoreEffect.receiver=dev.thaumcraft.client.legacy.BoreEffects::receive;
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(dev.thaumcraft.network.SealEffect.TYPE,(payload,context)->dev.thaumcraft.network.SealEffect.receiver.accept(payload));
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(dev.thaumcraft.network.BoreEffect.TYPE,(payload,context)->dev.thaumcraft.network.BoreEffect.receiver.accept(payload));
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(dev.thaumcraft.network.CatalogSync.TYPE,(payload,context)->dev.thaumcraft.network.CatalogSync.handle(payload));
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(dev.thaumcraft.network.ResearchSync.TYPE,(payload,context)->dev.thaumcraft.network.ResearchSync.handleClient(payload));
        dev.thaumcraft.network.VoidCompassTarget.requestSender=net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking::send;
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(dev.thaumcraft.network.VoidCompassTarget.TYPE,(payload,context)->dev.thaumcraft.client.VoidCompassTexture.INSTANCE.receive(payload));
        dev.thaumcraft.network.PortalScenes.requestSender=net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking::send;
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(dev.thaumcraft.network.PortalScenes.Snapshot.TYPE,(payload,context)->dev.thaumcraft.client.legacy.PortalViews.receive(payload));
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(dev.thaumcraft.network.PortalScenes.Entities.TYPE,(payload,context)->dev.thaumcraft.client.legacy.PortalViews.receiveEntities(payload));
        MenuScreens.register(Content.MACHINE_MENU,MachineScreen::new);
        Content.MACHINE_MENUS.values().forEach(type->MenuScreens.register(type,MachineScreen::new));
        MenuScreens.register(Content.VOID_MENU,dev.thaumcraft.client.VoidScreen::new);
        MenuScreens.register(Content.TRUNK_MENU,dev.thaumcraft.client.TrunkScreen::new);
        MenuScreens.register(Content.ROOMY_TRUNK_MENU,dev.thaumcraft.client.TrunkScreen::new);
        ClientRegistration.entities(EntityRendererRegistry::register);
        net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry.register(Content.MACHINE_ENTITY,dev.thaumcraft.client.LegacyBlockRenderer::new);
        net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry.register(Content.VISUAL_ENTITY,dev.thaumcraft.client.LegacyBlockRenderer::new);
        net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry.register(Content.MONOLITH_ENTITY,dev.thaumcraft.client.LegacyBlockRenderer::new);
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client->ClientRegistration.tick());
    }
}
