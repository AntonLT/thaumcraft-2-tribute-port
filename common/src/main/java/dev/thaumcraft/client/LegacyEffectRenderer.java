package dev.thaumcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.thaumcraft.client.legacy.LegacyDraw;
import dev.thaumcraft.client.legacy.LegacyEntityVisuals;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import java.util.List;

public final class LegacyEffectRenderer<T extends Entity> extends EntityRenderer<T,LegacyEffectRenderer.State> {
    public static final class State extends EntityRenderState {public List<LegacyDraw.Batch> batches=List.of();}
    public LegacyEffectRenderer(EntityRendererProvider.Context context,boolean wisp){super(context);shadowRadius=wisp?0:.1f;}
    @Override public State createRenderState(){return new State();}
    @Override protected net.minecraft.world.phys.AABB getBoundingBoxForCulling(T entity){return entity instanceof dev.thaumcraft.entity.RelicProjectile relic&&relic.mode()==0&&relic.visualAge()>=60?entity.getBoundingBox().inflate(12):entity.getBoundingBox().inflate(.6);}
    @Override public void extractRenderState(T entity,State state,float partial){super.extractRenderState(entity,state,partial);state.batches=LegacyEntityVisuals.effects(entity,partial);}
    @Override public void submit(State state,PoseStack poses,SubmitNodeCollector collector,CameraRenderState camera){if(!state.isInvisible)LegacyDraw.submit(state.batches,poses,collector);super.submit(state,poses,collector,camera);}
}
