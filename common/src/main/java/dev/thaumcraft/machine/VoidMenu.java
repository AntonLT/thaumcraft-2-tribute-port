package dev.thaumcraft.machine;

import dev.thaumcraft.content.Content;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Each viewer has its own page; item movement always reaches the backing local chest. */
public final class VoidMenu extends AbstractContainerMenu {
    private final Container source;
    private final ContainerData data;
    private int page;

    public VoidMenu(int id,Inventory inventory) {this(id,inventory,new SimpleContainer(72));}
    public VoidMenu(int id,Inventory inventory,Container source) {
        super(Content.VOID_MENU,id);this.source=source;
        data=source instanceof MachineBlockEntity machine?new ContainerData() {
            @Override public int get(int index) {return switch(index){case 0->page();case 1->machine.voidPages();case 2->machine.channel();case 3->machine.machineId().equals("void_interface")?1:0;default->0;};}
            @Override public void set(int index,int value) {}
            @Override public int getCount(){return 4;}
        }:new SimpleContainerData(4);
        for(int row=0;row<8;row++)for(int column=0;column<9;column++) {
            int slot=row*9+column;
            addSlot(new Slot(source,slot,8+column*18,9+row*18) {
                private int target() {return source instanceof MachineBlockEntity?page()*72+slot:slot;}
                @Override public ItemStack getItem(){return source.getItem(target());}
                @Override public void set(ItemStack stack){source.setItem(target(),stack);setChanged();}
                @Override public ItemStack remove(int amount){return source.removeItem(target(),amount);}
                @Override public boolean mayPlace(ItemStack stack){return source.canPlaceItem(target(),stack);}
            });
        }
        addStandardInventorySlots(inventory,8,158);addDataSlots(data);source.startOpen(inventory.player);
        if(source instanceof MachineBlockEntity machine&&machine.getLevel() instanceof net.minecraft.server.level.ServerLevel level)
            dev.thaumcraft.content.ModSounds.play(level,machine.getBlockPos(),machine.machineId().equals("void_interface")?"heal":"stoneopen",net.minecraft.sounds.SoundSource.BLOCKS,1,machine.machineId().equals("void_interface")?1.4f:1);
    }
    public int page() {
        if(source instanceof MachineBlockEntity machine)page=Math.clamp(page,0,Math.max(0,machine.voidPages()-1));
        else page=data.get(0);
        return page;
    }
    public int pages(){return data.get(1);}
    public int channel(){return data.get(2);}
    public boolean linkedInterface(){return data.get(3)!=0;}
    @Override public boolean clickMenuButton(Player player,int button) {
        if(!(source instanceof MachineBlockEntity machine)||!stillValid(player)||!machine.machineId().equals("void_interface"))return false;
        if(button==2||button==3){machine.setChannel(Math.floorMod(machine.channel()+(button==2?1:-1),6));page=0;broadcastChanges();return true;}
        if(button!=0&&button!=1||machine.voidPages()<2)return false;
        int next=Math.clamp(page()+(button==0?-1:1),0,machine.voidPages()-1);if(next==page)return false;
        page=next;broadcastChanges();return true;
    }
    @Override public boolean stillValid(Player player){return source.stillValid(player);}
    @Override public void removed(Player player){
        super.removed(player);source.stopOpen(player);
        if(source instanceof MachineBlockEntity machine&&machine.machineId().equals("void_chest")&&machine.getLevel() instanceof net.minecraft.server.level.ServerLevel level)
            dev.thaumcraft.content.ModSounds.play(level,machine.getBlockPos(),"stoneclose",net.minecraft.sounds.SoundSource.BLOCKS,1,1);
    }
    @Override public ItemStack quickMoveStack(Player player,int index) {
        if(index<0||index>=slots.size())return ItemStack.EMPTY;
        Slot slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;
        ItemStack sourceStack=slot.getItem(),copy=sourceStack.copy();
        if(index<72){if(!moveItemStackTo(sourceStack,72,slots.size(),true))return ItemStack.EMPTY;}
        else if(!moveItemStackTo(sourceStack,0,72,false))return ItemStack.EMPTY;
        if(sourceStack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();
        if(sourceStack.getCount()==copy.getCount())return ItemStack.EMPTY;
        slot.onTake(player,sourceStack);return copy;
    }
}
