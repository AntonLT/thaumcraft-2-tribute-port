# Existing content overrides

Copy this directory to `<world>/datapacks/addon-overrides` and run `/reload`. Requires TC2TP on Minecraft 26.1.2, Fabric or NeoForge. No other example pack is required.

| File under `data/` | Effect |
| --- | --- |
| `thaumcraft2tp/thaumcraft2tp/research/thaumic_restorer.json` | Replaces the built-in definition, preserving its original fields and setting eight required steps. |
| `overrides/thaumcraft2tp/research/infusion_basics.json` | Adds restricted research with three steps and no prerequisites. |
| `overrides/thaumcraft2tp/research_sources/quartz.json` | Lets quartz reveal Infusion Basics in the Quaesitum. |
| `thaumcraft2tp/thaumcraft2tp/craft_requirements/base_025.json` | Requires Infusion Basics to craft a Thaumic Infuser. |
| `thaumcraft2tp/thaumcraft2tp/research_categories/lost.json` | Renames the original Lost category. |
| `thaumcraft2tp/recipe/infusion/thaumium_ingot.json` | Replaces the original infusion, changing its cost from five to six while preserving ingredients and priority. |

In a fresh survival test world, the Infuser crafting output should be locked. Study quartz with paper in a Quaesitum until Infusion Basics appears, complete its three steps, and use its discovery. The Infuser recipe should then become available. Theory difficulty still receives its usual random adjustment.

Overrides replace complete definitions. Keeping the original research ID preserves learned knowledge and recipe references. Already learned research is not revoked. Existing theories retain their stored difficulty and progress but use the current required step count. Removing this pack restores built-in definitions and preserves dormant addon knowledge.

Install examples separately while learning: both packs define a quartz research source, and this pack's priority 10 wins over the starter's priority 0. Other packs may also override these same definitions.

See the [cookbook](../../docs/addon-examples.md) for removals, localization, and Java integrations.
