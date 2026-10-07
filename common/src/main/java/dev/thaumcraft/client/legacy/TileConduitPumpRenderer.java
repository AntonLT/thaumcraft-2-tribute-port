// Original ThaumCraft 2.1.6d drawing code by Azanor, adapted for the modern renderer.
package dev.thaumcraft.client.legacy;

import static dev.thaumcraft.client.legacy.LegacyCompat.*;


public class TileConduitPumpRenderer extends TileEntitySpecialRenderer {
   private ModelPump model = new ModelPump();

   private void translateFromOrientation(double x, double y, double z, int orientation) {
      GL11.glTranslatef((float)x + 0.5F, (float)y + 0.5F, (float)z + 0.5F);
      if (orientation != 0) {
         if (orientation == 1) {
            GL11.glRotatef(180.0F, 0.0F, 0.0F, 1.0F);
         } else if (orientation == 2) {
            GL11.glRotatef(90.0F, 1.0F, 0.0F, 0.0F);
         } else if (orientation == 3) {
            GL11.glRotatef(-90.0F, 1.0F, 0.0F, 0.0F);
         } else if (orientation == 4) {
            GL11.glRotatef(-90.0F, 0.0F, 0.0F, 1.0F);
         } else if (orientation == 5) {
            GL11.glRotatef(90.0F, 0.0F, 0.0F, 1.0F);
         }
      }

      GL11.glTranslatef(0.0F, -1.0F, 0.0F);
   }

   public void renderEntityAt(TileConduitPump pump, double x, double y, double z, float fq) {
      int count = ModLoader.getMinecraftInstance().thePlayer.ticksExisted;
      float bob = 1.0F;
      if (!pump.gettingPower()) {
         bob = Math.abs(MathHelper.sin(count / 10.0F) * 1.0F);
      }

      Minecraft mc = ModLoader.getMinecraftInstance();
      this.bindTextureByName("/thaumcraft/resources/pump.png");
      GL11.glEnable(2977);
      GL11.glEnable(3042);
      GL11.glPushMatrix();
      GL11.glEnable(32826);
      GL11.glBlendFunc(770, 771);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      this.translateFromOrientation((float)x, (float)y, (float)z, pump.orientation);
      GL11.glPushMatrix();
      GL11.glPushMatrix();
      GL11.glTranslatef(0.0F, 0.251F - bob / 4.0F, 0.0F);
      this.model.MoveBase.render(0.0625F);
      GL11.glPopMatrix();
      GL11.glTranslatef(0.0F, 0.875F, 0.0F);
      GL11.glPushMatrix();
      GL11.glScalef(1.0F, bob, 1.0F);
      this.model.MoveFrill.setRotationPoint(-5.0F, 0.0F, -5.0F);
      GL11.glRotatef(180.0F, 0.0F, 0.0F, 1.0F);
      this.model.MoveFrill.render(0.0625F);
      GL11.glScalef(1.0F, 1.0F, 1.0F);
      GL11.glPopMatrix();
      GL11.glPopMatrix();
      this.model.render();
      GL11.glDisable(32826);
      GL11.glPopMatrix();
      GL11.glDisable(3042);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
   }

   @Override
   public void renderTileEntityAt(TileEntity tileentity, double d, double d1, double d2, float f) {
      this.renderEntityAt((TileConduitPump)tileentity, d, d1, d2, f);
   }
}
