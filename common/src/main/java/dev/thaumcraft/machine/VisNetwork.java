package dev.thaumcraft.machine;

import dev.thaumcraft.api.VisContainer;
import dev.thaumcraft.gameplay.ArcaneWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/** Local, buffered vis flow. Suction falls by one per conduit, as in TileConduit. */
public final class VisNetwork {
    private static final Set<String> CONDUITS=Set.of("vis_conduit","vis_filter","vis_valve","advanced_vis_valve","vis_purifier");
    private static final Set<String> SOURCES=Set.of("crucible","crucible_of_eyes","thaumium_crucible","crucible_of_souls","vis_condenser","vis_storage_tank","thaumium_reinforced_tank","vis_pump");
    private static final Direction[] FLOW_ORDER={Direction.UP,Direction.DOWN,Direction.SOUTH,Direction.NORTH,Direction.EAST,Direction.WEST};
    private static final Direction[] USER_ORDER={Direction.UP,Direction.DOWN,Direction.EAST,Direction.WEST,Direction.SOUTH,Direction.NORTH};
    private static final int MAX_ADDON_SUCTION=1000;
    private static final Map<BlockEntityType<?>,Function<BlockEntity,VisContainer>> ADAPTERS=new ConcurrentHashMap<>();
    private VisNetwork() {}

    @SuppressWarnings("unchecked")
    public static <T extends BlockEntity> void register(BlockEntityType<T> type,Function<? super T,? extends VisContainer> adapter) {
        java.util.Objects.requireNonNull(adapter);
        if(ADAPTERS.putIfAbsent(java.util.Objects.requireNonNull(type),entity->adapter.apply((T)entity))!=null)throw new IllegalArgumentException("Vis container adapter already registered for "+type);
    }
    /** An addon container, never a Thaumcraft machine. */
    public static VisContainer addon(BlockEntity entity) {
        if(entity==null||entity instanceof MachineBlockEntity)return null;
        if(entity instanceof VisContainer container)return container;
        var adapter=ADAPTERS.get(entity.getType());return adapter==null?null:adapter.apply(entity);
    }
    public static VisContainer addon(BlockGetter level,BlockPos pos) {return addon(level.getBlockEntity(pos));}
    public static VisContainer container(ServerLevel level,BlockPos pos) {
        var entity=level.getBlockEntity(pos);
        return entity instanceof MachineBlockEntity machine?new MachineContainer(machine):addon(entity);
    }
    private static boolean buffer(MachineBlockEntity machine) {return CONDUITS.contains(machine.machineId())||SOURCES.contains(machine.machineId());}
    /** Thaumcraft machines seen through the addon API. Only enabled vis buffers exchange vis. */
    private record MachineContainer(MachineBlockEntity machine) implements VisContainer {
        @Override public boolean connects(Direction side) {return MachineConnections.accepts(machine.getLevel(),machine.getBlockPos(),machine.getBlockState(),side);}
        @Override public float vis(boolean tainted) {return tainted?machine.taintedVis():machine.pureVis();}
        @Override public float capacity() {return machine.capacity();}
        @Override public int suction(boolean tainted) {return tainted?machine.taintSuction():machine.visSuction();}
        @Override public float extract(float amount,boolean tainted) {return buffer(machine)&&machine.enabled()?machine.extractVis(amount,tainted):0;}
        @Override public float insert(float amount,boolean tainted) {return buffer(machine)&&machine.enabled()?machine.insertVis(amount,tainted):0;}
    }
    /** A connected neighbor: a Thaumcraft machine, or an addon container whose results are sanitized. */
    private record Node(MachineBlockEntity machine,VisContainer addon) {
        boolean source() {return machine==null||buffer(machine);}
        boolean tank() {return machine!=null&&isTank(machine.machineId());}
        float vis(boolean tainted) {
            if(machine!=null)return tainted?machine.taintedVis():machine.pureVis();
            float value=addon.vis(tainted);return Float.isFinite(value)&&value>0?value:0;
        }
        float total() {return vis(false)+vis(true);}
        int suction(BlockPos observer,boolean tainted) {return machine!=null?suctionFrom(machine,observer,tainted):Math.clamp(addon.suction(tainted),0,MAX_ADDON_SUCTION);}
        float extract(float amount,boolean tainted) {
            if(machine!=null)return machine.extractVis(amount,tainted);
            if(!(amount>0)||!Float.isFinite(amount))return 0;
            float extracted=addon.extract(amount,tainted);return Float.isFinite(extracted)?Math.clamp(extracted,0,amount):0;
        }
        float insert(float amount,boolean tainted) {
            if(machine!=null)return machine.insertVis(amount,tainted);
            if(!(amount>0)||!Float.isFinite(amount))return 0;
            float accepted=addon.insert(amount,tainted);return Float.isFinite(accepted)?Math.clamp(accepted,0,amount):0;
        }
    }

    public static boolean isTank(String id) {return id.equals("vis_storage_tank")||id.equals("thaumium_reinforced_tank");}
    private static Node neighbor(ServerLevel level,MachineBlockEntity target,Direction side) {return neighbor(level,target.getBlockPos(),target.getBlockState(),side);}
    private static Node neighbor(ServerLevel level,BlockPos pos,BlockState state,Direction side) {
        BlockPos next=pos.relative(side);
        if(!level.hasChunkAt(next)||!MachineConnections.connected(level,pos,state,side))return null;
        var entity=level.getBlockEntity(next);
        if(entity instanceof MachineBlockEntity machine)return new Node(machine,null);
        var addon=addon(entity);return addon==null?null:new Node(null,addon);
    }
    /** Suction of a pump is present only at its intake face. */
    public static int suctionFrom(MachineBlockEntity machine,BlockPos observer,boolean tainted) {
        if(!machine.enabled())return 0;
        if(machine.machineId().equals("vis_pump")&&!observer.equals(machine.getBlockPos().relative(machine.getBlockState().getValue(MachineBlock.FACING))))return 0;
        return tainted?machine.taintSuction():machine.visSuction();
    }
    public static int bellows(ServerLevel level,MachineBlockEntity target) {
        int count=0;
        boolean hotTarget=target.machineId().contains("crucible")||target.machineId().equals("arcane_furnace");
        for(Direction direction:Direction.Plane.HORIZONTAL) {
            BlockPos next=target.getBlockPos().relative(direction);
            if(!level.hasChunkAt(next))continue;
            if(level.getBlockEntity(next) instanceof MachineBlockEntity b&&b.machineId().equals("arcane_bellows")&&b.enabled()
                    &&b.getBlockState().getValue(MachineBlock.FACING)==direction.getOpposite()
                    &&(hotTarget||!level.hasNeighborSignal(next)))count++;
        }
        return count;
    }
    public static void tick(ServerLevel level,MachineBlockEntity machine) {
        String id=machine.machineId();
        if(!machine.enabled()){machine.setSuction(0,0);return;}
        if(id.equals("vis_pump")) {
            boolean powered=level.hasNeighborSignal(machine.getBlockPos());
            int suction=powered?0:20+10*bellows(level,machine);machine.setSuction(suction,suction);
            if(!powered){Node source=neighbor(level,machine,machine.getBlockState().getValue(MachineBlock.FACING));if(source!=null)transferMixed(source,machine,1,true,true);}
            return;
        }
        if(isTank(id)) {
            if(level.getGameTime()%10==0&&machine.taintedVis()>machine.capacity()*.9f) {
                int chance=id.equals("vis_storage_tank")?999:3333;
                if(id.equals("vis_storage_tank")&&level.getRandom().nextInt(chance)==123){dev.thaumcraft.content.TaintBlock.explosion(level,machine.getBlockPos());level.removeBlock(machine.getBlockPos(),false);return;}
                if(level.getRandom().nextInt(chance/8)==42)dev.thaumcraft.content.ModSounds.play(level,machine.getBlockPos(),"creaking",net.minecraft.sounds.SoundSource.BLOCKS,.75f,1);
            }
            int suction=10+10*bellows(level,machine);machine.setSuction(suction,suction);
            equalizeTank(level,machine);return;
        }
        if(id.equals("arcane_bellows")){int suction=level.hasNeighborSignal(machine.getBlockPos())?0:10;machine.setSuction(suction,suction);return;}
        if(!CONDUITS.contains(id)) {
            machine.setSuction(level.getGameTime()<=machine.visDemandUntil?50:0,level.getGameTime()<=machine.taintDemandUntil?50:0);return;
        }
        int pure=0,taint=0;
        // Connections cannot change while this conduit runs, so both passes share one lookup.
        Node[] nodes=new Node[FLOW_ORDER.length];
        for(int i=0;i<FLOW_ORDER.length;i++) {
            Node next=nodes[i]=neighbor(level,machine,FLOW_ORDER[i]);if(next==null)continue;
            pure=Math.max(pure,next.suction(machine.getBlockPos(),false)-1);
            taint=Math.max(taint,next.suction(machine.getBlockPos(),true)-1);
        }
        if(id.equals("vis_filter"))taint=Math.max(15,taint);
        if(id.equals("vis_purifier"))taint=Math.max(5,taint);
        if(id.equals("advanced_vis_valve")) {
            if(machine.channel()==0){pure=0;taint=0;}
            if(machine.channel()==1)taint=0;
            if(machine.channel()==2)pure=0;
        }
        machine.setSuction(pure,taint);
        for(Node source:nodes) {
            if(source==null)continue;
            transferMixed(source,machine,Math.min(4,source.total()/4),pure>source.suction(machine.getBlockPos(),false),taint>source.suction(machine.getBlockPos(),true));
        }
        if(id.equals("vis_purifier")&&machine.taintedVis()>.01f)machine.extractVis(.01f,true);
        if(id.equals("vis_filter")) {
            int stack=0;
            while(stack<level.getMaxY()-machine.getBlockPos().getY()-1) {
                BlockPos next=machine.getBlockPos().above(stack+1);
                if(!level.hasChunkAt(next)||!(level.getBlockEntity(next) instanceof MachineBlockEntity b)||!b.machineId().equals(id))break;
                stack++;
            }
            if(machine.filterStore<40+stack*4&&machine.taintedVis()>=.025f){
                machine.extractVis(.025f,true);machine.filterStore++;
                if(machine.filterStore%16==0)machine.emitFilterWisp(stack);
            }
            if(machine.filterStore>=40+stack*4){machine.filterStore=0;ArcaneWorldData.get(level).changeAura(level,machine.getBlockPos(),0,1);machine.setChanged();}
        }
    }
    /** Equal shares first, then use the other component to make up a shortage. */
    public static float[] mixedAmounts(float pure,float tainted,float amount) {
        if(!Float.isFinite(amount)||amount<.001f)return new float[]{0,0};
        float p=Math.min(pure,amount/2),t=Math.min(tainted,amount/2);
        if(p<amount/2&&t==amount/2)t=Math.min(amount-p,tainted);
        else if(t<amount/2&&p==amount/2)p=Math.min(amount-t,pure);
        return new float[]{p,t};
    }
    private static void transferMixed(Node source,MachineBlockEntity target,float amount,boolean pure,boolean tainted) {
        if((!pure&&!tainted)||!source.source())return;
        float[] portions=mixedAmounts(source.vis(false),source.vis(true),Math.min(amount,Math.max(0,target.capacity()-target.totalVis())));
        if(pure)target.insertVis(source.extract(portions[0],false),false);
        if(tainted)target.insertVis(source.extract(portions[1],true),true);
    }
    private static void equalizeTank(ServerLevel level,MachineBlockEntity bottom) {
        var tanks=new ArrayList<MachineBlockEntity>();tanks.add(bottom);
        float pure=bottom.pureVis(),tainted=bottom.taintedVis(),capacity=bottom.capacity();
        for(BlockPos pos=bottom.getBlockPos().above();pos.getY()<level.getMaxY()&&level.hasChunkAt(pos);pos=pos.above()) {
            if(!(level.getBlockEntity(pos) instanceof MachineBlockEntity next)||!isTank(next.machineId()))break;
            tanks.add(next);pure+=next.pureVis();tainted+=next.taintedVis();capacity+=next.capacity();
        }
        for(Direction side:FLOW_ORDER) {
            Node source=neighbor(level,bottom,side);if(source==null||source.tank()||!source.source())continue;
            float[] portions=mixedAmounts(source.vis(false),source.vis(true),Math.min(1,Math.max(0,capacity-pure-tainted)));
            if(bottom.visSuction()>source.suction(bottom.getBlockPos(),false))pure+=source.extract(portions[0],false);
            if(bottom.taintSuction()>source.suction(bottom.getBlockPos(),true))tainted+=source.extract(portions[1],true);
        }
        float total=pure+tainted,ratio=total>0?pure/total:0;
        if(Math.round(total)>=capacity)bottom.setSuction(0,0);
        for(MachineBlockEntity tank:tanks) {
            float volume=Math.min(total,tank.capacity());
            tank.refillVis(volume*ratio,volume*(1-ratio));total=Math.max(0,total-volume);
        }
    }
    public static void equalizeCondensers(ServerLevel level,MachineBlockEntity machine) {
        var group=new ArrayList<MachineBlockEntity>();group.add(machine);
        float pure=machine.pureVis(),taint=machine.taintedVis();
        for(Direction side:Direction.Plane.HORIZONTAL) {
            Node node=neighbor(level,machine,side);MachineBlockEntity next=node==null?null:node.machine();
            if(next!=null&&next.machineId().equals("vis_condenser")){group.add(next);pure+=next.pureVis();taint+=next.taintedVis();}
        }
        if(group.size()==1)return;
        pure/=group.size();taint/=group.size();
        for(MachineBlockEntity next:group)next.refillVis(pure,taint);
    }
    /** Original exact requests must be supplied by one adjacent source, atomically. */
    public static boolean takeExactPure(ServerLevel level,MachineBlockEntity target,float amount) {
        if(!Float.isFinite(amount)||amount<0||!target.enabled())return false;
        target.visDemandUntil=level.getGameTime();target.setSuction(50,target.taintSuction());
        for(Direction side:USER_ORDER) {
            Node source=neighbor(level,target,side);
            if(source==null||!source.source()||source.vis(false)<amount)continue;
            float taken=source.extract(amount,false);
            if(taken>=amount-.0001f)return true;
            source.insert(taken,false); // An addon source gave less than it reported.
        }
        return false;
    }
    /** Consumers draw only from adjacent buffers; conduits must transport vis first. */
    public static float pull(ServerLevel level,MachineBlockEntity target,float requested,boolean tainted) {
        if(!Float.isFinite(requested)||requested<=0||!target.enabled())return 0;
        if(tainted)target.taintDemandUntil=level.getGameTime();else target.visDemandUntil=level.getGameTime();
        target.setSuction(tainted?target.visSuction():50,tainted?50:target.taintSuction());
        float remaining=Math.min(requested,Math.max(0,target.capacity()-target.totalVis())),transferred=0;
        for(Direction side:USER_ORDER) {
            Node source=neighbor(level,target,side);
            if(source==null||!source.source())continue;
            float amount=Math.min(remaining,source.vis(tainted));
            if(amount<.001f)continue;
            float accepted=target.insertVis(source.extract(amount,tainted),tainted);transferred+=accepted;remaining-=accepted;
            if(remaining<=0)break;
        }
        return transferred;
    }
    /** Addon consumers draw like Thaumcraft consumers but store the result themselves. */
    public static float pullInto(ServerLevel level,BlockPos pos,float requested,boolean tainted) {
        if(!Float.isFinite(requested)||requested<=0||!level.hasChunkAt(pos))return 0;
        BlockState state=level.getBlockState(pos);float remaining=requested,transferred=0;
        for(Direction side:USER_ORDER) {
            Node source=neighbor(level,pos,state,side);
            if(source==null||!source.source())continue;
            float amount=Math.min(remaining,source.vis(tainted));
            if(amount<.001f)continue;
            float taken=source.extract(amount,tainted);transferred+=taken;remaining-=taken;
            if(remaining<=0)break;
        }
        return transferred;
    }
}
