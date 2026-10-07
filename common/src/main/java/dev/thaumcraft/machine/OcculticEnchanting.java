package dev.thaumcraft.machine;

import dev.thaumcraft.gameplay.ArcaneEnchantments;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Server validates every choice and spends vis over time, allowing costs larger than the tank. */
public final class OcculticEnchanting {
    private final MachineBlockEntity machine;
    private final String[] chosen=new String[4];
    private final int[] levels=new int[4];
    private ItemStack input=ItemStack.EMPTY;
    private int cursor;
    private int scroll;
    private long choicesTick=Long.MIN_VALUE;
    private List<EnchantmentInstance> cachedChoices=List.of();
    private boolean active;

    OcculticEnchanting(MachineBlockEntity machine) {this.machine=machine;}
    private ServerLevel level() {return machine.getLevel() instanceof ServerLevel level?level:null;}
    private int limit() {return machine.upgrades(6)>0?4:3;}
    private boolean validInput() {
        ItemStack stack=machine.getItem(0);
        return stack.getCount()==1&&(stack.isEnchantable()||stack.is(Items.BOOK));
    }
    private Holder<Enchantment> holder(int slot) {
        if(level()==null||chosen[slot]==null)return null;
        Identifier id=Identifier.tryParse(chosen[slot]);
        return id==null?null:level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(ResourceKey.create(Registries.ENCHANTMENT,id)).orElse(null);
    }
    private int count() {int count=0;for(int i=0;i<4;i++)if(chosen[i]!=null)count++;return count;}
    private int power() {
        int power=0;
        for(int i=0;i<4;i++){var enchantment=holder(i);if(enchantment!=null)power+=enchantment.value().getMinCost(levels[i]);}
        return power;
    }
    private int maximumPower() {
        if(level()==null)return 0;
        int boost=(int)(EnchantingBoosters.power(machine)*.666f);
        var enchantable=machine.getItem(0).get(DataComponents.ENCHANTABLE);
        return (int)((boost+(enchantable==null?1:enchantable.value()))*(machine.upgrades(5)>0?2.5f:2f));
    }
    private int candidatePower(){
        var enchantable=machine.getItem(0).get(DataComponents.ENCHANTABLE);
        return (int)(EnchantingBoosters.power(machine)*.666f)+(enchantable==null?1:enchantable.value());
    }
    private int cost() {
        int power=power();if(power==0)return 0;
        int cost=(int)(power*Math.sqrt(power)*3.0);
        if(machine.upgrades(1)>0)cost=(int)(cost*.85f);
        return Math.max(25,cost);
    }
    private boolean applies(Holder<Enchantment> enchantment){
        return EnchantingCompatibility.applies(enchantment,machine.getItem(0));
    }
    private boolean compatible(Holder<Enchantment> candidate) {
        for(int i=0;i<4;i++){var other=holder(i);if(other!=null&&!Enchantment.areCompatible(candidate,other))return false;}
        return true;
    }
    private List<EnchantmentInstance> choices() {
        if(level()==null||!validInput()||count()>=limit())return List.of();
        if(choicesTick==level().getGameTime())return cachedChoices;
        int availablePower=maximumPower()-power();
        var result=new ArrayList<EnchantmentInstance>();
        level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).listElements()
                .filter(EnchantingCompatibility::allowed)
                .filter(h->ArcaneEnchantments.available(level(),machine,h))
                .filter(this::applies)
                .filter(this::compatible).sorted(Comparator.comparing(h->h.key().identifier().toString()))
                .forEach(h->{for(int n=h.value().getMinLevel();n<=h.value().getMaxLevel();n++)if(h.value().getMinCost(n)<=Math.min(availablePower,candidatePower()))result.add(new EnchantmentInstance(h,n));});
        choicesTick=level().getGameTime();cachedChoices=List.copyOf(result);return cachedChoices;
    }
    private EnchantmentInstance candidate() {var choices=choices();return choices.isEmpty()?null:choices.get(Math.floorMod(cursor,choices.size()));}
    public void reset() {
        choicesTick=Long.MIN_VALUE;java.util.Arrays.fill(chosen,null);java.util.Arrays.fill(levels,0);input=ItemStack.EMPTY;
        active=false;cursor=0;scroll=0;machine.progress=0;machine.setChanged();
    }
    private void validate() {
        if(count()==0){if(active)reset();return;}
        if(!validInput()||!ItemStack.isSameItemSameComponents(input,machine.getItem(0))
                ||active&&machine.workRequired<=0){reset();return;}
        // Start validates today's environment. An accepted job owns its stored quote and selection.
        if(!active&&(count()>limit()||power()>maximumPower())){reset();return;}
        for(int i=0;i<4;i++)if(chosen[i]!=null) {
            var enchantment=holder(i);
            if(enchantment==null||levels[i]<enchantment.value().getMinLevel()||levels[i]>enchantment.value().getMaxLevel()
                    ||!EnchantingCompatibility.allowed(enchantment)||!applies(enchantment)
                    ||!ArcaneEnchantments.available(level(),machine,enchantment)
                    ||!active&&enchantment.value().getMinCost(levels[i])>candidatePower()){reset();return;}
            for(int j=0;j<i;j++){
                var other=holder(j);
                if(other!=null&&!Enchantment.areCompatible(enchantment,other)){reset();return;}
            }
        }
    }
    public boolean click(int button) {
        if(level()==null||!machine.machineId().equals("occultic_enchanter"))return false;
        validate();
        if(active)return false;
        if(button>=100&&button<140){
            var choices=choices();int index=scroll*10+button-100;if(index>=choices.size())return false;
            cursor=index;button=2;
        }
        switch(button) {
            case 0,1 -> {var choices=choices();if(choices.isEmpty())return false;scroll=Math.clamp(scroll+(button==0?-1:1),0,Math.max(0,(choices.size()-31)/10));}
            case 2 -> {
                var candidate=candidate();if(candidate==null)return false;
                for(int i=0;i<limit();i++)if(chosen[i]==null) {
                    chosen[i]=candidate.enchantment().unwrapKey().orElseThrow().identifier().toString();levels[i]=candidate.level();break;
                }
                input=machine.getItem(0).copyWithCount(1);cursor=0;scroll=0;
            }
            case 3 -> {if(count()==0)return false;active=true;machine.progress=0;machine.workRequired=cost();}
            case 4,5,6,7 -> {int slot=button-4;chosen[slot]=null;levels[slot]=0;cursor=0;scroll=0;}
            default -> {return false;}
        }
        choicesTick=Long.MIN_VALUE;machine.setChanged();return true;
    }
    public void tick() {
        validate();if(!active)return;
        int cost=machine.workRequired;
        if(machine.progress<cost) {
            float requested=Math.min(10,cost-machine.progress);
            float drawn=machine.pureVis()<requested?VisNetwork.pull(level(),machine,requested-machine.pureVis(),false):0;
            int spent=Math.min((int)machine.pureVis(),(int)requested);
            if(spent>0){machine.spendVis(spent,false);machine.progress+=spent;machine.setChanged();}
            if((drawn>=.025f||spent>0)&&machine.processes.soundDelay==0){machine.processes.soundDelay=80;dev.thaumcraft.content.ModSounds.play(level(),machine.getBlockPos(),"whisper",net.minecraft.sounds.SoundSource.BLOCKS,.2f,1);}
            if(machine.progress<cost)return;
        }
        ItemStack result=machine.getItem(0).is(Items.BOOK)?new ItemStack(Items.ENCHANTED_BOOK):machine.getItem(0).copyWithCount(1);
        int badVibes=0;
        for(int i=0;i<4;i++){var enchantment=holder(i);if(enchantment!=null){
            result.enchant(enchantment,levels[i]);badVibes+=enchantment.value().getMinCost(levels[i])/3;
        }}
        machine.setItem(0,result);
        dev.thaumcraft.gameplay.ArcaneWorldData.get(level()).addVibes(level(),machine.getBlockPos(),0,badVibes);
        reset();MachineLogic.effect(level(),machine);
    }
    public int data(int index) {
        if(level()==null||!machine.machineId().equals("occultic_enchanter"))return 0;
        if(index>=16&&index<96){var choices=choices();int visible=scroll*10+(index-16)/2;if(visible>=choices.size())return 0;var c=choices.get(visible);return index%2==0?level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).asHolderIdMap().getId(c.enchantment())+1:c.level();}
        if(index==96)return scroll;
        if(index==0||index==1){var candidate=candidate();return candidate==null?0:index==0?level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).asHolderIdMap().getId(candidate.enchantment())+1:candidate.level();}
        if(index>=2&&index<6){var h=holder(index-2);return h==null?0:level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).asHolderIdMap().getId(h)+1;}
        if(index>=6&&index<10)return levels[index-6];
        return switch(index){case 10->active?machine.workRequired:cost();case 11->power();case 12->maximumPower();case 13->active?1:0;case 14->choices().size();case 15->count();default->0;};
    }
    public void load(ValueInput saved) {
        for(int i=0;i<4;i++){chosen[i]=saved.getStringOr("enchantment_"+i,"");if(chosen[i].isEmpty())chosen[i]=null;levels[i]=Math.clamp(saved.getIntOr("enchantment_level_"+i,0),0,255);}
        input=saved.read("enchantment_input",ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        active=saved.getBooleanOr("enchanting",false);
        if(active&&saved.getIntOr("work_required",0)<=0)reset();
    }
    public void save(ValueOutput saved) {
        for(int i=0;i<4;i++)if(chosen[i]!=null){saved.putString("enchantment_"+i,chosen[i]);saved.putInt("enchantment_level_"+i,levels[i]);}
        saved.store("enchantment_input",ItemStack.OPTIONAL_CODEC,input);saved.putBoolean("enchanting",active);
    }
}
