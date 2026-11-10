# Optimization for Tacz + Feel 2.2.4

修复拖动**上下方向绕枪管自转**（roll）而不是枪口抬起/压低。

## 原因
在 `applyFirstPersonGunTransform` 的末尾，枪管朝向是 **X 轴**。2.2.3 用
`rotateX(pitch)` 想做俯仰，实际做的是**绕枪管翻滚**——画面里就是枪沿自己那根线打转，
所以特别别扭。正确的俯仰轴是与枪管垂直的那根轴，在这个 pose 里是 **Z**。

## 改法
```java
Quaternionf rotation = new Quaternionf()
        .rotationY(radians(yaw))      // 偏航：世界竖直轴
        .rotateZ(radians(pitch));     // 俯仰：垂直于枪管和竖直方向的轴
poseStack.m_252781_(rotation);
```

## 还剩一个点
现在的旋转中心是 **pose 原点**（枪械模型的原点），不一定正好在你画的那个黄点上。
如果 2.2.4 的上下拖是"整把枪在绕原点摆"而不是"枪在原地抬头"，告诉我，
我再按模型包围盒/根节点补一个位移补偿，让它绕枪身中线转。