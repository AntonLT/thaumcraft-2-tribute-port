// Original ThaumCraft 2.1.6d drawing code by Azanor, adapted for the modern renderer.
package dev.thaumcraft.client.legacy;

import static dev.thaumcraft.client.legacy.LegacyCompat.*;


public class ModelCarpet extends ModelBase {
   public ModelRenderer[] carpetSections = new ModelRenderer[7];
   public ModelRenderer[] carpetTassle = new ModelRenderer[4];

   public ModelCarpet() {
      this.textureWidth = 256;
      this.textureHeight = 128;
      this.carpetSections[0] = new ModelRenderer(this, 0, 0);
      this.carpetSections[0].addBox(-20.0F, 0.0F, 0.0F, 40, 2, 8);
      this.carpetSections[0].setRotationPoint(0.0F, 0.0F, 20.0F);
      this.carpetSections[0].setTextureSize(256, 128);
      this.carpetSections[0].mirror = true;
      this.carpetSections[1] = new ModelRenderer(this, 0, 10);
      this.carpetSections[1].addBox(-20.0F, 0.0F, 0.0F, 40, 2, 8);
      this.carpetSections[1].setRotationPoint(0.0F, 0.0F, 12.0F);
      this.carpetSections[1].setTextureSize(256, 128);
      this.carpetSections[1].mirror = true;
      this.carpetSections[2] = new ModelRenderer(this, 0, 20);
      this.carpetSections[2].addBox(-20.0F, 0.0F, 0.0F, 40, 2, 8);
      this.carpetSections[2].setRotationPoint(0.0F, 0.0F, 4.0F);
      this.carpetSections[2].setTextureSize(256, 128);
      this.carpetSections[2].mirror = true;
      this.carpetSections[3] = new ModelRenderer(this, 0, 30);
      this.carpetSections[3].addBox(-20.0F, 0.0F, 0.0F, 40, 2, 8);
      this.carpetSections[3].setRotationPoint(0.0F, 0.0F, -4.0F);
      this.carpetSections[3].setTextureSize(256, 128);
      this.carpetSections[3].mirror = true;
      this.carpetSections[4] = new ModelRenderer(this, 0, 40);
      this.carpetSections[4].addBox(-20.0F, 0.0F, 0.0F, 40, 2, 8);
      this.carpetSections[4].setRotationPoint(0.0F, 0.0F, -12.0F);
      this.carpetSections[4].setTextureSize(256, 128);
      this.carpetSections[4].mirror = true;
      this.carpetSections[5] = new ModelRenderer(this, 0, 50);
      this.carpetSections[5].addBox(-20.0F, 0.0F, 0.0F, 40, 2, 8);
      this.carpetSections[5].setRotationPoint(0.0F, 0.0F, -20.0F);
      this.carpetSections[5].setTextureSize(256, 128);
      this.carpetSections[5].mirror = true;
      this.carpetSections[6] = new ModelRenderer(this, 0, 60);
      this.carpetSections[6].addBox(-20.0F, 0.0F, 0.0F, 40, 2, 8);
      this.carpetSections[6].setRotationPoint(0.0F, 0.0F, -28.0F);
      this.carpetSections[6].setTextureSize(256, 128);
      this.carpetSections[6].mirror = true;
      this.carpetTassle[0] = new ModelRenderer(this, 0, 70);
      this.carpetTassle[0].addBox(0.0F, 0.0F, 0.0F, 2, 2, 2);
      this.carpetTassle[0].setRotationPoint(20.0F, 1.0F, -28.0F);
      this.carpetTassle[0].setTextureSize(256, 128);
      this.carpetTassle[0].mirror = true;
      this.setRotation(this.carpetTassle[0], -0.7071F, 0.0F, 0.7071F);
      this.carpetTassle[1] = new ModelRenderer(this, 0, 70);
      this.carpetTassle[1].addBox(0.0F, 0.0F, 0.0F, 2, 2, 2);
      this.carpetTassle[1].setRotationPoint(-20.0F, 1.0F, -28.0F);
      this.carpetTassle[1].setTextureSize(256, 128);
      this.carpetTassle[1].mirror = true;
      this.setRotation(this.carpetTassle[1], -0.7071F, 0.0F, 0.7071F);
      this.carpetTassle[2] = new ModelRenderer(this, 0, 70);
      this.carpetTassle[2].addBox(0.0F, 0.0F, 0.0F, 2, 2, 2);
      this.carpetTassle[2].setRotationPoint(20.0F, 1.0F, 28.0F);
      this.carpetTassle[2].setTextureSize(256, 128);
      this.carpetTassle[2].mirror = true;
      this.setRotation(this.carpetTassle[2], -0.7071F, 0.0F, 0.7071F);
      this.carpetTassle[3] = new ModelRenderer(this, 0, 70);
      this.carpetTassle[3].addBox(0.0F, 0.0F, 0.0F, 2, 2, 2);
      this.carpetTassle[3].setRotationPoint(-20.0F, 1.0F, 28.0F);
      this.carpetTassle[3].setTextureSize(256, 128);
      this.carpetTassle[3].mirror = true;
      this.setRotation(this.carpetTassle[3], -0.7071F, 0.0F, 0.7071F);
   }

   private void setRotation(ModelRenderer model, float x, float y, float z) {
      model.rotateAngleX = x;
      model.rotateAngleY = y;
      model.rotateAngleZ = z;
   }

   public void render() {
      int count = ModLoader.getMinecraftInstance().thePlayer.ticksExisted;

      for (int a = 0; a < 7; a++) {
         float bob = MathHelper.sin(count / 4.0F) * 0.25F + 0.25F;
         GL11.glPushMatrix();
         GL11.glTranslatef(0.0F, bob, 0.0F);
         this.carpetSections[a].render(0.0625F);
         GL11.glTranslatef(0.0F, -0.25F, 0.0F);
         GL11.glScalef(1.0F, 5.0F, 1.0F);
         if (a == 6) {
            this.carpetTassle[0].render(0.0625F);
            this.carpetTassle[1].render(0.0625F);
         } else if (a == 0) {
            this.carpetTassle[2].render(0.0625F);
            this.carpetTassle[3].render(0.0625F);
         }

         GL11.glPopMatrix();
         count++;
      }
   }

   @Override
   public void setRotationAngles(float par1, float par2, float par3, float par4, float par5, float par6) {
   }
}
