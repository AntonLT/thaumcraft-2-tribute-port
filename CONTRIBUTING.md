# Contributing

Thank you for helping with the Thaumcraft 2 Tribute Port.

## What belongs where

- **The port** (`common/`, `fabric/`, `neoforge/`) should play like the original Thaumcraft 2.1.6d. Changes here fix bugs, restore original behavior, or adapt the mod to multiplayer and to what Minecraft has changed since 1.2.5. Balance and design changes do not belong in the port.
- **Alt's patches** (`examples/alts-tc2tp-patches/`) are a personal balance addon. Suggestions are welcome, but they are decided by its author's taste.
- **New features for other mods and datapacks** belong in the [addon API](docs/addon-api.md) or in a separate addon. API changes must stay compatible within API v1.

## Reporting bugs

Anything that does not work as it did in the original Thaumcraft 2 is considered a bug. Open a GitHub issue and include:

- a screenshot or a video showing the problem;
- what you did, what you expected, and what happened;
- the loader (Fabric or NeoForge) and its version, and the port version;
- other installed mods, or the modpack name and version;
- the log (`logs/latest.log`) or crash report, if there is one.

Bugs are fixed on a best-effort basis.

## Requests from modpack creators

The addon API and datapack support can be extended to make the mod easier to fit into your pack. Open a GitHub issue that describes what you need, and we will tackle it if we can.

## Pull requests

1. Build with Java 25: `./gradlew build`. This also runs the localization check, which needs Python 3.
2. Test the change in game on both loaders when it touches loader code or shared gameplay. [docs/BUILDING.md](docs/BUILDING.md) describes the development clients and the server smoke tests.
3. Keep each pull request to one topic, and follow the style of the surrounding code.
4. Do not change registered IDs, saved data formats or network packets without a migration.

Translations go in `common/src/main/resources/assets/thaumcraft2tp/lang/`. English (`en_us.json`) is the fallback; keep every key and placeholder from it.

## Rights

Thaumcraft 2 is Azanor's work; see [LICENSE](LICENSE). Contributions are licensed under the [GNU GPL, version 3 or later](COPYING). By submitting one you confirm that you wrote it or have the right to submit it under that licence.
