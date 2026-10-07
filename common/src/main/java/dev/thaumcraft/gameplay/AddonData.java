package dev.thaumcraft.gameplay;

import com.google.gson.*;
import dev.thaumcraft.Thaumcraft;
import dev.thaumcraft.api.ThaumcraftApi;
import dev.thaumcraft.api.ThaumcraftEvents;
import dev.thaumcraft.recipe.InfusionRecipeData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A complete, validated snapshot. Readers never see a partially reloaded catalog. Invalid or unresolved addon
 * definitions are skipped with a warning, together with everything that depends on them.
 */
public final class AddonData {
    /** Addon categories may name item models for their fragments, theories and discoveries; null keeps the default look. */
    public record Category(String id,String name,int order,String name_key,String fragment_model,String theory_model,String discovery_model) {
        public Category(String id,String name,int order){this(id,name,order,null);}
        public Category(String id,String name,int order,String name_key){this(id,name,order,name_key,null,null,null);}
        public Category {
            GameData.validateTranslationKey(name_key);
            for(String model:new String[]{fragment_model,theory_model,discovery_model})if(model!=null)Identifier.parse(model);
        }
        public net.minecraft.network.chat.Component nameComponent(){return GameData.translated(name_key,name);}
    }
    public record Snapshot(List<GameData.Project> projects,List<GameData.Infusion> infusions,List<GameData.Vis> vis,
                           List<GameData.ResearchItem> researchItems,Map<String,Integer> locks,List<Category> categories,List<GameData.Craft> crafts,
                           List<GameData.Booster> boosters,List<GameData.TaintRule> taintBlocks,List<GameData.TaintEntityRule> taintEntities,
                           Map<String,GameData.TreasurePool> treasure,List<GameData.RestorerCost> restorerCosts) {
        public Snapshot(List<GameData.Project> projects,List<GameData.Infusion> infusions,List<GameData.Vis> vis,List<GameData.ResearchItem> researchItems,Map<String,Integer> locks,List<Category> categories){this(projects,infusions,vis,researchItems,locks,categories,GameData.bundledCrafts(),BASE_BOOSTERS,List.of(),List.of(),BASE_TREASURE,BASE_RESTORER_COSTS);}
        public Snapshot {
            projects=List.copyOf(projects);infusions=List.copyOf(infusions);vis=List.copyOf(vis);
            researchItems=List.copyOf(researchItems);locks=Map.copyOf(locks);categories=List.copyOf(categories);crafts=List.copyOf(crafts);
            boosters=boosters==null?List.of():List.copyOf(boosters);taintBlocks=taintBlocks==null?List.of():List.copyOf(taintBlocks);taintEntities=taintEntities==null?List.of():List.copyOf(taintEntities);
            treasure=treasure==null?Map.of():Map.copyOf(treasure);
            restorerCosts=restorerCosts==null?List.of():List.copyOf(restorerCosts);
        }
        public Map<Identifier,GameData.Project> byId(){var map=new HashMap<Identifier,GameData.Project>();for(var p:projects)map.put(p.key(),p);return map;}
    }
    /** Lock whose research is unavailable. It denies everyone rather than silently opening the recipe. */
    public static final int UNAVAILABLE=-2;
    public static final Identifier UNAVAILABLE_ID=Thaumcraft.id("unavailable");
    private static final Gson GSON=new Gson();
    private static final List<Category> BASE_CATEGORIES=List.of(new Category("thaumcraft2tp:lost","Lost",0,"category.thaumcraft2tp.lost"),new Category("thaumcraft2tp:forbidden","Forbidden",1,"category.thaumcraft2tp.forbidden"),new Category("thaumcraft2tp:tainted","Tainted",2,"category.thaumcraft2tp.tainted"),new Category("thaumcraft2tp:eldritch","Eldritch",3,"category.thaumcraft2tp.eldritch"));
    private static final List<GameData.Booster> BASE_BOOSTERS=List.of(new GameData.Booster("minecraft:bookshelf",1,2,1,.25),new GameData.Booster("thaumcraft2tp:brain_in_a_jar",4,4,2,1));
    /** Treasure pools and their original fill odds. */
    public static final Map<String,Integer> TREASURE_POOLS=Map.of("thaumcraft2tp:chest",3,"thaumcraft2tp:eldritch",6);
    private static final Map<String,JsonObject> TREASURE_SEED=treasureSeed();
    private static final Map<String,GameData.TreasurePool> BASE_TREASURE=treasurePools(TREASURE_SEED.values().stream().map(j->treasure(j,false)).toList());
    private static final List<GameData.RestorerCost> BASE_RESTORER_COSTS=restorerSeeds();
    private static final Snapshot BUNDLED=new Snapshot(GameData.bundledProjects(),GameData.bundledInfusions(),GameData.bundledVis(),GameData.bundledResearchItems(),GameData.bundledLocks(),BASE_CATEGORIES);
    private static final Set<String> COMMON_KEYS=Set.of("remove","required_mods");
    private static final Map<String,Set<String>> KEYS=Map.of(
            "research_categories",Set.of("name","name_key","order","fragment_model","theory_model","discovery_model"),
            "research",Set.of("name","name_key","text","text_key","category","difficulty","steps","prerequisites","restricted","discovery","result","type"),
            "research_sources",Set.of("ingredient","value","category","special","special_chance","priority"),
            "vis",Set.of("ingredient","value","priority"),
            "craft_requirements",Set.of("research"),
            "boosters",Set.of("block","enchanting","research_speed","research_bonus","failure_protection","priority"),
            "taint_blocks",Set.of("block","result","material","priority"),
            "taint_entities",Set.of("entity","result","priority"),
            "treasure",Set.of("pool","fill_one_in","entries","priority"),
            "restorer_costs",Set.of("ingredient","cost","priority"));
    public record Catalog(Snapshot snapshot,Map<Identifier,GameData.Project> projects,Map<Integer,Identifier> ids,Map<Identifier,Integer> requirements) {
        Catalog(Snapshot snapshot){this(snapshot,Map.copyOf(snapshot.byId()),snapshot.projects().stream().collect(java.util.stream.Collectors.toUnmodifiableMap(GameData.Project::index,GameData.Project::key)),requirements(snapshot));}
        private static Map<Identifier,Integer> requirements(Snapshot snapshot){
            var result=new HashMap<Identifier,Integer>();for(var recipe:snapshot.infusions())if(recipe.research()>=0)result.put(recipe.key(),recipe.research());
            snapshot.locks().forEach((id,index)->result.put(Identifier.parse(id),index));return Map.copyOf(result);
        }
    }
    private static volatile Catalog server=new Catalog(BUNDLED),client=new Catalog(BUNDLED);
    private static volatile Map<String,String> signatures=Map.of();
    private static Object loadedRecipes;
    private static Object loadedResources;
    public static BooleanSupplier clientThread=()->false;
    /** The connected client's registries, or null. */
    public static Supplier<net.minecraft.core.HolderLookup.Provider> clientRegistries=()->null;
    private static volatile net.minecraft.core.HolderLookup.Provider serverRegistries;
    public static Runnable clientChanged=()->{};
    /** Explicit side for API calls, overriding the thread-based default for the duration of {@link #on}. */
    private static final ThreadLocal<Boolean> SIDE=new ThreadLocal<>();
    private AddonData() {}
    public static boolean isClient(){Boolean side=SIDE.get();return side!=null?side:clientThread.getAsBoolean();}
    public static <T> T on(boolean client,Supplier<T> action){
        Boolean previous=SIDE.get();SIDE.set(client);
        try{return action.get();}finally{if(previous==null)SIDE.remove();else SIDE.set(previous);}
    }
    public static Catalog catalog(boolean client){return client?AddonData.client:server;}
    public static Snapshot current(){return catalog(isClient()).snapshot();}
    public static Snapshot server(){return server.snapshot();}
    public static GameData.Project project(Identifier id){return catalog(isClient()).projects().get(id);}
    public static Identifier researchId(int index){return catalog(isClient()).ids().getOrDefault(index,GameData.legacyResearchId(index));}
    public static int requirement(Identifier recipe){return catalog(isClient()).requirements().getOrDefault(recipe,-1);}
    /** Registries for decoding result components: the side's own when available, else the other side's. */
    static net.minecraft.core.HolderLookup.Provider registries(){
        var client=clientRegistries.get();
        return isClient()?(client!=null?client:serverRegistries):(serverRegistries!=null?serverRegistries:client);
    }
    public static String infusionSignature(String id){return signatures.get(id);}
    private static void refreshSignatures(Snapshot snapshot){var values=new HashMap<String,String>();for(var recipe:snapshot.infusions())values.put(recipe.id(),signature(recipe));signatures=Map.copyOf(values);}
    private static String signature(GameData.Infusion recipe){
        StringBuilder text=new StringBuilder(GSON.toJson(recipe));
        for(String ingredient:recipe.ingredients())if(ingredient.startsWith("#")){
            var tag=net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,Identifier.parse(ingredient.substring(1)));
            net.minecraft.core.registries.BuiltInRegistries.ITEM.get(tag).stream().flatMap(set->set.stream()).map(holder->net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(holder.value()).toString()).sorted().forEach(id->text.append("|").append(id));
        }
        try{return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(text.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)));}catch(java.security.NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}
    }
    public static void clearServer(){serverRegistries=null;GameData.invalidatePatches();server=new Catalog(BUNDLED);signatures=Map.of();loadedRecipes=null;loadedResources=null;GameData.unbindRecipes();}
    public static void clearClient(){client=new Catalog(BUNDLED);GameData.invalidatePatches();clientUpdated();}
    public static String encode(Snapshot snapshot){return GSON.toJson(snapshot);}
    public static void receive(String json){
        var encoded=JsonParser.parseString(json).getAsJsonObject();
        for(var project:encoded.getAsJsonArray("projects")){translationKey(project.getAsJsonObject(),"name_key");translationKey(project.getAsJsonObject(),"text_key");}
        for(var category:encoded.getAsJsonArray("categories"))translationKey(category.getAsJsonObject(),"name_key");
        Snapshot next=GSON.fromJson(encoded,Snapshot.class);
        GameData.invalidatePatches();validate(next);
        client=new Catalog(next);clientUpdated();
    }
    private static void clientUpdated(){
        GameData.invalidateVis();clientChanged.run();
        ThaumcraftEvents.CLIENT_CATALOG_CHANGED.post(listener->listener.onChanged(ThaumcraftApi.client()));
    }
    public static void sync(ServerPlayer player){
        try{dev.thaumcraft.network.CatalogSync.send(player,encode(server.snapshot()));}
        catch(RuntimeException error){Thaumcraft.LOG.warn("Cannot synchronize addon catalog to {}",player.getGameProfile().name(),error);}
    }
    public static boolean reload(MinecraftServer minecraft,boolean startup){
        var recipes=minecraft.getRecipeManager().getRecipes();var resources=minecraft.getResourceManager();
        if(recipes==loadedRecipes&&resources==loadedResources)return true;
        try {
            var skipped=new ArrayList<String>();
            serverRegistries=minecraft.registryAccess();GameData.invalidatePatches();
            Snapshot next=load(minecraft,skipped);
            validate(next);
            // Check transport size before committing so every joining client can receive this catalog.
            dev.thaumcraft.network.CatalogSync.validate(encode(next));
            refreshSignatures(next);server=new Catalog(next);loadedRecipes=recipes;loadedResources=resources;GameData.bindRecipes(minecraft.getRecipeManager());
            for(var level:minecraft.getAllLevels())for(var player:level.players())sync(player);
            Thaumcraft.LOG.info("Loaded addon catalog: {} research, {} infusions, {} categories, {} boosters, {} taint rules",next.projects().size(),next.infusions().size(),next.categories().size(),next.boosters().size(),next.taintBlocks().size()+next.taintEntities().size());
            if(!skipped.isEmpty())Thaumcraft.LOG.warn("Addon catalog skipped {} definitions; see the warnings above: {}",skipped.size(),skipped);
            for(var item:GameData.unknownVisOverrides())Thaumcraft.LOG.warn("ThaumcraftApi.registerVis names unregistered item {}; the value is unused",item);
            ThaumcraftEvents.CATALOG_RELOADED.post(listener->listener.onReloaded(minecraft,ThaumcraftApi.server()));
            return true;
        }catch(RuntimeException error){
            if(startup)throw error;
            refreshSignatures(server.snapshot());GameData.invalidateVis();
            for(var player:minecraft.getPlayerList().getPlayers())sync(player);
            Thaumcraft.LOG.error("Addon catalog reload rejected; keeping previous definitions",error);return false;
        }
    }
    private static String message(Throwable error){
        String text=error.getMessage();
        for(Throwable cause=error.getCause();cause!=null;cause=cause.getCause())if(cause.getMessage()!=null)text=(text==null?"":text+": ")+cause.getMessage();
        return text==null?error.toString():text;
    }
    private static void skip(List<String> skipped,String kind,String id,String reason){skipped.add(kind+" "+id);Thaumcraft.LOG.warn("Skipping {} {}: {}",kind,id,reason);}
    /** Parses one definition. An invalid override of a built-in falls back to the built-in; anything else is skipped. */
    private static <T> T parse(String kind,String id,JsonObject json,JsonObject builtIn,Function<JsonObject,T> parser,List<String> skipped){
        try{return parser.apply(json);}
        catch(RuntimeException error){
            if(builtIn!=null&&builtIn!=json)try{
                T value=parser.apply(builtIn);
                Thaumcraft.LOG.warn("Invalid override of {} {}; keeping the built-in definition: {}",kind,id,message(error));skipped.add(kind+" "+id);return value;
            }catch(RuntimeException ignored){}
            skip(skipped,kind,id,message(error));return null;
        }
    }
    private static boolean modsPresent(JsonObject value){for(String mod:strings(value,"required_mods"))if(!Thaumcraft.modLoaded.test(mod))return false;return true;}
    private static Map<String,JsonObject> read(MinecraftServer minecraft,String folder,Map<String,JsonObject> seed,List<String> skipped){
        var result=new TreeMap<>(seed);String prefix="thaumcraft2tp/"+folder+"/";
        for(var entry:minecraft.getResourceManager().listResources("thaumcraft2tp/"+folder,id->id.getPath().endsWith(".json")).entrySet()){
            var file=entry.getKey();String path=file.getPath();String id=Identifier.fromNamespaceAndPath(file.getNamespace(),path.substring(prefix.length(),path.length()-5)).toString();
            try(var input=entry.getValue().open()){
                byte[] bytes=input.readNBytes(131073);
                if(bytes.length>131072)throw new IllegalArgumentException("Definition exceeds 128 KiB");
                var value=JsonParser.parseString(new String(bytes,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                if(!modsPresent(value)){Thaumcraft.LOG.debug("Ignoring {} {}: a required mod is absent",folder,id);continue;}
                for(String key:value.keySet())if(!COMMON_KEYS.contains(key)&&!KEYS.get(folder).contains(key))Thaumcraft.LOG.warn("Unknown field \"{}\" in {} {} is ignored",key,folder,id);
                if(bool(value,"remove",false))result.remove(id);else result.put(id,value);
            }catch(Exception error){skip(skipped,folder,id+" ("+file+")",message(error));}
        }
        if(result.size()>4096)throw new IllegalArgumentException("Too many "+folder+" definitions");
        return result;
    }
    private static JsonObject object(Object value){return GSON.toJsonTree(value).getAsJsonObject();}
    private static String string(JsonObject value,String key){if(!value.has(key))throw new IllegalArgumentException("Missing "+key);return value.get(key).getAsString();}
    private static String string(JsonObject value,String key,String fallback){return value.has(key)?value.get(key).getAsString():fallback;}
    private static String translationKey(JsonObject value,String key){
        if(!value.has(key))return null;
        var element=value.get(key);
        if(!element.isJsonPrimitive()||!element.getAsJsonPrimitive().isString())throw new IllegalArgumentException(key+" must be a string");
        String result=element.getAsString();GameData.validateTranslationKey(result);return result;
    }
    private static String model(JsonObject value,String key){
        if(!value.has(key))return null;
        var element=value.get(key);
        if(!element.isJsonPrimitive()||!element.getAsJsonPrimitive().isString())throw new IllegalArgumentException(key+" must be an item model ID");
        return Identifier.parse(element.getAsString()).toString();
    }
    private static boolean bool(JsonObject value,String key,boolean fallback){
        if(!value.has(key))return fallback;var element=value.get(key);
        if(!element.isJsonPrimitive()||!element.getAsJsonPrimitive().isBoolean())throw new IllegalArgumentException(key+" must be true or false");
        return element.getAsBoolean();
    }
    private static int number(JsonObject value,String key,int fallback,int min,int max){
        int n=fallback;
        if(value.has(key)){
            var element=value.get(key);
            if(!element.isJsonPrimitive()||!element.getAsJsonPrimitive().isNumber())throw new IllegalArgumentException(key+" must be a number");
            try{n=element.getAsBigDecimal().intValueExact();}catch(ArithmeticException error){throw new IllegalArgumentException(key+" must be an integer");}
        }
        if(n<min||n>max)throw new IllegalArgumentException("Invalid "+key+": "+n);return n;
    }
    private static double decimal(JsonObject value,String key,double min,double max){
        if(!value.has(key))return 0;var element=value.get(key);
        if(!element.isJsonPrimitive()||!element.getAsJsonPrimitive().isNumber())throw new IllegalArgumentException(key+" must be a number");
        double n=element.getAsDouble();if(!(n>=min&&n<=max))throw new IllegalArgumentException("Invalid "+key+": "+n);return n;
    }
    private static List<String> strings(JsonObject value,String key){if(!value.has(key))return List.of();var list=new ArrayList<String>();for(var e:value.getAsJsonArray(key))list.add(e.getAsString());return List.copyOf(list);}
    private static int reference(Map<String,Integer> ids,String kind,String id){Integer index=ids.get(id);if(index==null)throw new IllegalArgumentException("Unknown "+kind+" "+id);return index;}
    private static String ingredient(JsonObject value){String ingredient=string(value,"ingredient");InfusionRecipeData.INGREDIENT.parse(com.mojang.serialization.JsonOps.INSTANCE,new JsonPrimitive(ingredient)).getOrThrow();return ingredient;}
    private record Candidate(String id,String category,int difficulty,int type,String name,String text,boolean restricted,List<String> prerequisites,String discovery,GameData.StackDef result,int steps,String name_key,String text_key) {}
    private static Candidate candidate(String id,JsonObject j){
        var result=j.has("result")?GSON.fromJson(j.get("result"),GameData.StackDef.class):new GameData.StackDef("minecraft:enchanted_book",1);result.create();
        String discovery=string(j,"discovery","thaumcraft2tp:discovery_generic");
        var entry=dev.thaumcraft.content.Content.entry(new GameData.StackDef(discovery,1).create());
        if(entry==null||!entry.source_class().equals("itemDiscovery"))throw new IllegalArgumentException("discovery must be a Thaumcraft discovery item: "+discovery);
        var prerequisites=strings(j,"prerequisites");for(String prerequisite:prerequisites)Identifier.parse(prerequisite);
        return new Candidate(id,string(j,"category"),number(j,"difficulty",0,0,5),number(j,"type",0,0,4),string(j,"name"),string(j,"text",""),bool(j,"restricted",false),prerequisites,discovery,result,number(j,"steps",5,1,32767),translationKey(j,"name_key"),translationKey(j,"text_key"));
    }
    private record Rule<T>(String id,int priority,T value) {}
    /** Parses value rules and orders them by descending priority, then ascending rule ID. */
    private static <T> List<T> rules(Map<String,JsonObject> files,Map<String,JsonObject> seed,String kind,List<String> skipped,Function<JsonObject,T> parser){
        var rules=new ArrayList<Rule<T>>();
        files.forEach((id,json)->{
            int fallback=id.startsWith("thaumcraft2tp:legacy/")?-100000:0;
            var rule=parse(kind,id,json,seed.get(id),j->new Rule<>(id,number(j,"priority",fallback,-100000,100000),parser.apply(j)),skipped);
            if(rule!=null)rules.add(rule);
        });
        rules.sort(Comparator.<Rule<T>>comparingInt(r->-r.priority()).thenComparing(Rule::id));
        return rules.stream().map(Rule::value).toList();
    }
    private static Snapshot load(MinecraftServer minecraft,List<String> skipped){
        var categorySeed=new LinkedHashMap<String,JsonObject>();for(var c:BASE_CATEGORIES)categorySeed.put(c.id(),object(c));
        var categoryFiles=read(minecraft,"research_categories",categorySeed,skipped);
        var categories=new ArrayList<Category>();
        for(var c:BASE_CATEGORIES){
            var json=categoryFiles.remove(c.id());
            if(json==null){Thaumcraft.LOG.warn("Built-in category {} cannot be removed; keeping it",c.id());json=categorySeed.get(c.id());}
            categories.add(parse("category",c.id(),json,categorySeed.get(c.id()),j->new Category(c.id(),string(j,"name"),c.order(),translationKey(j,"name_key")),skipped));
        }
        var addonCategories=new ArrayList<Category>();
        categoryFiles.forEach((id,json)->{var category=parse("category",id,json,null,j->new Category(id,string(j,"name"),number(j,"order",0,-100000,100000),translationKey(j,"name_key"),model(j,"fragment_model"),model(j,"theory_model"),model(j,"discovery_model")),skipped);if(category!=null)addonCategories.add(category);});
        addonCategories.sort(Comparator.comparingInt(Category::order).thenComparing(Category::id));
        while(categories.size()+addonCategories.size()>64)skip(skipped,"category",addonCategories.removeLast().id(),"at most 64 research categories are supported");
        categories.addAll(addonCategories);
        var categoryIds=new HashMap<String,Integer>();for(int i=0;i<categories.size();i++)categoryIds.put(categories.get(i).id(),i);

        var projectSeed=new LinkedHashMap<String,JsonObject>();
        for(var p:BUNDLED.projects()){
            var json=object(p);json.addProperty("category",BASE_CATEGORIES.get(p.category()).id());
            var prerequisites=new JsonArray();for(int index:p.prerequisites())prerequisites.add(GameData.legacyResearchId(index).toString());json.add("prerequisites",prerequisites);projectSeed.put(p.id(),json);
        }
        var candidates=new LinkedHashMap<String,Candidate>();
        read(minecraft,"research",projectSeed,skipped).forEach((id,json)->{var candidate=parse("research",id,json,projectSeed.get(id),j->candidate(id,j),skipped);if(candidate!=null)candidates.put(id,candidate);});
        // Drop research whose category or prerequisites are gone, repeating until dependents settle.
        for(boolean changed=true;changed;){
            changed=false;
            for(var iterator=candidates.values().iterator();iterator.hasNext();){
                var c=iterator.next();
                String missing=!categoryIds.containsKey(c.category())?"unknown category "+c.category():c.prerequisites().stream().filter(p->!candidates.containsKey(p)).findFirst().map(p->"missing prerequisite "+p).orElse(null);
                if(missing!=null){iterator.remove();skip(skipped,"research",c.id(),missing);changed=true;}
            }
        }
        var resolved=new HashSet<String>();
        for(boolean progress=true;progress;){progress=false;for(var c:candidates.values())if(!resolved.contains(c.id())&&resolved.containsAll(c.prerequisites())){resolved.add(c.id());progress=true;}}
        for(var iterator=candidates.values().iterator();iterator.hasNext();){var c=iterator.next();if(!resolved.contains(c.id())){iterator.remove();skip(skipped,"research",c.id(),"prerequisite cycle, or depends on one");}}
        var projectIds=new HashMap<String,Integer>();
        for(int i=0;i<70;i++){String id=GameData.legacyResearchId(i).toString();if(candidates.containsKey(id))projectIds.put(id,i);}
        int nextIndex=70;for(String id:candidates.keySet())if(!projectIds.containsKey(id))projectIds.put(id,nextIndex++);
        var projects=new ArrayList<GameData.Project>();
        for(var c:candidates.values())projects.add(new GameData.Project(c.id(),projectIds.get(c.id()),categoryIds.get(c.category()),c.difficulty(),c.type(),c.name(),c.text(),c.restricted(),c.prerequisites().stream().map(projectIds::get).toList(),c.discovery(),c.result(),c.steps(),c.name_key(),c.text_key()));
        projects.sort(Comparator.comparingInt(GameData.Project::index));

        var visSeed=new LinkedHashMap<String,JsonObject>();for(int i=0;i<BUNDLED.vis().size();i++)visSeed.put(String.format(Locale.ROOT,"thaumcraft2tp:legacy/%04d",i),object(BUNDLED.vis().get(i)));
        var vis=rules(read(minecraft,"vis",visSeed,skipped),visSeed,"vis rule",skipped,j->{
            String ingredient=ingredient(j);
            if(!j.has("value"))throw new IllegalArgumentException("Missing value");
            float value=j.get("value").getAsFloat();if(!Float.isFinite(value)||value<0)throw new IllegalArgumentException("Invalid vis value "+value);
            return new GameData.Vis(ingredient,value);
        });
        var sourceSeed=new LinkedHashMap<String,JsonObject>();for(int i=0;i<BUNDLED.researchItems().size();i++){
            var item=BUNDLED.researchItems().get(i);var j=object(item);j.addProperty("category",item.category()<0?"random":BASE_CATEGORIES.get(item.category()).id());var special=new JsonArray();for(int p:item.special())special.add(GameData.legacyResearchId(p).toString());j.add("special",special);sourceSeed.put(String.format(Locale.ROOT,"thaumcraft2tp:legacy/%04d",i),j);
        }
        var sources=rules(read(minecraft,"research_sources",sourceSeed,skipped),sourceSeed,"research source",skipped,j->{
            String ingredient=ingredient(j);String category=string(j,"category","random");
            var special=strings(j,"special").stream().map(p->reference(projectIds,"research",p)).toList();
            if(j.has("special_chance")&&special.isEmpty())throw new IllegalArgumentException("special_chance needs a special list");
            return new GameData.ResearchItem(ingredient,number(j,"value",-1,-1,100000),category.equals("random")?-1:reference(categoryIds,"category",category),special,j.has("special_chance")?number(j,"special_chance",0,0,100):null);
        });
        var lockSeed=new LinkedHashMap<String,JsonObject>();BUNDLED.locks().forEach((id,index)->{var j=new JsonObject();j.addProperty("research",GameData.legacyResearchId(index).toString());lockSeed.put(id,j);});
        var locks=new LinkedHashMap<String,Integer>();read(minecraft,"craft_requirements",lockSeed,skipped).forEach((id,json)->{
            Integer research=parse("craft requirement",id,json,lockSeed.get(id),j->{
                var holder=minecraft.getRecipeManager().byKey(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE,Identifier.parse(id)));
                if(holder.isEmpty()||!(holder.get().value() instanceof net.minecraft.world.item.crafting.CraftingRecipe))throw new IllegalArgumentException("missing or non-crafting recipe "+id);
                String required=string(j,"research");Identifier.parse(required);
                if(projectIds.containsKey(required))return projectIds.get(required);
                skipped.add("craft requirement "+id);Thaumcraft.LOG.warn("Craft requirement {} names unavailable research {}; the recipe stays locked for everyone",id,required);
                return UNAVAILABLE;
            },skipped);
            if(research!=null)locks.put(id,research);
        });
        // Minecraft already skipped malformed or condition-disabled recipes; an unresolved research lock would make one ungated.
        var infusionHolders=minecraft.getRecipeManager().getRecipes().stream().filter(h->h.value() instanceof InfusionRecipeData).sorted(Comparator.<net.minecraft.world.item.crafting.RecipeHolder<?>>comparingInt(h->-((InfusionRecipeData)h.value()).priority()).thenComparing(h->h.id().identifier().toString())).toList();
        var infusions=new ArrayList<GameData.Infusion>();for(var holder:infusionHolders){
            var recipe=(InfusionRecipeData)holder.value();var id=holder.id().identifier();
            var research=recipe.research().map(Identifier::toString);
            if(research.isPresent()&&!projectIds.containsKey(research.get())){skip(skipped,"infusion",id.toString(),"unknown research "+research.get());continue;}
            infusions.add(recipe.definition(id,research.map(projectIds::get).orElse(-1)));
        }
        var boosterSeed=new LinkedHashMap<String,JsonObject>();
        for(var booster:BASE_BOOSTERS){
            var j=new JsonObject();j.addProperty("block",booster.block());j.addProperty("enchanting",booster.enchanting());j.addProperty("research_speed",booster.researchSpeed());
            j.addProperty("research_bonus",booster.researchBonus());j.addProperty("failure_protection",booster.failureProtection());
            boosterSeed.put("thaumcraft2tp:"+Identifier.parse(booster.block()).getPath(),j);
        }
        var boosters=rules(read(minecraft,"boosters",boosterSeed,skipped),boosterSeed,"booster",skipped,j->new GameData.Booster(block(j,"block"),
                number(j,"enchanting",0,0,100),number(j,"research_speed",0,0,1000),number(j,"research_bonus",0,0,1000),decimal(j,"failure_protection",0,100)));
        var taintBlocks=rules(read(minecraft,"taint_blocks",Map.of(),skipped),Map.of(),"taint block rule",skipped,j->{
            String result=block(j,"result");if(result.startsWith("#"))throw new IllegalArgumentException("result must be a block ID");
            var block=net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(Identifier.parse(result));
            Integer material=j.has("material")?number(j,"material",0,0,15):null;
            if(material!=null&&!(block instanceof dev.thaumcraft.content.TaintBlock))throw new IllegalArgumentException("material needs a Thaumcraft taint block result");
            return new GameData.TaintRule(block(j,"block"),result,material);
        });
        var taintEntities=rules(read(minecraft,"taint_entities",Map.of(),skipped),Map.of(),"taint entity rule",skipped,j->{
            String result=entity(j,"result");if(result.startsWith("#"))throw new IllegalArgumentException("result must be an entity type ID");
            return new GameData.TaintEntityRule(entity(j,"entity"),result);
        });
        var treasure=treasurePools(rules(read(minecraft,"treasure",TREASURE_SEED,skipped),TREASURE_SEED,"treasure",skipped,j->treasure(j,true)));
        var restorerSeed=new LinkedHashMap<String,JsonObject>();
        for(var rule:BASE_RESTORER_COSTS)restorerSeed.put("thaumcraft2tp:legacy/"+Identifier.parse(rule.ingredient()).getPath(),object(rule));
        var restorerCosts=rules(read(minecraft,"restorer_costs",restorerSeed,skipped),restorerSeed,"restorer cost",skipped,j->{
            String target=ingredient(j);if(!target.startsWith("#"))item(target);
            if(!j.has("cost")||!j.get("cost").isJsonPrimitive()||!j.getAsJsonPrimitive("cost").isNumber())throw new IllegalArgumentException("cost must be a number");
            return new GameData.RestorerCost(target,j.get("cost").getAsFloat());
        });
        return new Snapshot(projects,infusions,vis,sources,locks,categories,crafts(minecraft,locks),boosters,taintBlocks,taintEntities,treasure,restorerCosts);
    }
    private static List<GameData.RestorerCost> restorerSeeds(){
        try(var in=AddonData.class.getResourceAsStream("/thaumcraft2tp/restorer_costs.json")){
            if(in==null)throw new IllegalStateException("Missing built-in restorer costs");
            return List.of(GSON.fromJson(new java.io.InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8),GameData.RestorerCost[].class));
        }catch(java.io.IOException error){throw new java.io.UncheckedIOException(error);}
    }
    private record TreasurePart(String pool,Integer fillOneIn,List<GameData.TreasureEntry> entries) {}
    private static Map<String,JsonObject> treasureSeed(){
        var seed=new LinkedHashMap<String,JsonObject>();
        for(String name:List.of("chest","eldritch"))try(var in=AddonData.class.getResourceAsStream("/thaumcraft2tp/treasure/"+name+".json")){
            if(in==null)throw new IllegalStateException("Missing built-in treasure "+name);
            seed.put("thaumcraft2tp:legacy/"+name,JsonParser.parseReader(new java.io.InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject());
        }catch(java.io.IOException error){throw new java.io.UncheckedIOException(error);}
        return seed;
    }
    /** {@code check} validates item IDs. The bundled pools skip it because they load before items are registered. */
    private static TreasurePart treasure(JsonObject j,boolean check){
        String pool=string(j,"pool");if(!TREASURE_POOLS.containsKey(pool))throw new IllegalArgumentException("Unknown treasure pool "+pool+"; use one of "+new TreeSet<>(TREASURE_POOLS.keySet()));
        Integer fillOneIn=j.has("fill_one_in")?number(j,"fill_one_in",1,1,1000):null;
        var entries=new ArrayList<GameData.TreasureEntry>();
        if(!j.has("entries")||!j.get("entries").isJsonArray())throw new IllegalArgumentException("entries must be an array");
        for(var element:j.getAsJsonArray("entries")){
            var entry=element.getAsJsonObject();var items=new ArrayList<GameData.TreasureItem>();
            if(!entry.has("items")||!entry.get("items").isJsonArray()||entry.getAsJsonArray("items").isEmpty()||entry.getAsJsonArray("items").size()>64)throw new IllegalArgumentException("items must list 1 to 64 items");
            for(var item:entry.getAsJsonArray("items"))items.add(treasureItem(item,check));
            entries.add(new GameData.TreasureEntry(number(entry,"repeat",1,1,1000),items));
        }
        return new TreasurePart(pool,fillOneIn,entries);
    }
    private static GameData.TreasureItem treasureItem(JsonElement value,boolean check){
        java.util.function.UnaryOperator<String> item=id->check?item(id):Identifier.parse(id).toString();
        if(value.isJsonPrimitive())return new GameData.TreasureItem(List.of(item.apply(value.getAsString())),1,1);
        var j=value.getAsJsonObject();
        if(j.has("item")==j.has("one_of"))throw new IllegalArgumentException("A treasure item needs exactly one of item or one_of");
        List<String> choices=j.has("item")?List.of(item.apply(string(j,"item"))):strings(j,"one_of").stream().map(item).toList();
        if(choices.isEmpty()||choices.size()>64)throw new IllegalArgumentException("one_of must list 1 to 64 items");
        int min=1,max=1;
        if(j.has("count")&&j.get("count").isJsonObject()){var count=j.getAsJsonObject("count");min=number(count,"min",1,1,99);max=number(count,"max",min,1,99);if(max<min)throw new IllegalArgumentException("count max is below min");}
        else if(j.has("count"))min=max=number(j,"count",1,1,99);
        return new GameData.TreasureItem(choices,min,max);
    }
    private static String item(String id){
        var key=Identifier.parse(id);
        if(!net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(key)||net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(key)==net.minecraft.world.item.Items.AIR)throw new IllegalArgumentException("Missing item "+id);
        return key.toString();
    }
    /** Concatenates contributions in rule order. A pool takes the first explicit fill_one_in, else its original odds. */
    private static Map<String,GameData.TreasurePool> treasurePools(List<TreasurePart> parts){
        var result=new HashMap<String,GameData.TreasurePool>();
        for(String pool:TREASURE_POOLS.keySet()){
            var entries=new ArrayList<GameData.TreasureEntry>();Integer fillOneIn=null;
            for(var part:parts)if(part.pool().equals(pool)){entries.addAll(part.entries());if(fillOneIn==null)fillOneIn=part.fillOneIn();}
            result.put(pool,new GameData.TreasurePool(fillOneIn!=null?fillOneIn:TREASURE_POOLS.get(pool),entries));
        }
        return result;
    }
    /** A block ID or {@code #}block tag. A missing ID is an error so that typos are reported. */
    private static String block(JsonObject value,String key){
        String rule=string(value,key);var id=Identifier.parse(rule.startsWith("#")?rule.substring(1):rule);
        if(!rule.startsWith("#")&&(!net.minecraft.core.registries.BuiltInRegistries.BLOCK.containsKey(id)||net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(id)==net.minecraft.world.level.block.Blocks.AIR))throw new IllegalArgumentException("Missing block "+rule);
        return rule;
    }
    private static String entity(JsonObject value,String key){
        String rule=string(value,key);var id=Identifier.parse(rule.startsWith("#")?rule.substring(1):rule);
        if(!rule.startsWith("#")&&!net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.containsKey(id))throw new IllegalArgumentException("Missing entity type "+rule);
        return rule;
    }
    private static List<GameData.Craft> crafts(MinecraftServer minecraft,Map<String,Integer> locks){
        var visible=new HashSet<String>(locks.keySet());for(var craft:GameData.bundledCrafts())visible.add(craft.id());
        var result=new ArrayList<GameData.Craft>();
        var ops=minecraft.registryAccess().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE);
        java.util.function.Function<net.minecraft.world.item.crafting.Ingredient,String> ingredient=value->{
            var encoded=net.minecraft.world.item.crafting.Ingredient.CODEC.encodeStart(ops,value).result();
            if(encoded.isPresent()&&encoded.get().isJsonPrimitive())return encoded.get().getAsString();
            return value.items().findFirst().map(holder->net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(holder.value()).toString()).orElse("minecraft:air");
        };
        for(var holder:minecraft.getRecipeManager().getRecipes()){
            String id=holder.id().identifier().toString();if(!visible.contains(id))continue;
            if(!(holder.value() instanceof net.minecraft.world.item.crafting.CraftingRecipe recipe)||recipe.isSpecial())continue;
            if(!(recipe instanceof net.minecraft.world.item.crafting.ShapedRecipe)&&!(recipe instanceof net.minecraft.world.item.crafting.ShapelessRecipe))continue;
            var output=recipe.assemble(net.minecraft.world.item.crafting.CraftingInput.EMPTY);if(output.isEmpty())continue;
            var stack=new GameData.StackDef(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(output.getItem()).toString(),output.getCount());
            if(recipe instanceof net.minecraft.world.item.crafting.ShapedRecipe shaped){
                var pattern=new ArrayList<String>();var key=new LinkedHashMap<String,String>();
                for(int y=0;y<shaped.getHeight();y++){StringBuilder row=new StringBuilder();for(int x=0;x<shaped.getWidth();x++){
                    int slot=y*shaped.getWidth()+x;var value=shaped.getIngredients().get(slot);
                    if(value.isEmpty())row.append(' ');else{String symbol=String.valueOf((char)('A'+slot));row.append(symbol);key.put(symbol,ingredient.apply(value.get()));}
                }pattern.add(row.toString());}
                result.add(new GameData.Craft(id,locks.getOrDefault(id,-1),true,pattern,key,List.of(),stack));
            }else if(recipe instanceof net.minecraft.world.item.crafting.ShapelessRecipe){
                result.add(new GameData.Craft(id,locks.getOrDefault(id,-1),false,List.of(),Map.of(),recipe.placementInfo().ingredients().stream().map(ingredient).toList(),stack));
            }
        }
        result.sort(Comparator.comparing(GameData.Craft::id));return List.copyOf(result);
    }
    private static void validate(Snapshot snapshot){
        if(snapshot.projects().size()>4096||snapshot.infusions().size()>4096||snapshot.categories().size()>64||snapshot.categories().size()<4)throw new IllegalArgumentException("Catalog size limit exceeded");
        var categoryIds=new HashSet<String>();for(var category:snapshot.categories()){Identifier.parse(category.id());if(!categoryIds.add(category.id()))throw new IllegalArgumentException("Duplicate category "+category.id());}
        if(snapshot.vis().size()>4096||snapshot.researchItems().size()>4096||snapshot.locks().size()>4096)throw new IllegalArgumentException("Too many value rules or craft requirements");
        var byId=snapshot.byId();if(byId.size()!=snapshot.projects().size())throw new IllegalArgumentException("Duplicate research ID");
        var byIndex=new HashMap<Integer,GameData.Project>();for(var p:snapshot.projects()){
            if(byIndex.put(p.index(),p)!=null||p.index()<0||p.category()<0||p.category()>=snapshot.categories().size()||p.difficulty()<0||p.difficulty()>5)throw new IllegalArgumentException("Invalid research "+p.id());
            if(p.index()<70&&!p.key().equals(GameData.legacyResearchId(p.index())))throw new IllegalArgumentException("Changed legacy research identity "+p.id());
            p.result().create();
            var discovery=dev.thaumcraft.content.Content.entry(new GameData.StackDef(p.discovery(),1).create());
            if(discovery==null||!discovery.source_class().equals("itemDiscovery"))throw new IllegalArgumentException("Research discovery must be a discovery item: "+p.id());
        }
        var resolved=new HashSet<Integer>();
        while(resolved.size()<byIndex.size()){
            int before=resolved.size();for(var p:snapshot.projects())if(!resolved.contains(p.index())&&resolved.containsAll(p.prerequisites()))resolved.add(p.index());
            if(before==resolved.size())throw new IllegalArgumentException("Research prerequisite cycle or missing reference: "+snapshot.projects().stream().filter(p->!resolved.contains(p.index())).map(GameData.Project::id).toList());
        }
        var ids=new HashSet<String>();for(var recipe:snapshot.infusions()){
            if(!ids.add(recipe.id())||recipe.cost()<1||recipe.cost()>100000||recipe.ingredients().isEmpty()||recipe.ingredients().size()>(recipe.dark()?5:6))throw new IllegalArgumentException("Invalid infusion "+recipe.id());
            Identifier.parse(recipe.id());recipe.result().create();
            if(recipe.requiredResearch()!=null&&(!byId.containsKey(recipe.requiredResearch())||byId.get(recipe.requiredResearch()).index()!=recipe.research()))throw new IllegalArgumentException("Unknown or inconsistent infusion research "+recipe.requiredResearch());
            for(String ingredient:recipe.ingredients())InfusionRecipeData.INGREDIENT.parse(com.mojang.serialization.JsonOps.INSTANCE,new JsonPrimitive(ingredient)).getOrThrow();
        }
        for(var value:snapshot.vis())if(!Float.isFinite(value.value())||value.value()<0)throw new IllegalArgumentException("Invalid vis value");
        if(snapshot.restorerCosts().size()>4096)throw new IllegalArgumentException("Too many restorer costs");
        for(var rule:snapshot.restorerCosts()){
            Identifier.parse(rule.ingredient().startsWith("#")?rule.ingredient().substring(1):rule.ingredient());
            if(!Float.isFinite(rule.cost())||rule.cost()<0||rule.cost()>100000)throw new IllegalArgumentException("Invalid restorer cost");
        }
        for(var value:snapshot.researchItems())if(value.category()<-1||value.category()>=snapshot.categories().size()||!byIndex.keySet().containsAll(value.special())||value.special_chance()!=null&&(value.special_chance()<0||value.special_chance()>100||value.special().isEmpty()))throw new IllegalArgumentException("Invalid research source");
        for(var lock:snapshot.locks().entrySet()){Identifier.parse(lock.getKey());if(lock.getValue()!=UNAVAILABLE&&!byIndex.containsKey(lock.getValue()))throw new IllegalArgumentException("Unknown craft research "+lock.getKey());}
        for(var pool:snapshot.treasure().values()){
            if(pool.fillOneIn()<1||pool.fillOneIn()>1000)throw new IllegalArgumentException("Invalid treasure odds");
            long size=0;for(var entry:pool.entries())size+=(long)entry.repeat()*entry.items().size();
            if(size>100000)throw new IllegalArgumentException("A treasure pool may hold at most 100,000 items");
        }
        if(snapshot.boosters().size()>4096||snapshot.taintBlocks().size()>4096||snapshot.taintEntities().size()>4096)throw new IllegalArgumentException("Too many booster or taint rules");
        for(var booster:snapshot.boosters())if(booster.enchanting()<0||booster.researchSpeed()<0||booster.researchBonus()<0||booster.failureProtection()<0)throw new IllegalArgumentException("Invalid booster "+booster.block());
        for(var rule:snapshot.taintBlocks())if(rule.result().startsWith("#")||rule.material()!=null&&(rule.material()<0||rule.material()>15))throw new IllegalArgumentException("Invalid taint rule "+rule.block());
    }
}
