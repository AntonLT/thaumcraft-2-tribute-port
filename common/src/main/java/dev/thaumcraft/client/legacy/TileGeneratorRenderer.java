// Original ThaumCraft 2.1.6d drawing code by Azanor, adapted for the modern renderer.
package dev.thaumcraft.client.legacy;

import static dev.thaumcraft.client.legacy.LegacyCompat.*;


public class TileGeneratorRenderer extends TileEntitySpecialRenderer {
   private int modelLoaded = -1;
   private ModelGenCore model;

   public void renderCore(TileGenerator gen, double x, double y, double z, float f) {
      if (this.modelLoaded != 1) {
         this.model = new ModelGenCore(0.0F);
         this.modelLoaded = 1;
      }

      float f2 = gen.rotation + f;
      GL11.glPushMatrix();
      GL11.glTranslatef((float)x + 0.5F, (float)y + 1.3F, (float)z + 0.5F);
      this.bindTextureByName("/thaumcraft/resources/gencore.png");
      float f3 = MathHelper.sin(f2 * 0.2F) / 2.0F + 0.5F;
      f3 = f3 * f3 + f3;
      this.model.render(0.0F, f2 * 1.0F, f3 * 0.01F, 0.0F, 0.0F, 0.0625F);
      GL11.glPopMatrix();
   }

   @Override
   public void renderTileEntityAt(TileEntity tileentity, double x, double y, double z, float f) {
      this.renderCore((TileGenerator)tileentity, x, y, z, f);
   }
}
