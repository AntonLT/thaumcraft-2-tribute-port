package dev.alts.tc2tppatches;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.function.Consumer;

/** Client mirror of the local player's learned vis values. Loader tooltip hooks call {@link #appendTooltip}. */
public final class ClientVisKnowledge {
    private static volatile Map<Identifier,Float> values=Map.of();
    private ClientVisKnowledge() {}

    public static void receive(VisKnowledgeSync payload){values=payload.values();}
    public static void clear(){values=Map.of();}

    public static void appendTooltip(ItemStack stack,Consumer<Component> lines) {
        if(stack.isEmpty())return;
        Float vis=values.get(BuiltInRegistries.ITEM.getKey(stack.getItem()));
        // Worn equipment cannot be dissolved, so don't claim it has the pristine value.
        if(vis==null||stack.isDamaged())return;
        lines.accept(Component.translatable("tooltip.alts_tc2tp_patches.vis",VisKnowledgeSync.format(vis)).withStyle(ChatFormatting.DARK_PURPLE));
    }
}
