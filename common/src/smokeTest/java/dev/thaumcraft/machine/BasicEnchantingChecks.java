package dev.thaumcraft.machine;

import dev.thaumcraft.content.Content;
import dev.thaumcraft.Thaumcraft;
import dev.thaumcraft.gameplay.ArcaneWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantable;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import java.util.List;
import java.util.UUID;

/** Exercises the Thaumic Enchanter with the real registry and machine state. */
public final class BasicEnchantingChecks {
    private static int checks;
    private static void check(boolean condition,String message){checks++;if(!condition)throw new AssertionError("Basic enchanting: "+message);}
    private static final class Rolls extends LegacyRandomSource {
        private final boolean maximumBonus;
        private final float variation;
        private final int extraRoll;
        Rolls(boolean maximumBonus,float variation,int extraRoll){super(0);this.maximumBonus=maximumBonus;this.variation=variation;this.extraRoll=extraRoll;}
        @Override public int nextInt(int bound){return bound==50?extraRoll:maximumBonus?bound-1:0;}
        @Override public float nextFloat(){return variation;}
    }

    public static int run(MinecraftServer server){
        checks=0;
        var level=server.overworld();var pos=new BlockPos(240,280,240);
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)for(int y=0;y<=1;y++)
            level.setBlockAndUpdate(pos.offset(x,y,z),Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos,Content.block("thaumic_enchanter").defaultBlockState());
        var machine=(MachineBlockEntity)level.getBlockEntity(pos);
        int shelves=0;
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)if(Math.abs(x)==2||Math.abs(z)==2){
            if(shelves++<15)level.setBlockAndUpdate(pos.offset(x,0,z),Blocks.BOOKSHELF.defaultBlockState());
        }
        check(EnchantingBoosters.power(machine)==15,"Fixture supplies fifteen bookshelf-equivalent power");
        machine.setItem(0,new ItemStack(Items.DIAMOND_SWORD));machine.basicEnchanting.tick(level);
        check(machine.basicEnchanting.offer(2)>=5&&machine.basicEnchanting.offer(2)<=16,"Fifteen bookshelves use the scaled original offer progression");
        check(BasicEnchanting.rollOffer(new Rolls(true,.5f,49),2,40)==65,"Maximum original roll is sixty-five");
        check(BasicEnchanting.rollOffer(new Rolls(false,.5f,49),2,40)==21,"Maximum boosters retain the original minimum third offer");
        check(BasicEnchanting.rollOffer(new Rolls(true,.5f,49),2,80)==65,"Booster strength still caps at forty");
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)if(Math.abs(x)==2||Math.abs(z)==2)
            level.setBlockAndUpdate(pos.offset(x,1,z),Content.block("brain_in_a_jar").defaultBlockState());
        for(int sample=0;sample<32;sample++){
            machine.setItem(0,ItemStack.EMPTY);machine.basicEnchanting.tick(level);
            machine.setItem(0,new ItemStack(Items.DIAMOND_SWORD));machine.basicEnchanting.tick(level);
            for(int row=0;row<3;row++)check(machine.basicEnchanting.offer(row)>0&&machine.basicEnchanting.offer(row)<=39,"Excess boosters never exceed thirty-nine");
            check(machine.basicEnchanting.offer(2)>=13,"Maximum boosters retain scaled original third-offer range");
        }
        var owner=UUID.fromString("faab2b9b-f670-44d8-9a35-8502c21e0146");machine.setOwner(owner);
        var registry=level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var repair=registry.getOrThrow(ResourceKey.create(Registries.ENCHANTMENT,Thaumcraft.id("repair")));
        var efficiency=registry.getOrThrow(Enchantments.EFFICIENCY);
        var unbreaking=registry.getOrThrow(Enchantments.UNBREAKING);
        check(!machine.basicEnchanting.enchantments().contains(repair),"Unknown research excludes Self Repair");
        ArcaneWorldData.researchData(level).unlock(owner,59);
        var pool=machine.basicEnchanting.enchantments();
        check(pool.contains(repair)&&pool.contains(efficiency),"Pool includes researched Thaumcraft and modern table enchantments");
        check(!pool.contains(registry.getOrThrow(Enchantments.MENDING))&&!pool.contains(registry.getOrThrow(Enchantments.SOUL_SPEED))
                &&!pool.contains(registry.getOrThrow(Enchantments.SWIFT_SNEAK))&&pool.stream().noneMatch(h->h.is(EnchantmentTags.CURSE)),"Basic pool excludes treasure enchantments and curses");
        var book=new ItemStack(Items.BOOK);book.set(DataComponents.ENCHANTABLE,new Enchantable(20));
        var result=BasicEnchanting.select(new Rolls(false,.5f,49),book,30,List.of(efficiency));
        check(result.size()==1&&result.getFirst().level()==3,"Original adjusted power 31 gives Efficiency III rather than the modern fourth tier");
        result=BasicEnchanting.select(new Rolls(true,.5f,49),book,50,List.of(efficiency));
        check(result.size()==1&&result.getFirst().level()==5,"Original half-enchantability bonus can reach Efficiency V");
        result=BasicEnchanting.select(new Rolls(true,0,49),book,50,List.of(efficiency));
        check(result.size()==1&&result.getFirst().level()==4,"Original negative twenty-five percent variation lowers the resulting tier");
        book.set(DataComponents.ENCHANTABLE,new Enchantable(1));
        result=BasicEnchanting.select(new Rolls(false,.5f,34),book,65,List.of(efficiency,unbreaking));
        check(result.size()==1,"At original adjusted power 66, a roll of 34 rejects a second enchantment");
        result=BasicEnchanting.select(new Rolls(false,.5f,33),book,65,List.of(efficiency,unbreaking));
        check(result.size()==2,"At original adjusted power 66, a roll of 33 accepts a second enchantment");
        result=BasicEnchanting.select(new Rolls(false,.5f,0),book,50,List.of(repair,unbreaking));
        check(result.size()==1&&result.getFirst().enchantment().equals(repair),"Original Self Repair threshold is reachable and still excludes Unbreaking");
        var potency=registry.getOrThrow(ResourceKey.create(Registries.ENCHANTMENT,Thaumcraft.id("potency")));
        book.set(DataComponents.ENCHANTABLE,new Enchantable(20));
        result=BasicEnchanting.select(new Rolls(true,.5f,49),book,65,List.of(potency));
        check(result.size()==1&&result.getFirst().level()==5,"Original offer sixty-five keeps Potency V reachable");
        book.set(DataComponents.ENCHANTABLE,new Enchantable(5));
        result=BasicEnchanting.select(new Rolls(true,.5f,49),book,36,List.of(potency));
        check(result.size()==1&&result.getFirst().level()==2,"Raw offer thirty-six preserves original Potency II rather than scaled Potency III");
        check(potency.value().getMinCost(5)==75,"Basic enchanting does not change thresholds used by the Occultic Enchanter");
        book.set(DataComponents.ENCHANTABLE,new Enchantable(1));
        record TierCase(ResourceKey<net.minecraft.world.item.enchantment.Enchantment> key,int offer,int tier) {}
        for(var test:List.of(
                new TierCase(Enchantments.EFFICIENCY,60,5),new TierCase(Enchantments.EFFICIENCY,100,5),new TierCase(Enchantments.EFFICIENCY,101,0),
                new TierCase(Enchantments.SHARPNESS,63,4),new TierCase(Enchantments.SHARPNESS,64,5),new TierCase(Enchantments.SHARPNESS,85,0),
                new TierCase(Enchantments.PROTECTION,33,3),new TierCase(Enchantments.UNBREAKING,23,2),new TierCase(Enchantments.UNBREAKING,81,0),
                new TierCase(Enchantments.SILK_TOUCH,23,0),new TierCase(Enchantments.SILK_TOUCH,24,1),new TierCase(Enchantments.SILK_TOUCH,61,0),
                new TierCase(Enchantments.LOOTING,18,0),new TierCase(Enchantments.LOOTING,43,3),new TierCase(Enchantments.FORTUNE,81,0))){
            result=BasicEnchanting.select(new Rolls(false,.5f,49),book,test.offer(),List.of(registry.getOrThrow(test.key())));
            check(test.tier()==0?result.isEmpty():result.size()==1&&result.getFirst().level()==test.tier(),"Original tier boundary for "+test.key().identifier()+" at raw offer "+test.offer());
        }
        check(efficiency.value().getMinCost(5)==41,"Basic original thresholds leave the modern registry unchanged");
        var respiration=registry.getOrThrow(Enchantments.RESPIRATION);
        for(String cls:List.of("ItemElementalAxeWater","ItemElementalCutter","ItemVoidCutter","ItemElementalCrusher","ItemVoidCrusher")){
            var entry=Content.ENTRIES.stream().filter(e->e.source_class().equals(cls)).findFirst().orElseThrow();
            var tool=new ItemStack(Content.item(entry.id()));
            result=BasicEnchanting.select(new Rolls(false,.5f,49),tool,20,List.of(respiration));
            check(result.size()==1,"Original Respiration exception survives for "+cls);
        }
        check(BasicEnchanting.select(new Rolls(false,.5f,49),new ItemStack(Items.DIAMOND_SWORD),20,List.of(respiration)).isEmpty(),"Respiration exception does not leak onto ordinary weapons");
        machine.setItem(0,new ItemStack(Items.DIAMOND_PICKAXE));machine.basicEnchanting.tick(level);
        var idle=machine.saveWithFullMetadata(level.registryAccess());idle.remove("basic_enchant_version");
        int[][] prices={{1,1,0},{2,1,4},{3,2,6},{49,29,2352},{50,30,2500},{64,38,4096},{65,39,4160}};
        for(var price:prices){
            idle.putInt("basic_enchant_offer2",price[0]);
            var quote=(MachineBlockEntity)BlockEntity.loadStatic(pos,machine.getBlockState(),idle,level.registryAccess());
            check(quote.basicEnchanting.offer(2)==price[1]&&quote.basicEnchanting.cost(2)==price[2],"Original price retained for roll "+price[0]);
            check(quote.data.get(34)==price[1]&&quote.data.get(42)==price[2],"Menu receives exact power and price for roll "+price[0]);
        }
        idle.putInt("basic_enchant_offer2",65);
        var oldIdle=(MachineBlockEntity)BlockEntity.loadStatic(pos,machine.getBlockState(),idle,level.registryAccess());
        oldIdle.setLevel(level);oldIdle.basicEnchanting.tick(level);
        check(oldIdle.basicEnchanting.offer(2)==39&&oldIdle.basicEnchanting.selected()==-1,"Original idle offer sixty-five becomes thirty-nine without rerolling its price");
        idle.putInt("basic_enchant_version",1);idle.putInt("basic_enchant_offer2",30);
        var interimIdle=(MachineBlockEntity)BlockEntity.loadStatic(pos,machine.getBlockState(),idle,level.registryAccess());
        check(interimIdle.basicEnchanting.offer(2)==0,"Interim idle offers invalidate their thirty-level distribution");
        interimIdle.setLevel(level);interimIdle.basicEnchanting.tick(level);
        check(interimIdle.basicEnchanting.offer(2)>=13&&interimIdle.basicEnchanting.offer(2)<=39,"Interim idle offers reroll on the scaled original progression");
        machine=oldIdle;level.setBlockEntity(machine);
        ArcaneWorldData.get(level).drainVibes(level,pos,100,true);
        check(machine.basicEnchanting.click(2)&&machine.workRequired==4160,"Level thirty-nine retains the original maximum price");
        machine.insertVis(10,false);machine.basicEnchanting.tick(level);
        var saved=machine.saveWithFullMetadata(level.registryAccess());
        var restored=(MachineBlockEntity)BlockEntity.loadStatic(pos,machine.getBlockState(),saved,level.registryAccess());
        check(restored!=null&&restored.basicEnchanting.selected()==2&&restored.progress==10&&restored.workRequired==4160
                &&restored.basicEnchanting.offer(2)==39&&restored.data.get(42)==4160,"Selected offer and exact payment survive saving");
        saved.remove("basic_enchant_version");saved.putInt("basic_enchant_offer2",65);saved.putInt("work_required",4160);
        var oldActive=(MachineBlockEntity)BlockEntity.loadStatic(pos,machine.getBlockState(),saved,level.registryAccess());oldActive.setLevel(level);oldActive.basicEnchanting.tick(level);
        check(oldActive.basicEnchanting.offer(2)==39&&oldActive.basicEnchanting.selected()==2&&oldActive.progress==10&&oldActive.workRequired==4160,"Old active job keeps its quoted cost and paid progress");
        saved.putInt("basic_enchant_version",1);
        var inheritedActive=(MachineBlockEntity)BlockEntity.loadStatic(pos,machine.getBlockState(),saved,level.registryAccess());
        check(inheritedActive.basicEnchanting.offer(2)==39&&inheritedActive.basicEnchanting.cost(2)==4160,"Original active job resaved by the interim port keeps its maximum power and quote");
        saved.putInt("basic_enchant_version",1);saved.putInt("basic_enchant_offer2",30);saved.putInt("work_required",900);
        var interimActive=(MachineBlockEntity)BlockEntity.loadStatic(pos,machine.getBlockState(),saved,level.registryAccess());interimActive.setLevel(level);interimActive.basicEnchanting.tick(level);
        check(interimActive.basicEnchanting.offer(2)==30&&interimActive.progress==10&&interimActive.basicEnchanting.cost(2)==900&&interimActive.data.get(42)==900,"Interim active job retains its previously quoted nine-hundred-vis price");
        var resaved=(MachineBlockEntity)BlockEntity.loadStatic(pos,machine.getBlockState(),interimActive.saveWithFullMetadata(level.registryAccess()),level.registryAccess());
        check(resaved.basicEnchanting.offer(2)==30&&resaved.basicEnchanting.cost(2)==900&&resaved.progress==10,"Migrated active quote survives a second save");
        for(int tick=0;tick<415;tick++){machine.insertVis(10,false);machine.basicEnchanting.tick(level);}
        var expected=BasicEnchanting.select(RandomSource.create(0),machine.getItem(0),65,pool);
        check(!expected.isEmpty(),"Completion reference roll has an enchantment");
        level.getRandom().setSeed(0);
        machine.basicEnchanting.tick(level);
        check(machine.getItem(0).isEnchanted()&&machine.basicEnchanting.selected()==-1,"Charged job finishes in its original slot");
        var actual=EnchantmentHelper.getEnchantmentsForCrafting(machine.getItem(0));
        check(actual.keySet().size()==expected.size()&&expected.stream().allMatch(e->actual.getLevel(e.enchantment())==e.level()),"Displayed thirty-nine completes with the same enchantments as the original sixty-five roll");
        check(machine.pureVis()==0,"Job consumes exactly its displayed four thousand one hundred sixty vis");
        check(ArcaneWorldData.get(level).aura(level,pos).badVibes()==32,"Maximum enchant preserves the intended original thirty-two bad vibes");
        check(EnchantmentHelper.getEnchantmentsForCrafting(machine.getItem(0)).keySet().stream().allMatch(pool::contains),"Actual machine result only uses the allowed pool");
        machine.setItem(0,new ItemStack(Items.BOOK));machine.basicEnchanting.tick(level);machine.basicEnchanting.click(2);
        machine.progress=machine.workRequired;machine.basicEnchanting.tick(level);
        check(machine.getItem(0).is(Items.ENCHANTED_BOOK)&&!EnchantmentHelper.getEnchantmentsForCrafting(machine.getItem(0)).isEmpty(),"A single book becomes an enchanted book in place");
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)for(int y=0;y<=1;y++)
            level.setBlockAndUpdate(pos.offset(x,y,z),Blocks.AIR.defaultBlockState());
        return checks;
    }
}
