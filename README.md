# Aven

仓库 https://github.com/Mingqingke/Aven
下载 https://github.com/Mingqingke/Aven/releases

AI 学业成长应用 · 面向中国大陆初高中学生

记录每一次考试，理解每一次变化，持续优化学习方式。

## 它做什么

- **成绩档案**：拍照 / 相册导入成绩单，本地离线 OCR 识别，人工核对后入库；也支持手动录入与历史导入。
- **可信比较**：本地计算引擎按共同科目、满分与考试类型判断进步 / 退步 / 持平；满分不同时改用得分率，并给出比较可信度。
- **个人最佳**：分别保存每科最高原始分与最高得分率，跨考试拼出「个人最佳组合」，标明这不是一次真实成绩。
- **学科与趋势**：每科独立折线图、波动程度、目标差距；题目级数据录入后可看知识点得分率。
- **AI 分析**：DeepSeek 生成结构化报告，明确区分已知事实、待验证推测与行动建议；支持围绕成绩继续对话。
- **学习计划**：目标 → 阶段 → 任务 → 复盘，任务可完成、延期、跳过；AI 可拆解任务，但必须由用户确认才写入。

## 界面

与 MEMORA 同一版式 中性底 超圆角卡片 左大卡加右侧两小卡 三卡保留轻微倾斜
主色改为 MIUIX 蓝 辅以紫 琥珀 绿 状态色 卡片按内容着色
底栏 卡片 按钮 输入 弹窗 开关 滑块 TabRow 全部使用 MIUIX 0.9.4 组件
每个页面独立不透明背景 页面宽度上限 700dp 宽屏居中 自适应窗口大小
文案不使用标点 保持短句与克制层级

## 技术栈

Kotlin 2.4.20 · Jetpack Compose（BOM 2026.09）· MIUIX 0.9.4（ui / preference / icons / squircle / nav）· Room 2.8.5 · OkHttp 5.5 · Coil 2.7 · ML Kit 中文文字识别 16.0.1（离线内置模型）

- compileSdk / targetSdk 37（Android 17）
- minSdk 26
- JDK 21

## 构建

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-arm64
/opt/gradle-9.4.1/bin/gradle assembleDebug --no-daemon
# 产物：app/build/outputs/apk/debug/app-debug.apk
```

单元测试（成绩引擎与成绩单解析）：

```bash
/opt/gradle-9.4.1/bin/gradle testDebugUnitTest --no-daemon
```

## 目录

```
app/src/main/java/com/aven/app
├── core/
│   ├── calc/       成绩计算引擎（纯 Kotlin，可单测）
│   ├── db/         Room 实体、DAO、数据库
│   ├── ocr/        ML Kit 离线 OCR 与成绩单解析
│   ├── ai/         DeepSeek 请求封装与结构化报告
│   ├── Backup.kt   JSON 备份 / 恢复 / CSV 导出
│   ├── Store.kt    唯一数据入口
│   └── Prefs.kt    设置项
└── ui/
    ├── theme/      设计令牌
    ├── components/ 卡片、图表、状态组件
    ├── screens/    首页、成绩、分析、计划、设置
    └── AppRoot.kt  四栏导航与页面栈
```

## 更新系统

- 三种来源 依次尝试 官方版本清单 备用清单 GitHub Releases
- 清单使用 latest.json 结构 支持 ETag 缓存 只按 versionCode 判断新旧
- 下载显示真实字节进度与速度 完成后校验 SHA-256 包名 版本号 签名证书
- 校验通过与已安装签名一致后才交给系统安装器 不绕过系统授权
- 设置 → 更新与版本 可填写清单地址与 GitHub 仓库 留空即未配置

清单格式见 `update.example.json`

正式发布必须保持同一包名与签名 并递增 versionCode 否则无法覆盖安装

## AI 配置

设置 → AI 配置：填写你自己的 DeepSeek API Key（使用 Android Keystore 加密保存），可选模型：

- `deepseek-flash`：支持图片输入，用于云端成绩单识别与文本分析
- `deepseek-v4-pro`：仅文本分析

默认不发送任何数据。生成报告或对话前需在「隐私与安全」中授权，且只发送分数、得分率、目标与任务完成数，不发送姓名、学校与图片。

## 开源

- 许可证 MIT 见 `LICENSE`
- 更新日志 `CHANGELOG.md` 参与说明 `CONTRIBUTING.md` 安全说明 `SECURITY.md`
- GitHub Actions 工作流 `.github/workflows/android.yml`
- 签名配置放在根目录 `keystore.properties` 与 jks 均不提交

## 数据

全部数据保存在本机（Room + 私有图片目录）。卸载即删除，请在「数据管理」中导出 JSON 备份。导入只追加，不覆盖已有记录。

## 已知限制

- 得分率无法完全消除试卷难度差异，比较结论附带可信度而不是定论。
- 赋分只按用户录入的数值展示，不内置任何省份的赋分公式。
- 未在真机验证相机、相册与 OCR 的实际识别效果（详见 `docs/VERIFY.md`）。
- 交付版本 1.0.5 弹窗全部可用 三卡按压倾斜 选科预填分组
  云端识别自动填充 对话回复自然化 输入框去除蓝色描边
  提示条缩短至 1.6 秒 设置文案精简 自定义 210ms 双向转场
