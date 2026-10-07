package dev.thaumcraft.machine;

import dev.thaumcraft.content.Content;
import dev.thaumcraft.entity.TravelingTrunk;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public final class TrunkMenu extends AbstractContainerMenu {
    private final Container source;
    private final ContainerData data;
    public final int rows;
    public TrunkMenu(int id,Inventory inventory,int rows){this(id,inventory,new SimpleContainer(rows*9),rows);}
    public TrunkMenu(int id,Inventory inventory,Container source,int rows){
        super(rows==4?Content.ROOMY_TRUNK_MENU:Content.TRUNK_MENU,id);this.source=source;this.rows=rows;checkContainerSize(source,rows*9);
        data=source instanceof TravelingTrunk trunk?new ContainerData(){
            public int get(int index){return switch(index){case 0->Math.round(trunk.getHealth()*10);case 1->Math.round(trunk.getMaxHealth()*10);case 2->trunk.staying()?1:0;case 3->trunk.upgradeMask();case 4->trunk.upgradeAt(0);case 5->trunk.upgradeAt(1);default->0;};}
            public void set(int index,int value){}public int getCount(){return 6;}
        }:new SimpleContainerData(6);
        for(int r=0;r<rows;r++)for(int c=0;c<9;c++)addSlot(new Slot(source,c+r*9,8+c*18,11+r*18){@Override public boolean mayPlace(ItemStack stack){return source.canPlaceItem(getContainerSlot(),stack);}});
        addStandardInventorySlots(inventory,8,104);addDataSlots(data);source.startOpen(inventory.player);
    }
    public int status(int index){return data.get(index);}
    @Override public boolean clickMenuButton(Player player,int button){if(button!=0||!(source instanceof TravelingTrunk trunk)||!stillValid(player))return false;trunk.toggleStay(player);broadcastChanges();return true;}
    @Override public boolean stillValid(Player player){return source.getContainerSize()==rows*9&&source.stillValid(player);}
    @Override public void removed(Player player){super.removed(player);source.stopOpen(player);}
    @Override public ItemStack quickMoveStack(Player player,int index){
        if(index<0||index>=slots.size())return ItemStack.EMPTY;Slot slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;
        ItemStack stack=slot.getItem(),copy=stack.copy();int size=rows*9;
        if(index<size){if(!moveItemStackTo(stack,size,slots.size(),true))return ItemStack.EMPTY;}else if(!moveItemStackTo(stack,0,size,false))return ItemStack.EMPTY;
        if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();if(stack.getCount()==copy.getCount())return ItemStack.EMPTY;slot.onTake(player,stack);return copy;
    }
}
