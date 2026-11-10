# Optimization for Tacz + Feel 2.2.6

修复"越转越近/越远"。

## 原因
2.2.5 之前，旋转是乘到 **poseStack** 上的。在 `applyFirstPersonGunTransform` 末尾，pose 处于
**相机坐标系**（相机在原点，朝 -Z 看），而枪模型在相机前方一段距离。绕原点旋转 = 让枪
**绕相机公转**，于是角度一转，枪和相机的距离就跟着变——看起来就是越转越近。

## 改法
改成旋转**模型自身**，而不是 pose：
```java
BedrockPart root = model.getRootNode();
root.additionalQuaternion.mul(new Quaternionf()
        .rotationY(radians(yaw))      // 偏航
        .rotateZ(radians(pitch)));    // 俯仰（枪管在 X 轴，绕 X 就是滚转）
```
TaCZ 自己处理开火/移动抖动时就是这么做的（`additionalQuaternion`），绕的是模型原点，
**不产生任何位移**，所以距离恒定。副作用是枪口位置缓存（曳光弹起点）也会跟着转，更正确。

## 注意
旋转中心现在是**模型根节点的原点**，未必正好是枪身中线。如果转大角度时枪还是会甩出去一点，
说明该枪的根节点不在重心，需要按包围盒补位移补偿 —— 反馈一下具体是哪把枪。