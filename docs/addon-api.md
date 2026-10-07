# Addon API v1

Thaumcraft 2 Tribute Port exposes the same data pack formats and `dev.thaumcraft.api.ThaumcraftApi` on Fabric and NeoForge for Minecraft 26.1.2. All capabilities documented here are part of API v1. The mod ID is `thaumcraft2tp`. Use namespaced IDs in addons; numeric research indices are internal migration adapters.

The runnable [example pack](../examples/addon-api) adds a research category, a project, normal and dark infusions, an infusion with an enchanted result, a tagged ingredient, vis and research values, a locked vanilla crafting recipe, a booster block, taint rules, and an aura-revealing helmet. Copy that directory into a world's `datapacks` directory and run `/reload`. The pack needs no Java code and works on both loaders.

## Data locations

The [examples cookbook](addon-examples.md) maps every supported API area to files or Java snippets, including an independent [existing-content override pack](../examples/addon-overrides/README.md).

| Definition | Location | Identity |
| --- | --- | --- |
| Infusion | `data/<namespace>/recipe/<path>.json` | `<namespace>:<path>` |
| Research | `data/<namespace>/thaumcraft2tp/research/<path>.json` | `<namespace>:<path>` |
| Category | `data/<namespace>/thaumcraft2tp/research_categories/<path>.json` | `<namespace>:<path>` |
| Research source rule | `data/<namespace>/thaumcraft2tp/research_sources/<path>.json` | Rule ID, used for replacement and ordering |
| Vis rule | `data/<namespace>/thaumcraft2tp/vis/<path>.json` | Rule ID, used for replacement and ordering |
| Restorer cost rule | `data/<namespace>/thaumcraft2tp/restorer_costs/<path>.json` | Rule ID, used for replacement and ordering |
| Craft requirement | `data/<recipe namespace>/thaumcraft2tp/craft_requirements/<recipe path>.json` | ID of the crafting recipe being locked |
| Booster rule | `data/<namespace>/thaumcraft2tp/boosters/<path>.json` | Rule ID, used for replacement and ordering |
| Taint block rule | `data/<namespace>/thaumcraft2tp/taint_blocks/<path>.json` | Rule ID, used for replacement and ordering |
| Taint entity rule | `data/<namespace>/thaumcraft2tp/taint_entities/<path>.json` | Rule ID, used for replacement and ordering |
| Treasure contribution | `data/<namespace>/thaumcraft2tp/treasure/<path>.json` | Rule ID, used for replacement and ordering |

Research, category, value, restorer cost, requirement, booster, taint, and treasure files overlay the built-in definitions. A file at the same ID replaces that definition. `{"remove": true}` removes a definition from these catalogs. The original four categories cannot be removed. Their names may be overridden.

Every file in these folders also accepts `"required_mods": ["modid", ...]`. When any listed mod is absent, the file is ignored as if it did not exist, so it neither adds, replaces, nor removes anything. Use it for definitions that name another mod's items, tags, recipes, or research. Unknown fields are logged as warnings and ignored. Booleans must be JSON `true` or `false`, and integer fields must be JSON numbers.

### Invalid and unresolved definitions

A bad definition is skipped with a warning naming its file and reason; the rest of the catalog still loads. Skipping cascades:

- Research with an unknown category or a missing prerequisite is skipped, and so is research that depends on it. Research in a prerequisite cycle is skipped.
- Research sources naming skipped research or categories are skipped.
- Infusions requiring skipped research are skipped, so they cannot be crafted ungated.
- A craft requirement naming skipped research stays in force and denies everyone. `requiredResearch` reports `thaumcraft2tp:unavailable` for it. A requirement naming a missing recipe is skipped.
- A file replacing a built-in definition that fails to parse keeps the built-in definition. A replacement that parses but names an unknown category or a missing prerequisite, or forms a cycle, is skipped like other research; the built-in project is not restored, and research depending on it is skipped too.
- Only 64 research categories are supported. Additional categories beyond that are skipped, last in book order first, together with their research.

A summary of skipped definitions is logged after each load. Only whole-catalog limits abort startup or reject a reload.

Infusions use Minecraft's recipe manager and pack precedence. Override a recipe by supplying the same recipe path. Disabling a pack removes its recipes. `remove` is not an infusion field. Higher pack priority determines which file wins at a given ID. Minecraft logs and skips a malformed infusion like any other recipe. For compatibility recipes, use the loader's recipe conditions; a file may carry both `fabric:load_conditions` and `neoforge:conditions`, and each loader ignores the other's key.

## Infusions

```json
{
  "type": "thaumcraft2tp:infusion",
  "mode": "normal",
  "research": "example:resonance",
  "priority": 0,
  "cost": 5,
  "ingredients": ["#example:resonant_crystals", "minecraft:glass"],
  "result": {"id": "minecraft:echo_shard", "count": 1}
}
```

`mode` is `normal` by default, or `dark`. `research` is optional; when it names research that is not loaded, the infusion is skipped. `cost` is an integer from 1 through 100,000. Normal infusions have one to six ingredients; dark infusions have one to five. Each ingredient is one item ID or `#tag` string. Repeated ingredients require separate occupied slots. Stack size in one slot cannot satisfy two ingredients. Overlapping tags match regardless of input slot order. Unrelated occupied input slots prevent crafting.

`result` contains an existing item ID, an optional count, default 1, from 1 through 99, and optional `components` in Minecraft's item component format:

```json
"result": {
  "id": "minecraft:golden_sword",
  "components": {"minecraft:enchantments": {"minecraft:sharpness": 3}}
}
```

Components are checked when the recipe loads, so a recipe naming an unknown enchantment is skipped like any malformed recipe. They reach clients with the catalog, so JEI and the book show the same result. The output only stacks in the infuser's output slot with items that have identical components. Infusions with components do not give their result item a derived vis value, because vis values belong to item types, not stacks. Ingredient alternative arrays are not part of this schema.

Matching recipes are ordered by descending integer `priority`, then ascending recipe ID. Priority defaults to 0 and ranges from -100,000 through 100,000. Built-ins use priorities 1000 down through 928 to preserve their original order. Replacing a built-in recipe file also replaces its priority, so copy that value when its position matters. Missing or empty tags produce no matching inputs.

Machines save a recipe ID and a signature of its definition and ingredient tag members. A changed or removed definition, or changed tag membership, cancels its paid work before output or input consumption. Paid vis is not refunded. Unchanged definitions retain work across reloads and saves. Input switches between existing recipes with equal costs retain the original progress behavior; switching costs clears work before processing. Old saves that only have an output ID or recipe ID migrate on use.

## Research and categories

A category file:

```json
{"name": "Resonance", "order": 10}
```

The four original categories keep their original positions: `thaumcraft2tp:lost`, `thaumcraft2tp:forbidden`, `thaumcraft2tp:tainted`, and `thaumcraft2tp:eldritch`. Additional categories follow, ordered by `order` and ID. The book scrolls its category list when necessary.

An additional category may also set `fragment_model`, `theory_model` and `discovery_model` to item model IDs (`assets/<namespace>/items/<path>.json`). Its fragments, its projects' generic theories and its projects' generic discoveries then use those models through the `minecraft:item_model` component. Omitted fields keep the default look: an Eldritch fragment and the generic theory and discovery. The models must ship with the addon's client assets. Stacks in player inventories switch models on their next tick, and other stacks switch when they are next created or carried. The original four categories ignore these fields. The AE2 and Refined Storage addons use them to repaint the seal and ribbon in their own colors.

A research file:

```json
{
  "name": "Crystal Resonance",
  "text": "Study quartz to discover how crystals resonate.",
  "category": "example:resonance",
  "difficulty": 1,
  "steps": 8,
  "prerequisites": ["thaumcraft2tp:thaumic_restorer"],
  "restricted": false
}
```

`name` and `category` are required. `text` defaults to empty, `difficulty` defaults to 0 and ranges from 0 through 5, `prerequisites` defaults to empty, and `restricted` defaults to false. `name` and `text` may still be translation keys, with the supplied string as their fallback.

Optional `name_key` and `text_key` fields provide stable translation keys with readable `name` and `text` fallbacks. Categories also accept `name_key`. Keys must contain 1–256 characters without whitespace or control characters. Unknown explicit keys display the fallback text. A full override without these fields keeps its old literal-or-key behavior and does not inherit the built-in key. Ship translations in a client resource pack or Java addon's assets; server data packs alone do not install them. Language files are UTF-8 JSON in `assets/<namespace>/lang/`; English is the fallback. The core mod ships its own in `assets/thaumcraft2tp/lang/`.

A restricted project is excluded from ordinary random project selection and may be found through a research source's `special` list. All prerequisites must be known for Quaesitum selection. The public unlock API lets an addon grant a project when its own conditions are met; it does not recheck prerequisites.

`steps` is optional, defaults to 5, and accepts integers from 1 through 32767. It controls the progress needed to complete a theory; difficulty and booster rules remain unchanged. The gauge preserves the original presentation: its 16-pixel interior fills at the last unfinished step (`steps - 1`), and the next successful step produces the discovery. A one-step theory completes on its first success. Existing theories retain their progress and use the current project requirement after reload; completion is evaluated on the next research attempt. This field can also be supplied in a full override of a built-in research definition.

New projects use `thaumcraft2tp:theory_generic` and `thaumcraft2tp:discovery_generic`, carrying their research ID in the existing synchronized custom data component. Category fragments also carry their category ID. The Quaesitum creates these items as part of normal research. The Java API can create theories and discoveries directly. No per-project item registration is required. Unknown project/category items remain dormant when their addon is absent.

Legacy fields `discovery`, `result`, and `type` remain accepted for built-in overrides. An explicit `discovery` must be a registered Thaumcraft discovery item. `result` is legacy metadata; book recipe previews come from actual recipes. `type` describes the legacy reward: 0 shaped, 1 shapeless, 2 normal infusion, 3 dark infusion, 4 enchantment. It does not select a new research algorithm. Addons should normally omit these fields.

## Research source values

```json
{
  "ingredient": "minecraft:quartz",
  "value": 30,
  "category": "example:resonance",
  "special": ["example:resonance"],
  "special_chance": 25,
  "priority": 0
}
```

`ingredient` is an item or tag. `value` is an integer from -1 through 100,000, default -1. A negative value keeps the square-root-of-vis fallback. `category` defaults to `random`; an explicit category must exist. Random source rules choose among all loaded categories. Items without source rules keep the original weighted selection among the four built-in categories. `special` defaults to empty and contains existing research IDs.

A `special` project is eligible when the machine owner doesn't know it yet and knows all of its prerequisites. Each successful Quaesitum roll on the source item may produce an eligible special project's theory instead of a category fragment. Only the item in the main research slot is checked.

`special_chance` is an optional integer from 0 through 100 and requires a non-empty `special` list. When set, each successful roll has that percent chance of producing a theory, picked evenly among the eligible special projects, so list order doesn't matter. When omitted, the original odds apply: each special project is tried in list order, at 12 in 33 for restricted projects and 1 in 33 otherwise. Setting it lets an unrestricted project stay discoverable through fragments while its own materials find it much faster.

Exact item rules take precedence over tag rules. Within either group, higher `priority` wins, followed by ascending rule ID. The first matching rule supplies the whole source definition. Built-in source rule IDs are `thaumcraft2tp:legacy/0000`, etc., corresponding to the bundled `research_items` array; they have implicit priority -100,000. Addon rules default to 0.

## Crucible vis values

Crucibles reject items below full durability, including items with explicit positive vis rules. Damaged items also receive no recipe-derived vis; explicit value queries still return registered values. This restriction is part of the core mod and requires no addon.

```json
{"ingredient": "minecraft:amethyst_shard", "value": 12, "priority": 0}
```

Values must be finite and nonnegative. Zero is an explicit value and suppresses recipe derivation. Matching uses item identity, not item components.

Precedence is:

1. `ThaumcraftApi.registerVis` Java overrides.
2. Exact item data rules, then tag data rules. Within each group, higher priority wins, then ascending rule ID.
3. Existing positive configuration overrides for items without a data rule.
4. Existing recipe-derived values.

Built-in vis rules use `thaumcraft2tp:legacy/0000`, etc., corresponding to the bundled `vis` array, with implicit priority -100,000. Recipe derivation retains the port's original calculation and cheapest positive tag-member rule. Stonecutter recipes are also considered, and a stonecut result uses the cheaper of its first crafting recipe and its cheapest stonecutter recipe. Successful catalog reloads clear the value cache and bind the current recipe manager. Client and server caches are separate. Fire-focus basic-value lookups still exclude recipe derivation.

## Restorer costs

```json
{"ingredient": "#example:expensive_tools", "cost": 1.5, "priority": 10}
```

`ingredient` is an item ID or item tag. `cost` is a finite JSON number from 0 through 100,000, in vis per durability point. Exact item rules beat tag rules. Within either group, higher priority wins, then ascending rule ID. Unmatched items cost 0.25. Existing repair costs are seeded as `thaumcraft2tp:legacy/<item path>`, with priority -100,000. For example, override or remove `thaumcraft2tp:legacy/flying_carpet`.

These rules reload and synchronize with the catalog. Invalid overrides retain the built-in rule. The Restorer applies its existing 0.8 efficiency-upgrade and 1.5 enchanted-item multipliers after the base cost. Changing this cost leaves crucible vis values unchanged. `ThaumcraftApi.server().restorerCost(stack)` and `.client().restorerCost(stack)` report the base cost.

## Enchanting and research boosters

Blocks around an enchanting machine or a Quaesitum boost it. They sit in the same ring as bookshelves around a vanilla enchanting table: two blocks out, on the machine's level or one above, with an air gap between. Each booster rule gives one block or block tag four values:

```json
{"block": "minecraft:amethyst_block", "enchanting": 2, "research_speed": 3, "research_bonus": 1, "failure_protection": 0}
```

| Field | Effect, summed over every booster in the ring | Range |
| --- | --- | --- |
| `enchanting` | Enchanting power for Thaumcraft's enchanting machines. | 0–100 |
| `research_speed` | Ticks removed from each Quaesitum cycle of 130 ticks plus difficulty, down to 20. | 0–1,000 |
| `research_bonus` | Quaesitum success bonus. Success chance scales by `(100 + bonus)`. | 0–1,000 |
| `failure_protection` | Lowers the chance, `5 + difficulty² − protection` percent with a floor of 5, that a failed step costs theory progress. Also lowers the chance that inputs are consumed. Decimals are allowed; a Quaesitum rounds its total to a whole percent. | 0–100 |

A Quaesitum counts boosters like a vanilla enchanting table counts bookshelves: a block counts only when the block halfway between it and the Quaesitum is in `#minecraft:enchantment_power_transmitter`. It counts at most 16 visible boosters, choosing those with the highest combined research values, triples their `research_speed`, and doubles their `research_bonus` and `failure_protection`. Enchanting machines keep the ring rule above with no cap.

Omitted fields are 0. The built-in rules are `thaumcraft2tp:bookshelf` (1, 2, 1, 0.25) and `thaumcraft2tp:brain_in_a_jar` (4, 4, 2, 1). They reproduce the original behavior except that a bookshelf gives a quarter of a brain's failure protection. Override or remove them by ID. Matching works like vis rules: an exact block rule wins over tag rules, then higher `priority`, then ascending rule ID. The first matching rule supplies all four values. `ThaumcraftApi.server().booster(state)` reads the matching rule.

## Taint

Taint converts blocks and mobs when aura taint is high, and silverwood and purifying tools restore them. Taint rules add conversions without Java:

```json
{"block": "#minecraft:terracotta", "result": "thaumcraft2tp:tainted_clay", "material": 15}
```

```json
{"entity": "minecraft:mooshroom", "result": "thaumcraft2tp:tainted_cow"}
```

Block rules are checked before the built-in conversions and take a block ID or `#tag`. `result` is a block ID. Properties the two blocks share, such as log axes, are copied. `material` is optional and only valid when `result` is a Thaumcraft taint block. It sets the material index that decides what the tainted block drops: 0–4 and 15 drop nothing, and higher values drop congealed taint more often. Thaumcraft remembers the original block of every rule conversion, so purifying restores exactly that block. A rule result that taint did not create, such as one a player placed, is never purified.

Entity rules take an entity type ID or `#tag`; `result` is an entity type ID. Rules replace the built-in naming convention, where `minecraft:cow` becomes `thaumcraft2tp:tainted_cow`. The converted mob keeps its health fraction, custom name, and persistence. Players are never converted.

The `thaumcraft2tp:taint_immune` block and entity type tags exclude blocks and mobs from every conversion, built-in or rule. Rule matching follows vis precedence: exact before tag, then `priority`, then rule ID.

`ThaumcraftTaint.taint(level, pos)`, `purify(level, pos)`, and `taintEntity(level, entity)` apply the same conversions from Java on the server thread. They ignore the aura, but `TAINT_SPREADING` listeners and the immune tags still apply.

## Treasure

Thaumcraft adds treasure to every chest that fills from a loot table, and to the void chests of eldritch structures. Each empty slot gets one roll: with probability 1 in `fill_one_in` it receives a random item from the pool's weighted list. Two pools exist: `thaumcraft2tp:chest` (1 in 3) and `thaumcraft2tp:eldritch` (1 in 6).

A treasure file contributes entries to one pool:

```json
{
  "pool": "thaumcraft2tp:chest",
  "entries": [
    {"repeat": 20, "items": [{"item": "minecraft:amethyst_shard", "count": {"min": 2, "max": 5}}]}
  ]
}
```

Each entry appends its `items`, in order, `repeat` times (1–1,000, default 1), so `repeat` is the weight. An item is an item ID, or an object with `item` or `one_of` (a list picked at random each time the list is built) and an optional `count`: a number, or `{"min", "max"}` from 1 through 99. Items are plain stacks without components.

A pool is every contribution's entries joined in rule order: descending `priority`, then ascending rule ID. Its odds come from the first contribution that sets `fill_one_in` (1–1,000), else the pool's original odds. A pool holds at most 100,000 items after repeats.

The built-in contributions are `thaumcraft2tp:legacy/chest` and `thaumcraft2tp:legacy/eldritch`, with priority -100,000. They are data versions of the original hardcoded lists, and they produce the same chests for the same seed. Override them by ID to rebalance, or remove them with `{"remove": true}` to stop that treasure. Adding contributions changes which items a given seed produces.

## Exclusion tags

These tags are empty by default. Packs fill them to protect blocks, mobs, or items from Thaumcraft effects.

| Tag | Type | Effect |
| --- | --- | --- |
| `thaumcraft2tp:taint_immune` | Block, entity type | Taint never converts it. See [Taint](#taint). |
| `thaumcraft2tp:bore_immune` | Block | The Arcane Bore skips it, like an unbreakable block. |
| `thaumcraft2tp:portable_hole_immune` | Block | A Portable Hole cannot open through it, like an unbreakable block. |
| `thaumcraft2tp:soul_immune` | Entity type | The Crucible of Souls ignores it. Players, tamed animals, trunks, and skeleton allies are always ignored. |
| `thaumcraft2tp:trunk_forbidden` | Item | Traveling Trunks do not pick it up, and their slots refuse it. Use it to stop storage nesting. |
| `thaumcraft2tp:duplicator_forbidden` | Item | The Thaumic Duplicator refuses these templates and cancels in-progress work. Crucible vis values are unaffected. |

For region protection, use the `BLOCK_REMOVING` event instead, which also covers blocks you cannot list by type.

## Biome selection

Fabric feature selectors and NeoForge biome modifiers read the same tags under `data/thaumcraft2tp/tags/worldgen/biome/`:

| Tag | Default | Effect |
| --- | --- | --- |
| `has_arcane_vegetation` | `#minecraft:is_overworld` | Overworld vegetation and aura initialization |
| `has_arcane_deposits` | `#minecraft:is_overworld` | Infused ores and crystals |
| `has_cinnabar_deposits` | `#minecraft:is_overworld` | Cinnabar |
| `has_monolith` | `#minecraft:is_overworld` | Monolith generation |
| `has_arcane_treasure` | `#minecraft:is_overworld` | Buried treasure |
| `has_nether_arcane_vegetation` | `#minecraft:is_nether` | Nether aura initialization and vegetation |
| `spawns_wisps` | `#minecraft:is_overworld` | Natural wisp spawns |
| `spawns_arcane_mobs` | `#minecraft:is_overworld` | Natural brainy zombie, Thaumic Slime, and tainted tree spawns |

The `thaumcraft2tp:cinnabar_deposits` configured feature accepts an optional `config.deepslate_state` block state, such as `{"Name":"alts_tc2tp_patches:deepslate_cinnabar_ore"}`. It changes the ore placed in deepslate hosts while preserving the original single-block attempts, random sequence and generation settings. The existing empty configuration produces regular cinnabar in both stone and deepslate.

All IDs have the `thaumcraft2tp:` namespace. To restrict vegetation to selected biomes, replace `has_arcane_vegetation.json`:

```json
{"replace": true, "values": ["minecraft:plains", "#example:magical_forests"]}
```

Install biome changes before the server loads its world, or restart after editing them. Biome generation settings and spawn lists are built at world load, so `/reload` alone does not rebuild them. Worldgen changes apply to newly generated chunks. Existing feature config flags and mob spawn checks still apply.

On NeoForge, wisp spawns come from `neoforge/biome_modifier/wisps.json` and the other natural mobs from `spawns.json`; override either file to change them.

## Advancements and predicates

Two native advancement triggers accept Minecraft's optional `player` predicate:

- `thaumcraft2tp:research_learned` accepts optional `research`, a namespaced research ID. It fires on a new API, command, or discovery unlock for each online player whose knowledge changed, including online team members. Repeated grants do not fire. Offline personal grants and later joins do not replay it.
- `thaumcraft2tp:infusion_completed` accepts optional `recipe`, a namespaced infusion recipe ID. It fires after a normal or dark infusion completes for the online machine owner. Ownerless or offline-owned machines award no advancement.

Omit the ID filter to match every research or infusion. The [example pack](../examples/addon-api/data/example/advancement) includes both triggers.

```json
{"criteria": {"learned": {"trigger": "thaumcraft2tp:research_learned", "conditions": {"research": "example:resonance"}}}}
```

The `thaumcraft2tp:knows_research` loot condition requires `research` and accepts `entity`, default `this`. It checks the selected server player's effective knowledge, including team sharing. A missing entity or a non-player returns false. For mob drops, use `attacking_player`; `this` is the dying mob in that context.

```json
{"condition": "thaumcraft2tp:knows_research", "research": "example:resonance", "entity": "attacking_player"}
```

Use this condition in loot tables or advancement player predicates. A file under `data/example/predicate/knows_resonance.json` with `entity` omitted also works with `execute as @a if predicate example:knows_resonance run ...`. Research IDs can remain dormant after an addon is removed, matching the knowledge API.

## Aura-revealing helmets

Helmets in the `thaumcraft2tp:reveals_aura` item tag show the Goggles of Revealing aura HUD while worn. The tag holds the goggles by default. Gear reported by `IntegrationHooks.registerWornItems` counts as worn too, so an addon can let goggles work from an accessory slot. The goggles keep recording the aura from their own inventory tick, which slot mods normally call; other tagged items are recorded by Thaumcraft.

## Locked crafts and automation

For a crafting recipe named `example:resonant_glass`, create:

```text
 data/example/thaumcraft2tp/craft_requirements/resonant_glass.json
```

```json
{"research": "example:resonance"}
```

The target must be an existing crafting recipe. If the research is not loaded, the recipe stays locked for everyone. This supports vanilla or other mods' crafting recipes without replacing their serializers. It does not automatically modify arbitrary third-party machines. Their authors should call `ThaumcraftApi.canCraft` at their server output/consumption boundary.

The shared gate covers player crafting, the vanilla Crafter, and Thaumcraft infusers. A locked recipe requires an owner who knows its research. Ownerless automation is denied, even if another player knows the project. JEI hiding mirrors this check; hiding alone is not authorization. Normal shaped and shapeless recipes also receive book previews. For Minecraft ingredient alternative lists, the book uses a representative item; JEI retains the full native recipe display.

## Common Java API

Compile against the matching Thaumcraft mod artifact for the loader, with a required mod dependency on `thaumcraft2tp`. The API classes are present in both release jars and their source jars. Import from `dev.thaumcraft.api`.

```java
import dev.thaumcraft.api.ThaumcraftApi;
import net.minecraft.resources.Identifier;

var research = Identifier.parse("example:resonance");
ThaumcraftApi.unlock(server, player.getUUID(), research);
boolean learned = ThaumcraftApi.knows(server, player.getUUID(), research);
var theory = ThaumcraftApi.server().theory(research);       // Optional<ItemStack>
var discovery = ThaumcraftApi.server().discovery(research); // Optional<ItemStack>

if (ThaumcraftApi.canCraft(server, machineOwner, recipeId)) {
    // Validate inputs, then consume them and produce the result on the server.
}
```

### Catalog views

`ThaumcraftApi.server()` is the running server's authoritative catalog, including an integrated server. `ThaumcraftApi.client()` is what the client last received; it holds the built-in definitions until a server sends its catalog. Each call reads one immutable snapshot, so both views are safe from any thread, including recipe viewer loading threads. Neither view guesses the side from the calling thread.

| `ThaumcraftCatalog` method | Contract |
| --- | --- |
| `research(id)`, `allResearch()` | `Research` views: ID, category, translated `name()` and `description()`, difficulty, steps, restricted, prerequisites. |
| `categories()`, `category(id)` | `ResearchCategory` views in book order. |
| `infusions()`, `infusion(id)` | `InfusionRecipe` views in matching order. `result()` returns a new stack. |
| `requiredResearch(recipeId)` | Research locking a crafting or infusion recipe. |
| `vis(stack)`, `researchValue(stack)` | Effective gameplay values for that side. |
| `restorerCost(stack)` | Base repair cost in vis per durability point, before machine multipliers. |
| `theory(id)`, `discovery(id)` | New items, or empty when the project is not in the catalog. |
| `booster(state)` | The `Booster` rule matching a block state, or empty. |

`Research`, `ResearchCategory`, `InfusionRecipe`, and `Booster` are interfaces so that later versions can add methods without breaking addons.

### Knowledge and authorization

Methods taking a `MinecraftServer` must run on the server thread. Knowledge is global to the server and personal by default, with optional group ownership below.

| Method | Contract |
| --- | --- |
| `known(server, playerId)` | Immutable list, including dormant addon IDs. |
| `knows(server, playerId, researchId)` | Checks saved knowledge. |
| `unlock(server, playerId, researchId)` | Persists knowledge, synchronizes an online player, awards associated crafting recipes, and posts `RESEARCH_UNLOCKED`. Returns false if already known. Throws for research not in the server catalog. Prerequisites are not checked. Offline owners are supported. |
| `revoke(server, playerId, researchId)` | Removes knowledge, including dormant IDs, synchronizes, removes associated recipe book entries, and posts `RESEARCH_REVOKED`. Returns false if not known. |
| `canCraft(server, ownerId, recipeId)`, `canCraft(player, recipeId)` | Checks crafting or infusion research requirements. A null owner is allowed only for unlocked recipes. This checks authorization, not input matching or recipe existence. |
| `clientKnown()`, `clientKnows(researchId)` | The local player's knowledge as last synchronized. |
| `registerVis(itemId, value)`, `unregisterVis(itemId)` | Finite, nonnegative Java overrides. Changes invalidate caches. |
| `setGeneratorEnergyMultiplier(multiplier)`, `generatorEnergyMultiplier()` | Scale Thaumic Generator FE production, storage and output together. Default 1; integers from 1 through 53,687. Set during common initialization on both sides. Existing stored FE is retained. |

Call `registerVis` from common addon initialization on both sides when client value displays should agree. Server-only Java/configuration overrides remain authoritative for gameplay; they are not part of the data pack catalog packet. IDs of items that no mod registered are reported as warnings on each catalog load.

### Shared research

Team addons install one resolver during common initialization:

```java
ThaumcraftKnowledge.setGroupResolver((server, playerId) -> {
    UUID team = MyTeams.teamId(server, playerId);
    return team == null ? null : Identifier.fromNamespaceAndPath("myaddon", "team/" + team);
});
// After changing membership, on the server thread:
ThaumcraftKnowledge.synchronize(server);
```

Return a stable namespaced group ID, or null for personal knowledge. The resolver receives the server and a UUID, so it must handle offline owners and multiple server lifetimes. Resolver calls run on the server thread. One addon owns this resolver; installing another replaces it and returns the previous resolver. `ThaumcraftKnowledge.group(server, playerId)` reports the current group.

All server research checks, grants, revokes, machine-owner gates, client knowledge packets, and research predicates use this ownership. Unlock and revoke synchronize every online member and update their recipe books. Java research events post once for the UUID used by the caller; research advancement triggers reach all online members on a new unlock. Membership synchronization updates client knowledge and recipe books without replaying unlock events or advancement triggers.

Both loaders refresh knowledge and recipe books on login, including membership changes made while a player was offline. `synchronize(player)` refreshes one online player; `synchronize(server)` refreshes all of them.

Group knowledge starts empty and saves independently of personal knowledge. Joining does not import personal progress. Leaving restores personal knowledge, and rejoining restores the group's saved progress. Groups and dormant addon IDs persist across restarts. Addons that want to import progress can copy `known` before changing membership, then grant the projects still present in the catalog after the change. Changing the resolver's group IDs changes which saved bucket is read; keep IDs stable. This hook provides no automatic integration with a particular team mod.

### Integration adapters

`dev.thaumcraft.api.IntegrationHooks` exposes the existing common integration registrations. Register during initialization, and keep the returned `Registration` if you need to remove an adapter with `close()`. Adapters run in registration order.

| Registration | Contract |
| --- | --- |
| `registerCrop(CropAdapter)` | Apply `GROW`, `HYDRATE`, or `HARVEST` only to crops you own. Return true only after changing the crop. The first successful adapter handles the action. |
| `registerInventory(InventoryAdapter)` | Return `ItemAccess` for the requested receiving face, or null when unsupported. The first adapter returning access wins. The fallback is any vanilla `Container` or `WorldlyContainer`, honoring sided slots, stack limits and extraction rules; a double chest is reached as its combined inventory, as hoppers see it. |
| `registerWaterContainer(Function<ItemStack, ItemStack>)` | Pure conversion of one empty container. Return `ItemStack.EMPTY` when unsupported and preserve relevant components. First nonempty result wins; buckets and bottles are the fallback. Runs on clients too. |
| `registerEnergy(EnergyBridge)` | Atomically accept 0 through `offered` internal generator units. Do unit conversion in the bridge. Generator bridges share the existing output budget with loader energy access. |
| `registerWornItems(Function<Player, List<ItemStack>>)` | Report stacks the player wears outside the vanilla armor slots, such as slot-mod accessories. Return the live stacks, only the ones that are currently active, and an empty list when there are none. Queried by clients and servers, and per rendered machine, so keep it cheap. Used for the aura HUD. |

Two queries carry the original mod's integration roles for third-party adapters. `backpackAllows(BackpackCategory, stack)` reports whether an item belonged to the original miner, forester or builder backpack whitelist. `softForQuarry(state)` reports the Eldritch blocks the original marked as soft for quarries.

`ItemAccess.insert` returns an accepted count and must preserve the offered stack. `extract` returns at most the requested count and respects its filter. Simulation changes nothing. Whole-stack delivery uses `insertWhole`; override it if your storage needs its own atomic reservation, otherwise the default simulates full capacity before insertion. Inventory and crop mutations and energy transfers run on the server thread. These adapters do not load unloaded chunks.

```java
IntegrationHooks.registerWaterContainer(empty ->
        empty.is(MyItems.EMPTY_CAN) ? new ItemStack(MyItems.WATER_CAN) : ItemStack.EMPTY);
IntegrationHooks.registerCrop((level, pos, action) -> MyCrops.apply(level, pos, action));
```

Thaumcraft machines also expose their item slots to the loader's item API on each side that has slots: `Capabilities.Item.BLOCK` on NeoForge, using `WorldlyContainerWrapper`, and the Fabric Transfer API `ItemStorage.SIDED` on Fabric. Both use the same faces, slot rules and transaction rollback as hoppers. Unsided queries, faces without slots, and Void Interfaces get no access, so upgrades and hidden slots stay unreachable; on Fabric those faces answer with an empty storage. Infuser and duplicator reservations below refuse transfers. Seals and the bore also reach inventories that exist only through these loader APIs, such as storage-network interfaces, through a built-in inventory adapter; ordinary containers keep the rules above.

Thaumic Generators share one 15-unit output budget per tick between `registerEnergy` bridges and loader energy access. On NeoForge they also expose `Capabilities.Energy.BLOCK` and push into neighbouring `Energy.BLOCK` receivers, transactionally: a rolled-back transfer restores both stored energy and the per-tick allowance. Fabric has no built-in energy bridge; register one with `registerEnergy`.

Furnaces burn alumentum for 16,000 ticks, silverwood logs for 600 and greatwood logs for 400 on both loaders. On NeoForge these are also furnace fuel data-map entries.

### Infuser automation

`dev.thaumcraft.api.ThaumcraftInfusers` lets a storage-network bridge run one complete infusion batch without reimplementing matching. All methods run on the server thread.

| Method | Contract |
| --- | --- |
| `find(level, pos)` | A loaded thaumic or dark infuser, its owner and its input slot count. |
| `match(level, pos, units)` | The recipe the infuser would select for exactly these units, one item per slot, by catalog order, owner research and `INFUSION_ALLOWED`. Changes nothing. |
| `reserve(level, pos, token, recipe, units, simulate)` | Places every unit and pins the recipe and its current definition, or changes nothing. Requires an unreserved infuser with empty input and output slots and a batch that selects exactly `recipe`. |
| `status(level, pos, token)` | `UNLOADED`, `NONE` (no such reservation), `RUNNING`, `BLOCKED` (definition changed by a reload, research lost or vetoed) or `DONE`. |
| `finish(level, pos, token)` | Ends the reservation and returns every stack it owned: the actual outputs and byproducts after completion, otherwise the unconsumed inputs. Paid vis is not refunded. Loads the infuser's chunk when needed, so a bridge removed while its target is unloaded still ends the reservation. |

While reserved, hoppers, loader handlers, seals and the machine menu refuse the input and output slots, and the infuser runs only the pinned definition with its ordinary cost, speed, upgrades and redstone pause. The reservation is saved with the machine. Breaking the infuser drops its contents as usual and ends the reservation. A bridge owns a batch until `reserve` succeeds and owns returned stacks after `finish`; it should save neither as a second copy while the infuser holds them.

### Duplicator automation

`dev.thaumcraft.api.ThaumcraftDuplicators` buys one duplication cycle under the same ownership contract. All methods run on the server thread.

| Method | Contract |
| --- | --- |
| `find(level, pos)` | A loaded duplicator, whether it is in repeat mode, and a copy of its template slot. |
| `cost(item)` | Vis one cycle can cost for the item, without the efficiency upgrade that lowers it, or 0 when duplicators refuse it (not common, no vis value, or `thaumcraft2tp:duplicator_forbidden`). |
| `output(item, repeat)` | What one cycle hands over: two copies in normal mode, one in repeat mode; identity and damage only, never stored contents. |
| `reserve(level, pos, token, item, simulate)` | Normal mode places the one item into an empty duplicator; repeat mode requires a template of the same item and room for its copy. Otherwise changes nothing. |
| `status(level, pos, token)` | As for infusers; `BLOCKED` means the template is now refused. |
| `finish(level, pos, token)` | Ends the reservation and returns the copies after completion, otherwise the unconsumed item. A repeat-mode template stays in the duplicator; copies already in its output slots are handed over too. Paid vis is not refunded. Loads the chunk when needed. |

While reserved, the duplicator runs exactly one cycle and then waits, its slots are closed to hoppers, loader handlers and menus, and its mode button does nothing.

### Events

`ThaumcraftEvents` works the same on both loaders. Register listeners during common initialization. Listeners run on the thread that caused the change, and a failing listener is logged without stopping the others. There are three kinds:

- `Event`s notify after the fact.
- `VetoEvent`s ask before an action. The first listener returning false denies it and later listeners are not asked. A listener that throws counts as allowing.
- `ValueEvent`s pass a value through each listener in turn. A result that is negative, NaN, or infinite is logged and ignored.

Tell the player why when you deny an action they caused; Thaumcraft shows nothing.

Notifications:

| Event | Posted when |
| --- | --- |
| `RESEARCH_UNLOCKED` | A player learns research from a discovery or `unlock`, after saving and synchronization. |
| `RESEARCH_REVOKED` | `revoke` removed research. |
| `CATALOG_RELOADED` | The server accepted a catalog at startup or after `/reload`. Not posted for a rejected reload. |
| `CLIENT_CATALOG_CHANGED` | The client received a catalog, or reset to built-ins on disconnect. |
| `CLIENT_RESEARCH_CHANGED` | The client received the local player's known research. |
| `INFUSION_COMPLETED` | A Thaumic or Dark Infuser produced output and consumed inputs. Provides level, position, owner (nullable), recipe ID, and a result copy. |
| `CRUCIBLE_DISSOLVED` | A crucible dissolved one item into vis. Provides level, position, the credited player (the thrower if a player threw it, else the crucible owner, else null), a one-item copy, and the item's full vis value before the pure/tainted split. Not posted for rejected items or the Crucible of Souls. `vis` is the value after `CRUCIBLE_DISSOLVING`. |

Vetoes and values, all on the server thread:

| Event | Kind | Asked when |
| --- | --- | --- |
| `RESEARCH_LEARNING` | Veto | A player reads a discovery for research they do not know. Provides the player, research ID, and a copy of the discovery. Denying leaves it unlearned and the discovery unopened. `unlock` and the admin command are not asked. |
| `INFUSION_ALLOWED` | Veto | A Thaumic or Dark Infuser matched a recipe its owner may craft. Provides level, position, owner (nullable), and recipe ID. Asked every tick while the match holds, so keep it cheap. Denying acts like a research lock: the infuser tries later recipes, and paid work is cancelled. |
| `CRUCIBLE_DISSOLVING` | Value | A crucible is about to dissolve an undamaged item with positive vis. Same arguments as `CRUCIBLE_DISSOLVED`, with the current value. Return a new value, or 0 to bounce the item out like one without vis. Asked again for the same item when a full Thaumium Crucible defers it. Not asked for the Crucible of Souls. |
| `BLOCK_REMOVING` | Veto | Thaumcraft is about to remove a block without a player mining it: the Arcane Bore mining it, a Portable Hole opening through it, or an Equal Trade wand replacing it. Provides level, position, state, and the acting player's UUID, or the bore's owner (null for ownerless bores). Denying makes the bore skip the block, the Portable Hole fail as at an unbreakable block, and Equal Trade skip the block. Use it for claims. |
| `TAINT_SPREADING` | Veto | Taint is about to convert a block, grow a taint plant or spore pod, or convert a mob, at a position. Denying leaves that position unchanged; taint may try elsewhere. Not asked during world generation. Use it for protected areas. |

```java
ThaumcraftEvents.TAINT_SPREADING.register((level, pos) -> !MyClaims.isProtected(level, pos));
ThaumcraftEvents.CRUCIBLE_DISSOLVING.register((level, pos, player, item, vis) ->
        item.is(MyItems.CONDENSED_VIS) ? vis * 2 : vis);
```

### Vis network

Addon blocks can join Thaumcraft conduit networks through `VisContainer`. Implement it on your block entity, or register an adapter for a block entity type you do not own. Adapters use `BlockEntityType`, so they work the same on both loaders without Fabric API Lookup or NeoForge capabilities.

```java
// Common initialization, on both sides. The adapter may return null for entities that should not connect.
ThaumcraftVis.register(MyBlockEntities.VIS_JAR, jar -> jar.visContainer());
```

Vis has two independent kinds, pure and tainted, selected by the `tainted` flag on each method. Conduits carry vis toward the highest suction and only take; nothing pushes vis into your block.

| `VisContainer` method | Contract |
| --- | --- |
| `connects(side)` | Whether a neighbor on that side may connect. Also called on the client to draw conduit attachments, which update on neighbor shape updates; base it on the block state. |
| `vis(tainted)`, `capacity()` | Stored vis of one kind, and total capacity. |
| `suction(tainted)` | Pull toward this block, default 0, clamped to 1,000. Built-in consumers use 50, tanks 10 plus 10 per bellows, and pumps 20 plus 10 per bellows. |
| `extract(amount, tainted)` | Return exactly `min(amount, vis(tainted))` to act as a source, or 0 to refuse. |
| `insert(amount, tainted)` | Return the accepted amount, or 0 to refuse. |

A **source** reports its vis with low suction; conduits, pumps, and tanks with higher suction extract from it. A **consumer** reports suction while it wants vis, then draws each tick on the server:

```java
float got = ThaumcraftVis.pull(serverLevel, worldPosition, 1, false);
stored += got; // Pulled vis has left the network; store or spend all of it.
```

Each chunk also has an aura of pure and tainted vis. `ThaumcraftVis.aura(level, pos)` reads it, `drainAura(level, pos, amount, tainted)` removes vis from it, and `addAura(level, pos, amount, tainted)` adds vis, capped at the configured maximum. Adding taint is how a machine pollutes: high aura taint spreads taint blocks. Each returns the amount actually moved.

`pull` takes from connected neighbors in the order Thaumcraft machines use and returns 0 when the position has no container. `ThaumcraftVis.container(level, pos)` returns the container at a position, including Thaumcraft blocks. Thaumcraft conduits, pumps, tanks, condensers, and crucibles accept `insert` and `extract` while enabled; consumer machines such as infusers report their vis but refuse both. Both methods must run on the server thread. Thaumcraft keeps no copy of addon amounts, so saving them is your block's job. Goggles do not show addon container contents.


### Admin command

`/thaumcraft2` requires operator permission level 2. It uses only `dev.thaumcraft.api`, so its behavior matches the Java API exactly. Player arguments accept names, selectors, and offline players.

| Command | Effect | Result value |
| --- | --- | --- |
| `research grant <players> <id>\|all` | `unlock` for each player. Prerequisites are not checked. | Projects newly granted |
| `research revoke <players> <id>\|all` | `revoke`. Accepts dormant IDs that are not in the catalog. | Projects removed |
| `research list <players>` | Lists known research and marks dormant IDs. | Known count |
| `research list <players> <id>` | Checks one project. | Players who know it |
| `catalog dump` | Writes `thaumcraft2tp/catalog-dump.json` in the server directory. It contains categories, research, infusions, locked recipes, and every item with nonzero vis or research value. | Research count |

Result values work with `execute store`.

### Versioning

`ThaumcraftApi.apiVersion()` returns 1 for the complete API documented here. It is a method, so an addon compiled against one version reads the running version. Only the `dev.thaumcraft.api` package and the documented JSON formats are the supported addon API. Members marked `@ApiStatus.Internal` are excluded. Internal `GameData` indices, generated registration order, and implementation classes may change. Preserve namespaced IDs between releases. Breaking API or schema changes require a new API version and a documented migration.

## Reload and compatibility

The server validates category/project references, prerequisite cycles, values, infusion recipes, and craft requirements before swapping its immutable catalog. Invalid definitions are skipped as described above. Only a catalog exceeding the limits below aborts startup; on reload it is rejected and the last valid Thaumcraft catalog stays in place. Minecraft may already have reloaded its other resources; this is not a rollback of the entire game's resource manager. Infuser signatures are checked against current tag members as well as the accepted definitions before any resumed work.

The server sends the complete accepted catalog on join and successful reload. Clients update the book and JEI. Research knowledge uses a separate bounded ID packet. Clients and servers must run matching mod versions. Both loaders use the same common code.

Limits are 4,096 definitions per data folder, 4,096 effective research projects and infusions, 128 KiB per custom definition file, and 300,000 characters / 900,000 UTF-8 bytes per catalog packet. Oversized catalogs are rejected before publication; the 64-category limit is the exception and skips categories as described above. Known research packets accept at most 16,384 IDs. These bounds protect decoding and make rejected packs diagnosable.

Old numeric research saves, theories, discoveries, book snapshots, and saved infuser work remain supported. `legacy_research_ids.json` is a permanent import map. Never reorder its entries or reuse an ID for a different project. Removed addon IDs stay in knowledge saves so reinstalling the addon restores progress.

The API keeps the existing research algorithm. Custom minigames, new infuser or crucible behaviors beyond the events above, and custom vis-derivation providers are outside this API.
