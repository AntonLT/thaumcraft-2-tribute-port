package dev.thaumcraft.client;

import dev.thaumcraft.Thaumcraft;
import dev.thaumcraft.machine.VoidMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class VoidScreen extends AbstractContainerScreen<VoidMenu> {
    private boolean interfaceLayout;
    private int layoutWidth=176;
    public VoidScreen(VoidMenu menu,Inventory inventory,Component title){super(menu,inventory,title,176,239);}
    @Override protected void init(){interfaceLayout=menu.linkedInterface();layoutWidth=interfaceLayout?219:176;super.init();leftPos=(width-layoutWidth)/2;}
    @Override protected void containerTick(){super.containerTick();if(interfaceLayout!=menu.linkedInterface())init();}
    @Override protected boolean hasClickedOutside(double x,double y,int left,int top){return x<left||y<top||x>=left+layoutWidth||y>=top+imageHeight;}
    private boolean inside(double x,double y,int dx,int dy,int w,int h){return x>=leftPos+dx&&x<leftPos+dx+w&&y>=topPos+dy&&y<topPos+dy+h;}
    @Override public boolean mouseClicked(MouseButtonEvent event,boolean doubleClick){
        if(menu.linkedInterface()&&minecraft.gameMode!=null){
            int action=inside(event.x(),event.y(),178,24,32,32)?event.button()==0?2:3:inside(event.x(),event.y(),178,64,14,28)?0:inside(event.x(),event.y(),178,92,14,28)?1:-1;
            if(action>=0){minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(),action==0?.6f:.8f,.1f));minecraft.gameMode.handleInventoryButtonClick(menu.containerId,action);return true;}
        }
        return super.mouseClicked(event,doubleClick);
    }
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float partial){
        super.extractBackground(g,mx,my,partial);
        var texture=Thaumcraft.id("textures/legacy/"+(interfaceLayout?"guivoidinter":"guivoidchest")+".png");
        g.blit(RenderPipelines.GUI_TEXTURED,texture,leftPos,topPos,0,0,layoutWidth,imageHeight,256,256);
        if(interfaceLayout){g.blit(RenderPipelines.GUI_TEXTURED,texture,leftPos+178,topPos+24,224,menu.channel()*32,32,32,256,256);if(inside(mx,my,178,24,32,32))g.setTooltipForNextFrame(Component.translatableWithFallback("gui.thaumcraft2tp.void.channel", "Channel %s — left/right click to change", menu.channel()+1),mx,my);}
    }
    @Override protected void extractLabels(GuiGraphicsExtractor g,int mx,int my){if(interfaceLayout&&menu.pages()>1)g.text(font,(menu.page()+1)+"/"+menu.pages(),195,88,0xff704070,false);}
}
