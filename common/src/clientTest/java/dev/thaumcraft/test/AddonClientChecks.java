package dev.thaumcraft.test;

import dev.thaumcraft.Thaumcraft;
import dev.thaumcraft.api.ThaumcraftApi;
import dev.thaumcraft.gameplay.*;
import dev.thaumcraft.jei.ThaumcraftJeiPlugin;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.LevelResource;
import java.nio.file.*;
import java.util.*;

/** Real catalog packet, knowledge packet, book render, and JEI refresh in an integrated client. */
final class AddonClientChecks {
    private static int ticks,stage;
    private static volatile Throwable failure;
    private static List<String> original;
    private static final Identifier PROJECT=Identifier.parse("example:resonance");
    static boolean tick(){
        var mc=Minecraft.getInstance();if(failure!=null)throw new AssertionError("Addon client fixture",failure);
        if(++ticks>1200)throw new AssertionError("Addon client fixture timed out at stage "+stage);
        IJeiRuntime jei;
        try{var field=ThaumcraftJeiPlugin.class.getDeclaredField("runtime");field.setAccessible(true);jei=(IJeiRuntime)field.get(null);}catch(ReflectiveOperationException e){throw new AssertionError(e);}
        if(jei==null)return false;
        if(stage==0){
            stage=1;
            mc.getSingleplayerServer().execute(()->{
                try{
                    var server=mc.getSingleplayerServer();original=List.copyOf(server.getPackRepository().getSelectedIds());
                    Path root=Path.of("").toAbsolutePath();while(root!=null&&!Files.isRegularFile(root.resolve("examples/addon-api/pack.mcmeta")))root=root.getParent();
                    if(root==null)throw new IllegalStateException("Missing example pack");
                    Path source=root.resolve("examples/addon-api"),target=server.getWorldPath(LevelResource.DATAPACK_DIR).resolve("addon-api-example");
                    try(var paths=Files.walk(source)){for(Path path:paths.toList()){Path out=target.resolve(source.relativize(path).toString());if(Files.isDirectory(path))Files.createDirectories(out);else Files.copy(path,out,StandardCopyOption.REPLACE_EXISTING);}}
                    server.getPackRepository().reload();var selected=new ArrayList<>(original);selected.add("file/addon-api-example");
                    server.reloadResources(selected).whenComplete((unused,error)->{if(error!=null)failure=error;else server.execute(()->{try{ThaumcraftApi.unlock(server,mc.player.getUUID(),PROJECT);}catch(Throwable e){failure=e;}});});
                }catch(Throwable e){failure=e;}
            });return false;
        }
        if(stage==1&&ThaumcraftApi.client().research(PROJECT).isPresent()&&ThaumcraftApi.clientKnows(PROJECT)){
            try(var recipes=jei.getRecipeManager().createRecipeLookup(ThaumcraftJeiPlugin.INFUSION).get()){
                if(recipes.noneMatch(r->r.infusion().id().equals("example:echo_shard")))throw new AssertionError("JEI did not receive the unlocked addon infusion");
            }
            mc.setScreen(new dev.thaumcraft.client.ResearchScreen(ThaumcraftApi.client().discovery(PROJECT).orElseThrow()));stage=2;ticks=0;return false;
        }
        if(stage==2&&ticks==25){
            net.minecraft.client.Screenshot.grab(mc.gameDirectory,"addon-research.png",mc.getMainRenderTarget(),1,result->Thaumcraft.LOG.info("Addon book screenshot: {}",result.getString()));
            mc.getSingleplayerServer().execute(()->mc.getSingleplayerServer().reloadResources(original).exceptionally(error->{failure=error;return null;}));stage=3;
        }
        if(stage==3&&GameData.findProject(PROJECT).isEmpty()){
            try(var recipes=jei.getRecipeManager().createRecipeLookup(ThaumcraftJeiPlugin.INFUSION).get()){
                if(recipes.anyMatch(r->r.infusion().id().equals("example:echo_shard")))throw new AssertionError("Removed addon infusion remains visible in JEI");
            }
            if(!ClientResearch.known().contains(PROJECT))throw new AssertionError("Client forgot dormant knowledge");
            Thaumcraft.LOG.info("THAUMCRAFT_ADDON_CLIENT_PASS");return true;
        }
        return false;
    }
}
