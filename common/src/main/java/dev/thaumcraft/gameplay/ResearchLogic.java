package dev.thaumcraft.gameplay;

import dev.thaumcraft.content.Content;
import dev.thaumcraft.item.ItemState;
import dev.thaumcraft.machine.MachineBlockEntity;
import dev.thaumcraft.machine.MachineLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/** Research selection, configurable theory steps, and prerequisite graph. */
public final class ResearchLogic {
    private ResearchLogic() {}
    public static boolean tick(ServerLevel level,MachineBlockEntity machine) {
        ItemStack primary=machine.getItem(0);
        if(primary.isEmpty() || !machine.getItem(3).is(Items.PAPER)){if(machine.progress!=0){machine.progress=0;machine.setChanged();}return false;}
        Content.Entry entry=Content.entry(primary);
        boolean theory=entry!=null && entry.legacy().endsWith("itemTheory");
        boolean fragment=entry!=null && entry.legacy().endsWith("itemKnowledgeFragment");
        if(fragment&&fragmentCategory(primary,entry)<0){if(machine.progress!=0){machine.progress=0;machine.setChanged();}return false;}
        var definition=ResearchItemData.project(primary);
        if(theory && definition.isEmpty()){if(machine.progress!=0){machine.progress=0;machine.setChanged();}return false;}
        if(theory)ResearchItemData.migrate(primary);
        int project=theory?definition.orElseThrow().index():-1;
        int difficulty=theory?ItemState.getInt(primary,"difficulty",GameData.project(project).difficulty()):0;
        var boosters=dev.thaumcraft.machine.EnchantingBoosters.research(level,machine.getBlockPos());
        int protection=boosters.failureProtection();
        machine.workRequired=Math.max(20,130+(theory?(difficulty-2)*30:0)-boosters.researchSpeed());
        machine.progress++;machine.setChanged();
        if(machine.progress<machine.workRequired)return true;
        machine.progress=0;machine.sequence++;
        var source=GameData.researchItem(primary);
        int category=theory?GameData.project(project).category():researchCategory(primary,categoryRandom(machine));
        int success=odds(machine).success();
        if(theory) {
            int progress=ItemState.getInt(primary,"research_progress",0);
            if(level.getRandom().nextInt(100)<success)progress++;
            else if(level.getRandom().nextInt(100)<Math.max(5,5+difficulty*difficulty-protection)) {
                if(progress>0)progress--;
                else if(level.getRandom().nextInt(20)==0) {
                    if(difficulty<5)ItemState.setInt(primary,"difficulty",difficulty+1);
                    else primary.shrink(1);
                }
            }
            if(progress>=GameData.project(project).steps()) {
                ItemStack discovery=discovery(GameData.project(project));
                storeResult(level,machine,discovery);primary.shrink(1);machine.consumeInput(3,1);
                MachineLogic.effect(level,machine);
            } else ItemState.setInt(primary,"research_progress",progress);
        } else if(fragment) {
            category=fragmentCategory(primary,entry);
            if(level.getRandom().nextInt(100)<success)randomTheory(level,machine,category);
        } else {
            // A valuable artifact can yield several results during one research cycle.
            // Each generated result consumes paper, even when full output slots make it drop into the world.
            for(int attempts=0;success>0&&attempts<256&&!machine.getItem(3).isEmpty();attempts++) {
                int roll=level.getRandom().nextInt(100);
                for(int slot=0;slot<3;slot++)roll-=switch(machine.getItem(slot).getRarity()){case RARE->2;case EPIC->4;default->0;};
                if(roll>=success)break;
                boolean special=false;
                if(source!=null&&source.special_chance()!=null) {
                    // One roll per success, then an even pick among the eligible specials, so list order carries no bias.
                    var projectDef=selectProject(level,machine,category,source.special());
                    if(projectDef!=null&&level.getRandom().nextInt(100)<source.special_chance()) {
                        if(!emit(level,machine,randomizedTheory(level,projectDef.index())))return true;
                        special=true;
                    }
                }
                else if(source!=null)for(int candidate:source.special()) {
                    var projectDef=selectProject(level,machine,category,List.of(candidate));
                    if(projectDef!=null&&level.getRandom().nextInt(33)<=(projectDef.restricted()?11:0)) {
                        if(!emit(level,machine,randomizedTheory(level,projectDef.index())))return true;
                        special=true;break;
                    }
                }
                if(special)success-=50;
                else if(roll>=0){success-=Math.max(1,roll);if(!emit(level,machine,fragment(Math.max(0,category))))return true;}
                else {success-=25;randomTheory(level,machine,category);}
            }
        }
        int loss=(theory?100:fragment?80:75)-protection;
        for(int slot=theory?1:0;slot<=2;slot++) {
            ItemStack stack=machine.getItem(slot);
            int rarity=switch(stack.getRarity()) {case UNCOMMON->5;case RARE->10;case EPIC->15;default->0;};
            if(!stack.isEmpty()){loss-=rarity;if(level.getRandom().nextInt(100)<loss)stack.shrink(1);}
        }
        machine.setChanged();
        return true;
    }
    public record Odds(int success,int failure,int loss,int theoryProgress) {}
    public static Odds odds(MachineBlockEntity machine){
        var primary=machine.getItem(0);if(primary.isEmpty()||machine.getLevel()==null)return new Odds(0,0,0,-1);
        var entry=Content.entry(primary);boolean theory=entry!=null&&entry.source_class().equals("itemTheory"),fragment=entry!=null&&entry.source_class().equals("ItemKnowledgeFragment");
        var definition=ResearchItemData.project(primary);
        if((theory && definition.isEmpty())||(fragment&&fragmentCategory(primary,entry)<0))return new Odds(0,0,0,-1);
        var boosters=dev.thaumcraft.machine.EnchantingBoosters.research(machine.getLevel(),machine.getBlockPos());
        int bonus=boosters.researchBonus(),brains=boosters.failureProtection();
        var source=GameData.researchItem(primary);
        var categoryRandom=categoryRandom(machine);
        int category=theory?definition.orElseThrow().category():fragment?fragmentCategory(primary,entry):researchCategory(primary,categoryRandom);
        int difficulty=theory?ItemState.getInt(primary,"difficulty",definition.orElseThrow().difficulty()):0;
        int success=theory?Math.clamp(Math.round((50-difficulty*difficulty*2)*(bonus+100)/80f),5,90):fragment?Math.round(12.5f*(bonus+100)/50f):Math.round(researchValue(primary)*(bonus+100)/100f);
        for(int slot=1;slot<=2;slot++)if(!machine.getItem(slot).isEmpty()){
            int value=researchValue(machine.getItem(slot));boolean matching=researchCategory(machine.getItem(slot),categoryRandom)==category;
            success+=fragment?Math.max(1,value/(matching?2:3)):matching?value:Math.max(1,value/2);
        }
        return new Odds(success,theory?Math.max(5,5+difficulty*difficulty-brains):0,(theory?100:fragment?80:75)-brains,theory?ItemState.getInt(primary,"research_progress",0):-1);
    }
    private static boolean emit(ServerLevel level,MachineBlockEntity machine,ItemStack result) {
        if(machine.getItem(3).isEmpty())return false;
        storeResult(level,machine,result);machine.consumeInput(3,1);return true;
    }
    private static void storeResult(ServerLevel level,MachineBlockEntity machine,ItemStack result) {
        if(machine.output(result,true)){machine.output(result,false);return;}
        BlockPos pos=machine.getBlockPos();
        ItemEntity dropped=new ItemEntity(level,pos.getX()+0.5,pos.getY()+0.5,pos.getZ()+0.5,result.copy());
        dropped.setDeltaMovement(0,0.2,0);
        level.addFreshEntity(dropped);
    }
    private static ItemStack randomizedTheory(ServerLevel level,int project) {
        ItemStack result=theory(project);
        int adjustment=switch(level.getRandom().nextInt(10)){case 0->-2;case 1,2->-1;case 7,8->1;case 9->2;default->0;};
        ItemState.setInt(result,"difficulty",Math.clamp(GameData.project(project).difficulty()+adjustment,0,5));return result;
    }
    private static void randomTheory(ServerLevel level,MachineBlockEntity machine,int category) {
        var project=selectProject(level,machine,category,List.of());
        if(project!=null)emit(level,machine,randomizedTheory(level,project.index()));
        else if(emit(level,machine,fragment(category))&&level.getRandom().nextBoolean())emit(level,machine,fragment(level.getRandom().nextInt(4)));
    }
    public static int researchValue(ItemStack stack) {
        var source=GameData.researchItem(stack);return source!=null&&source.value()>=0?source.value():Math.max(1,(int)Math.round(Math.sqrt(GameData.vis(stack))));
    }
    private static net.minecraft.util.RandomSource categoryRandom(MachineBlockEntity machine){return net.minecraft.util.RandomSource.create(machine.getBlockPos().asLong()^machine.sequence*0x9e3779b97f4a7c15L);}
    public static int researchCategory(ItemStack stack,net.minecraft.util.RandomSource random) {
        var source=GameData.researchItem(stack);
        if(source!=null)return source.category()<0?random.nextInt(AddonData.current().categories().size()):source.category();
        int roll=random.nextInt(100);return roll<1?3:roll<3?2:roll<6?1:0;
    }
    public static ItemStack discovery(GameData.Project project) {
        var stack=new GameData.StackDef(project.discovery(),1).create();
        ResearchItemData.setProject(stack,project.key());
        if(project.index()>=70)stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.translatableWithFallback("item.thaumcraft2tp.addon_discovery","Discovery: %s",project.nameComponent()));
        ResearchItemData.refreshModel(stack);return stack;
    }
    public static ItemStack theory(int project) {
        Content.Entry entry=Content.ENTRIES.stream().filter(e->e.legacy().endsWith("itemTheory") && e.research()!=null && e.research()==project).findFirst().orElseGet(()->Content.DEFINITIONS.get("theory_generic"));
        ItemStack stack=new ItemStack(Content.item(entry.id()));
        ResearchItemData.setProject(stack,GameData.project(project).key());
        if(project>=70)stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.translatableWithFallback("item.thaumcraft2tp.addon_theory","Theory: %s",GameData.project(project).nameComponent()));
        ItemState.setInt(stack,"difficulty",GameData.project(project).difficulty());ResearchItemData.refreshModel(stack);return stack;
    }
    private static int fragmentCategory(ItemStack stack,Content.Entry entry){
        String id=ItemState.getString(stack,"research_category","");
        if(id.isEmpty())return entry.meta();
        var categories=AddonData.current().categories();for(int i=0;i<categories.size();i++)if(categories.get(i).id().equals(id))return i;return -1;
    }
    private static ItemStack fragment(int category) {
        Content.Entry entry=Content.ENTRIES.stream().filter(e->e.legacy().endsWith("itemKnowledgeFragment") && e.meta()==Math.clamp(category,0,3)).findFirst().orElseThrow();
        ItemStack stack=new ItemStack(Content.item(entry.id()));
        if(category>=4&&category<AddonData.current().categories().size()){
            var definition=AddonData.current().categories().get(category);ItemState.setString(stack,"research_category",definition.id());ResearchItemData.refreshModel(stack);
            stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.translatableWithFallback("item.thaumcraft2tp.addon_fragment","%s Knowledge Fragment",definition.nameComponent()));
        }
        return stack;
    }
    private static GameData.Project selectProject(ServerLevel level,MachineBlockEntity machine,int category,List<Integer> preferred) {
        var data=ArcaneWorldData.researchData(level);
        var candidates=new ArrayList<GameData.Project>();
        for(GameData.Project project:GameData.projects()) {
            if((!preferred.isEmpty() && !preferred.contains(project.index())) || (preferred.isEmpty() && (project.category()!=category || project.restricted())))continue;
            boolean known=machine.owner()==null?data.globallyKnown(project.index()):data.knows(machine.owner(),project.index());
            if(known)continue;
            if(project.prerequisiteIds().stream().allMatch(p->machine.owner()==null?data.globallyKnown(p):data.knows(machine.owner(),p)))candidates.add(project);
        }
        return candidates.isEmpty()?null:candidates.get(level.getRandom().nextInt(candidates.size()));
    }
}
