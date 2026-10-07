package dev.alts.tc2tppatches.fabric;

import dev.alts.tc2tppatches.ClientVisKnowledge;
import dev.alts.tc2tppatches.VisKnowledgeSync;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class AltsPatchesFabricClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(VisKnowledgeSync.TYPE,(payload,context)->ClientVisKnowledge.receive(payload));
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->ClientVisKnowledge.clear());
        ItemTooltipCallback.EVENT.register((stack,context,flag,lines)->ClientVisKnowledge.appendTooltip(stack,lines::add));
    }
}
