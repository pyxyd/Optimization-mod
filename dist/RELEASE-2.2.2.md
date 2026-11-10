# Optimization for Tacz + Feel 2.2.2

修复 2.2.1 的拖动方向反了：**鼠标往左拖，枪口却往右走**。

## 原因
```java
yaw += deltaX * sensitivity;          // 往左拖 = deltaX 为负 = yaw 减小
poseStack.mulPose(new Quaternionf().rotationY(radians(yaw)));
```
`rotationY` 的正方向是**从上往下看的逆时针**。相机朝 -Z 看，+X 在屏幕右侧，
所以 yaw 减小时枪口转向 +X，看起来就是"手往左、枪口往右"。符号写反了。

## 改法
```java
yaw -= deltaX * sensitivity;          // 往左拖 = yaw 增大 = 枪口转向 -X（屏幕左侧）
```
`refitdrag.invert_x` 仍然可用，留给想反过来的人。

## 另外两处
- 按下鼠标时重载配置（原来是打开界面才重载），所以改 `config/taczopt.properties` 后
  **不用重开界面**，下一次按下去就生效
- 加了自检日志：
  - `[taczopt] refit screen opened, drag=...` —— 界面打开、复位
  - `[taczopt] refit drag active: sensitivity=... pitch_limit=...` —— 第一次按下
  - `[taczopt] refit drag released: yaw=... pitch=...` —— 松开时报告累计角度
  - `[taczopt] refit render hook reached` / `refit rotation applied: ...` —— 渲染侧确认

## 已知
本功能仍未逐帧实测观感（我这边只能构建）。若上下方向也反了，把 `refitdrag.invert_y` 设 `true`。