// Original ThaumCraft 2.1.6d drawing code by Azanor, adapted for the modern renderer.
package dev.thaumcraft.client.legacy;

import static dev.thaumcraft.client.legacy.LegacyCompat.*;


public class ModelBellows extends ModelBase {
   ModelRenderer BottomPlank;
   ModelRenderer MiddlePlank;
   ModelRenderer TopPlank;
   ModelRenderer Bag;
   ModelRenderer Nozzle;

   public ModelBellows() {
      this.textureWidth = 128;
      this.textureHeight = 64;
      this.BottomPlank = new ModelRenderer(this, 0, 0);
      this.BottomPlank.addBox(-6.0F, 0.0F, -6.0F, 12, 2, 12);
      this.BottomPlank.setRotationPoint(0.0F, 22.0F, 0.0F);
      this.BottomPlank.setTextureSize(128, 64);
      this.BottomPlank.mirror = true;
      this.setRotation(this.BottomPlank, 0.0F, 0.0F, 0.0F);
      this.MiddlePlank = new ModelRenderer(this, 0, 0);
      this.MiddlePlank.addBox(-6.0F, -1.0F, -6.0F, 12, 2, 12);
      this.MiddlePlank.setRotationPoint(0.0F, 16.0F, 0.0F);
      this.MiddlePlank.setTextureSize(128, 64);
      this.MiddlePlank.mirror = true;
      this.setRotation(this.MiddlePlank, 0.0F, 0.0F, 0.0F);
      this.TopPlank = new ModelRenderer(this, 0, 0);
      this.TopPlank.addBox(-6.0F, 0.0F, -6.0F, 12, 2, 12);
      this.TopPlank.setRotationPoint(0.0F, 8.0F, 0.0F);
      this.TopPlank.setTextureSize(128, 64);
      this.TopPlank.mirror = true;
      this.setRotation(this.TopPlank, 0.0F, 0.0F, 0.0F);
      this.Bag = new ModelRenderer(this, 48, 0);
      this.Bag.addBox(-10.0F, -12.03333F, -10.0F, 20, 24, 20);
      this.Bag.setRotationPoint(0.0F, 16.0F, 0.0F);
      this.Bag.setTextureSize(64, 32);
      this.Bag.mirror = true;
      this.setRotation(this.Bag, 0.0F, 0.0F, 0.0F);
      this.Nozzle = new ModelRenderer(this, 0, 36);
      this.Nozzle.addBox(-2.0F, -2.0F, 0.0F, 4, 4, 2);
      this.Nozzle.setRotationPoint(0.0F, 16.0F, 6.0F);
      this.Nozzle.setTextureSize(128, 64);
      this.Nozzle.mirror = true;
      this.setRotation(this.Nozzle, 0.0F, 0.0F, 0.0F);
   }

   @Override
   public void render(Entity entity, float f, float f1, float f2, float f3, float f4, float f5) {
      super.render(entity, f, f1, f2, f3, f4, f5);
      this.setRotationAngles(f, f1, f2, f3, f4, f5);
      this.BottomPlank.render(f5);
      this.MiddlePlank.render(f5);
      this.TopPlank.render(f5);
      this.Bag.render(f5);
      this.Nozzle.render(f5);
   }

   public void render() {
      this.MiddlePlank.render(0.0625F);
      this.Nozzle.render(0.0625F);
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
