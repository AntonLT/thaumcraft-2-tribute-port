// Original ThaumCraft 2.1.6d drawing code by Azanor, adapted for the modern renderer.
package dev.thaumcraft.client.legacy;

import static dev.thaumcraft.client.legacy.LegacyCompat.*;


public class TileBellowsRenderer extends TileEntitySpecialRenderer {
   private ModelBellows model = new ModelBellows();

   private void translateFromOrientation(double x, double y, double z, int orientation) {
      GL11.glTranslatef((float)x + 0.5F, (float)y - 0.5F, (float)z + 0.5F);
      if (orientation == 0) {
         GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F);
      } else if (orientation == 1) {
         GL11.glRotatef(90.0F, 0.0F, 1.0F, 0.0F);
      } else if (orientation != 2 && orientation == 3) {
         GL11.glRotatef(270.0F, 0.0F, 1.0F, 0.0F);
      }
   }

   public void renderEntityAt(TileBellows bellows, double x, double y, double z, float fq) {
      float tscale = 0.125F + bellows.scale * 0.875F;
      Minecraft mc = ModLoader.getMinecraftInstance();
      this.bindTextureByName("/thaumcraft/resources/Bellows.png");
      GL11.glEnable(2977);
      GL11.glEnable(3042);
      GL11.glPushMatrix();
      GL11.glEnable(32826);
      GL11.glBlendFunc(770, 771);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      this.translateFromOrientation((float)x, (float)y, (float)z, bellows.orientation);
      GL11.glTranslatef(0.0F, 1.0F, 0.0F);
      GL11.glPushMatrix();
      GL11.glScalef(0.5F, (bellows.scale + 0.1F) / 2.0F, 0.5F);
      this.model.Bag.setRotationPoint(0.0F, 0.5F, 0.0F);
      this.model.Bag.render(0.0625F);
      GL11.glScalef(1.0F, 1.0F, 1.0F);
      GL11.glPopMatrix();
      GL11.glTranslatef(0.0F, -1.0F, 0.0F);
      GL11.glPushMatrix();
      GL11.glTranslatef(0.0F, -tscale / 2.0F + 0.5F, 0.0F);
      this.model.TopPlank.render(0.0625F);
      GL11.glTranslatef(0.0F, tscale / 2.0F - 0.5F, 0.0F);
      GL11.glPopMatrix();
      GL11.glPushMatrix();
      GL11.glTranslatef(0.0F, tscale / 2.0F - 0.5F, 0.0F);
      this.model.BottomPlank.render(0.0625F);
      GL11.glTranslatef(0.0F, -tscale / 2.0F + 0.5F, 0.0F);
      GL11.glPopMatrix();
      this.model.render();
      GL11.glDisable(32826);
      GL11.glPopMatrix();
      GL11.glDisable(3042);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
   }

   @Override
   public void renderTileEntityAt(TileEntity tileentity, double d, double d1, double d2, float f) {
      this.renderEntityAt((TileBellows)tileentity, d, d1, d2, f);
   }
}
