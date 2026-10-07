// Original ThaumCraft 2.1.6d drawing code by Azanor, adapted for the modern renderer.
package dev.thaumcraft.client.legacy;

import static dev.thaumcraft.client.legacy.LegacyCompat.*;

import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.util.Random;

public class TileVoidCubeRenderer extends TileEntitySpecialRenderer {
   FloatBuffer fBuffer;
   private boolean inrange;
   private ModelCrystal model;
   private String t1 = "/thaumcraft/resources/tunnel.png";
   private String t2 = "/thaumcraft/resources/particlefield.png";
   private String t3 = "/thaumcraft/resources/particlefield32.png";

   public TileVoidCubeRenderer() {
      this.fBuffer = GLAllocation.createDirectFloatBuffer(16);
      this.model = new ModelCrystal();
   }

   public void drawPlaneYNeg(double x, double y, double z, float f) {
      float px = (float)this.tileEntityRenderer.playerX;
      float py = (float)this.tileEntityRenderer.playerY;
      float pz = (float)this.tileEntityRenderer.playerZ;
      GL11.glDisable(2896);
      Random random = new Random(31100L);
      float offset = 0.0F;
      if (this.inrange) {
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
      } else {
         GL11.glPushMatrix();
         this.bindTextureByName(this.t3);
         Tessellator tessellator = Tessellator.instance;
         tessellator.startDrawingQuads();
         tessellator.setBrightness(180);
         tessellator.setColorRGBA_F(0.5F, 0.5F, 0.5F, 1.0F);
         tessellator.addVertexWithUV(x, y + offset, z + 1.0, 0.0, 1.0);
         tessellator.addVertexWithUV(x, y + offset, z, 0.0, 0.0);
         tessellator.addVertexWithUV(x + 1.0, y + offset, z, 1.0, 0.0);
         tessellator.addVertexWithUV(x + 1.0, y + offset, z + 1.0, 1.0, 1.0);
         tessellator.draw();
         GL11.glPopMatrix();
      }

      GL11.glDisable(3042);
      GL11.glDisable(3168);
      GL11.glDisable(3169);
      GL11.glDisable(3170);
      GL11.glDisable(3171);
      GL11.glEnable(2896);
   }

   public void drawPlaneYPos(double x, double y, double z, float f) {
      float f1 = (float)this.tileEntityRenderer.playerX;
      float f2 = (float)this.tileEntityRenderer.playerY;
      float f3 = (float)this.tileEntityRenderer.playerZ;
      GL11.glDisable(2896);
      Random random = new Random(31100L);
      float offset = 1.0F;
      if (this.inrange) {
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
      } else {
         GL11.glPushMatrix();
         this.bindTextureByName(this.t3);
         Tessellator tessellator = Tessellator.instance;
         tessellator.startDrawingQuads();
         tessellator.setBrightness(180);
         tessellator.setColorRGBA_F(0.5F, 0.5F, 0.5F, 1.0F);
         tessellator.addVertexWithUV(x, y + offset, z, 0.0, 1.0);
         tessellator.addVertexWithUV(x, y + offset, z + 1.0, 0.0, 0.0);
         tessellator.addVertexWithUV(x + 1.0, y + offset, z + 1.0, 1.0, 0.0);
         tessellator.addVertexWithUV(x + 1.0, y + offset, z, 1.0, 1.0);
         tessellator.draw();
         GL11.glPopMatrix();
      }

      GL11.glDisable(3042);
      GL11.glDisable(3168);
      GL11.glDisable(3169);
      GL11.glDisable(3170);
      GL11.glDisable(3171);
      GL11.glEnable(2896);
   }

   public void drawPlaneZPos(double x, double y, double z, float f) {
      float px = (float)this.tileEntityRenderer.playerX;
      float py = (float)this.tileEntityRenderer.playerY;
      float pz = (float)this.tileEntityRenderer.playerZ;
      GL11.glDisable(2896);
      Random random = new Random(31100L);
      float offset = 1.0F;
      if (this.inrange) {
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
      } else {
         GL11.glPushMatrix();
         this.bindTextureByName(this.t3);
         Tessellator tessellator = Tessellator.instance;
         tessellator.startDrawingQuads();
         tessellator.setBrightness(180);
         tessellator.setColorRGBA_F(0.5F, 0.5F, 0.5F, 1.0F);
         tessellator.addVertexWithUV(x, y + 1.0, z + offset, 0.0, 1.0);
         tessellator.addVertexWithUV(x, y, z + offset, 0.0, 0.0);
         tessellator.addVertexWithUV(x + 1.0, y, z + offset, 1.0, 0.0);
         tessellator.addVertexWithUV(x + 1.0, y + 1.0, z + offset, 1.0, 1.0);
         tessellator.draw();
         GL11.glPopMatrix();
      }

      GL11.glDisable(3042);
      GL11.glDisable(3168);
      GL11.glDisable(3169);
      GL11.glDisable(3170);
      GL11.glDisable(3171);
      GL11.glEnable(2896);
   }

   public void drawPlaneZNeg(double x, double y, double z, float f) {
      float px = (float)this.tileEntityRenderer.playerX;
      float py = (float)this.tileEntityRenderer.playerY;
      float pz = (float)this.tileEntityRenderer.playerZ;
      GL11.glDisable(2896);
      Random random = new Random(31100L);
      float offset = 0.0F;
      if (this.inrange) {
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
      } else {
         GL11.glPushMatrix();
         this.bindTextureByName(this.t3);
         Tessellator tessellator = Tessellator.instance;
         tessellator.startDrawingQuads();
         tessellator.setBrightness(180);
         tessellator.setColorRGBA_F(0.5F, 0.5F, 0.5F, 1.0F);
         tessellator.addVertexWithUV(x, y, z + offset, 0.0, 1.0);
         tessellator.addVertexWithUV(x, y + 1.0, z + offset, 0.0, 0.0);
         tessellator.addVertexWithUV(x + 1.0, y + 1.0, z + offset, 1.0, 0.0);
         tessellator.addVertexWithUV(x + 1.0, y, z + offset, 1.0, 1.0);
         tessellator.draw();
         GL11.glPopMatrix();
      }

      GL11.glDisable(3042);
      GL11.glDisable(3168);
      GL11.glDisable(3169);
      GL11.glDisable(3170);
      GL11.glDisable(3171);
      GL11.glEnable(2896);
   }

   public void drawPlaneXPos(double x, double y, double z, float f) {
      float px = (float)this.tileEntityRenderer.playerX;
      float py = (float)this.tileEntityRenderer.playerY;
      float pz = (float)this.tileEntityRenderer.playerZ;
      GL11.glDisable(2896);
      Random random = new Random(31100L);
      float offset = 1.0F;
      if (this.inrange) {
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
      } else {
         GL11.glPushMatrix();
         this.bindTextureByName(this.t3);
         Tessellator tessellator = Tessellator.instance;
         tessellator.startDrawingQuads();
         tessellator.setBrightness(180);
         tessellator.setColorRGBA_F(0.5F, 0.5F, 0.5F, 1.0F);
         tessellator.addVertexWithUV(x + offset, y + 1.0, z, 0.0, 1.0);
         tessellator.addVertexWithUV(x + offset, y + 1.0, z + 1.0, 0.0, 0.0);
         tessellator.addVertexWithUV(x + offset, y, z + 1.0, 1.0, 0.0);
         tessellator.addVertexWithUV(x + offset, y, z, 1.0, 1.0);
         tessellator.draw();
         GL11.glPopMatrix();
      }

      GL11.glDisable(3042);
      GL11.glDisable(3168);
      GL11.glDisable(3169);
      GL11.glDisable(3170);
      GL11.glDisable(3171);
      GL11.glEnable(2896);
   }

   public void drawPlaneXNeg(double x, double y, double z, float f) {
      float px = (float)this.tileEntityRenderer.playerX;
      float py = (float)this.tileEntityRenderer.playerY;
      float pz = (float)this.tileEntityRenderer.playerZ;
      GL11.glDisable(2896);
      Random random = new Random(31100L);
      float offset = 0.0F;
      if (this.inrange) {
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
      } else {
         GL11.glPushMatrix();
         this.bindTextureByName(this.t3);
         Tessellator tessellator = Tessellator.instance;
         tessellator.startDrawingQuads();
         tessellator.setBrightness(180);
         tessellator.setColorRGBA_F(0.5F, 0.5F, 0.5F, 1.0F);
         tessellator.addVertexWithUV(x + offset, y, z, 0.0, 1.0);
         tessellator.addVertexWithUV(x + offset, y, z + 1.0, 0.0, 0.0);
         tessellator.addVertexWithUV(x + offset, y + 1.0, z + 1.0, 1.0, 0.0);
         tessellator.addVertexWithUV(x + offset, y + 1.0, z, 1.0, 1.0);
         tessellator.draw();
         GL11.glPopMatrix();
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
         this.inrange = true;
         double var10001 = te.xCoord + 0.5;
         var10002 = te.yCoord + 0.5;
         var10003 = te.zCoord;
         int vdist = (int)ModLoader.getMinecraftInstance().thePlayer.getDistance(var10001, var10002, var10003 + 0.5);
         int j = 64 << 3 - ModLoader.getMinecraftInstance().gameSettings.renderDistance;
         if (j > 400) {
            j = 400;
         }

         if (vdist > j / 16) {
            this.inrange = false;
         }

         float bob = 0.0F;
         if (te.getBlockMetadata() == 2) {
            int count = ModLoader.getMinecraftInstance().thePlayer.ticksExisted;
            bob = MathHelper.sin(count / 10.0F) * 0.1F + 0.1F;
         }

         if (te.getBlockMetadata() != 8
            && te.getBlockMetadata() != 4
            && !te.worldObj.isBlockOpaqueCube(te.xCoord, te.yCoord + 1, te.zCoord)
            && te.worldObj.getBlockId(te.xCoord, te.yCoord + 1, te.zCoord) != mod_ThaumCraft.blockHidden.blockID) {
            this.drawPlaneYPos(x, y + bob, z, f);
         }

         if (te.getBlockMetadata() != 5
            && te.getBlockMetadata() != 3
            && te.getBlockMetadata() != 8
            && te.getBlockMetadata() != 4
            && !te.worldObj.isBlockOpaqueCube(te.xCoord, te.yCoord - 1, te.zCoord)
            && te.worldObj.getBlockId(te.xCoord, te.yCoord - 1, te.zCoord) != mod_ThaumCraft.blockHidden.blockID) {
            this.drawPlaneYNeg(x, y + bob, z, f);
         }

         if (te.getBlockMetadata() != 5
            && te.getBlockMetadata() != 3
            && !te.worldObj.isBlockOpaqueCube(te.xCoord, te.yCoord, te.zCoord - 1)
            && te.worldObj.getBlockId(te.xCoord, te.yCoord, te.zCoord - 1) != mod_ThaumCraft.blockHidden.blockID) {
            this.drawPlaneZNeg(x, y + bob, z, f);
         }

         if (te.getBlockMetadata() != 5
            && te.getBlockMetadata() != 3
            && !te.worldObj.isBlockOpaqueCube(te.xCoord, te.yCoord, te.zCoord + 1)
            && te.worldObj.getBlockId(te.xCoord, te.yCoord, te.zCoord + 1) != mod_ThaumCraft.blockHidden.blockID) {
            this.drawPlaneZPos(x, y + bob, z, f);
         }

         if (te.getBlockMetadata() != 5
            && te.getBlockMetadata() != 3
            && !te.worldObj.isBlockOpaqueCube(te.xCoord - 1, te.yCoord, te.zCoord)
            && te.worldObj.getBlockId(te.xCoord - 1, te.yCoord, te.zCoord) != mod_ThaumCraft.blockHidden.blockID) {
            this.drawPlaneXNeg(x, y + bob, z, f);
         }

         if (te.getBlockMetadata() != 5
            && te.getBlockMetadata() != 3
            && !te.worldObj.isBlockOpaqueCube(te.xCoord + 1, te.yCoord, te.zCoord)
            && te.worldObj.getBlockId(te.xCoord + 1, te.yCoord, te.zCoord) != mod_ThaumCraft.blockHidden.blockID) {
            this.drawPlaneXPos(x, y + bob, z, f);
         }

         if (te.getBlockMetadata() == 2) {
            int b = te.getBlockType().getMixedBrightnessForBlock(te.worldObj, te.xCoord, te.yCoord, te.zCoord);
            int tx = 100;
            if (te.worldObj.getBlockId(te.xCoord, te.yCoord + 1, te.zCoord) != mod_ThaumCraft.blockHidden.blockID) {
               tx = 101;
               GL11.glPushMatrix();
               GL11.glTranslatef((float)x, (float)y + 1.0F + bob, (float)z);
               GL11.glRotatef(90.0F, 1.0F, 0.0F, 0.0F);
               GL11.glPushMatrix();
               ThaumCraftRenderer.renderItemFromTexture(
                  ModLoader.getMinecraftInstance(), "/thaumcraft/resources/blocks.png", 16, 99, 1.0F, 0.001F, false, 1.0F, 1.0F, 1.0F, b, 771
               );
            }

            if (te.worldObj.getBlockId(te.xCoord, te.yCoord - 1, te.zCoord) != mod_ThaumCraft.blockHidden.blockID) {
               tx = 102;
               GL11.glPushMatrix();
               GL11.glTranslatef((float)x, (float)y + bob, (float)z + 1.0F);
               GL11.glRotatef(-90.0F, 1.0F, 0.0F, 0.0F);
               GL11.glPushMatrix();
               ThaumCraftRenderer.renderItemFromTexture(
                  ModLoader.getMinecraftInstance(), "/thaumcraft/resources/blocks.png", 16, 99, 1.0F, 0.001F, false, 1.0F, 1.0F, 1.0F, b, 771
               );
            }

            for (int a = 0; a < 4; a++) {
               GL11.glPushMatrix();
               GL11.glTranslatef((float)x + (a != 2 && a != 3 ? 0 : 1), (float)y + bob, (float)z + (a != 1 && a != 2 ? 0 : 1));
               GL11.glRotatef(90.0F * a, 0.0F, 1.0F, 0.0F);
               GL11.glPushMatrix();
               ThaumCraftRenderer.renderItemFromTexture(
                  ModLoader.getMinecraftInstance(), "/thaumcraft/resources/blocks.png", 16, tx, 0.999F, 0.001F, false, 1.0F, 1.0F, 1.0F, b, 771
               );
            }
         } else if (te.getBlockMetadata() == 3) {
            GL11.glPushMatrix();
            GL11.glTranslatef((float)x, (float)y + 1.0F, (float)z);
            GL11.glRotatef(90.0F, 1.0F, 0.0F, 0.0F);
            GL11.glPushMatrix();
            int b = te.getBlockType().getMixedBrightnessForBlock(te.worldObj, te.xCoord, te.yCoord, te.zCoord);
            ThaumCraftRenderer.renderItemFromTexture(
               ModLoader.getMinecraftInstance(), "/thaumcraft/resources/blocks.png", 16, 98, 1.0F, 0.001F, false, 1.0F, 1.0F, 1.0F, b, 771
            );
            TileVoidCube tvc = (TileVoidCube)te;
            if (tvc.placed != -1) {
               boolean darken = false;
               switch (tvc.placed) {
                  case 1:
                     this.bindTextureByName("/thaumcraft/resources/crystaly.png");
                     break;
                  case 2:
                     this.bindTextureByName("/thaumcraft/resources/crystalb.png");
                     break;
                  case 3:
                     this.bindTextureByName("/thaumcraft/resources/crystalg.png");
                     break;
                  case 4:
                     this.bindTextureByName("/thaumcraft/resources/crystalr.png");
                     break;
                  case 5:
                     this.bindTextureByName("/thaumcraft/resources/crystal.png");
                     darken = true;
                     break;
                  default:
                     this.bindTextureByName("/thaumcraft/resources/crystal.png");
               }

               GL11.glEnable(2977);
               GL11.glEnable(3042);
               GL11.glPushMatrix();
               GL11.glEnable(32826);
               GL11.glBlendFunc(770, 771);
               GL11.glColor4f(darken ? 0.3F : 1.0F, darken ? 0.3F : 1.0F, darken ? 0.3F : 1.0F, 1.0F);
               Tessellator tessellator = Tessellator.instance;
               tessellator.setBrightness(220);
               GL11.glTranslatef((float)x + 0.5F, (float)y + 0.6F, (float)z + 0.5F);
               GL11.glPushMatrix();
               GL11.glScalef(0.15F, 0.45F, 0.15F);
               this.model.render();
               GL11.glScalef(1.0F, 1.0F, 1.0F);
               GL11.glPopMatrix();
               GL11.glDisable(32826);
               GL11.glPopMatrix();
               GL11.glDisable(3042);
               GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            }
         } else if (te.getBlockMetadata() == 8) {
            int b = te.getBlockType().getMixedBrightnessForBlock(te.worldObj, te.xCoord, te.yCoord, te.zCoord);

            for (int a = 0; a < 4; a++) {
               GL11.glPushMatrix();
               GL11.glTranslatef((float)x + (a != 2 && a != 3 ? 0 : 1), (float)y, (float)z + (a != 1 && a != 2 ? 0 : 1));
               GL11.glRotatef(90.0F * a, 0.0F, 1.0F, 0.0F);
               GL11.glPushMatrix();
               ThaumCraftRenderer.renderItemFromTexture(
                  ModLoader.getMinecraftInstance(), "/thaumcraft/resources/blocks.png", 16, 96, 1.0F, 1.0E-4F, false, 1.0F, 1.0F, 1.0F, b, 771
               );
            }
         } else if (te.getBlockMetadata() == 4) {
            int b = te.getBlockType().getMixedBrightnessForBlock(te.worldObj, te.xCoord, te.yCoord, te.zCoord);

            for (int a = 0; a < 4; a++) {
               GL11.glPushMatrix();
               GL11.glTranslatef((float)x + (a != 2 && a != 3 ? 0 : 1), (float)y, (float)z + (a != 1 && a != 2 ? 0 : 1));
               GL11.glRotatef(90.0F * a, 0.0F, 1.0F, 0.0F);
               GL11.glPushMatrix();
               ThaumCraftRenderer.renderItemFromTexture(
                  ModLoader.getMinecraftInstance(), "/thaumcraft/resources/blocks.png", 16, 107, 1.0F, 1.0E-4F, false, 1.0F, 1.0F, 1.0F, b, 771
               );
            }
         } else if (te.getBlockMetadata() == 5) {
            GL11.glPushMatrix();
            GL11.glTranslatef((float)x, (float)y + 1.0F, (float)z);
            GL11.glRotatef(90.0F, 1.0F, 0.0F, 0.0F);
            GL11.glPushMatrix();
            int b = te.getBlockType().getMixedBrightnessForBlock(te.worldObj, te.xCoord, te.yCoord, te.zCoord);
            ThaumCraftRenderer.renderItemFromTexture(
               ModLoader.getMinecraftInstance(), "/thaumcraft/resources/blocks.png", 16, 99, 1.0F, 0.001F, false, 1.0F, 1.0F, 1.0F, b, 771
            );
            if (mod_ThaumCraft.inventory[2] == 1) {
               int count = ModLoader.getMinecraftInstance().thePlayer.ticksExisted;
               bob = MathHelper.sin(count / 10.0F) * 0.1F + 0.1F;
               TileVoidCube tvc = (TileVoidCube)te;

               for (int a = 0; a < 4; a++) {
                  float xx = 0.0F;
                  float zz = 0.0F;
                  switch (a) {
                     case 0:
                        xx = 0.3F;
                        break;
                     case 1:
                        xx = -0.3F;
                        break;
                     case 2:
                        zz = 0.3F;
                        break;
                     case 3:
                        zz = -0.3F;
                  }

                  GL11.glPushMatrix();
                  GL11.glTranslatef((float)x + 0.375F, (float)y + 1.1F, (float)z + 0.375F);
                  GL11.glTranslatef(xx, 0.0F, zz);
                  GL11.glRotatef(90.0F, 1.0F, 0.0F, 0.0F);
                  GL11.glPushMatrix();
                  ThaumCraftRenderer.renderItemFromTexture(
                     ModLoader.getMinecraftInstance(), "/thaumcraft/resources/particles.png", 8, 56 + tvc.runes[a], 0.25F, bob, false, 1.0F, 1.0F, 1.0F, 220, 1
                  );
               }
            }
         }
      }
   }
}
