# Logistics 美术资源首版

独立物流模组的资源阶段，工作命名空间为 `logistics`。本目录目前没有 Java 注册、Gradle 工程或可运行物流 JAR。用户要求先完成美术供评审，再推进功能。资源可以打成 26.3 的独立资源包，用原版物品模型组件验证，不修改 IC2 或暮色。

首批名称为低速、中速、高速传送带及低速、中速、高速分拣器。资源 ID 使用 `low`、`medium`、`high`，不使用 `wood`、`copper`、`iron`。修改配方不需要改变物品身份；扩展等级时添加新的稳定 ID，不重排或复用旧 ID。

每格传送带是一个独立功能方块，不要求支撑。模型里的带面、边缘和机械臂零件都只是模型元素，不是玩家要分别制作放置的方块。主干优先侧向汇入、周期性带间分拣、无同层独立交叉通道的设计约束继续生效。

当前资源为原创、可重复生成的像素与几何定义，没有将 AI 概念图裁剪为贴图，也没有复制其他模组的贴图或代码。当前颜色采用暖棕、铜色、冷灰色的等级外观，配合一、二、三道标记；材质外观和未来配方独立。

目录：

- `art/tiers.json`：三档配色、标记数量、评审动画帧时长。帧时长不是实际物流速度。
- `art/rig.json`：分拣器活动部分、轴心和评审摆动范围，尚无游戏渲染器消费该文件。
- `art/manifest.json`：资源清单与评审资源包哈希。
- `src/main/resources/assets/logistics`：实际 PNG、`.mcmeta`、模型、物品模型定义、方块状态与中英文本。
- [资源生成器](../../tools/logistics_assets.py)：原始像素绘制和模型几何的可编辑源码，仅依赖 Python 标准库。直接修改导出资源会在重建时被覆盖，应改生成器或配色文件。
- [交互评审页](../../docs/design/logistics/art-v1/index.html)：直接渲染同一批 JSON 和 PNG，无外部网页依赖。

在仓库根目录执行：

```sh
python3 tools/logistics_assets.py build
python3 tools/logistics_assets.py check
./dev check
```

生成 51 个模型、42 张 PNG，其中 15 张为带面动画条，6 套物品模型定义和方块状态。三档均有直线、左侧转入、右侧转入、左侧汇入、右侧汇入、上坡、下坡以及对应静止模型；分拣器含完整模型、底座和活动臂三个模型。物品栏图标采用同一模型的三维渲染，不额外维护与实物不一致的平面图标。

评审包输出 `build/logistics-art-v1-resources.zip`。它增加 `logistics` 资源和 `logistics_review` 中的评审用物品模型别名，不覆盖原版资源，也不注册新方块。`src/main/resources` 内不含评审数据包、原版物品替换、物品运输探针或配方。资源包不是整合包更新，不能当成可玩的物流模组。

在单独的 26.3 创造测试世界加载该资源包，可用以下命令查看物品模型：

```mcfunction
give @s minecraft:paper[minecraft:item_model="logistics:low_conveyor"]
give @s minecraft:paper[minecraft:item_model="logistics:medium_conveyor"]
give @s minecraft:paper[minecraft:item_model="logistics:high_conveyor"]
give @s minecraft:paper[minecraft:item_model="logistics:low_inserter"]
give @s minecraft:paper[minecraft:item_model="logistics:medium_inserter"]
give @s minecraft:paper[minecraft:item_model="logistics:high_inserter"]
```

可选展示场景：运行 `python3 mods/logistics/art/build_showcase.py`，将生成的 `build/logistics-art-showcase` 复制进新建测试世界的 `datapacks`，执行 `reload` 后执行 `function logistics_review:setup`。该函数会清空坐标 `-12 80 -8` 至 `12 90 16` 并铺设展示地面，因此只在本任务新建的隔离世界使用。场景使用物品展示实体，不证明真实方块碰撞、库存传输或区块生命周期。

几何边界：平带带面高 2/16 格，边缘 2.5/16 格；升降段采用逐像素细阶几何，仍为一个方块模型。最高端带面与边缘会伸入上方空隙，后续放置逻辑必须检查相应净空，不能以添加支撑代替碰撞检查。上坡和下坡用于同一种物品的不同状态，不要求玩家拼装坡道。分拣器旋转轴为 `[8,8,8]`，活动臂独立；实际抓取位置、搬运周期和持货动画需要后续逻辑阶段校准。

方块状态 JSON 为待实现 Java 类预留 `facing`、`shape`、`running` 属性。本轮未实现这些属性或完成 NeoForge 客户端联调。真实材质动画由 Minecraft `.mcmeta` 驱动，分拣器摆动仅在评审页面演示。游戏的暂停状态切换、渲染器和逻辑同步尚未实现。

本轮验收与限制见 [验证记录](../../docs/verification/logistics-art-v1/README.md)。
