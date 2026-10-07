// Original ThaumCraft 2.1.6d drawing code by Azanor, adapted for the modern renderer.
package dev.thaumcraft.client.legacy;

import static dev.thaumcraft.client.legacy.LegacyCompat.*;
public class BlockApparatusFragile extends Block {
public int getBlockTextureFromSide(int i) {
      return 15;
   }

public int getBlockTexture(IBlockAccess iblockaccess, int i, int j, int k, int l) {
      int md = iblockaccess.getBlockMetadata(i, j, k);
      if (md == 1) {
         if (l <= 1) {
            return 66;
         }

         TileConduit tf = (TileFilter)iblockaccess.getBlockTileEntity(i, j, k);
         HelperLocation loc = new HelperLocation(tf);
         switch (l) {
            case 2:
               loc.facing = HelperFacing.NEGZ;
               break;
            case 3:
               loc.facing = HelperFacing.POSZ;
               break;
            case 4:
               loc.facing = HelperFacing.NEGX;
               break;
            case 5:
               loc.facing = HelperFacing.POSX;
         }

         if (!tf.getConnectable(loc.facing)) {
            return 65;
         }

         TileEntity te = loc.getConnectableTile(iblockaccess);
         return te != null ? 64 : 65;
      } else {
         if (md == 3) {
            return l <= 1 ? 26 : 27;
         }

         if (md == 4) {
            return l <= 1 ? 168 : 167;
         }

         if (md == 6) {
            TileEntity te = iblockaccess.getBlockTileEntity(i, j, k);
            if (te != null && te instanceof TilePurifier) {
               TilePurifier tp = (TilePurifier)te;
               if (tp.orientation != 0 && tp.orientation != 2) {
                  return l > 3 ? 116 : 117;
               } else if (l <= 1) {
                  return 118;
               } else {
                  return l <= 3 ? 116 : 117;
               }
            } else {
               return 116;
            }
         } else {
            return 15;
         }
      }
   }

public boolean renderAppFragileBlock(World w, RenderBlocks renderblocks, int i, int j, int k, Block block, boolean inv, int md) {
      if (md == -9) {
         md = w.getBlockMetadata(i, j, k);
      }

      if (md == 0) {
         return ThaumCraftRenderer.renderBlockConduit(w, renderblocks, i, j, k, block, md, inv);
      } else if (md == 1) {
         return ThaumCraftRenderer.renderBlockFilter(w, renderblocks, i, j, k, block, md, inv);
      } else if (md == 2) {
         return ThaumCraftRenderer.renderBlockBellows(w, renderblocks, i, j, k, block, md, inv);
      } else if (md == 3) {
         return ThaumCraftRenderer.renderBlockTank(w, renderblocks, i, j, k, block, md, inv);
      } else if (md == 4) {
         return ThaumCraftRenderer.renderBlockBrain(w, renderblocks, i, j, k, block, md, inv);
      } else if (md == 5) {
         return ThaumCraftRenderer.renderBlockValve(w, renderblocks, i, j, k, block, md, inv);
      } else if (md == 6) {
         return ThaumCraftRenderer.renderBlockPurifier(w, renderblocks, i, j, k, block, md, inv);
      } else if (md == 7) {
         return ThaumCraftRenderer.renderTrunk(w, renderblocks, i, j, k, block, md, inv);
      } else if (md == 8) {
         return ThaumCraftRenderer.renderBlockValveAdvanced(w, renderblocks, i, j, k, block, md, inv);
      } else {
         return md == 9 ? ThaumCraftRenderer.renderBlockPump(w, renderblocks, i, j, k, block, md, inv) : false;
      }
   }
}
