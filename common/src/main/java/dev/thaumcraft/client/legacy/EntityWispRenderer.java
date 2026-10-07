// Original ThaumCraft 2.1.6d drawing code by Azanor, adapted for the modern renderer.
package dev.thaumcraft.client.legacy;

import static dev.thaumcraft.client.legacy.LegacyCompat.*;


public class EntityWispRenderer extends Render {
   int particleAge = 0;
   int moteHalfLife = 10;

   public EntityWispRenderer() {
      this.shadowSize = 0.0F;
   }

   public void renderEntityAt(Entity entity, double x, double y, double z, float fq) {
      if (((EntityLiving)entity).getHealth() > 0) {
         this.particleAge = (int)(clockMillis() % 800L);
         float agescale = this.particleAge / 400.0F;
         if (agescale > 1.0F) {
            agescale = 2.0F - agescale;
         }

         GL11.glPushMatrix();
         GL11.glDepthMask(false);
         GL11.glEnable(3042);
         GL11.glBlendFunc(770, 1);
         MinecraftForgeClient.bindTexture("/thaumcraft/resources/p_large.png");
         float f1 = ActiveRenderInfo.rotationX;
         float f2 = ActiveRenderInfo.rotationXZ;
         float f3 = ActiveRenderInfo.rotationZ;
         float f4 = ActiveRenderInfo.rotationYZ;
         float f5 = ActiveRenderInfo.rotationXY;
         float f10 = agescale * 0.6F;
         float f11 = (float)x;
         float f12 = (float)y;
         float f13 = (float)z;
         Tessellator tessellator = Tessellator.instance;
         tessellator.startDrawingQuads();
         tessellator.setBrightness(240);
         if (((EntityWisp)entity).type != 5) {
            tessellator.setColorRGBA_F(0.6F, 0.0F, 0.75F, 0.5F);
         } else {
            tessellator.setColorRGBA_F(0.4F, 0.0F, 0.5F, 0.5F);
         }

         tessellator.addVertexWithUV(f11 - f1 * f10 - f4 * f10, f12 - f2 * f10, f13 - f3 * f10 - f5 * f10, 0.0, 1.0);
         tessellator.addVertexWithUV(f11 - f1 * f10 + f4 * f10, f12 + f2 * f10, f13 - f3 * f10 + f5 * f10, 1.0, 1.0);
         tessellator.addVertexWithUV(f11 + f1 * f10 + f4 * f10, f12 + f2 * f10, f13 + f3 * f10 + f5 * f10, 1.0, 0.0);
         tessellator.addVertexWithUV(f11 + f1 * f10 - f4 * f10, f12 - f2 * f10, f13 + f3 * f10 - f5 * f10, 0.0, 0.0);
         tessellator.draw();
         GL11.glDisable(3042);
         GL11.glDepthMask(true);
         GL11.glPopMatrix();
         GL11.glPushMatrix();
         GL11.glEnable(3042);
         GL11.glBlendFunc(770, 1);
         GL11.glBindTexture(3553, ModLoader.getMinecraftInstance().renderEngine.getTexture("/thaumcraft/resources/items.png"));
         int i = 234 + ((EntityWisp)entity).type;
         int size = ThaumCraftRenderer.getTexSize("/thaumcraft/resources/items.png", 16);
         float size16 = size * 16;
         float float_sizeMinus0_01 = size - 0.01F;
         float float_texNudge = 1.0F / ((float)size * size * 2.0F);
         float float_reciprocal = 1.0F / size;
         f1 = ActiveRenderInfo.rotationX;
         f2 = ActiveRenderInfo.rotationXZ;
         f3 = ActiveRenderInfo.rotationZ;
         f4 = ActiveRenderInfo.rotationYZ;
         f5 = ActiveRenderInfo.rotationXY;
         float x0 = (i % 16 * size + 0.0F) / size16;
         float x1 = (i % 16 * size + float_sizeMinus0_01) / size16;
         float x2 = (i / 16 * size + 0.0F) / size16;
         float x3 = (i / 16 * size + float_sizeMinus0_01) / size16;
         f10 = 0.15F;
         f11 = (float)x;
         f12 = (float)y;
         f13 = (float)z;
         tessellator.startDrawingQuads();
         tessellator.setBrightness(240);
         tessellator.setColorRGBA_F(1.0F, 1.0F, 1.0F, 1.0F);
         tessellator.addVertexWithUV(f11 - f1 * f10 - f4 * f10, f12 - f2 * f10, f13 - f3 * f10 - f5 * f10, x1, x3);
         tessellator.addVertexWithUV(f11 - f1 * f10 + f4 * f10, f12 + f2 * f10, f13 - f3 * f10 + f5 * f10, x1, x2);
         tessellator.addVertexWithUV(f11 + f1 * f10 + f4 * f10, f12 + f2 * f10, f13 + f3 * f10 + f5 * f10, x0, x2);
         tessellator.addVertexWithUV(f11 + f1 * f10 - f4 * f10, f12 - f2 * f10, f13 + f3 * f10 - f5 * f10, x0, x3);
         tessellator.draw();
         GL11.glDisable(3042);
         GL11.glPopMatrix();
      }
   }

   @Override
   public void doRender(Entity entity, double d, double d1, double d2, float f, float f1) {
      this.renderEntityAt(entity, d, d1, d2, f);
   }
}
