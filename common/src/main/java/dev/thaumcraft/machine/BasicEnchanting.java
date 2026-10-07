package dev.thaumcraft.machine;

import dev.thaumcraft.gameplay.ArcaneEnchantments;
import dev.thaumcraft.gameplay.ArcaneWorldData;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedRandom;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import java.util.ArrayList;
import java.util.List;

/** Original offers, strength and vis prices, with levels 50/65 displayed as 30/39. */
public final class BasicEnchanting {
    private final MachineBlockEntity machine;
    private ItemStack input=ItemStack.EMPTY;
    private final int[] offers=new int[3]; // Original levels retain exact prices despite display rounding.
    private int choice=-1;
    BasicEnchanting(MachineBlockEntity machine){this.machine=machine;}
    public int offer(int row){return Math.round(offers[row]*.6f);}
    public int cost(int row){return choice==row?machine.workRequired:offers[row]*2*(offers[row]/2);}
    public int selected(){return choice;}
    static int rollOffer(RandomSource random,int row,int boost){
        boost=Math.clamp(boost,0,40);
        int base=1+(boost>>1)+random.nextInt(boost+1)+random.nextInt(5);
        return row==0?(base>>1)+1:row==1?base*2/3+1:base;
    }
    private void validate(){
        if(ItemStack.matches(input,machine.getItem(0)))return;
        input=machine.getItem(0).copy();choice=-1;machine.progress=0;machine.workRequired=0;java.util.Arrays.fill(offers,0);
        if(input.getCount()==1&&(input.isEnchantable()||input.is(Items.BOOK))){
            int boost=EnchantingBoosters.power(machine);var random=machine.getLevel().getRandom();
            for(int row=0;row<3;row++)offers[row]=rollOffer(random,row,boost);
        }
        machine.setChanged();
    }
    public boolean click(int row){
        if(!(machine.getLevel() instanceof ServerLevel)||row<0||row>2)return false;
        validate();if(choice!=-1||offers[row]<=0)return false;
        machine.workRequired=cost(row);choice=row;machine.progress=0;machine.setChanged();return true;
    }
    public void tick(ServerLevel level){
        validate();if(choice<0)return;
        if(machine.progress<machine.workRequired){
            int request=Math.min(10,machine.workRequired-machine.progress);
            VisNetwork.pull(level,machine,Math.max(0,request-machine.pureVis()),false);
            int paid=Math.min(request,(int)machine.pureVis());machine.spendVis(paid,false);machine.progress+=paid;machine.setChanged();
            return;
        }
        int strength=offers[choice],badVibes=offers[choice]/2;var pool=enchantments();List<EnchantmentInstance> selected=List.of();
        for(int attempt=0;attempt<50;attempt++){
            selected=select(level.getRandom(),input,strength,pool);
            if(!selected.isEmpty())break;
        }
        if(!selected.isEmpty()){
            ItemStack result=input.is(Items.BOOK)?new ItemStack(Items.ENCHANTED_BOOK):input.copy();
            for(var enchantment:selected)result.enchant(enchantment.enchantment(),enchantment.level());
            machine.setItem(0,result);ArcaneWorldData.get(level).addVibes(level,machine.getBlockPos(),0,badVibes);MachineLogic.effect(level,machine);
        }
        choice=-1;machine.progress=0;machine.workRequired=0;validate();machine.setChanged();
    }
    List<Holder<Enchantment>> enchantments(){
        var level=(ServerLevel)machine.getLevel();
        return level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).listElements()
                .filter(EnchantingCompatibility::allowed)
                .filter(h->ArcaneEnchantments.available(level,machine,h)).map(h->(Holder<Enchantment>)h).toList();
    }
    // Original vanilla costs belong to this machine, without changing the shared registry or Occultic Enchanter.
    private static int minCost(String id,Enchantment enchantment,int rank){
        return switch(id){
            case "minecraft:protection","minecraft:sharpness" -> 1+16*(rank-1);
            case "minecraft:efficiency" -> 1+15*(rank-1);
            case "minecraft:silk_touch" -> 25;
            case "minecraft:unbreaking" -> 5+10*(rank-1);
            case "minecraft:looting","minecraft:fortune" -> 20+12*(rank-1);
            default -> enchantment.getMinCost(rank);
        };
    }
    private static int maxCost(String id,Enchantment enchantment,int rank,int min){
        return switch(id){
            case "minecraft:protection","minecraft:sharpness" -> min+20;
            case "minecraft:fire_protection","minecraft:blast_protection" -> min+12;
            case "minecraft:feather_falling" -> min+10;
            case "minecraft:projectile_protection" -> min+15;
            case "minecraft:knockback","minecraft:fire_aspect","minecraft:looting","minecraft:efficiency",
                    "minecraft:silk_touch","minecraft:unbreaking","minecraft:fortune" -> 51+10*rank;
            default -> enchantment.getMaxCost(rank);
        };
    }
    static List<EnchantmentInstance> select(RandomSource random,ItemStack stack,int strength,List<Holder<Enchantment>> pool){
        var enchantable=stack.get(DataComponents.ENCHANTABLE);
        if(enchantable==null)return List.of();
        int half=enchantable.value()/2;
        int power=strength+1+random.nextInt(half+1)+random.nextInt(half+1);
        power=Math.max(1,Math.round(power*(1+(random.nextFloat()+random.nextFloat()-1)*.25f)));
        var candidates=new ArrayList<EnchantmentInstance>();
        for(var holder:pool){
            if(!EnchantingCompatibility.applies(holder,stack))continue;
            var enchantment=holder.value();
            String id=holder.unwrapKey().map(k->k.identifier().toString()).orElse("");
            for(int rank=enchantment.getMaxLevel();rank>=enchantment.getMinLevel();rank--){
                int min=minCost(id,enchantment,rank),max=maxCost(id,enchantment,rank,min);
                if(power>=min&&power<=max){candidates.add(new EnchantmentInstance(holder,rank));break;}
            }
        }
        var result=new ArrayList<EnchantmentInstance>();
        WeightedRandom.getRandomItem(random,candidates,EnchantmentInstance::weight).ifPresent(result::add);
        if(result.isEmpty())return result;
        for(int chance=power/2;random.nextInt(50)<=chance;chance/=2){
            EnchantmentHelper.filterCompatibleEnchantments(candidates,result.getLast());
            if(candidates.isEmpty())break;
            WeightedRandom.getRandomItem(random,candidates,EnchantmentInstance::weight).ifPresent(result::add);
        }
        return result;
    }
    public void save(ValueOutput out){out.putInt("basic_enchant_version",2);out.store("basic_enchant_input",ItemStack.OPTIONAL_CODEC,input);out.putInt("basic_enchant_choice",choice);for(int i=0;i<3;i++)out.putInt("basic_enchant_offer"+i,offers[i]);}
    public void load(ValueInput in){
        input=in.read("basic_enchant_input",ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);choice=Math.clamp(in.getIntOr("basic_enchant_choice",-1),-1,2);
        int version=in.getIntOr("basic_enchant_version",0);
        for(int i=0;i<3;i++){
            int saved=Math.clamp(in.getIntOr("basic_enchant_offer"+i,0),0,65);
            // Version 1 could also carry an unfinished original job above its own thirty-level cap.
            offers[i]=version==1&&saved<=30?Math.round(saved/.6f):saved;
        }
        // Version 0 already stored original levels. Reroll only the interim thirty-level idle offers.
        // Active jobs keep their quoted charge in workRequired and retain their paid progress.
        if(version==1&&choice<0){input=ItemStack.EMPTY;java.util.Arrays.fill(offers,0);}
    }
}
