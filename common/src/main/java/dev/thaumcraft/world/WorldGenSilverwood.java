package dev.thaumcraft.world;

// Tree geometry from Thaumcraft 2.1.6d by Azanor, adapted to the modern world API.

import java.util.Random;
import net.minecraft.util.Mth;

public class WorldGenSilverwood extends TreeGenerator {
   static final byte[] otherCoordPairs = new byte[]{2, 0, 0, 1, 2, 1};
   Random rand;
   TreeWorld worldObj;
   int[] basePos = new int[]{0, 0, 0};
   int heightLimit;
   int height;
   double heightAttenuation;
   double field_875_h;
   double field_874_i;
   double horizontalspread;
   double thickness;
   int trunkSize;
   int heightLimitLimit;
   int leafDistanceLimit;
   int[][] leafNodes;

   public WorldGenSilverwood(boolean flag) {
      super(flag);
      this.rand = new Random();
      this.heightLimit = 0;
      this.heightAttenuation = 0.618;
      this.field_875_h = 1.0;
      this.field_874_i = 1.381;
      this.horizontalspread = 0.66;
      this.thickness = 1.0;
      this.trunkSize = 1;
      this.heightLimitLimit = 9;
      this.leafDistanceLimit = 3;
   }

   void generateLeafNodeList() {
      this.height = (int)(this.heightLimit * this.heightAttenuation);
      if (this.height >= this.heightLimit) {
         this.height = this.heightLimit - 1;
      }

      int i = (int)(1.382 + Math.pow(this.thickness * this.heightLimit / 13.0, 2.0));
      if (i < 1) {
         i = 1;
      }

      int[][] ai = new int[i * this.heightLimit][4];
      int j = this.basePos[1] + this.heightLimit - this.leafDistanceLimit;
      int k = 1;
      int l = this.basePos[1] + this.height;
      int i1 = j - this.basePos[1];
      ai[0][0] = this.basePos[0];
      ai[0][1] = j;
      ai[0][2] = this.basePos[2];
      ai[0][3] = l;
      j--;

      while (i1 >= 0) {
         int j1 = 0;
         float f = this.func_528_a(i1);
         if (f < 0.0F) {
            j--;
            i1--;
         } else {
            double d = 0.5;

            while (j1 < i) {
               double d1 = this.horizontalspread * (f * (this.rand.nextFloat() + 0.328));
               double d2 = this.rand.nextFloat() * 2.0 * 3.14159;
               int k1 = Mth.floor(d1 * Math.sin(d2) + this.basePos[0] + d);
               int l1 = Mth.floor(d1 * Math.cos(d2) + this.basePos[2] + d);
               int[] ai1 = new int[]{k1, j, l1};
               int[] ai2 = new int[]{k1, j + this.leafDistanceLimit, l1};
               if (this.checkBlockLine(ai1, ai2) == -1) {
                  int[] ai3 = new int[]{this.basePos[0], this.basePos[1], this.basePos[2]};
                  double d3 = Math.sqrt(Math.pow(Math.abs(this.basePos[0] - ai1[0]), 2.0) + Math.pow(Math.abs(this.basePos[2] - ai1[2]), 2.0));
                  double d4 = d3 * this.field_874_i;
                  if (ai1[1] - d4 > l) {
                     ai3[1] = l;
                  } else {
                     ai3[1] = (int)(ai1[1] - d4);
                  }

                  if (this.checkBlockLine(ai3, ai1) == -1) {
                     ai[k][0] = k1;
                     ai[k][1] = j;
                     ai[k][2] = l1;
                     ai[k][3] = ai3[1];
                     k++;
                  }
               }

               j1++;
            }

            j--;
            i1--;
         }
      }

      this.leafNodes = new int[k][4];
      System.arraycopy(ai, 0, this.leafNodes, 0, k);
   }

   void placeLeaves(int i, int j, int k, float f, byte byte0, int l) {
      int i1 = (int)(f + 0.618);
      byte byte1 = otherCoordPairs[byte0];
      byte byte2 = otherCoordPairs[byte0 + 3];
      int[] ai = new int[]{i, j, k};
      int[] ai1 = new int[]{0, 0, 0};
      int j1 = -i1;
      int k1 = -i1;

      for (ai1[byte0] = ai[byte0]; j1 <= i1; j1++) {
         ai1[byte1] = ai[byte1] + j1;
         int l1 = -i1;

         while (l1 <= i1) {
            double d = Math.sqrt(Math.pow(Math.abs(j1) + 0.5, 2.0) + Math.pow(Math.abs(l1) + 0.5, 2.0));
            if (d > f) {
               l1++;
            } else {
               ai1[byte2] = ai[byte2] + l1;
               int i2 = this.worldObj.getBlockId(ai1[0], ai1[1], ai1[2]);
               if (i2 != 0 && i2 != 18) {
                  l1++;
               } else {
                  this.setBlockAndMetadata(this.worldObj, ai1[0], ai1[1], ai1[2], l, 1);
                  l1++;
               }
            }
         }
      }
   }

   float func_528_a(int i) {
      if (i < this.heightLimit * 0.3) {
         return -1.618F;
      }

      float f = this.heightLimit / 2.0F;
      float f1 = this.heightLimit / 2.0F - i;
      float f2;
      if (f1 == 0.0F) {
         f2 = f;
      } else if (Math.abs(f1) >= f) {
         f2 = 0.0F;
      } else {
         f2 = (float)Math.sqrt(Math.pow(Math.abs(f), 2.0) - Math.pow(Math.abs(f1), 2.0));
      }

      return f2 * 0.5F;
   }

   float func_526_b(int i) {
      if (i >= 0 && i < this.leafDistanceLimit) {
         return i != 0 && i != this.leafDistanceLimit - 1 ? 3.0F : 2.0F;
      } else {
         return -1.0F;
      }
   }

   void generateLeafNode(int i, int j, int k) {
      int l = j;

      for (int i1 = j + this.leafDistanceLimit; l < i1; l++) {
         float f = this.func_526_b(l - j);
         this.placeLeaves(i, l, k, f, (byte)1, 248);
      }
   }

   void placeBlockLine(int[] ai, int[] ai1, int i) {
      int[] ai2 = new int[]{0, 0, 0};
      byte byte0 = 0;
      int j = 0;

      while (byte0 < 3) {
         ai2[byte0] = ai1[byte0] - ai[byte0];
         if (Math.abs(ai2[byte0]) > Math.abs(ai2[j])) {
            j = byte0;
         }

         byte0++;
      }

      if (ai2[j] != 0) {
         byte byte1 = otherCoordPairs[j];
         byte byte2 = otherCoordPairs[j + 3];
         byte byte3;
         if (ai2[j] > 0) {
            byte3 = 1;
         } else {
            byte3 = -1;
         }

         double d = (double)ai2[byte1] / ai2[j];
         double d1 = (double)ai2[byte2] / ai2[j];
         int[] ai3 = new int[]{0, 0, 0};
         int k = 0;

         for (int l = ai2[j] + byte3; k != l; k += byte3) {
            ai3[j] = Mth.floor(ai[j] + k + 0.5);
            ai3[byte1] = Mth.floor(ai[byte1] + k * d + 0.5);
            ai3[byte2] = Mth.floor(ai[byte2] + k * d1 + 0.5);
            this.setBlockAndMetadata(this.worldObj, ai3[0], ai3[1], ai3[2], i, 0);
         }
      }
   }

   void generateLeaves() {
      int i = 0;

      for (int j = this.leafNodes.length; i < j; i++) {
         int k = this.leafNodes[i][0];
         int l = this.leafNodes[i][1];
         int i1 = this.leafNodes[i][2];
         this.generateLeafNode(k, l, i1);
      }
   }

   boolean leafNodeNeedsBase(int i) {
      return i >= this.heightLimit * 0.2;
   }

   void generateTrunk() {
      int i = this.basePos[0];
      int j = this.basePos[1];
      int k = this.basePos[1] + this.height;
      int l = this.basePos[2];
      int[] ai = new int[]{i, j, l};
      int[] ai1 = new int[]{i, k, l};
      this.placeBlockLine(ai, ai1, 247);
      if (this.trunkSize == 2) {
         ai[0]++;
         ai1[0]++;
         this.placeBlockLine(ai, ai1, 247);
         ai[2]++;
         ai1[2]++;
         this.placeBlockLine(ai, ai1, 247);
         ai[0]--;
         ai1[0]--;
         this.placeBlockLine(ai, ai1, 247);
      }
   }

   void generateLeafNodeBases() {
      int i = 0;
      int j = this.leafNodes.length;
      int[] ai = new int[]{this.basePos[0], this.basePos[1], this.basePos[2]};

      while (i < j) {
         int[] ai1 = this.leafNodes[i];
         int[] ai2 = new int[]{ai1[0], ai1[1], ai1[2]};
         ai[1] = ai1[3];
         int k = ai[1] - this.basePos[1];
         if (this.leafNodeNeedsBase(k)) {
            this.placeBlockLine(ai, ai2, 247);
         }

         i++;
      }
   }

   int checkBlockLine(int[] ai, int[] ai1) {
      int[] ai2 = new int[]{0, 0, 0};
      byte byte0 = 0;
      int i = 0;

      while (byte0 < 3) {
         ai2[byte0] = ai1[byte0] - ai[byte0];
         if (Math.abs(ai2[byte0]) > Math.abs(ai2[i])) {
            i = byte0;
         }

         byte0++;
      }

      if (ai2[i] == 0) {
         return -1;
      }

      byte byte1 = otherCoordPairs[i];
      byte byte2 = otherCoordPairs[i + 3];
      byte byte3;
      if (ai2[i] > 0) {
         byte3 = 1;
      } else {
         byte3 = -1;
      }

      double d = (double)ai2[byte1] / ai2[i];
      double d1 = (double)ai2[byte2] / ai2[i];
      int[] ai3 = new int[]{0, 0, 0};
      int j = 0;

      int k;
      for (k = ai2[i] + byte3; j != k; j += byte3) {
         ai3[i] = ai[i] + j;
         ai3[byte1] = Mth.floor(ai[byte1] + j * d);
         ai3[byte2] = Mth.floor(ai[byte2] + j * d1);
         int l = this.worldObj.getBlockId(ai3[0], ai3[1], ai3[2]);
         if (l != 0 && l != 18) {
            break;
         }
      }

      return j == k ? -1 : Math.abs(j);
   }

   boolean validTreeLocation() {
      int[] ai = new int[]{this.basePos[0], this.basePos[1], this.basePos[2]};
      int[] ai1 = new int[]{this.basePos[0], this.basePos[1] + this.heightLimit - 1, this.basePos[2]};
      int i = this.worldObj.getBlockId(this.basePos[0], this.basePos[1] - 1, this.basePos[2]);
      if (i != 2 && i != 3) {
         return false;
      }

      int j = this.checkBlockLine(ai, ai1);
      if (j == -1) {
         return true;
      }

      if (j < 6) {
         return false;
      }

      this.heightLimit = j;
      return true;
   }

   public void func_517_a(double d, double d1, double d2) {
      this.heightLimitLimit = (int)(d * 12.0);
      if (d > 0.5) {
         this.leafDistanceLimit = 5;
      }

      this.horizontalspread = d1;
      this.thickness = d2;
   }

   @Override
   public boolean generate(TreeWorld world, Random random, int i, int j, int k) {
      this.worldObj = world;
      long l = random.nextLong();
      this.rand.setSeed(l);
      this.basePos[0] = i;
      this.basePos[1] = j;
      this.basePos[2] = k;
      if (this.heightLimit == 0) {
         this.heightLimit = 5 + this.rand.nextInt(this.heightLimitLimit);
      }

      if (!this.validTreeLocation()) {
         return false;
      }

      this.generateLeafNodeList();
      this.generateLeaves();
      this.generateTrunk();
      this.generateLeafNodeBases();
      world.finishTree();
      return true;
   }
}
