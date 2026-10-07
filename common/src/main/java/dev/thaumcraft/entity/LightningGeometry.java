package dev.thaumcraft.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Seeded seven-pass fractal from the original CodeChicken lightning implementation. */
public final class LightningGeometry {
    public record Point(Vec3 base,Vec3 offset) {public Vec3 position(){return base.add(offset);}}
    public static final class Segment {
        public final Point start,end;public final Vec3 diff;public final float light;public final int number,branch;
        public Segment previous,next;public Vec3 previousTangent,nextTangent;public double previousSine=1,nextSine=1;
        Segment(Point start,Point end,float light,int number,int branch){this.start=start;this.end=end;this.light=light;this.number=number;this.branch=branch;diff=end.position().subtract(start.position());}
        void tangents(){
            Vec3 direction=diff.normalize();previousTangent=nextTangent=direction;
            if(previous!=null){Vec3 before=previous.diff.normalize();previousTangent=direction.add(before).normalize();previousSine=Math.max(.01,Math.sin(Math.acos(Math.clamp(previousTangent.dot(before.scale(-1)),-1,1))/2));}
            if(next!=null){Vec3 after=next.diff.normalize();nextTangent=direction.add(after).normalize();nextSine=Math.max(.01,Math.sin(Math.acos(Math.clamp(nextTangent.dot(after.scale(-1)),-1,1))/2));}
        }
    }
    private List<Segment> segments=new ArrayList<>();private final Map<Integer,Integer> parents=new HashMap<>();private final Random random;private int branches,mainSegments=1;
    public final double length;
    public LightningGeometry(Vec3 end,long seed,float multiplier,int duration){
        random=new Random(seed);random.nextInt(3);random.nextInt(duration);length=end.length();
        segments.add(new Segment(new Point(Vec3.ZERO,Vec3.ZERO),new Point(end,Vec3.ZERO),1,0,0));
        float[] divisors={8,12,17,23,30,34,40},chances={.7f,.5f,.5f,.5f,0,0,0},angles={45,50,55,60,0,0,0};
        for(int i=0;i<7;i++)fractal(2,(float)length*multiplier/divisors[i],chances[i],i<4?.1f:0,angles[i]);
        finish();
    }
    public List<Segment> segments(){return Collections.unmodifiableList(segments);}
    public int mainSegments(){return mainSegments;}
    private void fractal(int splits,float amount,float chance,float splitLength,float splitAngle){
        List<Segment> old=segments;segments=new ArrayList<>();
        for(Segment segment:old){
            Segment previous=segment.previous;Vec3 step=segment.diff.scale(1.0/splits);Point[] points=new Point[splits+1];points[0]=segment.start;points[splits]=segment.end;
            for(int i=1;i<splits;i++){
                Vec3 offset=rotate(perpendicular(segment.diff),random.nextFloat()*360,segment.diff).scale((random.nextFloat()-.5f)*amount);
                points[i]=new Point(segment.start.position().add(step.scale(i)),offset);
            }
            for(int i=0;i<splits;i++){
                Segment next=new Segment(points[i],points[i+1],segment.light,segment.number*splits+i,segment.branch);next.previous=previous;if(previous!=null)previous.next=next;
                if(i!=0&&random.nextFloat()<chance){
                    Vec3 axis=rotate(new Vec3(0,next.diff.z,-next.diff.y),random.nextFloat()*360,next.diff);
                    Vec3 extension=rotate(next.diff,(random.nextFloat()*.66f+.33f)*splitAngle,axis).scale(splitLength);
                    parents.put(++branches,next.branch);Segment branch=new Segment(points[i],new Point(points[i+1].base,points[i+1].offset.add(extension)),segment.light/2,next.number,branches);branch.previous=previous;segments.add(branch);
                }
                previous=next;segments.add(next);
            }
            if(segment.next!=null)segment.next.previous=previous;
        }
        mainSegments*=splits;
    }
    private void finish(){
        // The original resistance query is dormant: its collision loop never accumulates resistance.
        // Keep that active behavior; do not invent clipping of the visual branches behind blocks.
        segments.sort(Comparator.comparingInt((Segment s)->s.branch).thenComparingInt(s->s.number));
        for(Segment segment:segments)segment.tangents();
        segments.sort(Comparator.comparingDouble((Segment s)->s.light).reversed());
    }
    public static Vec3 perpendicular(Vec3 vector){return vector.z==0?new Vec3(-vector.y,vector.x,0):new Vec3(0,vector.z,-vector.y);}
    /** Original WRMat4 rotation orientation; zero axes are made safe for coincident endpoints. */
    public static Vec3 rotate(Vec3 vector,double degrees,Vec3 axis){
        if(axis.lengthSqr()<1e-16)return vector;axis=axis.normalize();double angle=degrees*.0174532925,c=Math.cos(angle),s=Math.sin(angle);
        return vector.scale(c).add(axis.cross(vector).scale(-s)).add(axis.scale(axis.dot(vector)*(1-c)));
    }
    /** Retained source helper. Original calculateCollisionAndDiffs never calls it. */
    public static float rayTraceResistance(Level level,Vec3 start,Vec3 end,float resistance){
        var hit=level.clip(new ClipContext(start,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,net.minecraft.world.phys.shapes.CollisionContext.empty()));
        if(hit.getType()==HitResult.Type.BLOCK){BlockPos pos=hit.getBlockPos();return resistance+level.getBlockState(pos).getBlock().getExplosionResistance()+.3f;}
        return resistance;
    }
}
