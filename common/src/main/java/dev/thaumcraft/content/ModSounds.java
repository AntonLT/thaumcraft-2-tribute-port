package dev.thaumcraft.content;

import dev.thaumcraft.Thaumcraft;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/** Registered legacy sound events, including SoundPool's numbered variant groups. */
public final class ModSounds {
    private static final Map<String,SoundEvent> EVENTS=new LinkedHashMap<>();
    static {
        for(String name:new String[]{"attach", "beamloop", "bubbling", "creaking", "creaking1", "creaking2", "elecloop", "fireloop", "gore", "gore1", "gore2", "heal", "infuser", "infuserdark", "learn", "monolith", "monolithfound", "page", "page1", "page2", "pclose", "place", "podburst", "popen", "recover", "roots", "rumble", "rune_set", "scribble", "shock", "shock1", "shock2", "singularity", "stomp", "stomp1", "stomp2", "stoneclose", "stoneopen", "suck", "swing", "swing1", "swing2", "tinkering", "tool", "tool1", "tool2", "upgrade", "whisper", "wind", "wind1", "wind2", "zap", "zap1", "zap2"})
            EVENTS.put(name,SoundEvent.createVariableRangeEvent(Thaumcraft.id(name)));
    }
    private ModSounds() {}
    public static void register(BiConsumer<String,SoundEvent> registrar){EVENTS.forEach(registrar);}
    public static SoundEvent event(String name){
        SoundEvent event=EVENTS.get(name);
        if(event==null)throw new IllegalArgumentException("Unknown Thaumcraft sound: "+name);
        return event;
    }
    public static void play(Level level,BlockPos pos,String name,SoundSource source,float volume,float pitch){
        level.playSound(null,pos,event(name),source,volume,pitch);
    }
    public static void playAt(Level level,double x,double y,double z,String name,SoundSource source,float volume,float pitch){
        level.playSound(null,x,y,z,event(name),source,volume,pitch);
    }
}
