package dev.thaumcraft.test;

import dev.thaumcraft.Thaumcraft;
import dev.thaumcraft.client.legacy.LegacyDraw;
import dev.thaumcraft.client.legacy.LegacyVisuals;
import dev.thaumcraft.content.Content;
import dev.thaumcraft.content.CrystalBlock;
import dev.thaumcraft.machine.MachineBlock;
import dev.thaumcraft.machine.MachineBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Real client extraction of every block plus all crystal amounts/mounts and machine mounts. */
final class RenderParityChecks {
    private record Case(String name,BlockPos pos) {}
    private static volatile List<Case> cases=List.of();
    private static final Set<String> INVISIBLE=Set.of("glowing_nitor");
    private static void check(boolean result,String description){if(!result)throw new AssertionError("Render parity: "+description);}
    static void build(ServerLevel level){
        level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.RANDOM_TICK_SPEED,0,level.getServer());
        var entries=new LinkedHashMap<>(Content.BLOCKS);entries.put("temporary_space",Content.TEMPORARY_SPACE);
        List<Case> placed=new ArrayList<>();int index=0;
        for(var entry:entries.entrySet()){
            var pos=new BlockPos(34+(index%10)*3,120,3*(index/10));
            place(level,placed,entry.getKey(),pos,entry.getValue().defaultBlockState());index++;
        }
        for(int color=0;color<6;color++)for(Direction facing:Direction.values())for(int amount=1;amount<=5;amount++){
            String id=List.of("vis_ore","vaporous_vis_ore","aqueous_vis_ore","earthen_vis_ore","fiery_vis_ore","tainted_vis_ore").get(color);
            BlockPos pos=new BlockPos(34+facing.get3DDataValue()*3,122,26+color*16+amount*3);
            level.setBlockAndUpdate(pos.relative(facing.getOpposite()),Blocks.SMOOTH_STONE.defaultBlockState());
            place(level,placed,id+"/"+facing+"/"+amount,pos,Content.block(id).defaultBlockState().setValue(CrystalBlock.AMOUNT,amount).setValue(CrystalBlock.FACING,facing));
        }
        for(int kind=0;kind<3;kind++)for(Direction facing:Direction.values()){
            String id=List.of("arcane_bore","vis_pump","arcane_seal").get(kind);BlockPos pos=new BlockPos(55+kind*4,122,26+facing.get3DDataValue()*3);
            place(level,placed,id+"/"+facing,pos,Content.block(id).defaultBlockState().setValue(MachineBlock.FACING,facing));
            var machine=(MachineBlockEntity)level.getBlockEntity(pos);
            if(id.equals("arcane_seal"))machine.setItem(18,new ItemStack(Content.item("runic_essence_fire")));
            if(id.equals("arcane_bore")){var focus=Content.DEFINITIONS.values().stream().filter(e->e.source_class().equals("ItemFocus")&&e.meta()==0).findFirst().orElseThrow();machine.setItem(0,new ItemStack(Content.item(focus.id())));}
        }
        for(int i=0;i<4;i++){
            String id=List.of("crucible","crucible_of_souls","vis_storage_tank","thaumium_reinforced_tank").get(i);
            var pos=new BlockPos(55+3*i,120,48);place(level,placed,id+"/filled",pos,Content.block(id).defaultBlockState());
            var machine=(MachineBlockEntity)level.getBlockEntity(pos);machine.insertVis(machine.capacity()*.55f,false);machine.insertVis(machine.capacity()*.25f,true);
        }
        for(int i=0;i<2;i++){
            String lower=i==0?"vis_storage_tank":"thaumium_reinforced_tank";
            String upper=i==0?"thaumium_reinforced_tank":"vis_storage_tank";
            BlockPos pos=new BlockPos(52+3*i,120,60);
            level.setBlockAndUpdate(pos.below(),Blocks.SMOOTH_STONE.defaultBlockState());
            level.setBlockAndUpdate(pos,Content.block(lower).defaultBlockState());
            level.setBlockAndUpdate(pos.above(),Blocks.AIR.defaultBlockState());
            for(var player:level.getServer().getPlayerList().getPlayers()){
                ItemStack held=player.getItemInHand(InteractionHand.MAIN_HAND).copy();
                ItemStack tankItem=new ItemStack(Content.item(upper));
                player.setItemInHand(InteractionHand.MAIN_HAND,tankItem);
                var hit=new BlockHitResult(Vec3.atBottomCenterOf(pos.above()),Direction.UP,pos,false);
                var result=player.gameMode.useItemOn(player,level,tankItem,InteractionHand.MAIN_HAND,hit);
                player.setItemInHand(InteractionHand.MAIN_HAND,held);
                check(result.consumesAction()&&level.getBlockState(pos.above()).is(Content.block(upper)),"Normal right-click stacks "+upper+" on "+lower);
            }
        }
        cases=List.copyOf(placed);
    }
    private static void place(ServerLevel level,List<Case> placed,String name,BlockPos pos,BlockState state){
        var aura=dev.thaumcraft.gameplay.ArcaneWorldData.get(level);var cell=aura.aura(level,pos);aura.changeAura(level,pos,4000-cell.vis(),4000-cell.taint());
        level.setBlockAndUpdate(pos.below(),Blocks.GRASS_BLOCK.defaultBlockState());
        if(name.equals("void_interface"))level.setBlockAndUpdate(pos.below(),Content.block("void_chest").defaultBlockState());
        if(state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.PERSISTENT))state=state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.PERSISTENT,true);
        level.setBlockAndUpdate(pos,state);placed.add(new Case(name,pos));
    }
    static void verify(){
        var mc=Minecraft.getInstance();int special=0,vertices=0;
        var frame=dev.thaumcraft.client.legacy.LegacyPipelines.material(Thaumcraft.id("textures/legacy/blocks.png"),770,771,true,false);
        check(frame.pipeline().getShaderDefines().values().containsKey("ALPHA_CUTOUT"),"Transparent void chest frame pixels must not occlude the void surface");
        check(frame.hasBlending(),"Legacy atlas overlays retain alpha blending");
        List<Map<String,Object>> records=new ArrayList<>();
        for(var c:cases){
            check(mc.level.hasChunkAt(c.pos),"Client received "+c.name+" at "+c.pos);
            var state=mc.level.getBlockState(c.pos);check(!state.isAir(),"Placed "+c.name+" survives");
            int count=0;
            if(state.getRenderShape()==RenderShape.INVISIBLE){
                var entity=mc.level.getBlockEntity(c.pos);check(entity!=null,"Visual block entity for "+c.name);
                var batches=LegacyVisuals.render(entity,.5f,0xf000f0);
                if(c.name.equals("void_chest"))verifyVoidSubmissions(batches);
                if(c.name.equals("brain_in_a_jar")||c.name.equals("occultic_enchanter"))BrainJarRenderChecks.verify(batches,c.name);
                check(INVISIBLE.contains(c.name)||!batches.isEmpty(),"Geometry exists for "+c.name);
                for(var batch:batches){
                    check(batch.vertices().size()%4==0,"Complete quads for "+c.name);
                    for(var v:batch.vertices()){
                        check(Float.isFinite(v.x()+v.y()+v.z()+v.u()+v.v()+v.nx()+v.ny()+v.nz()),"Finite geometry/UV for "+c.name);
                        check(Math.abs(v.x())<16&&Math.abs(v.y())<16&&Math.abs(v.z())<16,"Block-local bounds for "+c.name);
                        count++;
                    }
                }
                special++;vertices+=count;
            }
            records.add(Map.of("case",c.name,"vertices",count,"render_shape",state.getRenderShape().toString()));
        }
        for(int i=0;i<2;i++){
            BlockPos lower=new BlockPos(52+3*i,120,60);
            BlockPos upper=lower.above();
            check(mc.level.getBlockEntity(lower) instanceof MachineBlockEntity&&mc.level.getBlockEntity(upper) instanceof MachineBlockEntity,"Both stacked tank types reached client");
            var bottomBatches=LegacyVisuals.render(mc.level.getBlockEntity(lower),.5f,0xf000f0);
            var topBatches=LegacyVisuals.render(mc.level.getBlockEntity(upper),.5f,0xf000f0);
            check(!horizontalFaceAt(bottomBatches,1,1)&&!horizontalFaceAt(topBatches,0,-1),"Stacked tanks have no overlapping cap faces");
            check(horizontalFaceAt(topBatches,1,1),"Upper tank keeps its exposed top face");
        }
        var far=new BlockPos(29_999_980,120,29_999_980);var draw=new LegacyDraw(far,0xf000f0);draw.begin(7);
        for(double[] p:new double[][]{{.125,.125},{.25,.125},{.25,.25},{.125,.25}})draw.vertex(far.getX()+p[0],120,far.getZ()+p[1],0,0);
        draw.end();var points=draw.finish().getFirst().vertices();
        check(points.getFirst().x()==.125f&&points.get(1).x()==.25f,"Sub-block geometry survives at the world border");
        try{java.nio.file.Files.writeString(mc.gameDirectory.toPath().resolve("render-parity.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(records));}
        catch(java.io.IOException e){throw new IllegalStateException(e);}
        Thaumcraft.LOG.info("THAUMCRAFT_RENDER_PARITY_PASS: {} block/state cases, {} custom renderers, {} finite vertices; world-border precision",cases.size(),special,vertices);
    }
    static void verifyVoidSubmissions(List<LegacyDraw.Batch> batches){
        var collector=new net.minecraft.client.renderer.SubmitNodeStorage();
        LegacyDraw.submit(batches,new com.mojang.blaze3d.vertex.PoseStack(),collector);
        check(collector.getSubmitsPerOrder().size()==batches.size(),"Void layers retain draw order instead of regrouping by texture");
    }
    private static boolean horizontalFaceAt(List<LegacyDraw.Batch> batches,float height,float normal){
        for(var batch:batches){var vertices=batch.vertices();for(int i=0;i+3<vertices.size();i+=4){
            boolean match=true;for(int j=0;j<4;j++){var v=vertices.get(i+j);if(Math.abs(v.y()-height)>.0001f||Math.abs(v.ny()-normal)>.0001f){match=false;break;}}
            if(match)return true;
        }}
        return false;
    }
}
