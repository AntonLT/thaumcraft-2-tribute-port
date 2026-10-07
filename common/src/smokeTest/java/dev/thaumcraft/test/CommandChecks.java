package dev.thaumcraft.test;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.thaumcraft.api.ThaumcraftApi;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import java.nio.file.Files;

/** Drives {@code /thaumcraft2} through the real dispatcher against an offline profile. */
final class CommandChecks {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static int run(MinecraftServer server,String command) throws CommandSyntaxException {
        return server.getCommands().getDispatcher().execute(command,server.createCommandSourceStack().withSuppressedOutput());
    }
    static int run(MinecraftServer server){
        checks=0;
        String name="TcCommandTester";var player=server.services().nameToIdCache().get(name).orElseThrow().id();
        var project=Identifier.parse("thaumcraft2tp:thaumic_restorer");int total=ThaumcraftApi.server().allResearch().size();
        try{
            check(run(server,"thaumcraft2 research list "+name)==0,"Fresh profile knows nothing");
            check(run(server,"thaumcraft2 research grant "+name+" "+project)==1,"Grant reports one change");
            check(ThaumcraftApi.knows(server,player,project),"Grant persists through the API");
            check(run(server,"thaumcraft2 research grant "+name+" "+project)==0,"Repeated grant changes nothing");
            check(run(server,"thaumcraft2 research list "+name+" "+project)==1,"List with an ID checks knowledge");
            try{run(server,"thaumcraft2 research grant "+name+" example:definitely_absent");check(false,"Unknown research is rejected");}
            catch(CommandSyntaxException expected){checks++;}
            check(run(server,"thaumcraft2 research grant "+name+" all")==total-1,"Grant all adds the remaining catalog");
            check(run(server,"thaumcraft2 research list "+name)==total,"List counts all known research");
            check(run(server,"thaumcraft2 research revoke "+name+" "+project)==1,"Revoke reports one change");
            check(!ThaumcraftApi.knows(server,player,project),"Revoke persists through the API");
            check(run(server,"thaumcraft2 research revoke "+name+" all")==total-1,"Revoke all clears the rest");
            check(ThaumcraftApi.known(server,player).isEmpty(),"Profile is clean after revoke all");
            check(run(server,"thaumcraft2 catalog dump")==total,"Dump reports the research count");
            var dump=server.getServerDirectory().resolve("thaumcraft2tp/catalog-dump.json");
            check(Files.readString(dump).contains("\""+project+"\""),"Dump contains built-in research");
            check(Files.readString(dump).contains("\"name\": \"Thaumic Restorer\""),"Dedicated catalog dump retains readable English research names");
            var gamemaster=server.createCommandSourceStack().withPermission(net.minecraft.server.permissions.LevelBasedPermissionSet.GAMEMASTER);
            check(server.getCommands().getDispatcher().getRoot().getChild("thaumcraft2").canUse(gamemaster),"Operators may use the command");
            check(!server.getCommands().getDispatcher().getRoot().getChild("thaumcraft2").canUse(server.createCommandSourceStack().withPermission(net.minecraft.server.permissions.PermissionSet.NO_PERMISSIONS)),"Non-operators may not");
        }catch(CommandSyntaxException|java.io.IOException e){throw new AssertionError("Command check failed",e);}
        finally{for(var id:ThaumcraftApi.known(server,player))ThaumcraftApi.revoke(server,player,id);}
        return checks;
    }
}
