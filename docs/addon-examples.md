# Addon examples cookbook

All examples use [Addon API v1](addon-api.md) for Minecraft 26.1.2 with matching TC2TP client/server builds. Start with one pack in a disposable world, run `/reload`, and inspect the log for rejected definitions. Java is unnecessary for the data examples below.

For translated research and category definitions, see the `name_key` and `text_key` fields in the [API reference](addon-api.md#research-and-categories) and the starter pack’s English/Ukrainian client assets.

## Choose an example

[Alt's Thaumcraft 2 Tribute Port patches](../examples/alts-tc2tp-patches/README.md) is a personal balance addon, usable as a datapack or as a Fabric/NeoForge mod. As a mod it is also the reference Java addon: it teaches players vis values through the `CRUCIBLE_DISSOLVED` event, stores them per player, syncs them, and shows them in tooltips. Its README includes proposals for modern Minecraft research and crucible values.

| Supported use case | Example |
| --- | --- |
| New research, category, prerequisites | [Starter research](../examples/addon-api/data/example/thaumcraft2tp/research/resonance.json) and [category](../examples/addon-api/data/example/thaumcraft2tp/research_categories/resonance.json) |
| Normal infusion with a tag ingredient | [Echo shard](../examples/addon-api/data/example/recipe/echo_shard.json) and [tag](../examples/addon-api/data/example/tags/item/resonant_crystals.json) |
| Dark infusion | [Resonant pearl](../examples/addon-api/data/example/recipe/resonant_pearl.json) |
| Infusion with an enchanted result | [Resonant blade](../examples/addon-api/data/example/recipe/resonant_blade.json) |
| Enchanting and research booster block | [Amethyst booster](../examples/addon-api/data/example/thaumcraft2tp/boosters/amethyst_block.json) |
| Taint conversions and immunity | [Terracotta](../examples/addon-api/data/example/thaumcraft2tp/taint_blocks/terracotta.json), [mooshroom](../examples/addon-api/data/example/thaumcraft2tp/taint_entities/mooshroom.json), and [immune tag](../examples/addon-api/data/thaumcraft2tp/tags/block/taint_immune.json) |
| Helmet that shows the aura HUD | [Aura tag](../examples/addon-api/data/thaumcraft2tp/tags/item/reveals_aura.json) |
| Add items to chest treasure | [Amethyst treasure](../examples/addon-api/data/example/thaumcraft2tp/treasure/amethyst.json) |
| Protect blocks, mobs, and items from Thaumcraft effects | [Bore](../examples/addon-api/data/thaumcraft2tp/tags/block/bore_immune.json), [Portable Hole](../examples/addon-api/data/thaumcraft2tp/tags/block/portable_hole_immune.json), [Crucible of Souls](../examples/addon-api/data/thaumcraft2tp/tags/entity_type/soul_immune.json), and [trunk](../examples/addon-api/data/thaumcraft2tp/tags/item/trunk_forbidden.json) tags |
| New crafting recipe gated by research | [Recipe](../examples/addon-api/data/example/recipe/resonant_glass.json) and [requirement](../examples/addon-api/data/example/thaumcraft2tp/craft_requirements/resonant_glass.json) |
| Crucible vis and research values | [Vis rule](../examples/addon-api/data/example/thaumcraft2tp/vis/amethyst.json) and [research source](../examples/addon-api/data/example/thaumcraft2tp/research_sources/quartz.json) |
| Detect research and infusion in advancements | [Research learned](../examples/addon-api/data/example/advancement/research_learned.json) and [infusion completed](../examples/addon-api/data/example/advancement/infusion_completed.json) |
| Research in commands, loot, and player predicates | [Knows resonance](../examples/addon-api/data/example/predicate/knows_resonance.json), use `entity: attacking_player` for mob drops |
| Exclude expensive items from duplication | [Duplicator tag](../examples/addon-api/data/thaumcraft2tp/tags/item/duplicator_forbidden.json) |
| Repair cost by item or tag | [Diamond sword](../examples/addon-api/data/example/thaumcraft2tp/restorer_costs/diamond_sword.json) and [swords](../examples/addon-api/data/example/thaumcraft2tp/restorer_costs/swords.json) |
| Select worldgen and spawn biomes on either loader | [Biome tag reference](addon-api.md#biome-selection) |
| Share team research, adapt crops/storage/water/energy | [Shared research](addon-api.md#shared-research) and [adapter registrations](addon-api.md#integration-adapters) |
| Override existing research, optional steps | [Thaumic Restorer override](../examples/addon-overrides/data/thaumcraft2tp/thaumcraft2tp/research/thaumic_restorer.json) |
| Restricted research from a particular source | [Infusion Basics](../examples/addon-overrides/data/overrides/thaumcraft2tp/research/infusion_basics.json) and [quartz source](../examples/addon-overrides/data/overrides/thaumcraft2tp/research_sources/quartz.json) |
| Gate an existing TC2TP crafting recipe | [Infuser requirement](../examples/addon-overrides/data/thaumcraft2tp/thaumcraft2tp/craft_requirements/base_025.json) |
| Rename an existing category | [Lost category](../examples/addon-overrides/data/thaumcraft2tp/thaumcraft2tp/research_categories/lost.json) |
| Override an existing infusion | [Thaumium cost override](../examples/addon-overrides/data/thaumcraft2tp/recipe/infusion/thaumium_ingot.json) |
| Remove definitions or gates, tag values, localization | Copyable examples below |
| Query or grant knowledge, create research items, integrate machines, Java vis overrides | Java examples below |

Install instructions: [starter pack](../examples/addon-api/README.md), [override pack](../examples/addon-overrides/README.md). These are separate examples. All JSON schemas and precedence rules are in the [API reference](addon-api.md).

## Override rules and progression

The namespace and path identify the definition being replaced. A file replaces the whole definition, not individual fields. Copy the original fields first; omitted optional fields take defaults. Built-in definitions are in `common/src/main/resources/thaumcraft2tp/gameplay.json`; convert numeric categories and prerequisite indices to namespaced IDs when authoring a pack. The permanent index-to-ID map is `common/src/main/resources/thaumcraft2tp/legacy_research_ids.json`. Do not copy the internal `index` field. Native recipes are under `common/src/main/resources/data/thaumcraft2tp/recipe/`.

For another existing infusion, copy its native recipe to the same relative path in your pack and add `"research": "overrides:infusion_basics"`. Preserve `priority` to preserve matching order. For crafting, add a separate requirement using the recipe ID, as in the Infuser example. One project can gate multiple recipes; one recipe has one required project. Each alternate recipe producing an item needs its own gate.

Use `prerequisites` to require earlier research. Avoid making research depend on a machine or item that its own completion unlocks. The override example leaves the Quaesitum craftable so Infusion Basics can be researched before building an Infuser.

Base difficulty accepts 0–5. Quaesitum theories receive an adjustment of −2/−1/0/+1/+2 with probabilities 10%/20%/40%/20%/10%, clamped to 0–5. `steps` accepts 1–32767 and defaults to 5. Existing theories keep their difficulty and progress; updated steps apply after reload. Existing learned knowledge is never revoked by a harder definition.

## Remove a crafting gate

In a separate pack, put this at `data/thaumcraft2tp/thaumcraft2tp/craft_requirements/research_000.json` to remove the built-in Thaumic Restorer crafting requirement:

```json
{"remove": true}
```

This keeps the recipe and research. To remove an infusion's research requirement, override that recipe and omit `research`; infusions do not support `remove`.

The same removal marker works for research, categories, vis rules, and research source rules at their respective paths. For example, to remove the starter pack's vis rule, use `data/example/thaumcraft2tp/vis/amethyst.json`. Removing a vis rule restores the next applicable rule or fallback; it does not force zero vis.

Research/category removal must also remove or update every dependent prerequisite, source, recipe requirement, and infusion reference. Original categories cannot be removed. Removing a learned addon project keeps its ID dormant in the save, so reinstalling it restores knowledge. A higher-priority removal pack must be loaded above the pack whose definition it removes.

## Tag rules and zero values

With the starter installed, a tag vis rule at `data/myaddon/thaumcraft2tp/vis/crystals.json` can contain:

```json
{"ingredient": "#example:resonant_crystals", "value": 8, "priority": 10}
```

Exact item rules win over tag rules even when their priority is lower. Thus the starter's exact amethyst rule still wins. Use this at `data/myaddon/thaumcraft2tp/vis/amethyst.json` to explicitly give amethyst zero vis:

```json
{"ingredient": "minecraft:amethyst_shard", "value": 0, "priority": 10}
```

A research source using a tag and the vis-based value fallback, at `data/myaddon/thaumcraft2tp/research_sources/crystals.json`:

```json
{
  "ingredient": "#example:resonant_crystals",
  "value": -1,
  "category": "example:resonance",
  "special": ["example:resonance"],
  "priority": 10
}
```

Use `"category": "random"` for random category selection. A source's `special` list can reveal restricted projects; their prerequisites still apply. Values describe item identity, not individual stack components.

To keep a project in the ordinary fragment pool but let its own material find it faster, leave the project unrestricted and give the material a `special_chance`, the percent chance per successful roll:

```json
{
  "ingredient": "minecraft:glowstone_dust",
  "value": 20,
  "category": "thaumcraft2tp:lost",
  "special": ["myaddon:nitor"],
  "special_chance": 25
}
```

## Translate research

Use translation keys as the research's `name` and `text`, for example `research.myaddon.engineering.name` and `research.myaddon.engineering.text`. Supply a companion resource pack or Java mod assets with `assets/myaddon/lang/en_us.json`:

```json
{
  "research.myaddon.engineering.name": "Thaumic Engineering",
  "research.myaddon.engineering.text": "Study the construction of thaumic devices."
}
```

Add other language files at the same location. A data pack alone does not install client translations; without them the literal key is the fallback. For plain text projects, no resource pack is needed.

## Java integration examples

These are method-body snippets for an existing Fabric or NeoForge addon, not a standalone mod template. Compile against the matching TC2TP artifact and declare its required mod dependency. Import `dev.thaumcraft.api.ThaumcraftApi` and `net.minecraft.resources.Identifier`. The starter pack supplies `example:resonance` and its recipes. Variables `server`, `player`, and `stack` are a `MinecraftServer`, `ServerPlayer`, and `ItemStack`; methods taking `server` run on the server thread.

Read definitions and player knowledge:

```java
var id = Identifier.parse("example:resonance");
var definition = ThaumcraftApi.server().research(id); // Optional: pack may be absent.
var learnedIds = ThaumcraftApi.known(server, player.getUUID());
boolean learned = ThaumcraftApi.knows(server, player.getUUID(), id);
definition.ifPresent(research -> {
    int baseDifficulty = research.difficulty();
    int requiredSteps = research.steps();
    Component title = research.name(); // Translated, with the definition text as fallback.
});
```

Use `ThaumcraftApi.client()` in client code such as screens, tooltips, or recipe viewer plugins, and `ThaumcraftApi.clientKnows(id)` for the local player's knowledge. Both catalog views are safe from any thread.

Grant research from your addon's quest-completion handler, checking prerequisites explicitly:

```java
var id = Identifier.parse("example:resonance");
ThaumcraftApi.server().research(id).ifPresent(research -> {
    if (research.prerequisites().stream().allMatch(
            prerequisite -> ThaumcraftApi.knows(server, player.getUUID(), prerequisite))) {
        boolean newlyLearned = ThaumcraftApi.unlock(server, player.getUUID(), id);
        // false means it was already learned. Saving and synchronization are automatic.
    }
});
```

Call this only after your own quest condition succeeds. `unlock` does not check prerequisites itself. `ThaumcraftApi.revoke(server, uuid, id)` removes knowledge and its recipe book entries. Offline player UUIDs are supported for knowledge operations.

React to Thaumcraft from common initialization:

```java
ThaumcraftEvents.RESEARCH_UNLOCKED.register((server, playerId, research) -> {
    // Grant an advancement, start a quest stage, and so on.
});
ThaumcraftEvents.INFUSION_COMPLETED.register((level, pos, owner, recipe, result) -> { });
ThaumcraftEvents.CATALOG_RELOADED.register((server, catalog) -> { });
// Veto: keep taint out of the Nether. Returning false denies the action.
ThaumcraftEvents.TAINT_SPREADING.register((level, pos) -> !level.dimension().equals(Level.NETHER));
// Veto: keep bores, Portable Holes, and Equal Trade out of claims the actor cannot build in.
ThaumcraftEvents.BLOCK_REMOVING.register((level, pos, state, actor) -> MyClaims.canBuild(level, pos, actor));
```

Create a theory or discovery as an event reward:

```java
var id = Identifier.parse("example:resonance");
ThaumcraftApi.server().theory(id).ifPresent(reward -> {
    // Use discovery(id) instead for a completed discovery.
    if (!player.getInventory().add(reward)) player.drop(reward, false);
});
```

API-created theories start at the exact base difficulty. Creating a discovery item does not itself grant knowledge. Deliver rewards once in your own event handler.

Guard an external machine before charging inputs and before finishing its output:

```java
var recipeId = Identifier.parse("example:resonant_glass");
if (!ThaumcraftApi.canCraft(server, ownerUuid, recipeId)) {
    return;
}
// Your machine validates the recipe and inputs, then performs its normal transaction.
```

`ownerUuid` is the machine owner's UUID. A null owner is denied for a locked recipe. For a player, use `canCraft(player, recipeId)`. `canCraft` does not validate recipe existence or input matching. No callback is registered automatically by this snippet.

Read effective values:

```java
float vis = ThaumcraftApi.server().vis(stack);
int researchValue = ThaumcraftApi.server().researchValue(stack);
```

Register an item vis override during common addon initialization:

```java
ThaumcraftApi.registerVis(Identifier.parse("minecraft:amethyst_shard"), 12);
```

Java overrides take precedence over data pack values. Register on both sides for consistent displays; server-only Java overrides are not synchronized in the data catalog. `unregisterVis` removes one. Prefer JSON when pack authors should be able to change values through pack priority and `/reload`.

## Scope

Java snippets show API usage and need an addon's lifecycle/event wiring.

The API does not currently supply item-use locks, custom research minigames, infusions that modify their center item, or custom book pages. Only the veto and value events listed in the API reference can deny or change an action; the others only notify. Recipe gates do not block loot, trading, gifts, or existing items. See the API reference for reload validation, limits, and compatibility.
