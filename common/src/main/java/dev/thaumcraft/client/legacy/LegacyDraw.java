package dev.thaumcraft.client.legacy;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.thaumcraft.Thaumcraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3d;
import org.joml.Matrix4d;
import org.joml.Vector3f;
import org.joml.Vector3d;
import org.joml.Vector4f;
import org.joml.Vector4d;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/** Captures the old immediate drawing API into immutable modern render submissions. */
public final class LegacyDraw {
    public record Vertex(float x,float y,float z,float u,float v,int color,int light,float nx,float ny,float nz) {}
    public record Batch(Identifier texture,int sourceBlend,int blend,boolean depthWrite,boolean cull,List<Vertex> vertices) {}
    private final List<Batch> batches=new ArrayList<>();
    private final ArrayDeque<Matrix4d> model=new ArrayDeque<>(),texture=new ArrayDeque<>();
    private final Vector4d[] texPlanes={new Vector4d(1,0,0,0),new Vector4d(0,1,0,0),new Vector4d(0,0,1,0),new Vector4d(0,0,0,1)};
    private final boolean[] texGen=new boolean[4];
    private final BlockPos origin;
    private boolean textureMode,blending,depthWrite=true,textured=true,cull;
    private int sourceBlend=770,destinationBlend=771,light,mode=7;
    private float red=1,green=1,blue=1,alpha=1,nx,ny=1,nz;
    private Identifier bound=Thaumcraft.id("textures/legacy/blocks.png");
    private List<Vertex> vertices=new ArrayList<>();
    private double offsetX,offsetY,offsetZ;

    public LegacyDraw(BlockPos origin,int light) {this.origin=origin;this.light=light;model.push(new Matrix4d());texture.push(new Matrix4d());}
    public void offset(double x,double y,double z){offsetX=x;offsetY=y;offsetZ=z;}
    private ArrayDeque<Matrix4d> stack(){return textureMode?texture:model;}
    public Matrix4d matrix(){return stack().peek();}
    public void push(){stack().push(new Matrix4d(matrix()));}
    public void pop(){if(stack().size()>1)stack().pop();else matrix().identity();}
    public void matrixMode(int mode){textureMode=mode==5890;}
    public void enable(int capability,boolean value){if(capability==3553)textured=value;else if(capability==3042)blending=value;else if(capability>=3168&&capability<=3171)texGen[capability-3168]=value;}
    public void plane(int coordinate,int mode,Vector4f value){
        // GL_EYE_PLANE is transformed at definition time, not when vertices are emitted.
        Vector4d plane=new Vector4d(value);
        if(mode==9474)plane=new Matrix4d(model.peek()).invert().transpose().transform(plane);
        texPlanes[coordinate-8192]=plane;
    }
    public void bind(Identifier texture){bound=texture;}
    public void blend(int source,int destination){sourceBlend=source;destinationBlend=destination;}
    public void depthWrite(boolean value){depthWrite=value;}
    public void cull(boolean value){cull=value;}
    public void color(float r,float g,float b,float a){red=r;green=g;blue=b;alpha=a;}
    public void normal(float x,float y,float z){nx=x;ny=y;nz=z;}
    public void light(int value){light=value<=255?((Math.clamp(value,0,240)&240)<<16)|(Math.clamp(value,0,240)&240):value;}
    public int light(){return light;}
    public void begin(int primitive){mode=primitive;vertices=new ArrayList<>();}
    public void vertex(double x,double y,double z,double u,double v){
        Vector4d source=new Vector4d(x,y,z,1);
        Vector3d position=model.peek().transformPosition(x,y,z,new Vector3d());
        Vector4d uv=new Vector4d(u,v,0,1);
        for(int i=0;i<4;i++)if(texGen[i])uv.setComponent(i,texPlanes[i].dot(source));
        texture.peek().transform(uv);
        double q=Math.abs(uv.w)>1.0e-6?uv.w:1;
        Vector3d normal=new Matrix3d(model.peek()).invert().transpose().transform(new Vector3d(nx,ny,nz));
        if(!Double.isFinite(normal.lengthSquared())||normal.lengthSquared()<1.0e-10)normal.set(0,1,0);else normal.normalize();
        if(!Double.isFinite(position.x+position.y+position.z+uv.x+uv.y))return;
        int a=Math.clamp((int)(alpha*255),0,255),r=Math.clamp((int)(red*255),0,255),g=Math.clamp((int)(green*255),0,255),b=Math.clamp((int)(blue*255),0,255);
        vertices.add(new Vertex((float)(position.x+offsetX-origin.getX()),(float)(position.y+offsetY-origin.getY()),(float)(position.z+offsetZ-origin.getZ()),(float)(uv.x/q),(float)(uv.y/q),(a<<24)|(r<<16)|(g<<8)|b,light,(float)normal.x,(float)normal.y,(float)normal.z));
    }
    public void end(){
        // Keep the original stencil aperture in geometry too: Iris replaces the
        // fragment program, so the custom portal shader's discard cannot clip it.
        if(bound.getNamespace().equals(Thaumcraft.MOD_ID)&&bound.getPath().startsWith("portal/")&&vertices.size()==4){
            Vertex a=vertices.get(0),b=vertices.get(1),d=vertices.get(3),center=portalPoint(a,b,d,.5,.5);
            List<Vertex> aperture=new ArrayList<>(40);
            for(int i=0;i<10;i++){
                Vertex first=portalPoint(a,b,d,.5+.5*Math.cos(i*Math.PI/5),.5+.5*Math.sin(i*Math.PI/5));
                Vertex next=portalPoint(a,b,d,.5+.5*Math.cos((i+1)*Math.PI/5),.5+.5*Math.sin((i+1)*Math.PI/5));
                aperture.add(center);aperture.add(first);aperture.add(next);aperture.add(next);
            }
            vertices=aperture;
        }
        if(mode==6&&vertices.size()>=3){
            List<Vertex> quads=new ArrayList<>();for(int i=1;i<vertices.size()-1;i++){quads.add(vertices.get(0));quads.add(vertices.get(i));quads.add(vertices.get(i+1));quads.add(vertices.get(i+1));}vertices=quads;
        }
        if(!vertices.isEmpty()&&vertices.size()%4==0)batches.add(new Batch(textured?bound:Thaumcraft.id("textures/legacy/white.png"),sourceBlend,blending?destinationBlend:0,depthWrite,cull,List.copyOf(vertices)));
        vertices=new ArrayList<>();
    }
    private static Vertex portalPoint(Vertex a,Vertex b,Vertex d,double u,double v){
        return new Vertex((float)(a.x+(b.x-a.x)*u+(d.x-a.x)*v),(float)(a.y+(b.y-a.y)*u+(d.y-a.y)*v),(float)(a.z+(b.z-a.z)*u+(d.z-a.z)*v),
            (float)(a.u+(b.u-a.u)*u+(d.u-a.u)*v),(float)(a.v+(b.v-a.v)*u+(d.v-a.v)*v),a.color,a.light,a.nx,a.ny,a.nz);
    }
    public List<Batch> finish(){
        var textures=net.minecraft.client.Minecraft.getInstance().getTextureManager();
        List<Batch> merged=new ArrayList<>();Batch previous=null;List<Vertex> combined=new ArrayList<>();
        for(Batch batch:batches){
            if(previous!=null&&(!previous.texture.equals(batch.texture)||previous.sourceBlend!=batch.sourceBlend||previous.blend!=batch.blend||previous.depthWrite!=batch.depthWrite||previous.cull!=batch.cull)){
                merged.add(new Batch(previous.texture,previous.sourceBlend,previous.blend,previous.depthWrite,previous.cull,List.copyOf(combined)));combined.clear();
            }
            textures.getTexture(batch.texture);combined.addAll(batch.vertices);previous=batch;
        }
        if(previous!=null)merged.add(new Batch(previous.texture,previous.sourceBlend,previous.blend,previous.depthWrite,previous.cull,List.copyOf(combined)));
        return List.copyOf(merged);
    }
    public static void submit(List<Batch> batches,PoseStack poses,SubmitNodeCollector collector){
        // Preserve immediate-mode layering; grouping by material lets translucent
        // frames write depth before the opaque void surface behind them.
        int order=0;
        for(Batch batch:batches){
            RenderType type=LegacyPipelines.material(batch.texture,batch.sourceBlend,batch.blend,batch.depthWrite,batch.cull);
            collector.order(order++).submitCustomGeometry(poses,type,(pose,buffer)->{
                for(Vertex vertex:batch.vertices)buffer.addVertex(pose,vertex.x,vertex.y,vertex.z).setColor(vertex.color).setUv(vertex.u,vertex.v)
                        .setOverlay(OverlayTexture.NO_OVERLAY).setLight(vertex.light).setNormal(pose,vertex.nx,vertex.ny,vertex.nz);
            });
        }
    }
}
