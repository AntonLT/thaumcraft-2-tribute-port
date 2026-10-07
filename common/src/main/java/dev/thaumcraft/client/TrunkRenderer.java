package dev.thaumcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.thaumcraft.Thaumcraft;
import dev.thaumcraft.entity.TravelingTrunk;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public final class TrunkRenderer extends MobRenderer<TravelingTrunk,TrunkRenderer.State,TrunkRenderer.Model> {
    public static final class State extends LivingEntityRenderState {public float lid,squish;public int upgrades;}
    public TrunkRenderer(EntityRendererProvider.Context context){super(context,new Model(),.5f);}
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(TravelingTrunk entity,State state,float partial){super.extractRenderState(entity,state,partial);state.lid=entity.lid(partial);state.squish=entity.squish(partial);state.upgrades=entity.upgradeMask();}
    @Override public Identifier getTextureLocation(State state){return Thaumcraft.id("textures/legacy/trunk"+((state.upgrades&4)!=0?"angry":(state.upgrades&32)!=0?"roomy":(state.upgrades&2)!=0?"greedy":"")+".png");}
    @Override protected void scale(State state,PoseStack poses){float w=1/(state.squish/2+1)/1.4f,size=2/((state.upgrades&32)!=0?1.4f:1.5f);poses.scale(w*size,.5f/w*size,w*size);poses.translate(-.45,.45,-.45);}
    public static final class Model extends EntityModel<State> {
        Model(){super(build());}
        private static ModelPart build(){
            var mesh=new MeshDefinition();var root=mesh.getRoot();
            root.addOrReplaceChild("lid",CubeListBuilder.create().texOffs(0,0).addBox(0,-5,-14,14,5,14),PartPose.offset(1,7,15));
            root.addOrReplaceChild("knob",CubeListBuilder.create().texOffs(0,0).addBox(-1,-2,-15,2,4,1),PartPose.offset(8,7,15));
            root.addOrReplaceChild("base",CubeListBuilder.create().texOffs(0,19).addBox(0,0,0,14,10,14),PartPose.offset(1,6,1));
            return LayerDefinition.create(mesh,64,64).bakeRoot();
        }
        @Override public void setupAnim(State state){super.setupAnim(state);float open=1-state.lid,angle=-(1-open*open*open)*(float)Math.PI/2;root().getChild("lid").xRot=angle;root().getChild("knob").xRot=angle;}
    }
}
