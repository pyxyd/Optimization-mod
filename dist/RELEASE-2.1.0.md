# Optimization for Tacz + Feel 2.1.0

TaCZ（Timeless and Classics Zero）的优化 + 手感增强，**已合并为一个模组、一个 jar**。

- 模组 id：`taczopt`（原 `taczfeel` 不再是独立模组）
- 适用：Minecraft 1.20.1 / Forge 47.x / TaCZ 1.0.0+
- 服务端：仅客户端功能全部可用；秒杀与命中标记需要服务端也装

## 合入的功能（原 Feel for TaCZ）
- 第一人称枪身后坐（后推 + 抬头 + 随机侧偏，按枪械真实后坐力缩放）
- 镜头短促上跳 + 随机偏航/翻滚（可只对开镜或腰射生效）
- 枪口火光放大（倍率可调）
- 开镜时屏幕边缘暗角（随开镜进度淡入）
- 命中火花、弹道颜色
- 开镜景深（自定义 post shader）
- 枪声随机音高 + 可选分层尾音

## 原有功能
秒杀、混合开镜、开火自动开镜、命中标记、腰射准星（空心十字）、蹲伏零散布、枪械击退、弹孔图片层

## 配置
- `config/taczopt.properties` —— 原有全部开关
- `config/taczfeel.properties` —— 质感增强开关

两个文件保存后 1 秒内热重载，不用重启游戏。

## 安装
把 jar 放进 `mods/` 即可，先决条件是 TaCZ 本体。

弹孔图片：把 PNG 放到 `mods/Bullet-hole-picture/bullet_hole.png`（或目录里任意一张 PNG）；
没有的话用内置默认图。方块上的原始弹孔始终由 TaCZ 绘制，模组只叠加图片层并按
`hole.hole_scale` 缩放。

## 2.1.0 变更
- 合并 Feel for TaCZ（源码版，比已发布的 taczfeel 1.0.0 多出景深与弹道颜色两个功能）
- `mods.toml` 合并为单条 mods 声明，版本号 2.1.0
- MANIFEST 同时声明两个 mixin 配置：`taczopt.mixins.json,taczfeel.mixins.json`
- 构建脚本：classpath 按游戏目录版本固定（原先在多版本共享 libraries 下会挑到 1.16.5 的 jar）
- 构建脚本：mixin 注册检查支持多配置，按各自 `package` 覆盖范围判定

## 注意
Feel 部分是从源码构建的，其中 `DofEffect`、`TracerColor`、`BulletTracerMixin` 是这个源码版本
新增、尚未在游戏里验证过的代码。装上去先打一枪确认没有崩，再发布。