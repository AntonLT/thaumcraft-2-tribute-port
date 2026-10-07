package dev.thaumcraft.test;

import dev.thaumcraft.content.Content;
import dev.thaumcraft.content.TaintBlock;
import dev.thaumcraft.entity.ModEntities;
import dev.thaumcraft.entity.RelicProjectile;
import dev.thaumcraft.gameplay.ArcaneWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

/** Impact regressions against EntityCustomSplashPotion and ThaumCraftCore. */
public final class PotionParitySmokeTests {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError("Potion parity: "+message);}
    private static void splash(ServerLevel level,BlockPos pos,int mode){
        var potion=new RelicProjectile(ModEntities.RELIC,level);potion.setPos(pos.getX(),pos.getY(),pos.getZ());
        try{var method=RelicProjectile.class.getDeclaredMethod("splash",ServerLevel.class,int.class);method.setAccessible(true);method.invoke(potion,level,mode);}
        catch(ReflectiveOperationException e){throw new AssertionError(e);}
    }
    private static void clear(ServerLevel level,BlockPos center){
        for(BlockPos pos:BlockPos.betweenClosed(center.offset(-7,-7,-7),center.offset(7,7,7))){level.getChunkAt(pos);level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());}
    }
    public static int run(MinecraftServer server){
        checks=0;ServerLevel level=server.overworld();BlockPos pos=new BlockPos(800,280,800);
        boolean spread=dev.thaumcraft.PortConfig.taintSpread;dev.thaumcraft.PortConfig.taintSpread=true;
        try{
            clear(level,pos);var data=ArcaneWorldData.get(level);var before=data.aura(level,pos);
            splash(level,pos,1);check(data.aura(level,pos).vis()==before.vis()+150,"Vis adds 150");
            for(String protector:new String[]{"silverwood_log","totem_of_dawn"}){
                clear(level,pos);level.setBlockAndUpdate(pos,Content.block(protector).defaultBlockState());
                level.setBlockAndUpdate(pos.east(),Blocks.DIRT.defaultBlockState());
                splash(level,pos,2);check(level.getBlockState(pos.east()).is(Blocks.DIRT),protector+" protects terrain");
            }
            clear(level,pos);
            for(BlockPos block:BlockPos.betweenClosed(pos.offset(-5,-5,-5),pos.offset(5,5,5)))level.setBlockAndUpdate(block,Blocks.STONE.defaultBlockState());
            splash(level,pos,2);check(level.getBlockState(pos).is(Blocks.STONE),"Buried stone is not corrupted");
            clear(level,pos);level.setBlockAndUpdate(pos,Blocks.OAK_LEAVES.defaultBlockState());
            splash(level,pos,2);check(level.getBlockState(pos).is(Blocks.OAK_LEAVES),"Isolated leaves need an arcane log");
            clear(level,pos);level.setBlockAndUpdate(pos.east(4),Blocks.DIRT.defaultBlockState());
            splash(level,pos,2);check(level.getBlockState(pos.east(4)).getBlock() instanceof TaintBlock,"Neighbor search reaches beyond the splash sphere");
            clear(level,pos);level.setBlockAndUpdate(pos,Content.block("tainted_soil").defaultBlockState());
            data.changeAura(level,pos,0,-data.aura(level,pos).taint());
            data.addVibes(level,pos,0,-100);
            int bad=data.aura(level,pos).badVibes();splash(level,pos,2);
            check(data.aura(level,pos).badVibes()==bad+25,"Taint adds 25 bad vibes");
            check(level.getBlockState(pos.above()).is(Content.block("taintweed"))||level.getBlockState(pos.above()).is(Content.block("glowing_taintweed")),"Splash grows taintweed");
            clear(level,pos);
            for(int material:new int[]{0,9,11,12,15}){
                level.setBlockAndUpdate(pos,Content.block("tainted_soil").defaultBlockState().setValue(TaintBlock.MATERIAL,material));
                splash(level,pos,3);
                check((level.getBlockState(pos).getBlock() instanceof TaintBlock)==(material>=10),"Purity material limit: "+material);
            }
            clear(level,pos);level.setBlockAndUpdate(pos.east(4),Content.block("tainted_soil").defaultBlockState());
            data.addVibes(level,pos,-100,0);
            int good=data.aura(level,pos).goodVibes();splash(level,pos,3);
            check(level.getBlockState(pos.east(4)).is(Blocks.DIRT),"Purity includes integer-coordinate radius boundary");
            check(data.aura(level,pos).goodVibes()==good+25,"Purity adds 25 good vibes");
            clear(level,pos);level.setBlockAndUpdate(pos.below(),Blocks.STONE.defaultBlockState());
            int removed=0;
            for(int seed=0;seed<90;seed++){
                level.setBlockAndUpdate(pos,Content.block("taint_spore_pod").defaultBlockState());
                level.getRandom().setSeed(seed);boolean expected=net.minecraft.util.RandomSource.create(seed).nextInt(3)==0;
                splash(level,pos,3);boolean actual=level.getBlockState(pos).isAir();
                check(actual==expected,"Pod cleanup follows one-in-three roll, seed "+seed);if(actual)removed++;
            }
            check(removed>0&&removed<90,"Both pod outcomes occur");
            var player=new net.minecraft.server.level.ServerPlayer(server,level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"PotionParity"),net.minecraft.server.level.ClientInformation.createDefault());
            player.getAbilities().instabuild=true;player.setPos(pos.getCenter());
            for(String id:new String[]{"concentrated_vis","concentrated_taint","potion_of_purity"}){
                var stack=new net.minecraft.world.item.ItemStack(Content.item(id),2);
                var entry=Content.ENTRIES.stream().filter(e->e.id().equals(id)).findFirst().orElseThrow();
                dev.thaumcraft.entity.RelicEntities.use(level,player,net.minecraft.world.InteractionHand.MAIN_HAND,stack,entry);
                check(stack.getCount()==1,"Creative consumes "+id);
            }
        }finally{clear(level,pos);dev.thaumcraft.PortConfig.taintSpread=spread;}
        return checks;
    }
}
