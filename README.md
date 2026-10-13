# Optimization for Tacz

**English** | [简体中文](#简体中文)

An all-in-one **optimization and quality-of-life add-on** for
[Timeless and Classics Zero (TaCZ)](https://www.curseforge.com/minecraft/mc-mods/timeless-and-classics-zero).

It only changes how things look and feel. It does **not** add attachment slots, does
**not** add items, and does **not** write into your gun packs.

<p align="center">
  <img src="icon.png" width="180" alt="Optimization for Tacz">
</p>

<p align="center">
  <a href="https://github.com/pyxyd/Optimization-mod"><img alt="GitHub" src="https://img.shields.io/badge/GitHub-Optimization--mod-181717?logo=github"></a>
  <a href="https://modrinth.com/mod/optimization-forge"><img alt="Modrinth" src="https://img.shields.io/badge/Modrinth-optimization--forge-00AF5C?logo=modrinth"></a>
  <img alt="Minecraft" src="https://img.shields.io/badge/Minecraft-1.20.1-62B47A">
  <img alt="Forge" src="https://img.shields.io/badge/Forge-47.4.26-DFA86A">
  <a href="LICENSE"><img alt="License: MIT" src="https://img.shields.io/badge/License-MIT-yellow"></a>
</p>

---

## Features

| # | Feature | What it does |
|---|---------|--------------|
| 1 | **One-shot kill** | Guns you list kill any non-player mob on hit. Players are never affected unless you enable it. |
| 2 | **Hybrid aim** | Tap the aim key to toggle aiming, hold it to aim only while held. Mouse and keyboard both work. |
| 3 | **Auto aim before fire** | Holding fire aims first and shoots only once the scope is up, so the shot never leaves the barrel unscoped. Per-gun on/off panel in game (default key `O`). |
| 4 | **Hit marker** | One X flashes next to the crosshair: **white** = body, **orange** = headshot, **red** = that shot killed. The shape never changes, only the colour. |
| 5 | **Hip-fire crosshair** | A Delta-Force style hollow cross whose gap follows the real TaCZ hip-fire inaccuracy of the gun in your hands. Hidden while aiming. |
| 6 | **Crouch-aim, recoil only** | While crouching **and** fully scoped in, bullet spread is forced to zero. Recoil, sound and damage are untouched. |
| 7 | **Gun knockback** | Every gun's `bullet.knockback` is set to `1`, so hits actually shove the target. |

Everything is toggled from a single config file: **`config/taczopt.properties`**.
It hot-reloads within a second -- no restart needed. Gun pack data is different: it is
read only at game startup, so changes there need a full restart.

---

## Requirements

| | |
|---|---|
| Minecraft | 1.20.1 |
| Forge | 47.4.26 (any 47.x should work) |
| TaCZ | 1.1.8 or newer |

---

## Install

1. Install Forge 47.x for Minecraft 1.20.1.
2. Drop [TaCZ](https://www.curseforge.com/minecraft/mc-mods/timeless-and-classics-zero) into `mods/`.
3. Drop `Optimization-for-Tacz-<version>-forge.jar` into `mods/`.
4. Start the game once -- `config/taczopt.properties` is created with comments
   explaining every option.

---

## Build from source

Windows only, and it needs a **JDK 17** plus a working Minecraft 1.20.1 / Forge 47.4.26
installation (the build compiles against the real Forge and TaCZ jars).

```powershell
git clone https://github.com/pyxyd/Optimization-mod.git
cd Optimization-mod
powershell -ExecutionPolicy Bypass -File .\build.ps1
```

`build.ps1` guesses the game folder as the sibling directory of this repository. Point it
somewhere else with:

```powershell
powershell -ExecutionPolicy Bypass -File .\build.ps1 -GameDir "D:\pcl2\.minecraft\versions\1.20.1-Forge_47.4.26"
```

The finished jar is written into that game folder's `mods\`.

### What the build refuses to skip

`build.ps1` will not produce a jar unless all of these pass. Every one of them exists
because that specific mistake actually happened and shipped once:

| Check | Catches |
|---|---|
| `ModsTomlCheck` | parses `mods.toml` with the same library Forge uses, so a corrupt toml cannot reach the game |
| `MixinsCheck` | a `@Mixin` class that is not listed in the mixins json (crashes the game the first time that code path runs) |
| `InjectionCheck` | a mixin target that no longer exists, or **arguments in the wrong order** -- it walks TaCZ's bytecode with ASM and derives which parameter feeds each argument instead of trusting parameter names |
| `CrosshairCheck` | crosshair geometry, spread mapping, stance priority |
| `ConfigUpgradeCheck` | legacy config values get upgraded, dead keys get removed |
| `HitFeedbackCheck` | hit-marker geometry and the white / orange / red colour rules |

### Packaging a source zip

```powershell
powershell -ExecutionPolicy Bypass -File .\tests\make_source_zip.ps1
```

Writes `Optimization-for-Tacz-<version>-src.zip` next to the repository. It refuses to
write an archive that contains build output, uses backslash entry names, or has lost the
mod icon.

---

## Repository layout

```
build.ps1                    build + verify + package the mod
src/
  com/taczopt/               Java sources
    adsfire/                 auto aim before fire (+ the in-game panel)
    bullethole/              internal, not a feature -- see the config file
    config/                  the shared config file reader/writer
    crosshair/               hip-fire crosshair and the TaCZ spread algorithm
    hitfeedback/             the hit marker
    hybridaim/               tap / hold aiming
    mixin/                   the Mixins into TaCZ and vanilla
    oneshot/                 one-shot kill
    util/                    small shared helpers
  resources/                 mods.toml, pack.mcmeta, mixins json, assets, icon
tests/                       build-time checkers and the packaging script
docs/                        user guide and developer notes
```

---

## Credits and licence

TaCZ is a separate project by its own authors. This is an **unofficial** add-on, not
affiliated with or endorsed by the TaCZ team, and it contains no TaCZ code.

The mod icon is based on TaCZ's own logo, with the author's artwork and text on top; the
TaCZ word mark remains the property of the TaCZ project, and the MIT licence below does
not grant any rights to it.

**MIT** -- do whatever you like with this mod: use it, fork it, ship it inside a modpack,
publish your own build. The only condition is that the copyright notice and the licence
text stay with it. See [LICENSE](LICENSE) for the exact terms.

---
---

# 简体中文

[English](#optimization-for-tacz) | **简体中文**

[Timeless and Classics Zero (TaCZ)](https://www.curseforge.com/minecraft/mc-mods/timeless-and-classics-zero) 的一站式**优化与体验增强附属**。

它只改变"看起来和用起来"的部分：**不**新增配件槽位、**不**新增物品、**不**往你的枪包里写任何东西。

<p align="center">
  <img src="icon.png" width="180" alt="Optimization for Tacz">
</p>

<p align="center">
  <a href="https://github.com/pyxyd/Optimization-mod"><img alt="GitHub" src="https://img.shields.io/badge/GitHub-Optimization--mod-181717?logo=github"></a>
  <a href="https://modrinth.com/mod/optimization-forge"><img alt="Modrinth" src="https://img.shields.io/badge/Modrinth-optimization--forge-00AF5C?logo=modrinth"></a>
  <img alt="Minecraft" src="https://img.shields.io/badge/Minecraft-1.20.1-62B47A">
  <img alt="Forge" src="https://img.shields.io/badge/Forge-47.4.26-DFA86A">
  <a href="LICENSE"><img alt="License: MIT" src="https://img.shields.io/badge/License-MIT-yellow"></a>
</p>

---

## 功能

| # | 功能 | 说明 |
|---|------|------|
| 1 | **一枪致死** | 你指定的枪命中非玩家生物即秒杀。默认不影响玩家，除非你手动打开。 |
| 2 | **混合瞄准** | 轻按瞄准键 = 切换开镜，长按 = 按住才开镜。鼠标、键盘都生效。 |
| 3 | **开火前自动开镜** | 按住开火会先开镜，等镜子完全抬起才真正开枪，子弹不会在没开镜时飞出去。游戏内有每把枪的开关面板（默认按键 `O`）。 |
| 4 | **击中反馈** | 准星旁闪一个 X：**白色** = 命中身体，**橙色** = 爆头，**红色** = 这一枪打死了。形状永远不变，只变颜色。 |
| 5 | **腰射准星** | Delta-Force 风格的空心十字，开口大小实时跟随手里这把枪真实的 TaCZ 腰射扩散。开镜时自动隐藏。 |
| 6 | **潜行开镜只消后坐** | 在潜行**且**完全开镜的状态下，把子弹扩散强制归零。后坐力、音效、伤害都不动。 |
| 7 | **枪械击退** | 把每把枪的 `bullet.knockback` 设为 `1`，命中真的能把目标推开。 |

所有开关都在同一个配置文件里：**`config/taczopt.properties`**，一秒内热重载，不用重启游戏。
但枪包数据不一样——它只在游戏启动时读取一次，所以改完枪包必须完整重启。

---

## 环境要求

| | |
|---|---|
| Minecraft | 1.20.1 |
| Forge | 47.4.26（47.x 任意版本应该都可以） |
| TaCZ | 1.1.8 或更新 |

---

## 安装

1. 给 Minecraft 1.20.1 装好 Forge 47.x。
2. 把 [TaCZ](https://www.curseforge.com/minecraft/mc-mods/timeless-and-classics-zero) 放进 `mods/`。
3. 把 `Optimization-for-Tacz-<版本>-forge.jar` 放进 `mods/`。
4. 启动一次游戏——`config/taczopt.properties` 会自动生成，里面每个选项都有注释说明。

---

## 从源码构建

仅支持 Windows，需要 **JDK 17**，以及一份可用的 Minecraft 1.20.1 / Forge 47.4.26 安装（构建会直接对着真实的 Forge 和 TaCZ jar 编译）。

```powershell
git clone https://github.com/pyxyd/Optimization-mod.git
cd Optimization-mod
powershell -ExecutionPolicy Bypass -File .\build.ps1
```

`build.ps1` 默认把这个仓库的相邻目录当成游戏目录。要指定别的目录：

```powershell
powershell -ExecutionPolicy Bypass -File .\build.ps1 -GameDir "D:\pcl2\.minecraft\versions\1.20.1-Forge_47.4.26"
```

构建出的 jar 会写进那个游戏目录的 `mods\` 里。

### 构建时不会跳过的检查

只要下面任何一项不过，`build.ps1` 就不会产出 jar。这些检查每一项都是因为**真的犯过、而且真的发布出去过**才加上去的：

| 检查 | 拦住的错误 |
|------|-----------|
| `ModsTomlCheck` | 用 Forge 自己那套库解析 `mods.toml`，坏掉的 toml 不可能进到游戏里 |
| `MixinsCheck` | `@Mixin` 类没有登记进 mixins json（第一次走到那段代码就会崩游戏） |
| `InjectionCheck` | mixin 目标已经不存在，或者**参数顺序反了**——它用 ASM 遍历 TaCZ 字节码，反推每个实参到底喂给了哪个形参，而不是相信参数名 |
| `CrosshairCheck` | 准星几何、扩散映射、姿态优先级 |
| `ConfigUpgradeCheck` | 旧配置值能被升级，废弃的键会被清掉 |
| `HitFeedbackCheck` | 击中反馈的几何与白 / 橙 / 红配色规则 |

### 打包源码 zip

```powershell
powershell -ExecutionPolicy Bypass -File .\tests\make_source_zip.ps1
```

在仓库旁边生成 `Optimization-for-Tacz-<版本>-src.zip`。如果压缩包里含有构建产物、条目名用了反斜杠、或者模组图标丢了，它都会拒绝生成。

---

## 仓库结构

```
build.ps1                    构建 + 校验 + 打包
src/
  com/taczopt/               Java 源码
    adsfire/                 开火前自动开镜（含游戏内面板）
    bullethole/              内部实现，不是功能 —— 见配置文件
    config/                  共用的配置文件读写
    crosshair/               腰射准星与 TaCZ 扩散算法
    hitfeedback/             击中反馈
    hybridaim/               轻按 / 长按瞄准
    mixin/                   注入 TaCZ 与原版的 Mixin
    oneshot/                 一枪致死
    util/                    小型共用工具
  resources/                 mods.toml、pack.mcmeta、mixins json、资源、图标
tests/                       构建期检查脚本与打包脚本
docs/                        用户手册与开发笔记
```

---

## 致谢与许可

TaCZ 是其作者们的独立项目。本模组是**非官方**附属，与 TaCZ 团队没有隶属关系、也未获其背书，且不包含任何 TaCZ 代码。

模组图标基于 TaCZ 自身的 logo，上面叠加了作者的美术与文字；TaCZ 字样仍归 TaCZ 项目所有，下方的 MIT 许可**不**授予任何与之相关的权利。

**MIT** —— 随便你怎么用：使用、fork、塞进整合包、发布你自己的构建版本都行。唯一的条件是把版权声明和许可原文一起保留。完整条款见 [LICENSE](LICENSE)。

---

## 更多文档

| 文档 | 内容 |
|------|------|
| [docs/USER-GUIDE.md](docs/USER-GUIDE.md) | 用户手册：8 项功能详解、全部配置键、常见问题排查 |
| [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) | 开发笔记：构建流程、Mixin 注入细节、PowerShell 踩坑记录 |
