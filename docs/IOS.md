# 关于 iOS

本次交付是原生 Android 应用（Kotlin + Compose + MIUIX），没有 iOS 安装包。

原因：当前构建环境是 aarch64 Ubuntu / proot，只有 Android SDK 与 JDK，没有 macOS、Xcode 与 Swift 工具链，无法编译或签名 iOS 应用。用 Flutter / React Native 重写虽然能"一套代码两端跑"，但会丢掉 MIUIX / HyperOS 的组件与视觉体系，与本项目最重要的设计要求冲突，因此没有为了跨平台而更换技术栈。

可以在 iOS 上复用的部分：

| 资产 | 位置 | 复用方式 |
|---|---|---|
| 成绩计算规则 | `core/calc/Engine.kt` | 逻辑与平台无关，可逐函数移植到 Swift |
| 成绩单解析规则 | `core/ocr/SheetParser.kt` | 纯字符串处理，可直接重写为 Swift 等价实现 |
| AI 提示词与报告结构 | `core/ai/AiTasks.kt` | JSON Schema 与提示词可直接复用 |
| 数据模型 | `core/db/Entities.kt` | 对应到 Core Data / SwiftData 实体 |
| 设计令牌 | `ui/theme/Theme.kt` | 颜色、圆角、间距直接对照 |
| 备份格式 | `core/Backup.kt` | JSON 结构可被 iOS 端读写，实现跨端迁移 |

如果后续需要 iOS 版，建议顺序：

1. 用 SwiftUI 重建四个一级页面与设置层级，保留卡片比例与状态色。
2. 先移植计算引擎与解析器，用同一批单元测试用例校验结果一致。
3. 图表改用 Swift Charts，交互与 Android 版保持一致。
4. AI 与 OCR 走同一套 JSON 协议；Key 存入 Keychain。
5. 备份 JSON 双向兼容，保证 Android ↔ iOS 可以迁移数据。
