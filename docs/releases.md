# 整合包发布流程

`pack/mods.lock.json` 记录已经交付的 IC2 local17 / 暮色森林 local5 八模组客户端包，包括各 JAR 的文件名和 SHA-256。其他六个模组只记录来源，不将源码和 JAR 加入此仓库。

当前历史 ZIP 为：

```text
Minecraft26.3-NeoForge-TF-local5-IC2R-local17-TeaCraft1.4.19-JEI-Xaero-IPN.zip
SHA-256: 914877e2e8b1ff423e3d37b272b5c86ad0aa8f7270411eec6b3cd289ddaa8506
```

完整原交付文件仍留在旧任务输出目录；其位置见 `provenance/local-deliveries.json`，该文件只是迁移时的定位记录。将已有包解压后，可以运行：

```sh
./dev verify-release /path/to/extracted-pack/mods
./dev verify-release /path/to/the-original-delivery.zip
```

这会核对八个 JAR 的内容哈希和 ZIP 完整性，不需要网络。重新编译会因编译器、条目顺序、元数据或时间戳产生不同哈希，不能要求新构建与历史增量构建逐字节相同。

后续发布顺序：

1. 在相应 `mods/*/gradle.properties` 中提高本地版本号，完成代码和回归测试。
2. 完整构建并按修改范围执行 JUnit、IC2 GameTests、隔离客户端和保存重启检查。
3. 收集 IC2 JAR、暮色 `universal` JAR 与确定版本的其他六个模组。一个模组只保留一个版本。
4. 更新锁定清单、哈希、版本变更说明和实际验证结果，保留各模组的许可证与来源。
5. 检查 ZIP 内容，不带测试探针、开发世界、玩家存档或凭据。新发布提交完成后创建版本 tag。

仓库当前没有自动下载或重新发布第三方模组的步骤，也没有推送或上传操作。现有局域网下载服务继续运行在旧任务中，不作为源码构建的依赖。
