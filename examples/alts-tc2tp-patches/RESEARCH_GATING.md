# Research gating

This plan is part of [Alt's addon](README.md) on both loaders and is enabled automatically by the addon jar. In a datapack-only install it comes with the same folder: copy `alts-tc2tp-patches` to `<world>/datapacks/alts-tc2tp-patches` and run `/reload`. Requires TC2TP on Minecraft 26.1.2, Fabric or NeoForge. The gates use no Java code; they sit under `data/research_gating/` and `data/thaumcraft2tp/` and use the `research_gating:` namespace.

The pack puts 33 basic arcane items behind 32 research entries. Arcane Bore research also unlocks the default Arcane Focus. Each new entry is difficulty 0 (the lowest) with the default five steps, and has a Thaumonomicon page showing its description and recipe. Until the research is learned, the recipe cannot be crafted by hand, by a Crafter or by an infuser, and it is hidden in JEI and the recipe book.

Translations for all 32 entries are in `assets/research_gating/lang/`: English, Ukrainian, every official EU language and the EEA languages, plus the other languages the core mod ships. Alt's addon bundles these client assets automatically. For a datapack-only install, also copy the `alts-tc2tp-patches` folder into each client's `resourcepacks/` directory and enable it in the resource-pack menu. Its metadata accepts both the client resource and server data formats for Minecraft 26.1.2. Installing only the datapack keeps readable English fallbacks for the new entries. The 25 built-in research overrides retain the core's translation keys and use the core's language resources.

| Research (`research_gating:`) | Locks | Prerequisites | Category |
| --- | --- | --- | --- |
| `nitor`, `alumentum`, `enchanted_fabric`, `enchanted_silverwood` | infusions | — | Lost |
| `arcane_singularity` | infusion | nitor, alumentum | Lost |
| `portable_hole` | infusion | arcane_singularity, enchanted_fabric | Lost |
| `runic_essence_air`, `_water`, `_earth`, `_fire`, `_magic` | infusions | nitor | Lost |
| `runic_essence_dark` | infusion | nitor | Tainted |
| `extract_of_lightest_air`, `_coolest_water`, `_deepest_earth`, `_warmest_fire`, `_purest_magic` | infusions | — | Lost |
| `extract_of_foulest_taint` | infusion | — | Tainted |
| `arcane_seal`, `animated_piston`, `arcane_furnace`, `thaumometer`, `boots_of_striding` | infusions | — | Lost |
| `wand_of_lightning`, `wand_of_water`, `wand_of_equal_trade`, `wand_of_fire` | infusions | — | Lost |
| `arcane_tinkering_tool`, `arcane_bellows` | crafting | — | Lost |
| `arcane_bore` | Bore and default Focus crafting | animated_piston, arcane_singularity | Lost |
| `vis_filter` | crafting | alumentum | Lost |
| `vis_pump` | crafting | arcane_bellows | Lost |

## Studying materials

Every new entry can still come from an ordinary Lost or Tainted theory. Studying one of its materials in the Quaesitum finds it much faster. Each successful roll on that material produces a theory for one of the entries it points to, picked evenly among those you haven't learned and whose prerequisites you know. Once you know them all, the material gives ordinary fragments again. Cheap materials keep the low farm-drop research values listed in [VALUES.md](VALUES.md#farmable-research-sources). Blaze Powder is Forbidden; the other materials are Lost. Catalysts with high research values, such as ancient artifacts, make every cycle succeed.

| Study | To find | Research value |
| --- | --- | --- |
| Glowstone Dust | Nitor | 4 |
| Coal or Charcoal | Alumentum | 2 |
| Any wool | Enchanted Fabric | 2 |
| Silverwood Leaves | Enchanted Silverwood | 4 |
| Gold Ingot | Arcane Seal | 4 |
| Piston | Animated Piston | 4 |
| Furnace | Arcane Furnace | 2 |
| Vis Detector or Taint Detector | Thaumometer | 8 |
| Feather | Boots of Striding | 3 |
| Leather | Arcane Bellows | 3 |
| Thaumium Ingot | Arcane Tinkering Tool | 8 |
| Vis Conduit | Vis Filter, Vis Pump | 2 |
| Nitor | Arcane Singularity, all six Runic Essences | 8 |
| Alumentum | Arcane Singularity | 8 |
| Arcane Singularity | Portable Hole, Arcane Bore | 16 |
| Animated Piston | Arcane Bore | 16 |
| Shimmerleaf | all six Extracts | 10 |
| Lightning Rod | Wand of Lightning | 4 |
| Kelp | Wand of Water | 2 |
| Emerald | Wand of Equal Trade | 4 |
| Blaze Powder | Wand of Fire | 5 |

Without catalysts, a material with value 2 needs about 37 items per theory, value 4 about 19, value 8 about 9, and value 16 about 5. The rules use the core's `special_chance` field at 100.

## What the files do

| Files under `data/` | Effect |
| --- | --- |
| `research_gating/thaumcraft2tp/research/*.json` | The 32 new research entries. |
| `thaumcraft2tp/recipe/infusion/*.json` (27) | Copies of the original infusions with a `research` field added. Cost, ingredients and priority are unchanged. |
| `thaumcraft2tp/thaumcraft2tp/craft_requirements/base_0{14,26,28,32,34,35}.json` | Research locks for the six crafting recipes. |
| `thaumcraft2tp/advancement/recipes/base_0{14,26,28,32,34,35}.json` | **Recipe book fix.** The built-in advancements gave these six recipes to the recipe book as soon as a vis crystal was picked up. The overrides use the `minecraft:impossible` trigger, so these recipes are only granted when their research is learned. |
| `thaumcraft2tp/thaumcraft2tp/research/*.json` (25) | Full overrides of built-in research whose recipes use a newly locked item. Each copies every original field and adds the new prerequisites. For example, Goggles of Revealing requires Thaumometer and Void Chest requires Portable Hole. Inert Carpet also requires Extract of Lightest Air, because its entry also covers the Flying Carpet infusion. |
| `research_gating/thaumcraft2tp/research_sources/*.json` (22) | The [study materials](#studying-materials). Each lists its entries as `special` with `special_chance: 100`, so the entries stay unrestricted and keep their place in the fragment pool. |
| `research_gating/tags/item/runic_essences.json` | New tag with all six runic essences. |
| `thaumcraft2tp/recipe/base_036.json` | Crystal Ball: each of the six essence slots accepts any runic essence from the tag. It is still unlocked and has no research. |

## Notes

- **Default Focus:** it has no separate research entry. Previously learned Focus research stays saved, but crafting the Focus requires learning Arcane Bore. The four upgraded Focus researches also require Bore.
- **Existing worlds:** already-learned research is not revoked, and items already crafted keep working. Recipes already in a player's recipe book stay there (`/recipe take <player> thaumcraft2tp:base_014` etc. removes them). The lock still blocks crafting.
- **Automation:** infusers with no owner stop producing the newly locked items.
- **Loading order:** other packs that override the same recipes, research or advancements may conflict. The pack loaded later (higher in `/datapack list`) wins per file.
- **Removing the pack:** restores the built-in definitions. Learned `research_gating:` progress stays saved and returns if the pack is reinstalled.
