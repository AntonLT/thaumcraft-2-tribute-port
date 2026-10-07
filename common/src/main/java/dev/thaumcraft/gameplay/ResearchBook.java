package dev.thaumcraft.gameplay;

import dev.thaumcraft.item.ItemState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.WrittenBookContent;

import java.util.ArrayList;
import java.util.List;

public final class ResearchBook {
    private ResearchBook() {}
    public static void openDiscovery(ServerPlayer player,ItemStack stack,InteractionHand hand,int project) {
        var def=GameData.project(project);
        ResearchItemData.setProject(stack,def.key());
        List<String> text=new ArrayList<>();text.add(def.name()+"\n\n"+def.text());
        addRecipes(text,project);
        awardRecipes(player,project);
        open(player,stack,hand,text);
    }
    public static void openTome(ServerPlayer player,ItemStack stack,InteractionHand hand) {
        var known=GameData.projects().stream().filter(p->ArcaneWorldData.knows(player,p.index())).toList();
        ItemState.setString(stack,"book_known",String.join(",",known.stream().map(GameData.Project::id).toList()));
        awardRecipes(player,-1);
        for(var project:known)awardRecipes(player,project.index());
        open(player,stack,hand,List.of("Thaumonomicon"));
    }
    private static void addRecipes(List<String> text,int project) {
        for(var recipe:GameData.infusions())if(recipe.research()==project)
            text.add(infusionText(recipe));
        for(var recipe:GameData.crafts())if(recipe.research()==project) {
            text.add(craftText(recipe));
        }
    }
    public static void openCrystalBall(ServerPlayer player,ItemStack stack,InteractionHand hand,int[] runes,boolean learn){
        // Seal knowledge belongs to the save, like research; adopt combinations learned under the old per-dimension storage.
        var data=ArcaneWorldData.researchData(player.level());
        for(String known:ArcaneWorldData.get(player.level()).knownSeals())data.learnSeal(known);
        String combination=runes[0]+","+runes[1]+","+runes[2];
        if(learn&&runes[0]>=0&&data.learnSeal(combination))player.sendSystemMessage(Component.translatableWithFallback("message.thaumcraft2tp.seal.learned", "You have learned a new seal combination!"));
        ItemState.setString(stack,"book_seals",String.join(";",data.knownSeals()));
        for(int i=0;i<3;i++)ItemState.setInt(stack,"book_rune_"+i,runes[i]);
        open(player,stack,hand,List.of("Select three runes to inspect a seal combination."));
    }
    private static String infusionText(GameData.Infusion recipe){return (recipe.dark()?"Dark Infusion":"Infusion")+"\n"+recipe.cost()+" vis -> "+recipe.result().count()+" "+ingredientName(recipe.result().id())+"\n\n"+String.join("\n",recipe.ingredients().stream().map(ResearchBook::ingredientName).toList());}
    private static String craftText(GameData.Craft recipe) {
        String grid=recipe.shaped()?String.join("\n",recipe.pattern()).replace(' ','·')+"\n\n"+String.join("\n",recipe.key().entrySet().stream().map(e->e.getKey()+" = "+ingredientName(e.getValue())).toList()):String.join("\n",recipe.ingredients().stream().map(ResearchBook::ingredientName).toList());
        return (recipe.shaped()?"Crafting grid":"Shapeless crafting")+"\n"+recipe.result().count()+" "+ingredientName(recipe.result().id())+"\n\n"+grid;
    }
    public static void awardRecipes(ServerPlayer player,int project) {player.awardRecipes(recipes(player,project));}
    public static void resetRecipes(ServerPlayer player,int project) {player.resetRecipes(recipes(player,project));}
    private static List<net.minecraft.world.item.crafting.RecipeHolder<?>> recipes(ServerPlayer player,int project) {
        var recipes=new ArrayList<net.minecraft.world.item.crafting.RecipeHolder<?>>();
        for(var recipe:GameData.crafts())if(recipe.research()==project)player.level().getServer().getRecipeManager().byKey(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE,net.minecraft.resources.Identifier.parse(recipe.id()))).ifPresent(recipes::add);
        return recipes;
    }
    private static String ingredientName(String id) {
        if(id.startsWith("#")){
            String path=id.substring(id.indexOf(':')+1);
            if(path.startsWith("legacy/"))path=path.substring(7);
            else if(path.startsWith("ingots/")||path.startsWith("nuggets/")){int slash=path.indexOf('/');path=path.substring(slash+1)+" "+path.substring(0,slash);}
            return "Any "+path.replace('_',' ').replace('/',' ');
        }
        return id.replace("minecraft:","").replace("thaumcraft2tp:","").replace('_',' ');
    }
    private static void open(ServerPlayer player,ItemStack stack,InteractionHand hand,List<String> texts) {
        var pages=new ArrayList<Filterable<Component>>();
        for(String text:texts) {
            // Written books permit more than 100 pages. Keep every discovered recipe and
            // wrap conservatively inside the native 114px x 128px book text area.
            var lines=new ArrayList<String>();
            for(String paragraph:text.replace("\\n","\n").split("\n",-1)) {
                String remaining=paragraph;
                while(remaining.length()>18) {
                    int end=remaining.lastIndexOf(' ',18);if(end<1)end=18;
                    lines.add(remaining.substring(0,end));remaining=remaining.substring(end).stripLeading();
                }
                lines.add(remaining);
            }
            for(int start=0;start<lines.size();start+=14) {
                pages.add(Filterable.passThrough(Component.literal(String.join("\n",lines.subList(start,Math.min(start+14,lines.size()))))));
            }
        }
        // Book data opens the research screen, but must not replace the item name or tooltip.
        stack.set(DataComponents.WRITTEN_BOOK_CONTENT,new WrittenBookContent(Filterable.passThrough(""),"Azanor",0,pages,true));
        stack.set(DataComponents.TOOLTIP_DISPLAY,stack.getOrDefault(DataComponents.TOOLTIP_DISPLAY,net.minecraft.world.item.component.TooltipDisplay.DEFAULT).withHidden(DataComponents.WRITTEN_BOOK_CONTENT,true));
        player.containerMenu.broadcastChanges();
        player.openItemGui(stack,hand);
    }
}
