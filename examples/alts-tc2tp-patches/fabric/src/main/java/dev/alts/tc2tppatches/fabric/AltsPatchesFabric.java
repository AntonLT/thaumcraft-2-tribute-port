package dev.alts.tc2tppatches.fabric;

import dev.alts.tc2tppatches.AltsPatches;
import dev.alts.tc2tppatches.VisKnowledgeSync;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class AltsPatchesFabric implements ModInitializer {
    @Override public void onInitialize() {
        AltsPatches.init();
        AltsPatches.registerBlocks((id,block)->Registry.register(BuiltInRegistries.BLOCK,id,block));
        AltsPatches.registerItems((id,item)->Registry.register(BuiltInRegistries.ITEM,id,item));
        CreativeModeTabEvents.modifyOutputEvent(AltsPatches.TAB).register(output->
{
            output.insertAfter(BuiltInRegistries.ITEM.getValue(AltsPatches.CINNABAR_ORE),AltsPatches.deepslateCinnabarOreItem);
            output.insertAfter(AltsPatches.deepslateCinnabarOreItem,AltsPatches.rawCinnabar);
        });
        ResourceLoader.registerBuiltinPack(AltsPatches.id("patches"),
                FabricLoader.getInstance().getModContainer(AltsPatches.MOD_ID).orElseThrow(),PackActivationType.ALWAYS_ENABLED);
        PayloadTypeRegistry.clientboundPlay().register(VisKnowledgeSync.TYPE,VisKnowledgeSync.CODEC);
        // Send knowledge only after the client has registered its payload receiver.
        VisKnowledgeSync.sender=(player,payload)->{if(ServerPlayNetworking.canSend(player,VisKnowledgeSync.TYPE))ServerPlayNetworking.send(player,payload);};
        ServerPlayConnectionEvents.JOIN.register((handler,sender,server)->VisKnowledgeSync.sendTo(handler.player));
    }
}
