package dev.thaumcraft.content;

import com.google.gson.Gson;
import dev.thaumcraft.Thaumcraft;
import dev.thaumcraft.item.ArcanaItem;
import dev.thaumcraft.machine.MachineBlock;
import dev.thaumcraft.machine.MachineBlockEntity;
import dev.thaumcraft.machine.MachineMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.equipment.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiConsumer;

public final class Content {
    public record Entry(String id, String name, String legacy, int meta, int icon,
                        String source_class, boolean block, boolean subtype, String kind,
                        int durability, int stack_size, Integer research, String rarity, int enchantability, Boolean glint) {
        // Foci were separate items; catalog metadata is zero for all five.
        public int focusType(){return source_class.equals("ItemFocus")?legacy.charAt(legacy.length()-1)-'0':-1;}
    }

    public static final List<Entry> ENTRIES;
    public static final Map<String, Entry> DEFINITIONS = new LinkedHashMap<>();
    public static final Map<String, Block> BLOCKS = new LinkedHashMap<>();
    public static final Map<String, Item> ITEMS = new LinkedHashMap<>();
    public static BlockEntityType<MachineBlockEntity> MACHINE_ENTITY;
    public static BlockEntityType<dev.thaumcraft.world.VisualBlockEntity> VISUAL_ENTITY;
    public static BlockEntityType<dev.thaumcraft.world.MonolithBlockEntity> MONOLITH_ENTITY;
    public static MenuType<MachineMenu> MACHINE_MENU;
    public static final Map<String,MenuType<MachineMenu>> MACHINE_MENUS=new LinkedHashMap<>();
    public static void registerMachineMenus(java.util.function.BiConsumer<String,MenuType<MachineMenu>> register){
        for(String key:dev.thaumcraft.machine.MachineLayout.ALL.keySet()){
            var type=new MenuType<MachineMenu>((id,inventory)->new MachineMenu(id,inventory,key),FeatureFlags.VANILLA_SET);
            MACHINE_MENUS.put(key,type);register.accept(key,type);
        }
    }
    public static MenuType<MachineMenu> machineMenu(String id){return MACHINE_MENUS.getOrDefault(id,MACHINE_MENU);}
    public static MenuType<dev.thaumcraft.machine.VoidMenu> VOID_MENU;
    public static MenuType<dev.thaumcraft.machine.TrunkMenu> TRUNK_MENU,ROOMY_TRUNK_MENU;
    public static MenuType<dev.thaumcraft.machine.TrunkMenu> createTrunkMenu(boolean roomy){var type=new MenuType<dev.thaumcraft.machine.TrunkMenu>((id,inventory)->new dev.thaumcraft.machine.TrunkMenu(id,inventory,roomy?4:3),FeatureFlags.VANILLA_SET);if(roomy)ROOMY_TRUNK_MENU=type;else TRUNK_MENU=type;return type;}
    public static Block TEMPORARY_SPACE;
    public static final ToolMaterial THAUMIUM = new ToolMaterial(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 400, 7, 2, 22, repairTag("thaumium"));
    public static final ToolMaterial VOID = new ToolMaterial(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 2000, 8, 3, 17, repairTag("void"));
    public static final ArmorMaterial THAUMIUM_ARMOR = armor("thaumium",25,2,6,5,2,25,0);
    public static final ArmorMaterial VOID_ARMOR = armor("void",45,3,8,6,3,17,0);

    public static final ArmorMaterial SPECIAL_ARMOR=armor("thaumium",25,1,3,2,1,10,0);
    private static final SoundType TAINT_SOUND=new SoundType(1,1,ModSounds.event("gore"),ModSounds.event("gore"),
            SoundType.STONE.getPlaceSound(),SoundType.STONE.getHitSound(),SoundType.STONE.getFallSound());

    static {
        try (var in = Content.class.getResourceAsStream("/thaumcraft2tp/catalog.json")) {
            if (in == null) throw new IllegalStateException("Missing Thaumcraft content catalog");
            var entries = new ArrayList<>(List.of(new Gson().fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), Entry[].class)));
            for(String kind:List.of("theory","discovery")){
                String cls=kind.equals("theory")?"itemTheory":"itemDiscovery";
                var template=entries.stream().filter(e->e.source_class().equals(cls)).findFirst().orElseThrow();
                entries.add(new Entry(kind+"_generic",kind.equals("theory")?"Theory":"Discovery",template.legacy(),0,template.icon(),cls,false,false,"item",0,1,null,template.rarity(),0,template.glint()));
            }
            ENTRIES=List.copyOf(entries);
            for (Entry entry : ENTRIES) {
                if (DEFINITIONS.put(entry.id(), entry) != null) throw new IllegalStateException("Duplicate item " + entry.id());
            }
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private Content() {}

    private static TagKey<Item> repairTag(String material) {
        return TagKey.create(Registries.ITEM, Thaumcraft.id("repairs_"+material));
    }

    private static ArmorMaterial armor(String name,int durability,int head,int chest,int legs,int feet,int enchant,float toughness) {
        return new ArmorMaterial(durability,Map.of(ArmorType.HELMET,head,ArmorType.CHESTPLATE,chest,ArmorType.LEGGINGS,legs,ArmorType.BOOTS,feet),
                enchant,SoundEvents.ARMOR_EQUIP_IRON,toughness,0,repairTag(name),ResourceKey.create(EquipmentAssets.ROOT_ID,Thaumcraft.id(name)));
    }

    public static void registerBlocks(BiConsumer<String, Block> register) {
        for (Entry entry : ENTRIES) {
            if (!entry.block()) continue;
            var props = BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, Thaumcraft.id(entry.id())))
                    .mapColor(MapColor.COLOR_PURPLE).strength(3,17).sound(SoundType.STONE);
            switch(entry.legacy()) {
                case "mod_ThaumCraft.blockAppWood" -> props.strength(2,6).sound(SoundType.WOOD).lightLevel(state->entry.meta()==0?5:0);
                case "mod_ThaumCraft.blockAppMetal" -> props.strength(3,10.2f).sound(SoundType.METAL).lightLevel(state->entry.meta()<4?5:entry.meta()==4?(state.getValue(MachineBlock.LIT)?13:0):entry.meta()==11?(state.getValue(MachineBlock.LIT)?15:0):0);
                case "mod_ThaumCraft.blockAppStone" -> props.strength(entry.meta()==0?.5f:entry.meta()==5?10:2,9).lightLevel(state->entry.meta()==5?12:0);
                case "mod_ThaumCraft.blockAppFragile" -> props.strength(entry.meta()==0||entry.meta()==5||entry.meta()==8?.25f:entry.meta()==10?0:1,1).sound(SoundType.WOOD).lightLevel(state->entry.meta()==0||entry.meta()==1||entry.meta()==3?5:entry.meta()==4?8:entry.meta()==10?14:0);
                case "mod_ThaumCraft.blockCustomOre" -> props.strength(1.5f,3);
                default -> {}
            }
            Block block;
            switch (entry.kind()) {
                case "machine" -> block = entry.id().equals("vis_conduit") ? new dev.thaumcraft.machine.ConduitBlock(props.noOcclusion()) : new MachineBlock((entry.id().equals("totem_of_dawn")||entry.id().equals("totem_of_dusk")||entry.id().equals("arcane_seal")?props.randomTicks():props).noOcclusion());
                case "log" -> block = new dev.thaumcraft.world.ArcaneLogBlock(props.strength(2.5f,2.5f).sound(SoundType.WOOD).randomTicks());
                case "leaves" -> block = new ArcaneLeavesBlock(props.strength(0.2f,0.2f).sound(SoundType.GRASS).noOcclusion().randomTicks().lightLevel(state->entry.meta()==1?10:entry.meta()==2?7:0));
                case "plant" -> block = new ArcanePlantBlock(props.strength(0,0).sound(SoundType.GRASS).noCollision().randomTicks()
                        .lightLevel(state -> entry.meta()==1?10:entry.meta()==0||entry.meta()==2?7:0));
                case "crystal" -> block = new CrystalBlock(props.strength(.75f,1.5f).noOcclusion().sound(SoundType.GLASS).lightLevel(state -> 12).randomTicks());
                case "taint" -> block = entry.id().equals("tainted_ground")?new dev.thaumcraft.world.ArcaneLogBlock(props.strength(2.5f,2.5f).sound(SoundType.WOOD)):new TaintBlock(entry.id(),props.strength(5,10).sound(TAINT_SOUND).lightLevel(state->2).randomTicks());
                default -> block = entry.id().equals("glowing_nitor")?new NitorBlock(props.lightLevel(state->14).noCollision().noOcclusion()):new Block(props);
            }
            register.accept(entry.id(),block);
            BLOCKS.put(entry.id(),block);
            // BlockCustomWood: flammability 75/spread 2 except depleted meta 14; BlockCustomLeaves: 100/3.
            if(entry.kind().equals("log")&&entry.meta()!=14)((FireBlock)Blocks.FIRE).setFlammable(block,2,75);
            if(entry.kind().equals("leaves"))((FireBlock)Blocks.FIRE).setFlammable(block,3,100);
        }
        Block structureStone=new Block(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK,Thaumcraft.id("eldritch_structure_stone"))).overrideDescription("block.thaumcraft2tp.eldritch_stone").strength(-1,3600000).noLootTable());
        register.accept("eldritch_structure_stone",structureStone);BLOCKS.put("eldritch_structure_stone",structureStone);
        for(String id:List.of("eldritch_core","eldritch_lock","eldritch_monolith","eldritch_receptacle")) {
            Block block=new dev.thaumcraft.world.EldritchBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK,Thaumcraft.id(id))).strength(id.equals("eldritch_lock")?10:-1,3600000).noLootTable().randomTicks().lightLevel(state->10));
            register.accept(id,block);BLOCKS.put(id,block);
        }
        TEMPORARY_SPACE=new dev.thaumcraft.world.TemporarySpaceBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK,Thaumcraft.id("temporary_space")))
                .strength(-1,3600000).noCollision().noOcclusion().noLootTable().randomTicks().lightLevel(state->10));
        register.accept("temporary_space",TEMPORARY_SPACE);
        Block pod=new dev.thaumcraft.world.TaintPodBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK,Thaumcraft.id("taint_spore_pod"))).noCollision().noOcclusion().strength(.5f));
        register.accept("taint_spore_pod",pod);BLOCKS.put("taint_spore_pod",pod);
    }

    public static void registerItems(BiConsumer<String, Item> register) {
        for (Entry entry : ENTRIES) {
            var props = new Item.Properties().setId(ResourceKey.create(Registries.ITEM,Thaumcraft.id(entry.id())));
            Item item;
            if (entry.block()) {
                item = new BlockItem(BLOCKS.get(entry.id()), props.useBlockDescriptionPrefix()) {
                    @Override public net.minecraft.world.InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
                        if(entry.id().equals("traveling_trunk") && context.getLevel() instanceof net.minecraft.server.level.ServerLevel server && context.getPlayer()!=null)
                            return dev.thaumcraft.entity.RelicEntities.useOn(server,context.getPlayer(),context,entry);
                        return super.useOn(context);
                    }
                };
            } else {
                props.stacksTo(Math.clamp(entry.stack_size()==0 ? 64 : entry.stack_size(),1,64));
                String cls = entry.source_class();
                ToolMaterial material = cls.contains("Void") || cls.contains("ElementalCrusher") || cls.contains("ElementalCutter") ? VOID : THAUMIUM;
                if (cls.contains("Sword")) props.sword(material,3,-2.4f);
                else if (cls.contains("Pick") || cls.contains("Crusher")) props.pickaxe(material,1,-2.8f);
                else if (cls.contains("Axe") || cls.contains("Cutter")) props.axe(material,cls.equals("ItemThaumiumAxe")?6:5,-3);
                else if (cls.contains("Shovel")) props.shovel(material,1.5f,-3);
                else if (cls.contains("Hoe")) props.hoe(material,-2,-1);
                else if (cls.contains("Armor") || cls.contains("Boots") || cls.contains("Goggles") || cls.contains("Mask")) {
                    ArmorType type = entry.legacy().contains("Plate") ? ArmorType.CHESTPLATE : entry.legacy().contains("Legs") ? ArmorType.LEGGINGS : entry.legacy().contains("Boots") ? ArmorType.BOOTS : ArmorType.HELMET;
                    props.humanoidArmor(cls.equals("ItemVisGoggles")?ArmorMaterials.GOLD:cls.contains("Void")?VOID_ARMOR:cls.contains("Thaumium")?THAUMIUM_ARMOR:SPECIAL_ARMOR,type);
                    if(entry.durability()>0)props.durability(entry.durability());
                    String equipment=switch(cls){case "ItemVisGoggles"->"goggles";case "ItemMaskCruelty"->"mask";case "ItemStridingBoots"->"bootsstriding";case "ItemSevenBoots"->"bootsseven";case "ItemStompBoots"->"bootsstomp";default->null;};
                    if(equipment!=null)props.component(net.minecraft.core.component.DataComponents.EQUIPPABLE,net.minecraft.world.item.equipment.Equippable.builder(type.getSlot()).setAsset(net.minecraft.resources.ResourceKey.create(net.minecraft.world.item.equipment.EquipmentAssets.ROOT_ID,Thaumcraft.id(equipment))).build());
                } else if (entry.durability()>0&&!cls.equals("ItemCharmSouls")) props.durability(entry.durability());
                if(cls.equals("ItemFocus"))props.durability(entry.focusType()==0?2500:2000);
                if(cls.equals("ItemVoidCutter")||cls.equals("ItemElementalCutter"))props.delayedComponent(net.minecraft.core.component.DataComponents.BLOCKS_ATTACKS,context->new net.minecraft.world.item.component.BlocksAttacks(
                        0.1f,1f,java.util.List.of(new net.minecraft.world.item.component.BlocksAttacks.DamageReduction(90,java.util.Optional.empty(),0,.5f)),
                        new net.minecraft.world.item.component.BlocksAttacks.ItemDamageFunction(3,1,1),java.util.Optional.of(context.getOrThrow(net.minecraft.tags.DamageTypeTags.BYPASSES_SHIELD)),
                        java.util.Optional.of(net.minecraft.sounds.SoundEvents.SHIELD_BLOCK),java.util.Optional.of(net.minecraft.sounds.SoundEvents.SHIELD_BREAK)));
                if(entry.rarity()!=null)props.rarity(Rarity.valueOf(entry.rarity().toUpperCase(java.util.Locale.ROOT)));
                if(entry.enchantability()>0)props.enchantable(entry.enchantability());
                if(entry.glint()!=null)props.component(net.minecraft.core.component.DataComponents.ENCHANTMENT_GLINT_OVERRIDE,entry.glint());
                item = cls.equals("ItemElementalBowBone") ? new dev.thaumcraft.item.BoneBowItem(props.enchantable(5)) : new ArcanaItem(props,entry);
            }
            register.accept(entry.id(),item);
            ITEMS.put(entry.id(),item);
        }
    }

    public static MenuType<MachineMenu> createMachineMenu() {
        return MACHINE_MENU = new MenuType<>(MachineMenu::new,FeatureFlags.VANILLA_SET);
    }
    public static MenuType<dev.thaumcraft.machine.VoidMenu> createVoidMenu() {
        return VOID_MENU=new MenuType<>(dev.thaumcraft.machine.VoidMenu::new,FeatureFlags.VANILLA_SET);
    }

    public static CreativeModeTab createTab() {
        return CreativeModeTab.builder(CreativeModeTab.Row.TOP,0).title(Component.translatable("itemGroup.thaumcraft2tp"))
                .icon(() -> new ItemStack(item("vis_crystal")))
                .displayItems((params,output) -> ITEMS.forEach((id,item) -> {
                    // The source omits stone metadata 0 (the seal), every BlockTaint
                    // subtype, and research discovery slips from creative inventory.
                    if (!id.startsWith("theory_")&&!id.startsWith("discovery_")
                            &&!id.equals("arcane_seal")&&!id.equals("glowing_nitor")&&!id.equals("tainted_ground")
                            &&!java.util.Set.of("tainted_grass","tainted_soil","tainted_sand","tainted_stone",
                                    "tainted_cobblestone","tainted_sandstone","tainted_gravel","tainted_clay").contains(id)) output.accept(item);
                })).build();
    }

    public static Item item(String id) {
        Item item = ITEMS.get(id);
        if (item == null) throw new IllegalArgumentException("Unknown Thaumcraft item: "+id);
        return item;
    }

    public static Block block(String id) {
        Block block = BLOCKS.get(id);
        if (block == null) throw new IllegalArgumentException("Unknown Thaumcraft block: "+id);
        return block;
    }

    public static Entry entry(ItemStack stack) {
        if (stack.getItem() instanceof ArcanaItem arcana) return arcana.entry();
        var key=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        return key.getNamespace().equals(Thaumcraft.MOD_ID)?DEFINITIONS.get(key.getPath()):null;
    }

    public static Block[] machineBlocks() {
        return BLOCKS.values().stream().filter(b -> b instanceof MachineBlock).toArray(Block[]::new);
    }
    public static java.util.Set<Block> visualBlocks() {
        var blocks=new java.util.HashSet<Block>();
        for(Block block:BLOCKS.values())if(block instanceof CrystalBlock||block instanceof NitorBlock||block instanceof dev.thaumcraft.world.EldritchBlock||block instanceof dev.thaumcraft.world.TaintPodBlock)blocks.add(block);
        blocks.add(TEMPORARY_SPACE);return java.util.Set.copyOf(blocks);
    }
}
