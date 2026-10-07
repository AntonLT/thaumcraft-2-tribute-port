package dev.thaumcraft.api;

import dev.thaumcraft.Thaumcraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Loader-neutral notifications. Register from common addon initialization. Listeners run on the thread that
 * caused the change: the server thread for server events, the client thread for client events. A failing
 * listener is logged and does not stop the others.
 *
 * <p>{@link Event}s only notify. {@link VetoEvent}s ask before an action and any listener may deny it.
 * {@link ValueEvent}s let listeners replace a value in turn.
 */
public final class ThaumcraftEvents {
    private ThaumcraftEvents() {}

    public static final class Event<T> {
        private final String name;
        private final List<T> listeners=new CopyOnWriteArrayList<>();
        private Event(String name){this.name=name;}
        public void register(T listener){listeners.add(Objects.requireNonNull(listener));}
        public void unregister(T listener){listeners.remove(listener);}
        /** Called by Thaumcraft. Addons must not post events. */
        public void post(Consumer<T> call){
            for(T listener:listeners)try{call.accept(listener);}
            catch(RuntimeException error){Thaumcraft.LOG.error("Thaumcraft {} listener failed",name,error);}
        }
    }

    /** A listener returning false denies the action and skips later listeners. A failing listener allows it. */
    public static final class VetoEvent<T> {
        private final String name;
        private final List<T> listeners=new CopyOnWriteArrayList<>();
        private VetoEvent(String name){this.name=name;}
        public void register(T listener){listeners.add(Objects.requireNonNull(listener));}
        public void unregister(T listener){listeners.remove(listener);}
        /** Called by Thaumcraft. Addons must not post events. */
        public boolean allows(Predicate<T> call){
            for(T listener:listeners)try{if(!call.test(listener))return false;}
            catch(RuntimeException error){Thaumcraft.LOG.error("Thaumcraft {} listener failed",name,error);}
            return true;
        }
    }

    /**
     * Each listener receives the value returned by the previous one. A failing listener, or one returning a negative
     * or non-finite value, is logged and leaves the value unchanged.
     */
    public static final class ValueEvent<T> {
        @FunctionalInterface public interface Call<T> {float apply(T listener,float value);}
        private final String name;
        private final List<T> listeners=new CopyOnWriteArrayList<>();
        private ValueEvent(String name){this.name=name;}
        public void register(T listener){listeners.add(Objects.requireNonNull(listener));}
        public void unregister(T listener){listeners.remove(listener);}
        /** Called by Thaumcraft. Addons must not post events. */
        public float apply(float value,Call<T> call){
            for(T listener:listeners)try{
                float next=call.apply(listener,value);
                if(Float.isFinite(next)&&next>=0)value=next;
                else Thaumcraft.LOG.error("Thaumcraft {} listener returned invalid value {}",name,next);
            }catch(RuntimeException error){Thaumcraft.LOG.error("Thaumcraft {} listener failed",name,error);}
            return value;
        }
    }

    @FunctionalInterface public interface ResearchChanged {void onChanged(MinecraftServer server,UUID player,Identifier research);}
    @FunctionalInterface public interface CatalogReloaded {void onReloaded(MinecraftServer server,ThaumcraftCatalog catalog);}
    @FunctionalInterface public interface ClientCatalogChanged {void onChanged(ThaumcraftCatalog catalog);}
    @FunctionalInterface public interface ClientResearchChanged {void onChanged(Set<Identifier> known);}
    /** {@code owner} is null for ownerless machines. {@code result} is a copy. */
    @FunctionalInterface public interface InfusionCompleted {void onCompleted(ServerLevel level,BlockPos pos,UUID owner,Identifier recipe,ItemStack result);}
    /**
     * {@code player} is the player who threw the item, else the crucible owner, else null (for example, hopper-fed
     * items in an ownerless crucible). {@code dissolved} is a single-item copy. {@code vis} is the item's full value
     * before the crucible's pure/tainted split.
     */
    @FunctionalInterface public interface CrucibleDissolved {void onDissolved(ServerLevel level,BlockPos pos,UUID player,ItemStack dissolved,float vis);}
    /** {@code discovery} is a copy of the item being read. */
    @FunctionalInterface public interface ResearchLearning {boolean allow(ServerPlayer player,Identifier research,ItemStack discovery);}
    /** {@code owner} is null for ownerless machines. */
    @FunctionalInterface public interface InfusionAllowed {boolean allow(ServerLevel level,BlockPos pos,UUID owner,Identifier recipe);}
    /**
     * Same {@code player} and {@code item} as {@link CrucibleDissolved}. {@code vis} is the current value; return the
     * value to dissolve for, or 0 to reject the item.
     */
    @FunctionalInterface public interface CrucibleDissolving {float onDissolving(ServerLevel level,BlockPos pos,UUID player,ItemStack item,float vis);}
    @FunctionalInterface public interface TaintSpreading {boolean allow(ServerLevel level,BlockPos pos);}
    /** {@code actor} is the player using the item, or the machine's owner, or null for ownerless machines. */
    @FunctionalInterface public interface BlockRemoving {boolean allow(ServerLevel level,BlockPos pos,BlockState state,UUID actor);}

    /** A player gained a project from a discovery or {@link ThaumcraftApi#unlock}. Already persisted and synchronized. */
    public static final Event<ResearchChanged> RESEARCH_UNLOCKED=new Event<>("research unlocked");
    /** A player lost a project through {@link ThaumcraftApi#revoke}. */
    public static final Event<ResearchChanged> RESEARCH_REVOKED=new Event<>("research revoked");
    /** The server accepted a catalog at startup or after a data pack reload. Not posted for a rejected reload. */
    public static final Event<CatalogReloaded> CATALOG_RELOADED=new Event<>("catalog reloaded");
    /** The client received a catalog from the server, or dropped it on disconnect. */
    public static final Event<ClientCatalogChanged> CLIENT_CATALOG_CHANGED=new Event<>("client catalog changed");
    /** The client received the local player's known research. */
    public static final Event<ClientResearchChanged> CLIENT_RESEARCH_CHANGED=new Event<>("client research changed");
    /** A Thaumic or Dark Infuser produced its output and consumed its inputs. */
    public static final Event<InfusionCompleted> INFUSION_COMPLETED=new Event<>("infusion completed");
    /** A crucible dissolved one item into vis. Not posted for rejected items or the Crucible of Souls. */
    public static final Event<CrucibleDissolved> CRUCIBLE_DISSOLVED=new Event<>("crucible dissolved");

    /**
     * A player is reading a discovery for research they do not know. Denying leaves it unlearned and the discovery
     * unopened; tell the player why. {@link ThaumcraftApi#unlock} and the admin command are not checked.
     */
    public static final VetoEvent<ResearchLearning> RESEARCH_LEARNING=new VetoEvent<>("research learning");
    /**
     * A Thaumic or Dark Infuser matched a recipe its owner may craft. Asked every tick the match holds, so keep it
     * cheap. Denying skips the recipe as if its research were locked: the infuser tries later recipes, and work
     * already paid for it is cancelled.
     */
    public static final VetoEvent<InfusionAllowed> INFUSION_ALLOWED=new VetoEvent<>("infusion allowed");
    /**
     * A crucible is about to dissolve an undamaged item with positive vis. Asked again for the same item when a full
     * Thaumium Crucible defers it. A result of 0 bounces the item out like one without vis. Not asked for the Crucible
     * of Souls.
     */
    public static final ValueEvent<CrucibleDissolving> CRUCIBLE_DISSOLVING=new ValueEvent<>("crucible dissolving");
    /**
     * Taint is about to convert the block at {@code pos}, grow a taint plant or spore pod there, or convert a mob
     * standing there. Denying leaves the position unchanged; taint may try other positions. Not asked during world
     * generation.
     */
    public static final VetoEvent<TaintSpreading> TAINT_SPREADING=new VetoEvent<>("taint spreading");
    /**
     * Thaumcraft is about to remove a block without a player mining it: the Arcane Bore mining it, a Portable Hole
     * opening through it, or an Equal Trade wand replacing it. Use it for claims and protected areas. Denying makes the
     * bore skip the block, the Portable Hole fail as it does at unbreakable blocks, and Equal Trade skip the block.
     */
    public static final VetoEvent<BlockRemoving> BLOCK_REMOVING=new VetoEvent<>("block removing");
    /** Called by Thaumcraft before a {@link #BLOCK_REMOVING} change. */
    public static boolean removalAllowed(ServerLevel level,BlockPos pos,BlockState state,UUID actor){
        BlockPos at=pos.immutable();return BLOCK_REMOVING.allows(listener->listener.allow(level,at,state,actor));
    }
}
