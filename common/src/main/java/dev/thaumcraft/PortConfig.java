package dev.thaumcraft;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class PortConfig {
    public static boolean worldGeneration=true,monoliths=true,taintSpread=true,naturalSpawns=true,areaMining=true;
    public static boolean lowGfx=false,pipedrips=true,portalGfx=true,toolShift=false;
    public static int auraMax=15000,taintSpawn=1,autosaveMinutes=5;
    public static String customSmelting="Nikolite:6;Flax_Seeds:1;Red_Alloy_Ingot:29;Blue_Alloy_Ingot:37;Marble:1;Basalt:1;Basalt_Cobblestone:1;";
    private static java.util.Map<String,Float> smelting=java.util.Map.of();
    private PortConfig() {}
    // Entries name a full item ID or, like the original's item names, a registry path in any namespace
    // ("Red_Alloy_Ingot" matches *:red_alloy_ingot). Display names are not used: an anvil rename must not add vis.
    public static float customVis(net.minecraft.world.item.ItemStack stack){
        var id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        return smelting.getOrDefault(id.toString(),smelting.getOrDefault(id.getPath(),0f));
    }
    public static void setCustomSmelting(String value){
        var parsed=new java.util.HashMap<String,Float>();
        for(String entry:value.split(";")){
            if(entry.isBlank())continue;int split=entry.lastIndexOf(':');
            if(split<=0)throw new IllegalArgumentException("Invalid smelting entry: "+entry);
            float amount=Float.parseFloat(entry.substring(split+1).trim());
            if(!Float.isFinite(amount)||amount<0)throw new IllegalArgumentException("Invalid smelting value: "+entry);
            String key=entry.substring(0,split).trim();parsed.put(key.contains(":")?key:key.toLowerCase(java.util.Locale.ROOT).replace(' ','_'),amount);
        }
        customSmelting=value;smelting=java.util.Map.copyOf(parsed);
    }

    public static void load(Path directory) {
        Path file=directory.resolve("thaumcraft2.properties");Properties properties=new Properties();
        try {
            if(Files.exists(file))try(var reader=Files.newBufferedReader(file)){properties.load(reader);}
            worldGeneration=setting(properties,"world_generation",true);monoliths=setting(properties,"monoliths",true);
            taintSpread=setting(properties,"taint_spread",true);naturalSpawns=setting(properties,"natural_mob_spawns",true);areaMining=setting(properties,"elemental_area_mining",true);
            lowGfx=setting(properties,"graphics.low",false);pipedrips=setting(properties,"graphics.pipedrips",true);
            portalGfx=setting(properties,"portals.seethrough",true);toolShift=setting(properties,"tools.shift.enables",false);
            auraMax=Math.clamp(Integer.parseInt(properties.getProperty("aura.max","15000")),5000,30000);
            taintSpawn=Math.clamp(Integer.parseInt(properties.getProperty("aura.taintspawn","1")),0,2);
            autosaveMinutes=Math.clamp(Integer.parseInt(properties.getProperty("auto.save","5")),0,32767);
            properties.setProperty("auto.save",Integer.toString(autosaveMinutes));
            properties.setProperty("aura.max",Integer.toString(auraMax));properties.setProperty("aura.taintspawn",Integer.toString(taintSpawn));
            setCustomSmelting(properties.getProperty("smelting.other.mods",customSmelting));properties.setProperty("smelting.other.mods",customSmelting);
            if(!Files.exists(file)){Files.createDirectories(directory);try(var writer=Files.newBufferedWriter(file)){properties.store(writer,"Thaumcraft 2 Tribute Port — restart the game after changing these settings.");}}
        } catch(java.io.IOException e) {throw new IllegalStateException("Cannot load Thaumcraft configuration "+file,e);}
    }
    private static boolean setting(Properties properties,String key,boolean fallback) {
        String value=properties.getProperty(key,Boolean.toString(fallback));
        if(!value.equalsIgnoreCase("true")&&!value.equalsIgnoreCase("false"))throw new IllegalArgumentException("Invalid boolean for "+key+": "+value);
        properties.setProperty(key,value);return Boolean.parseBoolean(value);
    }
}
