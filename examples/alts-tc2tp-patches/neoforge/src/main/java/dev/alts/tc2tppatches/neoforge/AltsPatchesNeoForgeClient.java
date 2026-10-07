package dev.alts.tc2tppatches.neoforge;

import dev.alts.tc2tppatches.AltsPatches;
import dev.alts.tc2tppatches.ClientVisKnowledge;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/** Client-only hooks, kept apart so a dedicated server never loads client event classes. */
@Mod(value=AltsPatches.MOD_ID,dist=Dist.CLIENT)
public final class AltsPatchesNeoForgeClient {
    public AltsPatchesNeoForgeClient(IEventBus bus) {
        NeoForge.EVENT_BUS.addListener((ItemTooltipEvent event)->ClientVisKnowledge.appendTooltip(event.getItemStack(),event.getToolTip()::add));
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event)->ClientVisKnowledge.clear());
        // Curios builds renderers when entity layers are added, after client setup has registered them.
        if(ModList.get().isLoaded("curios"))bus.addListener((FMLClientSetupEvent event)->GogglesCurioRenderer.register());
    }
}
