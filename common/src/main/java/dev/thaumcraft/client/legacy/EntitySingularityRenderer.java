// Original ThaumCraft 2.1.6d drawing code by Azanor, adapted for the modern renderer.
package dev.thaumcraft.client.legacy;

import static dev.thaumcraft.client.legacy.LegacyCompat.*;

import java.util.Random;

public class EntitySingularityRenderer extends Render {
   public EntitySingularityRenderer() {
      this.shadowSize = 0.1F;
   }

   public void renderEntityAt(EntitySingularity tg, double x, double y, double z, float fq) {
      if (tg.fuse > 0) {
         GL11.glPushMatrix();
         GL11.glDepthMask(false);
         GL11.glEnable(3042);
         GL11.glBlendFunc(770, 771);
         GL11.glBindTexture(3553, ModLoader.getMinecraftInstance().renderEngine.getTexture("/thaumcraft/resources/items.png"));
         int i = 99;
         int size = 16;
         float size16 = 256.0F;
         float float_sizeMinus0_01 = 15.99F;
         float float_texNudge = 0.001953125F;
         float float_reciprocal = 0.0625F;

         try {
            Class sizeClass = Class.forName("com.pclewis.mcpatcher.mod.TileSize");
            size = sizeClass.getDeclaredField("int_size").getInt(sizeClass);
            size16 = sizeClass.getDeclaredField("float_size16").getFloat(sizeClass);
            float_sizeMinus0_01 = sizeClass.getDeclaredField("float_sizeMinus0_01").getFloat(sizeClass);
            float_texNudge = sizeClass.getDeclaredField("float_texNudge").getFloat(sizeClass);
            float_reciprocal = sizeClass.getDeclaredField("float_reciprocal").getFloat(sizeClass);
         } catch (Throwable t) {
         }

         float f1 = ActiveRenderInfo.rotationX;
         float f2 = ActiveRenderInfo.rotationXZ;
         float f3 = ActiveRenderInfo.rotationZ;
         float f4 = ActiveRenderInfo.rotationYZ;
         float f5 = ActiveRenderInfo.rotationXY;
         float x0 = (i % 16 * size + 0.0F) / size16;
         float x1 = (i % 16 * size + float_sizeMinus0_01) / size16;
         float x2 = (i / 16 * size + 0.0F) / size16;
         float x3 = (i / 16 * size + float_sizeMinus0_01) / size16;
         float f10 = 0.2F;
         float f11 = (float)x;
         float f12 = (float)y;
         float f13 = (float)z;
         Tessellator tessellator = Tessellator.instance;
         tessellator.startDrawingQuads();
         tessellator.setBrightness(240);
         tessellator.setColorRGBA_F(1.0F, 1.0F, 1.0F, 1.0F);
         tessellator.addVertexWithUV(f11 - f1 * f10 - f4 * f10, f12 - f2 * f10, f13 - f3 * f10 - f5 * f10, x1, x3);
         tessellator.addVertexWithUV(f11 - f1 * f10 + f4 * f10, f12 + f2 * f10, f13 - f3 * f10 + f5 * f10, x1, x2);
         tessellator.addVertexWithUV(f11 + f1 * f10 + f4 * f10, f12 + f2 * f10, f13 + f3 * f10 + f5 * f10, x0, x2);
         tessellator.addVertexWithUV(f11 + f1 * f10 - f4 * f10, f12 - f2 * f10, f13 + f3 * f10 - f5 * f10, x0, x3);
         tessellator.draw();
         GL11.glDisable(3042);
         GL11.glDepthMask(true);
         GL11.glPopMatrix();
      } else {
         GL11.glPushMatrix();
         GL11.glTranslatef((float)x, (float)y, (float)z);
         int q = ModLoader.getMinecraftInstance().gameSettings.fancyGraphics && !Config.lowGfx ? 60 : 30;
         Tessellator tessellator = Tessellator.instance;
         float f1 = Math.abs(tg.fuse) / 170.0F;
         float f3 = 0.9F;
         float f2 = 0.0F;
         if (f1 > 0.8F) {
            f2 = (f1 - 0.8F) / 0.2F;
         }

         Random random = new Random(245L);
         GL11.glDisable(3553);
         GL11.glEnable(3042);
         GL11.glBlendFunc(770, 1);
         GL11.glDisable(3008);
         GL11.glEnable(2884);
         GL11.glDepthMask(false);
         GL11.glPushMatrix();

         for (int i = 0; i < (f1 + f1 * f1) / 2.0F * q; i++) {
            GL11.glRotatef(random.nextFloat() * 360.0F, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(random.nextFloat() * 360.0F, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(random.nextFloat() * 360.0F, 0.0F, 0.0F, 1.0F);
            GL11.glRotatef(random.nextFloat() * 360.0F, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(random.nextFloat() * 360.0F, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(random.nextFloat() * 360.0F + f1 * 90.0F, 0.0F, 0.0F, 1.0F);
            tessellator.startDrawing(6);
            float fa = random.nextFloat() * 20.0F + 5.0F + f2 * 10.0F;
            float f4 = random.nextFloat() * 2.0F + 1.0F + f2 * 2.0F;
            fa /= 3.0F;
            f4 /= 3.0F;
            tessellator.setColorRGBA_I(16777215, (int)(255.0F * (1.0F - f2)));
            tessellator.addVertex(0.0, 0.0, 0.0);
            tessellator.setColorRGBA_I(16711935, 0);
            tessellator.addVertex(-0.866 * f4, fa, -0.5F * f4);
            tessellator.addVertex(0.866 * f4, fa, -0.5F * f4);
            tessellator.addVertex(0.0, fa, 1.0F * f4);
            tessellator.addVertex(-0.866 * f4, fa, -0.5F * f4);
            tessellator.draw();
         }

         for (int i = 0; i < (f3 + f3 * f3) / 2.0F * q; i++) {
            GL11.glRotatef(random.nextFloat() * 360.0F, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(random.nextFloat() * 360.0F, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(random.nextFloat() * 360.0F, 0.0F, 0.0F, 1.0F);
            GL11.glRotatef(random.nextFloat() * 360.0F, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(random.nextFloat() * 360.0F, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(random.nextFloat() * 360.0F + f1 * 90.0F, 0.0F, 0.0F, 1.0F);
            tessellator.startDrawing(6);
            float fa = random.nextFloat() * 20.0F + 5.0F + f2 * 10.0F;
            float f4 = random.nextFloat() * 2.0F + 1.0F + f2 * 2.0F;
            fa /= 7.0F;
            f4 /= 7.0F;
            tessellator.setColorRGBA_I(16777215, (int)(255.0F * (1.0F - f2)));
            tessellator.addVertex(0.0, 0.0, 0.0);
            tessellator.setColorRGBA_I(255, 0);
            tessellator.addVertex(-0.866 * f4, fa, -0.5F * f4);
            tessellator.addVertex(0.866 * f4, fa, -0.5F * f4);
            tessellator.addVertex(0.0, fa, 1.0F * f4);
            tessellator.addVertex(-0.866 * f4, fa, -0.5F * f4);
            tessellator.draw();
         }

         GL11.glPopMatrix();
         GL11.glDepthMask(true);
         GL11.glDisable(2884);
         GL11.glDisable(3042);
         GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
         GL11.glEnable(3553);
         GL11.glEnable(3008);
         GL11.glPopMatrix();
      }
   }

   @Override
   public void doRender(Entity entity, double d, double d1, double d2, float f, float f1) {
      this.renderEntityAt((EntitySingularity)entity, d, d1, d2, f);
   }
}
