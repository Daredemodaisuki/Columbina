# 现状文件负责情况

- [MaalausMapMode](MaalausMapMode.java)〔Controller〕：
  - Maalaus模式入口点，管理模式的进入与退出
  - 持有[MaalausSessionData](MaalausSessionData.java)、[MaalausDrawingService](MaalausDrawingService.java)、[MaalausInfoWindow](MaalausInfoWindow.java)、[Previewer](Previewer.java)
  - 在Maalaus模式时，监听键鼠，发生键鼠事件时检查状态，根据状态：
    - 调用[MaalausDrawingService](MaalausDrawingService.java)的业务方法（如`startDrawing`、添加控制点、提交曲段）
    - 状态机控制，调用[MaalausDrawingService](MaalausDrawingService.java)的状态变更方法（如`pauseDrawing`、`continueDrawing`）
    - 设置[Previewer](Previewer.java)中需要画布绘制预览的数据
  - 实现[MaalausInfoWindow](MaalausInfoWindow.java)的`UserEventListener`接口，作为信息窗口按钮事件和输入框变更事件的统一入口
  - 暂存`lastDisplayData`（最近一次输入的完整参数），供 INFO 模式下点击「添加曲段」时通过`generateAllControlPoints`清空并重算所有控制点
  - 键盘事件通过`KeyEventDispatcher`全局拦截，确保 InfoWindow 组件获得焦点时快捷键（Space/ESC/Enter/Tab）仍然生效
  - 监听[MaalausSessionData](MaalausSessionData.java)的`PropertyChangeSupport`，属性变更事件发生时：
    - 进行操作（如状态变为`DONE``ABORT`时退出模式）
    - 调用[MaalausInfoWindow](MaalausInfoWindow.java)的信息更新方法以刷新外显和子面板
- [MaalausSessionData](MaalausSessionData.java)〔Model〕：
  - 纯数据容器，持有当前绘制状态、子模式、曲段列表、控制点列表、起点及切线等数据
  - 数据写方法为包级私有，仅[MaalausDrawingService](MaalausDrawingService.java)可调用
  - 数据读方法公开，供[MaalausMapMode](MaalausMapMode.java)和[MaalausInfoWindow](MaalausInfoWindow.java)读取
  - 持有`PropertyChangeSupport`，数据变更时触发事件通知监听方
- [MaalausDrawingService](MaalausDrawingService.java)〔Service / 业务逻辑层〕：
  - 封装所有与绘制相关的业务操作（`startDrawing`、`addControlPoint`、`confirmSec`、`clearPendingControlPoints`、`undoLastSec`、`commitAll`、`abort`、`pauseDrawing`、`continueDrawing`）
  - 操作[MaalausSessionData](MaalausSessionData.java)的数据写方法以变更会话状态
  - `confirmSec`通过[MaalausSubMode](MaalausSubMode.java)的`createCurveSec()`工厂方法创建曲段，新增子模式时无需修改此方法
  - 负责 JOSM Command 的构建与提交
- [MaalausInfoWindow](MaalausInfoWindow.java)〔View〕：
  - 信息窗口绘制、各组件事件响应函数
  - 外显信息更新方法（接收字符串或计数，不依赖具体业务类）
  - 持有子模式信息面板占位容器，提供子模式信息面板创建入口方法
  - 子模式信息面板的数据刷新通过[MaalausSubMode](MaalausSubMode.java)的`extractDisplayData()`将[MaalausSessionData](MaalausSessionData.java)转为`SecDisplayData` DTO后传递给面板
  - 按钮事件和输入框变更事件通过`UserEventListener`接口向外通知，不持有任何 Controller 或 Service 引用
  - 不依赖`MaalausSessionData`，对外仅接收`SecDisplayData` DTO
- [Previewer](Previewer.java)〔View〕：
  - 持有需要预览的数据和预览数据的修改方法
  - 画布预览实施，由外部调用（通过`MapView.addTemporaryLayer()`注册）
- [MaalausState](MaalausState.java)：
  - 状态枚举（`INIT`、`DRAW`、`INFO`、`DONE`、`ABORT`）
  - 持有各状态的外显描述文本和操作提示
- [MaalausSubMode](MaalausSubMode.java)〔Strategy / 工厂〕：
  - 子模式枚举（`LINE_EXTEND`、`ARC_EXTEND`、`PI_ARC_EXTEND`）
  - 提供`createSecInfoPanel()`由各子模式常量自行覆写，返回对应的`SecInfoPanel`实例（零参构造，不依赖 Controller）
  - 提供`createCurveSec(ColumbinaEN, ColumbinaEN, List<ColumbinaEN>)`由各子模式常量自行覆写，将起点、切线和控制点列表转化为对应的曲段实例，供[MaalausDrawingService](MaalausDrawingService.java)的`confirmSec`调用；新增子模式时`DrawingService`无需修改
  - 提供`extractDisplayData(ColumbinaEN, List<ColumbinaEN>)`由各子模式常量自行覆写，将起点和待提交控制点列表转换为不可变的`SecDisplayData` DTO
  - `extractDisplayData()`中透过曲段类的静态方法（如`LineExtendCurveSec.calculateDisplayData()`）完成具体计算，保持职责向曲段类集中
  - 提供`generateAllControlPoints(ColumbinaEN, SecDisplayData)`由各子模式常量自行覆写，从完整参数生成全部控制点，统一替代原有的`calculateControlPointFromDisplayData`（已移除）；INFO 状态下编辑输入框时取最后一个点作为预览点，点击「添加曲段」时则清空待提交列表后逐个添加
  - 持有子模式所需的控制点数量和操作提示文本
- [SecInfoPanel](secInfoPanel/SecInfoPanel.java)〔View / 接口〕：
  - 曲段信息面板接口，定义`getPanel()`、`updateValues(SecDisplayData)`、`setEditable(boolean)`、`requestFieldFocus()`
  - 面板不持有任何业务引用，数据通过 DTO 传入
- [LineExtendSecInfoPanel](secInfoPanel/LineExtendSecInfoPanel.java)〔View / 子面板示例〕：
  - 直线延伸子模式信息面板绘制
  - 实现`SecInfoPanel`，通过`LineExtendDisplayData` DTO 获取方位角和长度数据

# 最初的文件负责情况

- [MaalausMapMode](MaalausMapMode.java)〔Controller〕：
  - Maalaus模式入口点，管理模式的进入与退出
  - 持有[MaalausController](MaalausController.java)、[MaalausInfoWindow](MaalausInfoWindow.java)
  - 在Maalaus模式时，监听键鼠，发生键鼠事件时检查状态，根据状态：
    - 调用[MaalausController](MaalausController.java)的数据变更方法（如非`INFO`状态下点击鼠标添加待定控制点、提交曲段）
    - 状态机控制，调用[MaalausController](MaalausController.java)的状态变更方法（如按下Space变更为`INFO`状态）
    - 设置[PreviewPainter](PreviewPainter.java)中需要画布绘制预览的数据
  - 监听[MaalausController](MaalausController.java)的属性监听器，属性变更事件发生时：
    - 进行操作（如状态变为`DONE``ABORT`时退出模式）
    - 在`refreshPreview()`中调用[MaalausInfoWindow](MaalausInfoWindow.java)的信息更新方法以刷新外显和子面板
- [MaalausController](MaalausController.java)〔Model，但包含部分业务逻辑（如提交曲段）〕：
  - 持有、管理当前绘制状态、待确认控制点等数据
  - 数据与控制点操作（如添加控制点、提交曲段、放弃绘制）
  - 注册属性监听器，属性变更时调用以发送事件
- [MaalausInfoWindow](MaalausInfoWindow.java)：
  - 信息窗口绘制、各组件事件响应函数
  - 外显信息更新方法
  - 持有子模式信息面板，提供子模式信息面板创建、更新入口方法（调用创建、面板的更新方法）
- [PreviewPainter](PreviewPainter.java)：
  - 持有需要预览的数据和预览数据的修改方法
  - 画布预览实施，由外部调用
- [MaalausState](MaalausState.java)：
  - 状态枚举
- [MaalausSubMode](MaalausSubMode.java)：
  - 子模式枚举
  - 持有子模式信息面板需要实现的接口`SecInfoPanel`（更新子面板信息的几个方法）
  - 提供子模式信息面板创建方法（预期要求每个子模式均实现）
- [LineExtendSecInfoPanel](secInfoPanel/LineExtendSecInfoPanel.java)（子面板示例）：
  - 直线延伸子模式信息面板绘制
  - 实现`SecInfoPanel`

目前要求[MaalausInfoWindow](MaalausInfoWindow.java)不持有[MaalausController](MaalausController.java)，大体上已经做到，
但子模式信息面板需要当前状态数据（从[MaalausController](MaalausController.java)）读，导致子模式信息面板（如[LineExtendSecInfoPanel](secInfoPanel/LineExtendSecInfoPanel.java)）需要持有之，
进而`MaalausSubMode.createSecInfoPanel()`、`MaalausInfoWindow.rebuildSecInfo()`需要传入[MaalausController](MaalausController.java)，
导致[MaalausInfoWindow](MaalausInfoWindow.java)仍有[MaalausController](MaalausController.java)的耦合，需要解决

一种方法可能是创建dto，但可预期的是每种子模式外显、可供修改的数据不一致，如直线延伸仅为直线长度、方向角，有关曲线的部分可能涉及各种半径、缓和曲线长度
或许一种方法是子面板的刷新直接由[MaalausMapMode](MaalausMapMode.java)调用？
此外或许将[MaalausController](MaalausController.java)改造为纯Model更好（此外需要重命名），将业务逻辑全数移动至[MaalausMapMode](MaalausMapMode.java)或单开业务逻辑类（属于Controller）

此外，目前[MaalausController](MaalausController.java)等持有的仅为较为基础的控制点，对于目前实施的直线延伸，长度、角度等计算还好，但对于后续的各种圆曲线模式，半径等各种参数的计算可能需要规划到底谁计算

鉴于目前[MaalausController](MaalausController.java)的`confirmSec()`函数会创建`<?> extends AbstractBasicCurveSec`，或许可以有对应的曲段提供静态方法计算？未来，解调方法可能还需要提供根据参数算控制点的方法
