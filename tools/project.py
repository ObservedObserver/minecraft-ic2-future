#!/usr/bin/env python3
"""Portable entry point for the two independent Gradle projects."""
import argparse
import hashlib
import io
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import zipfile

ROOT = Path(__file__).resolve().parents[1]
PROJECTS = {
    "ic2": ROOT / "mods/ic2",
    "twilightforest": ROOT / "mods/twilightforest",
    "validation": ROOT / "validation/twilightforest",
}


def gradle(target, tasks, extra):
    project = PROJECTS[target]
    env = dict(os.environ)
    env.setdefault("GRADLE_USER_HOME", str(ROOT / ".cache/gradle"))
    java = Path(env["JAVA_HOME"]) / "bin/java" if env.get("JAVA_HOME") else shutil.which("java")
    if not java:
        raise SystemExit("Java 25 is required; set JAVA_HOME to your JDK 25 directory.")
    version = subprocess.run([str(java), "-version"], capture_output=True, text=True)
    if version.returncode or 'version "25' not in version.stderr + version.stdout:
        raise SystemExit("Set JAVA_HOME to a JDK 25 installation before building.")
    wrapper = [str(project / "gradlew.bat")] if os.name == "nt" else ["/bin/sh", str(project / "gradlew")]
    command = wrapper + ["--no-daemon", "--console=plain", "--max-workers=2",
                         "-Dorg.gradle.java.installations.auto-download=false"] + tasks + extra
    print(f"[{target}] {' '.join(tasks + extra)}", flush=True)
    subprocess.run(command, cwd=project, env=env, check=True)


def check():
    lock = json.loads((ROOT / "pack/mods.lock.json").read_text())
    assert lock["minecraft"] == "26.3" and lock["neoforge"] == "26.3.0.83"
    assert len(lock["mods"]) == 8
    assert len({m["file"] for m in lock["mods"]}) == 8
    for target, project in PROJECTS.items():
        for name in ["build.gradle", "settings.gradle", "gradlew", "gradle/wrapper/gradle-wrapper.jar"]:
            assert (project / name).is_file(), (target, name)
        assert not (project / ".git").exists(), f"Nested repository: {project}"
    for target in ["ic2", "twilightforest"]:
        assert re.search(r"(?m)^mod_version=\S+$", (PROJECTS[target] / "gradle.properties").read_text())
    parsed = 0
    for base in [ROOT / "mods", ROOT / "validation"]:
        for source in base.rglob("*.json"):
            if any(p in {"build", ".gradle", ".cache"} or p.startswith("run-") for p in source.parts):
                continue
            json.loads(source.read_text(encoding="utf-8"))
            parsed += 1
    print(f"Workspace checks passed; {parsed} JSON files parsed, 8 release mods locked.")


def verify_release(location):
    lock = json.loads((ROOT / "pack/mods.lock.json").read_text())
    def verify(data, name, expected):
        if hashlib.sha256(data).hexdigest() != expected:
            raise SystemExit(f"Release hash mismatch: {name}")
        with zipfile.ZipFile(io.BytesIO(data)) as jar:
            if jar.testzip() is not None:
                raise SystemExit(f"Corrupt JAR: {name}")
    if location.is_file():
        if hashlib.sha256(location.read_bytes()).hexdigest() != lock["package_sha256"]:
            raise SystemExit(f"Release ZIP hash mismatch: {location}")
        with zipfile.ZipFile(location) as bundle:
            for mod in lock["mods"]:
                entries = [n for n in bundle.namelist() if n.endswith("/" + mod["file"])]
                if len(entries) != 1:
                    raise SystemExit(f"Expected exactly one {mod['file']} in ZIP")
                verify(bundle.read(entries[0]), entries[0], mod["sha256"])
    else:
        for mod in lock["mods"]:
            file = location / Path(mod["file"]).name
            if not file.is_file():
                raise SystemExit(f"Missing release artifact: {file}")
            verify(file.read_bytes(), str(file), mod["sha256"])
    print("All 8 JARs match the recorded delivery. Rebuilt JARs may have different hashes.")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest="command", required=True)
    sub.add_parser("check")
    verify = sub.add_parser("verify-release")
    verify.add_argument("location", type=Path, help="Historical ZIP or extracted mods directory")
    for name in ["build", "test", "gametest", "client", "gradle"]:
        item = sub.add_parser(name)
        choices = ["ic2"] if name == "gametest" else list(PROJECTS)
        if name in {"build", "test"}:
            choices += ["all"]
        item.add_argument("target", choices=choices)
        item.add_argument("extra", nargs=argparse.REMAINDER)
    args = parser.parse_args()
    if args.command == "check":
        check()
        return
    if args.command == "verify-release":
        verify_release(args.location)
        return
    targets = list(PROJECTS) if args.target == "all" else [args.target]
    extra = args.extra[1:] if args.extra[:1] == ["--"] else args.extra
    for target in targets:
        if target == "validation":
            gradle("twilightforest", [":asm:classes"], [])
        if args.command == "build":
            tasks = ["assemble", "shadowJar"] if target == "twilightforest" else ["assemble"]
        elif args.command == "test":
            tasks = ["test"]
        elif args.command == "gametest":
            tasks = ["runGameTestServer", "-Pic2TestFilter=*", "-Pic2TestReport=build/reports/gametest.xml"]
        elif args.command == "client":
            if target == "validation":
                raise SystemExit("The independent validation project has no playable client.")
            tasks = ["runClient"]
        else:
            tasks = []
            if not extra:
                raise SystemExit("Provide Gradle tasks after the target, e.g. gradle ic2 tasks.")
        gradle(target, tasks, extra)


if __name__ == "__main__":
    main()
