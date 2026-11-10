# Optimization for Tacz + Feel 2.3.2

探头按"三角洲式战术侧身"重做：**镜头向侧方平移 + 轻微侧倾**，不转视角、不转朝向。

## 与 2.3.1 的区别
- 去掉 `ViewportEvent.ComputeCameraAngles` 里的 yaw（那是"视角左右转"，不是探头）
- 恢复 `CameraMixin` 的横向平移：`distance=0.45` 格，沿相机右向量
- 侧倾只给 3°（原来 7° 太大，看起来像整个世界歪了）

## 配置
```properties
lean.distance=0.45      # 横移多少格（0 = 只侧倾不横移）
lean.roll_degrees=3.0   # 侧倾角度（0 = 只横移不侧倾）
lean.speed=0.28         # 推进/回正速度
lean.invert=false
lean.yaw_degrees=0.0    # 已停用，保留键位避免旧配置报错
```

## 限制
横移只作用于**渲染相机**。玩家的 `getEyePosition()`/朝向没变，所以：
- 画面确实往侧方挪了，能"探出去看"
- 但子弹起点、准星命中点仍在身体原位置，偏移量是 `distance` 格
三角洲里探头射击是服务器认可的姿态，MC 这边要做到那一步得改实体眼位并同步服务端。