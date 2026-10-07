package dev.thaumcraft.client;

import dev.thaumcraft.Thaumcraft;
import dev.thaumcraft.machine.MachineMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.model.object.book.BookModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.enchantment.Enchantment;

/** The original textures, controls and coordinates, driven exclusively by synchronized menu state. */
public final class MachineScreen extends AbstractContainerScreen<MachineMenu> {
    private final Identifier texture;
    private BookModel book;
    private final java.util.Random bookRandom=new java.util.Random();
    private net.minecraft.world.item.ItemStack bookItem=net.minecraft.world.item.ItemStack.EMPTY;
    private float bookOpen,previousBookOpen,bookFlip,previousBookFlip,bookFlipTarget,bookFlipSpeed;
    private long enchantmentNameSeed;
    public MachineScreen(MachineMenu menu,Inventory inventory,Component title){
        super(menu,inventory,title,menu.layout.width(),menu.layout.height());texture=Thaumcraft.id("textures/legacy/"+menu.layout.texture()+".png");inventoryLabelY=menu.layout.playerY()-12;
    }
    @Override protected void init(){super.init();if(menu.machineId().equals("thaumic_enchanter"))book=new BookModel(minecraft.getEntityModels().bakeLayer(ModelLayers.BOOK));}
    @Override public void containerTick(){
        super.containerTick();
        if(!menu.machineId().equals("thaumic_enchanter"))return;
        var item=menu.getSlot(0).getItem();
        if(!net.minecraft.world.item.ItemStack.matches(item,bookItem)){
            bookItem=item.copy();enchantmentNameSeed=bookRandom.nextLong();
            do{bookFlipTarget+=bookRandom.nextInt(4)-bookRandom.nextInt(4);}while(Math.abs(bookFlip-bookFlipTarget)<=1);
        }
        previousBookOpen=bookOpen;previousBookFlip=bookFlip;
        boolean offers=menu.status(32)>0||menu.status(33)>0||menu.status(34)>0;
        bookOpen=Math.clamp(bookOpen+(offers?.2f:-.2f),0,1);
        float speed=Math.clamp((bookFlipTarget-bookFlip)*.4f,-.2f,.2f);
        bookFlipSpeed+=(speed-bookFlipSpeed)*.9f;bookFlip+=bookFlipSpeed;
    }
    private void blit(GuiGraphicsExtractor g,int x,int y,int u,int v,int w,int h){if(w>0&&h>0)g.blit(RenderPipelines.GUI_TEXTURED,texture,leftPos+x,topPos+y,u,v,w,h,256,256);}
    private int scaled(int value,int max,int pixels){return Math.clamp((int)((long)value*pixels/Math.max(1,max)),0,pixels);}
    private int progress(int pixels){return scaled(menu.progress(),menu.required(),pixels);}
    private void upgrade(GuiGraphicsExtractor g,int slot,int x,int y){int meta=menu.status(28+slot)-1;if(meta>=0)g.blit(RenderPipelines.GUI_TEXTURED,Thaumcraft.id("textures/legacy/items.png"),leftPos+x,topPos+y,meta*16,32,16,16,256,256);}
    private boolean inside(double x,double y,int left,int top,int w,int h){return x>=leftPos+left&&x<leftPos+left+w&&y>=topPos+top&&y<topPos+top+h;}
    private boolean send(int action){if(minecraft.gameMode==null)return false;minecraft.gameMode.handleInventoryButtonClick(menu.containerId,action);return true;}
    @Override public boolean mouseClicked(MouseButtonEvent event,boolean doubleClick){
        if(event.button()==0){
            if(menu.machineId().equals("thaumic_duplicator")&&inside(event.x(),event.y(),62,48,10,10))return send(0);
            if(menu.machineId().equals("thaumic_enchanter")&&menu.status(35)==0)for(int i=0;i<3;i++)if(menu.status(32+i)>0&&inside(event.x(),event.y(),60,22+19*i,108,19))return send(i);
            if(menu.machineId().equals("occultic_enchanter")&&menu.enchantmentData(13)==0){
                if(inside(event.x(),event.y(),45,96,16,16)&&menu.enchantmentData(15)>0)return send(3);
                if(inside(event.x(),event.y(),177,8,8,8))return send(0);
                if(inside(event.x(),event.y(),177,64,8,8))return send(1);
                for(int i=0;i<40;i++)if(menu.candidateData(2*i)>0&&inside(event.x(),event.y(),8+i%10*16,8+i/10*16,16,16))return send(100+i);
                int shown=0;for(int i=0;i<4;i++)if(menu.enchantmentData(2+i)>0){if(inside(event.x(),event.y(),64+shown*16,77,16,16))return send(4+i);shown++;}
            }
        }
        return super.mouseClicked(event,doubleClick);
    }
    @Override protected boolean hasClickedOutside(double x,double y,int left,int top){return x<left||y<top||x>=left+imageWidth+(menu.machineId().equals("occultic_enchanter")?9:0)||y>=top+imageHeight;}
    @Override public void extractBackground(GuiGraphicsExtractor g,int mouseX,int mouseY,float partial){
        super.extractBackground(g,mouseX,mouseY,partial);
        if(menu.layout.texture().isEmpty()){
            g.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,0xffc5b9a1);
            for(var slot:menu.slots)g.fill(leftPos+slot.x-1,topPos+slot.y-1,leftPos+slot.x+17,topPos+slot.y+17,0xff807569);return;
        }
        blit(g,0,0,0,0,imageWidth,imageHeight);
        int q;
        switch(menu.machineId()){
            case "quaesitum" -> {
                blit(g,53,69,176,0,progress(50),10);
                for(int i=0;i<3;i++){int odds=menu.status(120+i);if(i!=1||menu.status(123)>=0)blit(g,81,28+i*12,176,40,Math.clamp((odds+5)/20*4,0,20),4);if(inside(mouseX,mouseY,67,25+i*12,34,10))g.setTooltipForNextFrame(Component.translatable(new String[]{"gui.thaumcraft2tp.research.success","gui.thaumcraft2tp.research.failure","gui.thaumcraft2tp.research.loss"}[i],odds),mouseX,mouseY);}
                if(menu.status(123)>=0){blit(g,12,24,190,14,8,20);int required=dev.thaumcraft.gameplay.ResearchItemData.project(menu.getSlot(0).getItem()).map(dev.thaumcraft.gameplay.GameData.Project::steps).orElse(5);int steps=scaled(menu.status(123),Math.max(1,required-1),16);blit(g,14,42-steps,176,32-steps,4,steps);}
            }
            case "arcane_furnace" -> {q=scaled(menu.status(24),menu.status(25),16);blit(g,80,48-q,menu.status(36)>0?221:176,16-q,16,q);blit(g,75,22,176,28,progress(26),4);}
            case "arcane_bore" -> {q=scaled(menu.storedEnergy(),250,46);blit(g,103,66-q,176,46-q,9,q>0?q+1:0);}
            case "darkness_generator" -> {blit(g,56,64,192,72,scaled(menu.progress(),90000,63),8);blit(g,80,24,176,menu.status(124)*16,16,16);blit(g,84,44,200,menu.status(30)*8,8,8);}
            case "vis_condenser" -> {q=scaled(menu.status(26),4550,35);blit(g,78,58-q,176,35-q,20,q);blit(g,84,68,200,menu.status(30)*8,8,8);upgrade(g,0,56,56);upgrade(g,1,104,56);}
            case "thaumic_crystalizer","thaumic_infuser" -> {boost(g,161,38,192);q=progress(46);blit(g,160,151-q,176,46-q,9,q);upgrade(g,0,8,128);}
            case "dark_infuser" -> {q=progress(46);blit(g,158,151-q,176,46-q,6,q);q=scaled(menu.status(39),1000,46);blit(g,164,151-q,182,46-q,6,q);blit(g,160,8,192,menu.status(30)*8,8,8);upgrade(g,0,8,128);}
            case "thaumic_duplicator" -> {boost(g,157,45,208);blit(g,54,34,176,15,menu.progress()>0?progress(25)+1:0,16);blit(g,62,48,menu.status(27)>0?176:186,0,10,10);upgrade(g,0,152,60);if(inside(mouseX,mouseY,62,48,10,10))g.setTooltipForNextFrame(menu.status(27)>0?Component.translatableWithFallback("gui.thaumcraft2tp.duplicator.repeat", "Repeat: keep the template"):Component.translatableWithFallback("gui.thaumcraft2tp.duplicator.once", "Duplicate once: move both copies to output"),mouseX,mouseY);}
            case "thaumic_restorer" -> {boost(g,135,46,208);upgrade(g,0,130,54);}
            case "brazier_of_souls" -> {q=scaled(menu.status(24),6000,16);blit(g,80,35-q,176,16-q,16,q);blit(g,84,66,200,menu.status(30)*8,8,8);}
            case "thaumic_generator" -> {if(inside(mouseX,mouseY,84,22,9,46))g.setTooltipForNextFrame(Component.translatableWithFallback("gui.thaumcraft2tp.generator.energy", "%s / %s FE", menu.storedEnergy(), menu.status(31)),mouseX,mouseY);q=scaled(menu.storedEnergy(),menu.status(31),46);blit(g,84,68-q,176,46-q,9,q);blit(g,108,41,192,menu.status(30)*8,8,8);upgrade(g,0,56,25);upgrade(g,1,56,47);}
            case "thaumic_enchanter" -> basic(g,mouseX,mouseY,partial);
            case "occultic_enchanter" -> advanced(g,mouseX,mouseY);
        }
    }
    private void boost(GuiGraphicsExtractor g,int x,int bottom,int u){int pixels=Math.clamp(Math.round(.1f+menu.status(38)/2f)*6,0,30);blit(g,x,bottom-pixels,u,30-pixels,7,pixels);}
    private void basic(GuiGraphicsExtractor g,int mouseX,int mouseY,float partial){
        blit(g,21,71,176,0,progress(25),3);
        float open=net.minecraft.util.Mth.lerp(partial,previousBookOpen,bookOpen);
        float flip=net.minecraft.util.Mth.lerp(partial,previousBookFlip,bookFlip);
        g.book(book,Thaumcraft.id("textures/legacy/book.png"),40,open,flip,leftPos+14,topPos+17,leftPos+52,topPos+44);
        var names=net.minecraft.client.gui.screens.inventory.EnchantmentNames.getInstance();
        names.initSeed(enchantmentNameSeed);
        for(int i=0;i<3;i++){
            var name=names.getRandomName(font,208);
            int level=menu.status(32+i),cost=menu.status(40+i);
            if(cost<=0)continue;
            boolean hover=inside(mouseX,mouseY,60,22+19*i,108,19);
            boolean selected=menu.status(35)==i+1&&menu.progress()<menu.required();
            blit(g,60,22+19*i,0,selected?201:hover?220:182,108,19);
            g.textWithWordWrap(font,name,leftPos+62,topPos+24+19*i,104,hover?0xffffff80:0xff685e4a,false);
            String strength=Integer.toString(level),price=Integer.toString(cost);
            g.text(font,strength,leftPos+166-font.width(strength),topPos+23+19*i,0xff80ff20,true);
            g.text(font,price,leftPos+166-font.width(price),topPos+32+19*i,0xffa43fff,true);
        }
    }
    private void advanced(GuiGraphicsExtractor g,int mx,int my){
        blit(g,64,100,0,200,scaled(menu.progress(),menu.enchantmentData(10),104),7);
        blit(g,139,77,176,0,scaled(menu.enchantmentData(11),menu.enchantmentData(12),menu.status(28)==6?28:24),5);
        if(menu.status(28)==6)blit(g,162,76,176,72,6,6);
        if(menu.status(28)==7)blit(g,96,76,176,80,33,18);
        if(menu.enchantmentData(13)>0||menu.enchantmentData(15)>0)blit(g,44,95,176,menu.enchantmentData(13)>0?26:8,17,17);
        if(menu.status(37)>0)blit(g,177,7,176,48,8,8);
        if(menu.enchantmentData(14)>menu.status(37)*10+40)blit(g,177,63,176,56,8,8);
        if(menu.enchantmentData(13)==0)for(int i=0;i<40;i++)glyph(g,menu.candidateData(2*i),menu.candidateData(2*i+1),8+i%10*16,8+i/10*16,mx,my);
        int shown=0;for(int i=0;i<4;i++)if(menu.enchantmentData(2+i)>0)glyph(g,menu.enchantmentData(2+i),menu.enchantmentData(6+i),64+16*shown++,77,mx,my);
        if(menu.enchantmentData(15)>0){String cost=menu.enchantmentData(10)+"";g.text(font,cost,leftPos+152-font.width(cost)/2,topPos+86,0xff700070,false);}
        upgrade(g,0,8,96);
        if(inside(mx,my,139,77,28,8))g.setTooltipForNextFrame(Component.translatableWithFallback("gui.thaumcraft2tp.enchanter.power", "Enchantment power: %s / %s", menu.enchantmentData(11), menu.enchantmentData(12)),mx,my);
    }
    private void glyph(GuiGraphicsExtractor g,int id,int level,int x,int y,int mx,int my){
        if(id==0||minecraft.level==null)return;
        var holder=minecraft.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(id-1);if(holder.isEmpty())return;
        String name=holder.get().key().identifier().getPath();int icon=switch(name){
            case "protection"->0;case "fire_protection"->1;case "feather_falling"->2;case "blast_protection"->3;case "projectile_protection"->4;case "respiration"->5;case "aqua_affinity"->6;
            case "sharpness"->7;case "smite"->8;case "bane_of_arthropods"->9;case "knockback"->10;case "fire_aspect"->11;case "looting"->12;case "efficiency"->13;case "silk_touch"->14;case "unbreaking"->15;case "fortune"->16;case "power"->17;case "punch"->18;case "flame"->19;case "infinity"->20;
            case "vampiric"->40;case "soulstealer"->41;case "ice"->42;case "venom"->43;case "swimming"->44;case "repair"->45;case "relic"->46;case "striding"->47;case "potency"->48;default->63;
        };
        var icons=Thaumcraft.id("textures/legacy/enchantments.png");
        g.blit(RenderPipelines.GUI_TEXTURED,icons,leftPos+x,topPos+y,icon%8*32,icon/8*32,16,16,32,32,256,256);
        g.blit(RenderPipelines.GUI_TEXTURED,icons,leftPos+x,topPos+y,(level-1)%8*32,224,16,16,32,32,256,256);
        if(inside(mx,my,x,y,16,16))g.setTooltipForNextFrame(Enchantment.getFullname(holder.get(),level),mx,my);
    }
    @Override protected void extractLabels(GuiGraphicsExtractor g,int mouseX,int mouseY){
        if(menu.machineId().equals("occultic_enchanter"))return;
        if(menu.machineId().equals("thaumic_enchanter")){
            label(g,Component.translatableWithFallback("gui.thaumcraft2tp.machine.enchanter", "Thaumic Enchanter"),12,6,imageWidth-24,0xff404040,mouseX,mouseY);
            g.text(font,playerInventoryTitle,8,imageHeight-94,0xff404040,false);
            return;
        }
        if(menu.machineId().equals("darkness_generator")){
            label(g,Component.translatableWithFallback("gui.thaumcraft2tp.machine.dark_generator", "Dark Generator"),50,7,imageWidth-58,0xff101010,mouseX,mouseY);
            return;
        }
        if(menu.machineId().equals("vis_condenser")||menu.machineId().equals("arcane_bore")){
            String name=font.plainSubstrByWidth(title.getString(),imageWidth-16);
            g.text(font,name,(imageWidth-font.width(name))/2,5,0xff404040,false);
            if(font.width(title)>imageWidth-16&&inside(mouseX,mouseY,8,5,imageWidth-16,font.lineHeight))g.setTooltipForNextFrame(title,mouseX,mouseY);
            return;
        }
        if(menu.machineId().equals("brazier_of_souls")){label(g,Component.translatableWithFallback("gui.thaumcraft2tp.machine.soul_brazier", "Soul Brazier"),57,5,imageWidth-65,0xfff0f0f0,mouseX,mouseY);return;}
        Component name=switch(menu.machineId()){
            case "arcane_furnace" -> Component.translatableWithFallback("gui.thaumcraft2tp.machine.furnace", "Thaumic Furnace");
            case "thaumic_crystalizer" -> Component.translatableWithFallback("gui.thaumcraft2tp.machine.crystalizer", "Crystalyzer");
            case "thaumic_duplicator" -> Component.translatableWithFallback("gui.thaumcraft2tp.machine.duplicator", "Duplicator");
            case "thaumic_restorer" -> Component.translatableWithFallback("gui.thaumcraft2tp.machine.restorer", "Restorer");
            default -> title;
        };
        boolean restorer=menu.machineId().equals("thaumic_restorer");
        int labelX=restorer?30:8;
        int maxWidth=imageWidth-labelX-8;
        int nameWidth=Math.min(font.width(name),maxWidth);
        label(g,name,menu.machineId().equals("arcane_furnace")?(imageWidth-nameWidth)/2:menu.machineId().equals("thaumic_generator")?(176-nameWidth)/2:labelX,5,maxWidth,0xff404040,mouseX,mouseY);
        if(!menu.machineId().equals("quaesitum")&&menu.layout.playerY()>=0&&menu.layout.height()<239)g.text(font,playerInventoryTitle,labelX,restorer?imageHeight-94:menu.layout.playerY()-12,0xff404040,false);
    }
    private void label(GuiGraphicsExtractor g,Component text,int x,int y,int maxWidth,int color,int mx,int my){
        String full=text.getString();boolean clipped=font.width(full)>maxWidth;
        g.text(font,clipped?font.plainSubstrByWidth(full,maxWidth-font.width("..."))+"...":full,x,y,color,false);
        if(clipped&&inside(mx,my,x,y,maxWidth,font.lineHeight))g.setTooltipForNextFrame(text,mx,my);
    }
}
