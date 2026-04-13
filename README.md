# Morning Grace （晨光）

晨间 AI 助理 Android App. Alarm + weather + Bible reading + AI conversation.

## Modules
- `core` — shared models and utilities
- `alarm` — alarm scheduling and foreground service
- `orchestrator` — morning session flow state machine
- `bible` — Bible text and reading plans (Plan 2)
- `weather` — weather data (Plan 4)
- `finance` — financial news (Plan 4)
- `tts` — text-to-speech abstraction (Plan 3)
- `voice` — speech recognition (Plan 5)
- `ai` — AI client abstraction (Plan 5)

## Build

```bash
./gradlew assembleDebug
./gradlew test
```
