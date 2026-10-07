package dev.thaumcraft.test;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.thaumcraft.gameplay.*;
import dev.thaumcraft.item.ItemState;
import dev.thaumcraft.network.ResearchSync;
import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import java.util.List;
import java.util.UUID;

final class ResearchIdentityChecks {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    static int run(MinecraftServer server){
        checks=0;
        var owner=UUID.fromString("f3bdaf20-94ae-4a9b-99a1-c54412814662");
        var unknown=Identifier.parse("example:removed_research");
        var old=JsonParser.parseString("{\"research\":{\""+owner+"\":[0,0,999,\"example:removed_research\"]}}");
        var migrated=ArcaneWorldData.CODEC.parse(JsonOps.INSTANCE,old).getOrThrow();
        check(migrated.knows(owner,0),"Legacy numeric knowledge migrates");
        check(migrated.known(owner).equals(List.of(GameData.legacyResearchId(0),GameData.legacyResearchId(999),unknown)),"Unknown IDs survive and duplicates collapse");
        var encoded=ArcaneWorldData.CODEC.encodeStart(JsonOps.INSTANCE,migrated).getOrThrow();
        check(encoded.getAsJsonObject().getAsJsonObject("research").getAsJsonArray(owner.toString()).get(0).getAsJsonPrimitive().isString(),"Knowledge writes namespaced strings");
        check(ArcaneWorldData.CODEC.parse(JsonOps.INSTANCE,encoded).getOrThrow().known(owner).equals(migrated.known(owner)),"Unknown knowledge survives round trip");
        for(var project:GameData.projects())check(GameData.project(project.index())==GameData.project(project.key()),"Fixed legacy mapping "+project.id());
        var player=new net.minecraft.server.level.ServerPlayer(server,server.overworld(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"ResearchBook"),net.minecraft.server.level.ClientInformation.createDefault()){
            @Override public void openItemGui(net.minecraft.world.item.ItemStack stack,net.minecraft.world.InteractionHand hand){}
            @Override public int awardRecipes(java.util.Collection<net.minecraft.world.item.crafting.RecipeHolder<?>> recipes){return 0;}
        };
        var discovery=new net.minecraft.world.item.ItemStack(dev.thaumcraft.content.Content.item("discovery_traveling_trunk"));
        var name=discovery.getHoverName();
        ResearchBook.openDiscovery(player,discovery,net.minecraft.world.InteractionHand.MAIN_HAND,21);
        check(discovery.getHoverName().equals(name),"Reading discovery preserves its item name");
        check(!discovery.getOrDefault(net.minecraft.core.component.DataComponents.TOOLTIP_DISPLAY,net.minecraft.world.item.component.TooltipDisplay.DEFAULT).shows(net.minecraft.core.component.DataComponents.WRITTEN_BOOK_CONTENT),"Reading discovery hides book author and generation tooltip");
        var book=discovery.get(net.minecraft.core.component.DataComponents.WRITTEN_BOOK_CONTENT);
        check(book!=null&&!book.pages().isEmpty(),"Discovery retains book content for opening its research screen");
        discovery.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("My discovery"));
        ResearchBook.openDiscovery(player,discovery,net.minecraft.world.InteractionHand.MAIN_HAND,21);
        check(discovery.getHoverName().getString().equals("My discovery"),"Reading discovery preserves anvil names");
        var theory=ResearchLogic.theory(0);
        ItemState.setString(theory,"research_id","");
        ItemState.setInt(theory,"research_progress",3);
        ItemState.setInt(theory,"difficulty",4);
        ResearchItemData.migrate(theory);
        check(ResearchItemData.project(theory).orElseThrow().key().equals(GameData.legacyResearchId(0)),"Legacy theory resolves");
        check(ItemState.getInt(theory,"research_progress",0)==3&&ItemState.getInt(theory,"difficulty",0)==4,"Migration preserves theory state");
        ResearchItemData.setProject(theory,unknown);
        check(ResearchItemData.project(theory).isEmpty(),"Unknown explicit item ID never falls back");
        var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),server.registryAccess());
        try{
            var packet=new ResearchSync(migrated.known(owner));
            ResearchSync.CODEC.encode(buffer,packet);
            check(ResearchSync.CODEC.decode(buffer).equals(packet),"Research packet round trip");
            for(int count:new int[]{-1,16385}){
                buffer.clear();buffer.writeVarInt(count);
                boolean rejected=false;
                try{ResearchSync.CODEC.decode(buffer);}catch(IllegalArgumentException expected){rejected=true;}
                check(rejected,"Reject invalid packet count "+count);
            }
        }finally{buffer.release();}
        return checks;
    }
}
