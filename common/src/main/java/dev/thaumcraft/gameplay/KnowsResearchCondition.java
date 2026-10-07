package dev.thaumcraft.gameplay;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.thaumcraft.api.ThaumcraftApi;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import java.util.Set;

/** Use entity=this for commands/advancements and entity=attacking_player for mob drops. */
public record KnowsResearchCondition(Identifier research,LootContext.EntityTarget entity) implements LootItemCondition {
    public static final MapCodec<KnowsResearchCondition> CODEC=RecordCodecBuilder.mapCodec(i->i.group(
            Identifier.CODEC.fieldOf("research").forGetter(KnowsResearchCondition::research),
            LootContext.EntityTarget.CODEC.optionalFieldOf("entity",LootContext.EntityTarget.THIS).forGetter(KnowsResearchCondition::entity)
    ).apply(i,KnowsResearchCondition::new));
    @Override public MapCodec<KnowsResearchCondition> codec(){return CODEC;}
    @Override public Set<ContextKey<?>> getReferencedContextParams(){return Set.of(entity.contextParam());}
    @Override public boolean test(LootContext context){
        return context.getOptionalParameter(entity.contextParam()) instanceof ServerPlayer player
                &&ThaumcraftApi.knows(context.getLevel().getServer(),player.getUUID(),research);
    }
}
