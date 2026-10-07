package dev.thaumcraft.world;

import java.util.Random;

public abstract class TreeGenerator {
    private final boolean notify;
    protected TreeGenerator(boolean notify) {this.notify=notify;}
    protected void setBlockAndMetadata(TreeWorld world,int x,int y,int z,int id,int metadata) {world.setBlock(x,y,z,id,metadata,notify);}
    protected void setBlock(TreeWorld world,int x,int y,int z,int id) {world.setBlock(x,y,z,id,0,notify);}
    public abstract boolean generate(TreeWorld world,Random random,int x,int y,int z);
}
