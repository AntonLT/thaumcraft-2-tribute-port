package dev.thaumcraft.world;

import dev.thaumcraft.content.Content;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** The core alone owns the puzzle. Neighboring receptacles carry no saved state. */
public final class MonolithBlockEntity extends BlockEntity {
    public static final Direction[] SIDES={Direction.EAST,Direction.WEST,Direction.SOUTH,Direction.NORTH};
    private final byte[] targets=new byte[4],inserted={-1,-1,-1,-1};
    private int version;
    public MonolithBlockEntity(BlockPos pos,BlockState state){
        super(Content.MONOLITH_ENTITY,pos,state);
        for(int i=0;i<4;i++)targets[i]=(byte)EldritchBlock.rune(pos,i);
    }
    public int target(int index){return targets[index];}
    public int inserted(int index){return inserted[index];}
    public int index(BlockPos receptacle){for(int i=0;i<4;i++)if(worldPosition.relative(SIDES[i]).equals(receptacle))return i;return -1;}
    public static MonolithBlockEntity coreAt(Level level,BlockPos receptacle){
        MonolithBlockEntity found=null;
        for(Direction side:SIDES)if(level.hasChunkAt(receptacle.relative(side))&&level.getBlockEntity(receptacle.relative(side)) instanceof MonolithBlockEntity core){
            if(found!=null)return null; // An ambiguous, player-built arrangement has no owning core.
            found=core;
        }
        return found;
    }
    public void initialize(net.minecraft.util.RandomSource random){
        for(int i=0;i<4;i++){targets[i]=(byte)random.nextInt(6);inserted[i]=-1;}
        version=1;sync();
    }
    public boolean migrate(){
        if(version!=0)return true;
        if(!(level instanceof ServerLevel))return false;
        // Check the complete footprint before replacing anything, including inventory blocks.
        for(Direction side:SIDES){
            BlockPos pos=worldPosition.relative(side);
            if(!level.hasChunkAt(pos)||!level.getBlockState(pos).is(Content.block("eldritch_stone"))||level.getBlockEntity(pos)!=null)return false;
        }
        int paid=getBlockState().getValue(EldritchBlock.PROGRESS);
        for(int i=0;i<4;i++){
            targets[i]=(byte)EldritchBlock.rune(worldPosition,i);inserted[i]=i<paid?targets[i]:-1;
            level.setBlockAndUpdate(worldPosition.relative(SIDES[i]),Content.block("eldritch_receptacle").defaultBlockState());
        }
        version=1;sync();return true;
    }
    public InteractionResult insert(BlockPos pos,ItemStack stack,Player player){
        if(!(level instanceof ServerLevel server)||!migrate())return InteractionResult.FAIL;
        int index=index(pos);var entry=Content.entry(stack);
        if(index<0||entry==null||!entry.source_class().equals("ItemCrystals")||entry.meta()<0||entry.meta()>5||inserted[index]!=-1)return InteractionResult.FAIL;
        inserted[index]=(byte)entry.meta();stack.consume(1,player);
        if(inserted[index]!=targets[index]){
            inserted[index]=-1;sync();
            server.explode(null,pos.getX()+.5,pos.getY()+1.5,pos.getZ()+.5,1,false,Level.ExplosionInteraction.BLOCK);
        }else {sync();tryOpen();}
        dev.thaumcraft.content.ModSounds.play(server,pos,"place",net.minecraft.sounds.SoundSource.BLOCKS,.5f,1);
        return InteractionResult.SUCCESS;
    }
    public boolean tryOpen(){
        if(!(level instanceof ServerLevel server)||!migrate()||level.getBlockEntity(worldPosition)!=this)return false;
        for(int i=0;i<4;i++)if(inserted[i]!=targets[i]||!level.getBlockState(worldPosition.relative(SIDES[i])).is(Content.block("eldritch_receptacle"))||coreAt(level,worldPosition.relative(SIDES[i]))!=this)return false;
        return EldritchStructures.openEntrance(server,worldPosition);
    }
    private void sync(){
        setChanged();if(level!=null&&!level.isClientSide())level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);
    }
    @Override protected void loadAdditional(ValueInput input){
        super.loadAdditional(input);version=input.getIntOr("puzzle_version",0)==1?1:0;
        int[] savedTargets=input.getIntArray("targets").orElse(new int[0]),savedInserted=input.getIntArray("inserted").orElse(new int[0]);
        for(int i=0;i<4;i++){
            targets[i]=(byte)(i<savedTargets.length&&savedTargets[i]>=0&&savedTargets[i]<6?savedTargets[i]:EldritchBlock.rune(worldPosition,i));
            inserted[i]=(byte)(i<savedInserted.length&&savedInserted[i]>=-1&&savedInserted[i]<6?savedInserted[i]:-1);
            if(inserted[i]!=-1&&inserted[i]!=targets[i])inserted[i]=-1;
        }
    }
    @Override protected void saveAdditional(ValueOutput output){
        super.saveAdditional(output);output.putInt("puzzle_version",version);
        int[] targetData=new int[4],insertedData=new int[4];for(int i=0;i<4;i++){targetData[i]=targets[i];insertedData[i]=inserted[i];}
        output.putIntArray("targets",targetData);output.putIntArray("inserted",insertedData);
    }
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
    @Override public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries){return saveWithoutMetadata(registries);}
}
