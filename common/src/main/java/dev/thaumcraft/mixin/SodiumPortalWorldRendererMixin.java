package dev.thaumcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.thaumcraft.client.legacy.PortalViews;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager;
import net.caffeinemc.mods.sodium.client.render.chunk.map.ChunkTracker;
import net.caffeinemc.mods.sodium.client.world.LevelRendererExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets="net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer",remap=false)
public class SodiumPortalWorldRendererMixin {
    @Shadow private RenderSectionManager renderSectionManager;

    @WrapOperation(method="processChunkEvents",at=@At(value="INVOKE",target="Lnet/caffeinemc/mods/sodium/client/render/chunk/map/ChunkTracker;forEachEvent(Lnet/caffeinemc/mods/sodium/client/render/chunk/map/ChunkTracker$ChunkEventHandler;Lnet/caffeinemc/mods/sodium/client/render/chunk/map/ChunkTracker$ChunkEventHandler;)V"))
    private void thaumcraft$shareChunkEvents(ChunkTracker tracker,ChunkTracker.ChunkEventHandler load,ChunkTracker.ChunkEventHandler unload,Operation<Void> original){
        // A nearby portal shares the level, whose event queue is drained only once.
        // Let the main renderer drain it and deliver the same events to both renderers.
        if(PortalViews.sharedWorld())return;
        var near=PortalViews.nearRenderer();
        if(!PortalViews.active()&&near!=null){
            var renderer=((LevelRendererExtension)near).sodium$getWorldRenderer();
            var manager=((SodiumPortalWorldRendererMixin)(Object)renderer).renderSectionManager;
            if(manager!=null){
                manager.beforeSectionUpdates();
                original.call(tracker,(ChunkTracker.ChunkEventHandler)(x,z)->{load.apply(x,z);manager.onChunkAdded(x,z);},(ChunkTracker.ChunkEventHandler)(x,z)->{unload.apply(x,z);manager.onChunkRemoved(x,z);});
                return;
            }
        }
        original.call(tracker,load,unload);
    }
}
