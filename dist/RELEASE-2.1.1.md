# Optimization for Tacz + Feel 2.1.1

修复：2.1.0 的 Feel 后坐力默认值在实际手感上过重（枪身视觉后坐 + 镜头冲击叠加，
枪包后坐数据大的枪还会被放大到 2.2 倍）。

## 默认值变化

| 键 | 2.1.0 | 2.1.1 |
|---|---|---|
| `kick.back` | 0.045 | 0.024 |
| `kick.up` | 0.015 | 0.008 |
| `kick.side` | 0.010 | 0.006 |
| `kick.pitch_deg` | 1.1 | 0.55 |
| `kick.roll_deg` | 0.6 | 0.32 |
| `kick.max` | 0.09 | 0.05 |
| `camera.pitch_deg` | 0.45 | 0.22 |
| `camera.yaw_deg` | 0.12 | 0.06 |
| `camera.roll_deg` | 0.10 | 0.05 |

另外枪械后坐力倍数上限从 **2.2 收到 1.6**：后坐数据大的枪仍然更沉，但不会翻倍。

## 重要：老配置不会自动变

`config/taczfeel.properties` 一旦存在就不会被默认值覆盖。升级到 2.1.1 后想拿到新默认值：

- 手动把上表的值改成 2.1.1 列，**或者**
- 直接删掉 `config/taczfeel.properties`，进游戏重新生成

## 自己调

都在 `config/taczfeel.properties`，保存后 1 秒内热重载：

- 还嫌重：把 `kick.*`、`camera.*` 的幅度继续减半
- 只要镜头冲击、不要枪身晃动：`kick.enabled=false`
- 只要枪身晃动、不要镜头冲击：`camera.enabled=false`
- 全关后坐力：两个都设 false
- 想要"重枪更重、冲锋枪更轻"的区分：保持 `kick.recoil_scale=true`；想要所有枪一致就设 false
- 开镜时想更轻：调小 `camera.ads_scale`（默认 0.7）和 `kick.ads_scale`（默认 1.0）