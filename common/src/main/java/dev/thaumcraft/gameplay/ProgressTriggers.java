package dev.thaumcraft.gameplay;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.criterion.ContextAwarePredicate;
import net.minecraft.advancements.criterion.EntityPredicate;
import net.minecraft.advancements.criterion.SimpleCriterionTrigger;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import java.util.Optional;

/** Both triggers use the normal player predicate and an optional namespaced ID filter. */
public final class ProgressTriggers {
    private ProgressTriggers() {}
    public static final IdTrigger RESEARCH_LEARNED=new IdTrigger("research");
    public static final IdTrigger INFUSION_COMPLETED=new IdTrigger("recipe");
    public static final class IdTrigger extends SimpleCriterionTrigger<Instance> {
        private final Codec<Instance> codec;
        private IdTrigger(String field){codec=RecordCodecBuilder.create(i->i.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Instance::player),
                Identifier.CODEC.optionalFieldOf(field).forGetter(Instance::id)).apply(i,Instance::new));}
        @Override public Codec<Instance> codec(){return codec;}
        public void trigger(ServerPlayer player,Identifier id){trigger(player,instance->instance.matches(id));}
    }
    public record Instance(Optional<ContextAwarePredicate> player,Optional<Identifier> id) implements SimpleCriterionTrigger.SimpleInstance {
        public boolean matches(Identifier value){return id.isEmpty()||id.get().equals(value);}
    }
}
