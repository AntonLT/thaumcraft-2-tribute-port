package dev.thaumcraft.client;

import dev.thaumcraft.Thaumcraft;
import dev.thaumcraft.content.Content;
import dev.thaumcraft.gameplay.GameData;
import dev.thaumcraft.item.ItemState;
import dev.thaumcraft.machine.SealLogic;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Original four bookmarks, learned-first ordering, recipe scrolls and ordered seal selector. */
public final class ResearchScreen extends Screen {
    private dev.thaumcraft.gameplay.AddonData.Snapshot catalog=dev.thaumcraft.gameplay.AddonData.current();
    private int categoryOffset;
    private int visibleCategories(){return Math.min(catalog.categories().size(),Math.max(4,(height/2+35)/20));}
    private int categoryY(int offset){return height/2-45+offset*20;}
    private static final int[][] INFUSION={{128,26},{71,126},{185,126},{95,74},{160,74},{128,131}};
    private static final int[][] DARK={{128,35},{188,79},{164,150},{93,149},{67,78}};
    private static int lastCategory,lastIndex;
    private final boolean tome,crystal;
    private final Set<Identifier> known=new HashSet<>();
    private final Set<String> seals;
    private final int[] runes=new int[3];
    private List<GameData.Project> projects;
    private int category,index,recipeIndex,textOffset;
    public static void open(Minecraft minecraft,ItemStack stack){
        var entry=Content.entry(stack);if(entry==null)return;
        if(Set.of("ItemDiscoveryTome","itemDiscovery","ItemCrystalBall").contains(entry.source_class()))minecraft.setScreen(new ResearchScreen(stack));
    }
    public ResearchScreen(ItemStack stack){
        super(Component.translatableWithFallback("gui.thaumcraft2tp.research.title", "Thaumcraft Research"));
        var entry=Content.entry(stack);tome=entry.source_class().equals("ItemDiscoveryTome");crystal=entry.source_class().equals("ItemCrystalBall");
        for(String value:ItemState.getString(stack,"book_known","").split(","))if(!value.isEmpty()){var id=GameData.parseResearchId(value);if(id!=null)known.add(id);}
        seals=new HashSet<>(Arrays.asList(ItemState.getString(stack,"book_seals","").split(";")));
        for(int i=0;i<3;i++)runes[i]=Math.clamp(ItemState.getInt(stack,"book_rune_"+i,i==0?0:-1),i==0?0:-1,5);
        if(tome){category=Math.clamp(lastCategory,0,catalog.categories().size()-1);refresh();index=Math.clamp(lastIndex,0,Math.max(0,projects.size()-1));}
        else if(!crystal){projects=dev.thaumcraft.gameplay.ResearchItemData.project(stack).stream().toList();projects.forEach(p->known.add(p.key()));}
        else projects=List.of();
    }
    private void refresh(){projects=GameData.projects().stream().filter(p->p.category()==category).sorted(Comparator.comparing((GameData.Project p)->!known.contains(p.key())).thenComparing(GameData.Project::name)).toList();index=0;recipeIndex=0;textOffset=0;}
    public int category(){return category;}public int pageIndex(){return index;}public int[] selectedRunes(){return runes.clone();}
    private void blit(GuiGraphicsExtractor g,String texture,int x,int y,int u,int v,int w,int h){g.blit(RenderPipelines.GUI_TEXTURED,Thaumcraft.id("textures/legacy/"+texture+".png"),x,y,u,v,w,h,256,256);}
    private boolean inside(double mx,double my,int x,int y,int w,int h){return mx>=x&&mx<x+w&&my>=y&&my<y+h;}
    private void label(GuiGraphicsExtractor g,Component text,int x,int y,int maxWidth,int color,boolean shadow,int mx,int my){
        String full=text.getString();boolean clipped=font.width(full)>maxWidth;
        g.text(font,clipped?font.plainSubstrByWidth(full,maxWidth-font.width("..."))+"...":full,x,y,color,shadow);
        if(clipped&&inside(mx,my,x,y,maxWidth,font.lineHeight))g.setTooltipForNextFrame(text,mx,my);
    }
    private void sound(){if(minecraft.player!=null)minecraft.player.playSound(dev.thaumcraft.content.ModSounds.event("page"),1,1);}
    @Override public boolean isPauseScreen(){return false;}
    @Override public boolean keyPressed(KeyEvent event){if(minecraft.options.keyInventory.matches(event)){onClose();return true;}return super.keyPressed(event);}
    @Override public boolean mouseClicked(MouseButtonEvent event,boolean doubleClick){
        if(event.button()!=0&&!crystal)return super.mouseClicked(event,doubleClick);
        int x=(width-256)/2,y=(height-256)/2;
        if(crystal){for(int col=0;col<3;col++)for(int rune=col==0?0:-1;rune<6;rune++)if(inside(event.x(),event.y(),13+col*20,height/2-64+rune*20,20,20)){runes[col]=rune;minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(),1,.3f));return true;}}
        else {
            if(tome){for(int offset=0;offset<visibleCategories()&&categoryOffset+offset<catalog.categories().size();offset++)if(inside(event.x(),event.y(),5,categoryY(offset),80,18)){category=categoryOffset+offset;refresh();sound();return true;}
                if(inside(event.x(),event.y(),x+16,y+120,16,16)){index=Math.max(0,index-1);recipeIndex=0;textOffset=0;sound();return true;}
                if(inside(event.x(),event.y(),x+224,y+120,16,16)){index=Math.min(projects.size()-1,index+1);recipeIndex=0;textOffset=0;sound();return true;}}
            if(!projects.isEmpty()&&inside(event.x(),event.y(),x+88,y+236,80,14)){recipeIndex++;textOffset=0;sound();return true;}
        }
        return super.mouseClicked(event,doubleClick);
    }
    @Override public boolean mouseScrolled(double mx,double my,double dx,double dy){if(tome&&mx<100){categoryOffset=Math.clamp(categoryOffset-(int)dy,0,Math.max(0,catalog.categories().size()-visibleCategories()));return true;}if(!crystal){textOffset=Math.max(0,textOffset-(int)dy*3);return true;}return false;}
    @Override public void onClose(){if(tome){lastCategory=category;lastIndex=index;}super.onClose();}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float partial){
        if(catalog!=dev.thaumcraft.gameplay.AddonData.current()){String previous=catalog.categories().get(category).id();catalog=dev.thaumcraft.gameplay.AddonData.current();category=0;for(int i=0;i<catalog.categories().size();i++)if(catalog.categories().get(i).id().equals(previous))category=i;categoryOffset=Math.min(category,Math.max(0,catalog.categories().size()-visibleCategories()));if(tome)refresh();else projects=projects.stream().map(p->GameData.findProject(p.key())).flatMap(java.util.Optional::stream).toList();}
        if(tome&&!known.equals(dev.thaumcraft.gameplay.ClientResearch.known())){known.clear();known.addAll(dev.thaumcraft.gameplay.ClientResearch.known());refresh();}
        super.extractRenderState(g,mx,my,partial);int x=(width-256)/2,y=(height-256)/2;
        if(crystal){crystal(g,x,y);return;}
        if(tome){blit(g,"guiresearchbook",x,y,0,0,256,256);for(int offset=0;offset<visibleCategories()&&categoryOffset+offset<catalog.categories().size();offset++){int a=categoryOffset+offset;blit(g,"particles",0,categoryY(offset),0,120,95,18);var name=catalog.categories().get(a).nameComponent();label(g,name,15,categoryY(offset)+5,78,a==category?0xfff0f0f0:0xff252525,a==category,mx,my);}}
        if(projects.isEmpty())return;
        var project=projects.get(index);boolean visible=known.contains(project.key());
        var crafts=GameData.crafts().stream().filter(r->r.research()==project.index()).toList();
        var infusions=GameData.infusions().stream().filter(r->r.research()==project.index()).toList();
        int recipeCount=crafts.size()+infusions.size(),selected=Math.floorMod(recipeIndex,Math.max(1,recipeCount));
        var craft=selected<crafts.size()?crafts.get(selected):null;
        var infusion=selected>=crafts.size()&&selected<recipeCount?infusions.get(selected-crafts.size()):null;
        String texture=!visible?"guiscrollblank":craft!=null?"guiscrollcraft":infusion!=null?(infusion.dark()?"guiscrollinfdark":"guiscrollinf"):"guiscrollench";
        blit(g,texture,x,y,0,0,256,256);
        int headingY=y+(infusion!=null?8:18);
        if(tome)g.text(font,(index+1)+"/"+projects.size(),x+184,headingY,0xff252525,false);
        if(!visible)return;
        label(g,project.nameComponent(),x+48,headingY,tome?132:160,0xfff0f0f0,true,mx,my);
        if(craft!=null){if(craft.shaped()){for(int row=0;row<craft.pattern().size();row++)for(int col=0;col<craft.pattern().get(row).length();col++)drawIngredient(g,craft.key().get(String.valueOf(craft.pattern().get(row).charAt(col))),x+80+col*24,y+48+row*24,mx,my);}
            else for(int i=0;i<craft.ingredients().size();i++)drawIngredient(g,craft.ingredients().get(i),x+80+i%3*24,y+48+i/3*24,mx,my);
            drawItem(g,craft.result().create(),x+168,y+72,mx,my);
        }else if(infusion!=null){int[][] positions=infusion.dark()?DARK:INFUSION;for(int i=0;i<Math.min(positions.length,infusion.ingredients().size());i++)drawIngredient(g,infusion.ingredients().get(i),x+positions[i][0]-8,y+positions[i][1]-8,mx,my);
            drawItem(g,infusion.result().create(),x+120,y+(infusion.dark()?90:85),mx,my);
            var cost=infusion.dark()?Component.translatableWithFallback("gui.thaumcraft2tp.infusion.dark_cost", "%s Vis / %s Taint", infusion.cost()*2/3, infusion.cost()/3):Component.translatableWithFallback("gui.thaumcraft2tp.infusion.cost", "%s Vis", infusion.cost());g.text(font,cost,width/2-font.width(cost)/2,height/2-20,0xfff0f0f0,true);
        }
        var lines=font.split(project.descriptionComponent(),craft==null&&infusion==null?156:164);
        int textY=craft==null&&infusion==null?Math.max(y+40,height/2-lines.size()*font.lineHeight/2):height/2+42;
        int maxLines=Math.max(1,(y+232-textY)/font.lineHeight);textOffset=Math.min(textOffset,Math.max(0,lines.size()-maxLines));
        for(int i=textOffset;i<Math.min(lines.size(),textOffset+maxLines);i++)g.text(font,lines.get(i),width/2-82,textY+(i-textOffset)*font.lineHeight,0xff252525,false);
        if(lines.size()>maxLines)g.text(font,Component.translatableWithFallback("gui.thaumcraft2tp.research.scroll_hint", "Scroll to read"),x+88,y+226,0xff66512a,false);
        if(recipeCount>1)g.text(font,Component.translatableWithFallback("gui.thaumcraft2tp.research.recipe", "Recipe %s/%s  >", selected+1, recipeCount),x+88,y+238,0xff66512a,false);
    }
    private void drawIngredient(GuiGraphicsExtractor g,String id,int x,int y,int mx,int my){
        if(id==null||id.isBlank())return;ItemStack item;
        if(id.startsWith("#")){var tag=TagKey.create(Registries.ITEM,Identifier.parse(id.substring(1)));var holders=BuiltInRegistries.ITEM.get(tag);if(holders.isEmpty()||holders.get().size()==0)return;int tick=minecraft.player==null?0:minecraft.player.tickCount;item=new ItemStack(holders.get().get(Math.floorMod(tick/20,holders.get().size())));}
        else item=new GameData.StackDef(id,1).create();drawItem(g,item,x,y,mx,my);
    }
    private void drawItem(GuiGraphicsExtractor g,ItemStack item,int x,int y,int mx,int my){g.item(item,x,y);g.itemDecorations(font,item,x,y);if(inside(mx,my,x,y,16,16))g.setTooltipForNextFrame(font,item,mx,my);}
    private void crystal(GuiGraphicsExtractor g,int x,int y){
        blit(g,"guicrystalball",x,y,0,0,256,256);
        for(int col=0;col<3;col++){blit(g,"items",15+col*20,height/2-98,224,16,16,16);g.text(font,Integer.toString(col+1),20+col*20,height/2-94,0xfff0f0f0,true);
            for(int rune=col==0?0:-1;rune<6;rune++){int yy=height/2-60+rune*20;if(runes[col]==rune)blit(g,"items",15+col*20,yy,224,16,16,16);blit(g,"items",15+col*20,yy,128+16*rune,16,16,16);}}
        String key=runes[0]+","+runes[1]+","+runes[2],description=SealLogic.description(key);
        var text=description==null||description.isBlank()?Component.translatableWithFallback("gui.thaumcraft2tp.seal.no_effect", "This seal has no effect."):!seals.contains(key)?Component.translatableWithFallback("gui.thaumcraft2tp.seal.unknown", "You do not yet know what this combination of seals do."):Component.translatableWithFallback("seal.thaumcraft2tp.combination."+key.replace("-1","none").replace(",","_"),"This seal "+description);
        var lines=font.split(text,180);int yy=height/2-lines.size()*font.lineHeight/2;
        for(var line:lines){g.text(font,line,width/2-90,yy,0xfff0f0f0,true);yy+=font.lineHeight;}
    }
}
