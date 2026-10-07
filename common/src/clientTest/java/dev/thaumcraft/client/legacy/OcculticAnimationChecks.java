package dev.thaumcraft.client.legacy;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import static dev.thaumcraft.client.legacy.LegacyCompat.*;

/** Real nearest-player lookup with fresh animation positions in the current client world. */
public final class OcculticAnimationChecks {
    private static void check(boolean value,String message){if(!value)throw new AssertionError("Occultic animation: "+message);}
    private static float sample(TileEntity tile,BlockPos pos){
        tile.worldObj=new World(Minecraft.getInstance().level,0);tile.xCoord=pos.getX();tile.yCoord=pos.getY();tile.zCoord=pos.getZ();LegacyAnimation.apply(tile);return tile.rota;
    }
    public static void verify(){
        var mc=Minecraft.getInstance();var pos=mc.player.blockPosition();
        check(sample(new TileEnchanterAdvanced(),pos.offset(0,0,-4))>.025f,"Occultic brain tracks player four blocks away");
        check(sample(new TileBrain(),pos.offset(1,0,-4))>.025f,"Standalone brain tracks player four blocks away");
        check(Math.abs(sample(new TileEnchanter(),pos.offset(-1,0,-4))-.008f)<.0001f,"Basic enchanter retains three-block radius");
        check(Math.abs(sample(new TileEnchanterAdvanced(),pos.offset(0,0,-7))-.0004f)<.0001f,"Occultic brain idles beyond five blocks");
        dev.thaumcraft.Thaumcraft.LOG.info("THAUMCRAFT_OCCULTIC_ANIMATION_PASS tracking_radius_neighbors");
    }
}
