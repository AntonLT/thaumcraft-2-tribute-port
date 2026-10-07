package dev.thaumcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.thaumcraft.Thaumcraft;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.slime.SlimeModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.SlimeRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

public final class ThaumSlimeRenderer extends SlimeRenderer {
    private static final Identifier SKIN=Thaumcraft.id("textures/legacy/tslime.png"),TAINTED_SKIN=Thaumcraft.id("textures/legacy/taintslime.png");
    public static final class State extends SlimeRenderState {public boolean tainted;}
    @Override public SlimeRenderState createRenderState(){return new State();}
    @Override public void extractRenderState(net.minecraft.world.entity.monster.Slime entity,SlimeRenderState state,float partial){
        super.extractRenderState(entity,state,partial);((State)state).tainted=entity instanceof dev.thaumcraft.entity.ThaumSlime slime&&slime.tainted();
    }
    public ThaumSlimeRenderer(EntityRendererProvider.Context context){
        super(context);layers.clear();
        addLayer(new RenderLayer<SlimeRenderState,SlimeModel>(this){
            private final SlimeModel outer=new SlimeModel(context.bakeLayer(ModelLayers.SLIME_OUTER));
            @Override public void submit(PoseStack poses,SubmitNodeCollector collector,int light,SlimeRenderState state,float yaw,float pitch){
                if(!state.isInvisible||state.appearsGlowing())collector.order(1).submitModel(outer,state,poses,state.isInvisible?RenderTypes.outline(getTextureLocation(state)):RenderTypes.entityTranslucent(getTextureLocation(state)),light,LivingEntityRenderer.getOverlayCoords(state,0),state.outlineColor,null);
            }
        });
    }
    @Override public Identifier getTextureLocation(SlimeRenderState state){return state instanceof State slime&&slime.tainted?TAINTED_SKIN:SKIN;}
    @Override protected void scale(SlimeRenderState state,PoseStack poses){
        float squash=state.squish/(state.size*.5f+1),width=1/(squash+1)/1.5f,size=state.size/1.5f;
        poses.scale(width*size,.5f/width*size,width*size);
    }
}
