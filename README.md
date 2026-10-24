<div align="center">

# 🔋 BatteryNotify

**🔋 在状态栏与通知中心展示电池信息 · 流体云实时状态**

*LSPosed 模块 · 由 SystemUI 进程下发电池通知，展示电量、功率、电流、电压、温度等实时数据*

[![🔨 Build](https://github.com/LyonHyrik/BatteryNotify/actions/workflows/build.yml/badge.svg)](https://github.com/LyonHyrik/BatteryNotify/actions/workflows/build.yml)
[![📜 License: AGPL-3.0](https://img.shields.io/badge/License-AGPL--3.0-blue.svg)](LICENSE)
[![📱 API](https://img.shields.io/badge/API-31%2B-brightgreen.svg)](https://developer.android.com/about/versions/s)

[English](README.en.md) · 中文

</div>

---

## 📋 环境要求

- 🤖 Android 12+ (API 31+)，流体云需要 Android 16 / ColorOS 16
- 🔧 [LSPosed](https://github.com/LSPosed/LSPosed)
- ☑️ 勾选 `com.android.systemui` 与 `android`（系统框架）作用域

## 📱 功能

- 🔋 **电池信息通知** — 由 SystemUI 进程下发，展示电量、功率、电流、电压、温度、电池健康、循环次数等
- ☁️ **流体云（状态栏胶囊）** — 基于 Android 16 Live Updates，支持自定义内容与独立显示模式
- ⚡ **充电延迟显示** — 避免与系统充电流体云冲突
- 🎨 **Miuix UI** — Material You 动态取色主题
- 🌐 **国际化** — 中英文双语 UI
- 🔧 **Root Daemon** — 可选的 root 守护进程，读取更精确的 sysfs 数据

## 📥 安装

1. ⬇️ 从 [Releases](https://github.com/LyonHyrik/BatteryNotify/releases) 下载 APK
2. 📦 在 LSPosed 中安装并启用模块
3. ☑️ 勾选 `com.android.systemui` 与 `android` 作用域
4. 🔄 使用 LSPosed 软重启

## 🔨 编译

```bash
./gradlew assembleDebug
```

📦 APK 输出于 `app/build/outputs/apk/debug/app-debug.apk`

## 👤 作者

- 🌐 博客：[https://lyonhyrik.github.io/](https://lyonhyrik.github.io/)
- 📱 酷安：[LyonHyrik](https://www.coolapk.com/u/24533526)
- 💻 GitHub：[LyonHyrik](https://github.com/LyonHyrik)

## 📜 License

[GNU Affero General Public License v3.0](LICENSE)
