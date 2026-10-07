// Original ThaumCraft 2.1.6d drawing code by Azanor, adapted for the modern renderer.
package dev.thaumcraft.client.legacy;

import static dev.thaumcraft.client.legacy.LegacyCompat.*;
import java.util.*;
class LightningShape {

    java.util.ArrayList segments=new java.util.ArrayList(); java.util.HashMap splitparents=new java.util.HashMap();
    WRVector3 start,end,view; float multiplier,length; int numsegments0=1,numsplits,particleAge,particleMaxAge;
    boolean finalized,canhittarget=true; java.util.Random rand;
    float particleRed,particleGreen,particleBlue; static final float interpPosX=0,interpPosY=0,interpPosZ=0;
    public LightningShape(WRVector3 start,WRVector3 end,long seed,int duration,float multiplier){
        this.start=start;this.end=end;this.rand=new java.util.Random(seed);this.multiplier=multiplier;
        length=end.copy().sub(start).length();particleMaxAge=duration+rand.nextInt(duration)-duration/2;
        particleAge=-(int)(length*3);segments.add(new Segment(start,end));defaultFractal();finalizeBolt();
    }
    private WRVector3 getRelativeViewVector(WRVector3 pos){return view.copy().sub(pos);}
    public void fractal(int splits, float amount, float splitchance, float splitlength, float splitangle) {
      if (!this.finalized) {
         ArrayList oldsegments = this.segments;
         this.segments = new ArrayList();
         LightningShape.Segment prev = null;

         for (Object segmentValue : oldsegments) {
            LightningShape.Segment segment = (LightningShape.Segment)segmentValue;
            prev = segment.prev;
            WRVector3 subsegment = segment.diff.copy().scale(1.0F / splits);
            LightningShape.BoltPoint[] newpoints = new LightningShape.BoltPoint[splits + 1];
            WRVector3 startpoint = segment.startpoint.point;
            newpoints[0] = segment.startpoint;
            newpoints[splits] = segment.endpoint;

            for (int i = 1; i < splits; i++) {
               WRVector3 randoff = WRVector3.getPerpendicular(segment.diff).rotate(this.rand.nextFloat() * 360.0F, segment.diff);
               randoff.scale((this.rand.nextFloat() - 0.5F) * amount);
               WRVector3 basepoint = startpoint.copy().add(subsegment.copy().scale(i));
               newpoints[i] = new LightningShape.BoltPoint(basepoint, randoff);
            }

            for (int i = 0; i < splits; i++) {
               LightningShape.Segment next = new LightningShape.Segment(
                  newpoints[i], newpoints[i + 1], segment.light, segment.segmentno * splits + i, segment.splitno
               );
               next.prev = prev;
               if (prev != null) {
                  prev.next = next;
               }

               if (i != 0 && this.rand.nextFloat() < splitchance) {
                  WRVector3 splitrot = WRVector3.xCrossProduct(next.diff).rotate(this.rand.nextFloat() * 360.0F, next.diff);
                  WRVector3 diff = next.diff.copy().rotate((this.rand.nextFloat() * 0.66F + 0.33F) * splitangle, splitrot).scale(splitlength);
                  this.numsplits++;
                  this.splitparents.put(this.numsplits, next.splitno);
                  LightningShape.Segment split = new LightningShape.Segment(
                     newpoints[i],
                     new LightningShape.BoltPoint(newpoints[i + 1].basepoint, newpoints[i + 1].offsetvec.copy().add(diff)),
                     segment.light / 2.0F,
                     next.segmentno,
                     this.numsplits
                  );
                  split.prev = prev;
                  this.segments.add(split);
               }

               prev = next;
               this.segments.add(next);
            }

            if (segment.next != null) {
               segment.next.prev = prev;
            }
         }

         this.numsegments0 *= splits;
      }
   }

public void defaultFractal() {
      this.fractal(2, this.length * this.multiplier / 8.0F, 0.7F, 0.1F, 45.0F);
      this.fractal(2, this.length * this.multiplier / 12.0F, 0.5F, 0.1F, 50.0F);
      this.fractal(2, this.length * this.multiplier / 17.0F, 0.5F, 0.1F, 55.0F);
      this.fractal(2, this.length * this.multiplier / 23.0F, 0.5F, 0.1F, 60.0F);
      this.fractal(2, this.length * this.multiplier / 30.0F, 0.0F, 0.0F, 0.0F);
      this.fractal(2, this.length * this.multiplier / 34.0F, 0.0F, 0.0F, 0.0F);
      this.fractal(2, this.length * this.multiplier / 40.0F, 0.0F, 0.0F, 0.0F);
   }

private void calculateCollisionAndDiffs() {
      HashMap lastactivesegment = new HashMap();
      Collections.sort(this.segments, new LightningShape.SegmentSorter());
      int lastsplitcalc = 0;
      int lastactiveseg = 0;
      float splitresistance = 0.0F;

      for (Object segmentValue : this.segments) {
         LightningShape.Segment segment = (LightningShape.Segment)segmentValue;
         if (segment.splitno > lastsplitcalc) {
            lastactivesegment.put(lastsplitcalc, lastactiveseg);
            lastsplitcalc = segment.splitno;
            lastactiveseg = (Integer)lastactivesegment.get(this.splitparents.get(segment.splitno));
            splitresistance = lastactiveseg >= segment.segmentno ? 0.0F : 50.0F;
         }

         if (splitresistance < 40.0F * segment.light) {
            lastactiveseg = segment.segmentno;
         }
      }

      lastactivesegment.put(lastsplitcalc, lastactiveseg);
      lastsplitcalc = 0;
      lastactiveseg = (Integer)lastactivesegment.get(0);
      Iterator iterator = this.segments.iterator();

      while (iterator.hasNext()) {
         LightningShape.Segment segment = (LightningShape.Segment)iterator.next();
         if (lastsplitcalc != segment.splitno) {
            lastsplitcalc = segment.splitno;
            lastactiveseg = (Integer)lastactivesegment.get(segment.splitno);
         }

         if (segment.segmentno > lastactiveseg) {
            iterator.remove();
         }

         segment.calcEndDiffs();
      }

      if ((Integer)lastactivesegment.get(0) + 1 < this.numsegments0) {
         this.canhittarget = false;
      }
   }

public void finalizeBolt() {
      if (!this.finalized) {
         this.finalized = true;
         this.calculateCollisionAndDiffs();
         Collections.sort(this.segments, new LightningShape.SegmentLightSorter());
      }
   }public class BoltPoint {
      WRVector3 point;
      WRVector3 basepoint;
      WRVector3 offsetvec;
      

      public BoltPoint(WRVector3 basepoint, WRVector3 offsetvec) {
         this.point = basepoint.copy().add(offsetvec);
         this.basepoint = basepoint;
         this.offsetvec = offsetvec;
      }
   }

   public class Segment {
      public LightningShape.BoltPoint startpoint;
      public LightningShape.BoltPoint endpoint;
      public WRVector3 diff;
      public LightningShape.Segment prev;
      public LightningShape.Segment next;
      public WRVector3 nextdiff;
      public WRVector3 prevdiff;
      public float sinprev;
      public float sinnext;
      public float light;
      public int segmentno;
      public int splitno;
      

      public void calcDiff() {
         this.diff = this.endpoint.point.copy().sub(this.startpoint.point);
      }

      public void calcEndDiffs() {
         if (this.prev != null) {
            WRVector3 prevdiffnorm = this.prev.diff.copy().normalize();
            WRVector3 thisdiffnorm = this.diff.copy().normalize();
            this.prevdiff = thisdiffnorm.add(prevdiffnorm).normalize();
            this.sinprev = (float)Math.sin(WRVector3.anglePreNorm(thisdiffnorm, prevdiffnorm.scale(-1.0F)) / 2.0F);
         } else {
            this.prevdiff = this.diff.copy().normalize();
            this.sinprev = 1.0F;
         }

         if (this.next != null) {
            WRVector3 nextdiffnorm = this.next.diff.copy().normalize();
            WRVector3 thisdiffnorm = this.diff.copy().normalize();
            this.nextdiff = thisdiffnorm.add(nextdiffnorm).normalize();
            this.sinnext = (float)Math.sin(WRVector3.anglePreNorm(thisdiffnorm, nextdiffnorm.scale(-1.0F)) / 2.0F);
         } else {
            this.nextdiff = this.diff.copy().normalize();
            this.sinnext = 1.0F;
         }
      }

      @Override
      public String toString() {
         return this.startpoint.point.toString() + " " + this.endpoint.point.toString();
      }

      public Segment(LightningShape.BoltPoint start, LightningShape.BoltPoint end, float light, int segmentnumber, int splitnumber) {
         this.startpoint = start;
         this.endpoint = end;
         this.light = light;
         this.segmentno = segmentnumber;
         this.splitno = splitnumber;
         this.calcDiff();
      }

      public Segment(WRVector3 start, WRVector3 end) {
         this(
            LightningShape.this.new BoltPoint(start, new WRVector3(0.0, 0.0, 0.0)),
            LightningShape.this.new BoltPoint(end, new WRVector3(0.0, 0.0, 0.0)),
            1.0F,
            0,
            0
         );
      }
   }

   public class SegmentLightSorter implements Comparator {
      

      public int compare(LightningShape.Segment o1, LightningShape.Segment o2) {
         return Float.compare(o2.light, o1.light);
      }

      @Override
      public int compare(Object obj, Object obj1) {
         return this.compare((LightningShape.Segment)obj, (LightningShape.Segment)obj1);
      }
   }

   public class SegmentSorter implements Comparator {
      

      public int compare(LightningShape.Segment o1, LightningShape.Segment o2) {
         int comp = Integer.valueOf(o1.splitno).compareTo(o2.splitno);
         return comp == 0 ? Integer.valueOf(o1.segmentno).compareTo(o2.segmentno) : comp;
      }

      @Override
      public int compare(Object obj, Object obj1) {
         return this.compare((LightningShape.Segment)obj, (LightningShape.Segment)obj1);
      }
   }
public void renderBolt(Tessellator tessellator, float partialframe, WRVector3 look, int pass) {
      WRVector3 playervec = look;
      float boltage = this.particleAge >= 0 ? (float)this.particleAge / this.particleMaxAge : 0.0F;
      float mainalpha = 1.0F;
      if (pass == 0) {
         mainalpha = (1.0F - boltage) * 0.4F;
      } else {
         mainalpha = 1.0F - boltage * 0.5F;
      }

      int renderlength = (int)(
         (this.particleAge + partialframe + (int)(this.length * 3.0F)) / Math.max(1,(int)(this.length * 3.0F)) * this.numsegments0
      );

      for (Object rendersegmentValue : this.segments) {
         LightningShape.Segment rendersegment = (LightningShape.Segment)rendersegmentValue;
         if (rendersegment.segmentno <= renderlength) {
            float width = 0.03F * (getRelativeViewVector(rendersegment.startpoint.point).length() / 5.0F + 1.0F) * (1.0F + rendersegment.light) * 0.5F;
            WRVector3 diff1 = WRVector3.crossProduct(playervec, rendersegment.prevdiff).scale(width / rendersegment.sinprev);
            WRVector3 diff2 = WRVector3.crossProduct(playervec, rendersegment.nextdiff).scale(width / rendersegment.sinnext);
            WRVector3 startvec = rendersegment.startpoint.point;
            WRVector3 endvec = rendersegment.endpoint.point;
            float rx1 = (float)(startvec.x - interpPosX);
            float ry1 = (float)(startvec.y - interpPosY);
            float rz1 = (float)(startvec.z - interpPosZ);
            float rx2 = (float)(endvec.x - interpPosX);
            float ry2 = (float)(endvec.y - interpPosY);
            float rz2 = (float)(endvec.z - interpPosZ);
            tessellator.setColorRGBA_F(this.particleRed, this.particleGreen, this.particleBlue, mainalpha * rendersegment.light);
            tessellator.addVertexWithUV(rx2 - diff2.x, ry2 - diff2.y, rz2 - diff2.z, 0.5, 0.0);
            tessellator.addVertexWithUV(rx1 - diff1.x, ry1 - diff1.y, rz1 - diff1.z, 0.5, 0.0);
            tessellator.addVertexWithUV(rx1 + diff1.x, ry1 + diff1.y, rz1 + diff1.z, 0.5, 1.0);
            tessellator.addVertexWithUV(rx2 + diff2.x, ry2 + diff2.y, rz2 + diff2.z, 0.5, 1.0);
            if (rendersegment.next == null) {
               WRVector3 roundend = rendersegment.endpoint.point.copy().add(rendersegment.diff.copy().normalize().scale(width));
               float rx3 = (float)(roundend.x - interpPosX);
               float ry3 = (float)(roundend.y - interpPosY);
               float rz3 = (float)(roundend.z - interpPosZ);
               tessellator.addVertexWithUV(rx3 - diff2.x, ry3 - diff2.y, rz3 - diff2.z, 0.0, 0.0);
               tessellator.addVertexWithUV(rx2 - diff2.x, ry2 - diff2.y, rz2 - diff2.z, 0.5, 0.0);
               tessellator.addVertexWithUV(rx2 + diff2.x, ry2 + diff2.y, rz2 + diff2.z, 0.5, 1.0);
               tessellator.addVertexWithUV(rx3 + diff2.x, ry3 + diff2.y, rz3 + diff2.z, 0.0, 1.0);
            }

            if (rendersegment.prev == null) {
               WRVector3 roundend = rendersegment.startpoint.point.copy().sub(rendersegment.diff.copy().normalize().scale(width));
               float rx3 = (float)(roundend.x - interpPosX);
               float ry3 = (float)(roundend.y - interpPosY);
               float rz3 = (float)(roundend.z - interpPosZ);
               tessellator.addVertexWithUV(rx1 - diff1.x, ry1 - diff1.y, rz1 - diff1.z, 0.5, 0.0);
               tessellator.addVertexWithUV(rx3 - diff1.x, ry3 - diff1.y, rz3 - diff1.z, 0.0, 0.0);
               tessellator.addVertexWithUV(rx3 + diff1.x, ry3 + diff1.y, rz3 + diff1.z, 0.0, 1.0);
               tessellator.addVertexWithUV(rx1 + diff1.x, ry1 + diff1.y, rz1 + diff1.z, 0.5, 1.0);
            }
         }
      }
   }
}
