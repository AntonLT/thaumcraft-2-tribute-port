package dev.thaumcraft.test;

import dev.thaumcraft.Thaumcraft;
import dev.thaumcraft.api.ThaumcraftApi;
import dev.thaumcraft.client.ResearchScreen;
import dev.thaumcraft.content.Content;
import dev.thaumcraft.gameplay.GameData;
import dev.thaumcraft.item.ItemState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Real resources, language reload, research/seal screens and server command packets. */
final class LocalizationClientChecks {
    private static int ticks,stage;
    private static CompletableFuture<Void> reload;
    private static Component heldName;
    private static java.util.Set<Identifier> originalKnowledge;
    private static dev.thaumcraft.gameplay.AddonData.Snapshot snapshotBefore;
    private static final List<String> MENUS=List.of("quaesitum","arcane_furnace","arcane_bore","vis_condenser","darkness_generator","thaumic_crystalizer","thaumic_duplicator","thaumic_restorer","thaumic_infuser","dark_infuser","thaumic_enchanter","brazier_of_souls","thaumic_generator","trunk","void_chest");
    private static int menuIndex,jeiIndex;
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static void screenshot(String suffix){
        var mc=Minecraft.getInstance();
        Screenshot.grab(mc.gameDirectory,"localization-"+mc.getLanguageManager().getSelected()+"-"+suffix+".png",mc.getMainRenderTarget(),1,result->Thaumcraft.LOG.info("Localization screenshot: {}",result.getString()));
    }
    private static void verifyLanguage(){
        var mc=Minecraft.getInstance();boolean uk=mc.getLanguageManager().getSelected().equals("uk_ua");
        var project=ThaumcraftApi.client().research(Identifier.parse("thaumcraft2tp:thaumic_restorer")).orElseThrow();
        check(project.name().getString().equals(uk?"Таумічний відновлювач":"Thaumic Restorer"),"Received research resolves in the selected language");
        check(Component.translatableWithFallback("example.missing.key","Readable fallback").getString().equals("Readable fallback"),"Missing explicit key remains readable");
        check(heldName.getString().equals(uk?"Таумічний відновлювач":"Thaumic Restorer"),"Existing component refreshes after language reload");
        ThaumcraftApi.client().research(Identifier.parse("example:resonance")).ifPresent(addon->check(addon.name().getString().equals(uk?"Кристалічний резонанс":"Crystal Resonance"),"Client resource pack translates server addon data"));
        var lang=net.minecraft.locale.Language.getInstance();
        try(var input=LocalizationClientChecks.class.getResourceAsStream("/assets/thaumcraft2tp/lang/en_us.json")){
            var keys=com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(input,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            for(String key:keys.keySet())check(lang.has(key),"Loaded language key: "+key);
        }catch(java.io.IOException error){throw new AssertionError(error);}
        var nested=Component.translatable("message.thaumcraft2tp.discovery.learned",project.name());
        check(nested.getString().equals(uk?"Відкриття вивчено: Таумічний відновлювач":"Discovery learned: Thaumic Restorer"),"Nested research name is translated");
        check(Component.translatable("tooltip.thaumcraft2tp.theory.progress",3,5).getString().equals(uk?"Поступ: 3 / 5":"Progress: 3 / 5"),"Progress arguments retain their values");
        for(String glyph:List.of("і","ї","є","ґ"))check(mc.font.width(glyph)>0,"Ukrainian glyph has a font advance");
    }
    private static boolean commandReceived(){
        try{
            var field=net.minecraft.client.gui.components.ChatComponent.class.getDeclaredField("allMessages");field.setAccessible(true);
            var mc=Minecraft.getInstance();
            for(Object message:(List<?>)field.get(mc.gui.getChat())){
                var content=((net.minecraft.client.multiplayer.chat.GuiMessage)message).content();
                if(content.getContents() instanceof TranslatableContents translated&&translated.getKey().equals("commands.thaumcraft2tp.research.list_empty")){
                    boolean uk=mc.getLanguageManager().getSelected().equals("uk_ua");
                    check(content.getString().equals(uk?mc.player.getName().getString()+": відомих досліджень 0":mc.player.getName().getString()+" knows 0 research project(s)"),"Real server feedback translates per client");return true;
                }
            }
            return false;
        }catch(ReflectiveOperationException error){throw new AssertionError(error);}
    }
    private static void menu(){
        var mc=Minecraft.getInstance();var inventory=mc.player.getInventory();String id=MENUS.get(menuIndex);
        if(id.equals("trunk")){
            var menu=new dev.thaumcraft.machine.TrunkMenu(99,inventory,3);mc.player.containerMenu=menu;
            mc.setScreen(new dev.thaumcraft.client.TrunkScreen(menu,inventory,Component.translatable("block.thaumcraft2tp.traveling_trunk")));
        }else if(id.equals("void_chest")){
            var menu=new dev.thaumcraft.machine.VoidMenu(99,inventory);mc.player.containerMenu=menu;
            mc.setScreen(new dev.thaumcraft.client.VoidScreen(menu,inventory,Component.translatable("block.thaumcraft2tp.void_chest")));
        }else{
            var menu=new dev.thaumcraft.machine.MachineMenu(99,inventory,id);mc.player.containerMenu=menu;
            if(id.equals("quaesitum")){menu.getSlot(0).set(ThaumcraftApi.client().theory(Identifier.parse("thaumcraft2tp:thaumic_restorer")).orElseThrow());menu.setData(120,80);menu.setData(121,15);menu.setData(122,5);}
            mc.setScreen(new dev.thaumcraft.client.MachineScreen(menu,inventory,Component.translatable("block.thaumcraft2tp."+id)));
        }
        mc.options.guiScale().set(menuIndex%2==0?2:3);mc.resizeGui();
    }
    private static void jei(){
        try{
            var field=dev.thaumcraft.jei.ThaumcraftJeiPlugin.class.getDeclaredField("runtime");field.setAccessible(true);
            var runtime=(mezz.jei.api.runtime.IJeiRuntime)field.get(null);check(runtime!=null,"Real JEI runtime starts");
            var type=switch(jeiIndex){case 0->dev.thaumcraft.jei.ThaumcraftJeiPlugin.INFUSION;case 1->dev.thaumcraft.jei.ThaumcraftJeiPlugin.DARK_INFUSION;default->dev.thaumcraft.jei.ThaumcraftJeiPlugin.RESEARCH;};
            runtime.getRecipesGui().showTypes(List.of(type));
        }catch(ReflectiveOperationException error){throw new AssertionError(error);}
    }
    static boolean tick(){
        var mc=Minecraft.getInstance();if(++ticks>1800)throw new AssertionError("Localization client timed out at stage "+stage);
        mc.getToastManager().clear();
        if(mc.getOverlay()!=null)return false;
        if(stage==0){
            heldName=ThaumcraftApi.client().research(Identifier.parse("thaumcraft2tp:thaumic_restorer")).orElseThrow().name();
            verifyLanguage();
            mc.player.connection.sendCommand("thaumcraft2 research list "+mc.player.getName().getString());
            mc.setScreen(new ResearchScreen(ThaumcraftApi.client().discovery(Identifier.parse("thaumcraft2tp:thaumic_restorer")).orElseThrow()));
            stage=1;ticks=0;
        }else if(stage==1&&ticks>=20&&commandReceived()){
            screenshot("discovery");
            snapshotBefore=dev.thaumcraft.gameplay.AddonData.current();mc.player.connection.sendCommand("reload");stage=6;ticks=0;
        }else if(stage==6&&snapshotBefore!=dev.thaumcraft.gameplay.AddonData.current()){
            verifyLanguage();
            originalKnowledge=dev.thaumcraft.gameplay.ClientResearch.known();
            dev.thaumcraft.gameplay.ClientResearch.update(GameData.projects().stream().map(GameData.Project::key).toList());
            var tome=new ItemStack(Content.item("thaumonomicon"));
            ItemState.setString(tome,"book_known",String.join(",",GameData.projects().stream().map(p->p.key().toString()).toList()));
            mc.setScreen(new ResearchScreen(tome));stage=2;ticks=0;
        }else if(stage==2&&ticks==20){
            screenshot("tome");dev.thaumcraft.gameplay.ClientResearch.update(originalKnowledge);var crystal=new ItemStack(Content.item("crystal_ball"));
            ItemState.setString(crystal,"book_seals","0,1,-1");ItemState.setInt(crystal,"book_rune_0",0);ItemState.setInt(crystal,"book_rune_1",1);ItemState.setInt(crystal,"book_rune_2",-1);
            mc.setScreen(new ResearchScreen(crystal));stage=3;ticks=0;
        }else if(stage==3&&ticks==20){
            screenshot("seal");String next=mc.getLanguageManager().getSelected().equals("uk_ua")?"en_us":"uk_ua";
            mc.getLanguageManager().setSelected(next);mc.options.languageCode=next;reload=mc.reloadResourcePacks();stage=4;ticks=0;
        }else if(stage==4&&reload.isDone()){
            reload.join();verifyLanguage();stage=5;ticks=0;
        }else if(stage==5&&ticks==20){
            screenshot("seal-reloaded");
            menu();stage=7;ticks=0;
        }else if(stage==7&&ticks==20){
            screenshot(MENUS.get(menuIndex));ticks=0;
            if(++menuIndex<MENUS.size())menu();else{jei();stage=8;}
        }else if(stage==8&&ticks==20){
            check(mc.screen!=null&&mc.screen.getClass().getName().startsWith("mezz.jei"),"Localized JEI view opens");
            screenshot("jei-"+jeiIndex);ticks=0;
            if(++jeiIndex<3)jei();else{
                Thaumcraft.LOG.info("THAUMCRAFT_LOCALIZATION_CLIENT_PASS language={} dedicated={} menus={} jei=3",mc.getLanguageManager().getSelected(),mc.getSingleplayerServer()==null,MENUS.size());return true;
            }
        }
        return false;
    }
}
