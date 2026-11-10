# Optimization for Tacz + Feel 2.2.0

新增：**改装界面拖动旋转**。

## 用法
1. 手持枪械右键打开改装界面
2. 在**空白处**按住左键拖动 → 枪跟着鼠标转（按钮和配件槽位照常点击）
3. 松开保持当前角度，重新打开界面自动复位

## 配置（config/taczopt.properties）
```properties
refitdrag.enabled=true        # 总开关
refitdrag.sensitivity=0.55    # 灵敏度：每像素转多少度
refitdrag.pitch_limit=85      # 俯仰角上限（度），到顶就停
refitdrag.invert_x=false      # 左右方向反过来
refitdrag.invert_y=false      # 上下方向反过来
refitdrag.reset_on_open=true  # 打开界面时复位
```
保存后 1 秒内热重载；`reset_on_open` 与灵敏度在下次打开界面时生效。

## 实现说明
改装界面里看到的枪是**第一人称 viewmodel**（TaCZ 用 `RefitTransform` 把视角移到正在改的配件上），
所以没有独立模型可转。做法是：

- `RefitScreenDragMixin` — 注入 `Screen.m_6375_/m_7979_/m_6348_`，只在当前屏幕是 `GunRefitScreen`
  时接管；并且**在 `mouseClicked` 返回后**才判断，若控件已经消费了这次点击就不拦，
  因此不需要复制 Screen 的命中测试，按钮/槽位不受影响
- `FirstPersonGunTransformMixin` — 在 `applyFirstPersonGunTransform` 的 TAIL 施加旋转。
  此时 pose 已经在枪械模型自身坐标系里，所以是绕自己转，不是绕镜头公转；且只在改装界面打开时生效，
  正常第一人称渲染完全不受影响
- `GunRefitScreenInitMixin` — 在 `init` 的 TAIL 重载配置并复位角度（翻页、改装后都会重建界面）

## 注意
这个功能**没有在游戏里实测过**：编译、mixin 注入点、注册检查都通过了，但旋转轴心和观感需要你进游戏确认。
如果转起来感觉是"绕镜头转"而不是"原地转"，说明该枪的 viewmodel 原点不在模型中心，需要改成按模型包围盒
中心补偿位移；如果左右方向反了就把 `refitdrag.invert_x` 改 `true`。