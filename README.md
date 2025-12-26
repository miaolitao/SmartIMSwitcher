# Smart IM Switcher for IntelliJ IDEA

[![JetBrains IntelliJ Platform](https://img.shields.io/badge/IntelliJ-2024.3+-blue.svg)](https://plugins.jetbrains.com/)
[![macOS](https://img.shields.io/badge/Platform-macOS-green.svg)](https://www.apple.com/macos/)

一款为 macOS 开发者量身定制的 **智能输入法切换插件**，能够根据代码上下文自动切换中英文输入法，让您在编码时无需手动切换，专注于创作。

---

## ✨ 核心功能 (Core Features)

### 🎯 智能场景识别
- **代码区域** → 自动切换至 **英文输入法** (如 ABC)
- **注释区域** (单行/多行/文档注释) → 自动切换至 **中文输入法** (如搜狗拼音)
- **字符串字面量** → 可配置为中文或英文
- **Git 提交对话框** → 自动切换至中文

### 🚀 高性能切换引擎
- **Carbon API 原生调用**：抛弃传统 AppleScript，采用 macOS 原生 `TISSelectInputSource` 接口
- **毫秒级响应**：输入法切换几乎无感知延迟
- **智能缓存**：避免重复切换，降低系统开销

### ⚙️ 灵活配置
- **多语言支持**：独立配置 Java、Kotlin、Python 等不同语言的切换策略
- **自动同步**：基础配置变更时，自动同步到所有语言场景
- **离开 IDE 模式**：可配置 IDE 失焦时自动切换的输入法
- **光标颜色提示**：中英文模式下显示不同颜色的光标

---

## 📦 安装方式 (Installation)

### 方式一：从磁盘安装
1. **下载/打包** 插件 ZIP 文件 (见下方打包说明)
2. 打开 IntelliJ IDEA → `Settings` → `Plugins`
3. 点击齿轮图标 ⚙️ → `Install Plugin from Disk...`
4. 选择 `build/distributions/SmartIMSwitcher-x.x.x.zip`
5. 重启 IDE

### 方式二：从 Marketplace 安装 (待发布)
搜索 **Smart IM Switcher** 即可一键安装。

---

## 🔧 构建与打包 (Building)

### 快速打包
```bash
# 使用 Gradle Wrapper
./gradlew buildPlugin

# 或使用本地 Gradle
gradle buildPlugin
```

### 打包产物
成功后，安装包位于：
```
build/distributions/SmartIMSwitcher-<version>.zip
```

### 版本管理
项目采用自动版本递增机制：
- **开发版**：`x.y.z-SNAPSHOT`
- **正式版**：`x.y.z` (通过 `-Prelease` 参数触发)

```bash
# 打正式包 (自动递增版本号)
./gradlew buildPlugin -Prelease
```

---

## 🛠️ 开发环境 (Development)

| 依赖项 | 版本要求 |
|--------|---------|
| Java | 21+ |
| Gradle | 8.10+ |
| IntelliJ SDK | 2024.3+ |
| 操作系统 | macOS 11+ |

### 本地调试
```bash
# 启动带有插件的沙盒 IDE
./gradlew runIde
```

---

## ⚠️ 权限说明 (Permissions)

> **重要**：首次使用需授权辅助功能权限

1. 打开 `系统设置` → `隐私与安全性` → `辅助功能`
2. 点击 `+` 添加 **IntelliJ IDEA**
3. 确保其复选框已勾选

如果不授权，插件将无法控制系统输入法切换。

---

## 📁 项目结构 (Project Structure)

```
SmartIMSwitcher/
├── src/main/java/com/example/smartim/
│   ├── core/           # 上下文检测核心逻辑
│   ├── im/             # 输入法服务 (Carbon API)
│   ├── listener/       # IDE 事件监听器
│   └── settings/       # 配置界面与持久化
├── src/main/resources/
│   ├── META-INF/plugin.xml  # 插件清单
│   └── im-switch            # 原生输入法切换工具
└── build.gradle.kts         # Gradle 构建脚本
```

---

## 🙏 致谢 (Credits)

- 灵感来源：[Smart Input](https://developer.aliyun.com/article/1440836)
- 输入法切换实现参考：[im-select](https://github.com/daipeihust/im-select)

---

## 📄 许可证 (License)

MIT License - 详见 [LICENSE](LICENSE) 文件
