package dev.thaumcraft.client.legacy;

import net.caffeinemc.mods.sodium.client.render.chunk.map.ChunkStatus;
import net.caffeinemc.mods.sodium.client.render.chunk.map.ChunkTrackerHolder;
import net.minecraft.client.multiplayer.ClientLevel;

/** Optional integration for destination snapshots that bypass the normal packet listener. */
public final class SodiumPortals {
    private static final boolean PRESENT=present();
    private SodiumPortals() {}
    private static boolean present(){
        try{Class.forName("net.caffeinemc.mods.sodium.client.world.LevelRendererExtension",false,SodiumPortals.class.getClassLoader());return true;}
        catch(ClassNotFoundException e){return false;}
    }
    public static void lightReady(ClientLevel level,int x,int z){
        if(PRESENT)ChunkTrackerHolder.get(level).onChunkStatusAdded(x,z,ChunkStatus.FLAG_HAS_LIGHT_DATA);
    }
}
