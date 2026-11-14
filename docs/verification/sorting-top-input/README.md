# 分拣机正上方箱子主动取料

## 范围

用户明确要求：不使用漏斗，直接从正上方箱子主动吸入，再按东方过滤规则输出。

本次基于用户指定 GitHub 仓库的 `c238ea1f1fe5ed5a3c33bbe9f2baa74d243fb4e3`，原源码版本为 `26.3.0-local.17`。本地已安装的客户端为 local21，其最新源码不在本仓库中。本次开发版本单独标为 `26.3.0-local.17-topinput.1`，不冒充 local22，不替换玩家或房主正在使用的 local21，不推送远端。

## 行为

- 服务器每刻先检查至少 20 EU，再从正上方紧贴的原版单箱或双箱取入最多 1 个物品。合并时严格匹配现代物品组件，缓冲区无空间则不移除源物品。
- 随后沿用其他五个方向的输出逻辑。东方配置过滤物品和每批数量后，匹配物品输出到东侧容器。取料不额外扣电，成功输出仍为每个物品 20 EU。
- 上方固定为输入，不回送物品；界面上方改为输入提示并移除该行可编辑过滤槽。旧上方默认出口载入时迁移为下方，旧上方过滤样品仍保存在 NBT 中但不参与分流；其他方向规则保留。
- 东侧塞满时物品可暂存缓冲区，既不泄漏到默认口，也不扣输出电量。缓冲区满后停止取料。
- 不主动吸取其他方向容器或地面掉落物；原有漏斗、主动管道输入保留。不扩展共有存储适配器，不承诺任意模组专用存储兼容。

## 本次回归

`ItemRoutingGameTests` 新增 7 项外部结果断言：

1. 真实服务器自然游戏刻从上方箱子取料并向东方输出，检查源、缓冲区、目标、默认出口和耗电。
2. 19/20 EU 供电边界，以及带名称物品的组件保留。
3. 每批 4 个的规则：只有 3 个时等待，第 4 个到达后整批输出。
4. 东侧满箱时保留缓冲、无扣电、无回送；腾出空间后物品总数与耗电正确。
5. 缓冲区满时不取料；有可合并的部分堆叠时继续，检查物品总数。
6. 从上方双箱另一半取料并输出。
7. 旧上方默认出口和过滤规则的 NBT 重载、迁移，以及旧上方按钮事件不能恢复上方输出。

完整编译、JUnit、原生 GameTests、旧行为见证及实际重启的结果见下文；原始过程日志保存在任务临时目录，必要 XML 和结果摘要已归档到本目录。

## 尚未执行

房主实际环境验证和完整八模组客户端界面观察。本机不持有房主存档或服务端源码，未操作房主进程、网络代理、玩家配置或存档。实际保存退出重启已在隔离普通服务器完成，详见下文。

## 实际执行结果与交付

- 完整源码 `assemble` 和 8 项 JUnit 通过；修改后 29 项物品路由原生 GameTests 全部通过。
- 把同一测试运行在原始分拣机逻辑上，单项“自然游戏刻从上方箱子取料并向东输出”按预期失败（上方箱子始终未清空），证明测试能检出原功能缺失。脚本随后恢复候选源码并重新完整编译。
- local17 源码基线的完整 584 项回归为 583 通过、1 失败：`advminerskipsoreoutsidewhitelist`。没有修改高级采矿机代码，没有隐藏该失败。报告保存在 `source-local17-full-584.xml`。
- 核对实际 local21 二进制中分拣 tile entity、容器、GUI 及两个 GUI 内部类：去除常量池索引和字节码行地址后，与仓库原逻辑全部一致。local21 物品路由测试的方法集合也与原仓库 22 项一致。
- 因完整 local18–21 源码缺失，提供明确命名的受限兼容包 `26.3.0-local.21-topinput.1`。由完整编译得到的分拣变更、原生回归及最小语言提示覆盖必要条目；其他 **8582 个条目逐字节相同**，保留 local21 其他功能及原许可。构建脚本 `tools/build_sorting_top_input_compat.py` 只接受 SHA-256 严格匹配的已核对 local21 基线，且禁止覆盖输入 JAR。
- 让 NeoForge 测试服务器通过 `fml.modFolders` **直接加载交付的 local21 兼容 JAR**，而非开发输出目录：全套 **637 项原生 GameTests 全部通过**，报告为 `delivered-local21-full-637.xml`。高级采矿机测试在该实际交付基线上也通过。
- 同一交付 JAR 加载独立临时探针，启动普通无窗口独立服务器，实际保存、正常退出、重新启动两次。探针断言生产类 `CodeSource` 来自交付 JAR，不允许误加载开发 classes。生产 JAR 全程保持相同 SHA-256，且不含任何探针类。

重启夹具：分拣机在 `(8191,80,8192)`，东侧接收箱和上方双箱另一半位于相邻区块。过滤为 4 个带名称铁锭，源箱初始 5 个，电量 1000 EU。手动执行生产服务器更新 3 次后，源箱 2 个、缓冲区 3 个、东箱 0 个；实际保存并退出。第一次重启检查上述状态及过滤名称、默认下方出口和电量保留，再执行一次生产更新，源箱 1 个、缓冲区 0 个、东箱 4 个、电量 920 EU。再次保存退出，第二次重启核对最终状态仍正确。夹具远离出生点，未使用强加载让夹具自行额外运行；自然游戏刻取料另由原生 GameTest 覆盖。

交付：`output/sorting-top-input/IC2-local21-top-input-patch.zip`，包含 JAR、安装说明和 SHA256SUMS。单独 JAR 为 `ic2-neoforge-26.3.0-local.21-topinput.1.jar`，SHA-256：

```text
4df58cb3532b7169d01c598d488d63799f6e838756b2c12e639925491a46cd7c
```

房主和加入者都需退出游戏、备份并替换同一个 IC2 JAR，每个实例只保留一个 IC2 版本；其他七个模组无需替换。上方直接贴箱子，E 行配置目标物品及批量，东侧贴接收箱。未替用户安装，不操作玩家配置、存档或雷神代理。

## 复现环境与边界

使用仓库 `./dev` 入口，Minecraft 26.3、NeoForge 26.3.0.83、已有 Java 25 编译及运行；NeoForm 工具另显式指定已有 Java 21。首次构建已完整生成 Minecraft 编译依赖。无窗口验证使用 `-x downloadAssets`，资源索引 34 和本地 assets 路径写入忽略目录的 `build/moddev/minecraft_assets.properties`，避免为了服务器测试下载 5147 个客户端资源对象。客户端声音、资源和实际 GUI 渲染不属于本次验证。

核心命令（`JDK_PATHS` 代表本机 Java 21、25 的逗号分隔绝对路径）：

```sh
./dev gradle ic2 -- assemble test runGameTestServer -x downloadAssets \
  "-Dorg.gradle.java.installations.paths=$JDK_PATHS" \
  '-Pic2TestFilter=ic2:itemroutinggametests.*' \
  '-Pic2TestReport=build/reports/top-input-gametest.xml'
python3 tools/build_sorting_top_input_compat.py ORIGINAL_LOCAL21.jar \
  mods/ic2/build/libs/ic2-neoforge-26.3.0-local.17-topinput.1.jar \
  OUTPUT/ic2-neoforge-26.3.0-local.21-topinput.1.jar
```

实际 JAR 验证使用随证据保存的 `runtime.init.gradle` 替换开发 ModFolders 参数；`IC2_SORTING_RUNTIME_JAR` 指定交付 JAR，独立重启检查还需 `IC2_SORTING_PROBE_JAR` 及 `IC2_SORTING_RESTART_PHASE`。原始测试日志留在任务 `.local/sorting-restart-probe/` 和 `/private/tmp/ic2-top-input-*.log`，关键断言、XML、校验和与观察摘录已归档。临时探针源码在此目录供复核，不加入生产源集。

尚未验证房主实际环境、完整八模组客户端界面和长期多人负载。完整 local21 源码仍未提供，兼容修补不是长期依赖旧二进制的维护方案；下一次正式发布应先将本改动合并到完整当前源码，再完整构建。
