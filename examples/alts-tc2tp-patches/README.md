# Alt's Thaumcraft 2 Tribute Port patches

A personal balance addon and a small public example of TC2TP's data pack API. The initial focus is crucible vis and research values for modern Minecraft resources.

## Install

Requires Minecraft 26.1.2 and TC2TP `1.0.0-rc.2` or newer, on Fabric or NeoForge. There are two ways to install it:

- **Mod (recommended):** put `alts_tc2tp_patches-<loader>-26.1.2-<version>.jar` in `mods` next to TC2TP. Update both jars together. It includes every rule in this pack, the [32-item research plan](RESEARCH_GATING.md), crucible vis knowledge, and deepslate cinnabar ore. Install it on the server and on every client because it registers a new block.
- **Datapack only:** copy this folder into `<world>/datapacks/alts-tc2tp-patches`, then run `/reload` or reopen the world. The folder must have `pack.mcmeta` directly inside it. You get the balance values, Thaumonomicon recipe and research gates without vis knowledge, deepslate cinnabar, or the generation override.

Don't use both at once. The rules are identical, so it's harmless, but redundant.

On NeoForge, installing [Curios](https://modrinth.com/mod/curios) 15 or newer adds [accessory slots for TC2TP gear](#curios-support). It is optional and needs no setup.

The mod includes translations for its block, raw cinnabar item, tag, vis tooltip, learning message, and all 32 new research names and descriptions in English, Ukrainian, every official EU language, the EEA languages (Icelandic, Norwegian Bokmål and Nynorsk) and the other languages the core mod ships. The Irish, Maltese, Icelandic, Latvian, Lithuanian, Hungarian and Nynorsk text has had no native review. The 25 built-in research overrides retain the core's translations. With a datapack-only install, also copy this folder into each client's `resourcepacks/` directory and enable it; its `pack.mcmeta` accepts both the data and resource formats, and the translations are in `assets/research_gating/lang/`.

## Crucible vis knowledge

Dissolving an item in a crucible teaches you its vis value. From then on, that item's tooltip shows `Vis: <value>` for every stack of it, and the first time you learn one you see "Learned the vis value of …" above the hotbar.

- The player who threw the item learns it. Items that weren't thrown by a player (for example, from a hopper) teach the crucible's owner, or nobody if it has none.
- Knowledge is per player and per item type, and is saved with the world.
- Values come from the server, so recipe-derived values are correct and tooltips update after `/reload` changes a value.
- Damaged equipment shows no value, because a crucible rejects it.
- Only items a crucible actually dissolves are learned. Rejected items (no vis, or damaged) and the Crucible of Souls teach nothing.

It is built on TC2TP's `CRUCIBLE_DISSOLVED` event, so it uses only the public addon API. See [the API guide](../../docs/addon-api.md#events).

### Build and test

The mod builds as part of the TC2TP Gradle build. Common code in `common/` compiles into each loader jar; `fabric/` and `neoforge/` hold the entry points. The jar bundles this folder's `data/` directory and its `pack.mcmeta` as an always enabled builtin datapack above the core mod's resources. Each rule has one source file, shared by both loaders.

This folder's `assets/` directory (the research-gating translations) ships at the jar root with the addon's client assets. Both addon `check` tasks run the shared localization verifier, which checks language coverage, placeholders, English fallbacks and retained core translation keys.

```
./gradlew :alts-patches-fabric:build :alts-patches-neoforge:build
```

Jars are written to `fabric/build/libs` and `neoforge/build/libs`. `./gradlew :alts-patches-<loader>:runServer -PsmokeTest` boots a dev server with both mods and runs the checks in `common/src/smokeTest`: all 32 research gates and 25 prerequisite overrides load; player crafting, Crafters and infusers reject unlearned recipes and permit learned ones; Quaesitum selection enforces prerequisites; both Thaumonomicon recipes accept all four fragments in either crafting grid; both cinnabar ores drop raw cinnabar and keep their block with Silk Touch; raw cinnabar cooks into quicksilver; both cinnabar variants generate naturally; a real crucible teaches its owner; the sync payload round-trips; and tooltips follow it. The log prints `ALTS_SMOKE_TESTS_PASS` on success.

The NeoForge run also loads the real Curios jar and runs a second set of checks: slot tags and slot assignment, goggles revealing auras from a curio slot, a charm healing from one through Curios' own tick event, and all three arcane boots providing movement and fall protection from the feet slot. The log prints `ALTS_CURIOS_SMOKE_PASS` on success. `./gradlew :alts-patches-neoforge:runClient -PsmokeTest` opens a dev client that creates a flat world, renders goggles from a curio slot alone and over an iron helmet, checks that the head changed, saves screenshots in `neoforge/runs/client/screenshots`, prints `ALTS_CURIOS_CLIENT_PASS` and quits. Neither the Curios jar nor these checks are part of a normal build.

## Curios support

With [Curios](https://modrinth.com/mod/curios) 15 or newer on NeoForge, every player gets three accessory slots with no setup:

| Slot | Accepts | Notes |
| --- | --- | --- |
| Charm | Charm of Life, Vigor, the Dead, Cleansing and Souls | They tick from a charm slot through the same item code as in the inventory, so effects, durability use and the soul charge are unchanged. |
| Head | Goggles of Revealing | The aura HUD and the legacy goggles visuals work from the slot, so goggles can be worn with a helmet. Disabled slots reveal nothing. |
| Feet | Boots of Striding, Seven League Boots and Boots of the Meteor | Movement speed, jump height, step height, fall protection and the meteor stomp work with ordinary armor boots. Arcane boots in the vanilla armor slot take precedence, so effects do not stack. |

Each slot holds one item. The addon reuses Curios' built-in feet slot. The slot tags are `data/curios/tags/item/charm.json`, `head.json` and `feet.json`, and the slots are given to players by `data/alts_tc2tp_patches/curios/entities/entities.json`. Edit those files in the datapack to change what fits or who gets a slot, or add `size` through Curios' slot data for more than one slot. The files are inert without Curios. A datapack-only install gives players the slots, but the effects above, the goggles' aura HUD and their rendering on the player need the mod.

The Mask of Cruelty, Void Bracelet and Thaumometer are left out. The mask acts through the real helmet slot, and the bracelet and Thaumometer are tools you use or carry, so a slot would add nothing. Goggles in the head slot are drawn on the player with the same texture as in the helmet slot, slightly larger so they sit over a helmet. Curios' visibility toggle hides them.

Fabric has no Curios release for 26.1.2, so there the slot files are present but unused. Thaumcraft does not depend on Curios; the addon asks it for equipped items through the core's `IntegrationHooks.registerWornItems`, described in [the API guide](../../docs/addon-api.md#integration-adapters).

## Current changes

The Thaumic Generator produces 60,000 FE per vis, with small per-tick rounding. It stores 2,000,000 FE, or 4,000,000 FE with the storage upgrade, and can output 6,000 FE per tick. Generation, storage and output are scaled by 100 through the core addon API.

Brainy Zombies independently roll a 50% chance for one Zombie Brain and a 50% chance for one Distorted Skull, so both can drop together with a 25% chance. This loot override applies to both the mod and datapack installs. Zombie Brains have research value 20, inherited from the core's Forbidden research source and its original hints. Flesh and rare iron-item rolls keep their core behavior.

The [research plan](RESEARCH_GATING.md) adds 32 research entries for basic arcane materials, components, machines and wands. Bore research also unlocks the default Arcane Focus and requires Animated Piston and Arcane Singularity; upgraded foci require Bore research. Their 27 infusions and six crafting recipes stay locked until the player or machine owner learns the corresponding research. JEI and the recipe book hide unlearned recipes. The plan also adds ingredient-derived prerequisites to 25 existing projects. New research uses difficulty 0 and five steps; Dark Runic Essence and Extract of Foulest Taint belong to Tainted, and the rest belong to Lost. Crystal Ball remains ungated and accepts any runic essence in each essence slot. Every new entry also has a [study material](RESEARCH_GATING.md#studying-materials), for example glowstone for Nitor or a piston for Animated Piston, which finds it in the Quaesitum much faster than waiting for a random theory. Existing knowledge and items are retained; new research is not granted automatically. The stable IDs keep the `research_gating:` namespace of the earlier standalone pack, so saved progress carries over.

The mod adds Deepslate Cinnabar Ore, with the original cinnabar overlay on a deepslate texture. It smelts into one quicksilver, has the same vis value and hardness as regular cinnabar, and appears beside cinnabar in the TC2TP creative tab.

Both cinnabar ores drop Raw Cinnabar, like vanilla raw ores: Fortune increases the drop and Silk Touch drops the ore block. Raw cinnabar smelts or blasts into one quicksilver and has the ore's vis value of 16. The regular ore's loot table is overridden by the bundled pack, so removing the mod restores its original self-drop.

Cinnabar keeps the core's original scattered single-block generation, uniformly between the world bottom and Y=50, with 15 attempts per 51 vertical layers. The addon uses the same generator and only changes the resulting block when the host is deepslate. Stone targets produce regular cinnabar; deepslate targets produce the new variant. The bundled pack replaces `thaumcraft2tp:cinnabar_deposits` once, preserving positions, density, biome selection and the core's world-generation setting. Crystals retain their separate feature. Generation changes apply to newly generated chunks. Back up worlds before removing the mod because existing deepslate cinnabar blocks depend on it.

The Thaumonomicon uses a shapeless recipe: one ordinary book plus one knowledge fragment. The `alts_tc2tp_patches:knowledge_fragments` item tag contains the Eldritch, Forbidden, Lost, and Tainted fragments. Both original recipe IDs are overridden and share a recipe-book group. This works in the inventory grid or crafting table, in both the mod and datapack versions. Removing the addon restores the original recipes after reloading. Keep the datapack above other packs that override these recipes.

Axe of the Stream, Sword of the Zephyr, Pickaxe of the Core, Shovel of Renewal, and Hoe of the Mystic are crafted in the normal Thaumic Infuser for 50 vis each. Their original ingredient quantities, Thaumium tool tags, and research requirements are preserved. These recipes replace the workbench recipes and appear as infusions in the Thaumonomicon and JEI.

Thaumic Generator research requires Stabilized Singularity, and Traveling Trunk research requires Dark Infuser, in both the mod and datapack installs. Already learned research is retained.

The pack contains 123 vis rules and 110 research-source rules. See [the complete value table](VALUES.md) for all applied values and categories, including all 23 pottery sherds and smithing templates. Common farm drops such as paper, bone and redstone have explicit Lost or Forbidden research values, books and bookshelves are worth 20 and 30 Lost research, and the 24 TC2TP artifacts have double their original research values. Amethyst blocks are worth 4 vis. All 17 shulker box variants have zero vis to prevent crucible consumption.

Each vis rule is one file. For example, [stone.json](data/alts_tc2tp_patches/thaumcraft2tp/vis/stone.json) sets ordinary stone to 0.04 vis. A stack of 64 has a total input value of 2.56; actual pure vis yield depends on crucible conversion, with the remainder becoming taint. Fractional values are supported.

This is a shared item vis value, so other TC2TP systems querying that value also see the change, and recipe-derived values can be affected. Explicit rules for other items remain independent. It is not a crucible-only multiplier. Research values can be defined separately through `research_sources`.

The addon uses namespace `alts_tc2tp_patches` and the default rule priority of 0, which takes precedence over built-in rules. Other exact-item addon rules may compete; higher priority wins, then ascending rule ID. Java vis overrides take precedence over data rules. Remove the pack and reload to restore the remaining rules.

## Ideas for future patches

The ideas below describe the broader direction. Only the exact items and values in VALUES.md are enabled; other candidates remain proposals.

| Area | Candidates | Balance direction |
| --- | --- | --- |
| Abundant building materials | Cobblestone, deepslate, tuff, basalt, netherrack | Review as a group so bulk mining and generators do not dominate vis production. Keep exact item rules where acquisition differs. |
| Modern renewable resources | Bamboo, kelp, moss, dripstone, honey | Give useful but modest values; check automated production rates and crafted forms. |
| Crystals and geology | Amethyst shards, quartz, calcite | Give research usefulness independently of raw vis. Amethyst's magical theme need not imply a large renewable vis source. |
| Ocean exploration | Prismarine shards/crystals, nautilus shells, heart of the sea | Separate farmable drops from exploration rewards; consider Lost research sources. |
| Ancient cities | Echo shards, sculk, recovery compass | Consider Eldritch research associations. Treat farmable sculk differently from scarce chest loot. |
| Nether progression | Ancient debris, netherite scrap, crying obsidian | Reward exploration while checking barter availability and recipe conversions. |
| Archaeology | Pottery sherds and other excavation finds | Make finds useful research sources without requiring high crucible values. Named sherds can introduce specific projects through `special`. |
| Trial chambers | Breeze rods, heavy cores | Consider research rewards for combat and exploration; compare repeatable drops with rarer rewards. |

Before adding a family, inspect block-to-item conversions, crafting output counts, stonecutting, smelting, and farm throughput. Research source rules accept integer values; crucible vis accepts fractions. Research category/source changes and new projects are supported by the same API.

## Use this as an example

To add another vis adjustment, copy `stone.json` under a new filename and change `ingredient` and `value`. Use an item ID for a precise change or `#namespace:tag` for a deliberate group. Avoid tagging together items whose acquisition cost differs substantially.

For research values, research definitions, recipe gates, and Java integrations, see the [examples cookbook](../../docs/addon-examples.md) and [API reference](../../docs/addon-api.md).
