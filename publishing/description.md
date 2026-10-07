# Thaumcraft 2 Tribute Port

Azanor's **Thaumcraft 2.1.6d**, ported to **Minecraft 26.1.2** on **Fabric** and **NeoForge**.

Azanor started working on Thaumcraft 2 almost 15 years ago, and the latest update of Thaumcraft 6 was 8 years ago. Until Team CoFH gives us a new Thaumcraft, this port brings back the version many of us played first, so you can play it again with your friends.

## What you get

- **Research.** Study items in the Quaesitum to earn knowledge fragments and theories, then complete discoveries in the Thaumonomicon. All 70 original research projects are here.
- **Vis.** Melt items in a crucible, then move the vis through conduits, pumps, valves and tanks to power your machines.
- **Infusion.** Craft arcane equipment, wands, foci and machines in the Thaumic and Dark Infusers. The port has 73 infusion recipes and 67 crafting recipes from the original.
- **Arcane seals.** Combine runes into seals that farm, fight, collect items and open portals that show their destination.
- **The world.** Greatwood and Silverwood trees, vis crystals, wisps and Brainy Zombies, Eldritch monoliths and chambers, and taint that spreads when the aura is polluted.
- **The original look and sound.** Classic textures, sounds and the original GUIs, adapted to modern rendering.

## How close is it to the original?

The port changes only what multiplayer needs and what Minecraft has changed since 1.2.5. Everything else aims to play as the original did. Research is saved per player, machines remember who placed them, and every effect that changes the world runs on the server.

## Install

1. Create a Minecraft **26.1.2** instance with Java 25.
2. Install **Fabric Loader** with **Fabric API**, or install **NeoForge**.
3. Put the jar for your loader in the `mods` folder of the client and of the server.

Worlds from Minecraft 1.2.5 can't be converted. Start a new world.

## For modpack makers and addon authors

The port has an addon API for Java mods and datapacks. With it you can add research and categories, infusions, vis and research values, crafting locks, boosters, taint rules and more. The full reference is in the [addon API guide](https://github.com/AntonLT/thaumcraft-2-tribute-port/blob/main/docs/addon-api.md). If you need something the API can't do yet, open an issue on GitHub and describe what you need.

## Optional addons

- **Alt's Thaumcraft 2 Tribute Port patches** are my own balance changes for modern Minecraft: vis and research values for new items, extra research gates and deepslate cinnabar. They are not the gameplay Azanor intended, and nobody has endorsed them. Install them only if you want them.
- **Applied Energistics 2** and **Refined Storage** integrations connect infusers and vis storage to storage networks.

## Credits and license

Thaumcraft 2 is by **Azanor**. The original code, art, sounds and text remain his, All Rights Reserved. This port is unofficial and is not endorsed by Azanor. The port's own code is free software under the GNU GPL, version 3 or later. Source code, bug reports and releases are on [GitHub](https://github.com/AntonLT/thaumcraft-2-tribute-port).
