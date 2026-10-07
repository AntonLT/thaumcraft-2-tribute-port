// Original ThaumCraft 2.1.6d drawing code by Azanor, adapted for the modern renderer.
package dev.thaumcraft.client.legacy;

import static dev.thaumcraft.client.legacy.LegacyCompat.*;


public class TileTaintSeedRenderer extends TileEntitySpecialRenderer {
   private ModelTaintSeed model = new ModelTaintSeed();

   public void renderEntityAt(TileTaintSeed seed, double x, double y, double z, float fq) {
      float bob = MathHelper.sin(seed.growth / 5.0F) * 1.0F + 1.0F;
      float bob2 = MathHelper.sin(seed.growth / 10.0F) * 1.0F + 1.0F;
      Minecraft mc = ModLoader.getMinecraftInstance();
      this.bindTextureByName("/thaumcraft/resources/taintseed.png");
      GL11.glEnable(2977);
      GL11.glEnable(3042);
      GL11.glPushMatrix();
      GL11.glEnable(32826);
      GL11.glBlendFunc(770, 771);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      GL11.glTranslatef((float)x + 0.5F, (float)y + 0.1F, (float)z + 0.5F);
      GL11.glRotatef(180.0F, 0.0F, 0.0F, 1.0F);
      GL11.glPushMatrix();
      GL11.glRotatef(bob, 1.0F, 0.0F, 0.0F);
      GL11.glRotatef(bob2, 0.0F, 0.0F, 1.0F);
      float growthscale = 0.5F + seed.growth / 1500.0F;
      GL11.glScalef((1.0F + bob2 / 10.0F) * growthscale, (1.4F - bob2 / 10.0F) * growthscale, (1.0F + bob2 / 10.0F) * growthscale);
      this.model.Body.render(0.0625F);
      GL11.glScalef(1.0F, 1.0F, 1.0F);
      GL11.glPopMatrix();
      this.model.Root.render(0.0625F);
      GL11.glDisable(32826);
      GL11.glPopMatrix();
      GL11.glDisable(3042);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      this.drawDisk(x, y + 0.009999999776482582, z);
   }

   private void drawDisk(double x, double y, double z) {
      Tessellator tessellator = Tessellator.instance;
      GL11.glPushMatrix();
      GL11.glTranslatef((float)x + 0.5F, (float)y, (float)z + 0.5F);
      GL11.glPushMatrix();
      GL11.glTranslatef(-0.75F, 0.0F, -0.75F);
      MinecraftForgeClient.bindTexture("/thaumcraft/resources/taintseedroots.png");
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      float f10 = 0.25F;
      tessellator.startDrawingQuads();
      tessellator.setBrightness(200);
      tessellator.setColorRGBA_F(1.0F, 1.0F, 1.0F, 1.0F);
      tessellator.addVertexWithUV(0.0, 0.0, 1.5, 0.0, 1.0);
      tessellator.addVertexWithUV(1.5, 0.0, 1.5, 1.0, 1.0);
      tessellator.addVertexWithUV(1.5, 0.0, 0.0, 1.0, 0.0);
      tessellator.addVertexWithUV(0.0, 0.0, 0.0, 0.0, 0.0);
      tessellator.draw();
      GL11.glPopMatrix();
      GL11.glPopMatrix();
   }

   @Override
   public void renderTileEntityAt(TileEntity tileentity, double d, double d1, double d2, float f) {
      this.renderEntityAt((TileTaintSeed)tileentity, d, d1, d2, f);
   }
}
