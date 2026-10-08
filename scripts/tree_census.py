#!/usr/bin/env python3
"""Measure natural greatwood and silverwood density per 1,000 land chunks for habitat variants.

Each variant overrides the habitat tags with a datapack, so the shipped code runs unchanged.
Runs are resumable: an existing .cache/tree-census/<variant>-<seed>.log is reused.
"""
import argparse
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys

root = Path(__file__).resolve().parents[1]
out = root / ".cache" / "tree-census"
FOREST_VARIANTS = ["minecraft:flower_forest", "minecraft:dark_forest", "minecraft:pale_garden", "minecraft:grove",
                   "minecraft:old_growth_pine_taiga", "minecraft:old_growth_spruce_taiga"]
VARIANTS = {
    "F": {},
    "A": {"silverwood_habitat": (False, ["#minecraft:is_jungle"]), "silverwood_excluded": (True, [])},
    "B": {"silverwood_excluded": (True, [])},
    "E": {"silverwood_excluded": (True, FOREST_VARIANTS),
          "greatwood_habitat": (True, ["minecraft:forest", "minecraft:birch_forest", "minecraft:old_growth_birch_forest",
                                       "minecraft:taiga", "minecraft:snowy_taiga", "minecraft:plains"])},
}


def pack(variant):
    tags = VARIANTS[variant]
    if not tags:
        return None
    directory = out / "packs" / ("tree-census-" + variant.lower())
    shutil.rmtree(directory, ignore_errors=True)
    meta = json.loads((root / "examples/addon-api/pack.mcmeta").read_text())
    meta["pack"]["description"] = "Tree census variant " + variant
    (directory / "data/thaumcraft2tp/tags/worldgen/biome").mkdir(parents=True)
    (directory / "pack.mcmeta").write_text(json.dumps(meta, indent=2) + "\n")
    for name, (replace, values) in tags.items():
        (directory / "data/thaumcraft2tp/tags/worldgen/biome" / (name + ".json")).write_text(json.dumps({"replace": replace, "values": values}) + "\n")
    return directory


def run(variant, seed, args, shares):
    log = out / f"{variant}-{seed}.log"
    if log.exists():
        return log
    world = root / args.loader / "runs/server/world"
    if world.exists() and not args.archive_world:
        raise SystemExit("Refusing to reuse " + str(world) + "; pass --archive-world")
    command = [sys.executable, str(root / "scripts/smoke_server.py"), args.loader, "--java-home", args.java_home,
               "--seed", seed, "--timeout", "2400"]
    if world.exists():
        command.append("--archive-world")
    directory = pack(variant)
    if directory:
        command += ["--datapack", str(directory)]
    env = os.environ.copy()
    env.pop("JDK_JAVA_OPTIONS", None)
    # A settings file instead of JVM options lets Gradle reuse its daemon between runs.
    settings = root / args.loader / "runs/server/tree-census.properties"
    settings.write_text(f"radius={args.radius}\nthreads={args.threads}\nshares={str(shares).lower()}\n")
    print(f"variant {variant} seed {seed}", flush=True)
    try:
        result = subprocess.run(command, cwd=root, env=env)
    finally:
        settings.unlink()
    smoke_log = root / ".cache" / (args.loader + "-smoke.log")
    if result.returncode:
        raise SystemExit(f"census run failed; see {smoke_log}")
    shutil.copy(smoke_log, log)
    shutil.rmtree(world)
    return log


def number(text):
    return float(text.replace(",", "."))


def parse(log):
    shares, land, yields = {}, None, {}
    for line in log.open(errors="replace"):
        if m := re.search(r"TREE_CENSUS shares samples=\d+ land=([\d,.]+)", line):
            land = number(m.group(1))
        if m := re.search(r"TREE_CENSUS share (\S+) all=\S+ land=([\d,.]+)", line):
            shares[m.group(1)] = number(m.group(2))
        if m := re.search(r"TREE_CENSUS yield (\S+) chunks=([\d,.]+) greatwood=(\d+) silverwood=(\d+)", line):
            yields[m.group(1)] = (number(m.group(2)), int(m.group(3)), int(m.group(4)))
    return shares, land, yields


def density(shares, yields):
    silverwood = sum(shares.get(b, 0) * s / c for b, (c, g, s) in yields.items() if c > 0)
    greatwood = sum(shares.get(b, 0) * g / c for b, (c, g, s) in yields.items() if c > 0)
    return 1000 * silverwood, 1000 * greatwood


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--java-home", required=True)
    parser.add_argument("--loader", default="fabric", choices=["fabric", "neoforge"])
    parser.add_argument("--variants", default="F,B,E", help="F ships; A is the pre-change tags; B and E are alternatives")
    parser.add_argument("--seeds", default="216")
    parser.add_argument("--radius", type=int, default=4)
    parser.add_argument("--threads", type=int, default=12, help="Patches generated at once")
    parser.add_argument("--archive-world", action="store_true", help="Retain an existing development world before the first run")
    args = parser.parse_args()
    out.mkdir(parents=True, exist_ok=True)
    variants, seeds = args.variants.split(","), args.seeds.split(",")
    have_shares = any(parse(log)[0] for log in out.glob("*.log"))
    logs = {}
    for v in variants:
        for s in seeds:
            logs[(v, s)] = run(v, s, args, shares=not have_shares)
            have_shares = True
    shares, land = {}, None
    for log in out.glob("*.log"):
        candidate, candidate_land, _ = parse(log)
        if candidate:
            shares, land = candidate, candidate_land
            break
    print(f"land fraction {land:.3f}; per 1,000 land chunks; per-seed values in brackets")
    for v in variants:
        pooled = {}
        per_seed = []
        for s in seeds:
            _, _, yields = parse(logs[(v, s)])
            per_seed.append(density(shares, yields))
            for b, (c, g, w) in yields.items():
                total = pooled.setdefault(b, [0.0, 0, 0])
                total[0] += c
                total[1] += g
                total[2] += w
        silverwood, greatwood = density(shares, {b: tuple(t) for b, t in pooled.items()})
        seeds_text = " ".join(f"[{sw:.1f} {gw:.1f}]" for sw, gw in per_seed)
        print(f"{v}: silverwood {silverwood:5.1f} greatwood {greatwood:5.1f} ratio {greatwood / silverwood:4.2f} "
              f"world {silverwood * land:4.1f} {greatwood * land:4.1f}  seeds {seeds_text}")


if __name__ == "__main__":
    main()
