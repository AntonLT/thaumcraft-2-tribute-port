package dev.thaumcraft.test;

import dev.thaumcraft.Thaumcraft;
import dev.thaumcraft.content.Content;
import dev.thaumcraft.world.MonolithBlockEntity;
import dev.thaumcraft.client.legacy.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;

/** Real core packets, original rune geometry gate and inserted-crystal presentation. */
final class MonolithVisualChecks {
    private static BlockPos pos;
    private static int stage,ticks,deadline;
    private static boolean finished;
    private static boolean opening;
    private static long started;
    private static java.util.function.Consumer<dev.thaumcraft.network.EquipmentEffect> receiver;
    private static final java.util.Set<String> heard=new java.util.HashSet<>();
    private static final net.minecraft.client.sounds.SoundEventListener listener=(sound,event,range)->heard.add(sound.getIdentifier().toString());
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError("Monolith client: "+message);}
    private static int runes(Minecraft mc){return LegacyVisuals.render(mc.level.getBlockEntity(pos),.5f,0xf000f0).stream().filter(batch->batch.texture().getPath().endsWith("particles.png")).mapToInt(batch->batch.vertices().size()).sum();}
    /**
     * Undoes what opening builds: the entrance room at Y6 and the 3x3 shaft up to the core. Opening refuses to build
     * over an earlier entrance, so without this the fixture passes only once per QA world. Skipping side effects keeps
     * void chests from dropping their contents.
     */
    private static void reset(net.minecraft.server.level.ServerLevel level,BlockPos core){
        var stone=Blocks.STONE.defaultBlockState();int flags=net.minecraft.world.level.block.Block.UPDATE_CLIENTS|net.minecraft.world.level.block.Block.UPDATE_SKIP_ALL_SIDEEFFECTS;
        for(int x=-7;x<=7;x+=7)for(int z=-7;z<=7;z+=7)level.getChunkAt(core.offset(x,0,z));
        for(var p:BlockPos.betweenClosed(new BlockPos(core.getX()-7,6,core.getZ()-7),new BlockPos(core.getX()+7,14,core.getZ()+7)))level.setBlock(p,stone,flags);
        for(var p:BlockPos.betweenClosed(new BlockPos(core.getX()-1,15,core.getZ()-1),new BlockPos(core.getX()+1,core.getY()-2,core.getZ()+1)))level.setBlock(p,stone,flags);
    }
    static boolean tick(){
        if(finished)return true;var mc=Minecraft.getInstance();var server=mc.getSingleplayerServer();ticks++;
        // tick() runs once per frame. After a full run the player starts near spawn, so the distant chunk can take a while.
        if(started==0)started=System.nanoTime();
        if(System.nanoTime()-started>60_000_000_000L){
            var sp=server.getPlayerList().getPlayers().getFirst();
            check(false,"fixture timeout at stage "+stage+"; client player "+mc.player.blockPosition()+" in "+mc.level.dimension().identifier()+" riding="+mc.player.isPassenger()+" chunk="+mc.level.hasChunkAt(pos)+" clientCore="+mc.level.getBlockState(pos)+"; server player "+sp.blockPosition()+" in "+sp.level().dimension().identifier()+" serverCore="+server.overworld().getBlockState(pos));
        }
        if(stage==0){
            mc.getSoundManager().addListener(listener);
            receiver=dev.thaumcraft.network.EquipmentEffect.receiver;
            dev.thaumcraft.network.EquipmentEffect.receiver=event->{
                if(event.kind()!=dev.thaumcraft.network.EquipmentEffect.MONOLITH){receiver.accept(event);return;}
                try{
                    var field=mc.particleEngine.getClass().getDeclaredField("particlesToAdd");field.setAccessible(true);
                    var pending=(java.util.Queue<?>)field.get(mc.particleEngine);int before=pending.size();
                    receiver.accept(event);
                    check(pending.size()==before+50,"opening packet emits exactly 50 original wisps");opening=true;
                }catch(ReflectiveOperationException e){throw new AssertionError(e);}
            };
            pos=new BlockPos(6000,100,6000);server.execute(()->{
                var level=server.overworld();
                level.getChunkAt(pos);reset(level,pos);
                for(var p:BlockPos.betweenClosed(pos.offset(-2,-1,-2),pos.offset(2,3,2)))level.setBlockAndUpdate(p,p.getY()<pos.getY()?Blocks.SMOOTH_STONE.defaultBlockState():Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(pos,Content.block("eldritch_core").defaultBlockState());
                for(var side:MonolithBlockEntity.SIDES)level.setBlockAndUpdate(pos.relative(side),Content.block("eldritch_receptacle").defaultBlockState());
                ((MonolithBlockEntity)level.getBlockEntity(pos)).initialize(net.minecraft.util.RandomSource.create(216));
                for(var player:server.getPlayerList().getPlayers()){
                    player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);player.closeContainer();player.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);
                    player.teleportTo(level,pos.getX()+.5,pos.getY()+4,pos.getZ()+5,java.util.Set.of(),180,40,true);
                }
            });stage=1;deadline=ticks+30;return false;
        }
        if(ticks<deadline)return false;
        if(stage==4){
            check(opening,"successful opening reaches the client");
            check(heard.contains("thaumcraft2tp:place")&&heard.contains("thaumcraft2tp:rumble"),"placement and opening sounds reach the sound manager: "+heard);
            check(mc.level.getBlockState(pos).isAir(),"opened core becomes an empty shaft");
            mc.getSoundManager().removeListener(listener);dev.thaumcraft.network.EquipmentEffect.receiver=receiver;
            var core=pos;server.execute(()->reset(server.overworld(),core));
            Thaumcraft.LOG.info("THAUMCRAFT_MONOLITH_VISUAL_PASS goggles_gate core_packets inserted_crystal opening_wisps sounds");finished=true;return true;
        }
        if(stage==1){
            if(!(mc.level.getBlockEntity(pos) instanceof MonolithBlockEntity))return false;
            check(mc.level.getBlockEntity(pos) instanceof MonolithBlockEntity,"dedicated core reaches client");check(runes(mc)==0,"rune geometry hidden without goggles");
            Screenshot.grab(mc.gameDirectory,"thaumcraft-monolith-no-goggles.png",mc.getMainRenderTarget(),1,r->{});
            server.execute(()->{for(var player:server.getPlayerList().getPlayers())player.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Content.item("goggles_of_revealing")));});stage=2;deadline=ticks+20;return false;
        }
        if(stage==2){
            check(mc.player.getItemBySlot(EquipmentSlot.HEAD).is(Content.item("goggles_of_revealing")),"worn goggles synchronize");check(runes(mc)>=16,"four original clues render with goggles");
            Screenshot.grab(mc.gameDirectory,"thaumcraft-monolith-goggles.png",mc.getMainRenderTarget(),1,r->{});
            server.execute(()->{
                var level=server.overworld();var core=(MonolithBlockEntity)level.getBlockEntity(pos);int rune=core.target(0);
                var crystal=new ItemStack(Content.item(Content.ENTRIES.stream().filter(e->e.source_class().equals("ItemCrystals")&&e.meta()==rune).findFirst().orElseThrow().id()));
                var player=server.getPlayerList().getPlayers().getFirst();var receptacle=pos.east();
                level.getBlockState(receptacle).useItemOn(crystal,level,player,net.minecraft.world.InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(receptacle),Direction.UP,receptacle,false));
            });stage=3;deadline=ticks+20;return false;
        }
        var core=(MonolithBlockEntity)mc.level.getBlockEntity(pos);check(core.inserted(0)==core.target(0),"inserted crystal arrives through core update packet");
        var world=new LegacyCompat.World(mc.level,0);var tile=new LegacyCompat.TileEntity[1];LegacyCompat.capture(world,pos.east(),0xf000f0,mc.level.getGameTime(),()->tile[0]=LegacyVisuals.snapshot(world,pos.east()));check(tile[0].metadata==3&&tile[0].placed==core.target(0),"receptacle renderer receives actual crystal");
        check(!LegacyVisuals.render(mc.level.getBlockEntity(pos.east()),.5f,0xf000f0).isEmpty(),"inserted receptacle emits original geometry");
        Screenshot.grab(mc.gameDirectory,"thaumcraft-monolith-inserted.png",mc.getMainRenderTarget(),1,r->{});
        server.execute(()->{
            var level=server.overworld();var own=(MonolithBlockEntity)level.getBlockEntity(pos);var player=server.getPlayerList().getPlayers().getFirst();
            for(int i=1;i<4;i++){
                int rune=own.target(i);var crystal=new ItemStack(Content.item(Content.ENTRIES.stream().filter(e->e.source_class().equals("ItemCrystals")&&e.meta()==rune).findFirst().orElseThrow().id()));
                own.insert(pos.relative(MonolithBlockEntity.SIDES[i]),crystal,player);
            }
        });stage=4;deadline=ticks+30;return false;
    }
}
