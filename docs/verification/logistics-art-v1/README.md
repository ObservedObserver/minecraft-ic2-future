# 物流资源首版验证

日期：2026-10-31。基线 `96a301b7`，资源修订 `art-v1`。范围为美术资源，尚无 Java 方块注册、物流模拟、配方或独立模组 JAR。

## 产物

[资源源码与说明](../../../mods/logistics/README.md)，[可旋转预览](../../design/logistics/art-v1/index.html)。构建输出 `mods/logistics/build/logistics-art-v1-resources.zip`，SHA-256：

```text
1054d9d070f408a3ed9f2e4a46fdcfc91bea74b5ad00cf235538552400aefbb2
```

游戏实际加载的副本位于 `.local/logistics-art/game/resourcepacks`，与上述产物哈希一致。ZIP 只新增物流资源和展示别名，不覆盖原版文件。PNG、JSON、生成源码及网页已纳入仓库，构建 ZIP 与运行目录不提交。

## 已执行

| 检查 | 结果及边界 |
| --- | --- |
| `python3 tools/logistics_assets.py build` | 生成三档资源及预览数据 |
| `python3 tools/logistics_assets.py check` | 通过；51 个模型、42 张 PNG、3732 个面、6 套物品与方块状态、15 组动画；见 [机器记录](static-check.json) |
| `python3 mods/logistics/art/build_showcase.py` | 生成隔离世界展示函数，未放入生产资源 |
| `./dev check` | 通过；解析 10627 个 JSON，历史交付清单 8 个模组保持原样 |
| 网页显示 | 总览、分拣器单件、汇入与悬空线路可显示；浏览器控制台未捕获 error/warn；截图已归档 |
| 官方原版 26.3 客户端 | 使用本任务隔离世界与资源包，执行 `function logistics_review:setup`，三档设备及七种连接模型显示；未发现物流资源缺失模型、贴图或 JSON 解码错误 |
| 保存退出 | 展示世界正常保存并退出客户端；不作为物流库存持久化测试 |

游戏实拍：[三档设备](../../design/logistics/art-v1/in-game-tiers.png)、[连接形态](../../design/logistics/art-v1/in-game-shapes.png)。网页截图：[总览](../../design/logistics/art-v1/overview.png)、[分拣器](../../design/logistics/art-v1/inserter.png)、[路线](../../design/logistics/art-v1/route.png)。关键客户端日志摘录见 [client-evidence.txt](client-evidence.txt)。

## 复现与限制

资源生成、加载命令及展示函数的清空坐标范围见资源 README。只在新建测试世界执行展示函数。可选的 `tools/logistics_preview_client.py` 是 Apple Silicon macOS 的隔离资源检查工具，下载官方客户端和 Java 运行时并核对文件哈希，复用已安装且哈希匹配的库；不读取账号文件。首次运行会联网下载，正常美术修改只需资源生成和静态检查。

本轮使用 Mojang Java 25.0.1，客户端资源格式 97.1、展示数据包格式 121.0。节省磁盘而省略音频下载，未检查声音；离线身份导致的认证/Realms 401 和 `minecraft:end_of_frame` 警告有保留，不能把日志概括为全无警告。初次展示启动因测试用户名超过 16 字符失败，已改为 `ArtReview` 并重新进入世界完成检查。

客户端使用物品展示实体显示实际资源，不能验证未来方块状态注册、碰撞、自动连接、机械臂游戏动画、物品运输、性能、多人、IC2 接口或完整整合环境。分拣器摆动只在网页预览；未来功能接入需校准实际轴心、取放点和动画时序。坡道上端伸入上方空间，放置时必须检查净空。三档动画快慢只是评审参数，不是已经确定的吞吐量。

版本依据：[Minecraft 26.3 官方发布说明](https://feedback.minecraft.net/hc/en-us/articles/48913133328013-Minecraft-Java-Edition-26-3)、[Mojang 版本清单](https://piston-meta.mojang.com/mc/game/version_manifest_v2.json)。最终显示结果以本次实际客户端为证据。
