package dev.thaumcraft.test;

import dev.thaumcraft.client.legacy.PortalViews;
import net.caffeinemc.mods.sodium.client.render.chunk.map.ChunkStatus;
import net.caffeinemc.mods.sodium.client.render.chunk.map.ChunkTrackerHolder;
import net.minecraft.client.Minecraft;

/** The two renderers must both receive events from the shared world's single Sodium queue. */
final class SodiumPortalChecks {
    private static boolean present(){
        try{Class.forName("net.caffeinemc.mods.sodium.client.render.chunk.map.ChunkTrackerHolder");return true;}
        catch(ClassNotFoundException e){return false;}
    }
    private static final boolean PRESENT=present();
    static void tick(int tick){
        if(!PRESENT||tick!=100&&tick!=120&&tick!=180)return;
        var mc=Minecraft.getInstance();var near=PortalViews.nearRenderer();
        if(near==null)throw new AssertionError("Sodium nearby portal renderer was not created");
        var marker=PortalSceneChecks.SOURCE;int x=marker.getX()>>4,z=marker.getZ()>>4;
        var tracker=ChunkTrackerHolder.get(mc.level);
        if(tick==100){tracker.onChunkStatusRemoved(x,z,ChunkStatus.FLAG_ALL);return;}
        if(tick==120){
            if(mc.levelRenderer.isSectionCompiledAndVisible(marker)||near.isSectionCompiledAndVisible(marker))
                throw new AssertionError("Sodium chunk removal must reach both main and portal renderers");
            tracker.onChunkStatusAdded(x,z,ChunkStatus.FLAG_ALL);return;
        }
        if(!mc.levelRenderer.isSectionCompiledAndVisible(marker)||!near.isSectionCompiledAndVisible(marker))
            throw new AssertionError("Sodium chunk addition must reach both main and portal renderers");
        dev.thaumcraft.Thaumcraft.LOG.info("THAUMCRAFT_SODIUM_SHARED_CHUNKS_PASS");
    }
}
