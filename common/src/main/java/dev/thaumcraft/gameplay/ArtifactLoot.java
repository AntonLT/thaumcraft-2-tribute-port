package dev.thaumcraft.gameplay;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import java.util.List;

public final class ArtifactLoot {
    private ArtifactLoot() {}
    public static void add(Identifier table,LootContext context,List<ItemStack> loot) {
        smeltMinedDrops(context,loot);
        if(table.getPath().startsWith("entities/"))ArcaneEnchantments.loot(context,loot);

    }
    /** Original GenerateTreasure: each empty chest slot has a one-third weighted loot chance. */
    public static void fillOriginalTreasure(net.minecraft.world.Container chest,net.minecraft.util.RandomSource random){fill("thaumcraft2tp:chest",chest,random);}
    /** Original eldritch void chests: each empty slot has a one-sixth weighted loot chance. */
    public static void fillOriginalEldritch(net.minecraft.world.Container chest,net.minecraft.util.RandomSource random){fill("thaumcraft2tp:eldritch",chest,random);}
    /**
     * Builds the pool's weighted list, drawing random picks and counts as it goes, then gives each empty slot one roll
     * over {@code fillOneIn} times the list size. The built-in pools reproduce the original lists and random sequence.
     */
    public static void fill(String pool,net.minecraft.world.Container chest,net.minecraft.util.RandomSource random) {
        var definition=AddonData.current().treasure().get(pool);if(definition==null)return;
        var choices=new java.util.ArrayList<ItemStack>();
        for(var entry:definition.entries())for(int repeat=0;repeat<entry.repeat();repeat++)for(var item:entry.items())choices.add(item.create(random));
        if(choices.isEmpty())return;
        for(int slot=0;slot<chest.getContainerSize();slot++)if(chest.getItem(slot).isEmpty()) {
            int roll=random.nextInt(choices.size()*definition.fillOneIn());if(roll<choices.size())chest.setItem(slot,choices.get(roll).copy());
        }
    }
    private static void smeltMinedDrops(LootContext context,List<ItemStack> loot) {
        var tool=context.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.TOOL);
        var breaker=context.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY);
        if(tool==null||!(breaker instanceof net.minecraft.server.level.ServerPlayer player)||!dev.thaumcraft.item.ElementalTools.specialEnabled(player))return;
        if(!(tool.typeHolder().value() instanceof dev.thaumcraft.item.ArcanaItem arcana)||!arcana.entry().source_class().equals("ItemElementalPickFire"))return;
        var level=context.getLevel();var replacement=new java.util.ArrayList<ItemStack>();
        for(var drop:loot) {
            var input=new net.minecraft.world.item.crafting.SingleRecipeInput(drop);
            var recipe=level.getServer().getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.SMELTING,input,level);
            if(recipe.isEmpty())return;
            ItemStack result=recipe.get().value().assemble(input);result.setCount(result.getCount()*drop.getCount());replacement.add(result);
        }
        loot.clear();loot.addAll(replacement);
        if(!replacement.isEmpty()){
            var origin=context.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN);
            if(origin!=null)dev.thaumcraft.network.EquipmentEffect.send(level,dev.thaumcraft.network.EquipmentEffect.SMELT,origin.add(-.5,-.5,-.5));
        }
    }
}
