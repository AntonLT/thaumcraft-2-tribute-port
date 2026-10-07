package dev.thaumcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.thaumcraft.client.legacy.LegacyDraw;
import dev.thaumcraft.client.legacy.LegacyVisuals;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import java.util.List;

/** Original meshes are extracted once into a render state; submission never reads the world. */
public final class LegacyBlockRenderer<T extends BlockEntity> implements BlockEntityRenderer<T,LegacyBlockRenderer.State> {
    public static final class State extends BlockEntityRenderState {public List<LegacyDraw.Batch> batches=List.of();}
    public LegacyBlockRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public int getViewDistance(){return Math.min(400,net.minecraft.client.Minecraft.getInstance().options.getEffectiveRenderDistance()*16);}
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(T entity,State state,float partial,Vec3 camera,ModelFeatureRenderer.CrumblingOverlay breaking){
        BlockEntityRenderer.super.extractRenderState(entity,state,partial,camera,breaking);state.batches=LegacyVisuals.render(entity,partial,state.lightCoords);
    }
    @Override public void submit(State state,PoseStack poses,SubmitNodeCollector collector,CameraRenderState camera){LegacyDraw.submit(state.batches,poses,collector);}
}
