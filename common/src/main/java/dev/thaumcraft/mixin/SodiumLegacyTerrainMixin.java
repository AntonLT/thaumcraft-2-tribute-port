package dev.thaumcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.thaumcraft.client.legacy.LegacyTerrain;
import net.caffeinemc.mods.sodium.api.util.ColorMixer;
import net.caffeinemc.mods.sodium.client.render.model.MutableQuadViewImpl;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets="net.caffeinemc.mods.sodium.client.render.model.AbstractBlockRenderContext",remap=false)
public class SodiumLegacyTerrainMixin {
    @Shadow protected BlockState state;
    @Shadow protected BlockPos pos;
    @WrapOperation(method="bufferDefaultModel",at=@At(value="INVOKE",target="Lnet/caffeinemc/mods/sodium/client/render/model/MutableQuadViewImpl;fromBakedQuad(Lnet/minecraft/client/resources/model/geometry/BakedQuad;)Lnet/caffeinemc/mods/sodium/client/render/model/MutableQuadViewImpl;"))
    private MutableQuadViewImpl thaumcraft$legacyFace(MutableQuadViewImpl quad,BakedQuad baked,Operation<MutableQuadViewImpl> original){
        var result=original.call(quad,LegacyTerrain.texture(baked,state,pos));
        int tint=LegacyTerrain.tint(state);
        if(tint!=-1)for(int i=0;i<4;i++)result.setColor(i,ColorMixer.mulComponentWise(result.baseColor(i),tint));
        return result;
    }
}
