// Original ThaumCraft 2.1.6d drawing code by Azanor, adapted for the modern renderer.
package dev.thaumcraft.client.legacy;

import static dev.thaumcraft.client.legacy.LegacyCompat.*;


public class TileCondenserRenderer extends TileEntitySpecialRenderer {
   private ModelCrystal model;
   private float bob = 0.0F;

   public TileCondenserRenderer() {
      this.model = new ModelCrystal();
   }

   private void drawDisk(double x, double y, double z, float angle) {
      Tessellator tessellator = Tessellator.instance;
      GL11.glPushMatrix();
      GL11.glTranslatef((float)x + 0.5F, (float)y, (float)z + 0.5F);
      GL11.glPushMatrix();
      GL11.glRotatef(angle, 0.0F, 1.0F, 0.0F);
      GL11.glTranslatef(-0.3F, 0.0F, -0.3F);
      GL11.glDepthMask(false);
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 1);
      MinecraftForgeClient.bindTexture("/thaumcraft/resources/portal2.png");
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      float f10 = 0.25F;
      tessellator.startDrawingQuads();
      tessellator.setBrightness(200);
      tessellator.setColorRGBA_F(1.0F, 0.5F, 1.0F, 1.0F);
      tessellator.addVertexWithUV(0.0, 0.0, 0.6, 0.0, 1.0);
      tessellator.addVertexWithUV(0.6, 0.0, 0.6, 1.0, 1.0);
      tessellator.addVertexWithUV(0.6, 0.0, 0.0, 1.0, 0.0);
      tessellator.addVertexWithUV(0.0, 0.0, 0.0, 0.0, 0.0);
      tessellator.addVertexWithUV(0.0, 0.0, 0.0, 0.0, 1.0);
      tessellator.addVertexWithUV(0.6, 0.0, 0.0, 1.0, 1.0);
      tessellator.addVertexWithUV(0.6, 0.0, 0.6, 1.0, 0.0);
      tessellator.addVertexWithUV(0.0, 0.0, 0.6, 0.0, 0.0);
      tessellator.draw();
      GL11.glDisable(3042);
      GL11.glDepthMask(true);
      GL11.glPopMatrix();
      GL11.glPopMatrix();
   }

   public void renderEntityAt(TileCondenser condenser, double x, double y, double z, float fq) {
      Minecraft mc = ModLoader.getMinecraftInstance();
      if (condenser.degredation > 0.0F) {
         float tbob = this.bob;
         if (condenser.hasUpgrade((byte)1)) {
            tbob = 0.0F;
         }

         boolean darken = false;
         if (condenser.currentType != -1) {
            switch (condenser.currentType) {
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
         } else {
            this.bindTextureByName("/thaumcraft/resources/crystal.png");
         }

         GL11.glEnable(2977);
         GL11.glEnable(3042);
         GL11.glPushMatrix();
         GL11.glEnable(32826);
         GL11.glBlendFunc(770, 771);
         GL11.glColor4f(
            1.0F * condenser.degredation / (darken ? 6000.0F : 3500.0F),
            1.0F * condenser.degredation / (darken ? 6000.0F : 3500.0F),
            1.0F * condenser.degredation / (darken ? 6000.0F : 3500.0F),
            1.0F
         );
         Tessellator tessellator = Tessellator.instance;
         tessellator.setBrightness(220);
         GL11.glTranslatef((float)x + 0.5F, (float)y + tbob + 0.95F, (float)z + 0.5F);
         GL11.glRotatef(condenser.angle, 0.0F, 1.0F, 0.0F);
         GL11.glPushMatrix();
         GL11.glScalef(0.15F, 0.45F, 0.15F);
         this.model.render();
         GL11.glScalef(1.0F, 1.0F, 1.0F);
         GL11.glPopMatrix();
         GL11.glDisable(32826);
         GL11.glPopMatrix();
         GL11.glDisable(3042);
         GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
         if (condenser.hasUpgrade((byte)1)) {
            this.drawDisk(x, y + 1.1749999523162842 + this.bob * 6.0F, z, 360.0F - condenser.angle);
         }
      }
   }

   @Override
   public void renderTileEntityAt(TileEntity te, double d, double d1, double d2, float f) {
      double var10002 = te.xCoord + 0.5;
      double var10003 = te.yCoord + 0.5;
      double var10004 = te.zCoord;
      if (ThaumCraftCore.isVisibleTo(1.15F, ModLoader.getMinecraftInstance().thePlayer, var10002, var10003, var10004 + 0.5)) {
         int count = ModLoader.getMinecraftInstance().thePlayer.ticksExisted;
         this.bob = MathHelper.sin(count / 10.0F) * 0.05F + 0.05F;
         this.renderEntityAt((TileCondenser)te, d, d1, d2, f);
      }
   }
}
