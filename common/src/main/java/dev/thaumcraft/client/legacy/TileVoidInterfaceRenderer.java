// Original ThaumCraft 2.1.6d drawing code by Azanor, adapted for the modern renderer.
package dev.thaumcraft.client.legacy;

import static dev.thaumcraft.client.legacy.LegacyCompat.*;


public class TileVoidInterfaceRenderer extends TileEntitySpecialRenderer {
   @Override
   public void renderTileEntityAt(TileEntity te, double x, double y, double z, float f) {
      double var10002 = te.xCoord + 0.5;
      double var10003 = te.yCoord + 0.5;
      double var10004 = te.zCoord;
      if (ThaumCraftCore.isVisibleTo(1.5F, ModLoader.getMinecraftInstance().thePlayer, var10002, var10003, var10004 + 0.5)) {
         for (int a = 0; a < 4; a++) {
            float xx = 0.0F;
            float zz = 0.0F;
            switch (a) {
               case 0:
                  xx = 0.375F;
                  zz = 0.2F;
                  break;
               case 1:
                  zz = -0.375F;
                  xx = 0.2F;
                  break;
               case 2:
                  xx = -0.375F;
                  zz = -0.2F;
                  break;
               case 3:
                  zz = 0.375F;
                  xx = -0.2F;
            }

            GL11.glPushMatrix();
            GL11.glTranslatef((float)x + (a != 2 && a != 3 ? 0 : 1), (float)y + 0.44F, (float)z + (a != 1 && a != 2 ? 0 : 1));
            GL11.glTranslatef(xx, 0.0F, zz);
            GL11.glRotatef(90.0F * a, 0.0F, 1.0F, 0.0F);
            GL11.glPushMatrix();
            ThaumCraftRenderer.renderItemFromTexture(
               ModLoader.getMinecraftInstance(),
               "/thaumcraft/resources/particles.png",
               8,
               56 + ((TileVoidInterface)te).network,
               0.25F,
               0.0F,
               false,
               1.0F,
               1.0F,
               1.0F,
               220,
               1
            );
         }
      }
   }
}
