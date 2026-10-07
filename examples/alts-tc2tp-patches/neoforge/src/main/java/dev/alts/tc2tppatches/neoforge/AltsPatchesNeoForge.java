package dev.alts.tc2tppatches.neoforge;

import dev.alts.tc2tppatches.AltsPatches;
import dev.alts.tc2tppatches.ClientVisKnowledge;
import dev.alts.tc2tppatches.VisKnowledgeSync;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

@Mod(AltsPatches.MOD_ID)
public final class AltsPatchesNeoForge {
    public AltsPatchesNeoForge(IEventBus bus) {
        AltsPatches.init();
        // Curios is optional: its classes stay unresolved unless it is installed.
        if(ModList.get().isLoaded("curios"))CuriosCompat.register();
        bus.addListener((RegisterEvent event)->{
            event.register(Registries.BLOCK,helper->AltsPatches.registerBlocks(helper::register));
            event.register(Registries.ITEM,helper->AltsPatches.registerItems(helper::register));
        });
        bus.addListener((BuildCreativeModeTabContentsEvent event)->{
            if(!event.getTabKey().equals(AltsPatches.TAB))return;
            event.insertAfter(new ItemStack(BuiltInRegistries.ITEM.getValue(AltsPatches.CINNABAR_ORE)),
                    new ItemStack(AltsPatches.deepslateCinnabarOreItem),CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.insertAfter(new ItemStack(AltsPatches.deepslateCinnabarOreItem),new ItemStack(AltsPatches.rawCinnabar),CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        });
        bus.addListener((AddPackFindersEvent event)->event.addPackFinders(AltsPatches.id("resourcepacks/patches"),
                PackType.SERVER_DATA,Component.literal("Alt's TC2TP patches"),PackSource.BUILT_IN,true,Pack.Position.TOP));
        // Send knowledge only after the client has registered its payload receiver.
        VisKnowledgeSync.sender=(player,payload)->{if(player.connection.hasChannel(payload))PacketDistributor.sendToPlayer(player,payload);};
        bus.addListener((RegisterPayloadHandlersEvent event)->event.registrar("1").optional()
                .playToClient(VisKnowledgeSync.TYPE,VisKnowledgeSync.CODEC,(payload,context)->context.enqueueWork(()->ClientVisKnowledge.receive(payload))));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event)->{if(event.getEntity() instanceof ServerPlayer player)VisKnowledgeSync.sendTo(player);});
    }
}
