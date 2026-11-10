# Optimization for Tacz + Feel 2.2.7

上下方向再次修正：2.2.6 换成"旋转模型根节点"后方向反了。

## 原因
`GunItemRendererWrapper` 在画模型前会执行 `poseStack.mulPose(Axis.ZP.rotationDegrees(180f))`
把基岩模型翻正。这个 180° 翻转让**同样的 Z 旋转在世界里方向相反**，所以 2.2.5 里为 pose 空间
定的符号，到 2.2.6 的模型空间就反了。

## 现在
`pitch` 累积用正号；`yaw` 用负号（2.2.2 为"枪口跟手"定的）。

## 自己调（无需重编译）
`config/taczopt.properties`：
```properties
refitdrag.invert_x=false   # 左右反了改 true
refitdrag.invert_y=false   # 上下反了改 true
refitdrag.sensitivity=0.55 # 每像素度数
refitdrag.pitch_limit=85   # 俯仰上限
```
按下鼠标即重载，改完不用重开界面。