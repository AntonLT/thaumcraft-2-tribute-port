package dev.thaumcraft.test;

import dev.thaumcraft.content.Content;
import dev.thaumcraft.api.IntegrationHooks;
import dev.thaumcraft.machine.MachineBlockEntity;
import dev.thaumcraft.machine.MachineLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import java.util.function.Predicate;

/** Tests replacement integration contracts with explicit adapters, without claiming external mods are installed. */
final class IntegrationSmokeTests {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError("Integration: "+message);}
    static int run(MinecraftServer server){
        checks=0;var level=server.overworld();var pos=new BlockPos(416,280,416);level.getChunkAt(pos);
        var empty=new ItemStack(Items.BOWL,3);
        try(var registration=IntegrationHooks.registerWaterContainer(stack->stack.is(Items.BOWL)?new ItemStack(Items.WATER_BUCKET):ItemStack.EMPTY)){
            check(IntegrationHooks.fillWater(empty).is(Items.WATER_BUCKET)&&empty.getCount()==3,"Custom water-container mapping converts one item without mutating caller input");
        }
        check(IntegrationHooks.fillWater(empty).isEmpty(),"Closed water registration no longer handles containers");
        check(IntegrationHooks.fillWater(new ItemStack(Items.GLASS_BOTTLE)).is(Items.POTION),"Vanilla water bottle remains available without adapters");
        check(IntegrationHooks.backpackAllows(IntegrationHooks.BackpackCategory.MINER,new ItemStack(Content.item("quicksilver")))&&!IntegrationHooks.backpackAllows(IntegrationHooks.BackpackCategory.FORESTER,new ItemStack(Content.item("quicksilver"))),"Backpack adapter preserves original miner category");
        check(IntegrationHooks.backpackAllows(IntegrationHooks.BackpackCategory.FORESTER,new ItemStack(Content.item("silverwood_log")))&&IntegrationHooks.backpackAllows(IntegrationHooks.BackpackCategory.BUILDER,new ItemStack(Content.item("eldritch_stone"))),"Backpack adapter exposes original forestry and builder items");
        check(IntegrationHooks.softForQuarry(Content.block("eldritch_lock").defaultBlockState())&&!IntegrationHooks.softForQuarry(Blocks.STONE.defaultBlockState()),"Optional quarry predicate marks source hidden blocks only");
        int[] actions=new int[3];
        try(var registration=IntegrationHooks.registerCrop((world,target,action)->{if(!target.equals(pos))return false;actions[action.ordinal()]++;return true;})){
            for(var action:IntegrationHooks.CropAction.values())check(IntegrationHooks.crop(level,pos,action),"Registered crop handles "+action);
            check(java.util.Arrays.equals(actions,new int[]{1,1,1})&&!IntegrationHooks.crop(level,pos.above(),IntegrationHooks.CropAction.GROW),"Crop adapters select owned positions and dispatch each action once");
        }
        check(!IntegrationHooks.crop(level,pos,IntegrationHooks.CropAction.GROW),"Unregistered crop adapters are not invoked");
        int[] stored={4};
        var access=new IntegrationHooks.ItemAccess(){
            public int insert(ItemStack stack,boolean simulate){if(!stack.is(Items.STICK))return 0;int amount=Math.min(stack.getCount(),10-stored[0]);if(!simulate)stored[0]+=amount;return amount;}
            public ItemStack extract(Predicate<ItemStack> filter,int maximum,boolean simulate){var stack=new ItemStack(Items.STICK,Math.min(maximum,stored[0]));if(!filter.test(stack))return ItemStack.EMPTY;if(!simulate)stored[0]-=stack.getCount();return stack;}
        };
        try(var registration=IntegrationHooks.registerInventory((world,target,face)->target.equals(pos)&&face==Direction.WEST?access:null)){
            check(IntegrationHooks.inventory(level,pos,Direction.WEST).insert(new ItemStack(Items.STICK,20),true)==6&&stored[0]==4,"Pipe insertion simulation preserves contents");
            var offered=new ItemStack(Items.STICK,20);var remaining=IntegrationHooks.insert(level,pos,Direction.WEST,offered);
            check(remaining.getCount()==14&&offered.getCount()==20&&stored[0]==10,"Pipe insertion preserves caller stack and returns exact remainder");
            check(IntegrationHooks.extract(level,pos,Direction.WEST,s->s.is(Items.COAL),3).isEmpty()&&stored[0]==10,"Pipe extraction respects item predicate");
            check(IntegrationHooks.extract(level,pos,Direction.WEST,s->s.is(Items.STICK),3).getCount()==3&&stored[0]==7,"Pipe extraction respects requested maximum");
            check(IntegrationHooks.insert(level,pos,Direction.EAST,new ItemStack(Items.STICK)).getCount()==1,"Unsupported pipe face does not consume input");
        }
        level.setBlockAndUpdate(pos,Content.block("arcane_furnace").defaultBlockState());var furnace=(MachineBlockEntity)level.getBlockEntity(pos);
        check(IntegrationHooks.insert(level,pos,Direction.DOWN,new ItemStack(Items.COAL,2)).isEmpty()&&furnace.getItem(MachineBlockEntity.FUEL_SLOT).getCount()==2,"Default inventory adapter honors furnace bottom fuel face");
        check(IntegrationHooks.insert(level,pos,Direction.UP,new ItemStack(Items.COAL,2)).getCount()==2,"Default inventory adapter refuses input through output-only face");
        furnace.setItem(9,new ItemStack(Items.IRON_INGOT,3));
        check(IntegrationHooks.extract(level,pos,Direction.UP,s->s.is(Items.IRON_INGOT),2).getCount()==2&&furnace.getItem(9).getCount()==1,"Default inventory adapter extracts only allowed sided slots");
        level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());level.setBlockAndUpdate(pos,Content.block("thaumic_generator").defaultBlockState());var generator=(MachineBlockEntity)level.getBlockEntity(pos);generator.generateEnergy(100);
        check(!level.hasNeighborSignal(pos.above()),"Stored generator energy does not emit invented redstone and disable its own output");
        level.getChunkAt(pos.north());int[] received={0};
        try(var registration=IntegrationHooks.registerEnergy((world,target,face,offered)->{if(!target.equals(pos.north()))return 0;int accepted=Math.min(24,offered);received[0]+=accepted;return accepted;})){
            for(int i=0;i<4;i++)MachineLogic.generator(level,generator);
            check(received[0]==60&&generator.energy()==40&&generator.generatorOutput.available(60)==0,"Optional bridges share a 60 FE budget with capability extraction across repeated calls");
            level.setBlockAndUpdate(pos.above(),Blocks.REDSTONE_BLOCK.defaultBlockState());MachineLogic.generator(level,generator);
            check(received[0]==60&&generator.energy()==40,"Redstone blocks integration energy output");
        }
        level.setBlockAndUpdate(pos.above(),Blocks.AIR.defaultBlockState());level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
        return checks;
    }
}
