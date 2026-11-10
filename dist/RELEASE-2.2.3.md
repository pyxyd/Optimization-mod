# Optimization for Tacz + Feel 2.2.3

修复拖动**上下方向手感不对**：上下拖时枪看起来在"翻滚"，而不是枪口抬高/压低。

## 原因
2.2.2 把偏航和俯仰当成两次独立的轴向旋转，依次乘到 pose 上：
```java
if (pitch != 0) poseStack.mulPose(rotationX(pitch));   // 世界 X 轴
if (yaw   != 0) poseStack.mulPose(rotationY(yaw));     // 世界 Y 轴
```
枪一旦偏航过（比如转到侧面），再绕**世界 X 轴**转就变成"翻滚"——枪口并没有朝着
上/下去，而是绕自己的长轴打了个滚。所以上下拖很难受。

## 改法
合成一个四元数，先在本地 Y 轴偏航，再在**已经偏航后的本地 X 轴**俯仰：
```java
Quaternionf rotation = new Quaternionf()
        .rotationY(radians(yaw))
        .rotateX(radians(pitch));      // 本地轴，不是世界轴
poseStack.m_252781_(rotation);
```
这样无论枪转到什么朝向，上下拖始终是"枪口抬起 / 压低"，和常规 3D 预览一致。

## 调节
- `refitdrag.sensitivity=0.55` —— 每像素多少度
- `refitdrag.pitch_limit=85` —— 俯仰上限
- `refitdrag.invert_x` / `invert_y` —— 方向反过来
按下鼠标时会重载配置，改完不用重开界面。