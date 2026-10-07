// Original ThaumCraft 2.1.6d drawing code by Azanor, adapted for the modern renderer.
package dev.thaumcraft.client.legacy;

import static dev.thaumcraft.client.legacy.LegacyCompat.*;

import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.util.Random;

public class TileVoidHoleRenderer extends TileEntitySpecialRenderer {
   FloatBuffer fBuffer;
   private String t1 = "/thaumcraft/resources/tunnel.png";
   private String t2 = "/thaumcraft/resources/particlefield.png";

   public TileVoidHoleRenderer() {
      this.fBuffer = GLAllocation.createDirectFloatBuffer(16);
   }

   public void drawPlaneYPos(TileVoidHole tileentityendportal, double x, double y, double z, float f) {
      float px = (float)this.tileEntityRenderer.playerX;
      float py = (float)this.tileEntityRenderer.playerY;
      float pz = (float)this.tileEntityRenderer.playerZ;
      GL11.glDisable(2896);
      Random random = new Random(31100L);
      float offset = 0.999F;

      for (int i = 0; i < 16; i++) {
         GL11.glPushMatrix();
         float f5 = 16 - i;
         float f6 = 0.0625F;
         float f7 = 1.0F / (f5 + 1.0F);
         if (i == 0) {
            this.bindTextureByName(this.t1);
            f7 = 0.1F;
            f5 = 65.0F;
            f6 = 0.125F;
            GL11.glEnable(3042);
            GL11.glBlendFunc(770, 771);
         }

         if (i == 1) {
            this.bindTextureByName(this.t2);
            GL11.glEnable(3042);
            GL11.glBlendFunc(1, 1);
            f6 = 0.5F;
         }

         float f8 = (float)(y + offset);
         float f9 = f8 - ActiveRenderInfo.objectY;
         float f10 = f8 + f5 - ActiveRenderInfo.objectY;
         float f11 = f9 / f10;
         f11 = (float)(y + offset) + f11;
         GL11.glTranslatef(px, f11, pz);
         GL11.glTexGen(8192, 9473, this.calcFloatBuffer(1.0F, 0.0F, 0.0F, 0.0F));
         GL11.glTexGen(8193, 9473, this.calcFloatBuffer(0.0F, 0.0F, 1.0F, 0.0F));
         GL11.glTexGen(8194, 9473, this.calcFloatBuffer(0.0F, 0.0F, 0.0F, 1.0F));
         GL11.glTexGen(8195, 9474, this.calcFloatBuffer(0.0F, 1.0F, 0.0F, 0.0F));
         GL11.glEnable(3168);
         GL11.glEnable(3169);
         GL11.glEnable(3170);
         GL11.glEnable(3171);
         GL11.glPopMatrix();
         GL11.glMatrixMode(5890);
         GL11.glPushMatrix();
         GL11.glLoadIdentity();
         GL11.glTranslatef(0.0F, (float)(clockMillis() % 700000L) / 250000.0F, 0.0F);
         GL11.glScalef(f6, f6, f6);
         GL11.glTranslatef(0.5F, 0.5F, 0.0F);
         GL11.glRotatef((i * i * 4321 + i * 9) * 2.0F, 0.0F, 0.0F, 1.0F);
         GL11.glTranslatef(-0.5F, -0.5F, 0.0F);
         GL11.glTranslatef(-px, -pz, -py);
         GL11.glTranslatef(ActiveRenderInfo.objectX * f5 / f9, ActiveRenderInfo.objectZ * f5 / f9, -py);
         Tessellator tessellator = Tessellator.instance;
         tessellator.startDrawingQuads();
         f11 = random.nextFloat() * 0.5F + 0.1F;
         float f12 = random.nextFloat() * 0.5F + 0.4F;
         float f13 = random.nextFloat() * 0.5F + 0.5F;
         if (i == 0) {
            f13 = 1.0F;
            f12 = 1.0F;
            f11 = 1.0F;
         }

         tessellator.setBrightness(180);
         tessellator.setColorRGBA_F(f11 * f7, f12 * f7, f13 * f7, 1.0F);
         tessellator.addVertex(x, y + offset, z + 1.0);
         tessellator.addVertex(x, y + offset, z);
         tessellator.addVertex(x + 1.0, y + offset, z);
         tessellator.addVertex(x + 1.0, y + offset, z + 1.0);
         tessellator.draw();
         GL11.glPopMatrix();
         GL11.glMatrixMode(5888);
      }

      GL11.glDisable(3042);
      GL11.glDisable(3168);
      GL11.glDisable(3169);
      GL11.glDisable(3170);
      GL11.glDisable(3171);
      GL11.glEnable(2896);
   }

   public void drawPlaneYNeg(TileVoidHole tileentityendportal, double x, double y, double z, float f) {
      float f1 = (float)this.tileEntityRenderer.playerX;
      float f2 = (float)this.tileEntityRenderer.playerY;
      float f3 = (float)this.tileEntityRenderer.playerZ;
      GL11.glDisable(2896);
      Random random = new Random(31100L);
      float offset = 0.001F;

      for (int i = 0; i < 16; i++) {
         GL11.glPushMatrix();
         float f5 = 16 - i;
         float f6 = 0.0625F;
         float f7 = 1.0F / (f5 + 1.0F);
         if (i == 0) {
            this.bindTextureByName(this.t1);
            f7 = 0.1F;
            f5 = 65.0F;
            f6 = 0.125F;
            GL11.glEnable(3042);
            GL11.glBlendFunc(770, 771);
         }

         if (i == 1) {
            this.bindTextureByName(this.t2);
            GL11.glEnable(3042);
            GL11.glBlendFunc(1, 1);
            f6 = 0.5F;
         }

         float f8 = (float)(-(y + offset));
         float f9 = f8 + ActiveRenderInfo.objectY;
         float f10 = f8 + f5 + ActiveRenderInfo.objectY;
         float f11 = f9 / f10;
         f11 = (float)(y + offset) + f11;
         GL11.glTranslatef(f1, f11, f3);
         GL11.glTexGen(8192, 9473, this.calcFloatBuffer(1.0F, 0.0F, 0.0F, 0.0F));
         GL11.glTexGen(8193, 9473, this.calcFloatBuffer(0.0F, 0.0F, 1.0F, 0.0F));
         GL11.glTexGen(8194, 9473, this.calcFloatBuffer(0.0F, 0.0F, 0.0F, 1.0F));
         GL11.glTexGen(8195, 9474, this.calcFloatBuffer(0.0F, 1.0F, 0.0F, 0.0F));
         GL11.glEnable(3168);
         GL11.glEnable(3169);
         GL11.glEnable(3170);
         GL11.glEnable(3171);
         GL11.glPopMatrix();
         GL11.glMatrixMode(5890);
         GL11.glPushMatrix();
         GL11.glLoadIdentity();
         GL11.glTranslatef(0.0F, (float)(clockMillis() % 700000L) / 250000.0F, 0.0F);
         GL11.glScalef(f6, f6, f6);
         GL11.glTranslatef(0.5F, 0.5F, 0.0F);
         GL11.glRotatef((i * i * 4321 + i * 9) * 2.0F, 0.0F, 0.0F, 1.0F);
         GL11.glTranslatef(-0.5F, -0.5F, 0.0F);
         GL11.glTranslatef(-f1, -f3, -f2);
         GL11.glTranslatef(ActiveRenderInfo.objectX * f5 / f9, ActiveRenderInfo.objectZ * f5 / f9, -f2);
         Tessellator tessellator = Tessellator.instance;
         tessellator.startDrawingQuads();
         f11 = random.nextFloat() * 0.5F + 0.1F;
         float f12 = random.nextFloat() * 0.5F + 0.4F;
         float f13 = random.nextFloat() * 0.5F + 0.5F;
         if (i == 0) {
            f13 = 1.0F;
            f12 = 1.0F;
            f11 = 1.0F;
         }

         tessellator.setBrightness(180);
         tessellator.setColorRGBA_F(f11 * f7, f12 * f7, f13 * f7, 1.0F);
         tessellator.addVertex(x, y + offset, z);
         tessellator.addVertex(x, y + offset, z + 1.0);
         tessellator.addVertex(x + 1.0, y + offset, z + 1.0);
         tessellator.addVertex(x + 1.0, y + offset, z);
         tessellator.draw();
         GL11.glPopMatrix();
         GL11.glMatrixMode(5888);
      }

      GL11.glDisable(3042);
      GL11.glDisable(3168);
      GL11.glDisable(3169);
      GL11.glDisable(3170);
      GL11.glDisable(3171);
      GL11.glEnable(2896);
   }

   public void drawPlaneZNeg(TileVoidHole tileentityendportal, double x, double y, double z, float f) {
      float px = (float)this.tileEntityRenderer.playerX;
      float py = (float)this.tileEntityRenderer.playerY;
      float pz = (float)this.tileEntityRenderer.playerZ;
      GL11.glDisable(2896);
      Random random = new Random(31100L);
      float offset = 0.001F;

      for (int i = 0; i < 16; i++) {
         GL11.glPushMatrix();
         float f5 = 16 - i;
         float f6 = 0.0625F;
         float f7 = 1.0F / (f5 + 1.0F);
         if (i == 0) {
            this.bindTextureByName(this.t1);
            f7 = 0.1F;
            f5 = 65.0F;
            f6 = 0.125F;
            GL11.glEnable(3042);
            GL11.glBlendFunc(770, 771);
         }

         if (i == 1) {
            this.bindTextureByName(this.t2);
            GL11.glEnable(3042);
            GL11.glBlendFunc(1, 1);
            f6 = 0.5F;
         }

         float f8 = (float)(-(z + offset));
         float f9 = f8 + ActiveRenderInfo.objectZ;
         float f10 = f8 + f5 + ActiveRenderInfo.objectZ;
         float f11 = f9 / f10;
         f11 = (float)(z + offset) + f11;
         GL11.glTranslatef(px, py, f11);
         GL11.glTexGen(8192, 9473, this.calcFloatBuffer(1.0F, 0.0F, 0.0F, 0.0F));
         GL11.glTexGen(8193, 9473, this.calcFloatBuffer(0.0F, 1.0F, 0.0F, 0.0F));
         GL11.glTexGen(8194, 9473, this.calcFloatBuffer(0.0F, 0.0F, 0.0F, 1.0F));
         GL11.glTexGen(8195, 9474, this.calcFloatBuffer(0.0F, 0.0F, 1.0F, 0.0F));
         GL11.glEnable(3168);
         GL11.glEnable(3169);
         GL11.glEnable(3170);
         GL11.glEnable(3171);
         GL11.glPopMatrix();
         GL11.glMatrixMode(5890);
         GL11.glPushMatrix();
         GL11.glLoadIdentity();
         GL11.glTranslatef(0.0F, (float)(clockMillis() % 700000L) / 250000.0F, 0.0F);
         GL11.glScalef(f6, f6, f6);
         GL11.glTranslatef(0.5F, 0.5F, 0.0F);
         GL11.glRotatef((i * i * 4321 + i * 9) * 2.0F, 0.0F, 0.0F, 1.0F);
         GL11.glTranslatef(-0.5F, -0.5F, 0.0F);
         GL11.glTranslatef(-px, -py, -pz);
         GL11.glTranslatef(ActiveRenderInfo.objectX * f5 / f9, ActiveRenderInfo.objectY * f5 / f9, -pz);
         Tessellator tessellator = Tessellator.instance;
         tessellator.startDrawingQuads();
         f11 = random.nextFloat() * 0.5F + 0.1F;
         float f12 = random.nextFloat() * 0.5F + 0.4F;
         float f13 = random.nextFloat() * 0.5F + 0.5F;
         if (i == 0) {
            f13 = 1.0F;
            f12 = 1.0F;
            f11 = 1.0F;
         }

         tessellator.setBrightness(180);
         tessellator.setColorRGBA_F(f11 * f7, f12 * f7, f13 * f7, 1.0F);
         tessellator.addVertex(x, y + 1.0, z + offset);
         tessellator.addVertex(x, y, z + offset);
         tessellator.addVertex(x + 1.0, y, z + offset);
         tessellator.addVertex(x + 1.0, y + 1.0, z + offset);
         tessellator.draw();
         GL11.glPopMatrix();
         GL11.glMatrixMode(5888);
      }

      GL11.glDisable(3042);
      GL11.glDisable(3168);
      GL11.glDisable(3169);
      GL11.glDisable(3170);
      GL11.glDisable(3171);
      GL11.glEnable(2896);
   }

   public void drawPlaneZPos(TileVoidHole tileentityendportal, double x, double y, double z, float f) {
      float px = (float)this.tileEntityRenderer.playerX;
      float py = (float)this.tileEntityRenderer.playerY;
      float pz = (float)this.tileEntityRenderer.playerZ;
      GL11.glDisable(2896);
      Random random = new Random(31100L);
      float offset = 0.999F;

      for (int i = 0; i < 16; i++) {
         GL11.glPushMatrix();
         float f5 = 16 - i;
         float f6 = 0.0625F;
         float f7 = 1.0F / (f5 + 1.0F);
         if (i == 0) {
            this.bindTextureByName(this.t1);
            f7 = 0.1F;
            f5 = 65.0F;
            f6 = 0.125F;
            GL11.glEnable(3042);
            GL11.glBlendFunc(770, 771);
         }

         if (i == 1) {
            this.bindTextureByName(this.t2);
            GL11.glEnable(3042);
            GL11.glBlendFunc(1, 1);
            f6 = 0.5F;
         }

         float f8 = (float)(z + offset);
         float f9 = f8 - ActiveRenderInfo.objectZ;
         float f10 = f8 + f5 - ActiveRenderInfo.objectZ;
         float f11 = f9 / f10;
         f11 = (float)(z + offset) + f11;
         GL11.glTranslatef(px, py, f11);
         GL11.glTexGen(8192, 9473, this.calcFloatBuffer(1.0F, 0.0F, 0.0F, 0.0F));
         GL11.glTexGen(8193, 9473, this.calcFloatBuffer(0.0F, 1.0F, 0.0F, 0.0F));
         GL11.glTexGen(8194, 9473, this.calcFloatBuffer(0.0F, 0.0F, 0.0F, 1.0F));
         GL11.glTexGen(8195, 9474, this.calcFloatBuffer(0.0F, 0.0F, 1.0F, 0.0F));
         GL11.glEnable(3168);
         GL11.glEnable(3169);
         GL11.glEnable(3170);
         GL11.glEnable(3171);
         GL11.glPopMatrix();
         GL11.glMatrixMode(5890);
         GL11.glPushMatrix();
         GL11.glLoadIdentity();
         GL11.glTranslatef(0.0F, (float)(clockMillis() % 700000L) / 250000.0F, 0.0F);
         GL11.glScalef(f6, f6, f6);
         GL11.glTranslatef(0.5F, 0.5F, 0.0F);
         GL11.glRotatef((i * i * 4321 + i * 9) * 2.0F, 0.0F, 0.0F, 1.0F);
         GL11.glTranslatef(-0.5F, -0.5F, 0.0F);
         GL11.glTranslatef(-px, -py, -pz);
         GL11.glTranslatef(ActiveRenderInfo.objectX * f5 / f9, ActiveRenderInfo.objectY * f5 / f9, -pz);
         Tessellator tessellator = Tessellator.instance;
         tessellator.startDrawingQuads();
         f11 = random.nextFloat() * 0.5F + 0.1F;
         float f12 = random.nextFloat() * 0.5F + 0.4F;
         float f13 = random.nextFloat() * 0.5F + 0.5F;
         if (i == 0) {
            f13 = 1.0F;
            f12 = 1.0F;
            f11 = 1.0F;
         }

         tessellator.setBrightness(180);
         tessellator.setColorRGBA_F(f11 * f7, f12 * f7, f13 * f7, 1.0F);
         tessellator.addVertex(x, y, z + offset);
         tessellator.addVertex(x, y + 1.0, z + offset);
         tessellator.addVertex(x + 1.0, y + 1.0, z + offset);
         tessellator.addVertex(x + 1.0, y, z + offset);
         tessellator.draw();
         GL11.glPopMatrix();
         GL11.glMatrixMode(5888);
      }

      GL11.glDisable(3042);
      GL11.glDisable(3168);
      GL11.glDisable(3169);
      GL11.glDisable(3170);
      GL11.glDisable(3171);
      GL11.glEnable(2896);
   }

   public void drawPlaneXNeg(TileVoidHole tileentityendportal, double x, double y, double z, float f) {
      float px = (float)this.tileEntityRenderer.playerX;
      float py = (float)this.tileEntityRenderer.playerY;
      float pz = (float)this.tileEntityRenderer.playerZ;
      GL11.glDisable(2896);
      Random random = new Random(31100L);
      float offset = 0.001F;

      for (int i = 0; i < 16; i++) {
         GL11.glPushMatrix();
         float f5 = 16 - i;
         float f6 = 0.0625F;
         float f7 = 1.0F / (f5 + 1.0F);
         if (i == 0) {
            this.bindTextureByName(this.t1);
            f7 = 0.1F;
            f5 = 65.0F;
            f6 = 0.125F;
            GL11.glEnable(3042);
            GL11.glBlendFunc(770, 771);
         }

         if (i == 1) {
            this.bindTextureByName(this.t2);
            GL11.glEnable(3042);
            GL11.glBlendFunc(1, 1);
            f6 = 0.5F;
         }

         float f8 = (float)(-(x + offset));
         float f9 = f8 + ActiveRenderInfo.objectX;
         float f10 = f8 + f5 + ActiveRenderInfo.objectX;
         float f11 = f9 / f10;
         f11 = (float)(x + offset) + f11;
         GL11.glTranslatef(f11, py, pz);
         GL11.glTexGen(8193, 9473, this.calcFloatBuffer(0.0F, 1.0F, 0.0F, 0.0F));
         GL11.glTexGen(8192, 9473, this.calcFloatBuffer(0.0F, 0.0F, 1.0F, 0.0F));
         GL11.glTexGen(8194, 9473, this.calcFloatBuffer(0.0F, 0.0F, 0.0F, 1.0F));
         GL11.glTexGen(8195, 9474, this.calcFloatBuffer(1.0F, 0.0F, 0.0F, 0.0F));
         GL11.glEnable(3168);
         GL11.glEnable(3169);
         GL11.glEnable(3170);
         GL11.glEnable(3171);
         GL11.glPopMatrix();
         GL11.glMatrixMode(5890);
         GL11.glPushMatrix();
         GL11.glLoadIdentity();
         GL11.glTranslatef(0.0F, (float)(clockMillis() % 700000L) / 250000.0F, 0.0F);
         GL11.glScalef(f6, f6, f6);
         GL11.glTranslatef(0.5F, 0.5F, 0.0F);
         GL11.glRotatef((i * i * 4321 + i * 9) * 2.0F, 0.0F, 0.0F, 1.0F);
         GL11.glTranslatef(-0.5F, -0.5F, 0.0F);
         GL11.glTranslatef(-pz, -py, -px);
         GL11.glTranslatef(ActiveRenderInfo.objectZ * f5 / f9, ActiveRenderInfo.objectY * f5 / f9, -px);
         Tessellator tessellator = Tessellator.instance;
         tessellator.startDrawingQuads();
         f11 = random.nextFloat() * 0.5F + 0.1F;
         float f12 = random.nextFloat() * 0.5F + 0.4F;
         float f13 = random.nextFloat() * 0.5F + 0.5F;
         if (i == 0) {
            f13 = 1.0F;
            f12 = 1.0F;
            f11 = 1.0F;
         }

         tessellator.setBrightness(180);
         tessellator.setColorRGBA_F(f11 * f7, f12 * f7, f13 * f7, 1.0F);
         tessellator.addVertex(x + offset, y + 1.0, z);
         tessellator.addVertex(x + offset, y + 1.0, z + 1.0);
         tessellator.addVertex(x + offset, y, z + 1.0);
         tessellator.addVertex(x + offset, y, z);
         tessellator.draw();
         GL11.glPopMatrix();
         GL11.glMatrixMode(5888);
      }

      GL11.glDisable(3042);
      GL11.glDisable(3168);
      GL11.glDisable(3169);
      GL11.glDisable(3170);
      GL11.glDisable(3171);
      GL11.glEnable(2896);
   }

   public void drawPlaneXPos(TileVoidHole tileentityendportal, double x, double y, double z, float f) {
      float px = (float)this.tileEntityRenderer.playerX;
      float py = (float)this.tileEntityRenderer.playerY;
      float pz = (float)this.tileEntityRenderer.playerZ;
      GL11.glDisable(2896);
      Random random = new Random(31100L);
      float offset = 0.999F;

      for (int i = 0; i < 16; i++) {
         GL11.glPushMatrix();
         float f5 = 16 - i;
         float f6 = 0.0625F;
         float f7 = 1.0F / (f5 + 1.0F);
         if (i == 0) {
            this.bindTextureByName(this.t1);
            f7 = 0.1F;
            f5 = 65.0F;
            f6 = 0.125F;
            GL11.glEnable(3042);
            GL11.glBlendFunc(770, 771);
         }

         if (i == 1) {
            this.bindTextureByName(this.t2);
            GL11.glEnable(3042);
            GL11.glBlendFunc(1, 1);
            f6 = 0.5F;
         }

         float f8 = (float)(x + offset);
         float f9 = f8 - ActiveRenderInfo.objectX;
         float f10 = f8 + f5 - ActiveRenderInfo.objectX;
         float f11 = f9 / f10;
         f11 = (float)(x + offset) + f11;
         GL11.glTranslatef(f11, py, pz);
         GL11.glTexGen(8193, 9473, this.calcFloatBuffer(0.0F, 1.0F, 0.0F, 0.0F));
         GL11.glTexGen(8192, 9473, this.calcFloatBuffer(0.0F, 0.0F, 1.0F, 0.0F));
         GL11.glTexGen(8194, 9473, this.calcFloatBuffer(0.0F, 0.0F, 0.0F, 1.0F));
         GL11.glTexGen(8195, 9474, this.calcFloatBuffer(1.0F, 0.0F, 0.0F, 0.0F));
         GL11.glEnable(3168);
         GL11.glEnable(3169);
         GL11.glEnable(3170);
         GL11.glEnable(3171);
         GL11.glPopMatrix();
         GL11.glMatrixMode(5890);
         GL11.glPushMatrix();
         GL11.glLoadIdentity();
         GL11.glTranslatef(0.0F, (float)(clockMillis() % 700000L) / 250000.0F, 0.0F);
         GL11.glScalef(f6, f6, f6);
         GL11.glTranslatef(0.5F, 0.5F, 0.0F);
         GL11.glRotatef((i * i * 4321 + i * 9) * 2.0F, 0.0F, 0.0F, 1.0F);
         GL11.glTranslatef(-0.5F, -0.5F, 0.0F);
         GL11.glTranslatef(-pz, -py, -px);
         GL11.glTranslatef(ActiveRenderInfo.objectZ * f5 / f9, ActiveRenderInfo.objectY * f5 / f9, -px);
         Tessellator tessellator = Tessellator.instance;
         tessellator.startDrawingQuads();
         f11 = random.nextFloat() * 0.5F + 0.1F;
         float f12 = random.nextFloat() * 0.5F + 0.4F;
         float f13 = random.nextFloat() * 0.5F + 0.5F;
         if (i == 0) {
            f13 = 1.0F;
            f12 = 1.0F;
            f11 = 1.0F;
         }

         tessellator.setBrightness(180);
         tessellator.setColorRGBA_F(f11 * f7, f12 * f7, f13 * f7, 1.0F);
         tessellator.addVertex(x + offset, y, z);
         tessellator.addVertex(x + offset, y, z + 1.0);
         tessellator.addVertex(x + offset, y + 1.0, z + 1.0);
         tessellator.addVertex(x + offset, y + 1.0, z);
         tessellator.draw();
         GL11.glPopMatrix();
         GL11.glMatrixMode(5888);
      }

      GL11.glDisable(3042);
      GL11.glDisable(3168);
      GL11.glDisable(3169);
      GL11.glDisable(3170);
      GL11.glDisable(3171);
      GL11.glEnable(2896);
   }

   private FloatBuffer calcFloatBuffer(float f, float f1, float f2, float f3) {
      ((Buffer)this.fBuffer).clear();
      this.fBuffer.put(f).put(f1).put(f2).put(f3);
      ((Buffer)this.fBuffer).flip();
      return this.fBuffer;
   }

   @Override
   public void renderTileEntityAt(TileEntity te, double x, double y, double z, float f) {
      double var10002 = te.xCoord + 0.5;
      double var10003 = te.yCoord + 0.5;
      double var10004 = te.zCoord;
      if (ThaumCraftCore.isVisibleTo(1.5F, ModLoader.getMinecraftInstance().thePlayer, var10002, var10003, var10004 + 0.5)) {
         if (te.worldObj.isBlockOpaqueCube(te.xCoord, te.yCoord + 1, te.zCoord)) {
            this.drawPlaneYPos((TileVoidHole)te, x, y, z, f);
         }

         if (te.worldObj.isBlockOpaqueCube(te.xCoord, te.yCoord - 1, te.zCoord)) {
            this.drawPlaneYNeg((TileVoidHole)te, x, y, z, f);
         }

         if (te.worldObj.isBlockOpaqueCube(te.xCoord, te.yCoord, te.zCoord - 1)) {
            this.drawPlaneZNeg((TileVoidHole)te, x, y, z, f);
         }

         if (te.worldObj.isBlockOpaqueCube(te.xCoord, te.yCoord, te.zCoord + 1)) {
            this.drawPlaneZPos((TileVoidHole)te, x, y, z, f);
         }

         if (te.worldObj.isBlockOpaqueCube(te.xCoord - 1, te.yCoord, te.zCoord)) {
            this.drawPlaneXNeg((TileVoidHole)te, x, y, z, f);
         }

         if (te.worldObj.isBlockOpaqueCube(te.xCoord + 1, te.yCoord, te.zCoord)) {
            this.drawPlaneXPos((TileVoidHole)te, x, y, z, f);
         }
      }
   }
}
