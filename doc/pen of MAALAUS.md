# 梅拉（Maalaus）绘制模式计划

受益于123JK同志的RoadRailAlignment项目，Columbina大致有了下一步关于「交互式」绘图的基本思路，于此记录以备后续开发。

## 功能纪要

Maalaus模式下，用户绘制流程暨该功能预期印象应为：
* 选择并进入Maalaus模式（绘制模式而非功能菜单）
* 用户点选起点，此时弹出无边框信息窗口，窗口应显示在鼠标附近，并提示：
  * 当前模式
  * 参数设置（推荐参数、输入框、撤销按钮、调整按钮、继续绘制按钮、完成绘制按钮、调整并完成绘制按钮、窗口透明按钮、取消绘制按钮等）
  * 统计信息
* 开始绘制后默认状态为绘制状态，用户通过右键（或窗口中部分指定组件的键盘快捷键）可切换至暂停状态，此时可与前述信息窗口交互
* 用户按住窗口透明按钮或CapsLock按键，使得信息窗口临时透明（无边框Undecorated模式可透明）；信息窗口跟随鼠标位置移动
* Maalaus包含数个子模式，可通过Tab按键切换：
  * 直线延伸：需要1个控制点，以当前曲段前头端点为起点，控制点为终点产生简单直线曲段
  * 起点曲线延伸：需要1个控制点，以当前曲段前头端点为起点直接绘制圆弧曲段，类似于LargeSweepArc，以当前曲段前头法线方向（左右通过鼠标位置在当前曲段前头端点延伸线左侧还是右侧判断）信息窗口中半径距离（该项不随绘制模式实时计算，为指定参数）位置为圆心，控制点主要确定曲线转多少度
  * 交点曲线延伸：需要2个控制点，第1个控制点位于以当前曲段前头延伸线上（自动吸附），表示曲线切线交点，第2个控制点控制拐弯方向，类似于PI-based
  * 起点曲线延伸与交点曲线延伸的曲线部分均包含可选的缓和曲线，类似于Columbina中的缓-圆-缓结构，由曲段储存
* Maalaus内部维护一个曲段列表，故可以：
  * 用户选择子模式并绘制曲段，左键确定下1（或2）个控制点即按照模式要求立即显示曲段预览
  * 用户按下BackSpace按键快速撤销一个曲段，并刷新预览
  * 用户按下Enter按键快速完成绘制（相当于完成绘制按钮），拼接所有曲段，离散为节点并绘制成JOSM路径
  * 用户按下Esc按键放弃绘制

### 信息窗口

信息窗口使用`JWindow`而非JOSM窗口`ExtendedDialog`（为了实现可临时透明化），窗口上部应提示当前子模式。

中部应为参数设置部分，以直线延伸为例，应包含：
* 长度输入框（示例地，快捷键L）
* 方向角输入框（示例地，快捷键A）
* 以前头切线方向延伸复选框（示例地，快捷键O）

绘制状态下，鼠标移动，控制点跟随鼠标移动，同时随鼠标移动计算当前参数，动态刷新输入框中的数值；

用户通过按下右键可进入暂停状态；此外用户可通过按下L、A按键进入暂停状态并直接聚焦在长度或方向角输入框；用户可通过按下O选中、取消复选框，但不进入暂停状态，以快速切换是沿切线延伸还是自由绘制折线段。

暂停状态下，用户移动鼠标，控制点不再跟随移动，但用户可手动于输入框输入数据，类似于现状Columbina定向直线功能精确调整线段走向，按调整按钮以调整控制点并刷新浏览。

完成绘制按钮、调整并完成绘制按钮之区别在于前者不会为当前最新的控制点创建曲段（即认为最后一段是已经绘制完成的多余线头），而后者将最后一段视作正式的曲段。

## 曲段（CurveSec）与控制点

RoadRailAlignment在算法上与Columbina最大的不同在于前者使用Sampler，其表征一种连续的几何曲线，通过`sampleBySweep`方法离散化为节点；而后者在生成器阶段的曲线算法中尝试直接计算离散化的节点。

RoadRailAlignment之Sampler的好处在于其在离散为节点前均呈现为公式，可随时调整；而直接生成离散节点列表则无法调整，该局限性极大限制了Columbina的交互式绘图开发。

Sampler+离散化操作相当于Columbina中生成器调用算法直接生成离散节点的部分。

不过，现状RoadRailAlignment的Sampler尽管行为模式较为统一，但没有统一的接口，不便于融入Columbina的统一的抽象架构，为了填补该空缺，开发Maalaus模式时，应当设计一种统一的连续几何曲线管理抽象，在此称为「曲段」。

### 曲段

每种子模式应对应一种曲段，但曲段应该由基本的直线或曲线构成，定义基本曲段的抽象`AbstractBasicCurveSec`如下：

具体地，抽象的基本曲段应要求具体类采用段类预留下述属性：
* `startEN`：起点坐标
* `endEN`：终点坐标
* `startAngleRad`：入曲线方向角
* `endAngleRad`：出曲线方向角
* `length`：曲线长度
* `controlPointList`：控制点列表（用于撤销一段时直接读取上一段的控制点）

应要求具体类统一实现下述方法：
* `walk`：输入走行距离获取起点开始沿曲段走行对应距离后的坐标，用于离散化节点（其中缓和曲线部分可参考`UtilsArc.getUnrotatedEulerArc`）
* `sample`：离散化自身，返回节点列表

具体类应根据自身需要实现：
* 构造函数
* 具体地数学计算（可能需要包含移动、旋转、镜像等操作），不一定是具体的公式，但总之服务于`walk`方法

应至少实现：
* 直线基本曲段
* 圆曲线基本曲段
* 缓和曲线基本曲段

曲段由基本曲段组成（如交点曲线延伸可能对应一条直线+一组缓圆缓），定义组合曲段的抽象`AbstractCurveSec`，由`AbstractBasicCurveSec`派生，如下：

具体地，除`AbstractBasicCurveSec`要求外，其应要求具体类采用段类预留下述属性：
* `basicCurveSecList`：基本曲段列表

`AbstractCurveSec`需要重写`walk`和`sample`：输入走行距离后，从第一段基本曲段开始减除长度直到找到该距离具体位于哪段基本曲段上，最后调用基本曲段的`walk`（使用减去其他基本曲段后的距离）；同理`sample`需要收集、拼接基本曲段的离散化后节点列表

此外，在工具类或基类中应实现静态函数：
* `sampleAll`：输入曲段列表，检查各曲段是否相连，如果是，则离散化全部，拼接节点列表

实现各曲段和Maalaus模式后，远期Columbina既有各生成器流程亦可重构为：
* 根据输入计算曲段参数，产生曲段
* 拼接曲段列表并离散化

### 控制点

控制点基本可视作一个ColumbinaEN（或Node）+是否吸附标志。

## 状态管理

Maalaus模式应当包含以下状态：

```
enum MaalausState {
    INIT,       // 初始状态，未开始绘制，等待用户点击起点
    DRAW,       // 绘制状态，鼠标控制点跟随，实时预览
    INFO,       // 暂停状态，鼠标不再跟随，可与信息窗口交互
    DONE,       // 完成状态
    ABORT       // 取消状态
}
```

状态机流转：

```
                    ┌─────────────────────────────────┐
                    │  INIT                           │
                    │  (等待用户点击起点)             │
                    │  无控制点, 无预览               │
                    └──────┬──────────────────────────┘
                           │ 鼠标左键点击起点
                           ▼
                    ┌─────────────────────────────────┐
          Tab ────→ │  DRAW                           │ ←──── Backspace
 (切换子模式)       │  (控制点跟随, 实时预览)         │       (撤销上一段)
                    │  鼠标移动 → 更新预览            │
                    │  鼠标左键 → 添加控制点+确认段   │
                    └──────┬──────────────────────────┘
                           │ 右键 / 按下信息窗口中的指定快捷键
                           ▼
                    ┌──────────────────────────────────┐
                    │  INFO                            │
                    │  (鼠标不再跟, 输入框交互)        │
                    │  [调整按钮] → refreshPreview()   │
                    │                → 回到 DRAW       │
                    │  [调整并完成] → refreshPreview() │
                    │                → 进入 DONE       │
                    │  [继续绘制] → 回到 DRAW          │
                    │  [完成绘制] → 跳过最后一段+提交  │
                    │  [取消绘制] → 进入 ABORT         │
                    └──────┬───────────────────────────┘
                           │ Enter / [完成绘制按钮]
                           ▼
                    ┌───────────────────────────────────────┐
                    │  DONE                                 │
                    │  拼接→离散化→意图→toCommands→UndoRedo │
                    │  → exitMode()                         │
                    └───────────────────────────────────────┘

                              ┌──────────────────────┐
                              │  ABORT               │
                              │  → exitMode()        │
                              └──────────────────────┘
```

### 子模式与所需控制点数

```
enum MaalausSubMode {
    LINE_EXTEND,            // 直线延伸: 需要1个控制点
    ARC_EXTEND,             // 起点曲线延伸: 需要1个控制点
    PI_ARC_EXTEND           // 交点曲线延伸: 需要2个控制点（交点+方向点）
}
```

每个子模式定义其`requiredPointCount`，当前段的`controlPoints.size()`与此值比较决定是否达到确认条件。

Tab切换子模式时，应当清空当前段的未完成控制点，回到`DRAW`；执行Backspace撤销上一段时，重新填充上一段的控制点至当前段，回到`DRAW`状态。

### 状态管理工具

状态管理不应当散落在各个事件处理方法中，应当集中在`MaalausMapMode`中统一维护，参考RoadRailAlignment的`AlignmentController`（PropertyChangeSupport）设计：

```java
public class MaalausController {
    private MaalausState state = MaalausState.INIT;
    private MaalausSubMode subMode = MaalausSubMode.LINE_EXTEND;
    private final List<AbstractCurveSec> secs = new ArrayList<>();     // 已完成曲段列表
    private final List<EastNorth> pendingControlPoints = new ArrayList<>(); // 当前段控制点列表
    private EastNorth startAnchor;                     // 当前段的起点（=上段终点或起点）
    private Vector2D startTangent;                     // 当前段起点切线方向
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    public void addPropertyChangeListener(PropertyChangeListener l) { pcs.addPropertyChangeListener(l); }
    public void setState(MaalausState newState) { ... fire property change ... }
    public void setSubMode(MaalausSubMode newSubMode) { ... }
    // ...
}
```

### 可能需要额外注意之处

* `DRAW`、`INFO`状态下用户可能在JOSM编辑器中会进行撤销，导致运算中的数据在最终提交时与数据库不一致，可能需要在提交解释意图部分进一步考虑这一点
* 用户可能在绘制过程中切换图层、打开新文件或关闭数据集，需要监听相关事件（可在JOSM插件开发指导页寻得相关内容），此时MaalausMapMode应自动退出
* `JWindow`需要手动`dispose`，且需要确保在`exitMode()`中正确关闭

## 架构分层与视图、数据协调控制

作为交互式绘图功能，Maalaus模式不再使用`AbstractDrawingAction`的单次触发模板，而是采用持续的事件驱动架构。

### 架构总览

```
┌─────────────────────────────────────────────────────────────────┐
│  用户交互层 (View)                                              │
│  ┌──────────────────────┐  ┌──────────────────────────────────┐ │
│  │  MaalausMapMode      │  │  InfoWindow (JWindow)            │ │
│  │  (extends MapMode)   │  │  ┌─ 子模式标签                   │ │
│  │                      │  │  ├─ 长度/角度输入框              │ │
│  │  鼠标事件 → onMoved  │  │  ├─ 复选框                       │ │
│  │  鼠标事件 → onClicked│  │  ├─ 按钮 (调整/完成/取消)        │ │
│  │  键盘事件 → onKey    │  │  └─ 统计信息                     │ │
│  └──────────────────────┘  └──────────────────────────────────┘ │
├─────────────────────────────────────────────────────────────────┤
│  控制层 (Controller)                                            │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │  MaalausController                                          ││
│  │  状态: MaalausState, MaalausSubMode, secs列表, 控制点列表   ││
│  │  方法: addControlPoint, undoLastSec, commitAll, ...         ││
│  │  通知: PropertyChangeSupport → MapMode + InfoWindow 监听    ││
│  └─────────────────────────────────────────────────────────────┘│
├─────────────────────────────────────────────────────────────────┤
│  预览层 (Preview)                                               │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │  PreviewPainter (implements MapViewPaintable)               ││
│  │  负责绘制: 控制点标记、曲段预览、切线方向指示、吸附辅助线 ││
│  │  注册: mapView.addTemporaryLayer(previewPainter)            ││
│  └─────────────────────────────────────────────────────────────┘│
├─────────────────────────────────────────────────────────────────┤
│  数据层 (Model)                                                 │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │  AbstractCurveSec + 具体子类                               ││
│  │  连续曲线参数: startEN, endEN, startAngleRad, endAngleRad   ││
│  │  方法: walk(d), sample(interval)                            ││
│  └─────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────┘
```

### 数据流

为`MaalausController`设置`PropertyChangeSupport`，可参考RoadRailAlignment的`AlignmentController`设计：

```
   鼠标移动/点击
        │
        ▼
┌───────────────────┐
│  MaalausMapMode   │ 事件处理方法中调用各controller的方法
│  mouseMoved(e) ──→│──→ controller.addControlPoint(point)
│  mouseClicked(e)  │    controller.undoLastSec()
│  keyPressed(e)    │    controller.commitAll()
└───────────────────┘    等其他函数
        │                          │
        │ PropertyChange           │ PropertyChange
        │ "state"                  │ "controlPoints"
        │ "subMode"                │ "sampledPreview"
        ▼                          ▼
┌───────────────┐         ┌──────────────────────┐
│  InfoWindow   │         │  PreviewPainter      │
│  刷新按钮状态 │         │  setPreview()        │
│  刷新参数显示 │         │  → mapView.repaint() │
│  刷新提示信息 │         └──────────────────────┘
└───────────────┘
```

具体地：
1. 鼠标在MapView上移动 → `MapMode.mouseMoved` → 根据当前状态决定是否更新控制点位置 → 调用`controller.samplePreview(...)` → 通过PropertyChange通知`PreviewPainter.setPreview(controlPoints, sampledPoints)` → `mapView.repaint()`。
2. 鼠标左键点击 → `MapMode.mouseClicked` → 调用`controller.addControlPoint(snappedPoint)` → 如果当前段控制点数已满 → 构造`CurveSec`并添加至`secs`列表 → 清空`pendingControlPoints` → 更新`startAnchor` 和 `startTangent` 为本段终点 → 触发PropertyChange → `PreviewPainter`刷新预览。
3. 鼠标右键点击/按下指定键 → 触发切换到`INFO`状态 → 聚焦到对应输入框 → 鼠标不再影响控制点位置。
4. 信息窗口中的「调整」按钮 → `controller.refreshPreview()` → 根据输入框中的数值重新计算控制点位置 → 回到 `DRAW`状态 → `PreviewPainter`刷新预览。
5. Enter/完成绘制按钮 → `controller.commitAll()`：
   a. `CurveSecUtils.sampleAll(secs, interval)` → 拼接全部曲段的离散点列
   b. 调用JOSM的`MapView.getDataSet()`获得数据集
   c. 为每个采样点构造Node意图：无吸附则创建`AddThisNodeIfOK`意图，有吸附则创建`MergeExistToThisIfOK`意图（对于离散得到的节点，一般是控制点为终点且吸附到既有路径时存在该情况）
   d. 构造Way意图：创建`AddThisWayIfOK`意图
   e. `ColumbinaOutputIntent.toCommands(intents, ds)` → 统一协调冲突并生成Command列表
   f. `SequenceCommand` → `UndoRedoHandler.getInstance().add()`提交
   g. 调用`MapMode.exitMode()`退出绘制模式

## 预览渲染与JOSM API

### 预览层的实现

可参考RoadRailAlignment的实现方式：

1. `PreviewPainter`实现`MapViewPaintable`接口：
   ```java
   public class PreviewPainter implements MapViewPaintable {
       private List<EastNorth> previewPoints = Collections.emptyList();
       private List<EastNorth> controlPoints = Collections.emptyList();

       public void setPreview(List<EastNorth> preview, List<EastNorth> controls) {
           this.previewPoints = Collections.unmodifiableList(new ArrayList<>(preview));
           this.controlPoints = Collections.unmodifiableList(new ArrayList<>(controls));
       }

       @Override
       public void paint(Graphics2D g, MapView mv, Bounds bbox) {
           // 只在有数据时绘制
           if (previewPoints.isEmpty()) return;

           Graphics2D g2 = (Graphics2D) g.create();
           g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

           // 绘制预览线
           g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
           g2.setColor(new Color(0, 120, 255, 180));  // 半透明蓝色
           Point prev = null;
           for (EastNorth en : previewPoints) {
               Point p = mv.getPoint(en);
               if (prev != null) g2.drawLine(prev.x, prev.y, p.x, p.y);
               prev = p;
           }

           // 绘制控制点
           g2.setColor(new Color(255, 80, 80, 200));  // 半透明红色圆圈
           for (EastNorth en : controlPoints) {
               Point p = mv.getPoint(en);
               g2.fillOval(p.x - 5, p.y - 5, 10, 10);
           }

           g2.dispose();
       }
   }
   ```

2. 在`MaalausMapMode.enterMode()`中注册到MapView：
   ```java
   @Override
   public void enterMode() {
       super.enterMode();
       MapView mv = MainApplication.getMap().mapView;
       previewPainter = new PreviewPainter();
       mv.addTemporaryLayer(previewPainter);
       // 注册鼠标/键盘监听器
       mv.addMouseListener(mouseHandler);
       mv.addMouseMotionListener(mouseHandler);
       mv.addKeyListener(keyHandler);
       mv.setFocusable(true);
       mv.requestFocusInWindow();
       // 显示信息窗口
       infoWindow.setVisible(true);
   }

   @Override
   public void exitMode() {
       super.exitMode();
       MapView mv = MainApplication.getMap().mapView;
       mv.removeTemporaryLayer(previewPainter);
       mv.removeMouseListener(mouseHandler);
       mv.removeMouseMotionListener(mouseHandler);
       mv.removeKeyListener(keyHandler);
       infoWindow.setVisible(false);
       controller.reset();
   }
   ```

其中，`MapView.getPoint(EastNorth)`将地理坐标转换为屏幕像素坐标，系预览渲染核心。

### 坐标转换

| 用途                 | JOSM API                                                     |
|----------------------|--------------------------------------------------------------|
| 地理坐标 → 屏幕坐标  | `MapView.getPoint(EastNorth)`                                |
| 鼠标位置 → 地理坐标  | `MapView.getEastNorth(int x, int y)`                         |
| 坐标 → 投影坐标      | `EastNorth`本身即投影坐标                                    |
| 鼠标吸附到已有节点   | `NodeSnapper.findNearestNode(DataSet, EastNorth, tolerance)` |

### 吸附（Snap）与节点重用

可参考RoadRailAlignment：

```java
// 鼠标点击/移动时的吸附逻辑
public EastNorth snapControlPoint(EastNorth rawPoint, MapView mv) {
    DataSet ds = MainApplication.getLayerManager().getEditDataSet();
    double tolerance = controller.getNodeSnapToleranceMeters();
    // 投影坐标系下的容差转像素
    double pixelTolerance = tolerance / mv.getDist100Pixel() * 100;

    // 查找最近的已有节点
    Node nearest = NodeSnapper.findNearestNode(ds, rawPoint, tolerance);
    if (nearest != null) {
        EastNorth snapped = nearest.getEastNorth();
        double dist = rawPoint.distance(snapped);
        if (dist < tolerance) return snapped;
    }
    return rawPoint;  // 无吸附
}

// 适用于交点曲线延伸：吸附到切线延长线
public EastNorth snapToTangentLine(EastNorth raw, EastNorth anchor, Vector2D tangent) {
    // 计算 raw 在 anchor + t·tangent 直线上的投影
    double t = tangent.dot(raw.subtract(anchor));
    return anchor.add(tangent.scale(t));
}
```

注意需要标记吸附的节点，以便提交时使用`MergeExistToThisIfOK`意图。

### 最终提交

离散化完成后，不应直接用`AddCommand`手动组装，而应当遵循Columbina现有架构中的意图系统，通过 `ColumbinaOutputIntent`声明期望的修改，交由`ColumbinaOutputIntent.toCommands()`统一协调冲突并生成指令。

#### 意图提交流程

```
曲段列表 → sampleAll() → 离散点列
    │
    ▼
为每个新节点创建AddThisNodeIfOK意图
为吸附的已有节点创建MergeExistToThisIfOK意图
为新建路径创建AddThisWayIfOK意图
    │
    ▼
List<ColumbinaOutputIntent<?>> intents
    │
    ▼
ColumbinaOutputIntent.toCommands(intents, ds)
    │  (内部处理: 先加节点 → 尝试合并 → 加路径 → 冲突降级)
    ▼
List<Command> → SequenceCommand → UndoRedoHandler
```

#### 具体实现示例

```java
public void commitAll(double interval, String featureTagKey, String featureTagValue) {
    // 1. 拼接全部曲段
    List<EastNorth> allPoints = CurveSecUtils.sampleAll(secs, interval);

    // 2. 获取数据集
    DataSet ds = MainApplication.getLayerManager().getEditDataSet();

    // 3. 构造节点
    List<Node> newNodes = new ArrayList<>();
    List<ColumbinaOutputIntent<?>> intents = new ArrayList<>();
    double snapTolerance = 0.01;

    // 记录每个采样点对应的新节点
    Map<EastNorth, Node> pointNodeMap = new HashMap<>();
    // 记录被吸附的已有节点 → 新占位节点
    Map<Node, Node> snapTargets = new HashMap<>();

    for (EastNorth en : allPoints) {
        Optional<Node> existing = NodeSnapper.findNearestNode(ds, en, snapTolerance);
        Node node;
        if (existing.isPresent() && !snapTargets.containsKey(existing.get())) {
            // 吸附到已有节点：创建新节点作占位，toCommands 会处理合并
            node = new Node(en);
            snapTargets.put(existing.get(), node);
            // 此时先不添加 AddThisNodeIfOK，合并失败时 toCommands 会自动添加
        } else {
            node = new Node(en);
            intents.add(new ColumbinaOutputIntent.AddThisNodeIfOK(node));
        }
        pointNodeMap.put(en, node);
        newNodes.add(node);
    }

    // 4. 构造 Way（必须先于 MergeExist 意图，因为需要 way 引用）
    Way way = new Way();
    way.setNodes(newNodes);
    way.put(featureTagKey, featureTagValue);
    intents.add(new ColumbinaOutputIntent.AddThisWayIfOK(way));

    // 5. 处理节点吸附合并意图
    for (Map.Entry<Node, Node> snap : snapTargets.entrySet()) {
        Node existingNode = snap.getKey();      // 数据集中的已有节点
        Node placeholderNode = snap.getValue(); // 新创建的占位节点
        List<OsmPrimitive> newWayParents = List.of(way);
        // existingNode 当前在数据集中的引用者，若超出此范围则不合并
        List<OsmPrimitive> allowedParents = existingNode.getReferrers();

        intents.add(new ColumbinaOutputIntent.MergeExistToThisIfOK(
            existingNode, placeholderNode, newWayParents, allowedParents
        ));
    }

    // 6. 统一转换为 Command（含冲突检测与降级）
    List<Command> commands = ColumbinaOutputIntent.toCommands(intents, ds);
    if (commands.isEmpty()) return;

    // 7. 提交到撤销重做栈
    SequenceCommand seqCmd = new SequenceCommand(
        I18n.tr("Maalaus: Draw alignment"), commands
    );
    UndoRedoHandler.getInstance().add(seqCmd);

    // 8. 选中新路径
    ds.setSelected(way);
}
```

### 自动吸附到已有节点

在`DRAW`状态下的鼠标移动事件中，应当执行吸附检查，参考RoadRailAlignment的`AlignmentMapMode.handleMouseMoved`：
* 对于起始段：吸附到已有的 OSM 节点（如已有 Way 的端点）
* 对于后续段：吸附到当前段起点切线延长线（交点曲线模式下）
* 吸附后的位置应通过 PropertyChange 反馈到信息窗口的参数输入框中

### 连续绘制（Continuous Mode）

参考RoadRailAlignment的连续模式：
* 当一段完成后，如果启用了连续绘制，自动以上一段的终点切线方向作为下一段的起点方向
* 起点自动吸附到上一段的`endEN`，`startTangent`取上一段的`endAngleRad`

不同的是，Maalaus默认连续绘制，直到用户放弃或完成绘制。

## 文件分包

如前述，Maalaus模式需单独构建相应的状态管理工具，故可在`src`下，新建`modes`包与现状`features`同级，包含：
* `maalaus`包：Maalaus模式相关文件，包含信息窗口、Maalaus控制器等
* Maalaus状态管理工具文件

若未来需要继续构建其他交互式绘制模式，检查Maalaus状态管理工具是否适配，若匹配，则保留其直接在`modes`包下，否则移入`maalaus`包。另检查新模式与Maalaus模式是否有可抽象的内容，若有，可添加`AbstractEditMode`类。

新增的`CurveSec`可视作数据，放置于`data`下。

关于预览绘制等与JOSM地图界面显示有关的内容，结合现有`fillet.advanced.AdvFilletDialog`中的高亮等功能，可在`utils`下新增`UtilsMapView`集中收纳。

## 杂项

* 开发时需基于JDK 11语法以兼容JOSM本体
* 数学计算时，注意复用`UtilsArc`、`UtilsMath`、`ColumbinaEN`、`ColumbinaCorner`等现有代码中的便捷计算方法
* 上述示例代码仅供参考，更多起到伪代码作用，具体情况需具体分析
