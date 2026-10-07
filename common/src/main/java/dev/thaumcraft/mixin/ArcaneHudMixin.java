package dev.thaumcraft.mixin;

import dev.thaumcraft.client.ArcaneHud;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class ArcaneHudMixin {
    @Inject(method="extractRenderState",at=@At("RETURN"))
    private void thaumcraft$hud(GuiGraphicsExtractor graphics,DeltaTracker delta,CallbackInfo ci){ArcaneHud.extract(graphics);}
}
