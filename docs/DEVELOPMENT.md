# 开发笔记

[English](../README.md) | **简体中文**

这份文档记录**为什么代码长成这样**。每一条都对应一次真实的故障，不是预防性建议。
改动之前先读一遍，能省掉重新踩一遍的时间。

---

## 一、项目定位（重要）

这是个**优化**模组：只改观感与手感，**不新增槽位、不新增物品、不往枪包里塞内容**。

`build.ps1` 里有一条硬检查：打包后如果 jar 里出现任何 `barrelslot` / `BarrelSlotMixin` /
`refit_slot_icons.png` 之类的残留，**直接构建失败**。

---

## 二、构建流程

```
build.ps1
  ├─ 1) 拼 classpath（版本 json → libraries\ + forge client jar + TaCZ jar）
  ├─ 2) javac 编译 src\**\*.java
  ├─ 3) 打包（classes + resources → staging → jar）
  ├─ 3b) ModsTomlCheck      用 Forge 同款 night-config 真解析 mods.toml
  ├─ 3c) MixinsCheck        扫描 @Mixin 类，核对 mixins.json 有没有漏注册
  ├─ 3d) InjectionCheck     ASM 回溯 TaCZ 字节码，验注入点和实参顺序
  ├─ 3e) CrosshairCheck / ConfigUpgradeCheck / HitFeedbackCheck
  ├─ 4) 校验 jar 内容（图标字节一致、链接在、无 ITransformationService、无枪管残留）
  └─ 5) 清理游戏目录里的旧 jar / 旧枪包
```

**为什么校验器不是可选的**：这六项每一条都对应一次"改了没用"或"游戏崩了"。
尤其 `InjectionCheck` —— 它验的是**参数顺序**，见第四节。

---

## 三、PowerShell 的坑（build.ps1 必须保持纯 ASCII）

### 3.1 中文字面量会被按 ANSI 读成乱码

Windows PowerShell 5.1 读**没有 BOM** 的 `.ps1` 时用系统 ANSI 码页。中文字面量会变成
乱码落到磁盘上（文件名会真的写错）。所以：

- `build.ps1` / `tests\make_source_zip.ps1` 里的**每一处字符串都要是 ASCII**，
  包括 `throw` 的消息；
- 输出 jar 的文件名用**码点拼**：
  ```powershell
  $OutPrefix = "[TaCZ" + [char]0xFF1A + [char]0x4F18 + [char]0x5316 + "]"
  ```
  `U+FF1A` 是**全角冒号**，是文件名的一部分，**不要"修正"成半角 `:`**。

### 3.2 原生命令的 stderr 会中止脚本

`$ErrorActionPreference = "Stop"` 下，PowerShell 会把原生命令的 stderr 包装成
`NativeCommandError`。而 **javac 会把普通提示写到 stderr**（"使用了未经检查的操作"、
"[removal] ... 已过时"）—— 结果一个**编译成功**的构建死在 javac 那一行，什么都没打包。

试过并放弃的方案：

| 方案 | 为什么不行 |
|---|---|
| `2>$null` | 真正的编译错误也在 stderr，会一起吞掉 |
| 走 `cmd.exe /c "... 2>file"` | cmd 命令行上限 8191 字符，而本项目**光 classpath 就约 11300 字符**，分批也放不下 |
| javac 的 `@argfile` | 它按平台默认字符集读文件，UTF-8 和 `Encoding.Default` 都试过，中文 jar 名始终对不上，javac 一直报 `invalid flag: tacz-1.20.1-...jar` |

**最终方案**：用 PowerShell 的 `&`（没有长度限制、引号也正确），只把 **stderr 重定向到临时文件**。
`2>` 重定向不会产生 error record，只是把字节写到能读回来的地方。

### 3.3 原生命令的 stdout 会被算进函数返回值

```powershell
& $Exe @args
return $LASTEXITCODE      # ✗ 返回的是"输出行数组 + 退出码"
```

于是 `$rc -ne 0` **永远为真**。真实症状：`ModsTomlCheck` 打印了 `TOML ok: ...`，
脚本却报 `mods.toml validation failed`。

**做法**：先把 stdout 收进变量，再取 `$LASTEXITCODE`。

### 3.4 不要只信 `$LASTEXITCODE`

一个什么都不打印的工具可能留下**过期的**退出码。真实事故：`MixinsCheck.class`
根本没生成，退出码却是好的，下一步直接 `ClassNotFoundException: MixinsCheck`。
**做法**：拿退出码之后，**再确认产物文件存在**。

### 3.5 逗号比 `+` 结合得更紧

```powershell
@($v + '/a', $v + '/b')     # ✗ 解析成 $v + ('/a','/b')
                            #   → 数组拼接 → 一个字符串 "/a /b"
                            #   → 1 个元素，-contains 永远 False
@("$v/a", "$v/b")           # ✓ 2 个元素
@(($v + '/a'), ($v + '/b')) # ✓ 也可以
```

实测：`@($v+'/x',$v+'/y').Count` 是 **1**，`@("$v/x","$v/y").Count` 是 **2**。
真实症状：zip 校验报"缺 build.ps1 resources/mods.toml ..."，**五个路径用空格连成一条**。

### 3.6 其他

- `-contains` / `-notcontains` 要把**集合写在左边**。
- `New-Object Type(a, b)` 会被解析成**数组参数**，要写 `-ArgumentList`。
- **变量名不区分大小写**：函数参数 `$Source` 会被局部变量 `$source` **静默覆盖**。
  真实事故：`$source = Measure-Lettering ...` 把图像对象换成了测量结果的 hashtable，
  于是 `$Source.Width` 变成 `$null`、scale 变成 0，而所有校验都"通过"了。
- `.NET` 在 Windows 上用 `ZipFile.CreateFromDirectory` 生成的 zip，条目名用**反斜杠**，
  而 ZIP 规范要求 `/`。Linux/macOS 解压会出错。**做法**：自己逐条 `CreateEntry`，用 `/`。

---

## 四、Mixin 注入：只认字节码，不要看形参名

`CrouchAimSpreadMixin` 注入 `AbstractGunItem.doBulletSpread`，在把扩散交给弹道之前把它置 0。

**这个坑踩过两次，两次症状正好相反：**

1. 写成 `(…, float velocity, float degree)`（以为 T5 是扩散）——
   实际把 **T5 = 弹速置成了 0**，扩散原样放过。
   症状：**子弹落地、击退变零**，而且"还是有扩散"。
2. 后来按"形参名"再换一次 —— 又换回了错的顺序，复现同一个 bug。

**结论**：TaCZ 那几个 float 形参的**名字和语义是反的**。唯一可靠的办法是用
`tests\InjectionCheck.java` 读字节码、回溯 `Projectile.m_37251_` 调用点的**操作数栈**，
得出每个实参来自哪个方法参数：

```
m_37251_(Entity, x,   y,        z,       velocity, inaccuracy)
                  │    │        │        │         │
               fload8 fload9  fconst_0  fload6    fload7
               =参数7 =参数8    常量     =参数5    =参数6
```

所以处理器形参表是 `(…, Projectile, int, float T5, float T6, float T7, float T8)`，
**T5 = 弹速（必须原样转发）、T6 = 扩散角（要置 0 的就是它）**。

处理器里形参故意命名 `t5..t8` 而不是 `velocity/degree`，就是为了不再"按感觉猜"。
**顺序写错时 `build.ps1` 会直接构建失败。**

### 另一个相关坑：`@ModifyVariable(ordinal=N)`

早期版本用 `ordinal` 定位局部变量槽位，**静默失效**（`require = 0` 不报错），
用户实测"还是有扩散"。现在改成按**方法名 + 参数位置**注入，不存在猜槽位的问题。

---

## 五、配置迁移

`src\com\taczopt\config\ConfigFile.java` 是唯一入口，规则是"**已有配置优先**"——
只要键已经存在于玩家的配置文件里，就**永远不会**被新的默认值更新。

所以每次改默认值都必须往 `RENAMED_DEFAULT` 里加一条：

```java
{ "hit.offset_y", "14", "3" },   // 只有当前值正好等于某个旧默认值时才改
```

**真实事故**：`hit.offset_y` 的默认值改过好几轮，老配置里还是 14，标记继续偏下，
用户看到的结论是"改了没用"。

删掉一个键时必须往 `DEPRECATED_KEYS` 里加，否则它会永远留在玩家配置里，
看着像是还有那个功能。爆头从"双 X"改成橙色单 X 之后，
`hit.headshot_size` / `hit.headshot_spacing` 就是这么处理的。

这两条都有离线测试守着：`tests\ConfigUpgradeCheck.java`。

> ⚠ 那个校验器**只能用一个临时目录**：`ConfigFile.FILE` 是 `static final`，
> 在类加载时就按系统属性定死了，而且 `ensureLoaded()` 只跑一次迁移。
> 所以在同一个 JVM 里换目录再调用是没用的（第一版就这么写，结果读到的是上一次的配置）。

---

## 六、图标

`src\resources\icon.png` 是**手工制作的成品图**，构建时**逐字节复制**进 jar 根目录，
**不要过任何图像库**——重编码会让文件变化，即使每一个像素都相同
（实测：137,585 字节 → 163,285 字节，SHA256 不同）。
`build.ps1` 会对比源文件与 jar 内文件的 SHA256，不一致直接构建失败。

图标的来源：以 TaCZ 自己的 `logo.png` 字标为底，在上面加了 `Optimization` 一行。
TaCZ 字标两行（`TAC` ~112px、`ZERO` ~108px）是**同字号**的，所以加一行在上方读起来是同一套字标。

几条约定的排版规则：

- 新加的 `Optimization` 用 **`#999999`**（`ZERO` 的灰）。用白色会和上面的 `TAC` 叠成一片白。
- 它比 `ZERO` **小一点**，保证原图仍是主体。
- 插在 `TAC ZERO` **上方**，不覆盖原图。

> 教训：曾经拿一张 1440×1440 的版本当母图缩放 —— 但那本身就是 256 的图放大后再加字的，
> 缩回来只会带回放大的模糊和锐化光晕。
> **用原生 256 的导出**（`src\resources\icon.png` 现在就是）。
>
> 也踩过"用相邻像素差当锐度指标"的坑：那个指标**奖励锐化光晕**，会得出相反的结论。
> 不要用它挑素材。

---

## 七、其他已知陷阱

- **PCL 不读 jar 里的图标。** 它解析 `mcmod.info` / `fabric.mod.json` / `META-INF/mods.toml`
  的**文本字段**，图标只来自它的在线数据库缓存。所以本地自编译的模组在 PCL 列表里没有图标，
  这是 PCL 的设计，不是图标文件的问题。游戏内 Forge 的 Mods 列表读 jar 根目录的 `icon.png`，正常显示。
- **绝对不能提供 `ITransformationService`。** Forge 的 `ModsFolderLocator` 会把提供该服务的
  jar **静默踢出模组列表**（没有日志、没有报错）——整个模组凭空消失过一次。
  `build.ps1` 会检查 `META-INF/services/...` 是否存在。
- **同一个 `modId` 只能有一个 jar。** 留下旧文件名的副本会让 Forge 直接启动失败。
  文件名改过两次，所以 `build.ps1` 会自动清理 `mods\` 里文件名含 `optimization`
  且不是本次产物的 jar（匹配规则故意写窄，只认 `optimization`）。
- **`ResourceLocation.tryParse` 在 1.20.1 不存在**，而 `new ResourceLocation(...)` 标了
  `@Deprecated(forRemoval = true)`。只能用后者，并显式 `@SuppressWarnings({"deprecation", "removal"})`
  —— 只写 `"deprecation"` 压不住 `[removal]` 这一类警告。
