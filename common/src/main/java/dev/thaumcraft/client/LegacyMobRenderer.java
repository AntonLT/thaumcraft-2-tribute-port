package dev.thaumcraft.client;

import dev.thaumcraft.Thaumcraft;
import dev.thaumcraft.entity.TaintedCreature;
import dev.thaumcraft.entity.TaintedTree;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.Monster;

/** Legacy texture coordinates avoid the incompatible modern animal skin layouts. */
public final class LegacyMobRenderer extends MobRenderer<Monster,LegacyMobRenderer.State,LegacyMobRenderer.Model> {
    public static final class State extends LivingEntityRenderState {public boolean angry,sheared; public int treeSeed; public float wing,grazeY,grazeX,shudderX,shudderY,shudderZ;}
    private final String kind;
    public LegacyMobRenderer(EntityRendererProvider.Context context,String kind) {super(context,new Model(kind),kind.equals("tree")?1:kind.equals("chicken")?.3f:kind.equals("villager")?.5f:.7f);this.kind=kind;
        if(kind.equals("sheep"))addLayer(new RenderLayer<State,Model>(this) {
            private final Model fleece=new Model("fleece");
            @Override public void submit(PoseStack poses,SubmitNodeCollector collector,int light,State state,float yaw,float pitch) {
                if(!state.sheared)coloredCutoutModelCopyLayerRender(fleece,Thaumcraft.id("textures/legacy/sheep_fur.png"),poses,collector,light,state,-1,0);
            }
        });
    }
    @Override public State createRenderState() {return new State();}
    @Override public void extractRenderState(Monster entity,State state,float partialTicks) {super.extractRenderState(entity,state,partialTicks);state.angry=false;state.shudderX=state.shudderY=state.shudderZ=0;
        if(entity instanceof TaintedTree tree) {
            state.angry=tree.isAngry();state.treeSeed=tree.modelSeed();
            if(tree.shudder()>0){var random=new java.util.Random(((long)entity.getId()<<32)^entity.tickCount);state.shudderX=(random.nextFloat()-random.nextFloat())*.15f;state.shudderY=(random.nextFloat()-random.nextFloat())*.15f;state.shudderZ=(random.nextFloat()-random.nextFloat())*.15f;}
        }
        if(entity instanceof TaintedCreature creature){state.sheared=creature.isSheared();state.wing=creature.wingRotation(partialTicks);state.grazeY=creature.grazePosition(partialTicks);state.grazeX=creature.grazeAngle(partialTicks);}}
    @Override public Identifier getTextureLocation(State state) {return Thaumcraft.id("textures/legacy/"+(kind.equals("tree")&&state.angry?"treeangry":kind)+".png");}
    @Override protected void scale(State state,PoseStack poses){if(kind.equals("villager"))poses.scale(.9375f,.9375f,.9375f);}
    @Override public void submit(State state,PoseStack poses,SubmitNodeCollector collector,CameraRenderState camera){
        poses.pushPose();poses.translate(state.shudderX,state.shudderY,state.shudderZ);super.submit(state,poses,collector,camera);poses.popPose();
    }
    public static final class Model extends EntityModel<State> {
        private final boolean tree,sheep;
        Model(String kind) {super(build(kind));tree=kind.equals("tree");sheep=kind.equals("sheep")||kind.equals("fleece");}
        private static PartDefinition box(PartDefinition root,String name,int u,int v,float x,float y,float z,float w,float h,float d,PartPose pose) {
            return root.addOrReplaceChild(name,CubeListBuilder.create().texOffs(u,v).addBox(x,y,z,w,h,d),pose);
        }
        private static ModelPart build(String kind) {
            var mesh=new MeshDefinition();var root=mesh.getRoot();int width=64,height=32;
            switch(kind) {
                case "tree" -> {
                    width=256;height=128;
                    for(int seed=0;seed<10;seed++) {
                    var variant=root.addOrReplaceChild("tree"+seed,CubeListBuilder.create(),PartPose.ZERO);
                    box(variant,"trunk",0,0,-8,-32,-8,16,64,16,PartPose.offset(0,-8,0));
                    var random=new java.util.Random(seed);int limit=5-random.nextInt(2)-3,max=1+random.nextInt(6-limit),radius=0,index=0;
                    for(int y=5;y>=limit;y--) {
                        for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++)if(Math.abs(x)!=radius||Math.abs(z)!=radius||radius<=0)
                            box(variant,"leaves"+index++,64,0,-8,-8,-8,16,16,16,PartPose.offsetAndRotation(-x*16,-16-y*16,-z*16,random.nextInt(2)*(float)Math.PI,random.nextInt(2)*(float)Math.PI,random.nextInt(2)*(float)Math.PI));
                        if(radius>=1&&y==limit+1)radius--;else if(radius<max)radius++;
                    }
                    }
                }
                case "villager" -> {
                    height=64;
                    box(root,"head",0,0,-4,-10,-4,8,10,8,PartPose.ZERO);
                    box(root.getChild("head"),"nose",24,0,-1,-3,-6,2,4,2,PartPose.ZERO);
                    box(root,"body",16,20,-4,0,-3,8,12,6,PartPose.ZERO);
                    box(root,"robe",0,38,-4,0,-3,8,20,6,PartPose.ZERO);
                    box(root,"arms",44,22,-8,0,-2,4,8,4,PartPose.offsetAndRotation(0,3,-1,-0.75f,0,0));
                    box(root,"arm2",44,22,4,0,-2,4,8,4,PartPose.offsetAndRotation(0,3,-1,-0.75f,0,0));
                    box(root,"fold",40,38,-4,4,-2,8,4,4,PartPose.offsetAndRotation(0,3,-1,-0.75f,0,0));
                    box(root,"leg0",0,22,-2,0,-2,4,12,4,PartPose.offset(-2,12,0));
                    box(root,"leg1",0,22,-2,0,-2,4,12,4,PartPose.offset(2,12,0));
                }
                case "chicken" -> {
                    box(root,"head",0,0,-2,-6,-2,4,6,3,PartPose.offset(0,15,-4));
                    box(root.getChild("head"),"beak",14,0,-2,-4,-4,4,2,2,PartPose.ZERO);
                    box(root.getChild("head"),"wattle",14,4,-1,-2,-3,2,2,2,PartPose.ZERO);
                    box(root,"body",0,9,-3,-4,-3,6,8,6,PartPose.offsetAndRotation(0,16,0,(float)Math.PI/2,0,0));
                    box(root,"leg0",26,0,-1,0,-3,3,5,3,PartPose.offset(-2,19,1));
                    box(root,"leg1",26,0,-1,0,-3,3,5,3,PartPose.offset(1,19,1));
                    box(root,"wing0",24,13,0,0,-3,1,4,6,PartPose.offset(-4,13,0));
                    box(root,"wing1",24,13,-1,0,-3,1,4,6,PartPose.offset(4,13,0));
                }
                default -> {
                    boolean pig=kind.equals("pig"),cow=kind.equals("cow");
                    int leg=pig?6:12;
                    if(cow) {
                        box(root,"head",0,0,-4,-4,-6,8,8,6,PartPose.offset(0,4,-8));
                        box(root,"body",18,4,-6,-10,-7,12,18,10,PartPose.offsetAndRotation(0,5,2,(float)Math.PI/2,0,0));
                        box(root.getChild("head"),"horn0",22,0,-5,-5,-5,1,3,1,PartPose.ZERO);
                        box(root.getChild("head"),"horn1",22,0,4,-5,-5,1,3,1,PartPose.ZERO);
                    } else if(pig) {
                        box(root,"head",0,0,-4,-4,-8,8,8,8,PartPose.offset(0,12,-6));
                        box(root.getChild("head"),"snout",16,16,-2,-1,-9,4,3,1,PartPose.ZERO);
                        box(root,"body",28,8,-5,-10,-4,10,16,8,PartPose.offsetAndRotation(0,11,2,(float)Math.PI/2,0,0));
                    } else {
                        box(root,"head",0,0,-3,-4,-6,6,6,8,PartPose.offset(0,6,-8));
                        box(root,"body",28,8,-4,-10,-7,8,16,6,PartPose.offsetAndRotation(0,5,2,(float)Math.PI/2,0,0));
                    }
                    if(kind.equals("fleece")) {
                        root.addOrReplaceChild("head",CubeListBuilder.create().texOffs(0,0).addBox(-3,-4,-4,6,6,6,new CubeDeformation(.6f)),PartPose.offset(0,6,-8));
                        root.addOrReplaceChild("body",CubeListBuilder.create().texOffs(28,8).addBox(-4,-10,-7,8,16,6,new CubeDeformation(1.75f)),PartPose.offsetAndRotation(0,5,2,(float)Math.PI/2,0,0));
                        for(int i=0;i<4;i++)root.addOrReplaceChild("leg"+i,CubeListBuilder.create().texOffs(0,16).addBox(-2,0,-2,4,6,4,new CubeDeformation(.5f)),PartPose.offset(i%2==0?-3:3,12,i<2?7:-5));
                    } else for(int i=0;i<4;i++)box(root,"leg"+i,0,16,-2,0,-2,4,leg,4,PartPose.offset(i%2==0?-3:3,24-leg,i<2?7:-5));
                }
            }
            return LayerDefinition.create(mesh,width,height).bakeRoot();
        }
        @Override public void setupAnim(State state) {
            super.setupAnim(state);if(tree){for(int seed=0;seed<10;seed++)root().getChild("tree"+seed).visible=seed==state.treeSeed;return;}
            if(root().hasChild("head")) {var head=root().getChild("head");head.xRot=state.xRot*(float)Math.PI/180;head.yRot=state.yRot*(float)Math.PI/180;}
            if(sheep){root().getChild("head").y=6+state.grazeY*9;root().getChild("head").xRot=state.grazeX;}
            if(root().hasChild("wing0")){root().getChild("wing0").zRot=state.wing;root().getChild("wing1").zRot=-state.wing;}
            for(int i=0;i<4;i++)if(root().hasChild("leg"+i))root().getChild("leg"+i).xRot=(float)Math.cos(state.walkAnimationPos*0.6662f+(i%3==0?0:Math.PI))*1.4f*state.walkAnimationSpeed;
        }
    }
}
