package dev.thaumcraft.world;

import dev.thaumcraft.gameplay.ArcaneWorldData;
import net.minecraft.server.level.ServerLevel;


public final class AuraTicker {
    private AuraTicker() {}
    public static void tick(ServerLevel level) {
        dev.thaumcraft.Thaumcraft.tickData(level);
        dev.thaumcraft.gameplay.ArcaneEnchantments.tick(level);
        dev.thaumcraft.item.ElementalTools.tick(level);
        dev.thaumcraft.item.EqualTrade.tick(level);
        var data=ArcaneWorldData.get(level);
        for(var player:level.players())data.aura(level,player.blockPosition());
        dev.thaumcraft.item.ArcanaItem.recordTaggedHelmets(level);
        data.tickAura(level);
    }
}
