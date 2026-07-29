# 真人圣经录音导入

Morning Grace 支持 WordProject 的两种 ZIP 命名格式：

- 旧格式：`01_GEN/GEN_001_ck7_ef10.mp3`
- 当前格式：`1/1.mp3`

导入后统一保存为 `01_001.mp3`，对应“创世记第 1 章”。

## 推荐流程

1. 从 WordProject 官方页面按卷下载音频 ZIP。
2. 在 Mac 上保留 66 个按卷 ZIP，或合并成一个 ZIP。
3. 通过 USB、Android File Transfer 或其他私人文件传输方式，把 ZIP 复制到手机
   `Download` 目录。
4. 打开 Morning Grace 设置。
5. 保持“优先使用真人圣经录音”开启。
6. 点击“导入音频 ZIP（可多选）”。
7. 选择一个整本 ZIP，或一次选择多个按卷 ZIP。
8. 等待状态显示已导入的章节数量。

全本应显示：

```text
真人录音：1189 / 1189 章
```

## 手机存储位置

导入后由 App 管理的文件位于其专属音乐目录：

```text
Android/data/com.morninggrace.app/files/Music/bible-audio/
```

Android 新版本通常不允许普通文件管理器直接浏览这个目录，这是正常的。卸载 App
可能删除这些录音，因此应在 Mac 上保留原始 ZIP。

## 播放规则

1. 播放章节标题。
2. 如果本地存在该章真人录音，则播放 MP3。
3. 如果没有录音、录音损坏或用户关闭真人录音优先，则使用中文 TTS。
4. 按读经计划继续下一章。
5. 读经结束后继续三条中文要闻。

## 版权边界

WordProject 允许其音频用于非营利传福音或教育，但不允许未经批准从第三方 App
直接链接其服务器，也不允许把音频用于收费或带广告的 App。

因此 Morning Grace：

- 不在 App 内热链 WordProject。
- 不把录音提交到 GitHub。
- 不把录音打进公开 APK。
- 只导入用户自己合法取得的本地文件。
