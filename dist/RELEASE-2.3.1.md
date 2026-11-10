# Optimization for Tacz + Feel 2.3.1

探头从"机身侧倾 + 横移"改为"转头"：按 ←/→ 时**视角左右转**，不再侧倾、不再平移。

## 变化
- 新增 `lean.yaw_degrees`（默认 30），`ViewportEvent.ComputeCameraAngles` 里加 yaw
- `lean.roll_degrees` 默认改 0（侧倾关掉），`lean.distance` 默认改 0（横移关掉）
- 想恢复侧倾/横移，把这两个值调回非 0 即可

## 重要限制
`ViewportEvent.ComputeCameraAngles` 只影响**画面**：玩家实际朝向（`yRot`）没变，
而子弹、射线、准星判定都用实际朝向算。所以现在按住 ← 时，画面转了 30°，
但枪口和命中点仍朝原来的方向——观感上就是"头转了、身体和枪没转"。

如果要**瞄准也跟着转**，就不能只改相机，得改玩家朝向（`player.setYRot`），
那样人物模型也会跟着转，而且需要处理与鼠标视角的叠加。