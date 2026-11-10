# Optimization for Tacz + Feel 2.3.0

新增：**左右探头**，默认 **← / →**（可在「按键设置 → Optimization for Tacz」里改）。

## 效果
按住 ← 或 → 时视角向该侧平移并轻微侧倾，松开回正。平移量、侧倾角、回正速度都可调。

## 实现
- `LeanHandler` —— 注册两个按键（`RegisterKeyMappingsEvent`，MOD 总线），每 tick 读取按键状态；
  侧倾走 Forge 的 `ViewportEvent.ComputeCameraAngles`（`event.setRoll(...)`）
- `CameraMixin` —— 注入 `Camera.m_90575_`（setup）的 TAIL，此时相机位置已算完，
  按相机右向量平移：右向量 = `(cos yaw, 0, sin yaw)`，再 `m_90581_`（setPosition）
- `LeanState` —— −1..+1 的缓动值，避免瞬移

## 配置（config/taczopt.properties）
```properties
lean.enabled=true
lean.distance=0.42      # 平移距离（格）
lean.roll_degrees=7.0   # 侧倾角度
lean.speed=0.28         # 回正/推进速度
lean.invert=false       # 左右反过来
```

## 注意
探头是**纯客户端**的：身体和射线起点都不变，服务器不知道你在探头。
所以它能用来观察/瞄准，但不会让你的命中判定或掩体判定发生变化。