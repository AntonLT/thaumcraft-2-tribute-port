package dev.thaumcraft.client.legacy;

import dev.thaumcraft.content.Content;
import dev.thaumcraft.content.CrystalBlock;
import dev.thaumcraft.machine.MachineBlock;
import dev.thaumcraft.machine.MachineBlockEntity;
import dev.thaumcraft.world.EldritchBlock;
import dev.thaumcraft.world.TaintPodBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dev.thaumcraft.client.legacy.LegacyCompat.*;

/** Maps synchronized modern visual state to the original renderer's input fields. */
public final class LegacyVisuals {
    private static final ThreadLocal<Integer> PASS=ThreadLocal.withInitial(()->0);
    private static final Map<String,TileEntitySpecialRenderer> RENDERERS=new HashMap<>();
    private static final Map<BlockPos,Long> EFFECT_TICKS=new java.util.LinkedHashMap<>();
    private static final Map<BlockPos,Integer> CONDENSER_WISPS=new HashMap<>();
    private static final Map<BlockPos,Integer> CRUCIBLE_WISPS=new HashMap<>();
    private static final Map<BlockPos,Integer> FILTER_WISPS=new HashMap<>();
    private static net.minecraft.world.level.Level effectLevel;
    static {
        RENDERERS.put("quaesitum",new TileResearcherRenderer());
        RENDERERS.put("vis_condenser",new TileCondenserRenderer());
        RENDERERS.put("thaumic_duplicator",new TileDuplicatorRenderer());
        RENDERERS.put("thaumic_restorer",new TileRepairerRenderer());
        RENDERERS.put("thaumic_generator",new TileGeneratorRenderer());
        RENDERERS.put("thaumic_crystalizer",new TileCrystalizerRenderer());
        RENDERERS.put("thaumic_infuser",new TileInfuserRenderer());RENDERERS.put("dark_infuser",new TileInfuserRenderer());
        RENDERERS.put("thaumic_enchanter",new TileEnchanterRenderer());RENDERERS.put("occultic_enchanter",new TileEnchanterAdvancedRenderer());
        RENDERERS.put("brain_in_a_jar",new TileBrainRenderer());RENDERERS.put("arcane_bellows",new TileBellowsRenderer());
        RENDERERS.put("vis_pump",new TileConduitPumpRenderer());RENDERERS.put("arcane_bore",new TileBoreRenderer());
        RENDERERS.put("arcane_seal",new TileSealRenderer());RENDERERS.put("void_interface",new TileVoidInterfaceRenderer());
        RENDERERS.put("void_chest",new TileVoidCubeRenderer());RENDERERS.put("eldritch_core",new TileVoidCubeRenderer());
        RENDERERS.put("eldritch_receptacle",new TileVoidCubeRenderer());
        RENDERERS.put("eldritch_lock",new TileVoidCubeRenderer());RENDERERS.put("eldritch_monolith",new TileVoidCubeRenderer());
        RENDERERS.put("temporary_space",new TileVoidHoleRenderer());RENDERERS.put("taint_spore_pod",new TileTaintSeedRenderer());
        for(String id:List.of("vis_ore","vaporous_vis_ore","aqueous_vis_ore","earthen_vis_ore","fiery_vis_ore","tainted_vis_ore"))RENDERERS.put(id,new TileCrystalOreRenderer());
    }
    private LegacyVisuals() {}
    public static int renderPass(){return PASS.get();}
    public static Block blockFor(String id){
        var entry=Content.DEFINITIONS.get(id);Block block=entry==null?new Block():switch(entry.legacy()){
            case "mod_ThaumCraft.blockAppWood" -> new BlockApparatusWood();case "mod_ThaumCraft.blockAppMetal" -> new BlockApparatusMetal();
            case "mod_ThaumCraft.blockAppStone" -> new BlockApparatusStone();case "mod_ThaumCraft.blockAppFragile" -> new BlockApparatusFragile();default -> new Block();};
        block.metadata=entry==null?0:entry.meta();if(id.startsWith("eldritch_"))block.blockID=mod_ThaumCraft.blockHidden.blockID;return block;
    }
    public static TileEntity snapshot(World world,BlockPos pos){
        BlockState state=world.level.getBlockState(pos);String id=BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        TileEntity tile=switch(id){
            case "vis_conduit" -> new TileConduit();case "vis_valve" -> new TileConduitValve();case "advanced_vis_valve" -> new TileConduitValveAdvanced();
            case "vis_storage_tank","thaumium_reinforced_tank" -> new TileConduitTank();case "vis_pump" -> new TileConduitPump();case "vis_filter" -> new TileFilter();case "vis_purifier" -> new TilePurifier();
            case "crucible","crucible_of_eyes","thaumium_crucible","crucible_of_souls" -> new TileCrucible();case "vis_condenser" -> new TileCondenser();
            case "thaumic_duplicator" -> new TileDuplicator();case "thaumic_restorer" -> new TileRepairer();case "thaumic_generator" -> new TileGenerator();
            case "thaumic_crystalizer" -> new TileCrystalizer();case "thaumic_infuser","dark_infuser" -> new TileInfuser();case "arcane_bellows" -> new TileBellows();
            case "thaumic_enchanter" -> new TileEnchanter();case "occultic_enchanter" -> new TileEnchanterAdvanced();case "quaesitum" -> new TileResearcher();
            case "brain_in_a_jar" -> new TileBrain();case "arcane_bore" -> new TileBore();case "arcane_seal" -> new TileSeal();case "void_interface" -> new TileVoidInterface();
            case "eldritch_core","eldritch_lock","eldritch_monolith","eldritch_receptacle","void_chest" -> new TileVoidCube();case "temporary_space" -> new TileVoidHole();
            case "taint_spore_pod" -> new TileTaintSeed();case "arcane_furnace" -> new VisTile();
            case "glowing_nitor" -> new TileEntity();
            // Addon vis containers stand in as plain connections, so conduits draw their arms to them.
            default -> state.getBlock() instanceof CrystalBlock?new TileCrystalOre():state.getBlock() instanceof MachineBlock?new TileEntity()
                    :dev.thaumcraft.machine.VisNetwork.addon(world.level,pos)!=null?new VisTile():null;
        };
        if(tile==null)return null;
        tile.worldObj=world;tile.id=id;tile.xCoord=pos.getX();tile.yCoord=pos.getY();tile.zCoord=pos.getZ();
        var entry=Content.DEFINITIONS.get(id);tile.metadata=entry==null?0:entry.meta();float time=context().time();
        if(state.getBlock() instanceof MachineBlock){
            Direction facing=state.getValue(MachineBlock.FACING);
            tile.orientation=switch(id){case "arcane_bore","vis_pump","arcane_seal"->facing.get3DDataValue();default->switch(facing){case EAST->1;case SOUTH->2;case WEST->3;default->0;};};
        }
        if(world.level.getBlockEntity(pos) instanceof MachineBlockEntity machine){
            tile.pureVis=machine.pureVis();tile.displayPure=machine.displayPureVis();tile.taintedVis=machine.taintedVis();tile.displayTaint=machine.displayTaintedVis();tile.maxVis=machine.capacity();
            tile.open=machine.enabled();tile.setting=machine.enabled()?machine.channel():0;tile.powered=machine.visualPowered();tile.worked=machine.visualWorking();tile.duration=tile.worked?1:0;tile.sucked=id.equals("thaumic_infuser")?machine.visualInfuserSucked():tile.worked?1:0;tile.isPowering=tile.worked;
            tile.currentItemCopyCost=machine.workRequired;tile.duplicatorCopyTime=machine.progress;
            if(tile instanceof TileInfuser infuser)infuser.processing=machine.visualInfuserProcessing();
            for(int i=0;i<8;i++)tile.upgrades[i]=machine.upgrades(i);
            tile.focus=machine.visualFocus();for(int i=0;i<3;i++)tile.runes[i]=(byte)machine.visualRune(i);
            tile.placed=id.equals("void_chest")?machine.channel():-1;tile.network=machine.channel();
            tile.currentType=machine.visualCrystal();tile.degredation=machine.processes.degradation;
            tile.face=machine.visualFace();
            tile.portalOpen=machine.visualPortalOpen();
            if(tile.portalOpen&&!PortalViews.active())tile.txRender=PortalViews.request(machine);
            tile.isPowering=machine.totalVis()>=machine.capacity()*.9f;
        }
        tile.enchantmentChoice=tile.worked?0:-1;tile.enchantmentCost=tile.worked?1:0;
        if(state.getBlock() instanceof CrystalBlock){tile.crystals=state.getValue(CrystalBlock.AMOUNT);tile.orientation=state.getValue(CrystalBlock.FACING).get3DDataValue();}
        if(state.getBlock() instanceof TaintPodBlock)tile.growth=state.getValue(TaintPodBlock.AGE)*50;
        if(state.getBlock() instanceof EldritchBlock){
            tile.metadata=id.equals("eldritch_monolith")?2:id.equals("eldritch_receptacle")?3:id.equals("eldritch_lock")?4:5;
            var core=world.level.getBlockEntity(pos) instanceof dev.thaumcraft.world.MonolithBlockEntity own?own:dev.thaumcraft.world.MonolithBlockEntity.coreAt(world.level,pos);
            for(int i=0;i<4;i++)tile.runes[i]=(byte)(core==null?EldritchBlock.rune(pos,i):core.target(i));
            int index=core==null?-1:core.index(pos);tile.placed=index<0?-1:(byte)core.inserted(index);
        }
        LegacyAnimation.apply(tile);return tile;
    }
    public static List<LegacyDraw.Batch> render(net.minecraft.world.level.block.entity.BlockEntity entity,float partial,int light){
        var level=entity.getLevel();if(level==null)return List.of();BlockPos pos=entity.getBlockPos();float time=level.getGameTime()+partial;
        World world=new World(level,pos.asLong()^(long)(time*100));
        if(effectLevel!=level){EFFECT_TICKS.clear();CONDENSER_WISPS.clear();CRUCIBLE_WISPS.clear();FILTER_WISPS.clear();effectLevel=level;}
        Long previous=EFFECT_TICKS.put(pos,level.getGameTime());world.effectsAllowed=previous==null||previous!=level.getGameTime();
        if(PortalViews.active())world.effectsAllowed=false;
        if(EFFECT_TICKS.size()>4096)EFFECT_TICKS.remove(EFFECT_TICKS.keySet().iterator().next());
        return LegacyCompat.capture(world,pos,light,time,()->{
            var player=net.minecraft.client.Minecraft.getInstance().player;
            mod_ThaumCraft.inventory[2]=player!=null&&!dev.thaumcraft.api.IntegrationHooks.worn(player,stack->stack.is(Content.item("goggles_of_revealing"))).isEmpty()?1:0;
            if(world.effectsAllowed){
                ambientParticles(world,pos);
                if(entity instanceof MachineBlockEntity machine&&machine.machineId().equals("darkness_generator")){
                    var source=machine.visualDarknessMonolith();
                    int moon=level.environmentAttributes().getValue(net.minecraft.world.attribute.EnvironmentAttributes.MOON_PHASE,net.minecraft.world.phys.Vec3.ZERO).index();
                    int penalty=Math.abs(moon-4)*4+level.getMaxLocalRawBrightness(pos);
                    if(source!=null&&world.rand.nextInt(50+penalty)==0)LegacyLightning.spawn(world,pos,
                        new WRVector3(source.getX()-pos.getX()+.5,source.getY()-pos.getY()+.75+world.rand.nextFloat()*4,source.getZ()-pos.getZ()+.5),
                        new WRVector3(.5,.75,.5),5,3,2);
                }
            }
            TileEntity tile=snapshot(world,pos);if(tile==null)return;world.put(tile);Block block=blockFor(tile.id);RenderBlocks rb=new RenderBlocks();
            // Enclosed opaque models must precede their depth-writing glass shells.
            boolean enclosedBrain=tile instanceof TileBrain||tile instanceof TileEnchanterAdvanced;
            if(enclosedBrain){renderTile(tile,pos,partial,light);context().draw().offset(0,0,0);}
            if(entity instanceof MachineBlockEntity){
                for(int pass=0;pass<2;pass++){
                    if(tile.id.equals("traveling_trunk")&&pass!=0)continue;
                    PASS.set(pass);context().draw().bind(texture("/thaumcraft/resources/blocks.png"));context().draw().color(1,1,1,1);context().draw().light(light);context().draw().enable(3042,pass==1);context().draw().blend(770,771);
                    if(tile.id.equals("traveling_trunk"))context().draw().matrix().translate(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5);
                    if(block instanceof BlockApparatusWood wood)wood.renderAppWoodBlock(world,rb,pos.getX(),pos.getY(),pos.getZ(),block,false,tile.metadata);
                    else if(block instanceof BlockApparatusMetal metal)metal.renderAppMetalBlock(world,rb,pos.getX(),pos.getY(),pos.getZ(),block,false,tile.metadata);
                    else if(block instanceof BlockApparatusStone stone)stone.renderAppStoneBlock(world,rb,pos.getX(),pos.getY(),pos.getZ(),block,false,tile.metadata);
                    else if(block instanceof BlockApparatusFragile fragile)fragile.renderAppFragileBlock(world,rb,pos.getX(),pos.getY(),pos.getZ(),block,false,tile.metadata);
                    if(tile.id.equals("traveling_trunk"))context().draw().matrix().identity();
                }
                PASS.remove();
            }
            if(!enclosedBrain)renderTile(tile,pos,partial,light);
            LegacyLightning.render(world,pos,partial);
        });
    }
    private static void renderTile(TileEntity tile,BlockPos pos,float partial,int light){
        var renderer=RENDERERS.get(tile.id);if(renderer!=null){
            var view=net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera();var camera=view.position();
            renderer.tileEntityRenderer.playerX=camera.x;renderer.tileEntityRenderer.playerY=camera.y;renderer.tileEntityRenderer.playerZ=camera.z;
            var near=view.getNearPlane(view.getFov()).getPointOnPlane(0,0);
            ActiveRenderInfo.objectX=(float)near.x;ActiveRenderInfo.objectY=(float)near.y;ActiveRenderInfo.objectZ=(float)near.z;
            context().draw().offset(camera.x,camera.y,camera.z);
            context().draw().light(light);context().draw().color(1,1,1,1);renderer.renderTileEntityAt(tile,pos.getX()-camera.x,pos.getY()-camera.y,pos.getZ()-camera.z,partial);
        }
    }
    public static void ambient(net.minecraft.world.level.Level level,BlockPos pos){
        World world=new World(level,level.getRandom().nextLong());world.effectsAllowed=true;
        LegacyCompat.capture(world,pos,0xf000f0,level.getGameTime(),()->{displayParticles(world,pos);if(!(level.getBlockState(pos).getBlock() instanceof MachineBlock))ambientParticles(world,pos);});
    }
    private static void displayParticles(World world,BlockPos pos){
        String id=BuiltInRegistries.BLOCK.getKey(world.level.getBlockState(pos).getBlock()).getPath();var r=world.rand;var effects=new Effects();
        double x=pos.getX(),y=pos.getY(),z=pos.getZ();
        if((id.equals("thaumic_enchanter")||id.equals("occultic_enchanter")||id.equals("quaesitum"))&&r.nextBoolean()){
            // Original display scan: two outer-ring layers, with a per-layer air check.
            for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++){
                if(Math.abs(dx)!=2&&Math.abs(dz)!=2)continue;
                for(int dy=0;dy<=1;dy++){
                    var source=world.level.getBlockState(pos.offset(dx,dy,dz));
                    boolean shelf=source.is(net.minecraft.world.level.block.Blocks.BOOKSHELF);
                    boolean brain=source.is(Content.block("brain_in_a_jar"));
                    if(!(shelf||brain)||(shelf&&r.nextInt(16)!=0)||(brain&&r.nextInt(8)!=0))continue;
                    if(!world.level.isEmptyBlock(pos.offset(dx/2,dy,dz/2)))break;
                    // ENCHANT interprets velocity as the source offset and flies toward this target.
                    world.level.addParticle(net.minecraft.core.particles.ParticleTypes.ENCHANT,x+.5,y+2,z+.5,
                            dx+r.nextFloat()-.5,dy-r.nextFloat()-1f,dz+r.nextFloat()-.5);
                }
            }
        }
        if((id.equals("totem_of_dawn")||id.equals("totem_of_dusk"))&&r.nextInt(10)==0)effects.addEffect(new FXWisp(world,x+r.nextFloat(),y+r.nextFloat(),z+r.nextFloat(),.5,id.equals("totem_of_dawn")?0:5));
        if(List.of("crucible","crucible_of_eyes","thaumium_crucible").contains(id)){
            var flame=net.minecraft.client.Minecraft.getInstance().particleEngine.createParticle(net.minecraft.core.particles.ParticleTypes.FLAME,x+.2+r.nextFloat()*.6,y+.1,z+.2+r.nextFloat()*.6,0,0,0);
            if(flame instanceof net.minecraft.client.particle.SingleQuadParticle quad)quad.setColor(0,1,1);
        }
        if(id.equals("tainted_log")&&world.level.isEmptyBlock(pos.below())&&r.nextInt(10)==0){
            var drip=new FXDrip(world,x+.1+r.nextFloat()*.8,y,z+.1+r.nextFloat()*.8);drip.setRBGColorF(.45f+r.nextFloat()*.2f,.15f+r.nextFloat()*.15f,.45f+r.nextFloat()*.2f);effects.addEffect(drip);
        }
        if(id.equals("thaumic_crystalizer"))effects.addEffect(new FXSparkle(world,x+.1+r.nextFloat()*.8,y+.6+r.nextFloat()*.6,z+.1+r.nextFloat()*.8,1,r.nextInt(5),3));
        if(world.level.getBlockEntity(pos) instanceof MachineBlockEntity machine){
            if(id.equals("arcane_furnace")&&machine.visualWorking()){
                for(int i=0;i<3;i++)fire(world,x+.2+r.nextFloat()*.6,y+.1+r.nextFloat()*.5,z+.2+r.nextFloat()*.6,.06);
                double sy=y+.3+r.nextFloat()*.4,offset=r.nextFloat()*.6-.3;
                for(Direction d:Direction.Plane.HORIZONTAL)if(!world.level.getBlockState(pos.relative(d)).isSolidRender())fire(world,x+.5+d.getStepX()*.45+(d.getAxis()==Direction.Axis.Z?offset:0),sy,z+.5+d.getStepZ()*.45+(d.getAxis()==Direction.Axis.X?offset:0),0);
            }
            if(id.equals("void_interface")&&machine.visualVoidLinked()&&r.nextInt(10)==0)LegacyLightning.spawn(world,pos,new WRVector3(.5,.75,.5),new WRVector3(.5+r.nextFloat()-r.nextFloat(),2,.5+r.nextFloat()-r.nextFloat()),5,3,4);
        }
        if(id.equals("eldritch_monolith")&&r.nextInt(5)==0){
            for(int sign:new int[]{-1,1})if(!(world.level.getBlockState(pos.offset(0,sign,0)).getBlock() instanceof EldritchBlock))LegacyLightning.spawn(world,pos,new WRVector3(.5,sign<0?.25:.75,.5),new WRVector3(.5+(r.nextFloat()-r.nextFloat())*2,sign<0?-2:3,.5+(r.nextFloat()-r.nextFloat())*2),5,3,4);
        }
    }
    public static void equipmentEffect(dev.thaumcraft.network.EquipmentEffect event){
        var level=net.minecraft.client.Minecraft.getInstance().level;if(level==null)return;
        World world=new World(level,level.getRandom().nextLong());world.effectsAllowed=true;
        LegacyCompat.capture(world,BlockPos.ZERO,0xf000f0,level.getGameTime(),()->{
            var r=world.rand;var p=event.origin();var effects=new Effects();
            boolean low=dev.thaumcraft.PortConfig.lowGfx||!ModLoader.getMinecraftInstance().gameSettings.fancyGraphics;
            if(event.kind()==dev.thaumcraft.network.EquipmentEffect.DUPLICATOR){
                for(int i=0;i<(low?15:50);i++){
                    var wisp=new FXWisp(world,p.x,p.y,p.z,p.x-(r.nextFloat()-r.nextFloat())*1.5,p.y-(r.nextFloat()-r.nextFloat())*.1,p.z-(r.nextFloat()-r.nextFloat())*1.5,.15f,r.nextInt(5));
                    effects.addEffect(wisp);
                    level.playLocalSound(p.x,p.y,p.z,dev.thaumcraft.content.ModSounds.event("stomp"),net.minecraft.sounds.SoundSource.BLOCKS,.1f,1,false);
                }
            }else if(event.kind()==dev.thaumcraft.network.EquipmentEffect.MONOLITH){
                for(int i=0;i<50;i++){
                    var wisp=new FXWisp(world,p.x+r.nextFloat(),p.y-r.nextFloat()*2,p.z+r.nextFloat(),p.x+r.nextFloat(),p.y+r.nextFloat()*2+4,p.z+r.nextFloat(),.5f,5);
                    wisp.shrink=true;effects.addEffect(wisp);
                }
            }else if(event.kind()==dev.thaumcraft.network.EquipmentEffect.VOID_POOF){
                // Silent poofBad: the server sends one event per cleared doorway cell.
                for(int i=0;i<(low?5:10);i++){
                    var wisp=new FXWisp(world,p.x+.5+r.nextFloat()-r.nextFloat(),p.y+.5+r.nextFloat()-r.nextFloat(),p.z+.5+r.nextFloat()-r.nextFloat(),.3f,5);wisp.tinkle=false;effects.addEffect(wisp);
                }
            }else if(event.kind()==dev.thaumcraft.network.EquipmentEffect.VAMPIRIC||event.kind()==dev.thaumcraft.network.EquipmentEffect.SOUL_MARK){
                boolean vampiric=event.kind()==dev.thaumcraft.network.EquipmentEffect.VAMPIRIC;
                var sparkle=vampiric?new FXSparkle(world,p.x,p.y,p.z,2,4,6):new FXSparkle(world,p.x,p.y,p.z,1,5,5);sparkle.setGravity(vampiric?.1f:-.1f);effects.addEffect(sparkle);
            }else if(event.kind()==dev.thaumcraft.network.EquipmentEffect.BONE_MARK){
                var wisp=new FXWisp(world,p.x,p.y,p.z,.25f,5);wisp.setGravity(.05f);effects.addEffect(wisp);
            }else if(event.kind()==dev.thaumcraft.network.EquipmentEffect.SOUL){
                var wisp=new FXWisp(world,p.x,p.y,p.z,.5f,5);wisp.shrink=true;effects.addEffect(wisp);
            }else if(event.kind()==dev.thaumcraft.network.EquipmentEffect.WISP){
                if(dev.thaumcraft.PortConfig.lowGfx&&!r.nextBoolean())return;
                var wisp=new FXWisp(world,p.x,p.y,p.z,.4,2);wisp.shrink=true;effects.addEffect(wisp);
            }else if(event.kind()==dev.thaumcraft.network.EquipmentEffect.TILL){
                for(int i=0;i<(low?5:16);i++){
                    double x=p.x+r.nextFloat(),z=p.z+r.nextFloat();
                    var sparkle=new FXSparkle(world,x,p.y,z,x,p.y+r.nextFloat()*.5,z,2,0,5);sparkle.tinkle=true;effects.addEffect(sparkle);
                }
            }else if(event.kind()==dev.thaumcraft.network.EquipmentEffect.TRADE){
                for(int i=0;i<(low?5:10);i++)for(var face:net.minecraft.core.Direction.values()){
                    double x=p.x+r.nextFloat(),y=p.y+r.nextFloat(),z=p.z+r.nextFloat();
                    switch(face.getAxis()){
                        case X->x=p.x+(face.getStepX()>0?1.1:-.1);
                        case Y->y=p.y+(face.getStepY()>0?1.1:-.1);
                        case Z->z=p.z+(face.getStepZ()>0?1.1:-.1);
                    }
                    effects.addEffect(new FXSparkle(world,x,y,z,1.5f,3,3+r.nextInt(3)));
                }
            }else if(event.kind()==dev.thaumcraft.network.EquipmentEffect.POOF||event.kind()==dev.thaumcraft.network.EquipmentEffect.SMELT){
                for(int i=0;i<(low?3:6);i++){
                    level.addParticle(net.minecraft.core.particles.ParticleTypes.POOF,p.x+r.nextFloat(),p.y+r.nextFloat(),p.z+r.nextFloat(),0,0,0);
                    if(event.kind()==dev.thaumcraft.network.EquipmentEffect.SMELT)level.addParticle(net.minecraft.core.particles.ParticleTypes.FLAME,p.x+r.nextFloat(),p.y+r.nextFloat(),p.z+r.nextFloat(),0,0,0);
                }
            }
        });
    }
    public static void generatorArc(dev.thaumcraft.network.GeneratorArc event){
        var level=net.minecraft.client.Minecraft.getInstance().level;if(level==null)return;
        float arcs=event.amount()/4f;
        if(dev.thaumcraft.PortConfig.lowGfx||!ModLoader.getMinecraftInstance().gameSettings.fancyGraphics)arcs/=2;
        if(level.getRandom().nextFloat()*45>=arcs)return;
        World world=new World(level,level.getRandom().nextLong());world.effectsAllowed=true;
        var from=event.source();var to=event.target();
        LegacyLightning.spawn(world,from,new WRVector3(.5,.5,.5),new WRVector3(to.getX()-from.getX()+.5,to.getY()-from.getY()+.5,to.getZ()-from.getZ()+.5),0,6,9);
    }
    public static void soulAbsorption(dev.thaumcraft.network.SoulAbsorption effect){
        var level=net.minecraft.client.Minecraft.getInstance().level;if(level==null)return;
        World world=new World(level,level.getRandom().nextLong());world.effectsAllowed=true;
        var random=world.rand;var target=effect.target();var effects=new Effects();
        // Packet handlers run outside block extraction; effects still need a legacy context.
        LegacyCompat.capture(world,target,0xf000f0,level.getGameTime(),()->{
            for(int i=0;i<3;i++)effects.addEffect(new FXWisp(world,
                effect.x()+random.nextFloat()-random.nextFloat(),effect.y()+effect.height()/2+random.nextFloat()-random.nextFloat(),effect.z()+random.nextFloat()-random.nextFloat(),
                target.getX()+.5,target.getY()+.25,target.getZ()+.5,.3,5));
        });
    }
    private static boolean hasUpgrade(MachineBlockEntity machine){
        for(int i=0;i<8;i++)if(machine.upgrades(i)>0)return true;
        return false;
    }
    static void smallGreenFlame(World world,double x,double y,double z){
        if(!world.effectsAllowed)return;
        var flame=net.minecraft.client.Minecraft.getInstance().particleEngine.createParticle(net.minecraft.core.particles.ParticleTypes.FLAME,x,y,z,0,0,0);
        if(flame instanceof net.minecraft.client.particle.SingleQuadParticle quad){quad.setColor(0,1,1);quad.scale(.1f);}
    }
    private static void fire(World world,double x,double y,double z,double speed){
        world.level.addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE,x,y,z,0,speed,0);world.level.addParticle(net.minecraft.core.particles.ParticleTypes.FLAME,x,y,z,0,speed,0);
    }
    private static void ambientParticles(World world,BlockPos pos){
        String id=BuiltInRegistries.BLOCK.getKey(world.level.getBlockState(pos).getBlock()).getPath();var random=world.rand;
        double x=pos.getX()+.5,y=pos.getY()+.5,z=pos.getZ()+.5;Effects effects=new Effects();
        if(id.equals("vis_filter")&&world.level.getBlockEntity(pos) instanceof MachineBlockEntity machine){
            int sequence=machine.filterWisps();Integer previous=FILTER_WISPS.put(pos,sequence);
            if(previous!=null)for(int i=0;i<Math.clamp(sequence-previous,0,20);i++)
                effects.addEffect(new FXWisp(world,x,pos.getY()+.8+machine.filterStack(),z,
                    x+random.nextFloat()-random.nextFloat(),pos.getY()+3+machine.filterStack()+random.nextFloat(),z+random.nextFloat()-random.nextFloat(),.5,5));
        }
        if(id.contains("crucible")&&world.level.getBlockEntity(pos) instanceof MachineBlockEntity machine){
            int sequence=machine.processes.crucibleWisps;Integer previous=CRUCIBLE_WISPS.put(pos,sequence);
            if(previous!=null)for(int i=0;i<Math.clamp(sequence-previous,0,20);i++)
                effects.addEffect(new FXWisp(world,pos.getX()+random.nextFloat(),pos.getY()+.8,pos.getZ()+random.nextFloat(),
                    x+random.nextFloat()-random.nextFloat(),pos.getY()+3+random.nextFloat(),z+random.nextFloat()-random.nextFloat(),.5,5));
        }
        if(id.equals("vis_condenser")&&world.level.getBlockEntity(pos) instanceof MachineBlockEntity machine){
            int sequence=machine.processes.condenserWisps;Integer previous=CONDENSER_WISPS.put(pos,sequence);
            if(previous!=null&&machine.visualCrystal()>=0)for(int i=0;i<Math.clamp(sequence-previous,0,20);i++){
                var mote=new FXWisp(world,x+random.nextFloat()-random.nextFloat(),y+1+random.nextFloat()-random.nextFloat(),z+random.nextFloat()-random.nextFloat(),x,y+1,z,.1,machine.visualCrystal());effects.addEffect(mote);
            }
        }
        // The normal infuser emits its flame from TileInfuserRenderer's synchronized processing state.
        if((id.equals("dark_infuser")||id.equals("thaumic_restorer"))&&world.level.getBlockEntity(pos) instanceof MachineBlockEntity machine&&machine.visualWorking()&&hasUpgrade(machine)){
            double inset=id.equals("thaumic_restorer")?.25:.1;
            smallGreenFlame(world,pos.getX()+(random.nextBoolean()?inset:1-inset),pos.getY()+1.15,pos.getZ()+(random.nextBoolean()?inset:1-inset));
        }
        if(id.equals("everfull_urn")&&(!dev.thaumcraft.PortConfig.lowGfx||world.level.getGameTime()%3==0)){
            var water=new FXWisp(world,x,y+.5,z,x+random.nextFloat()-random.nextFloat(),y+4+random.nextFloat(),z+random.nextFloat()-random.nextFloat(),.15,7);water.setGravity(.2f);water.shrink=true;effects.addEffect(water);
        }else if(id.equals("glowing_nitor")){
            var sparkle=new FXSparkle(world,x,y,z,x+(random.nextFloat()-random.nextFloat())/3,y+(random.nextFloat()-random.nextFloat())/3,z+(random.nextFloat()-random.nextFloat())/3,1,6,3);sparkle.setGravity(.05f);effects.addEffect(sparkle);
            if(world.level.getGameTime()%6==0){var flame=new FXWisp(world,x,y,z,.5,4);flame.shrink=true;flame.setGravity(-.03f);effects.addEffect(flame);var center=new FXWisp(world,x,y,z,.25,1);center.setGravity(-.01f);effects.addEffect(center);}
        }else if(id.equals("taint_spore_pod")&&world.level.getBlockState(pos).getValue(TaintPodBlock.BURSTING)){
            int growth=world.level.getBlockState(pos).getValue(TaintPodBlock.AGE)*50;
            for(int i=0;i<3;i++){
                var spore=new FXWisp(world,x+(random.nextFloat()-random.nextFloat())*.5,y+.5*growth/1000,z+(random.nextFloat()-random.nextFloat())*.5,
                    x+(random.nextFloat()-random.nextFloat())*2,pos.getY()+4+random.nextFloat()*2,z+(random.nextFloat()-random.nextFloat())*2,.5,5);
                spore.setGravity(.1f);effects.addEffect(spore);
            }
        }else if(id.equals("shimmerleaf")&&random.nextBoolean())effects.addEffect(new FXWisp(world,x+(random.nextFloat()-random.nextFloat())*.1,y+.1+(random.nextFloat()-random.nextFloat())*.1,z+(random.nextFloat()-random.nextFloat())*.1,.25,.3+random.nextFloat()*.3,.7+random.nextFloat()*.3,.7+random.nextFloat()*.3));
        else if(id.equals("cinderpearl")){
            x+=(random.nextFloat()-random.nextFloat())*.1;y+=.1+(random.nextFloat()-random.nextFloat())*.1;z+=(random.nextFloat()-random.nextFloat())*.1;
            world.level.addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE,x,y,z,0,0,0);world.level.addParticle(net.minecraft.core.particles.ParticleTypes.FLAME,x,y,z,0,0,0);
        }else if(id.equals("silverwood_leaves")&&random.nextInt(8)==0)effects.addEffect(new FXSparkle(world,x+random.nextFloat()-random.nextFloat(),y+random.nextFloat()-random.nextFloat(),z+random.nextFloat()-random.nextFloat(),1.5,7,6));
        else if(id.equals("brazier_of_souls")&&world.level.getGameTime()%5==0&&world.level.getBlockEntity(pos) instanceof MachineBlockEntity machine&&machine.visualWorking()){
            var dark=new FXWisp(world,x,y+.5,z,.6,5);dark.shrink=true;dark.setGravity(-.03f);effects.addEffect(dark);
            var core=new FXWisp(world,x,y+.3,z,.2,6);core.setGravity(-.015f);effects.addEffect(core);
            var sparkle=new FXSparkle(world,x+(random.nextFloat()-random.nextFloat())/5,y+.5+(random.nextFloat()-random.nextFloat())/5,z+(random.nextFloat()-random.nextFloat())/5,.65,6,3);sparkle.setGravity(-.03f);effects.addEffect(sparkle);
        }
    }
}
