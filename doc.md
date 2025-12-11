# Columbina开发文档

## 总业务流程

1. JOSM在启动新建`Columbina`主类实体，主类构造函数通过具体操作类的`create`函数新建实体、注册菜单；
2. 当用户点击菜单出发点击事件时，走抽象绘图操作类确定下的模板流程（见抽象绘图操作类之抽象模板大致流程），流程内的一些具体细节由中间层和各个功能具体的五大类提供；
3. 流程走完后，功能结束。

目前有以下功能：
* Round Corners（倒圆角）
* Chamfer Corners（倒斜角）
* Transition Curve（过渡曲线）
* Oriented Line（定向画线）

## 重要的类

### 主类（`yakxin.columbina.Columbina`）

根据JOSM插件要求提供的插件入口点（定义在`build.xml`中的必需的清单属性之`Plugin-Class`），通过新建动作类的实体来在JOSM菜单中注册功能项。

<p align="center">
  <del>→ <a href="src/yakxin/columbina/Columbina.java">月之门由此进</a> ←</del>
</p>

### 抽象基类

#### 抽象绘图操作类（`yakxin.columbina.abstractClasses.AbstractDrawingAction`）

整体的绘图操作模板类，最底层的类。

最初本来参考其他插件，各个功能的操作多是分开单独写的Action类，但是对于这个插件，发现分开写有大量重复抄改的代码，出现了整个流程上的变更时需要挨个修改，为了方便，抽象出了功能的总流程。

> * 获取输入（选择的要素和输入参数）
> * 调用具体的生成器计算
> * 获取计算结果和失败结果
> * 产生绘制命令
> * 绘制（提交到撤销重做栈）
> * 产生移除旧输入命令
> * 移除（提交到撤销重做栈）
> 
> 现确定上述流程为最基本的一个绘图功能所需之模板流程。

##### 抽象模板大致流程

模板的大致流程是：当注册的JOSM菜单被触发，执行`actionPerformed`方法（在`AbstractDrawingAction`中）；
1. 调用`checkInputNum`方法（在中间层或具体动作类中实现），检查*当前选中的要素数量是否符合要求*；
2. 调用首选项类的`getParamsAndUpdatePreference`方法，这个方法包括：
   1. 弹出参数设置对话框，对话框类初始化时会调用首选项类的getter获取已储存的上次使用参数；
   2. 等待用户填写参数；
   3. 对话框点击确认并关闭后，校验*数值上的参数是否合法*；
   4. 如果不合法，抛出`IllegalArgumentException`；如果合法，首选项类保存参数并返回参数对象（如果有小问题，直接修正）；
3. 调用`concludeAddCommands`方法（在中间层或具体动作类中实现）生成添加新路径的命令，这个方法包括：
   1. 对于支持批量操作的功能，将整个传入的`ColumbinaInput`拆分为单组输入`ColumbinaSingleInput`；对于不支持的，直接转为`ColumbinaSingleInput`（视作只有一组）；
   2. 对每个单组输入调用传入的具体生成器的`getOutputForSingleInput`方法，获取单组输入的输出，这个方法包括：
      1. 如果必要，具体检查*单组输入内部是否满足生成所需条件*（如节点是否在线上）；
      2. 进行数学计算，并获取对于单组输入的输出节点、单组内的部分失败记录（如需要），打包为`getOutputForSingleInput`；
   3. 汇总各个单组输出，将输出连成路径，并汇总、显示单组内的部分失败的记录（如需要），同时在中间层或具体动作类中记录输入输出对或只记录输出（用于后续选中新路径时返回新路径是什么）；
   4. 构建、返回绘制新路径所需的命令列表和新绘制的要素；
4. 调用`concludeRemoveCommands`方法（在中间层或具体动作类中实现）生成移除输入旧路径的命令，其中：
   * 对于已上传的路径，使用UtilsPlugin2的`ReplaceGeometryCommand`替换旧路径；
   * 对于本地新绘制的、未上传的路径，检查后直接删除；
   * 如果不需要移除旧路径，返回空的命令列表；
5. 根据是否复制标签的参数调用`getNewWayTags`方法（在中间层或具体动作类中实现），为新绘制路径添加标签；
6. 将添加和移除命令分两次提交到`UndoRedoHandler`中并正式执行；
7. 根据是否选中新路径的参数调用`getWhatToSelectAfterDraw`方法（在中间层或具体动作类中实现）并选中新路径。

##### 泛型

抽象绘图操作类因为是插件操作的最底层，有相当多泛型：
* `GeneratorType extends AbstractGenerator<ParamType, InputFeatureType>`：生成器泛型
* `PreferenceType extends AbstractPreference<ParamType>`：首选项泛型
* `ParamType extends AbstractParams`：输入参数泛型

##### 中间抽象层

考虑到面向不同类型的输入要素，获取失败结果的类型、移除旧输入命令的具体操作可能会不同，所以稍微具体点，派生出了两个中间类。

与这些差异有关的内容（如获取失败结果并弹窗、产生移除旧输入命令）需要在子类中实现；同时，按照输入区分子类可以明确输入要素类型泛型，减少理解负担。

* 面向批量路径输入的抽象绘图操作类（`yakxin.columbina.abstractClasses.actionMiddle.ActionWithBatchWays`）

    路径倒圆角、倒斜角和根据路径绘制缓和曲线功能由此派生，这些操作有下面的共同性：
    1. 对于这些功能而言，生成器会对每个拐角进行分别计算、再汇总，故一条路径上有成功的也有失败的拐角，其失败结果可以用`List<Map<Way, List<Node>>>`表示，即：
       * `List<Map>`：失败结果列表
            * `Way`：存在失败情况的输入路径
            * `List<Node>`：处理这条路径时没有成功的节点列表
    2. 因为是细化原有路径，所以输入路径的操作往往需要移除或替换输入的旧路径。

> [!NOTE]
> 
> 正在考虑调整定义为「批量输入模板」
* 面向一条路径 + 一个节点输入的抽象绘图操作类（`yakxin.columbina.abstractClasses.ActionWithNodeWay`）

    导向直线（相对角度模式）、路径切圆功能由此派生，这些操作有下面的共同性：
    1. 对于这些功能而言，因为不是「分别计算的」，其失败就是整个失败，失败结果使用`List<Node>`即可；
    2. 因为输入的节点就是新绘制路径的起点，所以不可以删除输入节点；在相对角度下也无需删除原有线段，产生空命令列表交给抽象类执行西北风即可。
    注意：导向直线功能可以只输入一个节点（绝对角度模式），相当于传入的Way是空的。

> [!NOTE]
>
> 正在考虑调整定义为「非批量输入模板」

> [!NOTE]
> 
> 未来如果还需要更多种类型输入，如果不涉及复用（比如就某个功能使用这种输入，可以考虑直接从`AbstractDrawingAction`继承）
> 
> 如果涉及复用，还可以继续开中间层

#### 抽象生成器类（`yakxin.columbina.abstractClasses.AbstractGenerator`）

生成器主要用于计算结果和统计失败结果（如果必要），主要是`AbstractDrawingAction`需要调用一个统一的`getNewNodeWayForSingleInput`接口，所以每种操作的具体生成器需要实现它，并返回一个`ColumbinaSingleOutput`。

`ColumbinaSingleOutput`包括单组结果的节点、失败输入（如果必要），提供直接将节点拼合成路径的功能。目前够用，不过未来还是可能需要重构，尤其是失败记录部分。

#### 抽象参数类（`yakxin.columbina.abstractClasses.AbstractParams`）

因为发现所有功能的输入参数几乎都包含了「绘制后切换选择新路径」「绘制后移除旧路径」「复制旧路径标签」这3个，同时出于方便生成器辨别的考虑（明确泛函的类型标识符），抽象了参数类，不同的操作具体实现参数。

#### 抽象首选项类（`yakxin.columbina.abstractClasses.AbstractPreference`）

和抽象参数类差不多，主要是起到明确泛型的作用，当然，每个抽象首选项类也需要负责弹出参数设置对话框、获取参数、保存到自身并返回给动作类，有一个统一的抽象函数`getParamsAndUpdatePreference`。

### 工具类

* `yakxin.columbina.utils.UtilsData`：数据处理（包含从序列命令中提取命令列表等）；
* `yakxin.columbina.utils.UtilsMath`：数学计算（包含坐标转换、向量运算、几何计算、级数求和等）；
* `yakxin.columbina.utils.UtilsUI`：用户界面组件（包含添加各种组件、消息弹窗、测试用调试输出窗口等）。

### 数据类

* `yakxin.columbina.data.dto.inputs.ColumbinaInput`：总输入类，打包用户选择的所有要素；
* `yakxin.columbina.data.dto.inputs.ColumbinaSingleInput`：单组输入，用于传递给生成器；
* `yakxin.columbina.data.dto.ColumbinaSingleOutput`：单组输出，包含新节点和部分失败记录；
* `yakxin.columbina.data.ColumbinaException`：自定义异常类，主要起类型标识符作用；
* `yakxin.columbina.data.ColumbinaSeqCommand`：自定义命令序列（主要是改图标和重写描述），用于撤销/重做栈；
* `yakxin.columbina.data.dto.PanelSectionResult`：UI面板之分隔线+小栏目标题打包。

## 异常处理

总流程负责捕获会导致整个流程中止的异常：
* 【关键】最关键的错误、需要立即中止的，如输入了完全不能接受的参数、运行时出错等，由总流程捕获；
* 【部分】批量处理中处理单个输入时出错的，如单条路径整个没有产生结果，由总流程下的for内部捕获continue处理，尽可能不影响其他输入。

对于不支持批量的操作，由于只有一组输入和输出，部分异常即整个的关键异常。

运行时产生的警告、只是起提醒告知作用的（如单条路径内某个点没有圆角），由运行的地方直接弹消息发出，不算异常、不影响全流程。

抛出异常的地方：
* 【关键】动作类中的checkInputNum：这里只检查数量，当输入的选择数量有问题时，抛出IllegalArgumentException；
* 【部分】生成器类正式开始计算前：具体检查输入是否符合某些要求（如节点是否在路径上），如果有问题，抛出ColumbinaException；
* 【部分】生成器类计算时：如果有内部错误，抛出抛出ColumbinaException；

## 增加功能简明流程清单

如果未来需要增加一个新功能，且功能可以适应已有的中间层，可以遵循下面的流程：
1. 创建5个核心类（`xxAction`、`xxDialog`、`xxGenerator`、`xxParams`、`xxPreference`）
2. 根据调用和泛型使用的顺序，从后往前，先完成`xxParams`：
   * 继承`AbstractParams`
   * 添加功能特定的参数字段
3. 完成`xxDialog`：
   * 继承`ExtendedDialog`
   * 创建参数输入界面 
   * 实现获取参数的方法（getter）
4. 完成`xxPreference`：
   * 继承`AbstractPreference<xxParams>`
   * 实现`getParamsAndUpdatePreference`方法（调用`xxDialog`并检查数值）
   * 管理用户首选项的读写
5. 完成`xxGenerator`：
   * 继承`AbstractGenerator<xxParams>`
   * 实现数学上的具体算法（输入应为地面长度，实现数学计算时应在生成器内部转为东北坐标下的长度）
   * 实现`getOutputForSingleInput`方法（调用数学上的算法）
6. 最后完成`xxAction`：
   * 继承`中间层<xxGenerator, xxPreference, xxParams>`
   * 在静态工厂的`create`函数中填入功能信息
   * 实现`getUndoRedoInfo`方法
   * 如果定义了图标，需要在对应位置存入图片
7. 五大类实现后，在`Columbina`主类中注册菜单，并对代码进行调试、测试；
8. 使用`I18n`目录下的脚本提取文本并进行国际化。

如果需要新创建中间层或直接从`AbstractDrawingAction`继承，参考前文中的抽象模板大致流程。