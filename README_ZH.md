# Morning Grace （晨光）

老人友好的华人定时播报 Android 应用。闹钟触发后自动连续播报天气、三条中文
要闻和当天读经；没有网络时会跳过天气与新闻，直接播放本地真人圣经录音。

## 模块
- `core` — 共享数据模型与工具类
- `alarm` — 闹钟调度与前台服务
- `orchestrator` — 晨间播报流程状态机
- `bible` — 圣经文本、365 天读经表与本地真人录音
- `orchestrator` — 中国气象局天气、中文新闻与播报顺序
- `tts` — 中文系统语音

## 构建

```bash
./gradlew assembleDebug
./gradlew test
```
