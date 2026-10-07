// Original ThaumCraft 2.1.6d drawing code by Azanor, adapted for the modern renderer.
package dev.thaumcraft.client.legacy;

import static dev.thaumcraft.client.legacy.LegacyCompat.*;


public class TileSealRenderer extends TileEntitySpecialRenderer {
   private float bob = 0.0F;
   private int count = 0;
   private static int[] colors = new int[]{13532671, 16777088, 8421631, 8454016, 16744576, 4194368};

   private void translateFromOrientation(double x, double y, double z, int orientation) {
      GL11.glPushMatrix();
      if (orientation == 0) {
         GL11.glTranslatef((float)x, (float)y + 1.0F, (float)z + 1.0F);
         GL11.glRotatef(-90.0F, 1.0F, 0.0F, 0.0F);
      } else if (orientation == 1) {
         GL11.glTranslatef((float)x, (float)y, (float)z);
         GL11.glRotatef(90.0F, 1.0F, 0.0F, 0.0F);
      } else if (orientation == 2) {
         GL11.glTranslatef((float)x, (float)y, (float)z + 1.0F);
      } else if (orientation == 3) {
         GL11.glTranslatef((float)x + 1.0F, (float)y, (float)z);
         GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F);
      } else if (orientation == 4) {
         GL11.glTranslatef((float)x + 1.0F, (float)y, (float)z + 1.0F);
         GL11.glRotatef(90.0F, 0.0F, 1.0F, 0.0F);
      } else if (orientation == 5) {
         GL11.glTranslatef((float)x, (float)y, (float)z);
         GL11.glRotatef(-90.0F, 0.0F, 1.0F, 0.0F);
      }

      GL11.glPushMatrix();
   }

   private void drawSeal(float angle, int level, int rune) {
      Tessellator tessellator = Tessellator.instance;
      GL11.glRotatef(90.0F, -1.0F, 0.0F, 0.0F);
      GL11.glRotatef(angle, 0.0F, 1.0F, 0.0F);
      GL11.glTranslatef(-0.5F, 0.0F, -0.5F);
      GL11.glDepthMask(false);
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 1);
      if (level != 2) {
         MinecraftForgeClient.bindTexture("/thaumcraft/resources/s_" + level + "_" + rune + ".png");
      } else {
         MinecraftForgeClient.bindTexture("/thaumcraft/resources/seal5.png");
      }

      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      tessellator.startDrawingQuads();
      tessellator.setBrightness(220);
      if (level == 2) {
         tessellator.setColorRGBA_I(colors[rune], 255);
      }

      tessellator.addVertexWithUV(0.0, 0.0, 1.0, 0.0, 1.0);
      tessellator.addVertexWithUV(1.0, 0.0, 1.0, 1.0, 1.0);
      tessellator.addVertexWithUV(1.0, 0.0, 0.0, 1.0, 0.0);
      tessellator.addVertexWithUV(0.0, 0.0, 0.0, 0.0, 0.0);
      tessellator.draw();
      GL11.glDisable(3042);
      GL11.glDepthMask(true);
      GL11.glPopMatrix();
      GL11.glPopMatrix();
   }

   private void drawPortal(TileSeal seal, float angle, double x, double y, double z) {
      Tessellator tessellator = Tessellator.instance;
      Minecraft mc = ModLoader.getMinecraftInstance();
      GL11.glDisable(2896);
      if (Config.portalGfx && seal.txRender != null && PortalRenderer.renderRecursion < 2) {
         GL11.glPushMatrix();
         GL11.glDisable(3553);
         GL11.glColor4f(ThaumCraftCore.fColorR(), ThaumCraftCore.fColorG(), ThaumCraftCore.fColorB(), 1.0F);
         tessellator.setBrightness(220);
         GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F);
         GL11.glScaled(seal.pSize / 2.0F, seal.pSize / 2.0F, seal.pSize / 2.0F);
         GL11.glBegin(6);
         GL11.glVertex2f(0.0F, 0.0F);

         for (int oh = 0; oh <= 10; oh++) {
            double aa = 6.283185307179586 * oh / 10.0;
            GL11.glVertex2f((float)Math.cos(aa), (float)Math.sin(aa));
         }

         GL11.glEnd();
         GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
         GL11.glPopMatrix();
         GL11.glEnable(3553);
         GL11.glPushMatrix();
         GL11.glDisable(2896);
         GL11.glEnable(3042);
         GL11.glBlendFunc(770, 771);
         GL11.glRotatef(90.0F, 1.0F, 0.0F, 0.0F);
         GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F);
         GL11.glTranslatef(-seal.pSize / 2.0F, -0.01F, -seal.pSize / 2.0F);
         GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
         GL11.glBindTexture(3553, seal.txRender.portalTexture);
         tessellator.startDrawingQuads();
         tessellator.setBrightness(220);
         tessellator.setColorRGBA_F(1.0F, 1.0F, 1.0F, 1.0F);
         tessellator.addVertexWithUV(0.0, 0.0, 0.0, 0.0, 0.0);
         tessellator.addVertexWithUV(seal.pSize, 0.0, 0.0, 1.0, 0.0);
         tessellator.addVertexWithUV(seal.pSize, 0.0, seal.pSize, 1.0, 1.0);
         tessellator.addVertexWithUV(0.0, 0.0, seal.pSize, 0.0, 1.0);
         tessellator.draw();
         GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
         GL11.glDisable(3042);
         GL11.glPopMatrix();
      }

      GL11.glPushMatrix();
      GL11.glRotatef(90.0F, -1.0F, 0.0F, 0.0F);
      GL11.glRotatef(angle, 0.0F, 1.0F, 0.0F);
      GL11.glTranslatef(-seal.pSize, 0.02F, -seal.pSize);
      GL11.glDepthMask(false);
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 771);
      if (Config.portalGfx && seal.txRender != null) {
         MinecraftForgeClient.bindTexture("/thaumcraft/resources/portal2.png");
      } else {
         MinecraftForgeClient.bindTexture("/thaumcraft/resources/portal.png");
      }

      tessellator.startDrawingQuads();
      tessellator.setBrightness(220);
      tessellator.setColorRGBA_F(1.0F, 1.0F, 1.0F, 1.0F);
      tessellator.addVertexWithUV(0.0, 0.0, seal.pSize * 2.0F, 0.0, 1.0);
      tessellator.addVertexWithUV(seal.pSize * 2.0F, 0.0, seal.pSize * 2.0F, 1.0, 1.0);
      tessellator.addVertexWithUV(seal.pSize * 2.0F, 0.0, 0.0, 1.0, 0.0);
      tessellator.addVertexWithUV(0.0, 0.0, 0.0, 0.0, 0.0);
      tessellator.draw();
      GL11.glDisable(3042);
      GL11.glDepthMask(true);
      GL11.glPopMatrix();
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      GL11.glEnable(2896);
      GL11.glPopMatrix();
      GL11.glPopMatrix();
   }

   public void renderEntityAt(TileSeal seal, double x, double y, double z, float fq) {
      int a = this.count % 360;
      Minecraft mc = ModLoader.getMinecraftInstance();
      this.translateFromOrientation((float)x, (float)y, (float)z, seal.orientation);
      GL11.glTranslatef(0.33F, 0.33F, 0.0F);
      ThaumCraftRenderer.renderItemFromTexture(mc, "/thaumcraft/resources/blocks.png", 16, 46, 0.33F, 0.1F, false, 1.0F, 1.0F, 1.0F, 220, 771);
      this.translateFromOrientation((float)x, (float)y, (float)z, seal.orientation);
      GL11.glPushMatrix();
      boolean nopush = true;
      if (seal.runes[0] != -1) {
         GL11.glTranslatef(0.5F, 0.5F, -0.015F);
         this.drawSeal(180.0F, 0, seal.runes[0]);
         nopush = false;
      }

      if (seal.runes[1] != -1) {
         GL11.glPushMatrix();
         GL11.glPushMatrix();
         GL11.glTranslatef(0.5F, 0.5F, -0.02F);
         this.drawSeal(-a, 1, seal.runes[1]);
         nopush = false;
      }

      if (seal.runes[2] != -1) {
         GL11.glPushMatrix();
         GL11.glPushMatrix();
         GL11.glTranslatef(0.5F, 0.5F, -0.02F - this.bob);
         this.drawSeal(a, 2, seal.runes[2]);
         nopush = false;
      }

      if (seal.runes[0] == 0 && seal.runes[1] == 1 && seal.pSize > 0.0F) {
         GL11.glPushMatrix();
         GL11.glPushMatrix();
         GL11.glTranslatef(0.5F, 0.5F, -seal.pSize / 5.0F);
         this.drawPortal(seal, -a * 4, x, y, z);
         nopush = false;
      }

      if (nopush) {
         GL11.glPopMatrix();
         GL11.glPopMatrix();
      }

      GL11.glPopMatrix();
   }

   @Override
   public void renderTileEntityAt(TileEntity tileentity, double d, double d1, double d2, float f) {
      double var10002 = tileentity.xCoord + 0.5;
      double var10003 = tileentity.yCoord + 0.5;
      double var10004 = tileentity.zCoord;
      if (ThaumCraftCore.isVisibleTo(1.3F, ModLoader.getMinecraftInstance().thePlayer, var10002, var10003, var10004 + 0.5)) {
         this.count = ModLoader.getMinecraftInstance().thePlayer.ticksExisted;
         this.bob = MathHelper.sin(this.count / 10.0F) * 0.025F + 0.03F;
         this.renderEntityAt((TileSeal)tileentity, d, d1, d2, f);
      }
   }
}
