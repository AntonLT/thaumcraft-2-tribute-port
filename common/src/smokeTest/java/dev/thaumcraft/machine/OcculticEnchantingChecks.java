package dev.thaumcraft.machine;

import dev.thaumcraft.Thaumcraft;
import dev.thaumcraft.content.Content;
import dev.thaumcraft.gameplay.ArcaneWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import java.util.List;

/** Real registry, public candidate-window selection, persistence and exact accepted-job payments. */
public final class OcculticEnchantingChecks {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError("Occultic enchanting: "+message);}
    private static final BlockPos POS=new BlockPos(272,280,272);
    private static ItemStack tool(String cls){return new ItemStack(Content.item(Content.ENTRIES.stream().filter(e->e.source_class().equals(cls)).findFirst().orElseThrow().id()));}
    private static ItemStack upgrade(int meta){return new ItemStack(Content.item(Content.ENTRIES.stream().filter(e->e.source_class().equals("ItemUpgrades")&&e.meta()==meta).findFirst().orElseThrow().id()));}
    private static void surroundings(ServerLevel level,boolean boosted){
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)for(int y=0;y<2;y++)if(x!=0||z!=0||y!=0)
            level.setBlockAndUpdate(POS.offset(x,y,z),boosted&&(Math.abs(x)==2||Math.abs(z)==2)?Content.block("brain_in_a_jar").defaultBlockState():Blocks.AIR.defaultBlockState());
    }
    private static void input(MachineBlockEntity machine,ItemStack stack){machine.enchanting.reset();machine.setItem(0,stack);}
    private static int candidate(MachineBlockEntity machine,ResourceKey<Enchantment> key,int rank){
        var enchanting=machine.enchanting;var ids=machine.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).asHolderIdMap();
        int total=enchanting.data(14);
        for(int page=0;page<=total/10;page++){
            for(int slot=0;slot<40;slot++){int id=enchanting.data(16+2*slot);if(id>0&&ids.byId(id-1).is(key)&&enchanting.data(17+2*slot)==rank)return slot;}
            int previous=enchanting.data(96);enchanting.click(1);if(enchanting.data(96)==previous)break;
        }
        return -1;
    }
    private static void select(MachineBlockEntity machine,ResourceKey<Enchantment> key,int rank){int slot=candidate(machine,key,rank);check(slot>=0,"Candidate "+key.identifier()+" rank "+rank);check(machine.enchanting.click(100+slot),"Select candidate through visible window");}
    private static void finish(MachineBlockEntity machine,int quote,int paid){
        for(int remaining=quote-paid;remaining>0;){int amount=Math.min(10,remaining);machine.insertVis(amount,false);machine.enchanting.tick();remaining-=amount;}
        check(machine.enchanting.data(13)==0&&!EnchantmentHelper.getEnchantmentsForCrafting(machine.getItem(0)).isEmpty(),"Accepted job completes");
        check(machine.pureVis()==0,"Exact accepted total consumed");var result=machine.getItem(0).copy();machine.enchanting.tick();check(ItemStack.matches(result,machine.getItem(0)),"No duplicate completion");
    }
    public static int run(MinecraftServer server){
        checks=0;var level=server.overworld();surroundings(level,true);level.setBlockAndUpdate(POS,Content.block("occultic_enchanter").defaultBlockState());
        var machine=(MachineBlockEntity)level.getBlockEntity(POS);
        for(var key:List.of(Enchantments.MENDING,Enchantments.FROST_WALKER,Enchantments.SOUL_SPEED,Enchantments.SWIFT_SNEAK,Enchantments.BINDING_CURSE,Enchantments.VANISHING_CURSE)){
            input(machine,new ItemStack(Items.BOOK));check(candidate(machine,key,1)<0,"Excluded enchantment is absent: "+key.identifier());
        }
        input(machine,new ItemStack(Items.BOOK));check(machine.enchanting.data(12)==172,"32 jars give floor(128 * .666) + 1, doubled without a booster cap");
        machine.setItem(MachineBlockEntity.UPGRADE_START,upgrade(5));check(machine.enchanting.data(12)==215,"Upgrade 5 multiplies the original allowance by 2.5");machine.setItem(MachineBlockEntity.UPGRADE_START,ItemStack.EMPTY);
        for(String cls:List.of("ItemElementalAxeWater","ItemElementalCutter","ItemVoidCutter","ItemElementalCrusher","ItemVoidCrusher")){
            input(machine,tool(cls));check(candidate(machine,Enchantments.RESPIRATION,1)>=0,"Respiration exception for "+cls);
            if(cls.contains("Crusher")){input(machine,tool(cls));check(candidate(machine,Enchantments.AQUA_AFFINITY,1)>=0,"Aqua Affinity exception for "+cls);}
        }
        var weaponTag=TagKey.create(Registries.ITEM,Thaumcraft.id("enchantable/weapons"));
        for(String cls:List.of("ItemThaumiumSword","ItemElementalCutter","ItemVoidCutter")){input(machine,tool(cls));check(machine.getItem(0).is(weaponTag)&&candidate(machine,Enchantments.SHARPNESS,1)>=0,"Weapon tag and Sharpness retained for "+cls);}
        for(String cls:List.of("ItemThaumiumPickaxe","ItemThaumiumShovel","ItemThaumiumHoe","ItemElementalCrusher","ItemVoidCrusher")){input(machine,tool(cls));check(!machine.getItem(0).is(weaponTag)&&candidate(machine,Enchantments.SHARPNESS,1)<0,"No broad weapon membership for "+cls);}
        for(var item:List.of(Items.DIAMOND_SWORD,Items.DIAMOND_AXE)){input(machine,new ItemStack(item));check((candidate(machine,Enchantments.SHARPNESS,1)>=0)==(item==Items.DIAMOND_SWORD),"Sharpness follows table item eligibility");input(machine,new ItemStack(item));check(candidate(machine,Enchantments.RESPIRATION,1)<0,"No ordinary weapon exception");}
        input(machine,tool("ItemThaumiumPickaxe"));check(candidate(machine,Enchantments.RESPIRATION,1)<0,"No ordinary pick exception");
        input(machine,new ItemStack(Items.DIAMOND_PICKAXE));check(machine.enchanting.data(10)==0&&!machine.enchanting.click(3),"Empty selection cannot start");
        machine.setItem(MachineBlockEntity.UPGRADE_START,upgrade(1));select(machine,Enchantments.EFFICIENCY,2);check(machine.enchanting.data(10)==92,"Two-stage discounted price is 92");
        check(machine.enchanting.click(3)&&!machine.enchanting.click(3),"Only one Start is accepted");machine.insertVis(10,false);machine.enchanting.tick();
        machine.setItem(MachineBlockEntity.UPGRADE_START,ItemStack.EMPTY);check(machine.enchanting.data(10)==92,"Accepted discount survives removal");
        var saved=machine.saveWithFullMetadata(level.registryAccess());saved.putInt("work_required",93);
        var restored=(MachineBlockEntity)BlockEntity.loadStatic(POS,machine.getBlockState(),saved,level.registryAccess());restored.setLevel(level);level.setBlockEntity(restored);machine=restored;
        surroundings(level,false);machine.enchanting.tick();check(machine.progress==10&&machine.workRequired==93&&machine.enchanting.data(10)==93,"Old quote and payment survive reload without boosters");finish(machine,93,10);
        surroundings(level,true);input(machine,new ItemStack(Items.DIAMOND_PICKAXE));select(machine,Enchantments.UNBREAKING,1);select(machine,Enchantments.EFFICIENCY,2);
        int before=ArcaneWorldData.get(level).aura(level,POS).badVibes();int quote=machine.enchanting.data(10);machine.enchanting.click(3);finish(machine,quote,0);
        check(ArcaneWorldData.get(level).aura(level,POS).badVibes()-before==4,"Bad vibes truncate each individual contribution; delta="+(ArcaneWorldData.get(level).aura(level,POS).badVibes()-before)+" before="+before);
        for(int change=0;change<3;change++){
            surroundings(level,true);input(machine,new ItemStack(Items.DIAMOND_PICKAXE));select(machine,Enchantments.EFFICIENCY,5);quote=machine.enchanting.data(10);machine.enchanting.click(3);machine.insertVis(10,false);machine.enchanting.tick();
            surroundings(level,false);if(change==0)level.setBlockAndUpdate(POS.offset(2,0,0),Blocks.BOOKSHELF.defaultBlockState());else if(change==1)level.setBlockAndUpdate(POS.offset(2,0,0),Content.block("brain_in_a_jar").defaultBlockState());else level.setBlockAndUpdate(POS.offset(1,0,0),Blocks.STONE.defaultBlockState());
            machine.enchanting.tick();check(machine.enchanting.data(13)==1&&machine.progress==10,"Accepted tier survives environmental change "+change);finish(machine,quote,10);
        }
        surroundings(level,true);machine.setItem(MachineBlockEntity.UPGRADE_START,upgrade(6));input(machine,new ItemStack(Items.BOOK));
        for(var key:List.of(Enchantments.UNBREAKING,Enchantments.EFFICIENCY,Enchantments.RESPIRATION,Enchantments.PROTECTION))select(machine,key,1);
        quote=machine.enchanting.data(10);machine.enchanting.click(3);machine.setItem(MachineBlockEntity.UPGRADE_START,ItemStack.EMPTY);machine.enchanting.tick();check(machine.enchanting.data(15)==4&&machine.enchanting.data(13)==1,"Fourth accepted selection survives upgrade removal");finish(machine,quote,0);
        check(EnchantmentHelper.getEnchantmentsForCrafting(machine.getItem(0)).size()==4,"All four accepted enchantments applied");
        input(machine,new ItemStack(Items.DIAMOND_PICKAXE));select(machine,Enchantments.EFFICIENCY,5);surroundings(level,false);check(!machine.enchanting.click(3),"Start rejects lost per-tier power");
        for(int invalid=0;invalid<8;invalid++){
            surroundings(level,true);input(machine,new ItemStack(Items.DIAMOND_PICKAXE));select(machine,Enchantments.EFFICIENCY,2);machine.enchanting.click(3);machine.insertVis(10,false);machine.enchanting.tick();
            if(invalid==0)machine.setItem(0,ItemStack.EMPTY);
            else if(invalid==1)machine.getItem(0).set(DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("replacement"));
            else {saved=machine.saveWithFullMetadata(level.registryAccess());if(invalid==2)saved.putString("enchantment_0","missing:unknown");if(invalid==3)saved.putInt("enchantment_level_0",255);if(invalid==4)saved.remove("enchantment_0");if(invalid==5)saved.putInt("work_required",0);if(invalid>=6){saved.putString("enchantment_0",invalid==6?"minecraft:mending":"minecraft:sharpness");saved.putInt("enchantment_level_0",1);}restored=(MachineBlockEntity)BlockEntity.loadStatic(POS,machine.getBlockState(),saved,level.registryAccess());restored.setLevel(level);level.setBlockEntity(restored);machine=restored;}
            machine.enchanting.tick();check(machine.enchanting.data(13)==0&&!machine.getItem(0).isEnchanted(),"Invalid input or corrupt selection cancels job "+invalid);
        }
        input(machine,new ItemStack(Items.BOOK,2));check(machine.enchanting.data(14)==0&&!machine.enchanting.click(3),"Stacked books cannot start");
        surroundings(level,false);level.setBlockAndUpdate(POS,Blocks.AIR.defaultBlockState());return checks;
    }
}
