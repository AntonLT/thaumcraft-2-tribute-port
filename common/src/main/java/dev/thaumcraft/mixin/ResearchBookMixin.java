package dev.thaumcraft.mixin;

import dev.thaumcraft.client.ResearchScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundOpenBookPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ResearchBookMixin {
    @Inject(method="handleOpenBook",at=@At("RETURN"))
    private void thaumcraft$openBook(ClientboundOpenBookPacket packet,CallbackInfo ci){
        var minecraft=Minecraft.getInstance();
        if(minecraft.player!=null)ResearchScreen.open(minecraft,minecraft.player.getItemInHand(packet.getHand()));
    }
}
