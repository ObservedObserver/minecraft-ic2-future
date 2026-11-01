# 分拣机上方主动取料

基线：`c238ea1f1fe5ed5a3c33bbe9f2baa74d243fb4e3`，工作树为本仓库。用户先要求读取物品分拣机行为，后提供截图，最后明确授权新增正上方箱子主动取料并按东方过滤规则输出。

本次负责分拣机 tile entity、容器和界面、两种语言的输入提示、ItemRoutingGameTests、开发版本号及对应文档。没有其他未提交修改，不涉及共有物品处理器、其他机器或真实游戏实例。

用户实际通过雷神连接房主局域网世界，本机客户端 local21；当前 GitHub 只有 local17 源码，不能直接用本次源码完整 JAR 覆盖 local21。已询问 local21 源码位置，未收到地址。源码版本单独命名 `26.3.0-local.17-topinput.1`。后续确认原 local21 的分拣主类、菜单、GUI 及两个 GUI 内部类与仓库原版归一化字节码一致，制作受限 `local.21-topinput.1` 兼容补丁，保留其他 8582 个 JAR 条目。通过实际 JAR 的 637 项原生 GameTests、跨区块双箱及缓冲/规则/电量的两次真实保存退出重启。完整 local21 源码仍待补齐，未来正式版本应合入完整源码维护。

实施和证据见 `docs/verification/sorting-top-input/README.md`。构建缓存位于 `.cache/gradle`；JDK 使用本机已有 Minecraft epsilon 的 Java 25，不复制运行时。构建、测试使用统一 `./dev` 入口和隔离无窗口 GameTest 服务器。

日志：`/private/tmp/ic2-sorting-gradle-help.log`、`/private/tmp/ic2-top-input-validation.log`。依赖首次下载需要联网，初次沙箱内请求 DNS 失败，提升权限后的 Gradle help 已成功。

最终产物位于 `output/sorting-top-input/`，未安装到本机玩家或房主实例。源码基线完整构建和 8 项 JUnit 通过，29 项物品路由通过；local17 全套 584 项中高级采矿机白名单旧测试失败，已如实归档，实际 local21 修补包全套 637 项全部通过。临时探针为独立 sortingprobe 模组，不包含在交付 JAR，源码未注入探针。所有本任务验证服务器均已正常退出。
