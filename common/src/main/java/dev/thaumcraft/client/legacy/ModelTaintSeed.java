// Original ThaumCraft 2.1.6d drawing code by Azanor, adapted for the modern renderer.
package dev.thaumcraft.client.legacy;

import static dev.thaumcraft.client.legacy.LegacyCompat.*;


public class ModelTaintSeed extends ModelBase {
   ModelRenderer Root;
   ModelRenderer Stem;
   ModelRenderer Body;

   public ModelTaintSeed() {
      this.textureWidth = 64;
      this.textureHeight = 32;
      this.Root = new ModelRenderer(this, 0, 0);
      this.Root.addBox(0.0F, 0.0F, 0.0F, 6, 2, 6);
      this.Root.setRotationPoint(-3.0F, 0.0F, -3.0F);
      this.Root.setTextureSize(64, 32);
      this.Root.mirror = true;
      this.setRotation(this.Root, 0.0F, 0.0F, 0.0F);
      this.Stem = new ModelRenderer(this, 24, 0);
      this.Stem.addBox(0.0F, 0.0F, 0.0F, 2, 2, 2);
      this.Stem.setRotationPoint(-1.0F, 20.0F, -1.0F);
      this.Stem.setTextureSize(64, 32);
      this.Stem.mirror = true;
      this.setRotation(this.Stem, 0.0F, 0.0F, 0.0F);
      this.Body = new ModelRenderer(this, 0, 8);
      this.Body.addBox(-6.0F, -12.0F, -6.0F, 12, 12, 12);
      this.Body.setRotationPoint(0.0F, 0.0F, 0.0F);
      this.Body.setTextureSize(64, 32);
      this.Body.mirror = true;
      this.setRotation(this.Body, 0.0F, 0.0F, 0.0F);
   }

   @Override
   public void render(Entity entity, float f, float f1, float f2, float f3, float f4, float f5) {
      super.render(entity, f, f1, f2, f3, f4, f5);
      this.setRotationAngles(f, f1, f2, f3, f4, f5);
      this.Root.render(f5);
      this.Stem.render(f5);
      this.Body.render(f5);
   }

   public void render() {
      this.Root.render(0.0625F);
      this.Stem.render(0.0625F);
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
