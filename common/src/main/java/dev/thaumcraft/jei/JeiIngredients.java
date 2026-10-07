package dev.thaumcraft.jei;

import dev.thaumcraft.Thaumcraft;
import dev.thaumcraft.content.Content;
import dev.thaumcraft.gameplay.GameData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/** Ingredients for JEI infusion recipes and research guides. */
final class JeiIngredients {
    private JeiIngredients() {}

    static List<ItemStack> stacks(String ingredient) {
        if (!ingredient.startsWith("#")) {
            var item = BuiltInRegistries.ITEM.getValue(Identifier.parse(ingredient));
            if (item == null || item == Items.AIR) return List.of();
            return List.of(new ItemStack(item));
        }
        var tag = TagKey.create(Registries.ITEM, Identifier.parse(ingredient.substring(1)));
        var found = new ArrayList<ItemStack>();
        for (var holder : BuiltInRegistries.ITEM.get(tag).stream().flatMap(set -> set.stream()).toList()) {
            found.add(new ItemStack(holder.value()));
            if (found.size() >= 32) break;
        }
        return List.copyOf(found);
    }

    static ItemStack result(GameData.StackDef def) {
        try {
            return def.create();
        } catch (RuntimeException e) {
            Thaumcraft.LOG.warn("Skipping JEI recipe with missing item or invalid components {}", def.id(), e);
            return ItemStack.EMPTY;
        }
    }

    static ItemStack catalyst(boolean dark) {
        return catalyst(dark ? "dark_infuser" : "thaumic_infuser");
    }

    static ItemStack catalyst(String id) {
        var item = Content.ITEMS.get(id);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }
}
