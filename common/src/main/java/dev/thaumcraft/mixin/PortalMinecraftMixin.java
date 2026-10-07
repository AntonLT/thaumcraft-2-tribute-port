package dev.thaumcraft.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import dev.thaumcraft.client.legacy.PortalViews;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class PortalMinecraftMixin {
    @Inject(method="getMainRenderTarget",at=@At("HEAD"),cancellable=true)
    private void thaumcraft$portalTarget(CallbackInfoReturnable<RenderTarget> ci){if(PortalViews.active())ci.setReturnValue(PortalViews.target());}
}
