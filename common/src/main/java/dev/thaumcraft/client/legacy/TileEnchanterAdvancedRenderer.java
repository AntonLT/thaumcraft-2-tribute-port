// Original ThaumCraft 2.1.6d drawing code by Azanor, adapted for the modern renderer.
package dev.thaumcraft.client.legacy;

import static dev.thaumcraft.client.legacy.LegacyCompat.*;


public class TileEnchanterAdvancedRenderer extends TileEntitySpecialRenderer {
   private ModelBrain model = new ModelBrain();

   public void renderBrain(TileEnchanterAdvanced te, double x, double y, double z, float f) {
      GL11.glPushMatrix();
      GL11.glTranslatef((float)x + 0.5F, (float)y + 1.075F, (float)z + 0.5F);
      float f1 = te.bobbin + f;
      GL11.glTranslatef(0.0F, 0.1F + MathHelper.sin(f1 * 0.1F) * 0.01F, 0.0F);
      float f2 = te.rota - te.rotb;

      while (f2 >= 3.141593F) {
         f2 -= 6.283185F;
      }

      while (f2 < -3.141593F) {
         f2 += 6.283185F;
      }

      float f3 = te.rotb + f2 * f;
      GL11.glRotatef(-f3 * 180.0F / 3.141593F, 0.0F, 1.0F, 0.0F);
      GL11.glRotatef(180.0F, 0.0F, 0.0F, 1.0F);
      GL11.glRotatef(90.0F, 0.0F, 1.0F, 0.0F);
      this.bindTextureByName("/thaumcraft/resources/brain2.png");
      GL11.glScalef(0.55F, 0.55F, 0.55F);
      this.model.render();
      GL11.glScalef(1.0F, 1.0F, 1.0F);
      GL11.glPopMatrix();
   }

   @Override
   public void renderTileEntityAt(TileEntity tileentity, double d, double d1, double d2, float f) {
      this.renderBrain((TileEnchanterAdvanced)tileentity, d, d1, d2, f);
   }
}
