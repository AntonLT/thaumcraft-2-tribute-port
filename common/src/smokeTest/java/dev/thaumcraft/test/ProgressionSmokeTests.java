package dev.thaumcraft.test;

import com.mojang.authlib.GameProfile;
import dev.thaumcraft.Thaumcraft;
import dev.thaumcraft.content.Content;
import dev.thaumcraft.content.CrystalBlock;
import dev.thaumcraft.gameplay.ArcaneWorldData;
import dev.thaumcraft.gameplay.GameData;
import dev.thaumcraft.gameplay.ResearchLogic;
import dev.thaumcraft.gameplay.VoidNetworks;
import dev.thaumcraft.item.ItemState;
import dev.thaumcraft.machine.MachineBlockEntity;
import dev.thaumcraft.machine.MachineLogic;
import dev.thaumcraft.machine.VoidMenu;
import dev.thaumcraft.world.EldritchIndex;
import dev.thaumcraft.world.EldritchStructures;
import dev.thaumcraft.world.SealPortals;
import dev.thaumcraft.world.TreeWorld;
import dev.thaumcraft.world.WorldGenGreatwood;
import dev.thaumcraft.world.WorldGenSilverwood;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.lang.reflect.Method;
import java.lang.reflect.Field;

/** Behavior checks against loaded registries, vanilla crafting/loot entry points and disk saves. */
final class ProgressionSmokeTests {
    private static int checks;
    private static void check(boolean condition,String message) {checks++;if(!condition)throw new AssertionError(message);}
    static int runCrystals(MinecraftServer server) {
        checks=0;
        crystals(server.overworld(),new SmokePlayer(server,server.overworld()),new BlockPos(80,280,80));
        return checks;
    }
    static int runEnchantingTable(MinecraftServer server) {
        checks=0;
        var level=server.overworld();
        var data=ArcaneWorldData.researchData(level);
        var locked=new SmokePlayer(server,level,UUID.randomUUID());
        var unlocked=new SmokePlayer(server,level,UUID.randomUUID());
        var enchantments=level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        for(String name:List.of("vampiric","soulstealer","repair","relic","potency"))
            check(enchantments.getOrThrow(ResourceKey.create(Registries.ENCHANTMENT,Thaumcraft.id(name))).is(EnchantmentTags.IN_ENCHANTING_TABLE),"Researched enchantment enters the vanilla table pool: "+name);
        var potency=enchantments
                .getOrThrow(ResourceKey.create(Registries.ENCHANTMENT,Thaumcraft.id("potency")));
        var wand=new ItemStack(Content.item("wand_of_fire"));
        check(potency.is(EnchantmentTags.IN_ENCHANTING_TABLE)&&potency.value().isPrimaryItem(wand),"Potency is a valid vanilla table candidate");
        for(String id:List.of("wand_of_water","wand_of_bone","bow_of_bone"))
            check(!potency.value().isPrimaryItem(new ItemStack(Content.item(id))),"Potency excludes the original unsupported item "+id);
        check(!data.knows(locked.getUUID(),67),"First player has not researched Potency");
        data.unlock(unlocked.getUUID(),67);
        var lockedMenu=new EnchantmentMenu(0,locked.getInventory(),ContainerLevelAccess.create(level,BlockPos.ZERO));
        var unlockedMenu=new EnchantmentMenu(1,unlocked.getInventory(),ContainerLevelAccess.create(level,BlockPos.ZERO));
        try {
            Method choices=EnchantmentMenu.class.getDeclaredMethod("getEnchantmentList",RegistryAccess.class,ItemStack.class,int.class,int.class);
            choices.setAccessible(true);
            Field seed=EnchantmentMenu.class.getDeclaredField("enchantmentSeed");seed.setAccessible(true);
            var lockedSeed=(DataSlot)seed.get(lockedMenu);var unlockedSeed=(DataSlot)seed.get(unlockedMenu);
            int potencySeed=-1;
            for(int i=0;i<256;i++) {
                lockedSeed.set(i);unlockedSeed.set(i);
                @SuppressWarnings("unchecked") var hidden=(List<EnchantmentInstance>)choices.invoke(lockedMenu,level.registryAccess(),wand,2,30);
                @SuppressWarnings("unchecked") var visible=(List<EnchantmentInstance>)choices.invoke(unlockedMenu,level.registryAccess(),wand,2,30);
                check(hidden.stream().noneMatch(e->e.enchantment().equals(potency)),"Unresearched player never rolls Potency");
                if(visible.stream().anyMatch(e->e.enchantment().equals(potency))){potencySeed=i;break;}
            }
            check(potencySeed>=0,"Researched player can roll Potency at the vanilla table");
            data.unlock(locked.getUUID(),67);lockedSeed.set(potencySeed);
            @SuppressWarnings("unchecked") var learned=(List<EnchantmentInstance>)choices.invoke(lockedMenu,level.registryAccess(),wand,2,30);
            check(learned.stream().anyMatch(e->e.enchantment().equals(potency)),"The same player can roll Potency after learning it");
        } catch(ReflectiveOperationException e) {throw new AssertionError("Cannot exercise vanilla enchanting selection",e);}
        return checks;
    }
    static int run(MinecraftServer server) {
        checks=0;
        ServerLevel level=server.overworld();
        SmokePlayer player=new SmokePlayer(server,level);
        recipesAndResearch(level,player);
        researchOverflow(level);
        discoveryUse(level,player);
        voidStorage(level,player);
        occulticProcessing(level,player);
        equipmentAndLoot(level,player);
        crystals(level,player,new BlockPos(80,280,80));
        worldAndPersistence(level,player,new BlockPos(112,250,112));
        return checks;
    }
    /** Preserve server gameplay while capturing the UI/recipe packets of this offline fixture. */
    private static final class SmokePlayer extends ServerPlayer {
        boolean bookOpened;
        final HashSet<Identifier> recipeNotifications=new HashSet<>();
        SmokePlayer(MinecraftServer server,ServerLevel level) {this(server,level,UUID.fromString("68764a60-3d21-4cb6-a491-60525ce61e28"));}
        SmokePlayer(MinecraftServer server,ServerLevel level,UUID id) {super(server,level,new GameProfile(id,"SmokeResearch"),ClientInformation.createDefault());}
        @Override public void sendSystemMessage(Component message,boolean overlay) {}
        @Override public void openItemGui(ItemStack stack,InteractionHand hand) {bookOpened=true;}
        @Override public int awardRecipes(Collection<RecipeHolder<?>> recipes) {int before=recipeNotifications.size();recipes.forEach(r->recipeNotifications.add(r.id().identifier()));return recipeNotifications.size()-before;}
    }
    private static ItemStack ingredient(String id) {
        if(id.startsWith("#"))return new ItemStack(BuiltInRegistries.ITEM.get(TagKey.create(Registries.ITEM,Identifier.parse(id.substring(1)))).orElseThrow().iterator().next().value());
        return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(id)));
    }
    private static MachineBlockEntity machine(ServerLevel level,BlockPos pos,String id) {
        level.getChunkAt(pos);level.setBlockAndUpdate(pos,Content.block(id).defaultBlockState());
        return (MachineBlockEntity)level.getBlockEntity(pos);
    }
    private static void recipesAndResearch(ServerLevel level,ServerPlayer player) {
        var ids=new HashSet<String>();
        var data=ArcaneWorldData.researchData(level);
        var result=new ResultContainer();
        check(!player.isCreative(),"Research fixture is a survival ServerPlayer");
        for(var craft:GameData.crafts()) {
            check(ids.add(craft.id()),"Unique crafting recipe identifier "+craft.id());
            var holder=level.getServer().getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE,Identifier.parse(craft.id()))).orElseThrow();
            check(holder.value() instanceof CraftingRecipe,"Loaded crafting recipe "+craft.id());
            var input=new ArrayList<ItemStack>();
            int width=3,height=3;
            if(craft.shaped()) {
                height=craft.pattern().size();width=craft.pattern().getFirst().length();
                for(String row:craft.pattern())for(char symbol:row.toCharArray())input.add(symbol==' '?ItemStack.EMPTY:ingredient(craft.key().get(String.valueOf(symbol))));
            } else {for(String id:craft.ingredients())input.add(ingredient(id));while(input.size()<9)input.add(ItemStack.EMPTY);}
            var grid=CraftingInput.of(width,height,input);var recipe=(CraftingRecipe)holder.value();
            check(recipe.matches(grid,level),"Loaded recipe matches its documented grid "+craft.id());
            ItemStack output=recipe.assemble(grid),expected=craft.result().create();
            check(output.is(expected.getItem())&&output.getCount()==expected.getCount(),"Crafted output and quantity "+craft.id());
            // This calls the actual vanilla method targeted by ResearchCraftingMixin.
            if(craft.research()>=0&&!data.knows(player.getUUID(),craft.research())) {
                check(!result.setRecipeUsed(player,holder),"Survival crafting blocked before discovery "+craft.id());
            } else if(craft.research()<0)check(result.setRecipeUsed(player,holder),"Base crafting available "+craft.id());
        }
        var gated=GameData.crafts().stream().filter(c->c.research()>=0).findFirst().orElseThrow();
        var holder=level.getServer().getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE,Identifier.parse(gated.id()))).orElseThrow();
        check(data.unlock(player.getUUID(),gated.research()),"Discovery unlocks once");
        check(!data.unlock(player.getUUID(),gated.research()),"Repeated discovery does not duplicate research");
        check(result.setRecipeUsed(player,holder),"Survival crafting allowed after discovery");

        var infusion=GameData.infusions().stream().filter(r->!r.dark()&&r.research()>=0&&r.research()!=gated.research()&&r.cost()<=250).findFirst().orElseThrow();
        BlockPos pos=new BlockPos(96,290,64);
        var infuser=machine(level,pos,infusion.dark()?"dark_infuser":"thaumic_infuser");infuser.setOwner(player.getUUID());
        int slot=0;for(String id:infusion.ingredients())infuser.setItem(slot++,ingredient(id));
        infuser.insertVis(infusion.dark()?infusion.cost()*2f/3:infusion.cost(),false);if(infusion.dark())infuser.insertVis(infusion.cost()/3f,true);
        int ticks=infusion.cost()*4+5;
        for(int i=0;i<ticks;i++)infuser.processes.tick(level);
        check(infuser.getItem(9).isEmpty(),"Infusion blocked before owner's discovery");
        check(Math.abs(infuser.totalVis()-infusion.cost())<.001f,"Blocked infusion preserves vis");
        data.unlock(player.getUUID(),infusion.research());
        for(int i=0;i<ticks;i++)infuser.processes.tick(level);
        var expected=infusion.result().create();
        check(infuser.getItem(9).is(expected.getItem())&&infuser.getItem(9).getCount()==expected.getCount(),"Discovery unlocks one infusion output");
        for(int i=0;i<ticks;i++)infuser.processes.tick(level);
        check(infuser.getItem(9).getCount()==expected.getCount(),"Infusion cannot repeat after consuming inputs");
        check(infuser.totalVis()<.001f,"Unlocked infusion spends exact vis");
        level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
    }
    private static void researchOverflow(ServerLevel level) {
        BlockPos pos=new BlockPos(96,290,72);
        var quaesitum=machine(level,pos,"quaesitum");
        for(int slot=9;slot<18;slot++)quaesitum.setItem(slot,new ItemStack(Items.COBBLESTONE,64));
        var project=GameData.projects().getFirst();
        var theory=ResearchLogic.theory(project.index());
        ItemState.setInt(theory,"difficulty",0);
        ItemState.setInt(theory,"research_progress",4);
        quaesitum.setItem(0,theory);
        quaesitum.setItem(3,new ItemStack(Items.PAPER,64));
        AABB nearby=new AABB(pos).inflate(2);
        for(int attempt=0;attempt<100&&!quaesitum.getItem(0).isEmpty();attempt++) {
            quaesitum.progress=10_000;
            ResearchLogic.tick(level,quaesitum);
        }
        var discovery=new GameData.StackDef(project.discovery(),1).create();
        check(quaesitum.getItem(0).isEmpty(),"A completed theory is consumed with full Quaesitum results");
        check(level.getEntitiesOfClass(ItemEntity.class,nearby).stream().anyMatch(e->e.getItem().is(discovery.getItem())),"A full Quaesitum drops its discovery");
        level.getEntitiesOfClass(ItemEntity.class,nearby).forEach(ItemEntity::discard);

        var fragment=Content.ENTRIES.stream().filter(e->e.legacy().endsWith("itemKnowledgeFragment")&&e.meta()==0).findFirst().orElseThrow();
        quaesitum.setItem(0,new ItemStack(Content.item(fragment.id()),64));
        quaesitum.setItem(3,new ItemStack(Items.PAPER,64));
        for(int attempt=0;attempt<100&&level.getEntitiesOfClass(ItemEntity.class,nearby).isEmpty();attempt++) {
            if(quaesitum.getItem(0).isEmpty())quaesitum.setItem(0,new ItemStack(Content.item(fragment.id()),64));
            if(quaesitum.getItem(3).isEmpty())quaesitum.setItem(3,new ItemStack(Items.PAPER,64));
            quaesitum.progress=10_000;
            ResearchLogic.tick(level,quaesitum);
        }
        check(!level.getEntitiesOfClass(ItemEntity.class,nearby).isEmpty(),"A full Quaesitum drops fragment research results");
        level.getEntitiesOfClass(ItemEntity.class,nearby).forEach(ItemEntity::discard);
        level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
    }
    private static void discoveryUse(ServerLevel level,SmokePlayer player) {
        var data=ArcaneWorldData.researchData(level);
        var project=GameData.projects().stream().filter(p->!data.knows(player.getUUID(),p.index())&&p.prerequisites().stream().anyMatch(r->!data.knows(player.getUUID(),r))&&GameData.crafts().stream().anyMatch(c->c.research()==p.index())).findFirst().orElseThrow();
        var missing=project.prerequisites().stream().filter(r->!data.knows(player.getUUID(),r)).toList();
        var discovery=new GameData.StackDef(project.discovery(),1).create();player.setItemInHand(InteractionHand.MAIN_HAND,discovery);
        // ItemDiscovery calls AddResearchToList and opens its GUI unconditionally; prerequisite checks select new theories, not readable discoveries.
        check(discovery.getItem().use(level,player,InteractionHand.MAIN_HAND)==InteractionResult.SUCCESS,"An acquired discovery can be read before its prerequisites");
        check(data.knows(player.getUUID(),project.index())&&player.bookOpened,"Studying discovery unlocks player research and opens its book");
        check(missing.stream().noneMatch(r->data.knows(player.getUUID(),r)),"Reading a discovery does not silently unlock missing prerequisite projects");
        check(discovery.getCount()==1,"Reading preserves the discovery item");
        check(discovery.get(DataComponents.WRITTEN_BOOK_CONTENT)!=null&&!discovery.get(DataComponents.WRITTEN_BOOK_CONTENT).pages().isEmpty(),"Studied discovery contains readable pages");
        check(GameData.crafts().stream().filter(c->c.research()==project.index()).allMatch(c->player.recipeNotifications.contains(Identifier.parse(c.id()))),"Discovery sends its loaded recipes to the player's recipe book");
        var learned=new HashSet<>(data.known(player.getUUID()));int notifications=player.recipeNotifications.size();player.bookOpened=false;
        check(discovery.getItem().use(level,player,InteractionHand.MAIN_HAND)==InteractionResult.SUCCESS&&player.bookOpened,"A known discovery reopens its book");
        check(learned.equals(new HashSet<>(data.known(player.getUUID())))&&notifications==player.recipeNotifications.size()&&discovery.getCount()==1,"Rereading preserves knowledge, recipe identities and the original item count");
    }
    private static void voidStorage(ServerLevel level,ServerPlayer player) {
        BlockPos a=new BlockPos(96,280,96),b=a.east(5);
        var first=machine(level,a,"void_chest");var second=machine(level,b,"void_chest");
        first.setOwner(player.getUUID());second.setOwner(player.getUUID());
        first.setItem(71,new ItemStack(Items.DIAMOND,13));second.setItem(71,new ItemStack(Items.EMERALD,7));
        var portalA=machine(level,a.above(),"void_interface");var portalB=machine(level,b.above(),"void_interface");
        for(var portal:List.of(portalA,portalB)){portal.setOwner(player.getUUID());portal.setChannel(2);VoidNetworks.get(level).register(level,portal.getBlockPos());}
        check(portalA.getContainerSize()==144&&portalA.voidPages()==2,"Same-rune interfaces aggregate two local 72-slot chests");
        check(portalA.getItem(71).is(Items.DIAMOND)&&portalA.getItem(143).is(Items.EMERALD),"Interface exposes each chest's final slot without overlap");
        player.setPos(a.getX()+.5,a.getY()+1,a.getZ()+.5);
        var menu=new VoidMenu(7,player.getInventory(),portalA);
        check(menu.slots.size()==108&&menu.pages()==2&&menu.page()==0,"Void menu exposes 72 storage slots and player inventory");
        check(menu.clickMenuButton(player,1)&&menu.page()==1,"Void interface advances to another chest page");
        check(menu.getSlot(71).getItem().is(Items.EMERALD),"Next page reads the other backing chest");
        ItemStack removed=menu.getSlot(71).remove(3);
        check(removed.getCount()==3&&second.getItem(71).getCount()==4&&first.getItem(71).getCount()==13,"Taking items through a page updates exactly one backing chest");
        portalB.setChannel(3);
        check(portalA.voidPages()==1&&portalA.getContainerSize()==72,"Changing runes invalidates the linked chest list");
        level.setBlockAndUpdate(a.above(),Blocks.AIR.defaultBlockState());
        check(first.getItem(71).getCount()==13&&second.getItem(71).getCount()==4,"Breaking an interface preserves local chest contents");

        UUID legacyOwner=UUID.fromString("05a4ee28-5c74-4247-847a-aa6c249c1e70");
        var legacy=VoidNetworks.get(level).inventory(legacyOwner,4);legacy.set(0,new ItemStack(Items.GOLD_INGOT,11));
        var migratedA=machine(level,a.south(5),"void_chest");var migratedB=machine(level,b.south(5),"void_chest");
        for(var chest:List.of(migratedA,migratedB)){chest.setOwner(legacyOwner);chest.setChannel(4);}
        check(migratedA.getItem(0).getCount()==11&&migratedB.getItem(0).isEmpty(),"Old shared storage migrates once into a local chest");
        check(VoidNetworks.get(level).inventory(legacyOwner,4).stream().allMatch(ItemStack::isEmpty),"Migrated legacy inventory retains no duplicate items");
        for(var chest:List.of(first,second,migratedA,migratedB))chest.clearContent();
        for(BlockPos pos:List.of(a,b,b.above(),a.south(5),b.south(5)))level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
    }
    private static void occulticProcessing(ServerLevel level,ServerPlayer player) {
        BlockPos pos=new BlockPos(96,280,112);var enchanter=machine(level,pos,"occultic_enchanter");
        enchanter.setOwner(player.getUUID());enchanter.setItem(0,new ItemStack(Items.DIAMOND_SWORD));
        check(enchanter.enchanting.click(2)&&enchanter.enchanting.data(15)==1,"Occultic enchanter stages an eligible enchantment");
        var selected=level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(enchanter.enchanting.data(2)-1).orElseThrow();
        int cost=enchanter.enchanting.data(10);check(cost>10,"Selected enchantment has a nontrivial vis cost");
        enchanter.insertVis(10,false);check(enchanter.enchanting.click(3),"Occultic enchantment starts only after selection");
        for(int i=0;i<4;i++)enchanter.processes.tick(level);
        check(enchanter.progress==10&&enchanter.getItem(9).isEmpty()&&!enchanter.getItem(0).isEmpty(),"Insufficient vis stalls enchanting without consuming the sword");
        enchanter.insertVis(cost-10,false);
        for(int i=0;i<cost/10+2;i++)enchanter.processes.tick(level);
        ItemStack output=enchanter.getItem(0);
        check(output.is(Items.DIAMOND_SWORD)&&output.getCount()==1&&output.get(DataComponents.ENCHANTMENTS).getLevel(selected)>0&&enchanter.getItem(9).isEmpty(),"Refueled enchanter produces exactly one sword with the selected enchantment");
        check(enchanter.pureVis()==0,"Enchanter spends the displayed vis cost exactly");
        for(int i=0;i<8;i++)enchanter.processes.tick(level);
        check(enchanter.getItem(0).getCount()==1,"Completed enchantment cannot duplicate its output");
        enchanter.clearContent();level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
    }
    private static void equipmentAndLoot(ServerLevel level,ServerPlayer player) {
        var equipment=Map.of("goggles_of_revealing","goggles","mask_of_cruelty","mask","boots_of_striding","bootsstriding","seven_league_boots","bootsseven","boots_of_the_meteor","bootsstomp","thaumium_helm","thaumium","void_metal_helm","void");
        for(var expected:equipment.entrySet()) {
            var equippable=new ItemStack(Content.item(expected.getKey())).get(DataComponents.EQUIPPABLE);
            check(equippable!=null&&equippable.assetId().orElseThrow().identifier().equals(Thaumcraft.id(expected.getValue())),"Equipment uses its own visual asset "+expected.getKey());
            check(equippable.slot()==(expected.getKey().contains("boots")?EquipmentSlot.FEET:EquipmentSlot.HEAD),"Correct equipment slot "+expected.getKey());
        }
        var enchantments=level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var supported=Map.of("vampiric",Items.DIAMOND_SWORD,"soulstealer",Items.DIAMOND_SWORD,"repair",Items.DIAMOND_PICKAXE,"relic",Items.DIAMOND_SWORD,"potency",Content.item("wand_of_fire"),"striding",Items.DIAMOND_BOOTS,"swimming",Items.DIAMOND_LEGGINGS,"venom",Items.DIAMOND_SWORD,"ice",Items.DIAMOND_SWORD);
        for(var entry:supported.entrySet()) {
            var enchant=enchantments.getOrThrow(ResourceKey.create(Registries.ENCHANTMENT,Thaumcraft.id(entry.getKey())));
            check(enchant.value().canEnchant(new ItemStack(entry.getValue())),"Loaded enchantment supports intended equipment "+entry.getKey());
        }
        var striding=enchantments.getOrThrow(ResourceKey.create(Registries.ENCHANTMENT,Thaumcraft.id("striding")));
        var protection=enchantments.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.PROTECTION);
        var respiration=enchantments.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.RESPIRATION);
        var featherFalling=enchantments.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.FEATHER_FALLING);
        var fortune=enchantments.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.FORTUNE);
        var silkTouch=enchantments.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH);
        for(String id:List.of("goggles_of_revealing","mask_of_cruelty","thaumium_helm","void_metal_helm")){
            var stack=new ItemStack(Content.item(id));check(protection.value().isPrimaryItem(stack)&&respiration.value().isPrimaryItem(stack),"Original ItemArmor helmet accepts Protection and Respiration "+id);
        }
        for(String id:List.of("thaumium_chestplate","void_metal_chestplate")){
            var stack=new ItemStack(Content.item(id));check(protection.value().isPrimaryItem(stack)&&!respiration.value().isPrimaryItem(stack),"Original ItemArmor chestplate accepts only torso-compatible armor enchantments "+id);
        }
        for(String id:List.of("thaumium_boots","void_metal_boots","boots_of_striding","seven_league_boots","boots_of_the_meteor")){
            var stack=new ItemStack(Content.item(id));check(protection.value().isPrimaryItem(stack)&&featherFalling.value().isPrimaryItem(stack)&&striding.value().isPrimaryItem(stack),"Original ItemArmor boots accept Protection, Feather Falling and Striding "+id);
        }
        for(String id:List.of("thaumium_shovel","thaumium_pickaxe","thaumium_axe","axe_of_the_stream","pickaxe_of_the_core","shovel_of_renewal","void_crusher","void_cutter","elemental_crusher","elemental_cutter")){
            var stack=new ItemStack(Content.item(id));check(fortune.value().isPrimaryItem(stack)&&silkTouch.value().isPrimaryItem(stack),"Original digger accepts Fortune and Silk Touch "+id);
        }
        for(String id:List.of("thaumium_hoe","hoe_of_the_mystic","thaumium_sword","sword_of_the_zephyr")){
            var stack=new ItemStack(Content.item(id));check(!fortune.value().isPrimaryItem(stack)&&!silkTouch.value().isPrimaryItem(stack),"Non-digger keeps original Fortune and Silk Touch exclusion "+id);
        }
        var chestParams=new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN,new Vec3(64,80,64)).create(LootContextParamSets.CHEST);
        var dungeon=level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,Identifier.withDefaultNamespace("chests/simple_dungeon")));
        boolean lost=false,forbidden=false;
        for(int seed=1;seed<=96;seed++){
            var chest=new net.minecraft.world.level.block.entity.ChestBlockEntity(BlockPos.ZERO,Blocks.CHEST.defaultBlockState());dungeon.fill(chest,chestParams,seed);
            for(int slot=0;slot<chest.getContainerSize();slot++){var stack=chest.getItem(slot);
            var entry=Content.entry(stack);if(entry!=null){lost|=entry.source_class().equals("ItemArtifactLost");forbidden|=entry.source_class().equals("ItemArtifactForbidden");}
        }
        }
        check(lost&&forbidden,"Chest fill hook adds both research artifact families to actual dungeon loot");
        var eldritch=level.getServer().reloadableRegistries().getLootTable(EldritchStructures.LOOT).getRandomItems(chestParams,216);
        check(eldritch.stream().mapToInt(ItemStack::getCount).sum()>=4,"Eldritch chamber table generates treasure");
        check(eldritch.stream().allMatch(s->Content.entry(s)!=null),"Eldritch treasure resolves registered items");

        var tool=new ItemStack(Content.item("pickaxe_of_the_core"));player.setItemInHand(InteractionHand.MAIN_HAND,tool);
        boolean oldShift=player.isShiftKeyDown();player.setShiftKeyDown(dev.thaumcraft.PortConfig.toolShift);
        var iron=Blocks.IRON_ORE.defaultBlockState();
        var drops=iron.getDrops(new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN,new Vec3(64,80,64)).withParameter(LootContextParams.TOOL,tool).withParameter(LootContextParams.THIS_ENTITY,player));
        check(drops.stream().anyMatch(s->s.is(Items.IRON_INGOT))&&drops.stream().noneMatch(s->s.is(Items.RAW_IRON)),"Core pick smelts real block loot through loader hook");
        player.setShiftKeyDown(!dev.thaumcraft.PortConfig.toolShift);
        drops=iron.getDrops(new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN,new Vec3(64,80,64)).withParameter(LootContextParams.TOOL,tool).withParameter(LootContextParams.THIS_ENTITY,player));
        check(drops.stream().anyMatch(s->s.is(Items.RAW_IRON)),"The configured alternate modifier suppresses core pick smelting");player.setShiftKeyDown(oldShift);

        var sword=new ItemStack(Items.DIAMOND_SWORD);sword.enchant(enchantments.getOrThrow(ResourceKey.create(Registries.ENCHANTMENT,Thaumcraft.id("soulstealer"))),3);player.setItemInHand(InteractionHand.MAIN_HAND,sword);
        var victim=EntityType.ZOMBIE.create(level,EntitySpawnReason.COMMAND);
        var entityParams=new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN,new Vec3(64,80,64)).withParameter(LootContextParams.THIS_ENTITY,victim).withParameter(LootContextParams.DAMAGE_SOURCE,level.damageSources().playerAttack(player)).withParameter(LootContextParams.ATTACKING_ENTITY,player).create(LootContextParamSets.ENTITY);
        var zombieLoot=level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,Identifier.withDefaultNamespace("entities/zombie")));
        boolean soul=false;for(int seed=1;seed<=48;seed++)soul|=zombieLoot.getRandomItems(entityParams,seed).stream().anyMatch(s->s.is(Content.item("soul_fragment")));
        check(soul,"Soulstealer affects actual entity loot generation");victim.discard();
    }
    private static void crystals(ServerLevel level,ServerPlayer player,BlockPos pos) {
        level.getChunkAt(pos);level.setBlockAndUpdate(pos.below(),Blocks.STONE.defaultBlockState());
        var state=Content.block("vis_ore").defaultBlockState().setValue(CrystalBlock.AMOUNT,1);
        level.setBlockAndUpdate(pos,state);
        var aura=ArcaneWorldData.get(level);aura.changeAura(level,pos,15000,-15000);aura.addVibes(level,pos,200,0);
        var random=RandomSource.create(216);int cycles=0;
        while(level.getBlockState(pos).getValue(CrystalBlock.AMOUNT)<5&&cycles++<12000)level.getBlockState(pos).randomTick(level,pos,random);
        check(level.getBlockState(pos).getValue(CrystalBlock.AMOUNT)==5,"Healthy aura grows crystal clusters to full size");
        var full=level.getBlockState(pos);
        var broken=full.getDrops(new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN,Vec3.atCenterOf(pos)).withParameter(LootContextParams.TOOL,new ItemStack(Items.DIAMOND_PICKAXE)));
        check(broken.stream().filter(s->s.is(Content.item("vis_crystal"))).mapToInt(ItemStack::getCount).sum()==5,"Breaking full cluster drops all five crystals");
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Content.item("crystalline_bell")));
        Vec3 previousPosition=player.position();
        player.setPos(Vec3.atCenterOf(pos).add(3,1,-2));
        var context=new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));
        for(int i=0;i<4;i++)check(player.getMainHandItem().getItem().useOn(context)==InteractionResult.SUCCESS,"Bell harvest succeeds");
        check(level.getBlockState(pos).getValue(CrystalBlock.AMOUNT)==1,"Bell preserves a crystal growth seed");
        check(player.getMainHandItem().getItem().useOn(context)==InteractionResult.FAIL,"Bell cannot consume the final crystal");
        var dropped=level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(2));
        check(dropped.stream().filter(e->e.getItem().is(Content.item("vis_crystal"))).mapToInt(e->e.getItem().getCount()).sum()==4,"Bell harvest drops exactly four items");
        check(dropped.stream().allMatch(e->e.position().equals(Vec3.atCenterOf(pos))),"Bell crystals spawn at the cluster center");
        Vec3 expectedMotion=new Vec3(3,1,-2).scale((double).1f);
        check(dropped.stream().allMatch(e->e.getDeltaMovement().distanceToSqr(expectedMotion)<1e-12),"Bell crystals move toward the player with original velocity");
        check(player.getMainHandItem().getDamageValue()==4,"Bell costs one durability per harvest and none for the last crystal");
        player.setPos(previousPosition);
        dropped.forEach(ItemEntity::discard);level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());level.setBlockAndUpdate(pos.below(),Blocks.AIR.defaultBlockState());
    }
    private static void worldAndPersistence(ServerLevel level,ServerPlayer player,BlockPos origin) {
        var oldAura=ArcaneWorldData.Aura.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,com.google.gson.JsonParser.parseString("{\"vis\":3000,\"taint\":1000,\"base\":3000}")).getOrThrow();
        check(oldAura.goodVibes()==0&&oldAura.badVibes()==0,"Older aura saves acquire neutral vibes without losing vis");
        ArcaneWorldData.get(level).addVibes(level,origin,17,9);
        BlockPos logPos=origin.west(20),leafPos=logPos.above();
        var birch=Blocks.BIRCH_LOG.defaultBlockState().setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS,Direction.Axis.X);
        var spruce=Blocks.SPRUCE_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.DISTANCE,2).setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT,true);
        level.setBlockAndUpdate(logPos,birch);level.setBlockAndUpdate(leafPos,spruce);
        check(dev.thaumcraft.content.TaintBlock.taint(level,logPos)&&dev.thaumcraft.content.TaintBlock.taint(level,leafPos),"Taint converts vanilla log and leaves");
        var memory=dev.thaumcraft.world.TaintMemory.get(level);
        var serialized=dev.thaumcraft.world.TaintMemory.CODEC.encodeStart(NbtOps.INSTANCE,memory).getOrThrow();
        var restoredMemory=dev.thaumcraft.world.TaintMemory.CODEC.parse(NbtOps.INSTANCE,serialized).getOrThrow();
        check(restoredMemory.original(logPos).orElseThrow().equals(birch)&&restoredMemory.original(leafPos).orElseThrow().equals(spruce),"Taint memory preserves species, log axis and leaf properties");
        check(restoredMemory.consume(logPos).orElseThrow().equals(birch)&&restoredMemory.original(logPos).isEmpty()&&restoredMemory.original(leafPos).isPresent(),"Consuming restored taint memory removes only the selected position");
        check(dev.thaumcraft.content.TaintBlock.purify(level,logPos)&&dev.thaumcraft.content.TaintBlock.purify(level,leafPos),"Purification restores both converted foliage blocks");
        check(level.getBlockState(logPos).equals(birch)&&level.getBlockState(leafPos).equals(spruce),"Live purification restores exact original block states");
        level.setBlockAndUpdate(leafPos,Blocks.AIR.defaultBlockState());level.setBlockAndUpdate(logPos,Blocks.AIR.defaultBlockState());
        for(int n=0;n<2;n++) {
            BlockPos pos=origin.east(n*32);
            for(BlockPos p:BlockPos.betweenClosed(pos.offset(-8,-1,-8),pos.offset(8,-1,8)))level.setBlock(p,Blocks.DIRT.defaultBlockState(),2);
            boolean generated=n==0?new WorldGenGreatwood(false).generate(new TreeWorld(level),new Random(216),pos.getX(),pos.getY(),pos.getZ()):new WorldGenSilverwood(false).generate(new TreeWorld(level),new Random(216),pos.getX(),pos.getY(),pos.getZ());
            String family=n==0?"greatwood":"silverwood";check(generated,"Generate "+family+" in real level");
            int logs=0,leaves=0,attached=0,outer=0;
            for(BlockPos p:BlockPos.betweenClosed(pos.offset(-12,0,-12),pos.offset(12,40,12))) {
                var state=level.getBlockState(p);if(state.is(Content.block(family+"_log")))logs++;
                if(state.is(Content.block(family+"_leaves"))) {
                    leaves++;
                    check(!state.getValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT),"Generated "+family+" leaves are natural");
                    int distance=state.getValue(net.minecraft.world.level.block.LeavesBlock.DISTANCE);
                    if(distance==1) {
                        attached++;
                        check(java.util.Arrays.stream(Direction.values()).anyMatch(direction->level.getBlockState(p.relative(direction)).is(net.minecraft.tags.BlockTags.LOGS)),"Distance-one "+family+" leaf touches a log");
                    }
                    if(distance>1&&distance<=4)outer++;
                }
            }
            check(logs>=6&&leaves>=20,"Generated "+family+" contains trunk and canopy");
            check(attached>0&&outer>0,"Generated "+family+" leaves have resolved log distances");
        }
        for(String family:List.of("greatwood","silverwood")) {
            BlockPos ground=origin.east(family.equals("greatwood")?64:66);
            level.setBlockAndUpdate(ground,Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(ground.above(),Blocks.AIR.defaultBlockState());
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Content.item(family+"_leaves")));
            var placement=new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(ground),Direction.UP,ground,false));
            check(player.getMainHandItem().getItem().useOn(placement).consumesAction(),"Player can place "+family+" leaves");
            var placed=level.getBlockState(ground.above());
            check(placed.is(Content.block(family+"_leaves"))&&placed.getValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT),"Player-placed "+family+" leaves are persistent");
            for(int tick=0;tick<16;tick++)level.getBlockState(ground.above()).randomTick(level,ground.above(),level.getRandom());
            check(level.getBlockState(ground.above()).is(Content.block(family+"_leaves")),"Player-placed "+family+" leaves survive decay ticks");
        }
        BlockPos core=new BlockPos(176,90,112);
        check(EldritchStructures.openEntrance(level,core),"Original entrance blueprint builds at original depth");
        BlockPos center=new BlockPos(core.getX(),6,core.getZ());
        int locks=0;for(BlockPos p:BlockPos.betweenClosed(center.offset(-7,0,-7),center.offset(7,8,7)))if(level.getBlockState(p).is(Content.block("eldritch_lock")))locks++;
        check(locks==4,"Entrance retains four chamber locks");
        BlockPos compassCore=core.above(4);level.setBlockAndUpdate(compassCore,Content.block("eldritch_core").defaultBlockState());
        EldritchIndex.get(level).record(compassCore);
        check(EldritchIndex.get(level).closest(level,core).orElseThrow().equals(compassCore),"Monolith index retains generated core");
        BlockPos a=origin.north(24),b=a.east(5);
        var first=machine(level,a,"arcane_seal");var second=machine(level,b,"arcane_seal");
        for(var seal:List.of(first,second)) {
            seal.setOwner(player.getUUID());
            for(int rune=0;rune<3;rune++) {
                int index=rune;
                var entry=Content.ENTRIES.stream().filter(e->e.source_class().equals("ItemRunicEssence")&&e.meta()==index).findFirst().orElseThrow();
                seal.setItem(18+rune,new ItemStack(Content.item(entry.id())));
            }
            SealPortals.tick(level,seal,2);
        }
        // Exercise real SavedData disk serialization, including private portal/index codecs.
        level.getDataStorage().saveAndJoin();
        try {
            var dataFolder=DimensionType.getStorageFolder(level.dimension(),level.getServer().getWorldPath(LevelResource.ROOT)).resolve("data");
            var portal=NbtIo.readCompressed(Thaumcraft.id("seal_portals.dat").resolveAgainst(dataFolder),NbtAccounter.unlimitedHeap()).getListOrEmpty("data");
            check(portal.size()==2,"Both portal endpoints survive disk serialization");
            var positions=new HashSet<BlockPos>();
            for(var tag:portal) {
                var node=(net.minecraft.nbt.CompoundTag)tag;
                check(node.getStringOr("owner","").equals(player.getUUID().toString())&&node.getIntOr("channel",-1)==2,"Portal owner and channel persisted");
                positions.add(GlobalPos.CODEC.parse(NbtOps.INSTANCE,node.get("location")).getOrThrow().pos());
            }
            check(positions.equals(java.util.Set.of(a,b)),"Portal destinations persisted without duplication");
            var monoliths=NbtIo.readCompressed(Thaumcraft.id("monoliths.dat").resolveAgainst(dataFolder),NbtAccounter.unlimitedHeap()).getListOrEmpty("data");
            check(monoliths.stream().map(t->BlockPos.CODEC.parse(NbtOps.INSTANCE,t).getOrThrow()).anyMatch(compassCore::equals),"Monolith compass index survives disk serialization");
            var research=NbtIo.readCompressed(Thaumcraft.id("arcane_world.dat").resolveAgainst(dataFolder),NbtAccounter.unlimitedHeap());
            var loaded=ArcaneWorldData.CODEC.parse(NbtOps.INSTANCE,research.get("data")).getOrThrow();
            check(loaded.known(player.getUUID()).equals(ArcaneWorldData.researchData(level).known(player.getUUID())),"All player discoveries survive actual disk serialization");
            check(loaded.aura(level,origin).goodVibes()==17&&loaded.aura(level,origin).badVibes()==9,"Aura vibes survive actual disk serialization");
        } catch(java.io.IOException e) {throw new AssertionError("Saved progression must be readable from disk",e);}
        SealPortals.remove(level,a);SealPortals.remove(level,b);level.setBlockAndUpdate(a,Blocks.AIR.defaultBlockState());level.setBlockAndUpdate(b,Blocks.AIR.defaultBlockState());
    }
}
