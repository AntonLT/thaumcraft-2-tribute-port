// Original ThaumCraft 2.1.6d drawing code by Azanor, adapted for the modern renderer.
package dev.thaumcraft.client.legacy;

import static dev.thaumcraft.client.legacy.LegacyCompat.*;


public class TileInfuserRenderer extends TileEntitySpecialRenderer {
   private void drawDisk(double x, double y, double z, float angle, boolean active, boolean dark) {
      Tessellator tessellator = Tessellator.instance;
      GL11.glPushMatrix();
      GL11.glTranslatef((float)x + 0.5F, (float)y, (float)z + 0.5F);
      GL11.glPushMatrix();
      GL11.glRotatef(angle, 0.0F, 1.0F, 0.0F);
      GL11.glTranslatef(-0.45F, 0.0F, -0.45F);
      GL11.glDepthMask(false);
      GL11.glEnable(3042);
      if (active) {
         GL11.glBlendFunc(770, 1);
      } else {
         GL11.glBlendFunc(770, 771);
      }

      if (dark) {
         MinecraftForgeClient.bindTexture("/thaumcraft/resources/darkinfusersymbol.png");
      } else {
         MinecraftForgeClient.bindTexture("/thaumcraft/resources/infusersymbol.png");
      }

      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      tessellator.startDrawingQuads();
      tessellator.setBrightness(220);
      if (active) {
         tessellator.setColorRGBA_F(1.0F, 0.5F, 1.0F, 1.0F);
      } else {
         tessellator.setColorRGBA_F(0.0F, 0.0F, 0.0F, 1.0F);
      }

      tessellator.addVertexWithUV(0.0, 0.0, 0.8999999761581421, 0.0, 1.0);
      tessellator.addVertexWithUV(0.8999999761581421, 0.0, 0.8999999761581421, 1.0, 1.0);
      tessellator.addVertexWithUV(0.8999999761581421, 0.0, 0.0, 1.0, 0.0);
      tessellator.addVertexWithUV(0.0, 0.0, 0.0, 0.0, 0.0);
      tessellator.draw();
      GL11.glDisable(3042);
      GL11.glDepthMask(true);
      GL11.glPopMatrix();
      GL11.glPopMatrix();
   }

   public void renderEntityAt(TileInfuser infuser, double x, double y, double z, float fq) {
      float height = 0.9475F;
      Minecraft mc = ModLoader.getMinecraftInstance();
      this.drawDisk(x, y + height, z, infuser.angle, infuser.sucked > 0.0F, infuser.getBlockMetadata() == 2);
      if (infuser.sucked > 0.0F && infuser.worldObj.rand.nextFloat() < infuser.sucked) {
         float xx = infuser.xCoord + 0.5F - (infuser.worldObj.rand.nextFloat() - infuser.worldObj.rand.nextFloat()) * 0.35F;
         float yy = infuser.yCoord + height;
         float zz = infuser.zCoord + 0.5F - (infuser.worldObj.rand.nextFloat() - infuser.worldObj.rand.nextFloat()) * 0.35F;
         FXWisp ef = new FXWisp(
            infuser.worldObj,
            xx,
            yy,
            zz,
            xx,
            yy + infuser.worldObj.rand.nextFloat(),
            zz,
            0.1F,
            infuser.getBlockMetadata() == 2 ? 5 : infuser.worldObj.rand.nextInt(5)
         );
         ModLoader.getMinecraftInstance().effectRenderer.addEffect(ef);
      }
      if ("thaumic_infuser".equals(infuser.id) && infuser.processing && (infuser.hasUpgrade(0) || infuser.hasUpgrade(1))) {
         float fx = infuser.xCoord + 0.1F, fz = infuser.zCoord + 0.1F;
         switch (infuser.worldObj.rand.nextInt(4)) {
            case 1 -> fz = infuser.zCoord + 0.9F;
            case 2 -> fx = infuser.xCoord + 0.9F;
            case 3 -> { fx = infuser.xCoord + 0.9F; fz = infuser.zCoord + 0.9F; }
            default -> {}
         }
         LegacyVisuals.smallGreenFlame(infuser.worldObj, fx, infuser.yCoord + 1.15F, fz);
      }
   }

   @Override
   public void renderTileEntityAt(TileEntity te, double d, double d1, double d2, float f) {
      this.renderEntityAt((TileInfuser)te, d, d1, d2, f);
   }
}
