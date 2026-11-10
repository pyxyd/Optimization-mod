# Optimization for Tacz + Feel 2.3.3

探头方向翻正，并让配置支持运行时热重载。

## 改动
- `CameraMixin`：横移量取负（左探头往左挪）。原来用了"右向量"的正方向，按 ← 反而往右移
- `LeanHandler`：侧倾同样取负，和横移方向一致
- `LeanHandler` 每 tick 调 `LeanConfig.reloadIfChanged()`，改 `config/taczopt.properties` 后
  **不用重启游戏**，也不再需要重进世界

## 如果还想反
```properties
lean.invert=true   # 横移和侧倾一起反过来
```
保存即刻生效。