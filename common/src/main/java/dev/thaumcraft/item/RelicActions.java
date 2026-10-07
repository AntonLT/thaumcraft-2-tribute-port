package dev.thaumcraft.item;

import dev.thaumcraft.content.Content;
import dev.thaumcraft.entity.RelicEntities;
import dev.thaumcraft.world.TemporarySpace;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.*;

public final class RelicActions {
    private RelicActions() {}
    public static InteractionResult use(ServerLevel level,ServerPlayer player,InteractionHand hand,ItemStack stack,Content.Entry entry) {
        if(entry.source_class().equals("ItemVoidBracelet")) {
            int rune=ItemState.getInt(stack,"seal_rune",-1);
            if(!dev.thaumcraft.world.SealPortals.travelByBracelet(player,rune)) {
                player.sendSystemMessage(Component.translatableWithFallback("message.thaumcraft2tp.destination.none", "No valid destinations found."));return InteractionResult.FAIL;
            }
            return InteractionResult.SUCCESS;
        }
        return RelicEntities.use(level,player,hand,stack,entry);
    }
    public static InteractionResult useOn(ServerLevel level,Player player,UseOnContext context,Content.Entry entry) {
        BlockPos pos=context.getClickedPos();ItemStack stack=context.getItemInHand();String cls=entry.source_class();
        if(cls.equals("ItemWandReversal")&&level.getBlockEntity(pos) instanceof dev.thaumcraft.machine.MachineBlockEntity machine) {
            // Only installed slots: seal runes or the machine's upgrade range; Void storage has none.
            int start=dev.thaumcraft.machine.MachineBlockEntity.UPGRADE_START,installedSlots=machine.machineId().equals("arcane_seal")?3:dev.thaumcraft.machine.MachineBlockEntity.upgradeLimit(machine.machineId());
            for(int slot=start+installedSlots-1;slot>=start;slot--) {
                var installed=machine.getItem(slot);if(installed.isEmpty())continue;
                var definition=Content.entry(installed);if(definition==null||!(definition.source_class().equals("ItemUpgrades")||definition.source_class().equals("ItemRunicEssence")))continue;
                var removed=machine.removeItem(slot,1);
                if(machine.machineId().equals("arcane_seal"))dev.thaumcraft.machine.SealLogic.runesChanged(machine);
                int potency=dev.thaumcraft.gameplay.ArcaneEnchantments.level(level,stack,"potency");
                if(level.getRandom().nextInt(6+potency*2)!=0){
                    var drop=new net.minecraft.world.entity.item.ItemEntity(level,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,removed);
                    drop.setDeltaMovement(player.position().subtract(drop.position()).scale(.1));
                    level.addFreshEntity(drop);
                    dev.thaumcraft.content.ModSounds.play(level,pos,"zap",net.minecraft.sounds.SoundSource.BLOCKS,.5f,1);
                }else level.playSound(null,pos,net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH,net.minecraft.sounds.SoundSource.BLOCKS,.33f,1.3f+level.getRandom().nextFloat()*.2f);
                machine.setChanged();if(!machine.machineId().equals("arcane_seal"))dev.thaumcraft.world.SealPortals.remove(level,pos);ArcanaItem.charge(stack,player,context.getHand(),1);return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }
        if(cls.equals("ItemVoidBracelet")&&level.getBlockEntity(pos) instanceof dev.thaumcraft.machine.MachineBlockEntity seal&&seal.machineId().equals("arcane_seal")) {
            int[] runes=dev.thaumcraft.machine.SealLogic.runes(seal);
            if(runes[0]==0&&runes[1]==1&&runes[2]!=ItemState.getInt(stack,"seal_rune",-1)){
                ItemState.setInt(stack,"seal_rune",runes[2]);
                dev.thaumcraft.content.ModSounds.play(level,pos,"zap",net.minecraft.sounds.SoundSource.BLOCKS,.5f,1);
                player.sendSystemMessage(Component.translatableWithFallback("message.thaumcraft2tp.bracelet.linked", "You've linked the bracelet to a new network."));return InteractionResult.SUCCESS;
            }
        }
        if(cls.equals("ItemPortableHole")) {
            int opened=TemporarySpace.get(level).open(level,pos,context.getClickedFace().getOpposite(),player.isCreative()?32:stack.getMaxDamage()-stack.getDamageValue(),player.getUUID());
            if(opened==0)return InteractionResult.FAIL;
            ArcanaItem.charge(stack,player,context.getHand(),opened);
            level.playSound(null,pos,net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT,net.minecraft.sounds.SoundSource.BLOCKS,1,1);
            return InteractionResult.SUCCESS;
        }
        if(cls.equals("ItemWandEqualTrade"))return EqualTrade.use(level,player,context);
        if(cls.contains("Axe") || cls.contains("Cutter")) {
            String name=BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).toString();
            int separator=name.indexOf(':');String stripped=name.substring(0,separator+1)+"stripped_"+name.substring(separator+1);
            Block block=BuiltInRegistries.BLOCK.getValue(Identifier.parse(stripped));
            if(block!=null && block!=Blocks.AIR) {
                var result=block.defaultBlockState();var state=level.getBlockState(pos);
                if(state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS)&&result.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS))result=result.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS,state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS));
                level.setBlockAndUpdate(pos,result);ArcanaItem.charge(stack,player,context.getHand(),1);return InteractionResult.SUCCESS;
            }
        }
        return RelicEntities.useOn(level,player,context,entry);
    }
    public static boolean consumeCrystal(Player player,int meta) {
        if(player.isCreative())return true;
        for(int i=0;i<player.getInventory().getContainerSize();i++) {
            ItemStack stack=player.getInventory().getItem(i);var entry=Content.entry(stack);
            if(entry!=null && entry.legacy().endsWith("itemCrystals") && entry.meta()==meta){stack.shrink(1);return true;}
        }
        return false;
    }
    public static boolean consumeItem(Player player,net.minecraft.world.item.Item item) {
        if(player.isCreative())return true;
        for(int i=0;i<player.getInventory().getContainerSize();i++) {
            ItemStack stack=player.getInventory().getItem(i);if(stack.is(item)){stack.shrink(1);return true;}
        }
        return false;
    }
}
