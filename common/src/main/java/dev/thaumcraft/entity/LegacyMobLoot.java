package dev.thaumcraft.entity;

import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

/** Shared integer Looting input for the original mob drop distributions. */
final class LegacyMobLoot {
    private LegacyMobLoot(){}
    static int looting(ServerLevel level,DamageSource source){
        return source.getEntity() instanceof LivingEntity attacker?Math.max(0,EnchantmentHelper.getEnchantmentLevel(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.LOOTING),attacker)):0;
    }
}
