# Build and development

## Requirements

- Java 25 JDK, with `JAVA_HOME` pointing to it and its `bin` directory on `PATH`.
- Internet access for the first Gradle dependency download.
- Python 3 for the localization check that runs during `./gradlew build`, and for the optional server runner.

The pinned Minecraft and loader versions are in `gradle.properties`: Minecraft 26.1.2, Fabric Loader 0.19.5, Fabric API 0.155.3+26.1.2 and NeoForge 26.1.2.107.

## Runtime JARs

```bash
./gradlew :fabric:build :neoforge:build --console=plain
```

Loader outputs are placed in `fabric/build/libs/` and `neoforge/build/libs/`. Install the normal runtime JAR for the chosen loader. The `common` project is shared build input and is not an installable mod by itself.

Always make the final build without `-PsmokeTest` or `-PclientSmoke`. Those properties add integration fixtures to a development source set. The fixtures are omitted from normal builds.

Shared server fixtures live in `common/src/smokeTest/java`, loader-specific ones in `fabric/src/smokeTest/java` and `neoforge/src/smokeTest/java`, and client fixtures in `common/src/clientTest/java`. They are opt-in game launches rather than plain JVM unit tests.

## Launch a development instance

```bash
./gradlew :fabric:runClient --console=plain
./gradlew :neoforge:runClient --console=plain
```

Each loader uses its own `runs/client` directory. Servers use `runs/server` under that loader. Run one Gradle game launch at a time to keep logs, build outputs and ports unambiguous.

For item and recipe inspection, put JEI for Minecraft 26.1.2 in the loader's `runs/client/mods` directory. Thaumcraft adds its infusion and research views when JEI is present.

## Isolated server assertions

The runner is written for Linux.

```bash
python3 scripts/smoke_server.py fabric --java-home "$JAVA_HOME"
python3 scripts/smoke_server.py neoforge --java-home "$JAVA_HOME"
```

The runner prepares a local test server with seed 216, offline authentication and loopback-only binding. Fabric uses port 25575 and NeoForge uses 25576. It sets `eula=true` for that test instance, runs `ServerSmokeTests`, sends `stop` after the pass marker and retains the complete `.cache/<loader>-smoke.log`.

The runner refuses to reuse an existing `runs/server/world`. Move that test world to a new retained name before another fresh run, or pass `--archive-world` to retain it under a timestamped name automatically. Previous test logs are also retained. Do not point this fixture at a world you play: its assertions place machines, trees and an Eldritch chamber, modify research and aura, and inspect saved data. A pass requires both the test marker and a successful process exit.

## Client fixture

Create a disposable singleplayer world named `ThaumcraftQA` in the loader's development client first, then launch:

```bash
./gradlew :fabric:runClient -PclientSmoke --console=plain
./gradlew :neoforge:runClient -PclientSmoke --console=plain
```

The fixture opens that world, builds the machine/entity exhibition, renders near and distant paired-seal portals, and validates 272 block/state cases covering all 70 registered blocks. The distant portal checks actual chunk, lighting, block entity, creature and item packets at a destination 8,192 blocks away, outside the player's chunk cache. It checks finite local geometry, all crystal colors/counts/mounting directions, bore/pump/seal orientations, fluid variants and precision at the world border. It then opens 19 original machine and item screens, validates synchronized slots and controls, and checks the detector/carpet HUD. The linked-interface test sends the normal page-change packet and verifies that the second backing chest is displayed. The destructive active singularity is excluded from the exhibition.

If the Linux host cannot allocate graphics buffers, the validation clients can use Mesa software OpenGL without changing the mod or system configuration:

```bash
LIBGL_ALWAYS_SOFTWARE=true LIBGL_DRI3_DISABLE=1 GALLIUM_DRIVER=llvmpipe \
  __GLX_VENDOR_LIBRARY_NAME=mesa ./gradlew :fabric:runClient -PclientSmoke --console=plain
```

Use `:neoforge:runClient` for the other loader. Software rendering is suitable for these correctness checks; it does not measure normal GPU performance.

Images are retained under each instance's `screenshots` directory: the exhibition, near/far portal, direct 512×512 far destination framebuffer, state matrix, compact enchanter, linked interface, and `thaumcraft-gui-<screen>.png` for every GUI/HUD case. The fixture requests a fixed 1120×840 window and changes GUI scale for the compact check; the window manager may cap its height. Inspect those images and the client log together; a render pass marker alone does not prove visual parity. `-PclientSmoke -PguiSmoke` runs the GUI section alone for debugging.

## Releases

GitHub Actions builds only release tags. To publish a release, set `version` in `gradle.properties`, commit, and push a matching tag:

```bash
git tag v1.0.0-rc.2
git push origin v1.0.0-rc.2
```

`.github/workflows/release.yml` checks that the tag matches the version, builds, and creates a GitHub release with the Fabric and NeoForge JARs and Alt's patches. Versions with a suffix such as `-rc.2` are marked as pre-releases. The release notes are generated from the commits since the previous tag; edit them on GitHub afterwards.

