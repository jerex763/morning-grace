# 正式版签名与升级

老人手机首次安装前必须固定同一把正式签名密钥。以后每次升级都使用它，
否则 Android 无法覆盖安装，卸载旧版还会删除 App 专用目录中的真人录音。

在项目根目录创建不会提交到 GitHub 的 `keystore.properties`：

```properties
storeFile=/安全备份目录/morning-grace-release.jks
storePassword=自行设置的强密码
keyAlias=morning-grace
keyPassword=自行设置的强密码
```

使用 JDK `keytool` 创建密钥，并至少做两份离线备份。密钥和密码均不得提交
到 GitHub、聊天记录或 APK 中。

```bash
keytool -genkeypair \
  -keystore /安全备份目录/morning-grace-release.jks \
  -alias morning-grace \
  -keyalg RSA -keysize 4096 -validity 10000
```

随后运行：

```bash
./gradlew clean assembleRelease
apksigner verify --verbose app/build/outputs/apk/release/app-release.apk
```

项目会在缺少正式签名配置时主动阻止 Release 构建，避免误发 unsigned APK。
