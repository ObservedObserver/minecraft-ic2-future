# Maintenance instructions

- Maintain the actual mod sources in `mods/ic2` and `mods/twilightforest`. These are ordinary directories in this repository, not submodules.
- Target Minecraft 26.3, NeoForge 26.3.0.83 and Java 25 unless a task explicitly changes the target.
- Keep each mod's license and upstream attribution. Do not apply a single license to all imported sources or assets.
- Use `./dev` for build and test commands. See `docs/development.md` for the distinction between JUnit, native GameTests and client verification.
- Update the relevant mod version for future releases. Keep historical release hashes in `pack/mods.lock.json` unchanged until a new release is actually verified.
- Commit source, required generated game resources, tests and documentation. Do not commit Gradle caches, worlds, credentials, dependency jars or modpack ZIPs. Gradle wrapper jars are intentional exceptions.
- Do not rerun `tools/migration-archive` scripts against current sources. They record one-time migrations and refer to the former workspace.
- Write documentation as Markdown in this repository. Keep known problems and unverified behavior explicit in `docs/known-issues.md`.
