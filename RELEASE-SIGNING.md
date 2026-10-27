# 正式版签名

Android Studio 与命令行共用 `app` 模块的 `release` 签名配置。密钥和密码只保存在本机，不提交到 Git。

## 本机配置

项目根目录的 `keystore.properties` 使用 UTF-8 编码，内容如下（将占位内容替换为自己的配置）：

```properties
storeFile=/path/to/existing-keystore
storePassword=YOUR_STORE_PASSWORD
keyAlias=YOUR_KEY_ALIAS
keyPassword=YOUR_KEY_PASSWORD
```

Windows 路径使用 `/`，也可以使用相对于项目根目录的路径。密码中的反斜杠需要写为 `\\`，空格需要写为 `\ `。不要在值外加引号。该文件与 `*.jks`、`*.keystore`、`*.p12`、`*.pfx` 均被 Git 忽略。密钥库应放在项目外的私有目录，并单独备份；请勿只保存在下载目录。

迁移到另一台电脑时，需单独安全复制密钥库并重新创建此文件。不要为已发布应用重新生成密钥。

## Android Studio

1. 打开项目，执行 **Sync Project with Gradle Files**。
2. 在 **Settings > Build, Execution, Deployment > Build Tools > Gradle** 中，将 Gradle JDK 设为已安装的 JDK 17；本机已通过 `GRADLE_LOCAL_JAVA_HOME` 配置。
3. 在 **Build Variants** 中将 `app` 切换为 `release`。
4. 在 Gradle 工具窗口执行 **app > Tasks > build > assembleRelease**，即可使用上述本机配置自动签名。也可执行 `bundleRelease` 生成 AAB。

输出文件：

- APK：`app/build/outputs/apk/release/app-release.apk`
- AAB：`app/build/outputs/bundle/release/app-release.aab`

## 一键构建与检查

在项目根目录运行：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/build-release.ps1
```

脚本执行单元测试、lint、调试版构建与签名正式版 APK 构建。追加 `-Bundle` 可同时生成签名 AAB。也可直接运行 `./gradlew.bat assembleRelease`。

缺少本机签名配置或密钥文件时，正式版打包会失败并提示补全配置。调试版与 preview 构建不依赖正式版密钥。
