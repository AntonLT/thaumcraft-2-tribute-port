package dev.alts.tc2tppatches;

import dev.thaumcraft.api.ThaumcraftEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiConsumer;

/** Loader-neutral setup. Players learn an item's vis value by dissolving it in a crucible, cinnabar has a deepslate ore, and both ores drop raw cinnabar. */
public final class AltsPatches {
    public static final String MOD_ID="alts_tc2tp_patches";
    public static final Logger LOG=LoggerFactory.getLogger("Alt's TC2TP patches");
    private AltsPatches() {}
    public static Identifier id(String path){return Identifier.fromNamespaceAndPath(MOD_ID,path);}

    public static final Identifier DEEPSLATE_CINNABAR_ORE=id("deepslate_cinnabar_ore");
    public static final Identifier RAW_CINNABAR=id("raw_cinnabar");
    public static final Identifier CINNABAR_ORE=Identifier.fromNamespaceAndPath("thaumcraft2tp","cinnabar_ore");
    public static final ResourceKey<CreativeModeTab> TAB=ResourceKey.create(Registries.CREATIVE_MODE_TAB,Identifier.fromNamespaceAndPath("thaumcraft2tp","main"));
    public static Block deepslateCinnabarOre;
    public static Item deepslateCinnabarOreItem;
    public static Item rawCinnabar;

    /** Match regular cinnabar's mining behavior; only its appearance and sound differ. */
    public static void registerBlocks(BiConsumer<Identifier,Block> register) {
        deepslateCinnabarOre=new Block(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK,DEEPSLATE_CINNABAR_ORE))
                .mapColor(MapColor.DEEPSLATE).strength(1.5f,3).sound(SoundType.DEEPSLATE));
        register.accept(DEEPSLATE_CINNABAR_ORE,deepslateCinnabarOre);
    }
    public static void registerItems(BiConsumer<Identifier,Item> register) {
        deepslateCinnabarOreItem=new BlockItem(deepslateCinnabarOre,new Item.Properties().setId(ResourceKey.create(Registries.ITEM,DEEPSLATE_CINNABAR_ORE)).useBlockDescriptionPrefix());
        register.accept(DEEPSLATE_CINNABAR_ORE,deepslateCinnabarOreItem);
        rawCinnabar=new Item(new Item.Properties().setId(ResourceKey.create(Registries.ITEM,RAW_CINNABAR)));
        register.accept(RAW_CINNABAR,rawCinnabar);
    }

    public static void init() {
        dev.thaumcraft.api.ThaumcraftApi.setGeneratorEnergyMultiplier(100);
        ThaumcraftEvents.CRUCIBLE_DISSOLVED.register((level,pos,player,dissolved,vis)->{
            if(player==null)return;
            var server=level.getServer();
            if(!VisKnowledge.get(server).learn(player,BuiltInRegistries.ITEM.getKey(dissolved.getItem())))return;
            ServerPlayer online=server.getPlayerList().getPlayer(player);
            if(online==null)return;
            VisKnowledgeSync.sendTo(online);
            online.sendSystemMessage(Component.translatable("message.alts_tc2tp_patches.vis_learned",dissolved.getHoverName(),VisKnowledgeSync.format(vis)),true);
        });
        // Values can change on /reload, so resend what everyone knows with the new values.
        ThaumcraftEvents.CATALOG_RELOADED.register((server,catalog)->server.getPlayerList().getPlayers().forEach(VisKnowledgeSync::sendTo));
        if(Boolean.getBoolean("alts.smokeTest"))ThaumcraftEvents.CATALOG_RELOADED.register(new ThaumcraftEvents.CatalogReloaded() {
            // The first catalog arrives once the server has started; later reloads are not retested.
            boolean ran;
            public void onReloaded(net.minecraft.server.MinecraftServer server,dev.thaumcraft.api.ThaumcraftCatalog catalog) {
                if(ran)return;ran=true;
                try{Class.forName("dev.alts.tc2tppatches.AltsPatchesSmokeTests").getMethod("run",net.minecraft.server.MinecraftServer.class).invoke(null,server);}
                catch(ReflectiveOperationException e){throw new IllegalStateException("Alt's patches smoke test failed",e);}
            }
        });
    }
}
