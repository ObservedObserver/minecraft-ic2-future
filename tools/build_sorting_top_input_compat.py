#!/usr/bin/env python3
"""Apply the fully compiled sorting change to the verified local21 binary, preserving all other entries.

Temporary compatibility delivery while the local18-21 sources are absent from this repository.
This is not a replacement for merging the change into the complete current source tree.
"""
import argparse
import copy
import hashlib
import json
from pathlib import Path
import zipfile

BASE_SHA256 = '240878e16276fafdf5aaec1b6cb36e6a08bedc2915f66a332ee57ab904c81913'
VERSION = '26.3.0-local.21-topinput.1'
CLASSES = [
    'ic2/core/block/machine/tileentity/TileEntitySortingMachine.class',
    'ic2/core/block/machine/container/ContainerSortingMachine.class',
    'ic2/core/block/machine/gui/GuiSortingMachine.class',
    'ic2/core/block/machine/gui/GuiSortingMachine$1.class',
    'ic2/core/block/machine/gui/GuiSortingMachine$2.class',
    'ic2/core/block/machine/gui/GuiSortingMachine$3.class',
    'ic2/core/gametest/ItemRoutingGameTests.class',
]


def sha(data):
    return hashlib.sha256(data).hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('base', type=Path)
    parser.add_argument('compiled', type=Path)
    parser.add_argument('output', type=Path)
    args = parser.parse_args()
    if args.output.resolve() in {args.base.resolve(), args.compiled.resolve()}:
        raise SystemExit('Output must be a separate file, never the input or installed JAR.')
    if sha(args.base.read_bytes()) != BASE_SHA256:
        raise SystemExit('This compatibility patch supports only the byte-verified local21 baseline.')
    with zipfile.ZipFile(args.base) as base, zipfile.ZipFile(args.compiled) as compiled:
        if base.testzip() or compiled.testzip():
            raise SystemExit('An input JAR failed CRC validation.')
        replacements = {name: compiled.read(name) for name in CLASSES}
        metadata = base.read('META-INF/neoforge.mods.toml').decode()
        old = 'version="26.3.0-local.21"'
        if metadata.count(old) != 1:
            raise SystemExit('Unexpected baseline mod version metadata.')
        replacements['META-INF/neoforge.mods.toml'] = metadata.replace(
            old, 'version="' + VERSION + '"').encode()
        for language in ['zh_cn', 'en_us']:
            name = 'assets/ic2/lang/' + language + '.json'
            document = json.loads(base.read(name))
            document['ic2.SortingMachine.input'] = json.loads(compiled.read(name))[
                'ic2.SortingMachine.input']
            replacements[name] = (json.dumps(document, ensure_ascii=False, indent=2) + '\n').encode()
        args.output.parent.mkdir(parents=True, exist_ok=True)
        temporary = args.output.with_suffix('.jar.tmp')
        with zipfile.ZipFile(temporary, 'w', compression=zipfile.ZIP_DEFLATED) as output:
            for info in base.infolist():
                output.writestr(copy.copy(info), replacements.get(info.filename, base.read(info.filename)))
            for name in replacements.keys() - set(base.namelist()):
                output.writestr(name, replacements[name])
        unchanged = []
        with zipfile.ZipFile(temporary) as output:
            if output.testzip():
                raise SystemExit('Output JAR CRC validation failed.')
            assert not any('SortingRestartProbe' in name or 'sortingprobe' in name
                           for name in output.namelist())
            for name in set(base.namelist()) - replacements.keys():
                assert output.read(name) == base.read(name), name
                unchanged.append(name)
            for name, data in replacements.items():
                assert output.read(name) == data, name
        temporary.replace(args.output)
        report = {
            'version': VERSION,
            'method': 'Full source build of the sorting change, restricted local21 compatibility overlay',
            'baseline_sha256': BASE_SHA256,
            'compiled_source_jar_sha256': sha(args.compiled.read_bytes()),
            'result_sha256': sha(args.output.read_bytes()),
            'unchanged_entries_verified_equal': len(unchanged),
            'replacement_sha256': {name: sha(data) for name, data in replacements.items()},
            'limitations': 'Complete local21 source is not in this repository; merge upstream before a future normal release.',
        }
        args.output.with_suffix('.build.json').write_text(json.dumps(report, indent=2) + '\n')
        print(json.dumps({k: v for k, v in report.items() if k != 'replacement_sha256'}, indent=2))


if __name__ == '__main__':
    main()
