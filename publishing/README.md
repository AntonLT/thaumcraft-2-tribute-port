# Publish on Modrinth and CurseForge

The release workflow uploads the jars to Modrinth and CurseForge after it creates the GitHub release. It publishes to a platform only after you create the project there and give the workflow its ID. Until then, releases go to GitHub only.

## Create the projects

Create one project per mod on each site, with the values below. Both sites review new projects before they become public.

| Field | Thaumcraft 2 Tribute Port | Alt's Thaumcraft 2 Tribute Port patches |
| --- | --- | --- |
| Slug | `thaumcraft-2-tribute-port` | `alts-tc2tp-patches` |
| Summary | Azanor's Thaumcraft 2 on Minecraft 26.1.2: research, vis, infusion, arcane seals and taint, kept as close to the original as possible. | Alt's own balance changes for the Thaumcraft 2 Tribute Port: values for modern items, extra research, deepslate cinnabar. |
| Description | [description.md](description.md) | [alts-patches.md](alts-patches.md) |
| Icon | `common/src/main/resources/assets/thaumcraft2tp/icon.png` | The same icon, or none |
| Modrinth categories | Magic, Technology, Worldgen | Magic, Game Mechanics |
| CurseForge categories | Magic, Technology, World Gen | Magic |
| Loaders | Fabric, NeoForge | Fabric, NeoForge |
| Client and server | Required on both | Required on both |
| License | `LicenseRef-Thaumcraft-2-Tribute-Port`, linked to `LICENSE` | GPL-3.0-or-later |
| Source and issues | `https://github.com/AntonLT/thaumcraft-2-tribute-port` | The same repository |

Use the same slug on both sites. The addons declare their dependency on the core mod by the slug `thaumcraft-2-tribute-port`.

Upload the images in [gallery](gallery) to the core project's gallery. They are 1600×900 screenshots from the NeoForge client.

## Connect the workflow

1. Create a Modrinth personal access token with the **Create versions** and **Read projects** scopes.
2. Create a CurseForge API token in the CurseForge author console.
3. In the GitHub repository, open **Settings > Secrets and variables > Actions**.
4. Add the secrets `MODRINTH_TOKEN` and `CURSEFORGE_TOKEN`.
5. Add the variables below with each project's ID. On Modrinth, the ID is on the project page. On CurseForge, it is the **Project ID** in the project's side panel.

| Variable | Project |
| --- | --- |
| `MODRINTH_ID` | Thaumcraft 2 Tribute Port on Modrinth |
| `CURSEFORGE_ID` | Thaumcraft 2 Tribute Port on CurseForge |
| `ALTS_MODRINTH_ID` | Alt's patches on Modrinth |
| `ALTS_CURSEFORGE_ID` | Alt's patches on CurseForge |

Leave a variable empty to skip that platform.

## Release

To release the port, push a tag named after `version` in `gradle.properties`, for example `v1.0.0-rc.3`. To release Alt's patches, push a tag named after `addon_version` in `examples/alts-tc2tp-patches/addon.gradle`, for example `alts-v1.0.0`.

Each upload is named `<mod> <version> (Fabric)` or `(NeoForge)`, with the version number `<version>+fabric` or `<version>+neoforge`. A version with a suffix such as `-rc.3`, or below 1.0, is published as a beta. The changelog links to the GitHub release.

If an upload fails, open the run in the **Actions** tab and select **Re-run failed jobs**. The GitHub release is not created twice. A tag pushed before this workflow existed is not published; upload its jars by hand or release a new version.
