# Optimization for Tacz + Feel 2.2.8

2.2.7 之后的累积更新。

## 新增：辅助瞄准
按 `Y` 打开后，准心会朝视野内最近的生物**缓慢**牵引（每 tick 最多 1.2 度），
松开前后都不会瞬移，也不会替你开枪。只改你自己的视角，不向服务端发包。

- 目标不再只认生物：TaCZ 的标靶车是 `AbstractMinecart` + `ITargetEntity`，原本
  用 `LivingEntity` 搜索永远选不中它，现在两类都认，瞄点取碰撞箱中心。
- 开关会落盘：`config/taczopt.properties` 里的 `assist.on` 会在你按 Y 时立刻写入，
  下次进游戏保持上次的状态（以前只存在内存里，每进一次游戏都得重新按）。
- 默认不隔墙吸（`assist.require_line_of_sight=true`），且只有手持枪时生效。

## 修复：按键注册
`RegisterKeyMappingsEvent` 是 mod 总线事件，之前挂在了 Forge 总线上，导致 Y 和
`O`（开火自动开镜面板）的键位根本没注册。现在单独走 `KeyRegistration`。

## 移除：标靶车倒下
试过整车翻滚、绕靶板铰链等各种做法，观感都不对，直接去掉。
标靶车回到 TaCZ 原版行为（命中只抖一下）。

## 自己调（config/taczopt.properties，改完 1 秒内热重载）
```properties
assist.on=false                  # Y 键开关的持久化值，一般不用手改
assist.range=48                  # 吸的范围（格）
assist.half_angle=40             # 只吸正前方这个角度以内
assist.speed_degrees=1.2         # 每 tick 最多转几度（越小越"慢慢吸"）
assist.require_line_of_sight=true # 隔墙不吸
assist.require_gun=true          # 手持枪才吸
assist.include_players=false     # 玩家不吸
```