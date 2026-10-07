// Original ThaumCraft 2.1.6d drawing code by Azanor, adapted for the modern renderer.
package dev.thaumcraft.client.legacy;

import static dev.thaumcraft.client.legacy.LegacyCompat.*;
public class BlockApparatusWood extends Block {
public int getBlockTextureFromSide(int i) {
      return 15;
   }

public int getBlockTextureFromSideAndMetadata(int i, int j) {
      switch (j) {
         case 3:
            return 127;
         case 4:
            return 143;
         default:
            return super.getBlockTextureFromSideAndMetadata(i, j);
      }
   }

public int getBlockTexture(IBlockAccess iblockaccess, int i, int j, int k, int l) {
      int md = iblockaccess.getBlockMetadata(i, j, k);
      if (md == 0) {
         return l <= 1 ? 113 : 112;
      }

      if (md == 1) {
         if (l <= 1) {
            return 70;
         }

         TileEntity te = iblockaccess.getBlockTileEntity(i, j, k);
         if (te != null && te instanceof TileDuplicator) {
            if (((TileDuplicator)te).orientation == 0 && l == 2) {
               return 71;
            }

            if (((TileDuplicator)te).orientation == 1 && l == 5) {
               return 71;
            }

            if (((TileDuplicator)te).orientation == 2 && l == 3) {
               return 71;
            }

            if (((TileDuplicator)te).orientation == 3 && l == 4) {
               return 71;
            }
         }

         return 72;
      } else if (md == 2) {
         return l <= 1 ? 86 : 87;
      } else if (md == 3) {
         return l <= 1 ? 127 : 121 + Math.abs((i + j + k) % 6);
      } else if (md == 4) {
         return l <= 1 ? 143 : 137 + Math.abs((i + j + k) % 6);
      } else {
         return 15;
      }
   }

public boolean renderAppWoodBlock(World w, RenderBlocks renderblocks, int i, int j, int k, Block block, boolean inv, int md) {
      if (md == -9) {
         md = w.getBlockMetadata(i, j, k);
      }

      if (md == 0) {
         return ThaumCraftRenderer.renderBlockCondenser(w, renderblocks, i, j, k, block, md, inv);
      } else if (md == 1) {
         return ThaumCraftRenderer.renderBlockDuplicator(w, renderblocks, i, j, k, block, md, inv);
      } else if (md == 2) {
         return ThaumCraftRenderer.renderBlockRepairer(w, renderblocks, i, j, k, block, md, inv);
      } else if (md == 3) {
         return ThaumCraftRenderer.renderBlockTotem(w, renderblocks, i, j, k, block, md, inv);
      } else {
         return md == 4 ? ThaumCraftRenderer.renderBlockTotem(w, renderblocks, i, j, k, block, md, inv) : false;
      }
   }
}
