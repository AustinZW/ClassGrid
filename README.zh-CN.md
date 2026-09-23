# 课格 ClassGrid

[English](README.md)

基于 Material 3 的安卓课程表，使用 Kotlin 与 Jetpack Compose 构建。

课格以本地数据为核心，支持灵活的学期、周次、节次和课程时段设置，并可从东华大学本科教务系统导入课程。

## 功能

- 按周查看课表，左右滑动切换相邻教学周
- 显示星期、日期、节次及自定义上课时间
- 自定义上课日、每日节数、课程格高度与字号
- 一门课程支持多个教室、星期、节次和周次时段
- 添加、编辑、复制、拖动和调整课程长度
- 课程冲突检测及导入冲突选择
- RGB、HSB、色环、全色域图和 Hex 课程颜色设置
- 从东华大学本科教务系统登录、识别并选择导入课程
- 新建课表，并在替换已有内容前导出备份
- 使用 `.cgf` 文件导入、导出及迁移课表数据
- Material 3 动态配色与深色模式
- Room 与 Preferences DataStore 本地持久化

## 教务系统导入

在课表右上角的设置菜单中选择“从教务系统导入”：

1. 在应用内完成统一身份认证。
2. 登录成功后等待应用自动进入课表页；也可点击“前往课表”。
3. 点击右下角识别按钮，选择需要导入的课程。
4. 导入当前课表，或创建新课表后导入。

应用只允许内置导入页面访问东华大学域名，不读取或保存统一身份认证密码。

## 技术栈

- Kotlin
- Jetpack Compose
- Material 3
- Room
- Preferences DataStore
- ViewModel、StateFlow 与单向数据流

## 环境要求

- Android Studio
- JDK 17
- Android SDK 35
- 最低 Android 版本：Android 8.0（API 26）

## 构建与测试

使用 Android Studio 打开项目，等待 Gradle Sync 完成后运行 `app` 配置；也可以在 Windows PowerShell 中执行：

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug
```

调试 APK 生成于：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 数据文件

`.cgf`（ClassGrid File）是课格的可迁移数据格式，包含课程表设置与课程数据。导入文件前，应用会验证格式、版本及数据范围。

## 隐私

课程和设置默认保存在设备本地。应用不包含云同步功能，也不会将统一身份认证凭据写入课格数据库。

## 许可证

本项目采用 [MIT License](LICENSE)。
