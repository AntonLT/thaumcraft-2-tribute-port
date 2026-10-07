package dev.thaumcraft.machine;

import dev.thaumcraft.content.Content;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public final class MachineMenu extends AbstractContainerMenu {
    public static final int DATA_COUNT=129;
    private final Container machine;
    private final ContainerData data;
    public final MachineLayout layout;
    private final int machineSlots;
    public MachineMenu(int id,Inventory inventory){this(id,inventory,"machine");}
    public MachineMenu(int id,Inventory inventory,String kind){this(id,inventory,new SimpleContainer(MachineBlockEntity.SIZE),new SimpleContainerData(DATA_COUNT),kind);}
    public MachineMenu(int id,Inventory inventory,Container machine,ContainerData data){this(id,inventory,machine,data,machine instanceof MachineBlockEntity entity?entity.machineId():"machine");}
    private MachineMenu(int id,Inventory inventory,Container machine,ContainerData data,String kind){
        super(Content.machineMenu(kind),id);this.machine=machine;this.data=data;layout=MachineLayout.get(kind);
        checkContainerSize(machine,MachineBlockEntity.SIZE);checkContainerDataCount(data,DATA_COUNT);
        for(var cell:layout.cells())addSlot(new Slot(machine,cell.slot(),cell.x(),cell.y()){
            @Override public boolean mayPlace(ItemStack stack){
                if(machine instanceof MachineBlockEntity entity)return !entity.reservedSlot(cell.slot())&&entity.canPlaceItem(cell.slot(),stack);
                return MachineInputs.accepts(kind,cell.slot(),stack,inventory.player.level());
            }
            @Override public boolean mayPickup(Player player){return !(machine instanceof MachineBlockEntity entity&&entity.reservedSlot(cell.slot()))&&super.mayPickup(player);}
            @Override public int getMaxStackSize(){return cell.slot()>=18&&cell.slot()!=MachineBlockEntity.FUEL_SLOT||kind.endsWith("enchanter")||kind.equals("arcane_bore")&&cell.slot()==0?1:super.getMaxStackSize();}
        });
        machineSlots=slots.size();if(layout.playerY()>=0)addStandardInventorySlots(inventory,8,layout.playerY());addDataSlots(data);
    }
    public float vis(){return data.get(0)/10f;}
    public float taint(){return data.get(1)/10f;}
    public float capacity(){return data.get(2)/10f;}
    public int progress(){return (data.get(3)&0xffff)|(data.get(125)<<16);}
    public int required(){return Math.max(1,(data.get(4)&0xffff)|(data.get(126)<<16));}
    public boolean enabled(){return data.get(5)!=0;}
    public int storedEnergy(){return machineId().equals("thaumic_generator")?(data.get(6)&0xffff)|(data.get(127)<<16):data.get(6);}
    public String machineId(){return layout.id();}
    public int enchantmentData(int index){return data.get(index+8);}
    public int status(int index){return index==31&&machineId().equals("thaumic_generator")?(data.get(index)&0xffff)|(data.get(128)<<16):data.get(index);}
    public int candidateData(int index){return data.get(40+index);}
    public int machineSlotCount(){return machineSlots;}
    @Override public boolean clickMenuButton(Player player,int button){
        if(!(machine instanceof MachineBlockEntity entity)||!entity.stillValid(player))return false;
        // A bridge's job depends on the mode it was accepted in.
        if(machineId().equals("thaumic_duplicator")&&button==0){if(entity.reservation()!=null)return false;entity.processes.repeat=!entity.processes.repeat;entity.setChanged();return true;}
        if(machineId().equals("thaumic_enchanter"))return entity.basicEnchanting.click(button);
        return machineId().equals("occultic_enchanter")&&entity.enchanting.click(button);
    }
    @Override public boolean stillValid(Player player){return machine.stillValid(player);}
    @Override public ItemStack quickMoveStack(Player player,int slotIndex){
        if(slotIndex<0||slotIndex>=slots.size())return ItemStack.EMPTY;
        Slot slot=slots.get(slotIndex);if(!slot.hasItem())return ItemStack.EMPTY;
        ItemStack source=slot.getItem(),copy=source.copy();
        if(slotIndex<machineSlots){if(!moveItemStackTo(source,machineSlots,slots.size(),true))return ItemStack.EMPTY;}
        else {
            // Fuel gets its own slot even when the same item can also be smelted.
            boolean moved=false;
            if(machineId().equals("arcane_furnace")&&MachineInputs.fuelTicks(source,player.level())>0){
                for(int i=0;i<machineSlots;i++)if(slots.get(i).getContainerSlot()==MachineBlockEntity.FUEL_SLOT){moved=moveItemStackTo(source,i,i+1,false);break;}
            }
            if(!source.isEmpty())moved=moveItemStackTo(source,0,machineSlots,false)||moved;
            if(!moved)return ItemStack.EMPTY;
        }
        if(source.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();
        if(source.getCount()==copy.getCount())return ItemStack.EMPTY;slot.onTake(player,source);return copy;
    }
}
