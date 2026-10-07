// Original ThaumCraft 2.1.6d drawing code by Azanor, adapted for the modern renderer.
package dev.thaumcraft.client.legacy;

import static dev.thaumcraft.client.legacy.LegacyCompat.*;


public class TileBoreRenderer extends TileEntitySpecialRenderer {

   public void renderEntityAt(TileBore cr, double x, double y, double z, float fq) {
      if (cr.focus != -1) {
         Minecraft mc = ModLoader.getMinecraftInstance();
         int count = ModLoader.getMinecraftInstance().thePlayer.ticksExisted;
         float bob = 0.0F;
         float angleS = cr.rotation;
         float jitter = 0.0F;
         if (cr.duration > 0 && cr.gettingPower()) {
            jitter = (cr.worldObj.rand.nextFloat() - cr.worldObj.rand.nextFloat()) * 0.1F;
         }

         this.translateFromOrientation(x, y, z, cr.orientation);
         GL11.glTranslatef(0.5F, 0.5F, 0.0F);
         GL11.glRotatef(angleS, 0.0F, 0.0F, 1.0F);
         GL11.glTranslatef(-0.2F, -0.2F, 0.0F);
         ThaumCraftRenderer.renderItemFromTexture(
            mc, "/thaumcraft/resources/items.png", 16, 43 + cr.focus, 0.4F, 1.5F + jitter, true, 1.0F, 1.0F, 1.0F, 220, 771
         );
      }
   }

   private void translateFromOrientation(double x, double y, double z, int orientation) {
      GL11.glPushMatrix();
      if (orientation == 0) {
         GL11.glTranslatef((float)x, (float)y, (float)z + 1.0F);
         GL11.glRotatef(-90.0F, 1.0F, 0.0F, 0.0F);
      } else if (orientation == 1) {
         GL11.glTranslatef((float)x, (float)y + 1.0F, (float)z);
         GL11.glRotatef(90.0F, 1.0F, 0.0F, 0.0F);
      } else if (orientation == 2) {
         GL11.glTranslatef((float)x, (float)y, (float)z);
      } else if (orientation == 3) {
         GL11.glTranslatef((float)x + 1.0F, (float)y, (float)z + 1.0F);
         GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F);
      } else if (orientation == 4) {
         GL11.glTranslatef((float)x, (float)y, (float)z + 1.0F);
         GL11.glRotatef(90.0F, 0.0F, 1.0F, 0.0F);
      } else if (orientation == 5) {
         GL11.glTranslatef((float)x + 1.0F, (float)y, (float)z);
         GL11.glRotatef(-90.0F, 0.0F, 1.0F, 0.0F);
      }

      GL11.glPushMatrix();
   }

   @Override
   public void renderTileEntityAt(TileEntity te, double d, double d1, double d2, float f) {
      double var10002 = te.xCoord + 0.5;
      double var10003 = te.yCoord + 0.5;
      double var10004 = te.zCoord;
      if (ThaumCraftCore.isVisibleTo(1.5F, ModLoader.getMinecraftInstance().thePlayer, var10002, var10003, var10004 + 0.5)) {
         this.renderEntityAt((TileBore)te, d, d1, d2, f);
      }
   }
}
