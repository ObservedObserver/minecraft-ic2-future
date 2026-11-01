# Minecraft 26.3 模组维护仓库

本整合包系列的源码仓库，集中维护 IC2 Refactored 和暮色森林的 Minecraft 26.3 / NeoForge 26.3.0.83 移植。初始维护基线为 IC2 local17、暮色森林 local5，使用 Java 25。

两个模组的完整源码、必要生成资源及测试直接提交在本仓库中，没有子模块，也不依赖原 Codex 任务目录。此前发布的 JAR 与整合包 ZIP 通过版本清单和 SHA-256 追踪，不放进 Git。

```text
mods/ic2/                  IC2 全部源码、资源、GameTests、JUnit 与 Gradle 工程
mods/twilightforest/        暮色森林全部源码、资源、ASM 子工程与 JUnit
validation/twilightforest/  密度、地形、资源及原生 ASM 挂钩独立验证工程
mods/logistics/            独立物流模组的美术资源草稿，尚无可运行物流 JAR
tools/project.py           统一构建与检查入口
tools/migration-archive/    一次性迁移脚本档案，不作为日常构建步骤
pack/mods.lock.json        已交付八模组版本、来源与哈希
docs/history/              历次修复报告与关键验证记录
docs/provenance/           上游基线、导入时的逐文件哈希与来源
```

安装 JDK 25 并设置 `JAVA_HOME` 后，在仓库根目录执行：

```sh
./dev check
./dev build all
./dev test all
./dev gametest ic2
```

首次 Gradle 构建会下载依赖。默认缓存集中在 `.cache/gradle`，可用 `GRADLE_USER_HOME` 改到其他磁盘。输出分别在各工程 `build/` 下。

入口文档：

新 agent session 先读 [AGENTS.md](AGENTS.md)，其中包含累计定制、开发验收流程和多 session 协作约定。跨会话任务可使用 [交接模板](docs/session-template.md)。

- [开发、测试与构建](docs/development.md)
- [累计修改与版本](docs/changes.md)
- [已知问题与验证边界](docs/known-issues.md)
- [整合包发布流程](docs/releases.md)
- [仓库整理与核对结果](docs/consolidation.md)
- [物流资源首版与美术评审](mods/logistics/README.md)
- [特斯拉远程输电线圈设计草案，未实现](docs/design/wireless-power.md)
- [原作者与许可](NOTICE.md)

旧任务目录继续保留作历史资料；后续维护以本仓库为准。这里只创建了本地 Git 仓库，没有配置推送目标。
