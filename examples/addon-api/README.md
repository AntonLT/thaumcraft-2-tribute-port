# Thaumcraft addon API v1 example

For existing-content overrides, removals, localization, and Java integrations, see the [examples cookbook](../../docs/addon-examples.md) and [override pack](../addon-overrides/README.md).

Copy this folder into `<world>/datapacks/addon-api-example` and run `/reload`. It works unchanged with the Fabric and NeoForge builds for Minecraft 26.1.2.

Learn **Thaumic Restorer**, then study quartz in a Quaesitum to research **Crystal Resonance**. The new **Resonance** book category contains three infusions and a crafting recipe:

- Amethyst shard + glass, 5 vis: echo shard.
- Echo shard + obsidian in a dark infuser, 9 total vis/taint cost: ender pearl.
- Amethyst shard + glass in a crafting grid: two tinted glass, locked until the research is learned.
- Golden sword + two amethyst shards, 12 vis: a golden sword with Sharpness III and rare rarity, from the result's `components`.

The pack also shows the world rules:

- Amethyst blocks around a Quaesitum or enchanting machine act as boosters: 2 enchanting power, 3 ticks faster research, and 1 research bonus each.
- Taint turns terracotta of any color into tainted clay, and mooshrooms into tainted cows. Purifying restores the exact terracotta.
- Mycelium is in `thaumcraft2tp:taint_immune`, so taint never reaches mushroom islands.
- Turtle shells are in `thaumcraft2tp:reveals_aura` and show the goggles' aura HUD.
- Chest treasure can include 2–5 amethyst shards.
- The Arcane Bore skips spawners and budding amethyst, and Portable Holes cannot pass spawners.
- The Crucible of Souls spares villagers, wandering traders, and bosses in the `c:bosses` tag.
- Traveling Trunks refuse shulker boxes.
- Thaumic Duplicators refuse diamonds, while crucibles retain their vis value.
- Restorers charge 1.5 vis per durability point for diamond swords and 0.6 for other swords. The exact item rule wins over the higher-priority tag rule.

Two advancements detect learning `example:resonance` and completing the `example:echo_shard` infusion. `execute as @a if predicate example:knows_resonance run ...` checks research through the shipped predicate, including shared team knowledge when an addon installs a group resolver.

The `example:resonant_crystals` tag supplies the normal infusion's crystal ingredient. Amethyst is worth 12 vis and quartz has research value 30 in the new category. Removing this pack removes its definitions but retains learned `example:resonance` progress.

See [the API guide](../../docs/addon-api.md) for all schemas, priority rules, Java methods, reload behavior, and limits.

## Client translations

This example also contains `assets/example/lang/en_us.json` and `uk_ua.json`. Install the same example folder as a client resource pack to translate its research and category labels. Its data-pack installation supplies definitions to the server; it does not send language assets to clients. See [Research and categories](../../docs/addon-api.md#research-and-categories) for the key/fallback contract.
