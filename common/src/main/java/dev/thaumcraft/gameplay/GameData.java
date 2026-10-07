package dev.thaumcraft.gameplay;

import com.google.gson.Gson;
import dev.thaumcraft.Thaumcraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class GameData {
    /** {@code components} is a JSON-encoded data component patch, or null. */
    public record StackDef(String id,int count,String components) {
        public StackDef(String id,int count){this(id,count,null);}
        public ItemStack create() {
            if(count<1||count>99)throw new IllegalArgumentException("Invalid result count "+count+" for "+id);
            var key=Identifier.parse(id);
            var item=BuiltInRegistries.ITEM.getValue(key);
            if (item==null || item==Items.AIR) throw new IllegalStateException("Missing recipe item "+id);
            var stack=new ItemStack(item,count);
            if(components!=null)stack.applyComponents(patch(components));
            return stack;
        }
    }
    private static final Map<String,net.minecraft.core.component.DataComponentPatch> PATCHES=new java.util.concurrent.ConcurrentHashMap<>();
    /** Decodes with the current side's registries, which enchantments and other data-driven components need. */
    private static net.minecraft.core.component.DataComponentPatch patch(String json){
        var registries=AddonData.registries();
        if(registries==null)throw new IllegalStateException("Result components need a loaded world");
        return PATCHES.computeIfAbsent(json,text->net.minecraft.core.component.DataComponentPatch.CODEC.parse(registries.createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE),com.google.gson.JsonParser.parseString(text)).getOrThrow(IllegalArgumentException::new));
    }
    static void invalidatePatches(){PATCHES.clear();}
    /** Enchanting and research power of one block around a machine. */
    public record Booster(String block,int enchanting,int researchSpeed,int researchBonus,double failureProtection) implements dev.thaumcraft.api.Booster {}
    /** {@code material} sets the result's {@link dev.thaumcraft.content.TaintBlock#MATERIAL}, or null for its default. */
    public record TaintRule(String block,String result,Integer material) {}
    public record TaintEntityRule(String entity,String result) {}
    /** One treasure item: a fixed item or a random pick from {@code oneOf}, then a count from min through max. */
    public record TreasureItem(List<String> oneOf,int min,int max) {
        /** Draws in the original order: the pick, then the count, each only when it varies. */
        public ItemStack create(net.minecraft.util.RandomSource random){
            String id=oneOf.size()==1?oneOf.getFirst():oneOf.get(random.nextInt(oneOf.size()));
            int count=max>min?min+random.nextInt(max-min+1):min;
            return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(id)),count);
        }
    }
    /** Appends {@code items}, in order, {@code repeat} times. */
    public record TreasureEntry(int repeat,List<TreasureItem> items) {}
    /** Each empty slot receives an item with probability 1 in {@code fillOneIn}. */
    public record TreasurePool(int fillOneIn,List<TreasureEntry> entries) {}
    public record Infusion(String id,StackDef result,int cost,List<String> ingredients,boolean dark,int research,String researchKey) {
        public Infusion(StackDef result,int cost,List<String> ingredients,boolean dark,int research){this(result.id(),result,cost,ingredients,dark,research,null);}
        public Infusion(String id,StackDef result,int cost,List<String> ingredients,boolean dark,int research){this(id,result,cost,ingredients,dark,research,null);}
        public Identifier key(){return Identifier.parse(id);}
        public Identifier requiredResearch(){return researchKey!=null?Identifier.parse(researchKey):research<0?null:researchId(research);}
    }
    public record Vis(String ingredient,float value) {}
    public record RestorerCost(String ingredient,float cost) {
        public RestorerCost {if(!Float.isFinite(cost)||cost<0||cost>100000)throw new IllegalArgumentException("Invalid restorer cost "+cost);}
    }
    public record Project(String id,int index,int category,int difficulty,int type,String name,String text,boolean restricted,
                          List<Integer> prerequisites,String discovery,StackDef result,Integer steps,String name_key,String text_key) {
        public Project(String id,int index,int category,int difficulty,int type,String name,String text,boolean restricted,List<Integer> prerequisites,String discovery,StackDef result,Integer steps){this(id,index,category,difficulty,type,name,text,restricted,prerequisites,discovery,result,steps,null,null);}
        public Project {if(steps==null)steps=5;if(steps<1||steps>32767)throw new IllegalArgumentException("Research steps must be between 1 and 32767");validateTranslationKey(name_key);validateTranslationKey(text_key);}
        public Component nameComponent(){return translated(name_key,name);}
        public Component descriptionComponent(){return Component.translatableWithFallback(text_key==null?text:text_key,text.replace("\\n","\n"));}
        public Identifier key(){return Identifier.parse(id);}
        public List<Identifier> prerequisiteIds(){return prerequisites.stream().map(GameData::researchId).toList();}
    }
    /** Optional metadata never changes the legacy literal-or-key fallback contract. */
    static void validateTranslationKey(String key){
        if(key!=null&&(key.isBlank()||key.length()>256||key.chars().anyMatch(c->Character.isWhitespace(c)||Character.isISOControl(c))))throw new IllegalArgumentException("Translation key must contain 1 to 256 non-whitespace characters");
    }
    static Component translated(String key,String fallback){return Component.translatableWithFallback(key==null?fallback:key,fallback);}
    /** {@code special_chance} is the percent chance per successful roll of an even pick among eligible specials; null keeps the original per-project 12/33 restricted, 1/33 otherwise. */
    public record ResearchItem(String ingredient,int value,int category,List<Integer> special,Integer special_chance) {}
    public record Craft(String id,int research,boolean shaped,List<String> pattern,Map<String,String> key,List<String> ingredients,StackDef result) {}
    private record Definitions(List<Infusion> infusion,List<Vis> vis,List<Project> research,
                               List<ResearchItem> research_items,Map<String,Integer> crafting_research) {}
    private static final Definitions DATA;
    private static final List<Craft> CRAFTS;
    private static final List<Identifier> LEGACY_RESEARCH_IDS;

    static {
        try(var in=GameData.class.getResourceAsStream("/thaumcraft2tp/gameplay.json")) {
            if(in==null) throw new IllegalStateException("Missing gameplay definitions");
            DATA=new Gson().fromJson(new InputStreamReader(in,StandardCharsets.UTF_8),Definitions.class);
        } catch(Exception e) {throw new ExceptionInInitializerError(e);}
        try(var in=GameData.class.getResourceAsStream("/thaumcraft2tp/legacy_research_ids.json")) {
            if(in==null)throw new IllegalStateException("Missing legacy research ID mapping");
            LEGACY_RESEARCH_IDS=Arrays.stream(new Gson().fromJson(new InputStreamReader(in,StandardCharsets.UTF_8),String[].class)).map(Identifier::parse).toList();
        } catch(Exception e) {throw new ExceptionInInitializerError(e);}
        try(var in=GameData.class.getResourceAsStream("/thaumcraft2tp/crafting.json")) {
            if(in==null)throw new IllegalStateException("Missing crafting reference");
            CRAFTS=List.of(new Gson().fromJson(new InputStreamReader(in,StandardCharsets.UTF_8),Craft[].class));
        } catch(Exception e) {throw new ExceptionInInitializerError(e);}
    }
    private GameData() {}

    static List<Project> bundledProjects(){return DATA.research();}
    static List<Infusion> bundledInfusions(){return DATA.infusion();}
    static List<Vis> bundledVis(){return DATA.vis();}
    static List<ResearchItem> bundledResearchItems(){return DATA.research_items();}
    static Map<String,Integer> bundledLocks(){return DATA.crafting_research();}
    public static List<Project> projects() {return AddonData.current().projects();}
    public static List<Infusion> infusions() {return AddonData.current().infusions();}
    static List<Craft> bundledCrafts(){return CRAFTS;}
    public static List<Craft> crafts() {return AddonData.current().crafts();}
    public static Project project(int id) {
        return project(researchId(id));
    }
    public static Project project(Identifier id) {return findProject(id).orElseThrow(()->new IllegalArgumentException("Unknown research "+id));}
    public static Optional<Project> findProject(Identifier id) {return Optional.ofNullable(AddonData.project(id));}
    /** Fixed import mapping, independent of catalog order. Unknown old IDs remain dormant in saves. */
    public static Identifier legacyResearchId(int index) {
        return index>=0&&index<LEGACY_RESEARCH_IDS.size()?LEGACY_RESEARCH_IDS.get(index):Thaumcraft.id("legacy_research/"+index);
    }
    public static Identifier parseResearchId(String value) {
        try{return legacyResearchId(Integer.parseInt(value));}catch(NumberFormatException ignored){return Identifier.tryParse(value);}
    }
    public static int craftingResearch(Identifier recipe) {return AddonData.current().locks().getOrDefault(recipe.toString(),-1);}
    public static Identifier researchId(int index){return index<70?legacyResearchId(index):AddonData.researchId(index);}
    public static void invalidateVis(){VIS_CACHE.clear();CLIENT_VIS_CACHE.clear();}

    // Rules are matched many times per tick; each string is parsed once per registry. Invalid rules still throw on use.
    private static final Map<String,Object> BLOCK_RULES=new java.util.concurrent.ConcurrentHashMap<>();
    private static final Map<String,Object> ENTITY_RULES=new java.util.concurrent.ConcurrentHashMap<>();
    private static final Map<String,Object> ITEM_RULES=new java.util.concurrent.ConcurrentHashMap<>();
    /** An {@link Identifier}, or a {@link TagKey} for rules starting with '#'. */
    private static <T> Object parsedRule(Map<String,Object> parsed,String rule,net.minecraft.resources.ResourceKey<? extends net.minecraft.core.Registry<T>> registry) {
        Object value=parsed.get(rule);
        if(value==null){
            value=rule.startsWith("#")?TagKey.create(registry,Identifier.parse(rule.substring(1))):Identifier.parse(rule);
            parsed.put(rule,value);
        }
        return value;
    }
    @SuppressWarnings("unchecked")
    public static boolean matches(String rule,net.minecraft.world.level.block.state.BlockState state) {
        var parsed=parsedRule(BLOCK_RULES,rule,Registries.BLOCK);
        if(parsed instanceof TagKey<?> tag)return state.is((TagKey<net.minecraft.world.level.block.Block>)tag);
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).equals(parsed);
    }
    @SuppressWarnings("unchecked")
    public static boolean matches(String rule,net.minecraft.world.entity.EntityType<?> type) {
        var parsed=parsedRule(ENTITY_RULES,rule,Registries.ENTITY_TYPE);
        if(parsed instanceof TagKey<?> tag)return BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(type).is((TagKey<net.minecraft.world.entity.EntityType<?>>)tag);
        return BuiltInRegistries.ENTITY_TYPE.getKey(type).equals(parsed);
    }
    /** Rules are in priority order; the first exact ID match wins over tag matches, as for vis values. */
    private static <T> T firstMatch(List<T> rules,java.util.function.Function<T,String> key,java.util.function.Predicate<String> test){
        T wildcard=null;
        for(T rule:rules)if(test.test(key.apply(rule))){if(!key.apply(rule).startsWith("#"))return rule;if(wildcard==null)wildcard=rule;}
        return wildcard;
    }
    public static Booster booster(net.minecraft.world.level.block.state.BlockState state){return state.isAir()?null:firstMatch(AddonData.current().boosters(),Booster::block,rule->matches(rule,state));}
    public static TaintRule taintRule(net.minecraft.world.level.block.state.BlockState state){return firstMatch(AddonData.current().taintBlocks(),TaintRule::block,rule->matches(rule,state));}
    public static boolean taintResult(net.minecraft.world.level.block.state.BlockState state){
        String id=BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        for(var rule:AddonData.current().taintBlocks())if(rule.result().equals(id))return true;
        return false;
    }
    public static TaintEntityRule taintEntityRule(net.minecraft.world.entity.EntityType<?> type){return firstMatch(AddonData.current().taintEntities(),TaintEntityRule::entity,rule->matches(rule,type));}
    public static float restorerCost(ItemStack stack){
        var rule=firstMatch(AddonData.current().restorerCosts(),RestorerCost::ingredient,key->matches(key,stack));
        return rule==null?.25f:rule.cost();
    }

    @SuppressWarnings("unchecked")
    public static boolean matches(String ingredient,ItemStack stack) {
        if(stack.isEmpty()) return false;
        var parsed=parsedRule(ITEM_RULES,ingredient,Registries.ITEM);
        if(parsed instanceof TagKey<?> tag) return stack.is((TagKey<net.minecraft.world.item.Item>)tag);
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(parsed);
    }

    private static final Map<Identifier,Float> CUSTOM_VIS=new java.util.concurrent.ConcurrentHashMap<>();
    private static final Map<Identifier,Float> VIS_CACHE=new java.util.concurrent.ConcurrentHashMap<>();
    private static final Map<Identifier,Float> CLIENT_VIS_CACHE=new java.util.concurrent.ConcurrentHashMap<>();
    private static Map<Identifier,Float> visCache(){return AddonData.isClient()?CLIENT_VIS_CACHE:VIS_CACHE;}
    private static volatile List<net.minecraft.world.item.crafting.RecipeHolder<?>> runtimeRecipes=List.of();
    private static volatile java.lang.ref.WeakReference<net.minecraft.world.item.crafting.RecipeManager> recipeManager=new java.lang.ref.WeakReference<>(null);
    private static Object recipeSnapshot;
    /** Register a namespaced equivalent of the original addSmelting API. */
    public static void registerVis(Identifier item,float value){
        if(!Float.isFinite(value)||value<0)throw new IllegalArgumentException("Vis value must be finite and nonnegative");
        CUSTOM_VIS.put(item,value);invalidateVis();
    }
    public static boolean unregisterVis(Identifier item){
        if(CUSTOM_VIS.remove(item)==null)return false;
        invalidateVis();return true;
    }
    /** Java overrides naming items that no loaded mod registered. They never match, so a typo would otherwise be silent. */
    public static List<Identifier> unknownVisOverrides(){return CUSTOM_VIS.keySet().stream().filter(id->!BuiltInRegistries.ITEM.containsKey(id)).sorted().toList();}
    public static void unbindRecipes(){recipeManager=new java.lang.ref.WeakReference<>(null);runtimeRecipes=List.of();recipeSnapshot=null;invalidateVis();}
    public static void bindRecipes(net.minecraft.world.item.crafting.RecipeManager manager){
        recipeManager=new java.lang.ref.WeakReference<>(manager);recipeSnapshot=null;refreshRecipes();
    }
    private static synchronized void refreshRecipes(){
        var manager=recipeManager.get();if(manager==null)return;
        var current=manager.getRecipes();if(current==recipeSnapshot)return;
        runtimeRecipes=List.copyOf(current);recipeSnapshot=current;VIS_CACHE.clear();
    }
    public static float vis(ItemStack stack) {
        if(stack.isEmpty())return 0;
        refreshRecipes();return vis(stack,true,new HashSet<>());
    }
    /** Original basiconly lookup: Fire foci must not consume recipe-derived values, even cached ones. */
    public static float basicVis(ItemStack stack) {
        return stack.isEmpty()?0:Objects.requireNonNullElse(registeredVis(stack),0f);
    }
    private static Float registeredVis(ItemStack stack) {
        Identifier id=BuiltInRegistries.ITEM.getKey(stack.getItem());
        if(CUSTOM_VIS.containsKey(id))return CUSTOM_VIS.get(id);
        // Exact entries take precedence over wildcard families, as in the original map.
        Float wildcard=null;
        for(Vis value:AddonData.current().vis())if(matches(value.ingredient(),stack)){
            if(!value.ingredient().startsWith("#"))return value.value();if(wildcard==null)wildcard=value.value();
        }
        if(wildcard!=null)return wildcard;
        float custom=dev.thaumcraft.PortConfig.customVis(stack);if(custom>0)return custom;
        return null;
    }
    private static float vis(ItemStack stack,boolean derive,Set<Identifier> visiting) {
        Float registered=registeredVis(stack);if(registered!=null)return registered;
        // Original recipe valuation matched damage metadata; worn gear cannot use pristine recipes or their cache.
        if(stack.isDamaged())return 0;
        Identifier id=BuiltInRegistries.ITEM.getKey(stack.getItem());
        if(visCache().containsKey(id))return visCache().get(id);
        if(!derive||visiting.size()>=32||!visiting.add(id))return 0;
        try {
            // Infusion includes its vis input and the value of every consumed component.
            for(Infusion recipe:infusions())if(recipe.result().components()==null&&recipe.result().id().equals(id.toString())){
                float value=recipe.cost();for(String ingredient:recipe.ingredients())value+=ingredientVis(ingredient,true,visiting);
                if(value>0&&Float.isFinite(value)){visCache().put(id,value);return value;}
            }
            // Original crafting only used registered/basic or previously cached component values.
            float crafted=-1;
            for(var holder:runtimeRecipes){
                var recipe=holder.value();net.minecraft.world.item.crafting.CraftingRecipe crafting;
                if(recipe instanceof net.minecraft.world.item.crafting.ShapedRecipe shaped)crafting=shaped;
                else if(recipe instanceof net.minecraft.world.item.crafting.ShapelessRecipe shapeless)crafting=shapeless;
                else continue;
                ItemStack result=staticRecipeResult(crafting);
                if(result.isEmpty()||!result.is(stack.getItem()))continue;
                float sum=0;
                for(var ingredient:crafting.placementInfo().ingredients()){
                    float value=(float)ingredient.items().mapToDouble(item->vis(new ItemStack(item.value()),false,visiting)).filter(v->v>0).min().orElse(0);
                    if(value==0){sum=0;break;}sum+=value/result.getCount();
                }
                crafted=sum;break;
            }
            // Stonecutting is a modern route: a stonecut stair must not be worth more than its one block.
            float cut=Float.MAX_VALUE;
            for(var holder:runtimeRecipes){
                if(!(holder.value() instanceof net.minecraft.world.item.crafting.StonecutterRecipe stonecutter))continue;
                ItemStack result=staticRecipeResult(stonecutter);
                if(result.isEmpty()||!result.is(stack.getItem()))continue;
                float value=(float)stonecutter.input().items().mapToDouble(item->vis(new ItemStack(item.value()),false,visiting)).filter(v->v>0).min().orElse(0);
                if(value>0)cut=Math.min(cut,value/result.getCount());
            }
            if(cut<Float.MAX_VALUE&&(crafted<=0||cut<crafted))crafted=cut;
            if(crafted==0&&cut==Float.MAX_VALUE)return 0;
            if(crafted>0){visCache().put(id,crafted);return crafted;}
            // Standalone clients can still inspect the bundled recipe reference without a server.
            for(Craft recipe:crafts())if(recipe.result().id().equals(id.toString())){
                List<String> inputs=new ArrayList<>();
                if(recipe.shaped())for(String row:recipe.pattern())for(int i=0;i<row.length();i++){String key=String.valueOf(row.charAt(i));if(!key.equals(" "))inputs.add(recipe.key().get(key));}
                else inputs.addAll(recipe.ingredients());
                float sum=0;for(String ingredient:inputs){float value=ingredientVis(ingredient,false,visiting);if(value<=0)return 0;sum+=value/recipe.result().count();}
                if(sum>0){visCache().put(id,sum);return sum;}
            }
            return 0;
        } finally {visiting.remove(id);}
    }
    /** Read declared outputs; assembling modded recipes requires a real input inventory. */
    private static ItemStack staticRecipeResult(net.minecraft.world.item.crafting.Recipe<?> recipe){
        for(var display:recipe.display())
            if(display.result() instanceof net.minecraft.world.item.crafting.display.SlotDisplay.ItemStackSlotDisplay output&&output.stack()!=null)
                return output.stack().create();
        return ItemStack.EMPTY;
    }
    private static float ingredientVis(String ingredient,boolean derive,Set<Identifier> visiting){
        if(!ingredient.startsWith("#")){var item=BuiltInRegistries.ITEM.getValue(Identifier.parse(ingredient));return item==null||item==Items.AIR?0:vis(new ItemStack(item),derive,visiting);}
        var tag=TagKey.create(Registries.ITEM,Identifier.parse(ingredient.substring(1)));
        return (float)BuiltInRegistries.ITEM.get(tag).stream().flatMap(set->set.stream()).mapToDouble(item->vis(new ItemStack(item.value()),derive,visiting)).filter(v->v>0).min().orElse(0);
    }

    public static ResearchItem researchItem(ItemStack stack) {
        ResearchItem wildcard=null;
        for(ResearchItem item:AddonData.current().researchItems()) if(matches(item.ingredient(),stack)) {
            if(!item.ingredient().startsWith("#")) return item;
            if(wildcard==null)wildcard=item;
        }
        return wildcard;
    }

    /** Allocate a distinct unit for every ingredient, including repeated buckets. */
    public static int[] allocate(Infusion recipe,List<ItemStack> inventory,int inputSlots) {
        return allocate(recipe,inventory,inputSlots,false);
    }
    /** Original normal infusion takes at most one unit from each occupied slot, in recipe order. */
    public static int[] allocateNormal(Infusion recipe,List<ItemStack> inventory,int inputSlots) {
        return allocate(recipe,inventory,inputSlots,true);
    }
    private static int[] allocate(Infusion recipe,List<ItemStack> inventory,int inputSlots,boolean singlePerSlot) {
        int[] remaining=new int[inputSlots],used=new int[inputSlots];
        for(int i=0;i<inputSlots;i++) remaining[i]=singlePerSlot?(inventory.get(i).isEmpty()?0:1):inventory.get(i).getCount();
        return allocate(recipe.ingredients(),0,inventory,remaining,used)?used:null;
    }
    // Infusions have at most six ingredients. Backtracking handles overlapping tags without depending on slot order.
    private static boolean allocate(List<String> ingredients,int index,List<ItemStack> inventory,int[] remaining,int[] used){
        if(index==ingredients.size()){
            for(int slot=0;slot<used.length;slot++)if(!inventory.get(slot).isEmpty()&&used[slot]==0)return false;
            return true;
        }
        for(int slot=0;slot<remaining.length;slot++)if(remaining[slot]>0&&matches(ingredients.get(index),inventory.get(slot))){
            remaining[slot]--;used[slot]++;
            if(allocate(ingredients,index+1,inventory,remaining,used))return true;
            remaining[slot]++;used[slot]--;
        }
        return false;
    }

    public static void validate() {
        var recipeIds=new HashSet<Identifier>();
        for(Infusion recipe:infusions()) {
            if(!recipeIds.add(recipe.key()))throw new IllegalStateException("Duplicate infusion ID "+recipe.id());
            recipe.result().create();
            if(recipe.requiredResearch()!=null)project(recipe.requiredResearch());
        }
        for(Project project:projects()) {
            if(project.index()<70&&!legacyResearchId(project.index()).equals(project.key()))throw new IllegalStateException("Changed legacy research mapping for "+project.index());
            project.result().create();
            for(int prerequisite:project.prerequisites()) project(prerequisite);
        }
        Thaumcraft.LOG.info("Loaded {} research projects and {} infusion recipes",projects().size(),infusions().size());
    }
}
