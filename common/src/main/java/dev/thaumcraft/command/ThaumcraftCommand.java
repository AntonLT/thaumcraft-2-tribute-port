package dev.thaumcraft.command;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.thaumcraft.api.Research;
import dev.thaumcraft.api.ThaumcraftApi;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.item.ItemStack;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

/**
 * {@code /thaumcraft2}: operator tools for servers and pack testing. Deliberately uses only {@code dev.thaumcraft.api},
 * so it doubles as a consumer of the public addon API.
 */
public final class ThaumcraftCommand {
    private static final DynamicCommandExceptionType UNKNOWN=new DynamicCommandExceptionType(id->Component.translatableWithFallback("commands.thaumcraft2tp.research.unknown", "Unknown research %s", id.toString()));
    private static final SimpleCommandExceptionType DUMP_FAILED=new SimpleCommandExceptionType(Component.translatableWithFallback("commands.thaumcraft2tp.dump.failed", "Cannot write the catalog dump; see the server log"));
    private static final SuggestionProvider<CommandSourceStack> RESEARCH=(context,builder)->
            SharedSuggestionProvider.suggestResource(ThaumcraftApi.server().allResearch().stream().map(Research::id),builder);
    private ThaumcraftCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher){
        dispatcher.register(Commands.literal("thaumcraft2").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("research")
                        .then(Commands.literal("grant").then(Commands.argument("targets",GameProfileArgument.gameProfile())
                                .then(Commands.literal("all").executes(c->grantAll(c.getSource(),targets(c))))
                                .then(Commands.argument("id",IdentifierArgument.id()).suggests(RESEARCH).executes(c->grant(c.getSource(),targets(c),IdentifierArgument.getId(c,"id"))))))
                        .then(Commands.literal("revoke").then(Commands.argument("targets",GameProfileArgument.gameProfile())
                                .then(Commands.literal("all").executes(c->revokeAll(c.getSource(),targets(c))))
                                .then(Commands.argument("id",IdentifierArgument.id()).suggests(RESEARCH).executes(c->revoke(c.getSource(),targets(c),IdentifierArgument.getId(c,"id"))))))
                        .then(Commands.literal("list").then(Commands.argument("targets",GameProfileArgument.gameProfile())
                                .executes(c->list(c.getSource(),targets(c)))
                                .then(Commands.argument("id",IdentifierArgument.id()).suggests(RESEARCH).executes(c->knows(c.getSource(),targets(c),IdentifierArgument.getId(c,"id")))))))
                .then(Commands.literal("catalog").then(Commands.literal("dump").executes(c->dump(c.getSource())))));
    }

    private static Collection<NameAndId> targets(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return GameProfileArgument.getGameProfiles(context,"targets");
    }

    private static int grant(CommandSourceStack source,Collection<NameAndId> targets,Identifier id) throws CommandSyntaxException {
        if(ThaumcraftApi.server().research(id).isEmpty())throw UNKNOWN.create(id);
        int changed=0;
        for(var target:targets)if(ThaumcraftApi.unlock(source.getServer(),target.id(),id))changed++;
        int count=changed;
        source.sendSuccess(()->Component.translatableWithFallback("commands.thaumcraft2tp.research.grant", "Granted %s to %s of %s player(s)", id.toString(), count, targets.size()),true);
        return changed;
    }

    private static int grantAll(CommandSourceStack source,Collection<NameAndId> targets){
        int changed=0;
        for(var target:targets)for(var research:ThaumcraftApi.server().allResearch())
            if(ThaumcraftApi.unlock(source.getServer(),target.id(),research.id()))changed++;
        int count=changed;
        source.sendSuccess(()->Component.translatableWithFallback("commands.thaumcraft2tp.research.grant_all", "Granted %s research project(s) to %s player(s)", count, targets.size()),true);
        return changed;
    }

    /** Accepts IDs outside the catalog, so dormant knowledge from a removed addon can be cleared. */
    private static int revoke(CommandSourceStack source,Collection<NameAndId> targets,Identifier id){
        int changed=0;
        for(var target:targets)if(ThaumcraftApi.revoke(source.getServer(),target.id(),id))changed++;
        int count=changed;
        source.sendSuccess(()->Component.translatableWithFallback("commands.thaumcraft2tp.research.revoke", "Revoked %s from %s of %s player(s)", id.toString(), count, targets.size()),true);
        return changed;
    }

    private static int revokeAll(CommandSourceStack source,Collection<NameAndId> targets){
        int changed=0;
        for(var target:targets)for(var id:ThaumcraftApi.known(source.getServer(),target.id()))
            if(ThaumcraftApi.revoke(source.getServer(),target.id(),id))changed++;
        int count=changed;
        source.sendSuccess(()->Component.translatableWithFallback("commands.thaumcraft2tp.research.revoke_all", "Revoked %s research project(s) from %s player(s)", count, targets.size()),true);
        return changed;
    }

    private static int list(CommandSourceStack source,Collection<NameAndId> targets){
        int total=0;
        for(var target:targets){
            List<Identifier> known=ThaumcraftApi.known(source.getServer(),target.id());
            total+=known.size();
            var text=Component.empty();
            for(var id:known.stream().sorted().toList()){if(!text.getSiblings().isEmpty())text.append(", ");text.append(ThaumcraftApi.server().research(id).isPresent()?Component.literal(id.toString()):Component.translatableWithFallback("commands.thaumcraft2tp.research.dormant", "%s (dormant)", id.toString()));}
            source.sendSuccess(()->known.isEmpty()?Component.translatableWithFallback("commands.thaumcraft2tp.research.list_empty", "%s knows %s research project(s)", target.name(), known.size()):Component.translatableWithFallback("commands.thaumcraft2tp.research.list", "%s knows %s research project(s): %s", target.name(), known.size(), text),false);
        }
        return total;
    }

    private static int knows(CommandSourceStack source,Collection<NameAndId> targets,Identifier id){
        int knowing=0;
        for(var target:targets){
            boolean known=ThaumcraftApi.knows(source.getServer(),target.id(),id);
            if(known)knowing++;
            source.sendSuccess(()->known?Component.translatableWithFallback("commands.thaumcraft2tp.research.knows", "%s knows %s", target.name(), id.toString()):Component.translatableWithFallback("commands.thaumcraft2tp.research.not_known", "%s does not know %s", target.name(), id.toString()),false);
        }
        return knowing;
    }

    private static int dump(CommandSourceStack source) throws CommandSyntaxException {
        var server=source.getServer();var catalog=ThaumcraftApi.server();
        var root=new JsonObject();root.addProperty("api_version",ThaumcraftApi.apiVersion());
        var categories=new JsonArray();
        for(var category:catalog.categories()){
            var json=new JsonObject();json.addProperty("id",category.id().toString());json.addProperty("name",category.name().getString());json.addProperty("order",category.order());
            categories.add(json);
        }
        root.add("categories",categories);
        var research=new JsonArray();
        for(var project:catalog.allResearch()){
            var json=new JsonObject();
            json.addProperty("id",project.id().toString());json.addProperty("category",project.category().toString());
            json.addProperty("name",project.name().getString());json.addProperty("description",project.description().getString());
            json.addProperty("difficulty",project.difficulty());json.addProperty("steps",project.steps());json.addProperty("restricted",project.restricted());
            var prerequisites=new JsonArray();project.prerequisites().forEach(id->prerequisites.add(id.toString()));json.add("prerequisites",prerequisites);
            research.add(json);
        }
        root.add("research",research);
        var infusions=new JsonArray();
        for(var infusion:catalog.infusions()){
            var json=new JsonObject();
            json.addProperty("id",infusion.id().toString());json.addProperty("mode",infusion.dark()?"dark":"normal");json.addProperty("cost",infusion.cost());
            var ingredients=new JsonArray();infusion.ingredients().forEach(ingredients::add);json.add("ingredients",ingredients);
            var result=infusion.result();json.addProperty("result",BuiltInRegistries.ITEM.getKey(result.getItem()).toString());json.addProperty("count",result.getCount());
            if(!result.getComponentsPatch().isEmpty())json.add("components",net.minecraft.core.component.DataComponentPatch.CODEC.encodeStart(source.getServer().registryAccess().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE),result.getComponentsPatch()).getOrThrow());
            infusion.research().ifPresent(id->json.addProperty("research",id.toString()));
            infusions.add(json);
        }
        root.add("infusions",infusions);
        var locked=new JsonObject();
        server.getRecipeManager().getRecipes().stream().map(holder->holder.id().identifier()).sorted()
                .forEach(recipe->catalog.requiredResearch(recipe).ifPresent(id->locked.addProperty(recipe.toString(),id.toString())));
        root.add("locked_recipes",locked);
        var values=new JsonObject();
        for(var item:BuiltInRegistries.ITEM){
            var stack=new ItemStack(item);if(stack.isEmpty())continue;
            float vis=catalog.vis(stack);int value=catalog.researchValue(stack);
            if(vis<=0&&value<=0)continue;
            var json=new JsonObject();json.addProperty("vis",vis);json.addProperty("research",value);
            values.add(BuiltInRegistries.ITEM.getKey(item).toString(),json);
        }
        root.add("item_values",values);
        Path file=server.getServerDirectory().resolve("thaumcraft2tp").resolve("catalog-dump.json");
        try{Files.createDirectories(file.getParent());Files.writeString(file,new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(root));}
        catch(IOException e){dev.thaumcraft.Thaumcraft.LOG.error("Cannot write catalog dump {}",file,e);throw DUMP_FAILED.create();}
        source.sendSuccess(()->Component.translatableWithFallback("commands.thaumcraft2tp.dump.success", "Dumped %s categories, %s research, %s infusions, %s locked recipes and %s item values to %s", categories.size(), research.size(), infusions.size(), locked.size(), values.size(), file.toAbsolutePath().normalize().toString()),false);
        return research.size();
    }
}
