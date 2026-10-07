package dev.thaumcraft.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.thaumcraft.gameplay.GameData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import java.util.List;
import java.util.Optional;

/** Native data pack recipe. Research authorization belongs to the machine, not matches(). */
public record InfusionRecipeData(GameData.StackDef result,int cost,List<String> ingredients,String mode,
                                 Optional<Identifier> research,int priority) implements Recipe<RecipeInput> {
    public static final Codec<String> INGREDIENT=Codec.STRING.validate(value->{
        var id=Identifier.tryParse(value.startsWith("#")?value.substring(1):value);
        if(id==null)return DataResult.error(()->"Invalid ingredient "+value);
        if(!value.startsWith("#")&&(!BuiltInRegistries.ITEM.containsKey(id)||BuiltInRegistries.ITEM.getValue(id)==Items.AIR))return DataResult.error(()->"Missing ingredient item "+value);
        return DataResult.success(value);
    });
    public static final Codec<GameData.StackDef> STACK=RecordCodecBuilder.create(i->i.group(
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("id").forGetter(s->BuiltInRegistries.ITEM.getValue(Identifier.parse(s.id()))),
            Codec.intRange(1,99).optionalFieldOf("count",1).forGetter(GameData.StackDef::count),
            // Checked against the loading registries, then kept as JSON so the catalog can carry it to clients.
            Codec.PASSTHROUGH.validate(value->net.minecraft.core.component.DataComponentPatch.CODEC.parse(value).map(patch->value)).optionalFieldOf("components")
                    .forGetter(s->Optional.ofNullable(s.components()).map(json->new com.mojang.serialization.Dynamic<>(com.mojang.serialization.JsonOps.INSTANCE,com.google.gson.JsonParser.parseString(json))))
    ).apply(i,(item,count,components)->new GameData.StackDef(BuiltInRegistries.ITEM.getKey(item).toString(),count,
            components.map(value->value.convert(com.mojang.serialization.JsonOps.INSTANCE).getValue().toString()).filter(json->!json.equals("{}")).orElse(null))));
    public static final MapCodec<InfusionRecipeData> CODEC=RecordCodecBuilder.mapCodec(i->i.group(
            STACK.fieldOf("result").forGetter(InfusionRecipeData::result),
            Codec.intRange(1,100000).fieldOf("cost").forGetter(InfusionRecipeData::cost),
            INGREDIENT.listOf(1,6).fieldOf("ingredients").forGetter(InfusionRecipeData::ingredients),
            Codec.STRING.validate(s->s.equals("normal")||s.equals("dark")?DataResult.success(s):DataResult.error(()->"Mode must be normal or dark")).optionalFieldOf("mode","normal").forGetter(InfusionRecipeData::mode),
            Identifier.CODEC.optionalFieldOf("research").forGetter(InfusionRecipeData::research),
            Codec.intRange(-100000,100000).optionalFieldOf("priority",0).forGetter(InfusionRecipeData::priority)
    ).apply(i,InfusionRecipeData::new));
    public static final RecipeType<InfusionRecipeData> TYPE=new RecipeType<>() {public String toString(){return "thaumcraft2tp:infusion";}};
    public static final RecipeSerializer<InfusionRecipeData> SERIALIZER=new RecipeSerializer<>(CODEC,ByteBufCodecs.fromCodecWithRegistries(CODEC.codec()));
    public InfusionRecipeData {
        ingredients=List.copyOf(ingredients);
        if(mode.equals("dark")&&ingredients.size()>5)throw new IllegalArgumentException("Dark infusions have five input slots");
        if(BuiltInRegistries.ITEM.getValue(Identifier.parse(result.id()))==Items.AIR)throw new IllegalArgumentException("Infusion result cannot be empty");
    }
    public GameData.Infusion definition(Identifier id){return definition(id,research.flatMap(GameData::findProject).map(GameData.Project::index).orElse(-1));}
    public GameData.Infusion definition(Identifier id,int index){return new GameData.Infusion(id.toString(),result,cost,ingredients,mode.equals("dark"),index,research.map(Identifier::toString).orElse(null));}
    @Override public boolean matches(RecipeInput input,Level level){
        var stacks=new java.util.ArrayList<ItemStack>();for(int n=0;n<input.size();n++)stacks.add(input.getItem(n));
        return GameData.allocateNormal(definition(Identifier.parse("thaumcraft2tp:matching")),stacks,input.size())!=null;
    }
    @Override public ItemStack assemble(RecipeInput input){return result.create();}
    @Override public boolean isSpecial(){return true;}
    @Override public boolean showNotification(){return false;}
    @Override public String group(){return "";}
    @Override public RecipeSerializer<InfusionRecipeData> getSerializer(){return SERIALIZER;}
    @Override public RecipeType<InfusionRecipeData> getType(){return TYPE;}
    @Override public PlacementInfo placementInfo(){return PlacementInfo.NOT_PLACEABLE;}
    @Override public RecipeBookCategory recipeBookCategory(){return RecipeBookCategories.CRAFTING_MISC;}
}
