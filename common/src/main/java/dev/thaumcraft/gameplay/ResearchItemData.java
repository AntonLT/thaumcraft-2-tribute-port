package dev.thaumcraft.gameplay;

import dev.thaumcraft.content.Content;
import dev.thaumcraft.item.ItemState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.util.Optional;

/** Research identity stored in the item's existing synchronized custom data component. */
public final class ResearchItemData {
    private ResearchItemData() {}

    public static Optional<GameData.Project> project(ItemStack stack) {
        String saved=ItemState.getString(stack,"research_id","");
        if(!saved.isEmpty()){
            Identifier id=Identifier.tryParse(saved);
            return id==null?Optional.empty():GameData.findProject(id);
        }
        var entry=Content.entry(stack);
        if(entry==null||entry.research()==null)return Optional.empty();
        int legacy=ItemState.getInt(stack,"book_project",entry.research());
        return GameData.findProject(GameData.legacyResearchId(legacy));
    }

    public static void setProject(ItemStack stack,Identifier id) {
        ItemState.setString(stack,"research_id",id.toString());
    }

    /** Called only when using a research item on the server. Unknown IDs remain untouched. */
    public static void migrate(ItemStack stack) {
        project(stack).ifPresent(project->setProject(stack,project.key()));
    }

    /** Gives addon-category fragments, theories and discoveries their category's item model, or restores the default. */
    public static void refreshModel(ItemStack stack) {
        var entry=Content.entry(stack);if(entry==null)return;
        var categories=AddonData.current().categories();
        String model=switch(entry.id()){
            case "theory_generic" -> project(stack).map(p->categories.get(p.category()).theory_model()).orElse(null);
            case "discovery_generic" -> project(stack).map(p->categories.get(p.category()).discovery_model()).orElse(null);
            default -> {
                if(!entry.legacy().endsWith("itemKnowledgeFragment"))yield null;
                String id=ItemState.getString(stack,"research_category","");
                yield categories.stream().filter(c->c.id().equals(id)).map(AddonData.Category::fragment_model).findFirst().orElse(null);
            }
        };
        Identifier target=model==null?stack.getPrototype().get(DataComponents.ITEM_MODEL):Identifier.parse(model);
        if(!java.util.Objects.equals(stack.get(DataComponents.ITEM_MODEL),target))stack.set(DataComponents.ITEM_MODEL,target);
    }

    /** Refresh only the generated wrapper; anvil names and styled custom names stay intact. */
    public static void refreshName(ItemStack stack) {
        var entry=Content.entry(stack);if(entry==null)return;
        String key=switch(entry.id()){
            case "theory_generic" -> "item.thaumcraft2tp.addon_theory";
            case "discovery_generic" -> "item.thaumcraft2tp.addon_discovery";
            default -> entry.legacy().endsWith("itemKnowledgeFragment")&&!ItemState.getString(stack,"research_category","").isEmpty()?"item.thaumcraft2tp.addon_fragment":null;
        };
        if(key==null)return;
        var current=stack.get(DataComponents.CUSTOM_NAME);
        if(current==null||!current.getStyle().isEmpty()||!current.getSiblings().isEmpty()||!(current.getContents() instanceof TranslatableContents contents)||!contents.getKey().equals(key)||contents.getArgs().length!=1)return;
        Component name;
        if(key.endsWith("addon_fragment")){
            String id=ItemState.getString(stack,"research_category","");
            name=AddonData.current().categories().stream().filter(c->c.id().equals(id)).map(AddonData.Category::nameComponent).findFirst().orElse(null);
        }else name=project(stack).map(GameData.Project::nameComponent).orElse(null);
        if(name==null)return;
        var refreshed=Component.translatableWithFallback(key,contents.getFallback(),name);
        if(!refreshed.equals(current))stack.set(DataComponents.CUSTOM_NAME,refreshed);
    }
}
