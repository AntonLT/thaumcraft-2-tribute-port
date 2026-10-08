# Changelog

Changes that players and modpack creators notice, newest first. Anything that still differs from the original Thaumcraft 2.1.6d is a bug; see [CONTRIBUTING.md](CONTRIBUTING.md).

## 1.0.0-rc.3

### Changed

- Natural Silverwood grows only in the biomes the original listed: forest, taiga, snowy taiga, and jungle, which stands in for the original's jungle hills. Vanilla biomes added after Minecraft 1.2.5 no longer grow it: flower, birch, old growth birch, and dark forests, pale gardens, groves, both old growth taigas, sparse jungles, and bamboo jungles. Greatwood biomes are unchanged. Natural Silverwood is now about a third as common as before, which restores the original's ratio of about two Greatwoods per Silverwood.
- Modded forests and taigas tagged `c:is_forest` or `c:is_taiga` now get the forest aura and grow both trees, and biomes tagged `c:is_overworld` get Thaumcraft vegetation.
- Modpack creators can choose tree biomes and aura levels with five new biome tags: `silverwood_habitat`, `silverwood_excluded`, `greatwood_habitat`, `high_aura`, and `extreme_aura`. See the biome selection section of [docs/addon-api.md](docs/addon-api.md). Chunks that already have saved aura keep it.

### Fixed

- Natural Greatwood and Silverwood trees no longer generate on snow-covered ground, such as snowy taigas and groves. In the original, snow stopped these trees from growing.
- Bone meal, dispensers, and the Shovel of Renewal no longer grow a Greatwood Sapling. As in the original, the sapling grows only on its own, with light level 9 or more and more than half the maximum vis in its chunk.

## 1.0.0-rc.2

First public release.
