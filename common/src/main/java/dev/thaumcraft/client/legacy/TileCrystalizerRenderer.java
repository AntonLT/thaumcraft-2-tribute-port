// Original ThaumCraft 2.1.6d drawing code by Azanor, adapted for the modern renderer.
package dev.thaumcraft.client.legacy;

import static dev.thaumcraft.client.legacy.LegacyCompat.*;


public class TileCrystalizerRenderer extends TileEntitySpecialRenderer {
   private ModelCrystal model = new ModelCrystal();

   private void drawCrystal(float x, float y, float z, float a1, float a2, float b) {
      GL11.glEnable(2977);
      GL11.glEnable(3042);
      GL11.glPushMatrix();
      GL11.glEnable(32826);
      GL11.glBlendFunc(770, 771);
      // ModelRenderer in 1.2.5 starts fresh tessellator batches, clearing the prior
      // tessellator brightness override. Keep the surrounding world light here.
      GL11.glTranslatef(x, y, z);
      GL11.glRotatef(a1, 0.0F, 1.0F, 0.0F);
      GL11.glRotatef(a2, 1.0F, 0.0F, 0.0F);
      GL11.glPushMatrix();
      GL11.glColor4f(b, b, b, 1.0F);
      GL11.glScalef(0.15F, 0.45F, 0.15F);
      this.model.render();
      GL11.glScalef(1.0F, 1.0F, 1.0F);
      GL11.glPopMatrix();
      GL11.glDisable(32826);
      GL11.glPopMatrix();
      GL11.glDisable(3042);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
   }

   public void renderEntityAt(TileCrystalizer cr, double x, double y, double z, float fq) {
      Minecraft mc = ModLoader.getMinecraftInstance();
      int count = ModLoader.getMinecraftInstance().thePlayer.ticksExisted;
      float bob = 0.0F;
      float angleS = 45.0F;
      float angleI = 90.0F;
      if (cr.hasUpgrade((byte)3)) {
         angleS = 36.0F;
         angleI = 72.0F;
      }

      if (cr.isCooking()) {
         angleS += count % 360;
         bob = MathHelper.sin(count / 5.0F) * 0.15F + 0.15F;
      }

      this.bindTextureByName("/thaumcraft/resources/crystal.png");
      this.drawCrystal((float)x + 0.5F, (float)y + 0.25F, (float)z + 0.5F, angleS, 0.0F, 1.0F - bob);
      this.bindTextureByName("/thaumcraft/resources/crystaly.png");
      this.drawCrystal((float)x + 0.5F, (float)y + 0.25F, (float)z + 0.5F, angleS, 25.0F, 1.0F - bob);
      angleS += angleI;
      this.bindTextureByName("/thaumcraft/resources/crystalb.png");
      this.drawCrystal((float)x + 0.5F, (float)y + 0.25F, (float)z + 0.5F, angleS, 25.0F, 1.0F - bob);
      angleS += angleI;
      this.bindTextureByName("/thaumcraft/resources/crystalg.png");
      this.drawCrystal((float)x + 0.5F, (float)y + 0.25F, (float)z + 0.5F, angleS, 25.0F, 1.0F - bob);
      angleS += angleI;
      this.bindTextureByName("/thaumcraft/resources/crystalr.png");
      this.drawCrystal((float)x + 0.5F, (float)y + 0.25F, (float)z + 0.5F, angleS, 25.0F, 1.0F - bob);
      if (cr.hasUpgrade((byte)3)) {
         angleS += angleI;
         this.bindTextureByName("/thaumcraft/resources/crystal.png");
         this.drawCrystal((float)x + 0.5F, (float)y + 0.25F, (float)z + 0.5F, angleS, 25.0F, 0.4F - bob);
      }
   }

   @Override
   public void renderTileEntityAt(TileEntity te, double d, double d1, double d2, float f) {
      double var10002 = te.xCoord + 0.5;
      double var10003 = te.yCoord + 0.5;
      double var10004 = te.zCoord;
      if (ThaumCraftCore.isVisibleTo(1.15F, ModLoader.getMinecraftInstance().thePlayer, var10002, var10003, var10004 + 0.5)) {
         this.renderEntityAt((TileCrystalizer)te, d, d1, d2, f);
      }
   }
}
