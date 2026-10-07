package dev.thaumcraft.mixin;

import dev.thaumcraft.client.VoidCompassTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TextureAtlas.class)
abstract class VoidCompassTextureMixin {
    @Shadow private TextureAtlasSprite missingSprite;
    @Inject(method="tick",at=@At("TAIL"))
    private void thaumcraft$animateCompass(CallbackInfo ci){
        var atlas=(TextureAtlas)(Object)this;
        if(missingSprite!=null&&atlas.location().equals(TextureAtlas.LOCATION_ITEMS))VoidCompassTexture.INSTANCE.tick(atlas);
    }
}
