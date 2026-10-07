// Original ThaumCraft 2.1.6d drawing code by Azanor, adapted for the modern renderer.
package dev.thaumcraft.client.legacy;

import static dev.thaumcraft.client.legacy.LegacyCompat.*;


public class TileEnchanterRenderer extends TileEntitySpecialRenderer {
   private ModelBook field_40450_a = new ModelBook();

   private void drawDisk(double x, double y, double z) {
      float angle = ModLoader.getMinecraftInstance().thePlayer.ticksExisted % 360.0F;
      Tessellator tessellator = Tessellator.instance;
      GL11.glPushMatrix();
      GL11.glTranslatef((float)x + 0.5F, (float)y, (float)z + 0.5F);
      GL11.glPushMatrix();
      GL11.glRotatef(angle, 0.0F, 1.0F, 0.0F);
      GL11.glTranslatef(-0.5F, 0.0F, -0.5F);
      GL11.glDepthMask(false);
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 1);
      MinecraftForgeClient.bindTexture("/thaumcraft/resources/seal5.png");
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      float f10 = 0.25F;
      tessellator.startDrawingQuads();
      tessellator.setBrightness(200);
      tessellator.setColorRGBA_F(1.0F, 0.5F, 1.0F, 1.0F);
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

   public void func_40449_a(TileEnchanter te, double d, double d1, double d2, float f) {
      GL11.glPushMatrix();
      GL11.glTranslatef((float)d + 0.5F, (float)d1 + 0.75F, (float)d2 + 0.5F);
      float f1 = te.field_40068_a + f;
      GL11.glTranslatef(0.0F, 0.1F + MathHelper.sin(f1 * 0.1F) * 0.01F, 0.0F);
      float f2 = te.field_40069_h - te.field_40067_p;

      while (f2 >= 3.141593F) {
         f2 -= 6.283185F;
      }

      while (f2 < -3.141593F) {
         f2 += 6.283185F;
      }

      float f3 = te.field_40067_p + f2 * f;
      GL11.glRotatef(-f3 * 180.0F / 3.141593F, 0.0F, 1.0F, 0.0F);
      GL11.glRotatef(80.0F, 0.0F, 0.0F, 1.0F);
      this.bindTextureByName("/thaumcraft/resources/book.png");
      float f4 = te.field_40065_c + (te.field_40063_b - te.field_40065_c) * f + 0.25F;
      float f5 = te.field_40065_c + (te.field_40063_b - te.field_40065_c) * f + 0.75F;
      f4 = (f4 - MathHelper.func_40346_b(f4)) * 1.6F - 0.3F;
      f5 = (f5 - MathHelper.func_40346_b(f5)) * 1.6F - 0.3F;
      if (f4 < 0.0F) {
         f4 = 0.0F;
      }

      if (f5 < 0.0F) {
         f5 = 0.0F;
      }

      if (f4 > 1.0F) {
         f4 = 1.0F;
      }

      if (f5 > 1.0F) {
         f5 = 1.0F;
      }

      float f6 = te.field_40060_g + (te.field_40059_f - te.field_40060_g) * f;
      this.field_40450_a.render(null, f1, f4, f5, f6, 0.0F, 0.0625F);
      GL11.glPopMatrix();
      if (te.enchantmentChoice != -1 && te.enchantmentCost > 0) {
         this.drawDisk(d, d1 + 0.7599999904632568, d2);
      }
   }

   @Override
   public void renderTileEntityAt(TileEntity tileentity, double d, double d1, double d2, float f) {
      this.func_40449_a((TileEnchanter)tileentity, d, d1, d2, f);
   }
}
