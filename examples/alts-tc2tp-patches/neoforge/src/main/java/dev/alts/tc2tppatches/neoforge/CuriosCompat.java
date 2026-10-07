package dev.alts.tc2tppatches.neoforge;

import dev.thaumcraft.api.IntegrationHooks;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

import java.util.List;

/** Reports equipped curios as worn, so goggles in a curio slot show the aura HUD. Only loaded when Curios is installed. */
final class CuriosCompat {
    private CuriosCompat() {}
    static void register() {
        // findCurios skips disabled slots, so a locked slot reveals nothing.
        IntegrationHooks.registerWornItems(player->CuriosApi.getCuriosInventory(player)
                .map(curios->curios.findCurios(stack->true).stream().map(SlotResult::stack).toList()).orElse(List.of()));
    }
}
