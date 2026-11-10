# Optimization for Tacz + Feel 2.3.8

修复"侧拉后子弹还是从原位射出"。

## 根因
TaCZ 建子弹用的是 `EntityKineticBullet` 第 172 行：

```java
this(type, throwerIn.getX(), throwerIn.getEyeY() - 0.1F, throwerIn.getZ(), worldIn);
```

它读的是 **getX / getEyeY / getZ**，从来不调 `getEyePosition()`。所以 2.3.7 里偏移眼位对出弹点
毫无作用——子弹仍从身体位置生成。

## 改法
新增 `BulletSpawnMixin`：注入 `EntityKineticBullet` 那个 9 参构造器的 TAIL，
把刚放好的子弹位置按探头方向平移。方向（pitch/yaw）不动，所以弹道仍沿准星，
只有起点跟过去了。

- `CameraMixin`：相机平移（画面）
- `BulletSpawnMixin`：出弹点平移（子弹）

两者用同样的偏移，`EntityEyeMixin` 已删除（TaCZ 不读它）。

## 限制
只对本地玩家（含单机集成服务端里它的副本）生效。联机服务端没装本模组时，
服务端仍按身体位置生成子弹。