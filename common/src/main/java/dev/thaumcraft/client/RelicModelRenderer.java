package dev.thaumcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.thaumcraft.client.legacy.LegacyDraw;
import dev.thaumcraft.client.legacy.LegacyEntityVisuals;
import dev.thaumcraft.entity.CarpetEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import java.util.List;

/** The original seven wave sections, four tassels and asymmetric carpet scale. */
public final class RelicModelRenderer extends EntityRenderer<CarpetEntity,RelicModelRenderer.State> {
    public static final class State extends EntityRenderState {public List<LegacyDraw.Batch> batches=List.of();}
    public RelicModelRenderer(EntityRendererProvider.Context context){super(context);shadowRadius=.5f;}
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(CarpetEntity entity,State state,float partial){super.extractRenderState(entity,state,partial);state.batches=LegacyEntityVisuals.carpet(entity,partial,state.lightCoords);}
    @Override public void submit(State state,PoseStack poses,SubmitNodeCollector collector,CameraRenderState camera){if(!state.isInvisible)LegacyDraw.submit(state.batches,poses,collector);super.submit(state,poses,collector,camera);}
}
