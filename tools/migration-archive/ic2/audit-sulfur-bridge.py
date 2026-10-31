#!/usr/bin/env python3
"""Check sulfur conservation against the actual 26.3 and built IC2 recipe resources."""
import argparse
from collections import Counter
from fractions import Fraction
import hashlib
import json
from pathlib import Path
import zipfile


ROOT = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("--ic2-jar", type=Path, required=True)
parser.add_argument("--output", type=Path, required=True)
args = parser.parse_args()
vanilla = ROOT / ".gradle-user-home/caches/neoformruntime/artifacts/minecraft_26.3_client.jar"

# Units are one IC2 sulfur dust. A spike is a quarter block; a slab is half a block.
mass = {
    "ic2:sulfur_dust": Fraction(1),
    "ic2:small_sulfur_dust": Fraction(1, 9),
    "minecraft:sulfur_spike": Fraction(9, 4),
    "minecraft:potent_sulfur": Fraction(81),
}
for name in (
    "sulfur", "polished_sulfur", "sulfur_bricks", "chiseled_sulfur",
    "sulfur_stairs", "polished_sulfur_stairs", "sulfur_brick_stairs",
    "sulfur_wall", "polished_sulfur_wall", "sulfur_brick_wall",
):
    mass["minecraft:" + name] = Fraction(9)
for name in ("sulfur_slab", "polished_sulfur_slab", "sulfur_brick_slab"):
    mass["minecraft:" + name] = Fraction(9, 2)


def item(value):
    if isinstance(value, str):
        return value, 1
    if not isinstance(value, dict):
        return None, 1
    return value.get("item", value.get("id")), value.get("count", 1)


def ingredients(recipe):
    kind = recipe["type"]
    if kind.endswith(":crafting_shaped") or kind == "ic2:shaped":
        counts = Counter("".join(recipe["pattern"]).replace(" ", ""))
        return [(item(recipe["key"][key])[0], count) for key, count in counts.items()]
    if "ingredients" in recipe:
        return [item(value) for value in recipe["ingredients"]]
    if "ingredient" in recipe:
        return [item(recipe["ingredient"])]
    return []


checks = []
for path in (vanilla, args.ic2_jar):
    with zipfile.ZipFile(path) as archive:
        for name in sorted(archive.namelist()):
            if not (name.startswith("data/") and "/recipe/" in name and name.endswith(".json")):
                continue
            recipe = json.loads(archive.read(name))
            # Check every recipe whose entire input is within the sulfur family, including
            # vanilla cutting/crafting and IC2 small-dust consolidation. External sulfur
            # sources such as gunpowder extraction are outside this closed-material audit.
            inputs = ingredients(recipe)
            if not inputs or not all(key in mass for key, _ in inputs):
                continue
            outputs = recipe.get("result", [])
            if not isinstance(outputs, list):
                outputs = [outputs]
            assert not recipe.get("weighted"), (name, "unexpected random sulfur recipe")
            outputs = [item(value) for value in outputs]
            assert outputs and all(key in mass for key, _ in outputs), (name, outputs)
            before = sum(mass[key] * count for key, count in inputs)
            after = sum(mass[key] * count for key, count in outputs)
            assert after <= before, (name, "sulfur gain", str(before), str(after))
            checks.append({"recipe": name, "input_dust_units": str(before),
                           "output_dust_units": str(after)})

with zipfile.ZipFile(args.ic2_jar) as archive:
    block = json.loads(archive.read("data/ic2/recipe/shaped/sulfur_block_from_sulfur_dust.json"))
    grind = json.loads(archive.read("data/ic2/recipe/macerator/sulfur_to_sulfur_dust.json"))
    spikes = json.loads(archive.read("data/ic2/recipe/macerator/sulfur_spikes_to_sulfur_dust.json"))
    assert ingredients(block) == [("ic2:sulfur_dust", 9)]
    assert item(block["result"]) == ("minecraft:sulfur", 1)
    assert ingredients(grind) == [("minecraft:sulfur", 1)]
    assert item(grind["result"]) == ("ic2:sulfur_dust", 9)
    assert ingredients(spikes) == [("minecraft:sulfur_spike", 4)]
    assert item(spikes["result"]) == ("ic2:sulfur_dust", 9)
    # Blocks must never be admitted as dust by the shared ingredient tag.
    tag = json.loads(archive.read("data/c/tags/item/dusts/sulfur.json"))
    assert tag["values"] == ["ic2:sulfur_dust"], tag
    assert not any(name.startswith("data/minecraft/recipe/") for name in archive.namelist())

record = {
    "status": "PASS", "ic2_jar": str(args.ic2_jar.resolve()),
    "ic2_sha256": hashlib.sha256(args.ic2_jar.read_bytes()).hexdigest(),
    "vanilla_sha256": hashlib.sha256(vanilla.read_bytes()).hexdigest(),
    "scope": "Shipped vanilla 26.3 and IC2 recipes with only sulfur-family inputs; no optional mods or natural growth",
    "proof": "Every checked transformation preserves or reduces a positive sulfur mass, so compositions cannot increase it",
    "checked_recipes": checks,
}
args.output.parent.mkdir(parents=True, exist_ok=True)
args.output.write_text(json.dumps(record, indent=2) + "\n")
print(f"PASS: {len(checks)} sulfur-only recipes conserve material; tag and bridge ratios checked")
