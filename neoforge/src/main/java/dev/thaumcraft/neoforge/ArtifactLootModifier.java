package dev.thaumcraft.neoforge;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.thaumcraft.gameplay.ArtifactLoot;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.LootModifier;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;

public final class ArtifactLootModifier extends LootModifier {
    public static final MapCodec<ArtifactLootModifier> CODEC=RecordCodecBuilder.mapCodec(instance->codecStart(instance).apply(instance,ArtifactLootModifier::new));
    public ArtifactLootModifier(LootItemCondition[] conditions,int priority){super(conditions,priority);}
    @Override protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot,LootContext context){ArtifactLoot.add(context.getQueriedLootTableId(),context,loot);return loot;}
    @Override public MapCodec<? extends IGlobalLootModifier> codec(){return CODEC;}
}
