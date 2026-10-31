# 开发、测试与构建

## 环境

目标是 Minecraft 26.3、NeoForge 26.3.0.83、JDK 25。三个工程使用各自已提交的 Gradle 9.2.1 wrapper；不需要全局安装 Gradle。

设置 `JAVA_HOME` 为 JDK 根目录，并确保 Python 3 可用。`./dev` 只用 Python 标准库。Windows 可直接执行 `python tools/project.py ...`，也可以进入对应工程运行 `gradlew.bat`。

默认依赖缓存位于仓库 `.cache/gradle`，各模组共享它。首次构建需要网络和数 GB 临时空间，建议至少保留 8 GiB 空闲空间。可以把 `GRADLE_USER_HOME` 设到其他磁盘，缓存和游戏测试世界均不提交 Git。

## 日常命令

```sh
./dev check
./dev build ic2
./dev build twilightforest
./dev test ic2
./dev test twilightforest
./dev test validation
./dev gametest ic2
./dev client ic2
./dev client twilightforest
```

`build all` 按顺序组装两个模组和独立验证工程。IC2 使用 `assemble`；暮色森林使用 `assemble shadowJar`，交付应取带 `universal` 的 JAR，因为它包含 ASM 子包和迁移所需依赖。两个 Gradle 工程保持独立，避免合并模组 ID、映射环境及资源处理流程。

`test all` 按顺序运行三个工程的 JUnit。独立验证工程先构建暮色森林的 ASM 类并执行 `runClientData`，再加载真实迁移代码，检查数据生成输出与随包资源一致；不依赖旧任务目录残留的生成文件。

`gametest ic2` 使用带命名空间的 `ic2:*` 选择器执行原生游戏测试，并将 XML 写到 `mods/ic2/build/reports/gametest.xml`。入口还检查报告确实含有 IC2 测试，避免只运行原版 `always_pass` 就误报通过。这些测试与 `src/test` 下的 JUnit 是不同的一组；单独运行 JUnit 不能替代它们。

额外参数原样传给 Gradle：

```sh
./dev test twilightforest -- --tests '*OitRenderRegressionTests'
./dev gametest ic2 -- '-Pic2TestFilter=ic2:itemroutinggametests.*'
./dev gradle ic2 tasks
./dev gradle twilightforest runClientData
```

JAR 输出：

- `mods/ic2/build/libs/ic2-neoforge-<mod_version>.jar`
- `mods/twilightforest/build/libs/twilightforest-26.3-<mod_version>-universal.jar`

## 修改和数据生成

直接修改 `mods/<模组>/src`。IC2 的原生回归在 `src/main/java/ic2/core/gametest`，保持与现有运行注册方式一致。暮色森林 ASM 在 `src/asm`，由 `asm/` 子工程打包。

必要的 `src/generated/resources` 已提交。修改数据生成逻辑后，执行相应数据生成任务并审核结果，不能只改生成器而漏掉实际随包数据。暮色森林当前 `runClientData` 默认输出到 `build/generated-port-resources`，这是迁移时为保护资源设置的行为。将预期变更审核并同步回 `src/generated/resources` 后再提交，不能直接用输出目录覆盖整个资源树。

不要对两个模组一次性运行全树格式化。已有迁移差异较大，格式化会掩盖实质改动。需要格式化时限制到本次修改的文件。

查看相对上游快照的累计差异：

```sh
git diff upstream-baselines -- mods/ic2
git diff upstream-baselines -- mods/twilightforest
git log --oneline -- mods/ic2
```

`upstream-baselines` 对应两个原仓库 HEAD 的源码快照，并未合并上游的完整提交图。具体原提交、日期和说明保存在来源记录中；本仓库自己的后续提交可以正常按文件追踪。

## 验证层次

1. `check` 检查仓库布局、资源 JSON 和版本清单。
2. 完整源码编译与 JUnit 检查链接、资源和已覆盖的行为。
3. IC2 原生 GameTests 验证实际游戏 API 与机器执行路径。
4. 隔离客户端验证界面、输入网络、渲染和完整八模组联动。已有自动客户端探针属于历史验证，不会自动包含在普通 `runClient` 中。
5. 涉及物品、能量、过滤配置或区块票据时，需要真实保存、关闭并重新启动。

历史报告里有旧绝对路径，它们只用于来源追溯；当前构建入口和三个 Gradle 工程应使用仓库相对路径。`tools/migration-archive` 不是可重复执行的升级工具，不能将其作为构建前置任务。
