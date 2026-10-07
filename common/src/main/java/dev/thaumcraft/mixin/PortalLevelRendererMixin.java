package dev.thaumcraft.mixin;

import dev.thaumcraft.client.legacy.PortalViews;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class PortalLevelRendererMixin {
    @Inject(method="setSectionDirty(IIIZ)V",at=@At("TAIL"))
    private void thaumcraft$portalDirty(int x,int y,int z,boolean player,CallbackInfo ci){PortalViews.dirty((LevelRenderer)(Object)this,x,y,z);}
    @Inject(method="onResourceManagerReload",at=@At("HEAD"))
    private void thaumcraft$portalReload(ResourceManager resources,CallbackInfo ci){if(!PortalViews.active()&&!PortalViews.constructing())PortalViews.reset();}
}
