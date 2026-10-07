// Original ThaumCraft 2.1.6d drawing code by Azanor, adapted for the modern renderer.
package dev.thaumcraft.client.legacy;

import static dev.thaumcraft.client.legacy.LegacyCompat.*;


public class ModelCrystal extends ModelBase {
   ModelRenderer Crystal;

   public ModelCrystal() {
      this.textureWidth = 64;
      this.textureHeight = 32;
      this.Crystal = new ModelRenderer(this, 0, 0);
      this.Crystal.addBox(-16.0F, -16.0F, 0.0F, 16, 16, 16);
      this.Crystal.setRotationPoint(0.0F, 32.0F, 0.0F);
      this.Crystal.setTextureSize(64, 32);
      this.Crystal.mirror = true;
      this.setRotation(this.Crystal, 0.7071F, 0.0F, 0.7071F);
   }

   @Override
   public void render(Entity entity, float f, float f1, float f2, float f3, float f4, float f5) {
      super.render(entity, f, f1, f2, f3, f4, f5);
      this.setRotationAngles(f, f1, f2, f3, f4, f5);
      this.Crystal.render(f5);
   }

   public void render() {
      this.Crystal.render(0.0625F);
   }

   private void setRotation(ModelRenderer model, float x, float y, float z) {
      model.rotateAngleX = x;
      model.rotateAngleY = y;
      model.rotateAngleZ = z;
   }

   @Override
   public void setRotationAngles(float f, float f1, float f2, float f3, float f4, float f5) {
      super.setRotationAngles(f, f1, f2, f3, f4, f5);
   }
}
