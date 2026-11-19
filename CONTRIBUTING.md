# 贡献指南

感谢你关注小米便签 Android Studio 现代化维护版。欢迎通过 Issue 反馈问题，也欢迎提交 Pull Request。

## 反馈问题

提交 Issue 前请先搜索是否已有相同问题。Bug 报告建议包含：

- 设备型号、Android 版本、应用版本
- 复现步骤
- 预期结果
- 实际结果
- 相关日志或截图

功能建议请说明：

- 使用场景
- 期望的交互或效果
- 是否愿意参与实现

## 开发环境

- Android Studio 2023.3.1 或更新版本
- JDK 17
- Android SDK Platform 34
- Android SDK Build-Tools 34.0.0

## 提交代码

1. Fork 本仓库并创建功能分支，例如 `feature/export-saf`、`fix/alarm-notification`。
2. 保持改动聚焦，一个 Pull Request 尽量只解决一个问题。
3. 提交前运行：

   ```bash
   ./gradlew build
   ```

4. 如果改动涉及界面，请附上截图或录屏。
5. 如果改动涉及数据库、同步或提醒等核心逻辑，请说明测试方式。

## 代码风格

- 使用 Java 现有风格，保持缩进和命名一致。
- 新增类、方法、复杂语句块时补充中文注释，说明功能和意图。
- 不提交 `local.properties`、`.idea/`、`build/` 等本地文件。
- 不提交签名文件、密钥、Token 等敏感信息。

## 提交信息

建议使用清晰的提交信息，例如：

```text
fix: 修复 Android 14 广播接收器注册崩溃
feat: 使用 SAF 重做文本导出
docs: 补充构建与维护说明
```
