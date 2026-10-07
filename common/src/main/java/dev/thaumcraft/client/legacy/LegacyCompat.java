package dev.thaumcraft.client.legacy;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.thaumcraft.Thaumcraft;
import dev.thaumcraft.content.Content;
import dev.thaumcraft.machine.MachineBlock;
import dev.thaumcraft.machine.MachineBlockEntity;
import dev.thaumcraft.machine.MachineConnections;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Render-only compatibility values. No legacy game classes or OpenGL calls run. */
public final class LegacyCompat {
    private LegacyCompat() {}
    public record Context(LegacyDraw draw,World world,float time) {}
    private static final ThreadLocal<Context> CURRENT=new ThreadLocal<>();
    private static final Map<String,Integer> WIDTHS=new HashMap<>();
    private static final List<Identifier> TEXTURES=new ArrayList<>();
    public static int registerTexture(Identifier texture){int index=TEXTURES.indexOf(texture);if(index>=0)return index;TEXTURES.add(texture);return TEXTURES.size()-1;}
    public static Context context(){return java.util.Objects.requireNonNull(CURRENT.get(),"Legacy drawing outside extraction");}
    public static List<LegacyDraw.Batch> capture(World world,BlockPos origin,int light,float time,Runnable drawing) {
        Config.lowGfx=dev.thaumcraft.PortConfig.lowGfx;Config.pipedrips=dev.thaumcraft.PortConfig.pipedrips;Config.portalGfx=dev.thaumcraft.PortConfig.portalGfx;
        Context previous=CURRENT.get();LegacyDraw draw=new LegacyDraw(origin,light);CURRENT.set(new Context(draw,world,time));
        try {drawing.run();return draw.finish();} finally {if(previous==null)CURRENT.remove();else CURRENT.set(previous);}
    }
    public static long clockMillis(){return (long)(context().time*50);}
    public static Identifier texture(String path){
        if(path.startsWith("/thaumcraft/resources/"))return Thaumcraft.id("textures/legacy/"+path.substring("/thaumcraft/resources/".length()).toLowerCase(java.util.Locale.ROOT));
        if(path.equals("/terrain.png"))return Thaumcraft.id("textures/legacy/blocks.png");
        return Thaumcraft.id("textures/legacy/"+path.substring(path.lastIndexOf('/')+1).toLowerCase(java.util.Locale.ROOT));
    }
    public static int textureWidth(String path){return WIDTHS.computeIfAbsent(path,key->{
        try(var in=net.minecraft.client.Minecraft.getInstance().getResourceManager().open(texture(key));var image=NativeImage.read(in)){return image.getWidth();}
        catch(java.io.IOException e){throw new IllegalStateException("Missing original rendering texture "+key,e);}
    });}
    public static final class GL11 {
        public static void glPushMatrix(){context().draw.push();} public static void glPopMatrix(){context().draw.pop();}
        public static void glTranslatef(float x,float y,float z){context().draw.matrix().translate(x,y,z);}
        public static void glTranslated(double x,double y,double z){context().draw.matrix().translate(x,y,z);}
        public static void glScalef(float x,float y,float z){context().draw.matrix().scale(x,y,z);}
        public static void glScaled(double x,double y,double z){glScalef((float)x,(float)y,(float)z);}
        public static void glRotatef(float angle,float x,float y,float z){if(x*x+y*y+z*z>0)context().draw.matrix().rotate(Math.toRadians(angle),new org.joml.Vector3d(x,y,z).normalize());}
        public static void glMatrixMode(int mode){context().draw.matrixMode(mode);} public static void glLoadIdentity(){context().draw.matrix().identity();}
        public static void glEnable(int cap){context().draw.enable(cap,true);} public static void glDisable(int cap){context().draw.enable(cap,false);}
        public static void glBlendFunc(int source,int dest){context().draw.blend(source,dest);} public static void glDepthMask(boolean value){context().draw.depthWrite(value);}
        public static void glColor4f(float r,float g,float b,float a){context().draw.color(r,g,b,a);} public static void glColor3f(float r,float g,float b){glColor4f(r,g,b,1);}
        public static void glBindTexture(int target,int id){if(id>=0&&id<TEXTURES.size())context().draw.bind(TEXTURES.get(id));}
        public static void glTexGen(int coord,int pname,FloatBuffer plane){context().draw.plane(coord,pname,new Vector4f(plane.get(0),plane.get(1),plane.get(2),plane.get(3)));}
        public static void glBegin(int mode){context().draw.begin(mode);} public static void glVertex2f(float x,float y){context().draw.vertex(x,y,0,0,0);} public static void glEnd(){context().draw.end();}
    }
    public static final class Tessellator {
        public static final Tessellator instance=new Tessellator();
        public void startDrawingQuads(){context().draw.begin(7);} public void startDrawing(int mode){context().draw.begin(mode);}
        public void addVertexWithUV(double x,double y,double z,double u,double v){context().draw.vertex(x,y,z,u,v);}
        public void addVertex(double x,double y,double z){addVertexWithUV(x,y,z,0,0);} public void draw(){context().draw.end();}
        public void setBrightness(int value){context().draw.light(value);} public void setNormal(float x,float y,float z){context().draw.normal(x,y,z);}
        public void setColorOpaque_F(float r,float g,float b){context().draw.color(r,g,b,1);} public void setColorRGBA_F(float r,float g,float b,float a){context().draw.color(r,g,b,a);}
        public void setColorRGBA_I(int rgb,int alpha){setColorRGBA_F((rgb>>16&255)/255f,(rgb>>8&255)/255f,(rgb&255)/255f,alpha/255f);}
    }
    public static final class MathHelper {
        public static float sin(float x){return (float)Math.sin(x);} public static float cos(float x){return (float)Math.cos(x);}
        public static float sqrt_float(float x){return (float)Math.sqrt(x);} public static int floor_float(float x){return (int)Math.floor(x);}
        public static int func_40346_b(float x){return (int)Math.floor(x);}
    }
    public static class Entity {public int ticksExisted;public double posX,posY,posZ;public double getDistance(double x,double y,double z){return Math.sqrt((x-posX)*(x-posX)+(y-posY)*(y-posY)+(z-posZ)*(z-posZ));}}
    public static class EntityLiving extends Entity {public int health=1;public int getHealth(){return health;}}
    public static class EntityWisp extends EntityLiving {public int type;}
    public static class EntitySingularity extends Entity {public int fuse;}
    public static class Render {public float shadowSize;public void doRender(Entity entity,double x,double y,double z,float yaw,float partial){}public void loadTexture(String path){MinecraftForgeClient.bindTexture(path);}}
    public static class ModelBase {
        public int textureWidth=64,textureHeight=32;
        public void render(Entity entity,float a,float b,float c,float d,float e,float scale){}
        public void setRotationAngles(float a,float b,float c,float d,float e,float scale){}
    }
    public static class ModelRenderer {
        public float rotateAngleX,rotateAngleY,rotateAngleZ,rotationPointX,rotationPointY,rotationPointZ;
        public boolean mirror,showModel=true,isHidden;
        private int width,height,u,v;
        private final List<ModelPart.Cube> cubes=new ArrayList<>();
        public ModelRenderer(ModelBase model,int u,int v){width=model.textureWidth;height=model.textureHeight;this.u=u;this.v=v;}
        public ModelRenderer(ModelBase model,String name){this(model,0,0);}
        public ModelRenderer setTextureOffset(int u,int v){this.u=u;this.v=v;return this;}
        public ModelRenderer setTextureSize(int width,int height){this.width=width;this.height=height;return this;}
        public ModelRenderer addBox(float x,float y,float z,int w,int h,int d){return addBox(x,y,z,w,h,d,0);}
        public ModelRenderer addBox(float x,float y,float z,int w,int h,int d,float grow){cubes.add(new ModelPart.Cube(u,v,x,y,z,w,h,d,grow,grow,grow,mirror,width,height,EnumSet.allOf(Direction.class)));return this;}
        public void setRotationPoint(float x,float y,float z){rotationPointX=x;rotationPointY=y;rotationPointZ=z;}
        public void render(float scale){
            if(!showModel||isHidden)return;
            GL11.glPushMatrix();GL11.glTranslatef(rotationPointX*scale,rotationPointY*scale,rotationPointZ*scale);
            GL11.glRotatef((float)Math.toDegrees(rotateAngleZ),0,0,1);GL11.glRotatef((float)Math.toDegrees(rotateAngleY),0,1,0);GL11.glRotatef((float)Math.toDegrees(rotateAngleX),1,0,0);
            for(ModelPart.Cube cube:cubes)for(var polygon:cube.polygons){
                var normal=polygon.normal();Tessellator.instance.setNormal(normal.x(),normal.y(),normal.z());Tessellator.instance.startDrawingQuads();
                for(var vertex:polygon.vertices())Tessellator.instance.addVertexWithUV(vertex.x()*scale,vertex.y()*scale,vertex.z()*scale,vertex.u(),vertex.v());
                Tessellator.instance.draw();
            }
            GL11.glPopMatrix();
        }
    }
    public static final class ModelBook extends ModelBase {
        private final net.minecraft.client.model.object.book.BookModel model=new net.minecraft.client.model.object.book.BookModel(net.minecraft.client.model.object.book.BookModel.createBodyLayer().bakeRoot());
        @Override public void render(Entity entity,float time,float page1,float page2,float open,float ignored,float scale){
            model.setupAnim(net.minecraft.client.model.object.book.BookModel.State.forAnimation(time,page1,page2,open));
            model.root().visit(new PoseStack(),(pose,name,index,cube)->{
                for(var polygon:cube.polygons){var normal=pose.transformNormal(polygon.normal(),new Vector3f());Tessellator.instance.setNormal(normal.x,normal.y,normal.z);Tessellator.instance.startDrawingQuads();
                    for(var vertex:polygon.vertices()){var p=pose.pose().transformPosition(vertex.worldX(),vertex.worldY(),vertex.worldZ(),new Vector3f());Tessellator.instance.addVertexWithUV(p.x,p.y,p.z,vertex.u(),vertex.v());}Tessellator.instance.draw();}
            });
        }
    }
    public interface IBlockAccess {
        int getBlockMetadata(int x,int y,int z);TileEntity getBlockTileEntity(int x,int y,int z);int getBlockId(int x,int y,int z);
    }
    public static class World implements IBlockAccess {
        public final Level level;public final Random rand;
        public boolean effectsAllowed;
        private final Map<BlockPos,TileEntity> tiles=new HashMap<>();
        public World(Level level,long seed){this.level=level;rand=new Random(seed);}
        public void put(TileEntity tile){tile.worldObj=this;tiles.put(new BlockPos(tile.xCoord,tile.yCoord,tile.zCoord),tile);}
        @Override public int getBlockMetadata(int x,int y,int z){var tile=getBlockTileEntity(x,y,z);if(tile!=null)return tile.metadata;var state=level.getBlockState(new BlockPos(x,y,z));var entry=Content.DEFINITIONS.get(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath());return entry==null?0:entry.meta();}
        @Override public TileEntity getBlockTileEntity(int x,int y,int z){
            BlockPos pos=new BlockPos(x,y,z);if(tiles.containsKey(pos))return tiles.get(pos);
            TileEntity tile=LegacyVisuals.snapshot(this,pos);tiles.put(pos,tile);return tile;
        }
        @Override public int getBlockId(int x,int y,int z){var state=level.getBlockState(new BlockPos(x,y,z));return state.getBlock() instanceof dev.thaumcraft.world.EldritchBlock?mod_ThaumCraft.blockHidden.blockID:net.minecraft.core.registries.BuiltInRegistries.BLOCK.getId(state.getBlock());}
        public boolean isAirBlock(int x,int y,int z){return level.getBlockState(new BlockPos(x,y,z)).isAir();}
        public boolean isBlockOpaqueCube(int x,int y,int z){return level.getBlockState(new BlockPos(x,y,z)).isSolidRender();}
    }
    public static class Block {
        public int blockID,metadata;public double minX,minY,minZ,maxX=1,maxY=1,maxZ=1;
        public void setBlockBounds(float x,float y,float z,float a,float b,float c){minX=x;minY=y;minZ=z;maxX=a;maxY=b;maxZ=c;}
        public int getRenderBlockPass(){return LegacyVisuals.renderPass();}
        public int colorMultiplier(IBlockAccess world,int x,int y,int z){return 0xffffff;}
        public int getMixedBrightnessForBlock(IBlockAccess world,int x,int y,int z){return net.minecraft.client.renderer.LevelRenderer.getLightCoords(((World)world).level,new BlockPos(x,y,z));}
        public int getBlockTexture(IBlockAccess world,int x,int y,int z,int side){return getBlockTextureFromSideAndMetadata(side,world.getBlockMetadata(x,y,z));}
        public int getBlockTextureFromSideAndMetadata(int side,int meta){return getBlockTextureFromSide(side);}
        public int getBlockTextureFromSide(int side){return 255;}
    }
    public static final class RenderBlocks {
        public int overrideBlockTexture=-1;
        public boolean renderStandardBlock(Block block,int x,int y,int z){
            int rgb=block.colorMultiplier(context().world,x,y,z);
            for(int side=0;side<6;side++){
                var direction=Direction.from3DDataValue(side);BlockPos adjacent=new BlockPos(x,y,z).relative(direction);
                boolean boundary=switch(side){case 0->block.minY<=0;case 1->block.maxY>=1;case 2->block.minZ<=0;case 3->block.maxZ>=1;case 4->block.minX<=0;default->block.maxX>=1;};
                if(boundary&&context().world.level.getBlockState(adjacent).isSolidRender())continue;
                float shade=side==0?.5f:side==1?1:side<4?.8f:.6f;
                context().draw.color((rgb>>16&255)/255f*shade,(rgb>>8&255)/255f*shade,(rgb&255)/255f*shade,1);
                BlockPos lighting=boundary?adjacent:new BlockPos(x,y,z);context().draw.light(net.minecraft.client.renderer.LevelRenderer.getLightCoords(context().world.level,lighting));
                face(block,x,y,z,side,overrideBlockTexture>=0?overrideBlockTexture:block.getBlockTexture(context().world,x,y,z,side));
            }
            return true;
        }
        private void face(Block b,double x,double y,double z,int side,int tile){
            var animated=tile>=252&&tile<=254?LegacyTerrain.sprite(tile):null;
            if(animated!=null)context().draw.bind(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS);
            double x0=x+b.minX,x1=x+b.maxX,y0=y+b.minY,y1=y+b.maxY,z0=z+b.minZ,z1=z+b.maxZ;
            double[][] points=switch(side){
                case 0 -> new double[][]{{x0,y0,z1},{x0,y0,z0},{x1,y0,z0},{x1,y0,z1}};
                case 1 -> new double[][]{{x1,y1,z1},{x1,y1,z0},{x0,y1,z0},{x0,y1,z1}};
                case 2 -> new double[][]{{x1,y1,z0},{x1,y0,z0},{x0,y0,z0},{x0,y1,z0}};
                case 3 -> new double[][]{{x0,y1,z1},{x0,y0,z1},{x1,y0,z1},{x1,y1,z1}};
                case 4 -> new double[][]{{x0,y1,z0},{x0,y0,z0},{x0,y0,z1},{x0,y1,z1}};
                default -> new double[][]{{x1,y1,z1},{x1,y0,z1},{x1,y0,z0},{x1,y1,z0}};
            };
            Tessellator tess=Tessellator.instance;var normal=Direction.from3DDataValue(side).getUnitVec3f();tess.setNormal(normal.x(),normal.y(),normal.z());
            if(tile==71&&b instanceof BlockApparatusWood){
                // The duplicator front has a 10x10 transparent window. A solid
                // quad can still occlude its tile-entity piston in the modern pass.
                double[][] strips={{0,0,1,3/16d},{0,13/16d,1,1},{0,3/16d,3/16d,13/16d},{13/16d,3/16d,1,13/16d}};
                for(double[] strip:strips){
                    double[] us={strip[0],strip[0],strip[2],strip[2]},vs={strip[1],strip[3],strip[3],strip[1]};
                    tess.startDrawingQuads();
                    for(int n=0;n<4;n++){
                        double u=us[n],v=vs[n];
                        double px=points[0][0]*(1-u)*(1-v)+points[1][0]*(1-u)*v+points[2][0]*u*v+points[3][0]*u*(1-v);
                        double py=points[0][1]*(1-u)*(1-v)+points[1][1]*(1-u)*v+points[2][1]*u*v+points[3][1]*u*(1-v);
                        double pz=points[0][2]*(1-u)*(1-v)+points[1][2]*(1-u)*v+points[2][2]*u*v+points[3][2]*u*(1-v);
                        tess.addVertexWithUV(px,py,pz,((tile%16)+u*.999375)/16,((tile/16)+v*.999375)/16);
                    }
                    tess.draw();
                }
                return;
            }
            // World RenderBlocks emitted into a caller-owned batch; the adapter splits per face.
            tess.startDrawingQuads();
            for(double[] point:points){double u=side<2?point[0]-x:side<4?(side==2?x1-point[0]+b.minX:point[0]-x):(side==5?z1-point[2]+b.minZ:point[2]-z);double v=side<2?point[2]-z:1-(point[1]-y);u=Math.clamp(u,0,1);v=Math.clamp(v,0,1);tess.addVertexWithUV(point[0],point[1],point[2],animated==null?((tile%16)+u*.999375)/16:animated.getU((float)u),animated==null?((tile/16)+v*.999375)/16:animated.getV((float)v));}
            tess.draw();
            if(animated!=null)context().draw.bind(texture("/thaumcraft/resources/blocks.png"));
        }
        public void renderBottomFace(Block b,double x,double y,double z,int t){face(b,x,y,z,0,t);} public void renderTopFace(Block b,double x,double y,double z,int t){face(b,x,y,z,1,t);}
        // These pre-1.3 names refer to the old world-axis convention: north=-X, south=+X.
        public void renderNorthFace(Block b,double x,double y,double z,int t){face(b,x,y,z,4,t);} public void renderSouthFace(Block b,double x,double y,double z,int t){face(b,x,y,z,5,t);}
        public void renderEastFace(Block b,double x,double y,double z,int t){face(b,x,y,z,2,t);} public void renderWestFace(Block b,double x,double y,double z,int t){face(b,x,y,z,3,t);}
    }
    public interface IConnection {boolean getConnectable(HelperFacing facing);float getPureVis();float getTaintedVis();boolean isVisConduit();}
    public enum HelperFacing {NEGY,POSY,NEGZ,POSZ,NEGX,POSX,UNKNOWN;
        public HelperFacing turnAround(){return this==UNKNOWN?UNKNOWN:values()[ordinal()^1];}}
    public static class HelperLocation {
        public double x,y,z;public HelperFacing facing=HelperFacing.UNKNOWN;
        public HelperLocation(TileEntity tile){x=tile.xCoord;y=tile.yCoord;z=tile.zCoord;}
        public TileEntity getConnectableTile(IBlockAccess world){if(facing==HelperFacing.UNKNOWN)return null;Direction d=Direction.from3DDataValue(facing.ordinal());x+=d.getStepX();y+=d.getStepY();z+=d.getStepZ();TileEntity tile=world.getBlockTileEntity((int)x,(int)y,(int)z);return tile instanceof IConnection connection&&connection.getConnectable(facing.turnAround())?tile:null;}
    }
    public static class TileEntity {
        public World worldObj;public String id="";public int xCoord,yCoord,zCoord,metadata,orientation;
        public float pureVis,taintedVis,displayPure,displayTaint,maxVis=500,rotation,angle,rota,rotb,bobbin,scale=1,press,degredation,pSize,growth,sucked;
        public int focus=-1,duration,currentType,currentItemCopyCost,duplicatorCopyTime,setting,crystals=1,placed=-1,enchantmentChoice=-1,enchantmentCost,network,face=3;
        public boolean open=true,worked,powered,isPowering,portalOpen;public String png="";public int[] upgrades=new int[8];public byte[] runes={-1,-1,-1,-1};
        public float field_40059_f,field_40060_g,field_40063_b,field_40065_c,field_40067_p,field_40068_a,field_40069_h;
        public PortalRenderer txRender;
        public int getBlockMetadata(){return metadata;} public Block getBlockType(){return LegacyVisuals.blockFor(id);} public boolean gettingPower(){return powered;}
        public boolean hasUpgrade(int type){return type>=0&&type<upgrades.length&&upgrades[type]>0;} public boolean isCooking(){return worked;}
    }
    public static class VisTile extends TileEntity implements IConnection {
        public boolean getConnectable(HelperFacing side){return side!=HelperFacing.UNKNOWN&&MachineConnections.accepts(worldObj.level,new BlockPos(xCoord,yCoord,zCoord),worldObj.level.getBlockState(new BlockPos(xCoord,yCoord,zCoord)),Direction.from3DDataValue(side.ordinal()));}
        public float getPureVis(){return pureVis;}public float getTaintedVis(){return taintedVis;}public boolean isVisConduit(){return this instanceof TileConduit||this instanceof TileConduitTank;}
        public float getMaxVis(){return maxVis;}
    }
    public static class TileConduit extends VisTile {} public static class TileConduitTank extends VisTile {}
    public static class TileConduitPump extends TileConduit {} public static class TileConduitValve extends TileConduit {} public static class TileConduitValveAdvanced extends TileConduitValve {}
    public static class TileFilter extends TileConduit {} public static class TilePurifier extends VisTile {}
    public static class TileCrucible extends VisTile {} public static class TileBellows extends VisTile {}
    public static class TileBore extends VisTile {} public static class TileCondenser extends VisTile {}
    public static class TileDuplicator extends VisTile {} public static class TileRepairer extends VisTile {}
    public static class TileGenerator extends VisTile {} public static class TileCrystalizer extends VisTile {}
    public static class TileInfuser extends VisTile {public boolean processing;} public static class TileEnchanter extends VisTile {}
    public static class TileEnchanterAdvanced extends VisTile {} public static class TileResearcher extends TileEntity {}
    public static class TileBrain extends TileEntity {} public static class TileSeal extends TileEntity {}
    public static class TileVoidCube extends TileEntity {} public static class TileVoidHole extends TileEntity {}
    public static class TileVoidInterface extends TileEntity {} public static class TileCrystalOre extends TileEntity {} public static class TileTaintSeed extends TileEntity {}
    public static class TileEntitySpecialRenderer {
        public final Camera tileEntityRenderer=new Camera();
        public void bindTextureByName(String name){context().draw.bind(texture(name));}
        public void renderTileEntityAt(TileEntity tile,double x,double y,double z,float partial){}
    }
    public static class Camera {public double playerX,playerY,playerZ;}
    public static class Minecraft {
        public Entity thePlayer=new Entity();public World theWorld;public final RenderEngine renderEngine=new RenderEngine();public final Effects effectRenderer=new Effects();public final GameSettings gameSettings=new GameSettings();
    }
    public static final class GameSettings {public boolean fancyGraphics=net.minecraft.client.Minecraft.getInstance().options.getEffectiveRenderDistance()>4;public int renderDistance=Math.clamp(3-(int)(Math.log(Math.max(1,net.minecraft.client.Minecraft.getInstance().options.getEffectiveRenderDistance()/4.0))/Math.log(2)),0,3);}
    public static final class ModLoader {
        public static Minecraft getMinecraftInstance(){var mc=new Minecraft();mc.theWorld=context().world;mc.thePlayer.ticksExisted=(int)context().time;var player=net.minecraft.client.Minecraft.getInstance().player;if(player!=null){mc.thePlayer.posX=player.getX();mc.thePlayer.posY=player.getY();mc.thePlayer.posZ=player.getZ();}return mc;}
    }
    public static class RenderEngine {
        public int getTexture(String path){return registerTexture(texture(path));}
    }
    public static final class MinecraftForgeClient {public static void bindTexture(String path){context().draw.bind(texture(path));}}
    public static final class GLAllocation {public static FloatBuffer createDirectFloatBuffer(int size){return FloatBuffer.allocate(size);}}
    public static final class ActiveRenderInfo {public static float objectX,objectY,objectZ,rotationX,rotationXZ,rotationZ,rotationYZ,rotationXY;}
    public static final class Config {public static boolean lowGfx=false,pipedrips=true,portalGfx=true;}
    public static final class mod_ThaumCraft {public static final int spdUpgrade=252,visCubeFX=253,visDripFX=254;public static final int[] inventory=new int[3];public static final Block blockHidden=new Block();static {blockHidden.blockID=-999;}}
    public static final class ThaumCraftCore {
        public static boolean isVisibleTo(float radius,Entity player,double x,double y,double z){return player.getDistance(x,y,z)<Math.min(400,net.minecraft.client.Minecraft.getInstance().options.getEffectiveRenderDistance()*16);}
        public static float fColorR(){return 1;}public static float fColorG(){return 1;}public static float fColorB(){return 1;}
    }
    public static class FXWisp {
        public final World world;public final double x,y,z;public double dx,dy,dz;
        public boolean shrink,tinkle;public float gravity,red=1,green,blue,size=1;public int lifetime,multiplier=2,blend=1;
        public FXWisp(World world,double x,double y,double z,double... options){
            this.world=world;this.x=x;this.y=y;this.z=z;lifetime=(int)(36/(world.rand.nextFloat()*.3+.7));
            int offset=options.length>=5?3:0;
            if(options.length-offset==2){size=(float)options[offset];colorType((int)options[offset+1]);}
            else if(options.length-offset==3){red=(float)options[offset];green=(float)options[offset+1];blue=(float)options[offset+2];}
            else if(options.length-offset==4){size=(float)options[offset];red=(float)options[offset+1];green=(float)options[offset+2];blue=(float)options[offset+3];}
            if(options.length-offset>=3&&red==0)red=1;
            if(offset==3){dx=(options[0]-x)/lifetime;dy=(options[1]-y)/lifetime;dz=(options[2]-z)/lifetime;}
        }
        protected void colorType(int type){
            var r=world.rand;switch(type){
                case 0->{red=.75f+r.nextFloat()*.25f;green=.25f+r.nextFloat()*.25f;blue=.75f+r.nextFloat()*.25f;}
                case 1->{red=.5f+r.nextFloat()*.3f;green=.5f+r.nextFloat()*.3f;blue=.2f;}
                case 2->{red=green=.2f;blue=.7f+r.nextFloat()*.3f;}
                case 3->{red=blue=.2f;green=.7f+r.nextFloat()*.3f;}
                case 4->{red=.7f+r.nextFloat()*.3f;green=blue=.2f;}
                case 5->{blend=771;red=r.nextFloat()*.1f;green=r.nextFloat()*.1f;blue=r.nextFloat()*.1f;}
                case 6->{red=.8f+r.nextFloat()*.2f;green=.8f+r.nextFloat()*.2f;blue=.8f+r.nextFloat()*.2f;}
                case 7->{red=green=.2f+r.nextFloat()*.3f;blue=.7f+r.nextFloat()*.3f;}
            }
        }
        public void setGravity(float gravity){this.gravity=gravity;}public void setRBGColorF(float r,float g,float b){red=r;green=g;blue=b;}
    }
    public static class FXSparkle extends FXWisp {public FXSparkle(World world,double x,double y,double z,double... options){
        super(world,x,y,z,java.util.Arrays.copyOf(options,options.length-1));multiplier=(int)options[options.length-1];lifetime=3*multiplier;
        if(options.length>=6){dx=(options[0]-x)/lifetime;dy=(options[1]-y)/lifetime;dz=(options[2]-z)/lifetime;}
        int typeIndex=options.length>=6?4:1;
        if(options.length>typeIndex&&(int)options[typeIndex]==7){red=.2f;green=.5f+world.rand.nextFloat()*.3f;blue=.6f+world.rand.nextFloat()*.3f;}
    }}
    public static class FXDrip extends FXWisp {public FXDrip(World world,double x,double y,double z){super(world,x,y,z);red=green=0;blue=1;}}
    public static class Effects {public void addEffect(FXWisp effect){
        if(!context().world.effectsAllowed)return;
        var engine=net.minecraft.client.Minecraft.getInstance().particleEngine;
        if(effect instanceof FXDrip)engine.add(new LegacyDripParticle(effect));
        else engine.add(new LegacyParticle(effect));
    }}
    public static class PortalRenderer {public static int renderRecursion;public int portalTexture;}
}
