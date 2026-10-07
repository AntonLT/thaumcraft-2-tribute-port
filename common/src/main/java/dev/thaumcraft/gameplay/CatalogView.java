package dev.thaumcraft.gameplay;

import dev.thaumcraft.api.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import java.util.List;
import java.util.Optional;

/** API view of one side's catalog. Each call reads one snapshot; item and value work runs pinned to that side. */
public final class CatalogView implements ThaumcraftCatalog {
    private record ResearchView(Identifier id,Identifier category,Component name,Component description,int difficulty,int steps,boolean restricted,List<Identifier> prerequisites) implements Research {}
    private record CategoryView(Identifier id,Component name,int order) implements ResearchCategory {}
    private record InfusionView(Identifier id,boolean dark,int cost,List<String> ingredients,GameData.StackDef output,Optional<Identifier> research) implements InfusionRecipe {
        @Override public ItemStack result(){return output.create();}
    }
    private final boolean client;
    public CatalogView(boolean client){this.client=client;}
    private static Research research(AddonData.Catalog catalog,GameData.Project p){
        var prerequisites=p.prerequisites().stream().map(index->catalog.ids().getOrDefault(index,GameData.legacyResearchId(index))).toList();
        return new ResearchView(p.key(),Identifier.parse(catalog.snapshot().categories().get(p.category()).id()),p.nameComponent(),p.descriptionComponent(),p.difficulty(),p.steps(),p.restricted(),prerequisites);
    }
    private static ResearchCategory category(AddonData.Category c){return new CategoryView(Identifier.parse(c.id()),c.nameComponent(),c.order());}
    private static InfusionRecipe infusion(GameData.Infusion r){return new InfusionView(r.key(),r.dark(),r.cost(),r.ingredients(),r.result(),Optional.ofNullable(r.requiredResearch()));}

    @Override public Optional<Research> research(Identifier id){var catalog=AddonData.catalog(client);return Optional.ofNullable(catalog.projects().get(id)).map(p->research(catalog,p));}
    @Override public List<Research> allResearch(){var catalog=AddonData.catalog(client);return catalog.snapshot().projects().stream().map(p->research(catalog,p)).toList();}
    @Override public List<ResearchCategory> categories(){return AddonData.catalog(client).snapshot().categories().stream().map(CatalogView::category).toList();}
    @Override public Optional<ResearchCategory> category(Identifier id){return AddonData.catalog(client).snapshot().categories().stream().filter(c->c.id().equals(id.toString())).findFirst().map(CatalogView::category);}
    @Override public List<InfusionRecipe> infusions(){return AddonData.catalog(client).snapshot().infusions().stream().map(CatalogView::infusion).toList();}
    @Override public Optional<InfusionRecipe> infusion(Identifier id){return AddonData.catalog(client).snapshot().infusions().stream().filter(r->r.id().equals(id.toString())).findFirst().map(CatalogView::infusion);}
    @Override public Optional<Identifier> requiredResearch(Identifier recipe){
        var catalog=AddonData.catalog(client);Integer index=catalog.requirements().get(recipe);
        if(index==null)return Optional.empty();
        return Optional.of(index==AddonData.UNAVAILABLE?AddonData.UNAVAILABLE_ID:catalog.ids().getOrDefault(index,GameData.legacyResearchId(index)));
    }
    @Override public float vis(ItemStack stack){return AddonData.on(client,()->GameData.vis(stack));}
    @Override public float restorerCost(ItemStack stack){return AddonData.on(client,()->GameData.restorerCost(stack));}
    @Override public int researchValue(ItemStack stack){return AddonData.on(client,()->ResearchLogic.researchValue(stack));}
    @Override public Optional<Booster> booster(net.minecraft.world.level.block.state.BlockState state){return AddonData.on(client,()->Optional.ofNullable(GameData.booster(state)));}
    @Override public Optional<ItemStack> theory(Identifier research){return AddonData.on(client,()->GameData.findProject(research).map(p->ResearchLogic.theory(p.index())));}
    @Override public Optional<ItemStack> discovery(Identifier research){return AddonData.on(client,()->GameData.findProject(research).map(ResearchLogic::discovery));}
}
