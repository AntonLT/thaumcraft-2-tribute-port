package dev.alts.tc2tppatches.neoforge;

import com.mojang.authlib.GameProfile;
import dev.alts.tc2tppatches.AltsPatches;
import dev.thaumcraft.api.ThaumcraftCatalog;
import dev.thaumcraft.api.ThaumcraftEvents;
import dev.thaumcraft.content.Content;
import dev.thaumcraft.item.ArcanaItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;

import java.util.List;
import java.util.UUID;

/** Opt-in (-PsmokeTest) checks against the real Curios jar: slot tags, worn goggles, and a charm ticking from its curio slot. */
@Mod(AltsPatches.MOD_ID)
public final class CuriosSmokeTests {
    private static final List<String> CHARMS=List.of("charm_of_life","charm_of_vigor","charm_of_the_dead","charm_of_cleansing","charm_of_souls");
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}

    public CuriosSmokeTests() {
        if(!Boolean.getBoolean("alts.smokeTest"))return;
        ThaumcraftEvents.CATALOG_RELOADED.register(new ThaumcraftEvents.CatalogReloaded() {
            // The first catalog arrives once the server has started; later reloads are not retested.
            boolean ran;
            public void onReloaded(MinecraftServer server,ThaumcraftCatalog catalog) {
                if(ran)return;ran=true;
                try{run(server);}
                catch(Throwable error){AltsPatches.LOG.error("ALTS_CURIOS_SMOKE_FAIL",error);throw error;}
            }
        });
    }

    private static boolean valid(ServerPlayer player,String slot,ItemStack stack){return CuriosApi.isStackValid(new SlotContext(slot,player,0,false,true),stack);}

    static void run(MinecraftServer server) {
        var level=server.overworld();
        var player=new ServerPlayer(server,level,new GameProfile(UUID.randomUUID(),"CuriosSmoke"),ClientInformation.createDefault()) {
            // Capture computed damage without requiring a connected client or applying armor reduction.
            @Override public boolean hurtServer(net.minecraft.server.level.ServerLevel world,net.minecraft.world.damagesource.DamageSource source,float amount){setHealth(getHealth()-amount);return true;}
        };
        var goggles=new ItemStack(Content.item("goggles_of_revealing"));
        check(goggles.is(TagKey.create(Registries.ITEM,Identifier.parse("curios:head"))),"Goggles are in the curios head tag");
        for(String id:CHARMS)check(new ItemStack(Content.item(id)).is(TagKey.create(Registries.ITEM,Identifier.parse("curios:charm"))),id+" is in the curios charm tag");

        var curios=CuriosApi.getCuriosInventory(player).orElseThrow();
        // A player that never joined has no slots yet; this is how Curios builds them for a new one.
        curios.reset();
        check(curios.getCurios().keySet().equals(java.util.Set.of("head","charm","feet")),"The addon gives players exactly the head, charm and built-in feet slots");
        var boots=new ItemStack(Content.item("boots_of_striding"));
        check(valid(player,"feet",boots),"Striding boots fit the feet slot");
        check(!valid(player,"head",boots)&&!valid(player,"charm",boots),"Striding boots only fit the feet slot");
        curios.setEquippedCurio("feet",0,boots);
        check(dev.thaumcraft.gameplay.ArcaneEnchantments.wornBoots(player)==boots,"Accessory boots supply the movement stack");
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET,new ItemStack(net.minecraft.world.item.Items.IRON_BOOTS));
        check(dev.thaumcraft.gameplay.ArcaneEnchantments.wornBoots(player)==boots,"Accessory boots work with ordinary armor boots");
        player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        player.jumpFromGround();
        check(player.getDeltaMovement().y>.7,"Accessory boots boost jumping through the movement mixin");
        player.setHealth(20);
        player.causeFallDamage(8,1,player.damageSources().fall());
        check(player.getHealth()==19,"Accessory boots halve fall distance through the fall mixin");
        var seven=new ItemStack(Content.item("seven_league_boots"));
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET,seven);
        check(dev.thaumcraft.gameplay.ArcaneEnchantments.wornBoots(player)==seven,"Arcane armor boots take precedence without stacking effects");
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET,new ItemStack(net.minecraft.world.item.Items.IRON_BOOTS));
        check(valid(player,"feet",seven),"Seven league boots fit the feet slot");
        curios.setEquippedCurio("feet",0,seven);
        check(dev.thaumcraft.gameplay.ArcaneEnchantments.wornBoots(player)==seven,"Accessory seven league boots supply the movement stack");
        player.setHealth(20);
        player.causeFallDamage(12,1,player.damageSources().fall());
        check(player.getHealth()==19,"Accessory seven league boots cut fall distance to a third");
        var meteor=new ItemStack(Content.item("boots_of_the_meteor"));
        check(valid(player,"feet",meteor),"Meteor boots fit the feet slot");
        curios.setEquippedCurio("feet",0,meteor);
        check(dev.thaumcraft.gameplay.ArcaneEnchantments.wornBoots(player)==meteor,"Accessory meteor boots supply the movement stack");
        player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        player.jumpFromGround();
        check(player.getDeltaMovement().y>1,"Accessory meteor boots boost jumping");
        check(dev.thaumcraft.item.ItemState.getInt(meteor,"stomp_jump",0)==1,"A jump arms the stomp on the accessory meteor boots");
        player.setOnGround(false);player.setShiftKeyDown(true);player.setDeltaMovement(0,-.5,0);
        ArcanaItem.stompTick(meteor,level,player);
        check(player.getDeltaMovement().y<-.5&&dev.thaumcraft.item.ItemState.getInt(meteor,"stomp_fire",0)==1,"Accessory meteor boots accelerate a sneaking descent");
        player.setShiftKeyDown(false);
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET,ItemStack.EMPTY);
        curios.setEquippedCurio("feet",0,ItemStack.EMPTY);
        check(dev.thaumcraft.gameplay.ArcaneEnchantments.wornBoots(player).isEmpty(),"Removing accessory boots removes movement effects");
        check(valid(player,"head",goggles),"Goggles fit the head slot");
        check(!valid(player,"charm",goggles),"Goggles do not fit the charm slot");
        for(String id:CHARMS){
            check(valid(player,"charm",new ItemStack(Content.item(id))),id+" fits the charm slot");
            check(!valid(player,"head",new ItemStack(Content.item(id))),id+" does not fit the head slot");
        }
        check(!valid(player,"head",new ItemStack(Content.item("mask_of_cruelty"))),"The mask needs the real helmet slot and stays out of curio slots");

        check(ArcanaItem.auraRevealer(player).isEmpty(),"Nothing reveals auras before goggles are equipped");
        curios.setEquippedCurio("head",0,goggles);
        check(ArcanaItem.auraRevealer(player).is(goggles.getItem()),"Goggles in a curio slot reveal auras");
        curios.setEquippedCurio("head",0,ItemStack.EMPTY);
        check(ArcanaItem.auraRevealer(player).isEmpty(),"Removing the goggles stops the reveal");

        // Curios ticks equipped stacks from this event, passing no vanilla slot.
        player.setHealth(10);
        curios.setEquippedCurio("charm",0,new ItemStack(Content.item("charm_of_life")));
        NeoForge.EVENT_BUS.post(new EntityTickEvent.Post(player));
        check(player.getHealth()>10,"A charm in a curio slot heals its wearer");
        AltsPatches.LOG.info("ALTS_CURIOS_SMOKE_PASS checks={}",checks);
    }
}
