# COC阵型 APP

部落冲突阵型浏览与跳转工具。

## 功能

- 从腾讯文档自动拉取阵型图片和链接
- 图片网格展示，点击跳转游戏应用阵型
- 下拉刷新
- 本地缓存（图片+数据），启动时自动检查更新

## 构建步骤

1. 用 Android Studio 打开此项目
2. 等待 Gradle 同步完成
3. 连接手机或启动模拟器
4. 点击 Run 运行

## 数据源配置

在 `LayoutRepository.kt` 中修改两个常量：

```kotlin
const val DOC_LINKS = "DVHhFcERmZE9jTENm"    // 阵型链接文档ID
const val DOC_IMAGES = "DVE9iY1B5SEVsenZs"   // 对应图片文档ID
```

文档ID就是腾讯文档URL中 `/doc/` 后面的那串字符。

## 更新数据

在腾讯文档中增删阵型链接和对应图片，APP 下次启动或下拉刷新时自动同步。

## 技术栈

- Kotlin + Jetpack Compose
- Material 3
- Hilt (依赖注入)
- Room (本地缓存)
- Retrofit (网络请求)
- Coil (图片加载)
