# 鞘翅与喷气背包合并调查

日期：2026-10-31。范围：解释当前限制、追踪上游来源、静态审查开放合并的条件。本次不修改玩法、模组源码、版本或交付产物。

调查基线为 main，2026-10-31 的 `docs: simplify conveyors to unsupported single-block tiles`，提交 `96a301b7`。IC2 源码版本为 `26.3.0-local.17`。开始时工作区干净；调查期间出现其他 session 的 `docs/sessions/2026-10-31-logistics-art.md`，不纳入本次提交。

## 已确认的限制与来源

- [JetpackAttachmentRecipe.java](../../../mods/ic2/src/main/java/ic2/core/item/armor/jetpack/JetpackAttachmentRecipe.java) 的 `init()` 明确执行 `blacklistedItems.add(Items.ELYTRA)`，所以原版鞘翅不能通过附加配方。
- `assemble()` 只接受 `Ic2Items.JETPACK_ELECTRIC`，另需连接部件和允许的胸部装备。普通燃料背包不属于可融合材料。
- 配置 `jetpackAttachmentBlacklist` 只能增加黑名单。即使配置为空，初始化仍会加入鞘翅，无法通过清空配置放开。
- `git show upstream-baselines:mods/ic2/src/main/java/ic2/core/item/armor/jetpack/JetpackAttachmentRecipe.java` 和当前文件对比表明，上述两条配方限制已存在于上游快照。26.3 移植保留了它们。
- [上游来源记录](../../provenance/upstream.json) 指向 neo-industrial-mc/IC2R，基线为 2026-08-10 的 `fix(wrench): match personal safe mining speed`，提交 `417fddb1fd23b926accc02b380cd658121f5c9e1`。[该提交的配方源码](https://github.com/neo-industrial-mc/IC2R/blob/417fddb1fd23b926accc02b380cd658121f5c9e1/src/main/java/ic2/core/item/armor/jetpack/JetpackAttachmentRecipe.java) 在线也能确认黑名单。不能把这个基线提交误称为首次加入黑名单的提交。
- [JEI 扩展](../../../mods/ic2/src/main/java/ic2/integration/jei/JetpackAttachmentCategoryExtension.java) 使用同一黑名单，因此 JEI 不展示鞘翅与真实配方一致。

限制来自明确的代码规则，但现有证据没有说明作者加入它的理由，不能宣称已确认是平衡设计或特定故障规避。

## 当前运行逻辑的静态审查

| 路径 | 源码行为与含义 |
| --- | --- |
| 合并结果 | `armor.copy()` 后增加标记和电量，没有主动删除原物品组件、附魔或损伤。以鞘翅为输出载体具有实现基础，但本次未验证原版 26.3 的实际滑翔入口。 |
| 装备识别 | [StackUtil](../../../mods/ic2/src/main/java/ic2/core/util/StackUtil.java) 从 `EQUIPPABLE` 组件读取装备槽。[JetpackHandler](../../../mods/ic2/src/main/java/ic2/core/item/armor/jetpack/JetpackHandler.java) 按胸部槽和 `hasIC2Jetpack` 标记识别附加背包，不要求物品实现 IC2 护甲类，也没有再次拒绝鞘翅。 |
| 实际 tick | `EventHandlerForge.onPlayerTick` 接收 `PlayerTickEvent.Post`，经 `EventHandler.onPlayerTick` 调用 `JetpackHandler.onPlayerTick`，再进入 `JetpackLogic.onArmorTick`。支持附加装备的 tick 不依赖原护甲物品自己执行回调。 |
| 电量与耐久 | [GatewayElectricItemManager](../../../mods/ic2/src/main/java/ic2/core/item/GatewayElectricItemManager.java) 将非电力物品交给备用管理器。`JetpackHandler` 将电量放在 `CUSTOM_DATA.charge`，充放电方法没有设置原版损伤值。不能直接把 `ElectricItemManager` 对电力物品的耐久映射逻辑套到附加鞘翅上。 |
| 飞行操作 | [JetpackLogic](../../../mods/ic2/src/main/java/ic2/core/item/armor/jetpack/JetpackLogic.java) 没有 `isFallFlying` 或滑翔专用处理。空格或悬停触发喷气，普通分支增加竖直速度并以 `0.6` 限制结果，还会清除跌落距离。水平推力依据朝向和前进键，不是烟花沿视线助推。滑翔时的手感、按键与状态切换需要实测。 |
| 渲染 | [LayerJetpackOverride](../../../mods/ic2/src/main/java/ic2/core/item/armor/jetpack/LayerJetpackOverride.java) 对附加背包绘制额外身体/手臂模型，没有鞘翅专用适配。翅膀与背包能否正确同时显示未验证。 |
| 枯竭与持久化 | 当前没有为“鞘翅失去滑翔能力但仍有电”或“没电但鞘翅仍完好”定义额外策略。标记、电量和损伤分开保存具有基础，但需实际保存、关闭和重启确认。 |

静态结论：开放“鞘翅 + 电力背包 + 连接部件”有现成机制可复用，没有发现仅因物品是鞘翅就必然崩溃的直接证据。但移除黑名单只解决配方准入，不能据此宣称完整支持，更不能宣称已实现滑翔助推。

[ItemArmorJetpack](../../../mods/ic2/src/main/java/ic2/core/item/armor/ItemArmorJetpack.java) 的普通背包使用 `BIOGAS`，容量为 30000 mB。当前没有蒸汽背包实现。附加系统的运行代理固定为电力背包，若要允许燃料或蒸汽背包融合，还需实现背包类型、燃料存储、加注与消耗，不能只扩大配方材料。

## 1.12 的历史证据与边界

[IC2 官方论坛的 Jetpack + Elytra 帖子](https://forum.industrial-craft.net/thread/16394-jetpack-elytra/) 于 2020-12-07 由玩家报告：在 The 1.12.2 Pack 中能把电力背包附加到鞘翅，但随后无法展开鞘翅。它是历史玩家的一手报告，说明“某个 1.12.2 环境能合成”有记录；帖子没有解决方案，不能用作已定位代码根因或所有 1.12 版本都兼容的证据。

[IC2 Classic 项目 wiki](https://github.com/TinyModularThings/IC2Classic/wiki) 说明它是独立于 IC2 Experimental 的分支。因此“1.12 经典原版”不能仅凭称呼对应到唯一代码版本。用户当年的确切 JAR、附属模组和整合包脚本尚未核对。本次不能认定其记忆错误，也不能把当年功能直接归属于当前 IC2R 的上游。

## 后续若实施的最小验收范围

1. 验证真实合成入口、JEI、材料消耗，以及原名、附魔、损伤和电量保留；保留其他禁用物品的约束。
2. 验证实际起飞、展开鞘翅、松开空格滑翔、再次喷气、悬停切换与着陆。先明确普通喷气与滑翔助推的预期区别。
3. 分别验证有电/没电、完整/失去滑翔能力的鞘翅、修补与修理、客户端与服务端同步、保存关闭后重载。
4. 验证实际玩家模型上的翅膀与背包渲染，使用最终候选 JAR 测试。

## 本次实际验证

已执行 Git 状态/历史检查、上述文件与真实事件调用链的静态阅读、相对 `upstream-baselines` 的配方差异检查，以及上游配方和历史论坛原文查阅。文档完成后执行 `git diff --check` 与本文件相对链接存在性检查。

未构建、未执行 JUnit/GameTests、未启动客户端、未加载用户 JAR 或核对其 SHA-256。本次没有构建缓存，不为只读调查下载整套依赖。没有修改玩家存档，现有历史测试不作为本次融合验证结果。
