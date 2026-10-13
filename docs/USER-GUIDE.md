# User Guide

**English** | [简体中文](#简体中文)

---

## What this is

**Optimization for Tacz** is an add-on for
[TaCZ (Timeless and Classics Zero)](https://modrinth.com/mod/tacz) that merges eight
"look and feel" changes into a single jar.

The whole point is **optimization**: it adds no attachment slots, no items, and writes
nothing into your gun packs.

---

## Requirements

| | Version |
|---|---|
| Minecraft | 1.20.1 |
| Forge | 47.4.26 (any 47.x should work) |
| TaCZ | 1.1.8 or newer |

---

## Install

1. Install Forge 47.x for 1.20.1.
2. Drop TaCZ into `mods\`.
3. Drop `Optimization-for-Tacz-<version>-forge.jar` into `mods\`.
4. Start the game once -- `config/taczopt.properties` is generated with a comment on
   every option.

---

## The eight features

### 1) Custom bullet holes

The hole left in a block uses your own texture, floats one layer above the vanilla
bullet hole, and fades out after a while.

- By default it only applies to guns whose display name contains a `§` colour code --
  in other words, the skinned guns in your gun pack.
- Widen or narrow it with `hole.guns`: `auto` (detect automatically) / `*` (every gun) /
  an exact id / wildcards (`*` and `?`).
- `hole.hole_scale_guns` separately controls *which guns get the vanilla hole resized*;
  default `*`.

### 2) One-shot kill for chosen guns

Guns listed in `oneshot.guns` kill a mob outright on hit.

- Default is AWM only (`tacz:ai_awp`).
- **Players are safe by default** (`oneshot.affect_players=false`).
- Add more guns to `oneshot.guns`, comma separated.

### 3) Hybrid aim

The aim key works two ways: **tap = toggle** aiming on/off, **hold = aim only while
held, release to un-aim**. Both mouse and keyboard bindings are supported.

- `hybridaim.short_press_ms` defaults to 250 ms -- how short a press counts as a tap.
- `hybridaim.guns` limits which guns it applies to; default `*`.

### 4) Auto aim before fire

Holding fire **aims first and only shoots once the scope is fully up**, so the bullet
never leaves the barrel unscoped.

- Press **`O`** in game for the per-gun on/off panel.
- `adsfire.aim_wait_ticks` defaults to 4 -- how many ticks it will wait for the scope.
- `adsfire.default` is the starting state for a gun it has never seen.
- `adsfire.release_when_stop` un-aims after you let go.

### 5) Hit marker

A single X flashes next to the crosshair on hit. **The shape never changes -- only the
colour tells you what happened:**

| Colour | Meaning |
|---|---|
| White `#FFFFFF` | hit a non-head part |
| Orange `#FFA500` | headshot |
| Red `#FF5555` | that shot killed the target (highest priority, beats the headshot colour) |

There is also a very quiet hit-confirm sound and a tiny 90 ms camera shake.

> Earlier versions drew **two stacked X's** for a headshot. Measured, the two centres sat
> only 14 px apart and each X was 10 px wide, leaving 5-6 px between them -- at a normal
> viewing distance they blur into one blob, so nobody could read it as two X's. It is now
> a single orange X.

### 6) Hip-fire crosshair

A hand-drawn Delta-Force style **hollow cross** whose gap is driven by the **real
hip-fire inaccuracy** of the gun in your hands (it reuses TaCZ's own stance table and
spread algorithm -- nothing is estimated). Hidden while aiming.

- `crosshair.spread_scale` defaults to 2.5 -- measured, bullet holes land about 2.3x
  further from the crosshair centre than the crosshair itself predicts, so the gap needs
  a multiplier. Raise or lower it if the cross opens too far or too tight.
- `crosshair.thickness` / `arm` / `dot` are line width, arm length and the centre dot.
  They are **independent** of the multiplier.

### 7) Crouch-aim: recoil only, no spread

While sneaking **and** fully scoped in, bullet spread is forced to 0. Recoil, sound and
damage are all untouched.

> Two different things are easy to confuse here: **hip-firing while crouched** and
> **aiming while crouched**. TaCZ's accuracy has five mutually exclusive stances; aiming
> while crouched uses `aim`, hip-firing while crouched uses `sneak`.
> This mod sets `inaccuracy.sneak` to 0 in all 119 gun data files (so crouched hip-fire is
> also tight), and the crouched-aim zero spread is done in code by zeroing the spread
> right before the shot is fired.

### 8) Gun knockback

Every gun's `bullet.knockback` in the gun pack data is set to `1`, so a hit actually
shoves the target.

- This is a **gun pack data** change, not a code change.
- Gun pack data is read only at **game startup**, so this one needs a **full restart**.

---

## Config file

Everything lives in one file:

```
config/taczopt.properties
```

**It hot-reloads within a second -- no restart needed.** On first run the file is
rewritten with comments explaining each option.

### Every key

```
# bullet holes
hole.enabled  hole.guns  hole.texture  hole.scale  hole.alpha
hole.flip_u  hole.flip_v  hole.hole_scale  hole.hole_scale_guns
hole.logo_hold  hole.logo_fade  hole.logo_lift

# one-shot kill
oneshot.enabled  oneshot.guns  oneshot.affect_players
oneshot.damage  oneshot.force_kill

# hybrid aim
hybridaim.enabled  hybridaim.short_press_ms  hybridaim.guns

# auto aim before fire
adsfire.enabled  adsfire.default  adsfire.aim_wait_ticks
adsfire.hold_ticks  adsfire.release_when_stop  adsfire.debug_log
adsfire.gun.<gun id>          per-gun switch (this is what the in-game O panel writes)

# hit marker
hit.enabled  hit.duration_ms  hit.scale  hit.sound  hit.shake
hit.offset_x  hit.offset_y  hit.debug_log

# hip-fire crosshair
crosshair.enabled  crosshair.color  crosshair.thickness  crosshair.arm
crosshair.gap  crosshair.dynamic  crosshair.gap_multiplier  crosshair.gap_max
crosshair.dot  crosshair.hide_on_aim  crosshair.only_guns
crosshair.spread_scale  crosshair.debug_log
```

> `hit.headshot_size` and `hit.headshot_spacing` are keys from the "two stacked X's" era.
> They are **deprecated** and leftovers in an old config are removed automatically.

---

## Troubleshooting

Search the log for `[taczopt]`. On startup you should see:

```
[taczopt] Optimization for Tacz loaded: bullet holes / one-shot / hybrid aim / ads fire / hit marker / hip-fire crosshair; config config/taczopt.properties
```

Useful debug switches:

- `adsfire.debug_log=true` -- every "held a shot back for the scope" prints its source
  and the real aim progress.
- `hit.debug_log=true` -- one line per hit (headshot / killed / target).
- `crosshair.debug_log=true` -- prints the crosshair gap it computed.

You should **never** see the words "barrel" / `barrelslot` / "barrel slot" again. If you
do, an old jar is still lying around.

### FAQ

**Q: I changed gun pack data (knockback, for example) and nothing happened.**
Gun packs are read only at game startup. You need a **full restart** -- reloading the
world is not enough.

**Q: I changed `config/taczopt.properties` and nothing happened.**
Wait one second. It hot-reloads by checking the file timestamp once per second. If it
still does not take, make sure the file encoding is UTF-8.

**Q: Why is there no icon in PCL's mod list?**
When reading a local mod jar, PCL only parses the **text metadata** in `mcmod.info` /
`fabric.mod.json` / `META-INF/mods.toml`. It never reads images out of the jar; icons come
from its online database cache. A locally compiled mod matches no database entry, so it
shows blank. **Forge's in-game Mods list displays it fine**, because that one reads
`icon.png` from the jar root.

---
---

# 简体中文

[English](#user-guide) | **简体中文**

---

## 这是什么

**Optimization for Tacz** 是 [TaCZ（永恒枪械工坊：零）](https://modrinth.com/mod/tacz)
的一个附加模组，把八项"观感与手感"的改动合并到了一个 jar 里。

定位就是**优化**：它不新增配件槽位、不新增物品、不往枪包里写任何东西。

---

## 环境要求

| 项目 | 版本 |
|---|---|
| Minecraft | 1.20.1 |
| Forge | 47.4.26（47.x 任意版本应该都能用） |
| TaCZ | 1.1.8 或更新 |

---

## 安装

1. 装好 Forge 47.x（1.20.1）。
2. 把 TaCZ 放进 `mods\`。
3. 把 `Optimization-for-Tacz-<版本>-forge.jar` 放进 `mods\`。
4. 启动一次游戏 —— 会自动生成 `config/taczopt.properties`，里面有注释说明每一项。

---

## 八项功能

### 1) 自定义弹孔

方块上留下的弹孔换成你自己的贴图，悬浮在原版弹孔上方一层，显示一段时间后淡出。

- 默认只对"显示名里带 `§` 颜色码"的枪（也就是枪包里带皮肤名的那批）生效。
- 想改范围就调 `hole.guns`：`auto`（自动识别） / `*`（全部枪） / 精确 id / 通配符（`*` 和 `?`）。
- `hole.hole_scale_guns` 单独控制"哪些枪要改原版弹孔的大小"，默认 `*`。

### 2) 指定枪械秒杀

`oneshot.guns` 里列出的枪，打中生物直接致命。

- 默认只有 AWM（`tacz:ai_awp`）。
- **默认不误伤玩家**（`oneshot.affect_players=false`）。
- 想加别的枪就写进 `oneshot.guns`，逗号分隔。

### 3) 混合开镜

开镜键**短按 = 切换**开关、**长按 = 按住开镜、松手收镜**。鼠标和键盘绑定都支持。

- `hybridaim.short_press_ms` 默认 250 毫秒，决定"多短算短按"。
- `hybridaim.guns` 控制对哪些枪生效，默认 `*`。

### 4) 开火自动开镜

按住左键时**先自动开镜，镜走完再开火**，保证子弹出膛时已经在镜里。

- 游戏内按 **`O`** 打开逐把枪的开关面板。
- `adsfire.aim_wait_ticks` 默认 4 —— 最多等几个 tick 让镜走完。
- `adsfire.default` 是新枪的默认状态。
- `adsfire.release_when_stop` 松手后是否收镜。

### 5) 命中标记

命中时在准星旁闪一个 X。**形状永远不变，只用颜色区分**：

| 颜色 | 含义 |
|---|---|
| 白色 `#FFFFFF` | 打到非头部部位 |
| 橙色 `#FFA500` | 打到头部 |
| 红色 `#FF5555` | 这一枪打死了目标（优先级最高，压过爆头） |

同时有一声很轻的命中确认音，和 90 毫秒的极小镜头抖动。

> 之前的版本爆头会显示"两个上下叠着的 X"，但实测两个 X 的中心距离只有 14 像素、
> 各自宽 10 像素，中间只剩 5~6 像素，正常坐姿看屏幕会连成一团，读不出是两个 X，
> 所以改成了橙色单 X。

### 6) 腰射准星

自己画的三角洲风格**空心十字**，间距由你当前手上那把枪的**真实腰射扩散**驱动
（用的是 TaCZ 原版的档位与扩散算法，不是估的）。开镜时自动隐藏。

- `crosshair.spread_scale` 默认 2.5 —— 实测弹孔离准星中心的距离约为准星的 2.3 倍，
  所以间距要乘一个系数。嫌准星张得太开/太紧就调它。
- `crosshair.thickness` / `arm` / `dot` 是线宽、线段长、中心点，**各自独立**，不受倍率影响。

### 7) 蹲下瞄准 = 只有后坐，没有扩散

潜行（蹲下）**并且开镜到位**时，子弹扩散被强制为 0。后坐力、音效、伤害全部不动。

> 注意区分两件事：**蹲着腰射**和**蹲着开镜**。
> TaCZ 的精度分五档且互斥，蹲着开镜生效的是 `aim` 档，蹲着腰射生效的是 `sneak` 档。
> 本模组把 119 把枪数据里的 `inaccuracy.sneak` 改成了 0（蹲着腰射也不散），
> 蹲着开镜的零扩散则由代码在开火前把扩散置 0 实现。

### 8) 枪械击退

枪包数据里所有枪的 `bullet.knockback` 都改成了 `1`，打中目标会真的把它推开。

- 这是**改枪包数据**，不是改代码。
- 枪包数据只在**游戏启动时**读取，所以改这里需要**完全重启游戏**。

---

## 配置文件

所有开关都在一个文件里：

```
config/taczopt.properties
```

**保存后 1 秒内热重载，改配置不用重启游戏。** 文件在首次运行时会被重写并带上中文注释。

### 完整键名

```
# 弹孔
hole.enabled  hole.guns  hole.texture  hole.scale  hole.alpha
hole.flip_u  hole.flip_v  hole.hole_scale  hole.hole_scale_guns
hole.logo_hold  hole.logo_fade  hole.logo_lift

# 秒杀
oneshot.enabled  oneshot.guns  oneshot.affect_players
oneshot.damage  oneshot.force_kill

# 混合开镜
hybridaim.enabled  hybridaim.short_press_ms  hybridaim.guns

# 开火自动开镜
adsfire.enabled  adsfire.default  adsfire.aim_wait_ticks
adsfire.hold_ticks  adsfire.release_when_stop  adsfire.debug_log
adsfire.gun.<枪械ID>          每把枪单独开关（游戏内按 O 面板写的就是这个）

# 命中标记
hit.enabled  hit.duration_ms  hit.scale  hit.sound  hit.shake
hit.offset_x  hit.offset_y  hit.debug_log

# 腰射准星
crosshair.enabled  crosshair.color  crosshair.thickness  crosshair.arm
crosshair.gap  crosshair.dynamic  crosshair.gap_multiplier  crosshair.gap_max
crosshair.dot  crosshair.hide_on_aim  crosshair.only_guns
crosshair.spread_scale  crosshair.debug_log
```

> `hit.headshot_size` 和 `hit.headshot_spacing` 是"爆头双 X"时代的键，**已废弃**，
> 老配置文件里残留的会被自动清除。

---

## 排错

日志里搜 `[taczopt]`。启动时应该看到：

```
[taczopt] Optimization for Tacz 已加载：弹孔 / 秒杀 / 混合开镜 / 开火自动开镜 / 命中标记 / 腰射准星；配置 config/taczopt.properties
```

几个常用的调试开关：

- `adsfire.debug_log=true` —— 每次"拦下一发等开镜"都会打印来源，以及真实开镜进度。
- `hit.debug_log=true` —— 每次命中打印一行（爆头 / 致死 / 目标）。
- `crosshair.debug_log=true` —— 打印准星算出来的间距。

**不该**再看到任何"枪管" / `barrel` / "枪管槽位"字样。看到就说明有旧 jar 没删干净。

### 常见问题

**Q：改了枪包数据（比如击退）没生效？**
枪包只在游戏启动时读取，必须**完全重启游戏**，重进存档没用。

**Q：改了 `config/taczopt.properties` 没生效？**
等 1 秒。它是每秒检查一次文件时间戳热重载的。如果还没生效，确认文件编码是 UTF-8。

**Q：为什么 PCL 的模组列表里没有图标？**
PCL 读本地 mod 的 jar 时只解析 `mcmod.info` / `fabric.mod.json` / `META-INF/mods.toml`
这些**文本元数据**，不读 jar 里的图片；图标来自它的在线数据库缓存。本地自己编译的
模组匹配不到数据库条目，所以显示空白。**游戏内 Forge 的 Mods 列表是能正常显示的**，
那里读的是 jar 根目录的 `icon.png`。
