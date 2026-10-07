// Original ThaumCraft 2.1.6d drawing code by Azanor, adapted for the modern renderer.
package dev.thaumcraft.client.legacy;

import static dev.thaumcraft.client.legacy.LegacyCompat.*;


public class TileDuplicatorRenderer extends TileEntitySpecialRenderer {
   private ModelDuplicator model = new ModelDuplicator();

   public void renderEntityAt(TileDuplicator duplicator, double x, double y, double z, float fq) {
      float dist = 0.1875F - duplicator.press;
      if (duplicator.worldObj.rand.nextFloat() < duplicator.duplicatorCopyTime / duplicator.currentItemCopyCost) {
         float xx = duplicator.xCoord + 0.5F - (duplicator.worldObj.rand.nextFloat() - duplicator.worldObj.rand.nextFloat()) * 0.7F;
         float yy = duplicator.yCoord + 0.5F - (duplicator.worldObj.rand.nextFloat() - duplicator.worldObj.rand.nextFloat()) * 0.7F;
         float zz = duplicator.zCoord + 0.5F - (duplicator.worldObj.rand.nextFloat() - duplicator.worldObj.rand.nextFloat()) * 0.7F;
         FXWisp ef = new FXWisp(
            duplicator.worldObj,
            duplicator.xCoord + 0.5F,
            duplicator.yCoord + 0.5F,
            duplicator.zCoord + 0.5F,
            xx,
            yy,
            zz,
            0.1F,
            duplicator.worldObj.rand.nextInt(5)
         );
         ModLoader.getMinecraftInstance().effectRenderer.addEffect(ef);
      }

      this.bindTextureByName("/thaumcraft/resources/duplicator.png");
      GL11.glEnable(2977);
      GL11.glEnable(3042);
      GL11.glPushMatrix();
      GL11.glEnable(32826);
      GL11.glBlendFunc(770, 771);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      GL11.glTranslatef((float)x + 0.5F, (float)y - 0.5F, (float)z + 0.5F);
      GL11.glPushMatrix();
      GL11.glTranslatef(0.0F, -dist, 0.0F);
      this.model.render();
      GL11.glPopMatrix();
      GL11.glPushMatrix();
      GL11.glRotatef(180.0F, 0.0F, 0.0F, 1.0F);
      GL11.glTranslatef(0.0F, -2.0F - dist, 0.0F);
      this.model.render();
      GL11.glPopMatrix();
      GL11.glDisable(32826);
      GL11.glPopMatrix();
      GL11.glDisable(3042);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
   }

   @Override
   public void renderTileEntityAt(TileEntity tileentity, double d, double d1, double d2, float f) {
      this.renderEntityAt((TileDuplicator)tileentity, d, d1, d2, f);
   }
}
