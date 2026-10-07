package dev.thaumcraft.machine;

import java.util.*;

/** Original container coordinates, mapped to the port's persistent inventory indices. */
public record MachineLayout(String id,String texture,int width,int height,int playerY,List<Cell> cells) {
    public record Cell(int slot,int x,int y) {}
    public static final Map<String,MachineLayout> ALL=new LinkedHashMap<>();
    static {
        var c=new ArrayList<Cell>();
        c.add(new Cell(0,24,26));c.add(new Cell(1,24,49));c.add(new Cell(2,24,71));c.add(new Cell(3,134,12));grid(c,9,3,3,116,35);put("quaesitum","guiresearch",176,178,97,c);
        c=new ArrayList<>();grid(c,0,3,3,17,17);c.add(new Cell(MachineBlockEntity.FUEL_SLOT,80,53));grid(c,9,3,3,107,17);put("arcane_furnace","guiarcanefurnace",176,166,84,c);
        put("arcane_bore","guiarcanebore",176,166,84,List.of(new Cell(0,65,17),new Cell(1,65,55)));
        put("vis_condenser","guicondenser",176,166,84,List.of(new Cell(0,46,32),new Cell(9,114,32)));
        put("darkness_generator","guidarkgen",176,166,84,List.of(new Cell(0,46,32),new Cell(9,114,32)));
        c=new ArrayList<>();c.add(new Cell(0,80,70));positions(c,9,new int[][]{{80,12},{131,41},{30,41},{30,100},{131,100},{80,129}});put("thaumic_crystalizer","guicrystalizer",176,239,158,c);
        c=new ArrayList<>();c.add(new Cell(0,23,34));grid(c,9,3,3,90,17);put("thaumic_duplicator","guiduplicator",176,166,84,c);
        c=new ArrayList<>();grid(c,0,2,3,37,16);grid(c,9,2,3,90,16);put("thaumic_restorer","guirepairer",176,166,84,c);
        c=new ArrayList<>();positions(c,0,new int[][]{{80,11},{28,102},{132,102},{50,55},{110,55},{80,106}});c.add(new Cell(9,80,72));c.add(new Cell(10,80,135));put("thaumic_infuser","guiinfuser",176,239,158,c);
        c=new ArrayList<>();positions(c,0,new int[][]{{80,16},{132,54},{111,118},{48,118},{25,54}});c.add(new Cell(9,80,72));put("dark_infuser","guidarkinfuser",176,239,158,c);
        put("thaumic_enchanter","guienchanter",176,182,100,List.of(new Cell(0,25,47)));
        put("occultic_enchanter","guienchanter2",176,198,116,List.of(new Cell(0,8,77)));
        put("brazier_of_souls","guisoulbrazier",176,166,84,List.of(new Cell(0,80,41)));
        put("thaumic_generator","guigenerator",176,82,-1,List.of());
    }
    private static void put(String id,String texture,int width,int height,int playerY,List<Cell> cells){ALL.put(id,new MachineLayout(id,texture,width,height,playerY,List.copyOf(cells)));}
    private static void positions(List<Cell> cells,int start,int[][] positions){for(int i=0;i<positions.length;i++)cells.add(new Cell(start+i,positions[i][0],positions[i][1]));}
    private static void grid(List<Cell> cells,int start,int columns,int rows,int x,int y){for(int r=0;r<rows;r++)for(int col=0;col<columns;col++)cells.add(new Cell(start+r*columns+col,x+18*col,y+18*r));}
    public static MachineLayout get(String id){
        if(ALL.containsKey(id))return ALL.get(id);
        var cells=new ArrayList<Cell>();grid(cells,0,9,3,8,30);
        return new MachineLayout(id,"",176,222,140,List.copyOf(cells));
    }
    public int inputs(){return (int)cells.stream().filter(c->c.slot<9).count();}
    public int outputEnd(){return cells.stream().filter(c->c.slot>=9&&c.slot<18).mapToInt(c->c.slot+1).max().orElse(18);}
}
