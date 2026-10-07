// Original ThaumCraft 2.1.6d drawing code by Azanor, adapted for the modern renderer.
package dev.thaumcraft.client.legacy;

import static dev.thaumcraft.client.legacy.LegacyCompat.*;
public class BlockApparatusMetal extends Block {
public int getBlockTextureFromSide(int i) {
      return 31;
   }

public int getBlockTextureFromSideAndMetadata(int i, int j) {
      if (j == 5) {
         return 144;
      }

      if (j == 8) {
         if (i == 0) {
            return 104;
         } else {
            return i == 1 ? 97 : 103;
         }
      } else if (j == 9) {
         return i <= 1 ? 105 : 106;
      } else if (j == 11) {
         return i <= 1 ? 149 : 148;
      } else {
         return super.getBlockTextureFromSideAndMetadata(i, j);
      }
   }

public int getBlockTexture(IBlockAccess iblockaccess, int i, int j, int k, int side) {
      int meta = iblockaccess.getBlockMetadata(i, j, k);
      if (meta < 3) {
         if (side == 1) {
            return 0 + 16 * meta;
         }

         if (side == 0) {
            return 3 + 16 * meta;
         }

         TileCrucible tc = (TileCrucible)iblockaccess.getBlockTileEntity(i, j, k);
         return meta > 0 && tc != null && tc.isPowering ? 4 + 16 * meta : 5 + 16 * meta;
      } else if (meta == 3) {
         if (side == 1) {
            return 39;
         }

         if (side == 0) {
            return 40;
         }

         TileCrucible tc = (TileCrucible)iblockaccess.getBlockTileEntity(i, j, k);
         return tc != null ? 41 + tc.face : 44;
      } else if (meta == 4) {
         if (side == 1) {
            return 49;
         } else {
            return side == 0 ? 50 : 48;
         }
      } else {
         if (meta == 5) {
            return 144;
         }

         if (meta == 6) {
            if (side == 1) {
               return 130;
            } else {
               return side == 0 ? 129 : 128;
            }
         } else if (meta == 8) {
            if (side == 0) {
               return 104;
            } else {
               return side == 1 ? 97 : 255;
            }
         } else if (meta == 10) {
            return side <= 1 ? 78 : 79;
         } else {
            return super.getBlockTexture(iblockaccess, i, j, k, side);
         }
      }
   }

public boolean renderAppMetalBlock(World w, RenderBlocks rb, int i, int j, int k, Block block, boolean inv, int md) {
      if (md == -9) {
         md = w.getBlockMetadata(i, j, k);
      }

      switch (md) {
         case 0:
         case 1:
         case 2:
         case 3:
            ThaumCraftRenderer.renderBlockCrucible(w, rb, i, j, k, block, md, inv);
            return true;
         case 4:
            ThaumCraftRenderer.renderBlockArcaneFurnace(w, rb, i, j, k, block, md, inv);
            return true;
         case 5:
            ThaumCraftRenderer.renderBlockGenerator(w, rb, i, j, k, block, md, inv);
            return true;
         case 6:
            ThaumCraftRenderer.renderBlockCrystalizer(w, rb, i, j, k, block, md, inv);
            return true;
         case 7:
            ThaumCraftRenderer.renderBlockBore(w, rb, i, j, k, block, md, inv);
            return true;
         case 8:
            block.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
            if (block.getRenderBlockPass() == 0 && !inv) {
               rb.renderStandardBlock(block, i, j, k);
            } else if (inv) {
               ThaumCraftRenderer.DrawFaces(rb, block, 97, 104, 103, 103, 103, 103, false);
            }

            return true;
         case 9:
            ThaumCraftRenderer.renderBlockVoidInterface(w, rb, i, j, k, block, md, inv);
            return true;
         case 10:
            ThaumCraftRenderer.renderBlockTank(w, rb, i, j, k, block, md, inv);
            return true;
         case 11:
            ThaumCraftRenderer.renderBlockSoulBrazier(w, rb, i, j, k, block, md, inv);
            return true;
         default:
            return false;
      }
   }
}
