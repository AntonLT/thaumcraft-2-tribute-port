// Original ThaumCraft 2.1.6d drawing code by Azanor, adapted for the modern renderer.
package dev.thaumcraft.client.legacy;

import static dev.thaumcraft.client.legacy.LegacyCompat.*;


public class ModelPump extends ModelBase {
   ModelRenderer Front;
   ModelRenderer MoveBase;
   ModelRenderer MoveFrill;
   ModelRenderer Center;
   ModelRenderer Back;

   public ModelPump() {
      this.textureWidth = 128;
      this.textureHeight = 64;
      this.Front = new ModelRenderer(this, 0, 0);
      this.Front.addBox(0.0F, 0.0F, 0.0F, 12, 6, 12);
      this.Front.setRotationPoint(-6.0F, 18.0F, -6.0F);
      this.Front.setTextureSize(128, 64);
      this.Front.mirror = true;
      this.setRotation(this.Front, 0.0F, 0.0F, 0.0F);
      this.MoveBase = new ModelRenderer(this, 0, 18);
      this.MoveBase.addBox(0.0F, 0.0F, 0.0F, 12, 2, 12);
      this.MoveBase.setRotationPoint(-6.0F, 8.0F, -6.0F);
      this.MoveBase.setTextureSize(128, 64);
      this.MoveBase.mirror = true;
      this.setRotation(this.MoveBase, 0.0F, 0.0F, 0.0F);
      this.MoveFrill = new ModelRenderer(this, 0, 32);
      this.MoveFrill.addBox(0.0F, 0.0F, 0.0F, 10, 4, 10);
      this.MoveFrill.setRotationPoint(-5.0F, 10.0F, -5.0F);
      this.MoveFrill.setTextureSize(128, 64);
      this.MoveFrill.mirror = true;
      this.setRotation(this.MoveFrill, 0.0F, 0.0F, 0.0F);
      this.Center = new ModelRenderer(this, 48, 0);
      this.Center.addBox(0.0F, 0.0F, 0.0F, 16, 4, 16);
      this.Center.setRotationPoint(-8.0F, 14.0F, -8.0F);
      this.Center.setTextureSize(128, 64);
      this.Center.mirror = true;
      this.setRotation(this.Center, 0.0F, 0.0F, 0.0F);
      this.Back = new ModelRenderer(this, 0, 46);
      this.Back.addBox(0.0F, 0.0F, 0.0F, 6, 6, 6);
      this.Back.setRotationPoint(-3.0F, 8.0F, -3.0F);
      this.Back.setTextureSize(128, 64);
      this.Back.mirror = true;
      this.setRotation(this.Back, 0.0F, 0.0F, 0.0F);
   }

   @Override
   public void render(Entity entity, float f, float f1, float f2, float f3, float f4, float f5) {
      super.render(entity, f, f1, f2, f3, f4, f5);
      this.setRotationAngles(f, f1, f2, f3, f4, f5);
      this.Front.render(f5);
      this.MoveBase.render(f5);
      this.MoveFrill.render(f5);
      this.Center.render(f5);
      this.Back.render(f5);
   }

   public void render() {
      this.Front.render(0.0625F);
      this.Center.render(0.0625F);
      this.Back.render(0.0625F);
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
