package dev.thaumcraft.client;

import dev.thaumcraft.Thaumcraft;
import dev.thaumcraft.machine.TrunkMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class TrunkScreen extends AbstractContainerScreen<TrunkMenu> {
    public TrunkScreen(TrunkMenu menu,Inventory inventory,Component title){super(menu,inventory,title,176,190);}
    @Override protected void init(){super.init();topPos=(height-166)/2;}
    @Override protected boolean hasClickedOutside(double x,double y,int left,int top){return x<left||x>=left+176||y<top-6||y>=top+184;}
    @Override public boolean mouseClicked(MouseButtonEvent event,boolean doubleClick){
        if(event.button()==0&&event.x()>=leftPos+159&&event.x()<leftPos+169&&event.y()>=topPos+88&&event.y()<topPos+98&&minecraft.gameMode!=null){minecraft.gameMode.handleInventoryButtonClick(menu.containerId,0);return true;}
        return super.mouseClicked(event,doubleClick);
    }
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float partial){
        super.extractBackground(g,mx,my,partial);var texture=Thaumcraft.id("textures/legacy/guitrunkbase.png");
        g.blit(RenderPipelines.GUI_TEXTURED,texture,leftPos,topPos-6,0,0,176,190,256,256);
        int hp=Math.clamp(Math.round(menu.status(0)*39f/Math.max(1,menu.status(1))),0,39);
        if(hp>0)g.blit(RenderPipelines.GUI_TEXTURED,texture,leftPos+130,topPos,176,16,hp,6,256,256);
        g.blit(RenderPipelines.GUI_TEXTURED,Thaumcraft.id("textures/legacy/guitrunkslots.png"),leftPos,topPos-6,0,0,176,menu.rows*18+16,256,256);
        g.blit(RenderPipelines.GUI_TEXTURED,texture,leftPos+159,topPos+88,menu.status(2)>0?186:176,0,10,10,256,256);
        for(int slot=0;slot<2;slot++){int meta=menu.status(4+slot);if(meta>=0&&(menu.status(3)&1<<meta)!=0)g.blit(RenderPipelines.GUI_TEXTURED,Thaumcraft.id("textures/legacy/items.png"),leftPos+98+22*slot,topPos+85,meta*16,32,16,16,256,256);}
        if(mx>=leftPos+159&&mx<leftPos+169&&my>=topPos+88&&my<topPos+98)g.setTooltipForNextFrame(menu.status(2)>0?Component.translatableWithFallback("gui.thaumcraft2tp.trunk.stay", "Stay here"):Component.translatableWithFallback("gui.thaumcraft2tp.trunk.follow", "Follow owner"),mx,my);
    }
    @Override protected void extractLabels(GuiGraphicsExtractor g,int mx,int my){g.text(font,Component.translatableWithFallback("gui.thaumcraft2tp.trunk.title", "Trunk"),8,0,0xff101010,false);g.text(font,playerInventoryTitle,8,90,0xff101010,false);}
}
