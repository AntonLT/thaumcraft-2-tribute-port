// Original ThaumCraft 2.1.6d drawing code by Azanor, adapted for the modern renderer.
package dev.thaumcraft.client.legacy;

import static dev.thaumcraft.client.legacy.LegacyCompat.*;

import java.util.Random;

public class TileCrystalOreRenderer extends TileEntitySpecialRenderer {
   private ModelCrystal model = new ModelCrystal();

   private void translateFromOrientation(float x, float y, float z, int orientation) {
      if (orientation == 0) {
         GL11.glTranslatef(x + 0.5F, y + 1.3F, z + 0.5F);
         GL11.glRotatef(180.0F, 1.0F, 0.0F, 0.0F);
      } else if (orientation == 1) {
         GL11.glTranslatef(x + 0.5F, y - 0.3F, z + 0.5F);
      } else if (orientation == 2) {
         GL11.glTranslatef(x + 0.5F, y + 0.5F, z + 1.3F);
         GL11.glRotatef(-90.0F, 1.0F, 0.0F, 0.0F);
      } else if (orientation == 3) {
         GL11.glTranslatef(x + 0.5F, y + 0.5F, z - 0.3F);
         GL11.glRotatef(90.0F, 1.0F, 0.0F, 0.0F);
      } else if (orientation == 4) {
         GL11.glTranslatef(x + 1.3F, y + 0.5F, z + 0.5F);
         GL11.glRotatef(90.0F, 0.0F, 0.0F, 1.0F);
      } else if (orientation == 5) {
         GL11.glTranslatef(x - 0.3F, y + 0.5F, z + 0.5F);
         GL11.glRotatef(-90.0F, 0.0F, 0.0F, 1.0F);
      }
   }

   private void drawCrystal(int ori, float x, float y, float z, float a1, float a2, float shade, Random rand) {
      GL11.glEnable(2977);
      GL11.glEnable(3042);
      GL11.glPushMatrix();
      GL11.glEnable(32826);
      GL11.glBlendFunc(770, 771);
      Tessellator tessellator = Tessellator.instance;
      tessellator.setBrightness(220);
      this.translateFromOrientation(x, y, z, ori);
      GL11.glPushMatrix();
      GL11.glRotatef(a1, 0.0F, 1.0F, 0.0F);
      GL11.glRotatef(a2, 1.0F, 0.0F, 0.0F);
      GL11.glPushMatrix();
      GL11.glColor4f(shade, shade, shade, 1.0F);
      GL11.glScalef(0.15F + rand.nextFloat() * 0.075F, 0.5F + rand.nextFloat() * 0.1F, 0.15F + rand.nextFloat() * 0.05F);
      this.model.render();
      GL11.glScalef(1.0F, 1.0F, 1.0F);
      GL11.glPopMatrix();
      GL11.glPopMatrix();
      GL11.glDisable(32826);
      GL11.glPopMatrix();
      GL11.glDisable(3042);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
   }

   @Override
   public void renderTileEntityAt(TileEntity te, double x, double y, double z, float f) {
      double var10002 = te.xCoord + 0.5;
      double var10003 = te.yCoord + 0.5;
      double var10004 = te.zCoord;
      if (ThaumCraftCore.isVisibleTo(1.4F, ModLoader.getMinecraftInstance().thePlayer, var10002, var10003, var10004 + 0.5)) {
         TileCrystalOre tco = (TileCrystalOre)te;
         float shade = 1.0F;
         switch (tco.getBlockMetadata()) {
            case 1:
               this.bindTextureByName("/thaumcraft/resources/crystaly.png");
               break;
            case 2:
               this.bindTextureByName("/thaumcraft/resources/crystalb.png");
               break;
            case 3:
               this.bindTextureByName("/thaumcraft/resources/crystalg.png");
               break;
            case 4:
               this.bindTextureByName("/thaumcraft/resources/crystalr.png");
               break;
            case 5:
               shade = 0.2F;
            default:
               this.bindTextureByName("/thaumcraft/resources/crystal.png");
         }

         Random rand = new Random(tco.getBlockMetadata() + tco.xCoord + tco.yCoord * tco.zCoord);
         this.drawCrystal(
            tco.orientation,
            (float)x,
            (float)y,
            (float)z,
            (rand.nextFloat() - rand.nextFloat()) * 5.0F,
            (rand.nextFloat() - rand.nextFloat()) * 5.0F,
            shade,
            rand
         );

         for (int a = 1; a < tco.crystals; a++) {
            int angle1 = rand.nextInt(45) + 90 * a;
            int angle2 = 15 + rand.nextInt(15);
            this.drawCrystal(tco.orientation, (float)x, (float)y, (float)z, angle1, angle2, shade, rand);
         }
      }
   }
}
