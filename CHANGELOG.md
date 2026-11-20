# 更新日志

本文件记录项目的重要变更。版本号在正式发布时补充。

## [Unreleased] - 现代化维护版

### 新增

- 文本导出改用 Storage Access Framework，由用户选择保存位置，兼容 Android 10+ 分区存储。
- 导出成功后展示实际保存的文件名，不再依赖外部存储路径。
- 恢复提醒时间设置，并改用通知 + 全屏 Intent 重做便签提醒。
- 增加提醒通知渠道，支持高优先级通知、锁屏全屏提醒和点击查看便签。
- 增加 Android 13+ 通知权限申请，以及精确闹钟、全屏通知权限声明。
- 恢复便签编辑页 ActionBar，重新提供字体大小、背景颜色、清单模式、分享和提醒入口。
- 新增“背景颜色”菜单项，并保留标题栏调色板快捷入口。
- 恢复便签列表页 ActionBar，重新提供批量操作、新建文件夹、搜索和设置入口。
- 增加“批量操作”菜单项，可在不依赖长按的情况下进入多选模式。
- 列表多选模式恢复删除、批量删除和移动便签入口；移动目标需要至少一个用户文件夹。
- 移动和删除操作完成后刷新列表并退出多选模式。
- 新增便签排序功能，支持按修改时间、创建时间和标题排序，并持久化排序偏好。
- 新增便签置顶/取消置顶，编辑页和列表多选模式均可操作，置顶便签优先展示。
- 新增便签标签编辑、列表展示和按标签筛选；标签自动去重并规范化为逗号分隔格式。
- 数据库升级到版本 5，新增 `pinned` 和 `tags` 字段及对应的增量迁移。
- 迁移为 Android Studio 标准 Gradle 工程结构。
- 增加 `README.md`、`LICENSE`、`CONTRIBUTING.md` 和 Issue/PR 模板。
- 为核心业务代码补充中文类注释、方法注释、语句块注释和关键语句注释。

### 变更

- 使用 Gradle 8.6 + AGP 8.4.2 构建。
- `compileSdk` / `targetSdk` 升级到 34，`minSdk` 调整为 21。
- 应用包名保持 `net.micode.notes`。
- 便签列表、编辑、设置等界面完成新系统兼容性验证。

### 修复

- 修复 AGP 8 下 R 资源 ID 不能用于 `switch` 的编译错误。
- 增加 `org.apache.http.legacy`，修复 Google Tasks 同步模块编译失败。
- 为 Android 12+ 组件补充 `android:exported`。
- 为 `PendingIntent` 增加 `FLAG_IMMUTABLE`，修复 Android 12+ 崩溃。
- 适配 Android 14 动态广播接收器的导出标记。
- 用 `Notification.Builder` + NotificationChannel 替换已删除的通知 API。
- 同步广播增加 `setPackage()`，避免隐式广播风险。
- 修复数据库从版本 1/2/3 升级时版本号推进错误、回收站文件夹重复插入导致升级失败的问题。
- 修复便签摘要为空、搜索参数为空、ContentResolver 返回空 Uri 等场景的空指针和非法参数问题。
- 修复按标签筛选时子串误匹配以及 LIKE 通配符未转义的问题。
- 修复多选模式未扣除列表 header 位置、空游标全选和取消选择等边界问题。
- 修复长按文件夹不弹出菜单、长按便签未选中当前项的问题。
- 删除未使用方法、过期注释和注释掉的旧代码。

### 验证

- `gradlew build` 构建通过。
- `gradlew assembleDebug --offline` 构建通过。
- 在 Android 16（API 36）模拟器上验证便签排序、置顶/取消置顶、标签编辑与展示、按标签筛选和返回清除筛选。
- 在 Android 14（API 34）模拟器上完成安装、启动、新建便签、保存、列表展示和设置页验证。
- `logcat` 未出现 `AndroidRuntime` 崩溃。

## 上游历史

本项目基于 [MiCode/Notes](https://github.com/MiCode/Notes) 的 `master` 分支改造，保留 Apache License 2.0 许可证与原始 NOTICE 文件。
