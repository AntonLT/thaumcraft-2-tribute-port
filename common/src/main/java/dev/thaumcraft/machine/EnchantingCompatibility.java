package dev.thaumcraft.machine;

import dev.thaumcraft.content.Content;
import dev.thaumcraft.Thaumcraft;
import net.minecraft.core.Holder;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

/** Shared machine eligibility, including original elemental tool exceptions. */
final class EnchantingCompatibility {
    private EnchantingCompatibility() {}
    static boolean allowed(Holder<Enchantment> enchantment){
        return !enchantment.is(EnchantmentTags.CURSE)&&(enchantment.is(EnchantmentTags.IN_ENCHANTING_TABLE)
                ||enchantment.unwrapKey().map(k->k.identifier().getNamespace().equals(Thaumcraft.MOD_ID)).orElse(false));
    }
    static boolean applies(Holder<Enchantment> enchantment,ItemStack stack){
        return stack.is(Items.BOOK)||enchantment.value().isPrimaryItem(stack)||hasLegacyException(enchantment,stack);
    }
    static boolean hasLegacyException(Holder<Enchantment> enchantment,ItemStack stack){
        var entry=Content.entry(stack);if(entry==null)return false;
        return switch(entry.source_class()){
            case "ItemElementalAxeWater","ItemElementalCutter","ItemVoidCutter" -> enchantment.is(Enchantments.RESPIRATION);
            case "ItemElementalCrusher","ItemVoidCrusher" -> enchantment.is(Enchantments.RESPIRATION)||enchantment.is(Enchantments.AQUA_AFFINITY);
            default -> false;
        };
    }
}
