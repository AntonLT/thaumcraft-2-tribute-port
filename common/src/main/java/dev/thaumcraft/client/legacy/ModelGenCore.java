// Original ThaumCraft 2.1.6d drawing code by Azanor, adapted for the modern renderer.
package dev.thaumcraft.client.legacy;

import static dev.thaumcraft.client.legacy.LegacyCompat.*;


public class ModelGenCore extends ModelBase {
   private ModelRenderer cube;
   private ModelRenderer outer = new ModelRenderer(this, "glass");

   public ModelGenCore(float f) {
      this.outer.setTextureOffset(0, 0).addBox(-4.0F, -4.0F, -4.0F, 8, 8, 8);
      this.cube = new ModelRenderer(this, "cube");
      this.cube.setTextureOffset(32, 0).addBox(-4.0F, -4.0F, -4.0F, 8, 8, 8);
   }

   public void render(float f, float f1, float f2, float f3, float f4, float f5) {
      GL11.glPushMatrix();
      GL11.glScalef(0.8F, 0.8F, 0.8F);
      GL11.glTranslatef(0.0F, -1.0F, 0.0F);
      GL11.glRotatef(f1, 0.0F, 1.0F, 0.0F);
      GL11.glRotatef(60.0F, 0.7071F, 0.0F, 0.7071F);
      this.outer.render(f5);
      float f6 = 0.75F;
      GL11.glScalef(f6, f6, f6);
      GL11.glRotatef(60.0F, 0.7071F, 0.0F, 0.7071F);
      GL11.glRotatef(f1, 0.0F, 1.0F, 0.0F);
      this.outer.render(f5);
      GL11.glScalef(f6, f6, f6);
      GL11.glRotatef(60.0F, 0.7071F, 0.0F, 0.7071F);
      GL11.glRotatef(f1, 0.0F, 1.0F, 0.0F);
      this.cube.render(f5);
      GL11.glPopMatrix();
   }

   @Override
   public void setRotationAngles(float f, float f1, float f2, float f3, float f4, float f5) {
      super.setRotationAngles(f, f1, f2, f3, f4, f5);
   }
}
