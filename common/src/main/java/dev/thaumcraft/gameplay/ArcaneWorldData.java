package dev.thaumcraft.gameplay;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.thaumcraft.Thaumcraft;
import dev.thaumcraft.PortConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.*;

/** Dimension-local aura and overworld-owned player research. No process-global world state. */
public final class ArcaneWorldData extends SavedData {
    public record Aura(float vis,float taint,float base,int goodVibes,int badVibes,int boost) {
        public static final Codec<Aura> CODEC=RecordCodecBuilder.create(i->i.group(
                Codec.FLOAT.fieldOf("vis").forGetter(Aura::vis),Codec.FLOAT.fieldOf("taint").forGetter(Aura::taint),
                Codec.FLOAT.fieldOf("base").forGetter(Aura::base),Codec.INT.optionalFieldOf("good_vibes",0).forGetter(Aura::goodVibes),
                Codec.INT.optionalFieldOf("bad_vibes",0).forGetter(Aura::badVibes),Codec.INT.optionalFieldOf("boost",0).forGetter(Aura::boost)).apply(i,Aura::new));
        public Aura(float vis,float taint,float base){this(vis,taint,base,0,0,0);}
        public Aura(float vis,float taint,float base,int good,int bad){this(vis,taint,base,good,bad,0);}
        public Aura {boost=Math.clamp(boost,0,100);goodVibes=Math.clamp(goodVibes,0,100);badVibes=Math.clamp(badVibes,0,100);}
        public Aura change(float pure,float corruption) {
            return new Aura(Math.clamp(vis+pure,0,PortConfig.auraMax),Math.clamp(taint+corruption,0,PortConfig.auraMax),base,goodVibes,badVibes,boost);
        }
        public Aura vibes(int good,int bad){return new Aura(vis,taint,base,Math.clamp((long)goodVibes+good,0,100),Math.clamp((long)badVibes+bad,0,100),boost);}
    }
    private static final Codec<net.minecraft.resources.Identifier> RESEARCH_ID_CODEC=Codec.either(net.minecraft.resources.Identifier.CODEC,Codec.INT)
            .xmap(value->value.map(id->id,GameData::legacyResearchId),com.mojang.datafixers.util.Either::left);
    public static final Codec<ArcaneWorldData> CODEC=RecordCodecBuilder.create(i->i.group(
            Codec.unboundedMap(Codec.STRING,Aura.CODEC).optionalFieldOf("aura",Map.of()).forGetter(ArcaneWorldData::savedAura),
            Codec.unboundedMap(Codec.STRING,RESEARCH_ID_CODEC.listOf()).optionalFieldOf("research",Map.of()).forGetter(d->d.research),
            Codec.STRING.listOf().optionalFieldOf("known_seals",List.of()).forGetter(d->List.copyOf(d.knownSeals))
    ).apply(i,ArcaneWorldData::new));
    public static final SavedDataType<ArcaneWorldData> TYPE=new SavedDataType<>(Thaumcraft.id("arcane_world"),ArcaneWorldData::new,CODEC,null);
    /** Cells by packed chunk position; saved under the decimal string of that key. */
    private final Map<Long,Aura> aura;
    private final it.unimi.dsi.fastutil.longs.LongArrayList auraOrder;
    private int auraCursor;
    private final Map<String,List<net.minecraft.resources.Identifier>> research;
    private java.lang.ref.WeakReference<net.minecraft.server.MinecraftServer> researchServer=new java.lang.ref.WeakReference<>(null);
    private final Set<String> knownSeals;
    private static final Map<ServerLevel,ArcaneWorldData> LEVELS=new WeakHashMap<>();

    public ArcaneWorldData() {this(Map.of(),Map.of(),List.of());}
    private ArcaneWorldData(Map<String,Aura> aura,Map<String,List<net.minecraft.resources.Identifier>> research,List<String> seals) {
        this.knownSeals=new HashSet<>(seals);
        this.aura=new java.util.concurrent.ConcurrentHashMap<>(aura.size());
        this.auraOrder=new it.unimi.dsi.fastutil.longs.LongArrayList(aura.size());
        aura.forEach((key,cell)->{
            long chunk;
            try{chunk=Long.parseLong(key);}catch(NumberFormatException invalid){Thaumcraft.LOG.warn("Ignoring aura cell with invalid chunk key {}",key);return;}
            if(this.aura.put(chunk,cell)==null)auraOrder.add(chunk);
        });
        this.research=new HashMap<>();
        research.forEach((key,values)->this.research.put(key,new ArrayList<>(new java.util.LinkedHashSet<>(values))));
        // Persist imported numeric knowledge using the current ID codec on the next save.
        if(!research.isEmpty())setDirty();
    }
    public static synchronized ArcaneWorldData get(ServerLevel level) {
        var data=LEVELS.get(level);
        if(data==null){
            if(!level.getServer().isSameThread())throw new IllegalStateException("Aura data was not initialized before generation");
            data=level.getDataStorage().computeIfAbsent(TYPE);data.researchServer=new java.lang.ref.WeakReference<>(level.getServer());LEVELS.put(level,data);
        }
        return data;
    }
    /** Immutable cells can be sampled by generation workers without creating or changing them. */
    public Aura knownAura(BlockPos pos){return aura.get(ChunkPos.pack(pos));}
    private Map<String,Aura> savedAura(){
        var saved=new HashMap<String,Aura>();
        aura.forEach((chunk,cell)->saved.put(Long.toString(chunk),cell));
        return saved;
    }
    public static ArcaneWorldData researchData(ServerLevel level) {return get(level.getServer().overworld());}

    public Aura aura(ServerLevel level,BlockPos pos) {
        long key=ChunkPos.pack(pos);
        Aura cell=aura.get(key);
        if(cell==null) {
            var biome=dev.thaumcraft.world.ArcaneFeatures.surfaceBiome(level,new BlockPos((pos.getX()>>4)*16,0,(pos.getZ()>>4)*16));
            cell=initialAura(level.getSeed(),pos.getX()>>4,pos.getZ()>>4,biome,level.dimension()==net.minecraft.world.level.Level.NETHER);
            aura.put(key,cell);auraOrder.add(key);setDirty();
        }
        return cell;
    }
    /** Deterministic biome strengths can also be read safely by parallel world generation. */
    public static Aura initialAura(long worldSeed,int chunkX,int chunkZ,net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> biome,boolean nether){
        long seed=worldSeed^(long)chunkX*341873128712L^(long)chunkZ*132897987541L;
            var random=new Random(seed);
            int max=PortConfig.auraMax,lower=max/5,upper=max/3;
            boolean extraTaint=nether||biome.is(net.minecraft.world.level.biome.Biomes.SWAMP)||biome.is(net.minecraft.world.level.biome.Biomes.MANGROVE_SWAMP);
            if(nether||biome.is(net.minecraft.world.level.biome.Biomes.DESERT)){lower=max/20;upper=max/8;}
            else if(biome.is(dev.thaumcraft.content.ModTags.EXTREME_AURA)){lower=max/2;upper=(int)(max*.7f);}
            else if(biome.is(dev.thaumcraft.content.ModTags.HIGH_AURA)){lower=max/3;upper=(int)(max*.6f);}
            else if(extraTaint){lower=max/3;upper=max/2;}
            float strength=lower+random.nextInt(upper-lower);
            float taint=(int)(strength/3);
            if(PortConfig.taintSpawn>0&&random.nextInt(PortConfig.taintSpawn==2?300:2200)==0) {strength=(lower+random.nextInt(upper-lower))/2;taint=(int)(max*(PortConfig.taintSpawn==2?.8f:.5f))+random.nextInt((int)(max*.2f));}
            if(extraTaint)taint=(int)(taint*1.5f);
        return new Aura(strength,taint,strength);
    }
    /** Called on the server thread after a feature computes its immutable initial cell. */
    public void seedAura(BlockPos pos,Aura cell){
        long key=ChunkPos.pack(pos);
        if(aura.putIfAbsent(key,cell)==null){auraOrder.add(key);setDirty();}
    }
    /** Initial patches only affect aura cells already known, as in the original generator. */
    public void raiseExistingTaint(BlockPos pos,float threshold,float value){
        long key=ChunkPos.pack(pos);Aura cell=aura.get(key);
        if(cell!=null&&cell.taint()<threshold){
            aura.put(key,new Aura(cell.vis(),(int)value,cell.base(),cell.goodVibes(),cell.badVibes(),cell.boost()));setDirty();
        }
    }
    public void changeAura(ServerLevel level,BlockPos pos,float vis,float taint) {
        Aura current=aura(level,pos);
        aura.put(ChunkPos.pack(pos),current.change(vis,taint));setDirty();
    }
    public float drainAura(ServerLevel level,BlockPos pos,float amount,boolean tainted) {
        if(!Float.isFinite(amount)||amount<=0)return 0;
        Aura cell=aura(level,pos);
        float drained=Math.min(amount,tainted?cell.taint():cell.vis());
        if(drained>0)changeAura(level,pos,tainted?0:-drained,tainted?-drained:0);
        return drained;
    }
    public int changeBoost(ServerLevel level,BlockPos pos,int amount){
        Aura cell=aura(level,pos);int next=Math.clamp(cell.boost()+amount,0,100);
        if(next!=cell.boost()){aura.put(ChunkPos.pack(pos),new Aura(cell.vis(),cell.taint(),cell.base(),cell.goodVibes(),cell.badVibes(),next));setDirty();}
        return next-cell.boost();
    }
    public void addVibes(ServerLevel level,BlockPos pos,int good,int bad) {
        if(good==0&&bad==0)return;
        Aura current=aura(level,pos);
        aura.put(ChunkPos.pack(pos),current.vibes(good,bad));setDirty();
    }
    public int drainVibes(ServerLevel level,BlockPos pos,int amount,boolean tainted) {
        if(amount<=0)return 0;
        Aura cell=aura(level,pos);int drained=Math.min(amount,tainted?cell.badVibes():cell.goodVibes());
        if(drained>0)addVibes(level,pos,tainted?0:-drained,tainted?-drained:0);
        return drained;
    }
    /** Amortized original aura update; saved cells can evolve without loading their terrain. */
    public void tickAura(ServerLevel level) {
        if(auraOrder.isEmpty())return;
        int count=Math.max(1,auraOrder.size()/200),max=PortConfig.auraMax,margin=(int)(max/7.5f);var random=level.getRandom();
        for(int i=0;i<count;i++) {
            if(auraCursor>=auraOrder.size())auraCursor=0;
            long key=auraOrder.getLong(auraCursor++);Aura before=aura.get(key);
            float vis=before.vis(),taint=before.taint();int good=before.goodVibes(),bad=before.badVibes(),boost=before.boost();
            if(bad>50&&good==0){vis--;bad-=5;}
            if(good>0&&bad>0){good--;bad--;}
            if(random.nextInt(100)<good&&good>0){vis++;good-=Math.max(1,(int)(vis/(max/10)));}
            if(random.nextInt(100)<bad&&bad>0){taint++;bad-=Math.max(1,(int)(taint/(max/10)));}
            ChunkPos pos=ChunkPos.unpack(key);
            for(var direction:net.minecraft.core.Direction.Plane.HORIZONTAL) {
                long neighborKey=ChunkPos.pack(pos.x()+direction.getStepX(),pos.z()+direction.getStepZ());
                Aura neighbor=aura.get(neighborKey);if(neighbor==null)continue;
                float pure=neighbor.vis()>vis+margin&&vis<max?Math.min(neighbor.vis(),Math.max(1,(int)((neighbor.vis()-vis)/max*10))):0;
                float corruption=neighbor.taint()>taint+margin*.75f&&taint<max?Math.min(neighbor.taint(),Math.max(1,(int)((neighbor.taint()-taint)/max*10))):0;
                pure=Math.min(pure,max-vis);corruption=Math.min(corruption,max-taint);
                int boostTransfer=neighbor.boost()>boost&&neighbor.boost()>50&&boost<100?1:0;
                if(pure>0||corruption>0||boostTransfer>0){
                    vis+=pure;taint+=corruption;boost+=boostTransfer;
                    aura.put(neighborKey,new Aura(neighbor.vis()-pure,neighbor.taint()-corruption,neighbor.base(),neighbor.goodVibes(),neighbor.badVibes(),neighbor.boost()-boostTransfer));setDirty();
                }
            }
            Aura after=new Aura(Math.clamp(vis,0,PortConfig.auraMax),Math.clamp(taint,0,PortConfig.auraMax),before.base(),good,bad,boost);
            if(!after.equals(before)){aura.put(key,after);setDirty();}
            if(dev.thaumcraft.PortConfig.taintSpread&&after.taint()>max/2){
                BlockPos column=new BlockPos(pos.x()*16+random.nextInt(16),level.getSeaLevel(),pos.z()*16+random.nextInt(16));
                if(level.hasChunkAt(column)){
                    int top=level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,column.getX(),column.getZ());
                    BlockPos target=new BlockPos(column.getX(),level.getMinY()+random.nextInt(Math.max(1,top-level.getMinY())),column.getZ());
                    dev.thaumcraft.content.TaintBlock.increase(level,target,after.taint(),random);
                }
            }
        }
    }
    public boolean knows(UUID player,int project) {
        return project<0 || knows(player,GameData.researchId(project));
    }
    private String knowledgeKey(UUID player){
        var server=researchServer.get();
        var group=server==null?null:dev.thaumcraft.api.ThaumcraftKnowledge.group(server,player);
        return group==null?player.toString():"group:"+group;
    }
    public boolean sameKnowledgeOwner(UUID first,UUID second){return knowledgeKey(first).equals(knowledgeKey(second));}
    public boolean knows(UUID player,net.minecraft.resources.Identifier project) {return research.getOrDefault(knowledgeKey(player),List.of()).contains(project);}
    public boolean globallyKnown(int project) {
        return project<0 || globallyKnown(GameData.researchId(project));
    }
    public boolean globallyKnown(net.minecraft.resources.Identifier project) {return research.values().stream().anyMatch(values->values.contains(project));}
    public boolean unlock(UUID player,int project) {
        return unlock(player,GameData.researchId(project));
    }
    public boolean unlock(UUID player,net.minecraft.resources.Identifier project) {
        GameData.project(project);
        var known=research.computeIfAbsent(knowledgeKey(player),ignored->new ArrayList<>());
        if(known.contains(project))return false;
        known.add(project);setDirty();return true;
    }
    public boolean revoke(UUID player,net.minecraft.resources.Identifier project) {
        var known=research.get(knowledgeKey(player));
        if(known==null||!known.remove(project))return false;
        setDirty();return true;
    }
    public List<net.minecraft.resources.Identifier> known(UUID player) {return List.copyOf(research.getOrDefault(knowledgeKey(player),List.of()));}
    public Set<String> knownSeals(){return Set.copyOf(knownSeals);}
    public boolean learnSeal(String combination){if(!knownSeals.add(combination))return false;setDirty();return true;}
    public static boolean knows(ServerPlayer player,int project) {
        return researchData(player.level()).knows(player.getUUID(),project);
    }
}
