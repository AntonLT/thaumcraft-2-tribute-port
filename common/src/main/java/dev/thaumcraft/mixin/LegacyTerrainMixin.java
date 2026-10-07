package dev.thaumcraft.mixin;

import com.mojang.blaze3d.vertex.QuadInstance;
import dev.thaumcraft.client.legacy.LegacyTerrain;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ModelBlockRenderer.class)
public class LegacyTerrainMixin {
    @Redirect(method="putQuadWithTint",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/block/BlockQuadOutput;put(FFFLnet/minecraft/client/resources/model/geometry/BakedQuad;Lcom/mojang/blaze3d/vertex/QuadInstance;)V"))
    private void thaumcraft$legacyFace(BlockQuadOutput output,float x,float y,float z,BakedQuad quad,QuadInstance instance,
                                      BlockQuadOutput original,float ox,float oy,float oz,BlockAndTintGetter level,BlockState state,BlockPos pos,BakedQuad originalQuad){
        int tint=LegacyTerrain.tint(state);if(tint!=-1)instance.multiplyColor(tint);
        output.put(x,y,z,LegacyTerrain.texture(quad,state,pos),instance);
    }
}
