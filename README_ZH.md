# Morning Grace （晨光）

老人友好的华人晨间播报 Android 应用。闹钟触发后自动连续播报天气、圣经读经和三条中文要闻。

## 模块
- `core` — 共享数据模型与工具类
- `alarm` — 闹钟调度与前台服务
- `orchestrator` — 晨间播报流程状态机
- `bible` — 圣经文本与读经计划（计划 2）
- `weather` — 天气数据（计划 4）
- `tts` — 中文系统语音

## 构建

```bash
./gradlew assembleDebug
./gradlew test
```
