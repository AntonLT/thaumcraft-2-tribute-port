package dev.thaumcraft.api;

import dev.thaumcraft.machine.VisNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import java.util.Optional;
import java.util.function.Function;

/** Vis network access for addon blocks. Methods taking a {@link ServerLevel} must run on the server thread. */
public final class ThaumcraftVis {
    private ThaumcraftVis() {}

    /**
     * Adapts block entities of a type you cannot make implement {@link VisContainer}. The adapter may return null for
     * an entity that should not connect. Register from common initialization on both sides, so conduits draw their
     * attachments on the client. A block entity that implements {@link VisContainer} itself needs no registration.
     * @throws IllegalArgumentException if the type already has an adapter
     */
    public static <T extends BlockEntity> void register(BlockEntityType<T> type,Function<? super T,? extends VisContainer> adapter){VisNetwork.register(type,adapter);}

    /**
     * The container at a position: a Thaumcraft machine, conduit, or tank, or an addon container. Thaumcraft blocks
     * report their vis, capacity, and suction. Only vis buffers accept {@code insert} and {@code extract}: crucibles,
     * condensers, conduits, pumps, and tanks, and only while enabled. Consumer machines refuse both.
     */
    public static Optional<VisContainer> container(ServerLevel level,BlockPos pos){
        serverThread(level);return level.hasChunkAt(pos)?Optional.ofNullable(VisNetwork.container(level,pos)):Optional.empty();
    }

    /**
     * Draws up to {@code amount} of one kind from connected neighbors of the container at {@code pos}, in the order
     * Thaumcraft consumers use. Only sources that allow extraction give vis. Returns the amount removed from them,
     * which the caller must store or spend. Returns 0 when {@code pos} holds no container.
     */
    public static float pull(ServerLevel level,BlockPos pos,float amount,boolean tainted){
        serverThread(level);return VisNetwork.pullInto(level,pos,amount,tainted);
    }

    /** One chunk's aura. {@code max} is the configured cap for both kinds. */
    public record Aura(float vis,float taint,float max) {}
    /** The aura of the chunk containing {@code pos}. Reading an unvisited chunk creates its initial aura. */
    public static Aura aura(ServerLevel level,BlockPos pos){
        serverThread(level);var aura=dev.thaumcraft.gameplay.ArcaneWorldData.get(level).aura(level,pos);
        return new Aura(aura.vis(),aura.taint(),dev.thaumcraft.PortConfig.auraMax);
    }
    /** Removes up to {@code amount} of one kind from the chunk's aura and returns the amount removed. */
    public static float drainAura(ServerLevel level,BlockPos pos,float amount,boolean tainted){
        serverThread(level);return dev.thaumcraft.gameplay.ArcaneWorldData.get(level).drainAura(level,pos,amount,tainted);
    }
    /**
     * Adds up to {@code amount} of one kind to the chunk's aura, as Thaumcraft machines do when they leak or vent,
     * and returns the amount added. The aura is capped at {@link Aura#max}. High aura taint spreads taint blocks.
     */
    public static float addAura(ServerLevel level,BlockPos pos,float amount,boolean tainted){
        serverThread(level);if(!Float.isFinite(amount)||amount<=0)return 0;
        var data=dev.thaumcraft.gameplay.ArcaneWorldData.get(level);var before=data.aura(level,pos);
        data.changeAura(level,pos,tainted?0:amount,tainted?amount:0);var after=data.aura(level,pos);
        return tainted?after.taint()-before.taint():after.vis()-before.vis();
    }

    private static void serverThread(ServerLevel level){if(!level.getServer().isSameThread())throw new IllegalStateException("Thaumcraft vis API requires the server thread");}
}
