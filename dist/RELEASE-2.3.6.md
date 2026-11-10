# Optimization for Tacz + Feel 2.3.6

探头按参考实现重做：**位移实体坐标**，而不是分别挪相机和眼位。

## 参考
GD656Peek（MIT，Forge 1.20.1，38 万下载）的做法是"左右探头时玩家实体向对应方向偏移 0.5 格"。
它没有分两处改相机/眼位，而是直接位移实体——相机、准星、子弹、第三人称模型自然全部跟上。

## 实现
`EntityPosMixin` 注入 `Entity` 的三个方法：
- `m_20185_()`（getX）、`m_20189_()`（getZ）→ 返回时加上侧向偏移
- `m_20182_()`（position()）→ 同样返回偏移后的坐标

`Camera.setup` 用 getX/getY/getZ 算位置、`getEyePosition` 插值 getX/getZ、
TaCZ 的出弹点用 `shooter.getEyePosition()`——它们读的都是这几个方法，所以**一处偏移、全都跟上**。

同时删掉了 2.3.4/2.3.5 的 `CameraMixin` 与 `EntityEyeMixin`，避免三处叠加。
`lean.distance` 默认改为 **0.5**（与参考一致）。

## 与参考的差距
GD656Peek 的动画不受 20 tick 限制（子 tick 插值），这边是每 tick 缓动（`lean.speed`）。
手感上如果想要更顺，可以把插值改到渲染帧（需要额外的 partialTick 处理），说一声我加。