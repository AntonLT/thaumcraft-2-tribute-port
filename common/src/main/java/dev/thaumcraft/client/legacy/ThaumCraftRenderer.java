// Original ThaumCraft 2.1.6d drawing code by Azanor, adapted for the modern renderer.
package dev.thaumcraft.client.legacy;

import static dev.thaumcraft.client.legacy.LegacyCompat.*;
public final class ThaumCraftRenderer {
public static void renderWaterUrn(World w, RenderBlocks rb, int i, int j, int k, Block block, int md, boolean inv) {
      if (block.getRenderBlockPass() == 0 || inv) {
         float t4 = 0.25F;
         float t2 = 0.125F;
         float t5 = 0.3125F;
         float t1 = 0.0625F;
         float t3 = 0.1875F;
         block.setBlockBounds(t2, 0.0F, t2, 1.0F - t2, 0.5F + t1, 1.0F - t2);
         if (inv) {
            DrawFaces(rb, block, 159, 158, 157, 157, 157, 157, true);
         } else {
            rb.renderStandardBlock(block, i, j, k);
         }

         block.setBlockBounds(t5, 0.5F + t1, t5, 1.0F - t5, 1.0F - t3, 1.0F - t5);
         if (inv) {
            DrawFaces(rb, block, 159, 158, 157, 157, 157, 157, true);
         } else {
            rb.renderStandardBlock(block, i, j, k);
         }

         block.setBlockBounds(t4, 1.0F - t3, t4, 1.0F - t4, 1.0F, 1.0F - t4);
         if (inv) {
            DrawFaces(rb, block, 159, 158, 157, 157, 157, 157, true);
         } else {
            rb.renderStandardBlock(block, i, j, k);
         }
      }

      rb.overrideBlockTexture = -1;
      block.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
   }

public static boolean renderBlockSoulBrazier(World w, RenderBlocks rb, int i, int j, int k, Block block, int md, boolean inv) {
      if (block.getRenderBlockPass() == 0 || inv) {
         float t4 = 0.25F;
         float t2 = 0.125F;
         float t6 = 0.375F;
         block.setBlockBounds(t2, 0.5F, t6, t4, 1.0F, 1.0F - t6);
         if (inv) {
            DrawFaces(rb, block, 149, 149, 148, 148, 148, 148, true);
         } else {
            rb.renderStandardBlock(block, i, j, k);
         }

         block.setBlockBounds(t6, 0.5F, t2, 1.0F - t6, 1.0F, t4);
         if (inv) {
            DrawFaces(rb, block, 149, 149, 148, 148, 148, 148, true);
         } else {
            rb.renderStandardBlock(block, i, j, k);
         }

         block.setBlockBounds(1.0F - t4, 0.5F, t6, 1.0F - t2, 1.0F, 1.0F - t6);
         if (inv) {
            DrawFaces(rb, block, 149, 149, 148, 148, 148, 148, true);
         } else {
            rb.renderStandardBlock(block, i, j, k);
         }

         block.setBlockBounds(t6, 0.5F, 1.0F - t4, 1.0F - t6, 1.0F, 1.0F - t2);
         if (inv) {
            DrawFaces(rb, block, 149, 149, 148, 148, 148, 148, true);
         } else {
            rb.renderStandardBlock(block, i, j, k);
         }

         block.setBlockBounds(t4, 0.0F, t4, 1.0F - t4, 0.5F + t4, 1.0F - t4);
         if (inv) {
            DrawFaces(rb, block, 149, 149, 148, 148, 148, 148, true);
         } else {
            rb.renderStandardBlock(block, i, j, k);
         }
      }

      rb.overrideBlockTexture = -1;
      block.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
      return true;
   }

public static boolean renderBlockDarkGen(World w, RenderBlocks rb, int i, int j, int k, Block block, int md, boolean inv) {
      float t1 = 0.0625F;
      float t4 = 0.25F;
      float t2 = 0.125F;
      if (block.getRenderBlockPass() == 0 || inv) {
         block.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.5F - t1, 1.0F);
         if (inv) {
            DrawFaces(rb, block, 105, 104, 108, 108, 108, 108, true);
         } else {
            rb.renderStandardBlock(block, i, j, k);
         }

         block.setBlockBounds(0.5F - t2, 0.5F - t2, 0.5F - t2, 0.5F + t2, 0.5F, 0.5F + t2);
         if (inv) {
            DrawFaces(rb, block, 108, true);
         } else {
            rb.renderStandardBlock(block, i, j, k);
         }

         block.setBlockBounds(t4, 0.5F, t4, 1.0F - t4, 1.0F, 1.0F - t4);
         if (inv) {
            DrawFaces(rb, block, 109, 109, 108, 108, 108, 108, true);
         } else {
            rb.renderSouthFace(block, i, j, k, 108);
            rb.renderNorthFace(block, i, j, k, 108);
            rb.renderWestFace(block, i, j, k, 108);
            rb.renderEastFace(block, i, j, k, 108);
            rb.renderTopFace(block, i, j, k, 109);
            rb.renderBottomFace(block, i, j, k, 109);
         }
      }

      rb.overrideBlockTexture = -1;
      block.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
      return true;
   }

public static boolean renderTrunk(World w, RenderBlocks renderblocks, int i, int j, int k, Block block, int md, boolean inventory) {
      int t1 = 176;
      int t2 = 177;
      int t3 = 178;
      block.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
      DrawFaces(renderblocks, block, t1, t1, t2, t2, t2, t3, true);
      return true;
   }

public static boolean renderBlockVoidInterface(World w, RenderBlocks rb, int i, int j, int k, Block block, int md, boolean inv) {
      if (block.getRenderBlockPass() == 0 || inv) {
         float t1x = 0.0625F;
         float t3x = 0.1875F;
         float t4x = 0.25F;
         float t5x = 0.3125F;
         block.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, t3x, 1.0F);
         if (inv) {
            DrawFaces(rb, block, 105, 105, 106, 106, 106, 106, true);
         } else {
            rb.renderStandardBlock(block, i, j, k);
         }

         block.setBlockBounds(0.0F, 0.5F - t4x, 0.0F, 1.0F, 0.5F - t1x, 1.0F);
         if (inv) {
            DrawFaces(rb, block, 105, 105, 106, 106, 106, 106, true);
         } else {
            rb.renderStandardBlock(block, i, j, k);
         }

         block.setBlockBounds(t4x, 0.5F - t1x, t4x, 1.0F - t4x, 0.5F + t4x, 1.0F - t4x);
         if (inv) {
            DrawFaces(rb, block, 105, 105, 106, 106, 106, 106, true);
         } else {
            rb.renderStandardBlock(block, i, j, k);
         }

         block.setBlockBounds(t1x, t3x, t1x, 1.0F - t1x, 0.5F - t4x, 1.0F - t1x);
         if (!inv) {
            Tessellator tessellator = Tessellator.instance;
            tessellator.setBrightness(255);
            tessellator.setColorOpaque_F(1.0F, 1.0F, 1.0F);
            rb.renderSouthFace(block, i, j, k, mod_ThaumCraft.visCubeFX);
            rb.renderNorthFace(block, i, j, k, mod_ThaumCraft.visCubeFX);
            rb.renderWestFace(block, i, j, k, mod_ThaumCraft.visCubeFX);
            rb.renderEastFace(block, i, j, k, mod_ThaumCraft.visCubeFX);
         } else {
            rb.overrideBlockTexture = mod_ThaumCraft.visCubeFX;
            DrawFaces(rb, block, mod_ThaumCraft.visCubeFX, true);
         }
      }

      rb.overrideBlockTexture = -1;
      block.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
      return true;
   }

public static boolean renderBlockBore(World w, RenderBlocks rb, int i, int j, int k, Block block, int md, boolean inv) {
      if (block.getRenderBlockPass() == 0 || inv) {
         boolean b1 = false;
         boolean b2 = false;
         boolean b3 = false;
         if (!inv) {
            TileBore te = (TileBore)w.getBlockTileEntity(i, j, k);
            if (te.orientation == 0 || te.orientation == 1) {
               b1 = true;
            }

            if (te.orientation == 4 || te.orientation == 5) {
               b2 = true;
            }

            if (te.orientation == 2 || te.orientation == 3) {
               b3 = true;
            }
         }

         float t2x = 0.125F;
         float t4x = 0.25F;
         int t1 = 146;
         int t2 = 145;
         int tx = 146;
         rb.overrideBlockTexture = tx;
         block.setBlockBounds(t2x, t2x, t2x, 1.0F - t2x, 1.0F - t2x, 1.0F - t2x);
         if (inv) {
            DrawFaces(rb, block, t1, true);
         } else {
            rb.renderStandardBlock(block, i, j, k);
         }

         if (b1) {
            tx = t2;
         } else {
            tx = t1;
         }

         rb.overrideBlockTexture = tx;
         block.setBlockBounds(t4x, 0.0F, t4x, 1.0F - t4x, t2x, 1.0F - t4x);
         if (inv) {
            DrawFaces(rb, block, tx, true);
         } else {
            rb.renderStandardBlock(block, i, j, k);
         }

         block.setBlockBounds(t4x, 1.0F - t2x, t4x, 1.0F - t4x, 1.0F, 1.0F - t4x);
         if (inv) {
            DrawFaces(rb, block, tx, true);
         } else {
            rb.renderStandardBlock(block, i, j, k);
         }

         if (b2) {
            tx = t2;
         } else {
            tx = t1;
         }

         rb.overrideBlockTexture = tx;
         block.setBlockBounds(0.0F, t4x, t4x, t2x, 1.0F - t4x, 1.0F - t4x);
         if (inv) {
            DrawFaces(rb, block, tx, true);
         } else {
            rb.renderStandardBlock(block, i, j, k);
         }

         block.setBlockBounds(1.0F - t2x, t4x, t4x, 1.0F, 1.0F - t4x, 1.0F - t4x);
         if (inv) {
            DrawFaces(rb, block, tx, true);
         } else {
            rb.renderStandardBlock(block, i, j, k);
         }

         if (b3) {
            tx = t2;
         } else {
            tx = t1;
         }

         rb.overrideBlockTexture = tx;
         block.setBlockBounds(t4x, t4x, 0.0F, 1.0F - t4x, 1.0F - t4x, t2x);
         if (inv) {
            DrawFaces(rb, block, tx, true);
         } else {
            rb.renderStandardBlock(block, i, j, k);
         }

         block.setBlockBounds(t4x, t4x, 1.0F - t2x, 1.0F - t4x, 1.0F - t4x, 1.0F);
         if (inv) {
            DrawFaces(rb, block, tx, true);
         } else {
            rb.renderStandardBlock(block, i, j, k);
         }
      }

      rb.overrideBlockTexture = -1;
      block.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
      return true;
   }

public static boolean renderBlockCrucible(World w, RenderBlocks rb, int i, int j, int k, Block block, int md, boolean inv) {
      if (block.getRenderBlockPass() == 0 && !inv) {
         block.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
         rb.renderStandardBlock(block, i, j, k);
      } else if (inv && md < 3) {
         int m = 16 * md;
         block.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
         DrawFaces(rb, block, 3 + m, 6 + m, 5 + m, 5 + m, 5 + m, 5 + m, true);
      } else if (inv && md == 3) {
         block.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
         DrawFaces(rb, block, 40, 23, 44, 44, 44, 44, true);
      }

      Tessellator tessellator = Tessellator.instance;
      tessellator.setBrightness(block.getMixedBrightnessForBlock(w, i, j, k));
      float f = 1.0F;
      int l = block.colorMultiplier(w, i, j, k);
      float f1 = (l >> 16 & 0xFF) / 255.0F;
      float f2 = (l >> 8 & 0xFF) / 255.0F;
      float f3 = (l & 0xFF) / 255.0F;
      tessellator.setColorOpaque_F(f * f1, f * f2, f * f3);
      int c = 2 + md * 16;
      int c1 = 1 + md * 16;
      if (md == 3) {
         c = 40;
         c1 = 40;
      }

      float f5 = 0.123F;
      if (!inv) {
         if (block.getRenderBlockPass() == 0) {
            rb.renderSouthFace(block, i - 1.0F + f5, j, k, c);
            rb.renderNorthFace(block, i + 1.0F - f5, j, k, c);
            rb.renderWestFace(block, i, j, k - 1.0F + f5, c);
            rb.renderEastFace(block, i, j, k + 1.0F - f5, c);
            rb.renderTopFace(block, i, j - 1.0F + 0.25F, k, c1);
            rb.renderBottomFace(block, i, j + 1.0F - 0.75F, k, c1);
         } else if (block.getRenderBlockPass() == 1) {
            TileCrucible tc = (TileCrucible)w.getBlockTileEntity(i, j, k);
            float tvis = tc.pureVis + tc.taintedVis;
            if (tvis > 0.1F) {
               float h = Math.min(tvis, tc.maxVis);
               float level = 0.75F * (h / tc.maxVis);
               if (tc.maxVis == tvis) {
                  level = (float)(level - 0.001);
               }

               float b = Math.min(1.0F, tc.pureVis / (tc.taintedVis + tc.pureVis));
               tessellator.setBrightness(20 + (int)(b * 210.0F));
               rb.renderTopFace(block, i, j + 0.25F + level - 1.0F, k, mod_ThaumCraft.visCubeFX);
               if (tvis > tc.maxVis) {
                  rb.renderSouthFace(block, i, j, k, mod_ThaumCraft.visDripFX);
                  rb.renderNorthFace(block, i, j, k, mod_ThaumCraft.visDripFX);
                  rb.renderWestFace(block, i, j, k, mod_ThaumCraft.visDripFX);
                  rb.renderEastFace(block, i, j, k, mod_ThaumCraft.visDripFX);
               }
            }
         }
      }

      rb.overrideBlockTexture = -1;
      block.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
      return true;
   }

public static boolean renderBlockQuaesitum(World w, RenderBlocks rb, int i, int j, int k, Block block, int md, boolean inv) {
      float w3 = 0.1875F;
      float w2 = 0.125F;
      float w1 = 0.0625F;
      float w4 = 0.25F;
      if (block.getRenderBlockPass() == 0 || inv) {
         block.setBlockBounds(0.0F, 0.5F - w2, 0.0F, 1.0F, 0.5F + w2, 1.0F);
         if (inv) {
            DrawFaces(rb, block, 93, 93, 92, 92, 92, 92, true);
         } else {
            rb.renderStandardBlock(block, i, j, k);
         }

         rb.overrideBlockTexture = 93;
         block.setBlockBounds(w2, 0.0F, w2, 1.0F - w2, w3, 1.0F - w2);
         if (inv) {
            DrawFaces(rb, block, 91, 93, 92, 92, 92, 92, true);
         } else {
            rb.renderStandardBlock(block, i, j, k);
         }

         block.setBlockBounds(0.5F - w2, w3, 0.5F - w2, 0.5F + w2, w3 + w3, 0.5F + w2);
         if (inv) {
            DrawFaces(rb, block, 92, true);
         } else {
            rb.renderStandardBlock(block, i, j, k);
         }
      }

      if (block.getRenderBlockPass() == 1 || inv) {
         rb.overrideBlockTexture = 95;
         if (inv) {
            block.setBlockBounds(w1, 0.5F + w2, w1, w1 + w3, 0.5F + w4, w1 + w3);
            if (inv) {
               DrawFaces(rb, block, 95, true);
            } else {
               rb.renderStandardBlock(block, i, j, k);
            }
         } else {
            TileResearcher tr = (TileResearcher)w.getBlockTileEntity(i, j, k);
            switch (tr.orientation) {
               case 0:
                  block.setBlockBounds(1.0F - w4, 0.5F + w2, 1.0F - w4, 1.0F - w1, 0.5F + w4, 1.0F - w1);
                  break;
               case 1:
                  block.setBlockBounds(w1, 0.5F + w2, 1.0F - w4, w4, 0.5F + w4, 1.0F - w1);
                  break;
               case 2:
                  block.setBlockBounds(w1, 0.5F + w2, w1, w4, 0.5F + w4, w4);
                  break;
               case 3:
                  block.setBlockBounds(1.0F - w4, 0.5F + w2, w1, 1.0F - w1, 0.5F + w4, w4);
            }

            rb.renderStandardBlock(block, i, j, k);
         }
      }

      rb.overrideBlockTexture = -1;
      block.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
      return true;
   }

public static boolean renderBlockGenerator(World w, RenderBlocks rb, int i, int j, int k, Block block, int md, boolean inv) {
      float w3 = 0.1875F;
      float w2 = 0.125F;
      float w4 = 0.25F;
      if (block.getRenderBlockPass() == 0 || inv) {
         block.setBlockBounds(w4, 0.0F, w4, 1.0F - w4, w2, 1.0F - w4);
         if (inv) {
            DrawFaces(rb, block, 144, true);
         } else {
            rb.renderStandardBlock(block, i, j, k);
         }

         block.setBlockBounds(w4, 1.0F - w2, w4, 1.0F - w4, 1.0F, 1.0F - w4);
         if (inv) {
            DrawFaces(rb, block, 144, true);
         } else {
            rb.renderStandardBlock(block, i, j, k);
         }

         block.setBlockBounds(1.0F - w2, 0.5F - w4, 0.5F - w4, 1.0F, 0.5F + w4, 0.5F + w4);
         if (inv) {
            DrawFaces(rb, block, 144, true);
         } else {
            rb.renderStandardBlock(block, i, j, k);
         }

         block.setBlockBounds(0.0F, 0.5F - w4, 0.5F - w4, w2, 0.5F + w4, 0.5F + w4);
         if (inv) {
            DrawFaces(rb, block, 144, true);
         } else {
            rb.renderStandardBlock(block, i, j, k);
         }

         block.setBlockBounds(0.5F - w4, 0.5F - w4, 1.0F - w2, 0.5F + w4, 0.5F + w4, 1.0F);
         if (inv) {
            DrawFaces(rb, block, 144, true);
         } else {
            rb.renderStandardBlock(block, i, j, k);
         }

         block.setBlockBounds(0.5F - w4, 0.5F - w4, 0.0F, 0.5F + w4, 0.5F + w4, w2);
         if (inv) {
            DrawFaces(rb, block, 144, true);
         } else {
            rb.renderStandardBlock(block, i, j, k);
         }
      }

      if (block.getRenderBlockPass() == 1 || inv) {
         rb.overrideBlockTexture = 166;
         block.setBlockBounds(w2, w2, w2, 1.0F - w2, 1.0F - w2, 1.0F - w2);
         if (inv) {
            DrawFaces(rb, block, 166, true);
         } else {
            rb.renderStandardBlock(block, i, j, k);
         }
      }

      rb.overrideBlockTexture = -1;
      block.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
      return true;
   }

public static boolean renderBlockCrystalizer(World w, RenderBlocks rb, int i, int j, int k, Block block, int md, boolean inv) {
      float w2 = 0.125F;
      if (block.getRenderBlockPass() == 0 || inv) {
         block.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.5F + w2, 1.0F);
         if (inv) {
            DrawFaces(rb, block, 129, 130, 128, 128, 128, 128, true);
         } else {
            rb.renderStandardBlock(block, i, j, k);
         }

         if (!inv) {
            float w3 = 0.1875F;
            block.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.5F + w2, 1.0F);
            rb.renderSouthFace(block, i - 1 + w3, j, k, 51);
            rb.renderNorthFace(block, i + 1 - w3, j, k, 51);
            rb.renderWestFace(block, i, j, k - 1 + w3, 51);
            rb.renderEastFace(block, i, j, k + 1 - w3, 51);
            rb.renderTopFace(block, i, j - 0.49F, k, 51);
         }
      }

      rb.overrideBlockTexture = -1;
      block.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
      return true;
   }

public static boolean renderBlockInfuser(World w, RenderBlocks rb, int i, int j, int k, Block block, int md, boolean inv) {
      float w1 = 0.0625F;
      float w2 = 0.125F;
      float w3 = 0.1875F;
      float w4 = 0.25F;
      if (block.getRenderBlockPass() == 0 || inv) {
         int add = md == 2 ? 6 : 0;
         block.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F - w1, 1.0F);
         if (inv) {
            DrawFaces(rb, block, 53 + add, 57 + add, 55 + add, 55 + add, 55 + add, 55 + add, false);
         } else {
            rb.renderStandardBlock(block, i, j, k);
         }

         block.setBlockBounds(0.0F, 1.0F - w1, 0.0F, w3, 1.0F, w3);
         if (inv) {
            DrawFaces(rb, block, 53 + add, 57 + add, 55 + add, 55 + add, 55 + add, 55 + add, false);
         } else {
            rb.renderStandardBlock(block, i, j, k);
         }

         block.setBlockBounds(0.0F, 1.0F - w1, 1.0F - w3, w3, 1.0F, 1.0F);
         if (inv) {
            DrawFaces(rb, block, 53 + add, 57 + add, 55 + add, 55 + add, 55 + add, 55 + add, false);
         } else {
            rb.renderStandardBlock(block, i, j, k);
         }

         block.setBlockBounds(1.0F - w3, 1.0F - w1, 0.0F, 1.0F, 1.0F, w3);
         if (inv) {
            DrawFaces(rb, block, 53 + add, 57 + add, 55 + add, 55 + add, 55 + add, 55 + add, false);
         } else {
            rb.renderStandardBlock(block, i, j, k);
         }

         block.setBlockBounds(1.0F - w3, 1.0F - w1, 1.0F - w3, 1.0F, 1.0F, 1.0F);
         if (inv) {
            DrawFaces(rb, block, 53 + add, 57 + add, 55 + add, 55 + add, 55 + add, 55 + add, false);
         } else {
            rb.renderStandardBlock(block, i, j, k);
         }
      }

      rb.overrideBlockTexture = -1;
      block.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
      return true;
   }

public static boolean renderBlockArcaneFurnace(World w, RenderBlocks rb, int i, int j, int k, Block block, int md, boolean inv) {
      if (block.getRenderBlockPass() == 0 && !inv) {
         float w3 = 0.1875F;
         block.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
         rb.renderStandardBlock(block, i, j, k);
         rb.renderTopFace(block, i, j - 1.0F + w3, k, 51);
         rb.renderSouthFace(block, i - 1 + w3, j, k, 51);
         rb.renderNorthFace(block, i + 1 - w3, j, k, 51);
         rb.renderWestFace(block, i, j, k - 1 + w3, 51);
         rb.renderEastFace(block, i, j, k + 1 - w3, 51);
      } else if (inv) {
         block.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
         DrawFaces(rb, block, 50, 49, 48, 48, 48, 48, true);
         block.setBlockBounds(0.1F, 0.1F, 0.1F, 0.9F, 0.99F, 0.9F);
         DrawFaces(rb, block, 51, false);
      }

      rb.overrideBlockTexture = -1;
      block.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
      return true;
   }

public static boolean renderBlockPurifier(World w, RenderBlocks rb, int i, int j, int k, Block block, int md, boolean inv) {
      if (block.getRenderBlockPass() == 0 || inv) {
         block.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
         if (inv) {
            DrawFaces(rb, block, 117, 117, 116, 116, 117, 117, false);
         } else {
            rb.renderStandardBlock(block, i, j, k);
         }
      }

      rb.overrideBlockTexture = -1;
      block.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
      return true;
   }

public static boolean renderBlockTotem(World w, RenderBlocks rb, int i, int j, int k, Block block, int md, boolean inv) {
      float w2 = 0.125F;
      if (block.getRenderBlockPass() == 0 || inv) {
         block.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
         if (inv && md == 3) {
            DrawFaces(rb, block, 127, 126, 125, 124, 123, 122, false);
         } else if (inv && md == 4) {
            DrawFaces(rb, block, 143, 142, 141, 140, 139, 138, false);
         } else {
            rb.renderStandardBlock(block, i, j, k);
         }
      }

      rb.overrideBlockTexture = -1;
      block.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
      return true;
   }

public static boolean renderBlockRepairer(World w, RenderBlocks rb, int i, int j, int k, Block block, int md, boolean inv) {
      if (block.getRenderBlockPass() == 0 || inv) {
         block.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
         if (inv) {
            DrawFaces(rb, block, 86, 86, 87, 87, 87, 87, false);
         } else {
            rb.renderStandardBlock(block, i, j, k);
         }

         if (!inv) {
            rb.renderTopFace(block, i, j - 1.0F + 0.425F, k, 86);
            rb.renderBottomFace(block, i, j + 1 - 0.425F, k, 86);
            rb.renderSouthFace(block, i - 1 + 0.425F, j, k, 86);
            rb.renderNorthFace(block, i + 1 - 0.425F, j, k, 86);
            rb.renderWestFace(block, i, j, k - 1 + 0.425F, 86);
            rb.renderEastFace(block, i, j, k + 1 - 0.425F, 86);
         } else {
            block.setBlockBounds(0.01F, 0.01F, 0.01F, 0.99F, 0.99F, 0.99F);
            DrawFaces(rb, block, 51, false);
         }
      }

      rb.overrideBlockTexture = -1;
      block.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
      return true;
   }

public static boolean renderBlockDuplicator(World w, RenderBlocks rb, int i, int j, int k, Block block, int md, boolean inv) {
      float w4 = 0.25F;
      float w3 = 0.1775F;
      float w2 = 0.125F;
      if (block.getRenderBlockPass() == 0 || inv) {
         block.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
         if (inv) {
            DrawFaces(rb, block, 70, 70, 72, 72, 72, 71, true);
         } else {
            rb.renderStandardBlock(block, i, j, k);
         }

         if (!inv) {
            // These planes face into the cavity. Their back faces must not cover
            // the press when viewed through the front opening.
            context().draw().cull(true);
            rb.renderTopFace(block, i, j - 1.0F + w3, k, 73);
            rb.renderSouthFace(block, i - 1 + w3, j, k, 73);
            rb.renderNorthFace(block, i + 1 - w3, j, k, 73);
            rb.renderWestFace(block, i, j, k - 1 + w3, 73);
            rb.renderEastFace(block, i, j, k + 1 - w3, 73);
            context().draw().cull(false);
         } else {
            block.setBlockBounds(0.1F, 0.1F, 0.1F, 0.9F, 0.99F, 0.9F);
            DrawFaces(rb, block, 51, false);
         }
      }

      rb.overrideBlockTexture = -1;
      block.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
      return true;
   }

public static boolean renderBlockBrain(World w, RenderBlocks rb, int i, int j, int k, Block block, int md, boolean inv) {
      float w4 = 0.25F;
      float w3 = 0.1875F;
      float w2 = 0.125F;
      if (block.getRenderBlockPass() == 0 || inv) {
         block.setBlockBounds(w4, 0.0F, w4, 1.0F - w4, w2, 1.0F - w4);
         if (inv) {
            DrawFaces(rb, block, 168, 168, 167, 167, 167, 167, false);
         } else {
            rb.renderStandardBlock(block, i, j, k);
         }

         block.setBlockBounds(w4, 1.0F - w3, w4, 1.0F - w4, 1.0F, 1.0F - w4);
         if (inv) {
            DrawFaces(rb, block, 168, 168, 167, 167, 167, 167, false);
         } else {
            rb.renderStandardBlock(block, i, j, k);
         }

         if (inv) {
            rb.overrideBlockTexture = 166;
            block.setBlockBounds(w2, w2, w2, 1.0F - w2, 1.0F - w3, 1.0F - w2);
            DrawFaces(rb, block, 166, true);
         }
      }

      if (block.getRenderBlockPass() == 1 && !inv) {
         rb.overrideBlockTexture = 166;
         block.setBlockBounds(w2, w2, w2, 1.0F - w2, 1.0F - w3, 1.0F - w2);
         rb.renderStandardBlock(block, i, j, k);
      }

      rb.overrideBlockTexture = -1;
      block.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
      return true;
   }

public static boolean renderEnchanterAdvanced(World w, RenderBlocks rb, int i, int j, int k, Block block, int md, boolean inv) {
      float w3 = 0.1875F;
      float w2 = 0.125F;
      float w1 = 0.0625F;
      if (block.getRenderBlockPass() == 0 || inv) {
         block.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.5F, 1.0F);
         if (inv) {
            DrawFaces(rb, block, 164, 164, 163, 163, 163, 163, false);
         } else {
            rb.renderStandardBlock(block, i, j, k);
         }

         block.setBlockBounds(0.0F, 0.5F, 0.5F - w3, w2, 0.5F + w3, 0.5F + w3);
         if (inv) {
            DrawFaces(rb, block, 164, 164, 163, 163, 163, 163, false);
         } else {
            rb.renderStandardBlock(block, i, j, k);
         }

         block.setBlockBounds(1.0F - w2, 0.5F, 0.5F - w3, 1.0F, 0.5F + w3, 0.5F + w3);
         if (inv) {
            DrawFaces(rb, block, 164, 164, 163, 163, 163, 163, false);
         } else {
            rb.renderStandardBlock(block, i, j, k);
         }

         block.setBlockBounds(0.5F - w3, 0.5F, 0.0F, 0.5F + w3, 0.5F + w3, w2);
         if (inv) {
            DrawFaces(rb, block, 164, 164, 163, 163, 163, 163, false);
         } else {
            rb.renderStandardBlock(block, i, j, k);
         }

         block.setBlockBounds(0.5F - w3, 0.5F, 1.0F - w2, 0.5F + w3, 0.5F + w3, 1.0F);
         if (inv) {
            DrawFaces(rb, block, 164, 164, 163, 163, 163, 163, false);
         } else {
            rb.renderStandardBlock(block, i, j, k);
         }

         if (inv) {
            rb.overrideBlockTexture = 165;
            block.setBlockBounds(w2, 0.5F, w2, 1.0F - w2, 1.0F, 1.0F - w2);
            DrawFaces(rb, block, 165, true);
         } else {
            block.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.499F, 1.0F);
            rb.renderSouthFace(block, i - 1 + w3, j, k, 51);
            rb.renderNorthFace(block, i + 1 - w3, j, k, 51);
            rb.renderWestFace(block, i, j, k - 1 + w3, 51);
            rb.renderEastFace(block, i, j, k + 1 - w3, 51);
            rb.renderTopFace(block, i, j - 0.5F + w1, k, 51);
         }
      }

      if (block.getRenderBlockPass() == 1 && !inv) {
         rb.overrideBlockTexture = 165;
         block.setBlockBounds(w2, 0.5F, w2, 1.0F - w2, 1.0F, 1.0F - w2);
         rb.renderStandardBlock(block, i, j, k);
      }

      rb.overrideBlockTexture = -1;
      block.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
      return true;
   }

public static boolean renderBlockTank(World w, RenderBlocks rb, int i, int j, int k, Block block, int md, boolean inv) {
      if (block.getRenderBlockPass() == 0 || inv) {
         float w1 = 0.0625F;
         float w2 = 0.125F;
         int t1 = 26;
         int t2 = 27;
         if (md != 3) {
            t1 = 78;
            t2 = 79;
         }

          block.setBlockBounds(w1, 0.0F, w1, 1.0F - w1, 1.0F, 1.0F - w1);
          if (inv) {
             DrawFaces(rb, block, t1, t1, t2, t2, t2, t2, false);
           } else {
             // Stacked tanks share exactly coplanar glass caps that flicker when both draw.
             // Shrink the stacked sides by an invisible epsilon instead of deleting a face,
             // so caps stay visible from both above and below with no z-fighting.
             float y0 = w.getBlockTileEntity(i, j - 1, k) instanceof TileConduitTank ? 0.002F : 0.0F;
             float y1 = w.getBlockTileEntity(i, j + 1, k) instanceof TileConduitTank ? 0.998F : 1.0F;
             block.setBlockBounds(w1, y0, w1, 1.0F - w1, y1, 1.0F - w1);
             rb.renderStandardBlock(block, i, j, k);
           }

         if (!inv) {
            rb.overrideBlockTexture = 28;
            TileConduitTank tc = (TileConduitTank)w.getBlockTileEntity(i, j, k);
            HelperLocation loc = new HelperLocation(tc);
            loc.facing = HelperFacing.NEGX;
            TileEntity te = loc.getConnectableTile(w);
            if (te != null && tc.getConnectable(loc.facing) && !(te instanceof TileConduitTank)) {
               block.setBlockBounds(0.0F, 0.5F - w2, 0.5F - w2, w1, 0.5F + w2, 0.5F + w2);
               rb.renderStandardBlock(block, i, j, k);
            }

            loc = new HelperLocation(tc);
            loc.facing = HelperFacing.POSX;
            te = loc.getConnectableTile(w);
            if (te != null && tc.getConnectable(loc.facing) && !(te instanceof TileConduitTank)) {
               block.setBlockBounds(1.0F - w1, 0.5F - w2, 0.5F - w2, 1.0F, 0.5F + w2, 0.5F + w2);
               rb.renderStandardBlock(block, i, j, k);
            }

            loc = new HelperLocation(tc);
            loc.facing = HelperFacing.NEGZ;
            te = loc.getConnectableTile(w);
            if (te != null && tc.getConnectable(loc.facing) && !(te instanceof TileConduitTank)) {
               block.setBlockBounds(0.5F - w2, 0.5F - w2, 0.0F, 0.5F + w2, 0.5F + w2, w1);
               rb.renderStandardBlock(block, i, j, k);
            }

            loc = new HelperLocation(tc);
            loc.facing = HelperFacing.POSZ;
            te = loc.getConnectableTile(w);
            if (te != null && tc.getConnectable(loc.facing) && !(te instanceof TileConduitTank)) {
               block.setBlockBounds(0.5F - w2, 0.5F - w2, 1.0F - w1, 0.5F + w2, 0.5F + w2, 1.0F);
               rb.renderStandardBlock(block, i, j, k);
            }
         }
      }

      if (block.getRenderBlockPass() == 1 && !inv) {
         float wx = 0.003F;
         float w1 = 0.0625F;
         TileConduitTank tc = (TileConduitTank)w.getBlockTileEntity(i, j, k);
         if (tc != null && tc.pureVis + tc.taintedVis > 0.1F) {
            Tessellator tessellator = Tessellator.instance;
            float hfill = (1.0F - wx * 2.0F) * ((tc.pureVis + tc.taintedVis) / tc.getMaxVis());
            float b = Math.min(1.0F, tc.pureVis / (tc.taintedVis + tc.pureVis));
            block.setBlockBounds(wx + w1, wx, wx + w1, 1.0F - wx - w1, wx + hfill, 1.0F - wx - w1);
            tessellator.setBrightness(20 + (int)(b * 210.0F));
            tessellator.setColorOpaque_F(1.0F, 1.0F, 1.0F);
            rb.renderSouthFace(block, i, j, k, mod_ThaumCraft.visCubeFX);
            rb.renderNorthFace(block, i, j, k, mod_ThaumCraft.visCubeFX);
            rb.renderWestFace(block, i, j, k, mod_ThaumCraft.visCubeFX);
            rb.renderEastFace(block, i, j, k, mod_ThaumCraft.visCubeFX);
            rb.renderTopFace(block, i, j, k, mod_ThaumCraft.visCubeFX);
            rb.renderBottomFace(block, i, j, k, mod_ThaumCraft.visCubeFX);
         }
      }

      rb.overrideBlockTexture = -1;
      block.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
      return true;
   }

public static boolean renderBlockBellows(World w, RenderBlocks rb, int i, int j, int k, Block block, int md, boolean inv) {
      float w4 = 0.25F;
      float w3 = 0.1875F;
      float w2 = 0.125F;
      float w1 = 0.0625F;
      if (inv) {
         block.setBlockBounds(w2, 0.0F, w2, 1.0F - w2, w2, 1.0F - w2);
         DrawFaces(rb, block, 81, 81, 80, 80, 80, 80, false);
         block.setBlockBounds(w2, 1.0F - w2, w2, 1.0F - w2, 1.0F, 1.0F - w2);
         DrawFaces(rb, block, 81, 81, 80, 80, 80, 80, false);
         block.setBlockBounds(w2, 0.5F - w1, w2, 1.0F - w2, 0.5F + w1, 1.0F - w2);
         DrawFaces(rb, block, 81, 81, 80, 80, 80, 80, false);
         block.setBlockBounds(w3, w2, w3, 1.0F - w3, 1.0F - w2, 1.0F - w3);
         DrawFaces(rb, block, 82, false);
         block.setBlockBounds(1.0F - w3, 0.5F - w2, 0.5F - w2, 1.0F, 0.5F + w2, 0.5F + w2);
         DrawFaces(rb, block, 83, false);
      }

      rb.overrideBlockTexture = -1;
      block.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
      return true;
   }

public static boolean renderBlockPump(World w, RenderBlocks rb, int i, int j, int k, Block block, int md, boolean inv) {
      float w4 = 0.25F;
      float w3 = 0.1875F;
      float w2 = 0.125F;
      float w1 = 0.0625F;
      if (inv) {
         block.setBlockBounds(w2, 0.0F, w2, 1.0F - w2, w2, 1.0F - w2);
         DrawFaces(rb, block, 132, 132, 131, 131, 131, 131, false);
         block.setBlockBounds(w3, w2, w3, 1.0F - w3, w4, 1.0F - w3);
         DrawFaces(rb, block, 132, 132, 131, 131, 131, 131, false);
         block.setBlockBounds(0.0F, w4, 0.0F, 1.0F, 0.5F + w2, 1.0F);
         DrawFaces(rb, block, 132, 132, 131, 131, 131, 131, false);
         block.setBlockBounds(w2, 0.5F + w2, w2, 1.0F - w2, 1.0F, 1.0F - w2);
         DrawFaces(rb, block, 132, 132, 131, 131, 131, 131, false);
      }

      rb.overrideBlockTexture = -1;
      block.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
      return true;
   }

public static boolean renderBlockCondenser(World w, RenderBlocks rb, int i, int j, int k, Block block, int md, boolean inv) {
      float w4 = 0.25F;
      float w3 = 0.1875F;
      float w2 = 0.125F;
      if (block.getRenderBlockPass() == 0 || inv) {
         block.setBlockBounds(w3, 1.0F - w4, w3, 1.0F - w3, 1.0F, 1.0F - w3);
         if (inv) {
            DrawFaces(rb, block, 113, 113, 112, 112, 112, 112, false);
         } else {
            rb.renderStandardBlock(block, i, j, k);
         }

         block.setBlockBounds(w3, 0.0F, w3, 1.0F - w3, w4, 1.0F - w3);
         if (inv) {
            DrawFaces(rb, block, 113, 113, 112, 112, 112, 112, false);
         } else {
            rb.renderStandardBlock(block, i, j, k);
         }

         rb.overrideBlockTexture = 114;
         block.setBlockBounds(0.5F - w2, w2, 0.0F, 0.5F + w2, 1.0F - w2, 1.0F);
         if (inv) {
            DrawFaces(rb, block, 114, false);
         } else {
            rb.renderStandardBlock(block, i, j, k);
         }

         block.setBlockBounds(0.0F, w2, 0.5F - w2, 1.0F, 1.0F - w2, 0.5F + w2);
         if (inv) {
            DrawFaces(rb, block, 114, false);
         } else {
            rb.renderStandardBlock(block, i, j, k);
         }

         block.setBlockBounds(0.5F - w2, w2, 0.0F, 0.5F + w2, 1.0F - w2, 1.0F);
         if (inv) {
            DrawFaces(rb, block, 114, false);
         } else {
            rb.renderStandardBlock(block, i, j, k);
         }

         block.setBlockBounds(0.0F, w2, 0.5F - w2, 1.0F, 1.0F - w2, 0.5F + w2);
         if (inv) {
            DrawFaces(rb, block, 114, false);
         } else {
            rb.renderStandardBlock(block, i, j, k);
         }

         if (!inv) {
            TileCondenser tc = (TileCondenser)w.getBlockTileEntity(i, j, k);
            if (tc != null && tc.hasUpgrade((byte)0)) {
               rb.overrideBlockTexture = mod_ThaumCraft.spdUpgrade;
               block.setBlockBounds(w4, w4, w4, 1.0F - w4, 1.0F - w4, 1.0F - w4);
               rb.renderStandardBlock(block, i, j, k);
            }
         }
      }

      rb.overrideBlockTexture = -1;
      block.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
      return true;
   }

public static boolean renderBlockFilter(World w, RenderBlocks rb, int i, int j, int k, Block block, int md, boolean inv) {
      float w4 = 0.25F;
      float w3 = 0.1875F;
      float w2 = 0.125F;
      if (block.getRenderBlockPass() == 0 || inv) {
         block.setBlockBounds(w2, 0.0F, w2, 1.0F - w2, w3, 1.0F - w2);
         if (inv) {
            DrawFaces(rb, block, 66, 66, 65, 65, 65, 65, false);
         } else {
            rb.renderStandardBlock(block, i, j, k);
         }

         block.setBlockBounds(w2, 1.0F - w3, w2, 1.0F - w2, 1.0F, 1.0F - w2);
         if (inv) {
            DrawFaces(rb, block, 66, 66, 65, 65, 65, 65, false);
         } else {
            rb.renderStandardBlock(block, i, j, k);
         }

         block.setBlockBounds(w3, w3, w3, 1.0F - w3, 1.0F - w3, 1.0F - w3);
         if (inv) {
            DrawFaces(rb, block, 66, 66, 65, 65, 65, 65, false);
         } else {
            rb.renderStandardBlock(block, i, j, k);
         }
      }

      if (!inv) {
         float w6 = 0.375F;
         float wq = 0.38125F;
         float w1 = 0.0625F;
         TileConduit tc = (TileConduit)w.getBlockTileEntity(i, j, k);
         Tessellator tessellator = Tessellator.instance;
         float b = 0.0F;
         float total = 0.0F;
         float hfill = 0.0F;
         boolean visible = false;
         if (block.getRenderBlockPass() != 0) {
            visible = tc.displayPure + tc.displayTaint >= 0.1F;
            if (visible) {
               b = Math.min(1.0F, tc.displayPure / (tc.displayTaint + tc.displayPure));
               total = Math.min(tc.displayPure + tc.displayTaint, tc.maxVis);
               hfill = (1.0F - wq * 2.0F) * (total / tc.maxVis);
               tessellator.setBrightness(20 + (int)(b * 210.0F));
               tessellator.setColorOpaque_F(1.0F, 1.0F, 1.0F);
            }
         }

         for (int dir = 2; dir < 6; dir++) {
            HelperLocation loc = new HelperLocation(tc);
            switch (dir) {
               case 2:
                  loc.facing = HelperFacing.POSZ;
                  break;
               case 3:
                  loc.facing = HelperFacing.NEGZ;
                  break;
               case 4:
                  loc.facing = HelperFacing.POSX;
                  break;
               case 5:
                  loc.facing = HelperFacing.NEGX;
            }

            TileEntity te = loc.getConnectableTile(w);
            if (te != null && tc.getConnectable(loc.facing)) {
               if (block.getRenderBlockPass() == 0) {
                  rb.overrideBlockTexture = 67;
                  switch (dir) {
                     case 2:
                        block.setBlockBounds(w6, w6, 1.0F - w3, w6 + w4, w6 + w4, 1.0F);
                        rb.renderStandardBlock(block, i, j, k);
                        break;
                     case 3:
                        block.setBlockBounds(w6, w6, 0.0F, w6 + w4, w6 + w4, w3);
                        rb.renderStandardBlock(block, i, j, k);
                        break;
                     case 4:
                        block.setBlockBounds(1.0F - w3, w6, w6, 1.0F, w6 + w4, w6 + w4);
                        rb.renderStandardBlock(block, i, j, k);
                        break;
                     case 5:
                        block.setBlockBounds(0.0F, w6, w6, w3, w6 + w4, w6 + w4);
                        rb.renderStandardBlock(block, i, j, k);
                  }
               } else if (visible && (((IConnection)te).getPureVis() + ((IConnection)te).getTaintedVis() > 0.1F || !((IConnection)te).isVisConduit())) {
                  renderConduitVis(w, rb, i, j, k, block, dir, hfill);
               }
            }
         }

         if (block.getRenderBlockPass() != 0) {
            tc.displayPure = 0.0F;
            tc.displayTaint = 0.0F;
         }
      }

      rb.overrideBlockTexture = -1;
      block.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
      return true;
   }

private static void renderConduitVis(World w, RenderBlocks rb, int i, int j, int k, Block block, int dir, float hfill) {
      float w4 = 0.25F;
      float w6 = 0.375F;
      float wq = 0.38125F;
      float w1 = 0.0625F;
      Tessellator tessellator = Tessellator.instance;
      switch (dir) {
         case 0:
            block.setBlockBounds(0.5F - hfill / 2.0F, wq + hfill, 0.5F - hfill / 2.0F, 0.5F + hfill / 2.0F, 1.0F, 0.5F + hfill / 2.0F);
            rb.renderSouthFace(block, i, j, k, mod_ThaumCraft.visCubeFX);
            rb.renderNorthFace(block, i, j, k, mod_ThaumCraft.visCubeFX);
            rb.renderWestFace(block, i, j, k, mod_ThaumCraft.visCubeFX);
            rb.renderEastFace(block, i, j, k, mod_ThaumCraft.visCubeFX);
            break;
         case 1:
            block.setBlockBounds(0.5F - hfill / 2.0F, 0.0F, 0.5F - hfill / 2.0F, 0.5F + hfill / 2.0F, wq, 0.5F + hfill / 2.0F);
            rb.renderSouthFace(block, i, j, k, mod_ThaumCraft.visCubeFX);
            rb.renderNorthFace(block, i, j, k, mod_ThaumCraft.visCubeFX);
            rb.renderWestFace(block, i, j, k, mod_ThaumCraft.visCubeFX);
            rb.renderEastFace(block, i, j, k, mod_ThaumCraft.visCubeFX);
            break;
         case 2:
            block.setBlockBounds(wq, wq, 1.0F - wq, 1.0F - wq, wq + hfill, 1.0F);
            rb.renderSouthFace(block, i, j, k, mod_ThaumCraft.visCubeFX);
            rb.renderNorthFace(block, i, j, k, mod_ThaumCraft.visCubeFX);
            rb.renderTopFace(block, i, j, k, mod_ThaumCraft.visCubeFX);
            rb.renderBottomFace(block, i, j, k, mod_ThaumCraft.visCubeFX);
            break;
         case 3:
            block.setBlockBounds(wq, wq, 0.0F, 1.0F - wq, wq + hfill, wq);
            rb.renderSouthFace(block, i, j, k, mod_ThaumCraft.visCubeFX);
            rb.renderNorthFace(block, i, j, k, mod_ThaumCraft.visCubeFX);
            rb.renderTopFace(block, i, j, k, mod_ThaumCraft.visCubeFX);
            rb.renderBottomFace(block, i, j, k, mod_ThaumCraft.visCubeFX);
            break;
         case 4:
            block.setBlockBounds(1.0F - wq, wq, wq, 1.0F, wq + hfill, 1.0F - wq);
            rb.renderWestFace(block, i, j, k, mod_ThaumCraft.visCubeFX);
            rb.renderEastFace(block, i, j, k, mod_ThaumCraft.visCubeFX);
            rb.renderTopFace(block, i, j, k, mod_ThaumCraft.visCubeFX);
            rb.renderBottomFace(block, i, j, k, mod_ThaumCraft.visCubeFX);
            break;
         case 5:
            block.setBlockBounds(0.0F, wq, wq, wq, wq + hfill, 1.0F - wq);
            rb.renderWestFace(block, i, j, k, mod_ThaumCraft.visCubeFX);
            rb.renderEastFace(block, i, j, k, mod_ThaumCraft.visCubeFX);
            rb.renderTopFace(block, i, j, k, mod_ThaumCraft.visCubeFX);
            rb.renderBottomFace(block, i, j, k, mod_ThaumCraft.visCubeFX);
      }
   }

public static boolean renderBlockConduit(World w, RenderBlocks rb, int i, int j, int k, Block block, int md, boolean inv) {
      float w4 = 0.25F;
      float w6 = 0.375F;
      float wq = 0.38125F;
      float w1 = 0.0625F;
      Tessellator tessellator = Tessellator.instance;
      if (!inv) {
         TileConduit tc = (TileConduit)w.getBlockTileEntity(i, j, k);
         float b = 0.0F;
         float total = 0.0F;
         float hfill = 0.0F;
         boolean visible = false;
         if (block.getRenderBlockPass() == 0) {
            rb.overrideBlockTexture = 11;
            block.setBlockBounds(w6, w6, w6, w6 + w4, w6 + w4, w6 + w4);
            rb.renderStandardBlock(block, i, j, k);
            rb.overrideBlockTexture = 12;
         } else {
            visible = tc.displayPure + tc.displayTaint >= 0.1F;
            if (visible) {
               b = Math.min(1.0F, tc.displayPure / (tc.displayTaint + tc.displayPure));
               total = Math.min(tc.displayPure + tc.displayTaint, tc.maxVis);
               hfill = (1.0F - wq * 2.0F) * (total / tc.maxVis);
               tessellator.setBrightness(20 + (int)(b * 210.0F));
               tessellator.setColorOpaque_F(1.0F, 1.0F, 1.0F);
               block.setBlockBounds(wq, wq, wq, 1.0F - wq, wq + hfill, 1.0F - wq);
               rb.renderSouthFace(block, i, j, k, mod_ThaumCraft.visCubeFX);
               rb.renderNorthFace(block, i, j, k, mod_ThaumCraft.visCubeFX);
               rb.renderWestFace(block, i, j, k, mod_ThaumCraft.visCubeFX);
               rb.renderEastFace(block, i, j, k, mod_ThaumCraft.visCubeFX);
               rb.renderTopFace(block, i, j, k, mod_ThaumCraft.visCubeFX);
               rb.renderBottomFace(block, i, j, k, mod_ThaumCraft.visCubeFX);
               if (!Config.lowGfx && Config.pipedrips && w.rand.nextInt(50) == 1 && w.isAirBlock(i, j - 1, k) && tc.displayPure + tc.displayTaint > 3.5F) {
                  FXDrip obj = new FXDrip(w, i + w6 + w.rand.nextFloat() * w4, j + w6 - 0.05F, k + w6 + w.rand.nextFloat() * w4);
                  obj.setRBGColorF((0.4F + w.rand.nextFloat() * 0.2F) * (b + 0.1F), 0.0F, (0.8F + w.rand.nextFloat() * 0.2F) * (b + 0.1F));
                  ModLoader.getMinecraftInstance().effectRenderer.addEffect(obj);
               }
            }
         }

         for (int dir = 0; dir < 6; dir++) {
            HelperLocation loc = new HelperLocation(tc);
            switch (dir) {
               case 0:
                  loc.facing = HelperFacing.POSY;
                  break;
               case 1:
                  loc.facing = HelperFacing.NEGY;
                  break;
               case 2:
                  loc.facing = HelperFacing.POSZ;
                  break;
               case 3:
                  loc.facing = HelperFacing.NEGZ;
                  break;
               case 4:
                  loc.facing = HelperFacing.POSX;
                  break;
               case 5:
                  loc.facing = HelperFacing.NEGX;
            }

            TileEntity te = loc.getConnectableTile(w);
            if (te != null) {
               if (block.getRenderBlockPass() == 0) {
                  switch (dir) {
                     case 0:
                        block.setBlockBounds(w6, w6 + w4, w6, w6 + w4, 1.0F, w6 + w4);
                        rb.renderStandardBlock(block, i, j, k);
                        break;
                     case 1:
                        block.setBlockBounds(w6, 0.0F, w6, w6 + w4, w6, w6 + w4);
                        rb.renderStandardBlock(block, i, j, k);
                        break;
                     case 2:
                        block.setBlockBounds(w6, w6, w6 + w4, w6 + w4, w6 + w4, 1.0F);
                        rb.renderStandardBlock(block, i, j, k);
                        break;
                     case 3:
                        block.setBlockBounds(w6, w6, 0.0F, w6 + w4, w6 + w4, w6);
                        rb.renderStandardBlock(block, i, j, k);
                        break;
                     case 4:
                        block.setBlockBounds(w6 + w4, w6, w6, 1.0F, w6 + w4, w6 + w4);
                        rb.renderStandardBlock(block, i, j, k);
                        break;
                     case 5:
                        block.setBlockBounds(0.0F, w6, w6, w6, w6 + w4, w6 + w4);
                        rb.renderStandardBlock(block, i, j, k);
                  }
               } else if (visible && (((IConnection)te).getPureVis() + ((IConnection)te).getTaintedVis() > 0.1F || !((IConnection)te).isVisConduit())) {
                  renderConduitVis(w, rb, i, j, k, block, dir, hfill);
               }
            }
         }

         if (block.getRenderBlockPass() != 0) {
            tc.displayPure = 0.0F;
            tc.displayTaint = 0.0F;
         }
      } else {
         block.setBlockBounds(w6, 0.0F, w6, w6 + w4, 1.0F, w6 + w4);
         DrawFaces(rb, block, 28, false);
      }

      rb.overrideBlockTexture = -1;
      block.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
      return true;
   }

public static boolean renderBlockValve(World w, RenderBlocks rb, int i, int j, int k, Block block, int md, boolean inv) {
      float w4 = 0.25F;
      float wq = 0.38125F;
      float w1 = 0.0625F;
      float w6 = 0.375F;
      Tessellator tessellator = Tessellator.instance;
      if (!inv) {
         TileConduitValve tc = (TileConduitValve)w.getBlockTileEntity(i, j, k);
         float b = 0.0F;
         float total = 0.0F;
         float hfill = 0.0F;
         boolean visible = false;
         if (block.getRenderBlockPass() == 0) {
            if (tc.open) {
               rb.overrideBlockTexture = 9;
            } else {
               rb.overrideBlockTexture = 10;
            }

            block.setBlockBounds(w4, w4, w4, 1.0F - w4, 1.0F - w4, 1.0F - w4);
            rb.renderStandardBlock(block, i, j, k);
            rb.overrideBlockTexture = 12;
         } else {
            visible = tc.displayPure + tc.displayTaint >= 0.1F;
            if (visible) {
               b = Math.min(1.0F, tc.displayPure / (tc.displayTaint + tc.displayPure));
               total = Math.min(tc.displayPure + tc.displayTaint, tc.maxVis);
               hfill = (1.0F - wq * 2.0F) * (total / tc.maxVis);
               tessellator.setBrightness(20 + (int)(b * 210.0F));
               tessellator.setColorOpaque_F(1.0F, 1.0F, 1.0F);
               if (!Config.lowGfx && Config.pipedrips && w.rand.nextInt(50) == 1 && w.isAirBlock(i, j - 1, k) && tc.displayPure + tc.displayTaint > 3.5F) {
                  FXDrip obj = new FXDrip(w, i + w4 + w.rand.nextFloat() * w6, j + w4 - 0.05F, k + w4 + w.rand.nextFloat() * w6);
                  obj.setRBGColorF((0.4F + w.rand.nextFloat() * 0.2F) * (b + 0.1F), 0.0F, (0.8F + w.rand.nextFloat() * 0.2F) * (b + 0.1F));
                  ModLoader.getMinecraftInstance().effectRenderer.addEffect(obj);
               }
            }
         }

         for (int dir = 0; dir < 6; dir++) {
            HelperLocation loc = new HelperLocation(tc);
            switch (dir) {
               case 0:
                  loc.facing = HelperFacing.POSY;
                  break;
               case 1:
                  loc.facing = HelperFacing.NEGY;
                  break;
               case 2:
                  loc.facing = HelperFacing.POSZ;
                  break;
               case 3:
                  loc.facing = HelperFacing.NEGZ;
                  break;
               case 4:
                  loc.facing = HelperFacing.POSX;
                  break;
               case 5:
                  loc.facing = HelperFacing.NEGX;
            }

            TileEntity te = loc.getConnectableTile(w);
            if (te != null) {
               if (block.getRenderBlockPass() == 0) {
                  switch (dir) {
                     case 0:
                        block.setBlockBounds(w6, 1.0F - w4, w6, 1.0F - w6, 1.0F, 1.0F - w6);
                        rb.renderStandardBlock(block, i, j, k);
                        break;
                     case 1:
                        block.setBlockBounds(w6, 0.0F, w6, 1.0F - w6, w6, 1.0F - w6);
                        rb.renderStandardBlock(block, i, j, k);
                        break;
                     case 2:
                        block.setBlockBounds(w6, w6, 1.0F - w4, 1.0F - w6, 1.0F - w6, 1.0F);
                        rb.renderStandardBlock(block, i, j, k);
                        break;
                     case 3:
                        block.setBlockBounds(w6, w6, 0.0F, 1.0F - w6, 1.0F - w6, w4);
                        rb.renderStandardBlock(block, i, j, k);
                        break;
                     case 4:
                        block.setBlockBounds(1.0F - w4, w6, w6, 1.0F, 1.0F - w6, 1.0F - w6);
                        rb.renderStandardBlock(block, i, j, k);
                        break;
                     case 5:
                        block.setBlockBounds(0.0F, w6, w6, w4, 1.0F - w6, 1.0F - w6);
                        rb.renderStandardBlock(block, i, j, k);
                  }
               } else if (visible && (((IConnection)te).getPureVis() + ((IConnection)te).getTaintedVis() > 0.1F || !((IConnection)te).isVisConduit())) {
                  renderConduitVis(w, rb, i, j, k, block, dir, hfill);
               }
            }
         }

         if (block.getRenderBlockPass() != 0) {
            tc.displayPure = 0.0F;
            tc.displayTaint = 0.0F;
         }
      } else {
         block.setBlockBounds(w6, 0.0F, w6, 1.0F - w6, 1.0F, 1.0F - w6);
         DrawFaces(rb, block, 28, false);
         block.setBlockBounds(w4, w4, w4, 1.0F - w4, 1.0F - w4, 1.0F - w4);
         DrawFaces(rb, block, 9, false);
      }

      rb.overrideBlockTexture = -1;
      block.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
      return true;
   }

public static boolean renderBlockValveAdvanced(World w, RenderBlocks rb, int i, int j, int k, Block block, int md, boolean inv) {
      float w4 = 0.25F;
      float wq = 0.38125F;
      float w1 = 0.0625F;
      float w6 = 0.375F;
      Tessellator tessellator = Tessellator.instance;
      if (!inv) {
         TileConduitValveAdvanced tc = (TileConduitValveAdvanced)w.getBlockTileEntity(i, j, k);
         float b = 0.0F;
         float total = 0.0F;
         float hfill = 0.0F;
         boolean visible = false;
         if (block.getRenderBlockPass() == 0) {
            switch (tc.setting) {
               case 0:
                  rb.overrideBlockTexture = 25;
                  break;
               case 1:
                  rb.overrideBlockTexture = 8;
                  break;
               case 2:
                  rb.overrideBlockTexture = 24;
            }

            block.setBlockBounds(w4, w4, w4, 1.0F - w4, 1.0F - w4, 1.0F - w4);
            rb.renderStandardBlock(block, i, j, k);
            rb.overrideBlockTexture = 12;
         } else {
            visible = tc.displayPure + tc.displayTaint >= 0.1F;
            if (visible) {
               b = Math.min(1.0F, tc.displayPure / (tc.displayTaint + tc.displayPure));
               total = Math.min(tc.displayPure + tc.displayTaint, tc.maxVis);
               hfill = (1.0F - wq * 2.0F) * (total / tc.maxVis);
               tessellator.setBrightness(20 + (int)(b * 210.0F));
               tessellator.setColorOpaque_F(1.0F, 1.0F, 1.0F);
               if (!Config.lowGfx && Config.pipedrips && w.rand.nextInt(50) == 1 && w.isAirBlock(i, j - 1, k) && tc.displayPure + tc.displayTaint > 3.5F) {
                  FXDrip obj = new FXDrip(w, i + w4 + w.rand.nextFloat() * w6, j + w4 - 0.05F, k + w4 + w.rand.nextFloat() * w6);
                  obj.setRBGColorF((0.4F + w.rand.nextFloat() * 0.2F) * (b + 0.1F), 0.0F, (0.8F + w.rand.nextFloat() * 0.2F) * (b + 0.1F));
                  ModLoader.getMinecraftInstance().effectRenderer.addEffect(obj);
               }
            }
         }

         for (int dir = 0; dir < 6; dir++) {
            HelperLocation loc = new HelperLocation(tc);
            switch (dir) {
               case 0:
                  loc.facing = HelperFacing.POSY;
                  break;
               case 1:
                  loc.facing = HelperFacing.NEGY;
                  break;
               case 2:
                  loc.facing = HelperFacing.POSZ;
                  break;
               case 3:
                  loc.facing = HelperFacing.NEGZ;
                  break;
               case 4:
                  loc.facing = HelperFacing.POSX;
                  break;
               case 5:
                  loc.facing = HelperFacing.NEGX;
            }

            TileEntity te = loc.getConnectableTile(w);
            if (te != null) {
               if (block.getRenderBlockPass() == 0) {
                  switch (dir) {
                     case 0:
                        block.setBlockBounds(w6, 1.0F - w4, w6, 1.0F - w6, 1.0F, 1.0F - w6);
                        rb.renderStandardBlock(block, i, j, k);
                        break;
                     case 1:
                        block.setBlockBounds(w6, 0.0F, w6, 1.0F - w6, w6, 1.0F - w6);
                        rb.renderStandardBlock(block, i, j, k);
                        break;
                     case 2:
                        block.setBlockBounds(w6, w6, 1.0F - w4, 1.0F - w6, 1.0F - w6, 1.0F);
                        rb.renderStandardBlock(block, i, j, k);
                        break;
                     case 3:
                        block.setBlockBounds(w6, w6, 0.0F, 1.0F - w6, 1.0F - w6, w4);
                        rb.renderStandardBlock(block, i, j, k);
                        break;
                     case 4:
                        block.setBlockBounds(1.0F - w4, w6, w6, 1.0F, 1.0F - w6, 1.0F - w6);
                        rb.renderStandardBlock(block, i, j, k);
                        break;
                     case 5:
                        block.setBlockBounds(0.0F, w6, w6, w4, 1.0F - w6, 1.0F - w6);
                        rb.renderStandardBlock(block, i, j, k);
                  }
               } else if (visible && (((IConnection)te).getPureVis() + ((IConnection)te).getTaintedVis() > 0.1F || !((IConnection)te).isVisConduit())) {
                  renderConduitVis(w, rb, i, j, k, block, dir, hfill);
               }
            }
         }

         if (block.getRenderBlockPass() != 0) {
            tc.displayPure = 0.0F;
            tc.displayTaint = 0.0F;
         }
      } else {
         block.setBlockBounds(w6, 0.0F, w6, 1.0F - w6, 1.0F, 1.0F - w6);
         DrawFaces(rb, block, 28, false);
         block.setBlockBounds(w4, w4, w4, 1.0F - w4, 1.0F - w4, 1.0F - w4);
         DrawFaces(rb, block, 25, false);
      }

      rb.overrideBlockTexture = -1;
      block.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
      return true;
   }

public static void DrawFaces(RenderBlocks renderblocks, Block block, int i, boolean st) {
      DrawFaces(renderblocks, block, i, i, i, i, i, i, st);
   }

public static void DrawFaces(RenderBlocks renderblocks, Block block, int i1, int i2, int i3, int i4, int i5, int i6, boolean solidtop) {
      Tessellator tessellator = Tessellator.instance;
      GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
      tessellator.startDrawingQuads();
      tessellator.setNormal(0.0F, -1.0F, 0.0F);
      renderblocks.renderBottomFace(block, 0.0, 0.0, 0.0, i1);
      tessellator.draw();
      if (solidtop) {
         GL11.glDisable(3008);
      }

      tessellator.startDrawingQuads();
      tessellator.setNormal(0.0F, 1.0F, 0.0F);
      renderblocks.renderTopFace(block, 0.0, 0.0, 0.0, i2);
      tessellator.draw();
      if (solidtop) {
         GL11.glEnable(3008);
      }

      tessellator.startDrawingQuads();
      tessellator.setNormal(0.0F, 0.0F, 1.0F);
      renderblocks.renderNorthFace(block, 0.0, 0.0, 0.0, i3);
      tessellator.draw();
      tessellator.startDrawingQuads();
      tessellator.setNormal(0.0F, 0.0F, -1.0F);
      renderblocks.renderSouthFace(block, 0.0, 0.0, 0.0, i4);
      tessellator.draw();
      tessellator.startDrawingQuads();
      tessellator.setNormal(1.0F, 0.0F, 0.0F);
      renderblocks.renderEastFace(block, 0.0, 0.0, 0.0, i5);
      tessellator.draw();
      tessellator.startDrawingQuads();
      tessellator.setNormal(-1.0F, 0.0F, 0.0F);
      renderblocks.renderWestFace(block, 0.0, 0.0, 0.0, i6);
      tessellator.draw();
      GL11.glTranslatef(0.5F, 0.5F, 0.5F);
   }

public static void renderItemFromTexture(Minecraft mc, String t, int tile, int texture) {
      renderItemFromTexture(mc, t, tile, texture, 1.0F, 0.0625F, true, 1.0F, 1.0F, 1.0F, 220, 771);
   }

public static void renderItemFromTexture(Minecraft mc, String t, int tile, int texture, float scale, float thickness) {
      renderItemFromTexture(mc, t, tile, texture, scale, thickness, true, 1.0F, 1.0F, 1.0F, 200, 771);
   }

public static void renderItemFromTexture(
      Minecraft mc,
      String t,
      int tile,
      int texture,
      float scale,
      float thickness,
      boolean drawSidesAndBottom,
      float red,
      float green,
      float blue,
      int brightness,
      int blend
   ) {
      GL11.glBindTexture(3553, ModLoader.getMinecraftInstance().renderEngine.getTexture(t));
      int size = getTexSize(t, tile);
      float size16 = size * tile;
      float float_sizeMinus0_01 = size - 0.01F;
      float float_texNudge = 1.0F / ((float)size * size * 2.0F);
      float float_reciprocal = 1.0F / size;
      Tessellator tessellator = Tessellator.instance;
      int i = texture;
      float f = (i % tile * size + 0.0F) / size16;
      float f1 = (i % tile * size + float_sizeMinus0_01) / size16;
      float f2 = (i / tile * size + 0.0F) / size16;
      float f3 = (i / tile * size + float_sizeMinus0_01) / size16;
      float f4 = 1.0F;
      float f5 = 0.0F;
      float f6 = 0.3F;
      GL11.glEnable(32826);
      GL11.glScalef(scale, scale, scale);
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, blend);
      GL11.glColor3f(red, green, blue);
      if (drawSidesAndBottom) {
         tessellator.startDrawingQuads();
         tessellator.setBrightness(brightness);
         tessellator.setNormal(0.0F, 0.0F, 1.0F);
         tessellator.addVertexWithUV(0.0, 0.0, 0.0, f1, f3);
         tessellator.addVertexWithUV(f4, 0.0, 0.0, f, f3);
         tessellator.addVertexWithUV(f4, 1.0, 0.0, f, f2);
         tessellator.addVertexWithUV(0.0, 1.0, 0.0, f1, f2);
         tessellator.draw();
      }

      tessellator.startDrawingQuads();
      tessellator.setBrightness(brightness);
      tessellator.setNormal(0.0F, 0.0F, -1.0F);
      tessellator.addVertexWithUV(0.0, 1.0, 0.0F - thickness, f1, f2);
      tessellator.addVertexWithUV(f4, 1.0, 0.0F - thickness, f, f2);
      tessellator.addVertexWithUV(f4, 0.0, 0.0F - thickness, f, f3);
      tessellator.addVertexWithUV(0.0, 0.0, 0.0F - thickness, f1, f3);
      tessellator.draw();
      if (drawSidesAndBottom) {
         tessellator.startDrawingQuads();
         tessellator.setBrightness(brightness);
         tessellator.setNormal(-1.0F, 0.0F, 0.0F);

         for (int j = 0; j < size; j++) {
            float f9 = (float)j / size;
            float f13 = f1 + (f - f1) * f9 - float_texNudge;
            float f17 = f4 * f9;
            tessellator.addVertexWithUV(f17, 0.0, 0.0F - thickness, f13, f3);
            tessellator.addVertexWithUV(f17, 0.0, 0.0, f13, f3);
            tessellator.addVertexWithUV(f17, 1.0, 0.0, f13, f2);
            tessellator.addVertexWithUV(f17, 1.0, 0.0F - thickness, f13, f2);
         }

         tessellator.draw();
         tessellator.startDrawingQuads();
         tessellator.setBrightness(brightness);
         tessellator.setNormal(1.0F, 0.0F, 0.0F);

         for (int k = 0; k < size; k++) {
            float f10 = (float)k / size;
            float f14 = f1 + (f - f1) * f10 - float_texNudge;
            float f18 = f4 * f10 + float_reciprocal;
            tessellator.addVertexWithUV(f18, 1.0, 0.0F - thickness, f14, f2);
            tessellator.addVertexWithUV(f18, 1.0, 0.0, f14, f2);
            tessellator.addVertexWithUV(f18, 0.0, 0.0, f14, f3);
            tessellator.addVertexWithUV(f18, 0.0, 0.0F - thickness, f14, f3);
         }

         tessellator.draw();
         tessellator.startDrawingQuads();
         tessellator.setBrightness(brightness);
         tessellator.setNormal(0.0F, 1.0F, 0.0F);

         for (int l = 0; l < size; l++) {
            float f11 = (float)l / size;
            float f15 = f3 + (f2 - f3) * f11 - float_texNudge;
            float f19 = f4 * f11 + float_reciprocal;
            tessellator.addVertexWithUV(0.0, f19, 0.0, f1, f15);
            tessellator.addVertexWithUV(f4, f19, 0.0, f, f15);
            tessellator.addVertexWithUV(f4, f19, 0.0F - thickness, f, f15);
            tessellator.addVertexWithUV(0.0, f19, 0.0F - thickness, f1, f15);
         }

         tessellator.draw();
         tessellator.startDrawingQuads();
         tessellator.setBrightness(brightness);
         tessellator.setNormal(0.0F, -1.0F, 0.0F);

         for (int i1 = 0; i1 < size; i1++) {
            float f12 = (float)i1 / size;
            float f16 = f3 + (f2 - f3) * f12 - float_texNudge;
            float f20 = f4 * f12;
            tessellator.addVertexWithUV(f4, f20, 0.0, f, f16);
            tessellator.addVertexWithUV(0.0, f20, 0.0, f1, f16);
            tessellator.addVertexWithUV(0.0, f20, 0.0F - thickness, f1, f16);
            tessellator.addVertexWithUV(f4, f20, 0.0F - thickness, f, f16);
         }

         tessellator.draw();
      }

      GL11.glDisable(3042);
      GL11.glDisable(32826);
      GL11.glPopMatrix();
      GL11.glPopMatrix();
   }
public static int getTexSize(String texture,int divisions) { return LegacyCompat.textureWidth(texture)/divisions; }

}
