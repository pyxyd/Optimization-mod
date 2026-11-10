# Optimization for Tacz + Feel 2.3.4

探头不再只是"改视角"：**眼位本身跟着挪**，子弹和准星一起出去。

## 之前为什么打不出去
2.3.3 只改 `Camera` 的位置。相机位置只影响**渲染**；TaCZ 生成子弹用的是
`shooter.getEyePosition()`（`ModernKineticGunItem`），准星射线也是从眼位发出的，
所以画面挪了、枪口还在原地。

## 改法
新增 `EntityEyeMixin`：注入 `Entity.m_20252_`（`getEyePosition(float)`）的 RETURN，
把返回的眼位按探头方向平移。`getEyePosition()` 内部调的就是这个带 partialTick 的版本，
一次注入两套调用都覆盖。

连带效果：
- `Camera.setup` 本来就读 `entity.getEyePosition(partialTick)` → **相机自动跟随**，
  所以 `CameraMixin` 已删除（避免双重平移）
- 准星射线、`ModernKineticGunItem` 的出弹点一起跟着走
- 单人游戏里集成服务端跑的是同一份类，服务端眼位同样偏移 → **子弹真的从探头位置射出**

## 限制
联机且服务端没装这个模组时，服务端不会偏移眼位，子弹仍从身体位置射出，
只有你本机画面是探出去的。要联机一致就得把这个模组也装到服务端。