package dev.alts.tc2tppatches.neoforge;

import com.mojang.blaze3d.platform.NativeImage;
import dev.alts.tc2tppatches.AltsPatches;
import dev.thaumcraft.content.Content;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.client.ICurioRenderer;

import java.util.function.Consumer;

/** Opt-in (-PsmokeTest runClient) check: goggles in a curio slot are drawn on the head, alone and over a helmet. Saves screenshots for review. */
@Mod(value=AltsPatches.MOD_ID,dist=Dist.CLIENT)
public final class CuriosClientChecks {
    private static final String WORLD="AltsCuriosQA";
    private boolean opening;
    private int ticks;
    private NativeImage bare,withGoggles;

    public CuriosClientChecks() {
        if(Boolean.getBoolean("alts.curiosClientSmoke"))NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event)->tick());
    }

    private static void check(boolean value,String message){if(!value)throw new AssertionError("Curios client check: "+message);}
    private static void server(Consumer<ServerPlayer> action) {
        var mc=Minecraft.getInstance();var server=mc.getSingleplayerServer();var id=mc.player.getUUID();
        server.execute(()->action.accept(server.getPlayerList().getPlayer(id)));
    }
    private static void command(String command) {
        var server=Minecraft.getInstance().getSingleplayerServer();
        server.execute(()->server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),command));
    }
    private static void curio(ServerPlayer player,ItemStack stack){CuriosApi.getCuriosInventory(player).orElseThrow().setEquippedCurio("head",0,stack);}
    private static void screenshot(String name) {
        var mc=Minecraft.getInstance();
        Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),1,result->AltsPatches.LOG.info("Curios client screenshot: {}",result.getString()));
    }
    /** Pixels around the screen centre, where the third-person camera puts the head, that changed noticeably. */
    private static int changedAroundHead(NativeImage a,NativeImage b) {
        int cx=a.getWidth()/2,cy=a.getHeight()/2,changed=0;
        for(int x=cx-80;x<cx+80;x++)for(int y=cy-80;y<cy+80;y++) {
            int p=a.getPixel(x,y),q=b.getPixel(x,y),diff=0;
            for(int shift=0;shift<24;shift+=8)diff=Math.max(diff,Math.abs((p>>shift&255)-(q>>shift&255)));
            if(diff>24)changed++;
        }
        return changed;
    }

    private void tick() {
        var mc=Minecraft.getInstance();
        if(mc.level==null||mc.player==null) {
            if(opening||mc.getOverlay()!=null||!(mc.screen instanceof TitleScreen))return;
            opening=true;
            if(mc.getLevelSource().levelExists(WORLD))mc.createWorldOpenFlows().openWorld(WORLD,()->mc.setScreen(new TitleScreen()));
            else mc.createWorldOpenFlows().createFreshLevel(WORLD,
                    new LevelSettings(WORLD,GameType.CREATIVE,LevelSettings.DifficultySettings.DEFAULT,true,WorldDataConfiguration.DEFAULT),
                    new WorldOptions(1,false,false),WorldPresets::createFlatWorldDimensions,new TitleScreen());
            return;
        }
        var goggles=new ItemStack(Content.item("goggles_of_revealing"));
        switch(ticks++) {
            case 40 -> {
                check(ICurioRenderer.getOrNull(goggles) instanceof GogglesCurioRenderer,"Goggles have the curio renderer");
                command("time set noon");command("weather clear");
                server(player->{player.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);curio(player,ItemStack.EMPTY);});
                mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);mc.options.hideGui=true;
                mc.player.setYRot(0);mc.player.setXRot(0);
            }
            case 80 -> {screenshot("alts_curios_0_bare.png");Screenshot.takeScreenshot(mc.getMainRenderTarget(),image->bare=image);}
            case 90 -> server(player->curio(player,goggles.copy()));
            case 130 -> {
                check(CuriosApi.getCuriosInventory(mc.player).orElseThrow().isEquipped(goggles.getItem()),"The client sees the equipped curio");
                screenshot("alts_curios_1_goggles.png");Screenshot.takeScreenshot(mc.getMainRenderTarget(),image->withGoggles=image);
            }
            case 140 -> server(player->player.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.IRON_HELMET)));
            case 180 -> screenshot("alts_curios_2_goggles_over_helmet.png");
            // Reference: the same goggles in the real helmet slot.
            case 190 -> server(player->{curio(player,ItemStack.EMPTY);player.setItemSlot(EquipmentSlot.HEAD,goggles.copy());});
            case 230 -> screenshot("alts_curios_3_helmet_slot_reference.png");
            case 250 -> {
                check(bare!=null&&withGoggles!=null,"Both comparison frames were captured");
                int changed=changedAroundHead(bare,withGoggles);bare.close();withGoggles.close();
                check(changed>300,"Curio goggles change the head ("+changed+" pixels)");
                AltsPatches.LOG.info("ALTS_CURIOS_CLIENT_PASS changed={}",changed);
                mc.stop();
            }
            default -> {}
        }
    }
}
