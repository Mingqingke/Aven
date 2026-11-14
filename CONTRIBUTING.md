# 参与开发

## 环境

- JDK 21
- Android SDK Platform 37 与 Build Tools 36.0.0
- Gradle 9.4.1

## 构建

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

Release 构建需要根目录 `keystore.properties` 指向签名文件 该文件与 jks 均不应提交

## 代码约定

- 成绩计算放在 core/calc 纯 Kotlin 可单测
- 数据读写只经过 core/Store
- 界面统一使用 MIUIX 组件
- 文案不使用标点 保持短句
- 新增数据库字段必须升 version 并提供 Migration

## 提交

- 一次提交只解决一个问题
- 提交前跑单元测试与 assembleDebug
- 不提交 API Key 签名文件 个人数据
