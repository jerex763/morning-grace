# Morning Grace （晨光）

海外华人晨间 AI 助理 Android 应用。闹钟触发后依次播报天气、圣经读经、财经新闻，随后进入 AI 语音对话模式。

## 模块
- `core` — 共享数据模型与工具类
- `alarm` — 闹钟调度与前台服务
- `orchestrator` — 晨间播报流程状态机
- `bible` — 圣经文本与读经计划（计划 2）
- `weather` — 天气数据（计划 4）
- `finance` — 财经新闻（计划 4）
- `tts` — TTS 抽象层（计划 3）
- `voice` — 语音识别（计划 5）
- `ai` — AI 客户端抽象层（计划 5）

## 构建

```bash
./gradlew assembleDebug
./gradlew test
```
