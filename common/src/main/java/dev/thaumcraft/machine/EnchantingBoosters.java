package dev.thaumcraft.machine;

import dev.thaumcraft.gameplay.GameData;
import java.util.ArrayList;
import java.util.Comparator;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EnchantingTableBlock;

/** Booster blocks come from the catalog's booster rules; the built-ins are the bookshelf and the brain in a jar. */
public final class EnchantingBoosters {
    private EnchantingBoosters() {}
    public record Totals(int enchanting,int researchSpeed,int researchBonus,int failureProtection) {}
    public static Totals count(Level level,BlockPos origin){
        double[] total=new double[4];
        for(int z=-1;z<=1;z++)for(int x=-1;x<=1;x++){
            if(x==0&&z==0||!level.isEmptyBlock(origin.offset(x,0,z))||!level.isEmptyBlock(origin.offset(x,1,z)))continue;
            for(int y=0;y<=1;y++){
                add(level,origin.offset(x*2,y,z*2),total);
                if(x!=0&&z!=0){add(level,origin.offset(x*2,y,z),total);add(level,origin.offset(x,y,z*2),total);}
            }
        }
        return totals(total,1,1);
    }
    /** Most boosters a Quaesitum counts. */
    public static final int RESEARCH_LIMIT=16;
    /** Multiplier on each counted booster's research speed. */
    public static final int RESEARCH_SPEED_MULTIPLIER=3;
    /** Multiplier on each counted booster's research bonus and failure protection. */
    public static final int RESEARCH_MULTIPLIER=2;
    /**
     * Quaesitum boosters use the vanilla enchanting table's sight rule: a block counts only when the block halfway
     * toward it is an enchantment power transmitter. The strongest {@value #RESEARCH_LIMIT} visible boosters count.
     */
    public static Totals research(Level level,BlockPos origin){
        var visible=new ArrayList<GameData.Booster>();
        for(var offset:EnchantingTableBlock.BOOKSHELF_OFFSETS){
            if(!level.getBlockState(origin.offset(offset.getX()/2,offset.getY(),offset.getZ()/2)).is(BlockTags.ENCHANTMENT_POWER_TRANSMITTER))continue;
            var booster=GameData.booster(level.getBlockState(origin.offset(offset)));if(booster!=null)visible.add(booster);
        }
        visible.sort(Comparator.comparingDouble((GameData.Booster b)->b.researchSpeed()+b.researchBonus()+b.failureProtection()).reversed());
        double[] total=new double[4];
        for(var booster:visible.subList(0,Math.min(RESEARCH_LIMIT,visible.size())))add(booster,total);
        return totals(total,RESEARCH_SPEED_MULTIPLIER,RESEARCH_MULTIPLIER);
    }
    /** Research speed is scaled by {@code speedMultiplier} and the other research values by {@code multiplier}; fractional failure protection rounds to whole percent. */
    private static Totals totals(double[] total,int speedMultiplier,int multiplier){
        return new Totals((int)total[0],(int)total[1]*speedMultiplier,(int)total[2]*multiplier,(int)Math.round(total[3]*multiplier));
    }
    private static void add(Level level,BlockPos pos,double[] total){
        var booster=GameData.booster(level.getBlockState(pos));if(booster!=null)add(booster,total);
    }
    private static void add(GameData.Booster booster,double[] total){
        total[0]+=booster.enchanting();total[1]+=booster.researchSpeed();total[2]+=booster.researchBonus();total[3]+=booster.failureProtection();
    }
    public static int power(MachineBlockEntity machine){return count(machine.getLevel(),machine.getBlockPos()).enchanting();}
}
