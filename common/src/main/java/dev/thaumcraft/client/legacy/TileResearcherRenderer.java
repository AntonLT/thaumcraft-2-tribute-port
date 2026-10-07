// Original ThaumCraft 2.1.6d drawing code by Azanor, adapted for the modern renderer.
package dev.thaumcraft.client.legacy;

import static dev.thaumcraft.client.legacy.LegacyCompat.*;


public class TileResearcherRenderer extends TileEntitySpecialRenderer {
   private void drawPaper(double x, double y, double z, float angle) {
      Tessellator tessellator = Tessellator.instance;
      GL11.glPushMatrix();
      GL11.glTranslatef((float)x + 0.5F, (float)y, (float)z + 0.5F);
      GL11.glPushMatrix();
      GL11.glRotatef(angle, 0.0F, 1.0F, 0.0F);
      GL11.glTranslatef(-0.35F, 0.0F, -0.35F);
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 771);
      MinecraftForgeClient.bindTexture("/thaumcraft/resources/researchertop.png");
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      tessellator.startDrawingQuads();
      tessellator.setBrightness(220);
      tessellator.setColorRGBA_F(1.0F, 1.0F, 1.0F, 1.0F);
      tessellator.addVertexWithUV(0.0, 0.0, 0.699999988079071, 0.0, 1.0);
      tessellator.addVertexWithUV(0.699999988079071, 0.0, 0.699999988079071, 1.0, 1.0);
      tessellator.addVertexWithUV(0.699999988079071, 0.0, 0.0, 1.0, 0.0);
      tessellator.addVertexWithUV(0.0, 0.0, 0.0, 0.0, 0.0);
      tessellator.draw();
      GL11.glDisable(3042);
      GL11.glPopMatrix();
      GL11.glPopMatrix();
   }

   public void renderEntityAt(TileResearcher researcher, double x, double y, double z, float fq) {
      Minecraft mc = ModLoader.getMinecraftInstance();
      if (researcher.worked) {
         int count = ModLoader.getMinecraftInstance().thePlayer.ticksExisted;
         float bob = MathHelper.sin(count / 10.0F) * 0.025F + 0.025F;
         float height = 0.65F + bob;
         this.drawPaper(x, y + height, z, researcher.orientation * 90.0F);
         if (researcher.worldObj.rand.nextInt(20) == 0) {
            float xx = researcher.xCoord + 0.5F - (researcher.worldObj.rand.nextFloat() - researcher.worldObj.rand.nextFloat()) * 0.25F;
            float yy = researcher.yCoord + height + 0.02F;
            float zz = researcher.zCoord + 0.5F - (researcher.worldObj.rand.nextFloat() - researcher.worldObj.rand.nextFloat()) * 0.25F;
            FXSparkle ef2 = new FXSparkle(researcher.worldObj, xx, yy, zz, 0.8F, researcher.worldObj.rand.nextInt(5), 4);
            ef2.setGravity(-0.04F);
            ModLoader.getMinecraftInstance().effectRenderer.addEffect(ef2);
         }
      }

      GL11.glPushMatrix();
      GL11.glTranslatef((float)x, (float)y + 0.65F, (float)z);
      GL11.glTranslatef(0.5F, 0.0F, 0.5F);
      GL11.glRotatef(-90.0F, 0.0F, 1.0F, 0.0F);
      GL11.glPushMatrix();
      GL11.glRotatef(researcher.orientation * -90.0F, 0.0F, 1.0F, 0.0F);
      GL11.glTranslatef(-0.11F, 0.1F, -0.23F);
      GL11.glRotatef(15.0F, 0.0F, 1.0F, 0.0F);
      GL11.glPushMatrix();
      ThaumCraftRenderer.renderItemFromTexture(mc, "/thaumcraft/resources/blocks.png", 16, 94, 0.5F, 0.025F, true, 1.0F, 1.0F, 1.0F, 220, 771);
      GL11.glPopMatrix();
   }

   @Override
   public void renderTileEntityAt(TileEntity te, double d, double d1, double d2, float f) {
      this.renderEntityAt((TileResearcher)te, d, d1, d2, f);
   }
}
