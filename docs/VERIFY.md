# 验证记录

日期：2026-11-13

## 已执行并通过

| 检查 | 方式 | 结果 |
|---|---|---|
| 依赖真实存在 | Maven 实际下载：MIUIX 0.9.4、Room 2.8.5、ML Kit text-recognition-chinese 16.0.1、OkHttp 5.5.0、Coil 2.7.0 | 通过 |
| MIUIX API 对齐 | 读取官方 sources jar，核对 Card / TextField / Switch / TabRow / NavigationBar / OverlayDialog / Preference / NavController 签名 | 通过 |
| DeepSeek 能力核实 | 访问官方文档与定价页 | `deepseek-flash` 支持图片输入；`deepseek-v4-pro` 仅文本；Chat Completions 为 OpenAI 兼容格式 |
| 成绩引擎单测 | `testDebugUnitTest`（ScoreEngineTest 9 项） | 9/9 通过 |
| 成绩单解析单测 | `testDebugUnitTest`（SheetParserTest 6 项） | 6/6 通过 |
| Debug 构建 | `assembleDebug` | BUILD SUCCESSFUL |
| Release 构建 | `assembleRelease` | BUILD SUCCESSFUL |
| APK 签名 | `apksigner verify --print-certs` | v2/v3 签名有效，CN=Aven |
| APK 元数据 | `aapt2 dump badging` | com.aven.app / 1.0.5 / versionCode 6 / minSdk 26 / targetSdk 37 |
| OCR 模型内置 | 解包检查 `assets/mlkit-google-ocr-models` 与 `libmlkit_google_ocr_pipeline.so` | 中文模型随 APK 打包 |

交付物：

```
/workspace/Aven-v1.0.5.apk
sha256 186030b603a1bbf56e6f7d549a87ea577975c13cfd191ecbb84c01e8d3f88259
versionCode 6 / versionName 1.0.5
签名 SHA-256 454cb336602bdc2ff2face5303623f3754e852129246f215ea1cea0c825fef87
```

## 未执行（不计为通过）

- 真机或模拟器安装、启动、页面交互（当前环境没有 Android 设备桥接，无法安装 APK）。
- 相机拍摄、相册选择、照片导入的实机行为。
- ML Kit 中文 OCR 对真实成绩单的识别准确率。
- DeepSeek 实际请求（需要用户自己的 API Key，本次未提供）。
- 更新检查 下载 校验 安装 全链路（当前没有可用的清单地址与 APK 下载地址 未做真实请求验证）。
- GitHub Actions 工作流（当前环境没有 GitHub Runner）。
- 深色模式、字体缩放、平板大屏的实机观感。
- 进程被杀的恢复、极端数据量下的滚动性能。

这些限制同样写在 README 的「已知限制」中。


## 发布

- 仓库 https://github.com/Mingqingke/Aven
- Release https://github.com/Mingqingke/Aven/releases/tag/v1.0.6
- 资产 SHA-256 与 GitHub 记录一致 b840653ced1badbe96552fa8b0f670ca9408a8046a4f2a6a87beae27c044da52
- raw 清单与 jsDelivr 镜像均返回 200
- GitHub API releases/latest 返回 v1.0.6 与 APK 资产
- GitHub Actions 首次运行因 setup-android 的 tools 包缺失失败 已改为使用预装 SDK
