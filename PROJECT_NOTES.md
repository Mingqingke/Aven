# Aven 工程笔记

## 分层

```
UI (Compose + MIUIX)
  ↓ 只读 StateFlow / 只调 Store
Store（唯一数据入口，Room + 文件）
  ↓
calc.ScoreEngine（纯 Kotlin，15 个单测）
ocr.SheetParser（纯 Kotlin） + ocr.Ocr（ML Kit）
ai.Ai（OkHttp） + ai.AiTasks（提示词与报告解析）
```

## 关键决策

**比较口径**
1. 先筛共同科目，再按本次满分加权比较得分率（pp）。
2. 满分一致时才额外给出原始分差；不一致时原始分差不展示。
3. 考试类型、学段、间隔天数、共同科目占比、是否有排名共同决定可信度（高 / 参考 / 仅参照）。
4. 变化绝对值小于设置的中性区间（默认 2pp）判为「基本持平」，避免 1 分来回变色。
5. 没有共同科目、只有一场考试、或只有一科共同且满分类型不同时，不做进步 / 退步判断。

**个人最佳**
- 同时保留每科最高原始分与最高得分率，两者可能来自不同考试。
- 组合成绩 = 各科最佳得分率 × 本次满分求和，明确标注「跨考试拼接」。
- 差距优先按得分率折算成分数，避免满分不同直接相减。

**AI**
- 所有数字由客户端计算后随提示词提供，提示词禁止模型自行换算。
- 输出固定 JSON 结构，客户端解析为：事实 / 趋势 / 优势 / 问题 / 推测 / 行动 / 下一阶段。
- 解析失败时保留原文并标记「非结构化」，不伪造字段。
- 默认不发送任何数据；文字授权与图片授权分开。
- 模型：`deepseek-flash`（支持图片）、`deepseek-v4-pro`（仅文本），均来自官方文档当前页面。

**OCR**
- 本地 ML Kit 中文模型（随 APK 安装，离线），识别后只做字段猜测，全部进入可编辑核对界面。
- 单数字行（只有分数没有满分）标记 `confident=false`，界面显示「需核对」。
- 云端识别只在用户显式点击并确认上传范围后调用。

**数据安全**
- API Key：Android Keystore AES-GCM，密文 + IV 存 SharedPreferences，界面只显示掩码。
- 卸载即删除数据；备份为 JSON（含成绩、题目、目标、计划、任务、报告），导入只追加。
- 编辑考试时按科目暂存题目并回填，避免级联删除用户录入的题目。

## 数据库版本

`version = 1`，8 张表：exams / subject_scores / questions / goals / plans / tasks / reports / chats。
后续加字段必须升 version 并提供 Migration，禁止 destructive fallback。

## 与 MIUIX 的关系

- 卡片、开关、输入框、TabRow、弹窗、图标全部来自 `top.yukonga.miuix.kmp:*:0.9.4`。
- 主题通过 `MiuixTheme(colors = lightColorScheme/darkColorScheme)` 注入中性色板。
- 导航使用 `miuix-nav` 的 `NavController` + `NavDisplay` + `NavTransitions.MiuixDefault`。
- 首页卡片按压用 MIUIX `PressFeedbackType.Tilt`，并受「减少动态效果」开关控制。

## 后端

无。没有服务器、没有账号体系、没有云端存储。唯一的外部请求是用户主动触发的 DeepSeek API 调用。
