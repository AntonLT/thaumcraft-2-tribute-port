# Applied values

All values are per item. Vis is the input value before crucible conversion. Research values are Quaesitum source/support values, not guaranteed progress.

| Item | Vis | Research | Category |
| --- | ---: | ---: | --- |
| `minecraft:stone` | 0.04 | 0 | Lost |
| `minecraft:cobblestone` | 0.02 | 0 | Lost |
| `minecraft:deepslate` | 0.04 | 0 | Lost |
| `minecraft:cobbled_deepslate` | 0.04 | 0 | Lost |
| `minecraft:netherrack` | 0.01 | 0 | Forbidden |
| `minecraft:tuff` | 0.04 | 0 | Lost |
| `minecraft:calcite` | 0.04 | 0 | Lost |
| `minecraft:basalt` | 0.02 | 0 | Lost |
| `minecraft:moss_block` | 0.02 | 1 | Lost |
| `minecraft:pointed_dripstone` | 0.04 | 1 | Lost |
| `minecraft:sculk` | 0.04 | 1 | Eldritch |
| `minecraft:soul_soil` | 0.04 | 2 | Forbidden |
| `minecraft:crying_obsidian` | 16 | 6 | Forbidden |
| `minecraft:ancient_debris` | 64 | 12 | Lost |
| `minecraft:honeycomb` | 4 | 4 | Lost |
| `minecraft:amethyst_shard` | 4 | 12 | Lost |
| `minecraft:prismarine_shard` | 4 | 6 | Lost |
| `minecraft:prismarine_crystals` | 9 | 10 | Lost |
| `minecraft:glow_ink_sac` | 9 | 8 | Lost |
| `minecraft:rabbit_foot` | 25 | 12 | Forbidden |
| `minecraft:phantom_membrane` | 25 | 16 | Forbidden |
| `minecraft:nautilus_shell` | 25 | 18 | Lost |
| `minecraft:heart_of_the_sea` | 64 | 40 | Lost |
| `minecraft:echo_shard` | 64 | 30 | Eldritch |
| `minecraft:disc_fragment_5` | 11.111111 | 20 | Eldritch |
| `minecraft:sniffer_egg` | 25 | 16 | Lost |
| `minecraft:breeze_rod` | 36 | 14 | Lost |
| `minecraft:heavy_core` | 96 | 45 | Eldritch |
| `minecraft:netherite_scrap` | 68 | 18 | Lost |
| All 23 vanilla pottery sherds | 2 | 18 | Lost |
| `minecraft:amethyst_block` | 4 | Original fallback | Original default |

Research value zero does not disable every supporting-item contribution: some Quaesitum calculations enforce a minimum.

Netherite scrap is deliberately more valuable than ancient debris after processing. Crafted forms not listed here retain the normal value rules and recipe derivation; explicit built-in rules can take precedence over derivation.

## Additions

| Item | Vis | Research |
| --- | ---: | --- |
| `minecraft:packed_ice` | 1 | Original rules and fallback |
| `minecraft:blue_ice` | 4 | Original rules and fallback |
| `minecraft:prismarine` | 8 | Original rules and fallback |
| `minecraft:prismarine_bricks` | 16 | Original rules and fallback |
| `minecraft:cake` | 34 | Original rules and fallback |
| `minecraft:golden_apple` | 220 | Original rules and fallback |
| `minecraft:netherite_ingot` | 380 | Original rules and fallback |
| `minecraft:emerald` | 25 | Original rules and fallback |
| `minecraft:emerald_ore`, `minecraft:deepslate_emerald_ore` | 25 | Original rules and fallback |
| All 17 shulker box variants, empty or filled | 0 | Unchanged |
| Smithing templates except the five below | 32 | 20 Lost |
| Silence, Spire, Ward, Flow, Bolt templates | 48 | 30 Eldritch |

Explicit zero vis prevents recipe fallback and makes crucibles eject shulker boxes intact. This does not block Quaesitum consumption. Java vis registrations or a higher priority data pack can override these values.

Cake excludes returned buckets from its ingredient cost. Golden apples use eight original-value gold ingots plus an apple. Netherite ingots use four scraps at 68 and four original-value gold ingots at 27. Emeralds use the original 25-vis `gemEmerald` integration value; emerald ores match the gem, as diamond ore matches diamond, so smelting a silk-touched ore gains nothing. Ordinary research retains the original square-root fallback and existing special rules.

## Original-scale anchors

Values below follow the original TC2 anchors (diamond 64, gold ingot 27). Research values and categories are not changed by them. Bulk stone, deepslate, netherrack, basalt, calcite, tuff, moss, dripstone, sculk and soul soil keep their low values.

| Item | Vis | Anchor |
| --- | ---: | --- |
| Crying obsidian | 16 | Obsidian 16 |
| Ancient debris / netherite scrap | 64 / 68 | Diamond ore 64; small refining premium as gold ore 25 → ingot 27 |
| Netherite ingot | 380 | Four scraps and four gold ingots |
| Breeze rod | 36 | Blaze rod 36 |
| Rabbit's foot, phantom membrane, nautilus shell, sniffer egg | 25 | Slime ball 25 |
| Echo shard | 64 | Ender pearl 64 |
| Heart of the sea / heavy core | 64 / 96 | Same ratio to diamond 64 |
| Disc fragment 5 | 11.111111 | Nine fragments make a 100-vis disc |
| Prismarine shard / crystals | 4 / 9 | Common drop 4; glowstone dust 9 |
| Prismarine / prismarine bricks | 8 / 16 | Shard ratio kept |
| Glow ink sac, honeycomb | 9 / 4 | Glowstone dust 9; common drop 4 |
| Amethyst shard / block | 4 / 4 | Common drop 4; block still equals shard |
| Smithing templates / Silence, Spire, Ward, Flow, Bolt | 32 / 48 | Twice their diamond-relative price |
| Saddle | 17 | Modern recipe: three leather and one iron ingot |

Stone-scale products follow their inputs instead of the core's higher original values, which removes crafting and smelting gains:

| Item | Core | Patch | Basis |
| --- | ---: | ---: | --- |
| Mossy cobblestone | 4 | 0.04 | Cobblestone 0.02 and moss 0.02 |
| Mossy stone bricks | 1 | 0.06 | Stone bricks 0.04 and moss 0.02 |
| Cracked stone bricks | 1 | 0.04 | Smelted stone bricks |
| Nether brick | derived | 0.01 | Smelted netherrack |
| Nether bricks | 2 | 0.04 | Four nether bricks |

## Conversion fixes

These values make processing never create vis, except the intended copper smelting premium.

| Item | Core | Patch | Basis |
| --- | ---: | ---: | --- |
| Pumpkin seeds | 4 | 1 | One pumpkin crafts four |
| Melon seeds | 4 | 2 | One melon slice |
| Green dye | 4 | 2 | Smelted cactus |
| White dye | 4 | 1.333333 | One bone makes three bone meal |
| Lime / gray / light gray dye | 4 | 1.666667 / 2.666667 / 2 | Cheapest dye-mixing recipe |
| Light blue / pink / cyan / magenta dye | 4 | 2.666667 / 2.666667 / 3 / 3.333333 | Cheapest dye-mixing recipe |
| Brick | 2 | 1 | Smelted clay ball |
| Honey bottle / honey block | derived | 13 / 48 | Three honeycomb at 4 plus bottle; four bottles' honey |
| Copper ore, deepslate copper ore, raw copper | derived | 3 | Ore value |
| Copper ingot | derived | 4 | Ingot value; one-vis smelting premium |

Honey sugar costs 4, matching sugar cane, so both cake routes cost 34.

## Farmable research sources

These change Quaesitum research only; vis values are unchanged. Without this pack, these items use the square-root fallback, mostly 1–2, and except for items with an original rule they roll the default category, which is Lost 94% of the time. An explicit category lets a farm aim at one fragment type.

| Item | Research | Category | Core | Hints |
| --- | ---: | --- | --- | --- |
| `minecraft:paper` | 5 | Lost | 2 | — |
| `minecraft:ink_sac` | 4 | Lost | 2 | — |
| `minecraft:feather` | 3 | Lost | 2, study material | Boots of Striding (study material) |
| `minecraft:leather` | 3 | Lost | 2, study material | Arcane Bellows (study material) |
| `minecraft:redstone` | 4 | Lost | 2 | — |
| `minecraft:lapis_lazuli` | 5 | Lost | 3 | — |
| `minecraft:experience_bottle` | 6 | Lost | 1 | Collected Wisdom, 10% per successful roll |
| `minecraft:book` | 20 | Lost | 5, original | Thaumic Enchanter (original) |
| `minecraft:bookshelf` | 30 | Lost | 8, original | Thaumic Enchanter (original) |
| `minecraft:bone` | 4 | Forbidden | 2, original | Crucible of Souls, Charm of the Dead (original) |
| `minecraft:rotten_flesh` | 4 | Forbidden | 2, original | Crucible of Souls, Charm of the Dead (original) |
| `minecraft:nether_wart` | 4 | Forbidden | 2, original | — |
| `minecraft:soul_sand` | 3 | Forbidden | 1, original | Traveling Trunk, Crucible of Souls (original) |
| `minecraft:gunpowder` | 4 | Forbidden | 3 | — |
| `minecraft:blaze_rod` | 8 | Forbidden | 6 | — |
| `minecraft:blaze_powder` | 5 | Forbidden | 4 Lost, study material | Wand of Fire (study material) |
| `minecraft:magma_cream` | 8 | Forbidden | 7 or less | — |
| `minecraft:wither_rose` | 6 | Forbidden | 1 | — |

Feather, leather and blaze powder are [study materials](RESEARCH_GATING.md#studying-materials), so their rules stay in `data/research_gating/` with the new values; a second rule for the same item would replace the study hint. Rules for items with an original source copy its hints, because an addon rule replaces the whole built-in definition.

Without boosters, a main-slot item with research value *v* gives a fragment on about *v*% of cycles and is consumed 75% of the time, so value 4 costs about 19 items per fragment. In a theory, both support slots are consumed every cycle and a matching support adds its full value. Farm drops capped near 8 therefore make difficulty 4 and below practical. Difficulty 5 still needs the higher exploration values above.

Paper at 5 would make the original book (5, three paper and leather) pointless, so books are 20. Bookshelves (three books and six planks) are 30: a fragment costs about 7.5 books in bookshelves instead of 3.75 loose books, but comes 1.5 times faster in one Quaesitum.

## Artifact research values

The 24 TC2TP artifacts have double their original research values. Original categories and hints are kept.

| Tier | Lost | Forbidden | Tainted | Eldritch | Core | Patch |
| --- | --- | --- | --- | --- | ---: | ---: |
| Common | Ancient Pottery, Tarnished Chalice | Cracked Wisp Shell, Distorted Skull | Taint Spores, Tainted Organ | Shard of Strange Metal, Eldritch Mechanism | 10 | 20 |
| Uncommon | Worn Statuette, Ancient Weapon | Inhuman Skull, Darkened Crystal Eye | Tainted Fruit, Tainted Branch | Opalescent Eye, Disturbing Mirror | 20 | 40 |
| Rare | Ancient Seal | Knotted Spite | Intact Taintspore Pod | Glowing Eldritch Device | 40 | 80 |
| Unique | Ancient Stone Tablet | Tome of Forbidden Knowledge | Writhing Taint Tendrils | Eldritch Repository | 80 | 160 |

A main-slot artifact can produce several results in one cycle, each costing a paper, so doubled values roughly double fragments per artifact. As a matching support, a unique artifact (160) guarantees a theory step at any difficulty, and a rare one (80) gives at least 85%.
