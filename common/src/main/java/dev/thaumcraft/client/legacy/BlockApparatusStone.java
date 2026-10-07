// Original ThaumCraft 2.1.6d drawing code by Azanor, adapted for the modern renderer.
package dev.thaumcraft.client.legacy;

import static dev.thaumcraft.client.legacy.LegacyCompat.*;
public class BlockApparatusStone extends Block {
public int getBlockTextureFromSide(int i) {
      return 31;
   }

public int getBlockTextureFromSideAndMetadata(int i, int j) {
      if (j == 0) {
         return 46;
      }

      if (j == 4) {
         return 93;
      }

      if (j == 5) {
         return 104;
      }

      if (j == 6) {
         return i < 2 ? 105 : 108;
      }

      if (j == 8) {
         if (i == 0) {
            return 159;
         } else {
            return i == 1 ? 158 : 157;
         }
      } else {
         return super.getBlockTextureFromSideAndMetadata(i, j);
      }
   }

public int getBlockTexture(IBlockAccess iblockaccess, int i, int j, int k, int side) {
      int meta = iblockaccess.getBlockMetadata(i, j, k);
      if (meta == 3) {
         if (side == 0) {
            return 162;
         } else {
            return side == 1 ? 161 : 160;
         }
      } else if (meta == 7) {
         if (side == 0) {
            return 162;
         } else {
            return side == 1 ? 164 : 163;
         }
      } else if (meta == 1) {
         if (side == 0) {
            return 53;
         }

         if (side == 1) {
            return 54;
         }

         TileInfuser ti = (TileInfuser)iblockaccess.getBlockTileEntity(i, j, k);
         HelperLocation loc = new HelperLocation(ti);
         switch (side) {
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

         if (!ti.getConnectable(loc.facing)) {
            return 55;
         }

         TileEntity te = loc.getConnectableTile(iblockaccess);
         return te != null ? 56 : 55;
      } else if (meta == 2) {
         if (side == 0) {
            return 59;
         }

         if (side == 1) {
            return 60;
         }

         TileInfuser ti = (TileInfuser)iblockaccess.getBlockTileEntity(i, j, k);
         HelperLocation loc = new HelperLocation(ti);
         switch (side) {
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

         if (!ti.getConnectable(loc.facing)) {
            return 61;
         }

         TileEntity te = loc.getConnectableTile(iblockaccess);
         return te != null ? 62 : 61;
      } else if (meta == 4) {
         if (side == 0) {
            return 93;
         } else {
            return side == 1 ? 91 : 92;
         }
      } else {
         return super.getBlockTexture(iblockaccess, i, j, k, side);
      }
   }

public boolean renderAppStoneBlock(World w, RenderBlocks rb, int i, int j, int k, Block block, boolean inv, int md) {
      if (md == -9) {
         md = w.getBlockMetadata(i, j, k);
      }

      switch (md) {
         case 1:
            ThaumCraftRenderer.renderBlockInfuser(w, rb, i, j, k, block, md, inv);
            return true;
         case 2:
            ThaumCraftRenderer.renderBlockInfuser(w, rb, i, j, k, block, md, inv);
            return true;
         case 3:
            block.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.6875F, 1.0F);
            if (block.getRenderBlockPass() == 0 && !inv) {
               rb.renderStandardBlock(block, i, j, k);
            } else if (inv) {
               ThaumCraftRenderer.DrawFaces(rb, block, 161, 162, 160, 160, 160, 160, false);
            }

            return true;
         case 4:
            ThaumCraftRenderer.renderBlockQuaesitum(w, rb, i, j, k, block, md, inv);
            return true;
         case 5:
            block.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
            if (block.getRenderBlockPass() == 0 && !inv) {
               rb.renderStandardBlock(block, i, j, k);
            } else if (inv) {
               ThaumCraftRenderer.DrawFaces(rb, block, 104, false);
            }

            return true;
         case 6:
            ThaumCraftRenderer.renderBlockDarkGen(w, rb, i, j, k, block, md, inv);
            return true;
         case 7:
            ThaumCraftRenderer.renderEnchanterAdvanced(w, rb, i, j, k, block, md, inv);
            return true;
         case 8:
            ThaumCraftRenderer.renderWaterUrn(w, rb, i, j, k, block, md, inv);
            return true;
         default:
            return false;
      }
   }
}
