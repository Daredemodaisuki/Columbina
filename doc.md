# Columbina开发文档

本文档用于记录插件的架构和一些注意事项，从1.0.2版本开始产生抽象层时开始记录。

〔自1.0.3起〕Columbina使用Java11以兼容JOSM最低Java版本。

## 总业务流程

1. JOSM在启动新建`Columbina`主类实体，主类构造函数通过具体操作类的`create`函数新建实体、注册菜单；
2. 当用户点击菜单出发点击事件时，走抽象绘图操作类确定下的模板流程（见抽象绘图操作类之抽象模板大致流程），流程内的一些具体细节由中间层和各个功能具体的五大类提供；
3. 流程走完后，功能结束。

目前有以下功能：
* 〔自1.0.0起〕【R】Round Corners（倒圆角）（半径模式）
  * 〔自1.0.2起〕改进了用于控制倒出的圆角节点密度的参数，并提供推荐半径显示
* 〔自1.0.1起〕【C】Chamfer Corners（倒斜角）
  * 切距模式
  * 角A模式
* 〔自1.0.2起〕【T】Transition Curve（过渡曲线）
* 〔自1.0.2起〕【A】Oriented Line（定向画线）（相对角度模式）

正在 · 计划开发：
* 倒圆角的切距模式
* 定向画线的绝对角度模式
* 〔计划1.0.3〕【W】Curve Connect（曲线连接）
* 【P】Regular Polygon（产生正多边形）
* 【V】Voronoi Line（计算Voronoi中线）
* 【B】Buffer（绘制缓冲区）

## 重要的类

### 主类（`yakxin.columbina.Columbina`）

根据JOSM插件要求提供的插件入口点（定义在`build.xml`中的必需的清单属性之`Plugin-Class`），通过新建动作类的实体来在JOSM菜单中注册功能项。

<p align="center">
  <del>→ <a href="src/yakxin/columbina/Columbina.java">月之门由此进</a> ←</del>
</p>

### 抽象五大基类

#### 抽象绘图操作类（`yakxin.columbina.abstractClasses.AbstractDrawingAction`）

整体的绘图操作模板类，最底层的类。

最初本来参考其他插件，各个功能的操作多是分开单独写的Action类，但是对于这个插件，发现分开写有大量重复抄改的代码，出现了整个流程上的变更时需要挨个修改，为了方便，抽象出了功能的总流程。

> * 获取输入（选择的要素和输入参数）
> * 调用具体的生成器计算
> * 获取计算结果和失败输入记录
> * 产生绘制命令并绘制（提交到撤销重做栈）
> * 产生移除旧输入命令并移除（提交到撤销重做栈）
> * 善后工作
> 
> 现确定上述流程为最基本的一个绘图功能所需之模板流程。

##### 抽象模板大致流程

模板的大致流程是：当注册的JOSM菜单被触发，执行`actionPerformed`方法（在`AbstractDrawingAction`中）：
1. 调用`checkInputNum`方法（在中间层或具体动作类中实现），检查*当前选中的要素数量是否符合要求*；
2. 调用`splitBatchInputs`方法（在中间层或具体动作类中实现），将总输入拆包为单组输入：
   * 对于支持批量操作的功能，将整个传入的`ColumbinaInput`拆分为单组输入构成的列表`List<ColumbinaSingleInput>`；
   * 对于不支持的，视作只有一组；
3. 调用`checkInputDetails`方法（在中间层或具体动作类中实现），具体检查*单组输入内部是否满足生成所需条件*（如节点是否在线上）；
4. 调用首选项类的`getParamsAndUpdatePreference`方法，这个方法包括：
   1. 弹出参数设置对话框，对话框类初始化时会调用首选项类的getter获取已储存的上次使用参数；
   2. 等待用户填写参数；
   3. 对话框点击确认并关闭后，校验*数值上的参数是否合法*；
      * 如果不合法，抛出`IllegalArgumentException`；
      * 如果合法，首选项类保存参数并返回参数对象；
      * 如果有小问题，首选项类修正，随后弹警告、保存参数并返回参数对象；
5. 调用`concludeAddCommands`方法（在中间层或具体动作类中实现）生成添加新路径的命令，这个方法包括：
   1. 对每个单组输入调用传入的具体生成器的`getOutputForSingleInput`方法，获取单组输入的输出，这个方法将进行数学计算，并获取对于单组输入的输出节点、单组内的部分失败记录（如需要），打包为`getOutputForSingleInput`；
   2. 汇总各个单组输出，将输出连成路径，并汇总、显示单组内的部分失败的记录（如需要），同时在中间层或具体动作类中记录输入输出对或只记录输出（用于后续选中新路径时返回新路径是什么）；
   3. 构建、返回绘制新路径所需的命令列表和新绘制的要素；
6. 调用`concludeRemoveCommands`方法（在中间层或具体动作类中实现）生成移除输入旧路径的命令，其中：
   * 对于已上传的路径，使用UtilsPlugin2的`ReplaceGeometryCommand`替换旧路径；
   * 对于本地新绘制的、未上传的路径，检查后直接删除；
   * 如果不需要移除旧路径，返回空的命令列表；
7. 根据是否复制标签的参数调用`getNewWayTags`方法（在中间层或具体动作类中实现），为新绘制路径添加标签；
8. 将添加和移除命令分两次提交到`UndoRedoHandler`中并正式执行；
9. 根据是否选中新路径的参数调用`getWhatToSelectAfterDraw`方法（在中间层或具体动作类中实现）并选中新路径。

##### 泛型

抽象绘图操作类因为是插件操作的最底层，为了具体实现时的类型安全，设置了几个泛型：
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

* 面向一条路径 + 一个节点输入的抽象绘图操作类（`yakxin.columbina.abstractClasses.ActionWithNodeWay`）

    导向直线（相对角度模式）、路径切圆功能由此派生，这些操作有下面的共同性：
    1. 对于这些功能而言，因为不是「分别计算的」，其失败就是整个失败，失败结果使用`List<Node>`即可；
    2. 因为输入的节点就是新绘制路径的起点，所以不可以删除输入节点；在相对角度下也无需删除原有路径，产生空命令列表交给抽象类执行西北风即可。
    注意：导向直线功能可以只输入一个节点（绝对角度模式），相当于传入的Way是空的。

> [!NOTE]
> 
> 正在考虑整合取消中间层，详见文末之「未来重构计划」第1点。

#### 抽象生成器类（`yakxin.columbina.abstractClasses.AbstractGenerator`）

生成器主要用于计算结果和统计失败结果（如果必要），主要是`AbstractDrawingAction`需要调用一个统一的`getNewNodeWayForSingleInput`接口，所以每种操作的具体生成器需要实现它，并返回一个`ColumbinaSingleOutput`。

`ColumbinaSingleOutput`包括单组结果的节点、失败输入（如果必要），提供直接将节点拼合成路径的功能。目前够用，不过未来还是可能需要重构，尤其是失败记录部分。

#### 抽象参数类（`yakxin.columbina.abstractClasses.AbstractParams`）

因为发现所有功能的输入参数几乎都包含了「绘制后切换选择新路径」「绘制后移除旧路径」「复制旧路径标签」这3个，同时出于方便生成器辨别的考虑（明确泛函的类型标识符），抽象了参数类，不同的操作具体实现参数。

#### 抽象首选项类（`yakxin.columbina.abstractClasses.AbstractPreference`）

和抽象参数类差不多，主要是起到明确泛型的作用，当然，每个抽象首选项类也需要负责弹出参数设置对话框、获取参数、保存到自身并返回给动作类，有一个统一的抽象函数`getParamsAndUpdatePreference`。

### 工具类

* `yakxin.columbina.utils.UtilsArc`〔自1.0.3起〕：曲线相关的计算（包括根据圆心和角度画圆弧、画螺旋线等）：考虑到多个功能都要用，就整合在一起了；
* `yakxin.columbina.utils.UtilsData`：数据处理（包含从序列命令中提取命令列表等）；
* `yakxin.columbina.utils.UtilsMath`：数学计算（包含坐标转换、角度归一化、级数求和等）：
  * 〔自1.0.3起〕完全弃用`double[]`的坐标转换、向量运算，使用`ColumbinaEN`提供的方法；
* `yakxin.columbina.utils.UtilsUI`：用户界面组件（包含添加各种组件、消息弹窗、测试用调试输出窗口等）。

### 数据类

* `yakxin.columbina.data.dto.inputs.ColumbinaInput`：总输入类，打包用户选择的所有要素；
* `yakxin.columbina.data.dto.inputs.ColumbinaSingleInput`：单组输入，用于传递给生成器生成结果、和参数窗口计算推荐参数；
  * 〔自1.0.3起〕`public Map<String, Object>  quickPrecomputedData`：快捷传递中间量公共字段：
    * 如果在检查期间就预计算了一些内容（比如路径上的节点索引），可以赋值扔这里方便的给到生成器减少重复计算，生成器需要自己拆包；
    * 也考虑弹窗的推荐参数提前算好，通过这里直接传递到窗口；
* `yakxin.columbina.data.dto.ColumbinaSingleOutput`：单组输出，包含新节点和部分失败记录；
* `yakxin.columbina.data.dto.PanelSectionResult`：UI面板之分隔线+小栏目标题打包；
* `yakxin.columbina.data.ColumbinaCorner`〔自1.0.3起〕：拐角类：
  * 先前每个生成器都是手动存储拐角ABC节点又手动构建BA、BC等向量，这个类把它们统合在了一起，直接访问成员即可知道各种向量、长度、角度；
  * `public static ColumbinaCorner create(Way way, int indexA)`这个方法可以轻松从路径中直接提取对应节点（做了闭合路径的循环索引）并产生拐角，无需手动储存ABC三个点再构建；
* `yakxin.columbina.data.ColumbinaEN`〔自1.0.3起〕：自定义东北坐标类：
  * JOSM墨卡托投影的坐标是`EastNorth`类，但是早期没有注意到里面有加减乘除方法，相关的加减乘除先前每个生成器都需要调用`UtilsMath`中的各种静态`double[]`函数进行向量计算，需要额外存储很多变量，很繁琐；
  * `EastNorth`类本身有加减乘除，但是基于下面的原因，还是自行继承、实现了这个类；
    * `EastNorth`类的角度系统和本插件所用的「东为0，逆时针（左转、北）正角度，顺时针（右转、南）负角度」不一致，比如`rotate`方法的旋转方向和本插件预期相反（需要注意的是，`ColumbinaEN`还没有重写这个方法）；
    * `EastNorth`还差比如取得自己的方向角之类的方法；
  * 下面是一些非常方便的特有方法：
    * `public ColumbinaEN(EastNorth a, EastNorth b)`和`public ColumbinaEN(Node a, Node b)`：从两个`EastNorth`（或`ColumbinaEN`）或`Node`直接构造`ColumbinaEN`，获取从A到B的向量；
    * `public double bearingRad()`：获取向量相对于原点的方向角；
    * `public double deflectionRadTo(ColumbinaEN other)`：获取从`this`（vecA）到`other`（vecB）的偏转角；
    * `public double angleRadBetween(ColumbinaEN other)`：获取`this`（vecA）和`other`（vecB）的夹角；
    * `public int turnLeftRightTo(ColumbinaEN other)`：判断从`this`（vecA）到`other`（vecB）是左拐（逆时针偏）还是右拐（顺时针偏）；
    * `public ColumbinaEN walk(double bearingRad, double enDistance)`：从`this`出发，沿指定角度行进指定距离，得到新的点；
* `yakxin.columbina.data.ColumbinaException`：自定义异常类，主要起类型标识符作用；
* `yakxin.columbina.data.ColumbinaSeqCommand`：自定义命令序列（主要是改图标和重写描述），用于撤销/重做栈；

## 异常处理

总流程负责捕获会导致整个流程中止的异常：
* 【关键】最关键的错误、需要立即中止的，如输入了完全不能接受的参数、运行时出错等，由总流程捕获；
* 【部分】批量处理中处理单个输入时出错的，如单条路径整个没有产生结果，由总流程下的for内部捕获并continue处理，尽可能不影响其他输入。
* 【警告】运行时产生的警告、只是起提醒告知作用的（如单条路径内某个点没有圆角），由运行的地方直接弹消息发出，不算异常、不影响全流程。

对于不支持批量的操作，由于只有一组输入和输出，部分异常即整个的关键异常。

〔自1.0.3起〕抛出异常时，面向用户的的提示需要使用`I18n`；非面向用户的异常则不用，格式为`函数名: 异常信息`。

### 对无效返回值的约定

插件中因计算失败等需要返回无效值，为了方便，在此记下一些比较重要的地方的无效返回值是什么，以方便统一管理和调用者的异常处理：
* 各个生成器类中，如果因为内部问题导致的输入类型不正确、或几何限制无法产生计算结果，向调用`getOutputForSingleInput`的动作类返回`null`；
* 倒角等需要对单组输入内每个子部分（拐角）单独计算的生成器类中，如果因为几何限制无法产生计算子结果，向子结果列表中添加`null`；
* 各个动作类的`concludeAddCommands`、`concludeRemoveCommands`中：如果不需要产生添加或移除指令，或计划内没有产生添加或移除指令的，返回空`ArrayList<Command>`；
* 首选项类的`getParamsAndUpdatePreference`中：如果用户在输入参数窗口取消操作，返回`null`。

### 抛出异常的地方

* 【关键】动作类中的`checkInputNum`：这里只检查数量，当输入的选择数量有问题时，抛出`IllegalArgumentException`；
* 【关键】动作类中的`checkInputDetails`：具体检查输入是否符合某些要求（如节点是否在路径上），如果有问题，抛出`ColumbinaException`；
* 【警告】支持批量输入的动作类的`concludeAddCommands`：如果调用`getOutputForSingleInput`得到了`null`，在for内部警告（考虑改为「部分」级别）；
* 【警告】支持批量输入的动作类的`concludeRemoveCommands`：如果调用Utilsplugin2时它抛出了异常，在for内部警告（考虑改为「部分」级别）；
* 【关键】动作类的`concludeAddCommands`：如果最终发现生成器没有产生任何需要添加的结果，抛出`ColumbinaException`；
* 【关键】`AbstractDrawingAction`的总流程中：如果发现`concludeAddCommands`返回了`null`或空列表，抛出`ColumbinaException`（考虑与上一条合并）；
* 【警告】`AbstractDrawingAction`的总流程中：对于需要`deleteOld`的功能，如果发现`concludeRemoveCommands`返回了null或空列表，抛出`ColumbinaException`，警告没有移除的路径；
* 【部分】生成器类计算时：如果有内部意料之外的错误（如不知道怎么的就除以0了），由相关函数抛出`Exception`，由调用者`concludeAddCommands`在for内捕获。

## 增加功能简明流程清单

如果未来需要增加一个新功能（假如名为`xx`），且功能可以适应已有的中间层，可以遵循下面的流程：
1. 创建5个核心类（`xxAction`、`xxDialog`、`xxGenerator`、`xxParams`、`xxPreference`）
2. 根据调用和泛型使用的顺序，从后往前，先完成`xxParams`：
   * 继承`AbstractParams`
   * 添加功能特定的参数字段
3. 完成`xxPreference`（与4直接有相互调用，应当同步实现）：
   * 继承`AbstractPreference<xxParams>`
   * 实现`getParamsAndUpdatePreference`方法（调用`xxDialog`并检查数值）
   * 管理用户首选项的读写
4. 完成`xxDialog`：
   * 继承`ExtendedDialog`
   * 创建参数输入界面
   * 实现获取参数的方法（getter）
   * 如果需要计算推荐参数，需要提前实现在Action类的检查部分，并把参数提交到`ColumbinaSingleInput`的`quickPrecomputedData`中；创建界面时提取相关推荐参数
5. 完成`xxGenerator`：
   * 继承`AbstractGenerator<xxParams>`
   * 实现数学上的具体算法（输入应为地面长度，实现数学计算时应在生成器内部转为东北坐标下的长度）
   * 实现`getOutputForSingleInput`方法（调用数学上的算法）
   * 如果预估Action类检查期间预计算的东西有用，需要提前实现这个检查部分，并把预计算内容提交到`ColumbinaSingleInput`的`quickPrecomputedData`中；实现`getOutputForSingleInput`时，提取预计算内容
6. 最后完成`xxAction`：
   * 继承`中间层<xxGenerator, xxPreference, xxParams>`
   * 在静态工厂的`create`函数中填入功能信息
   * 实现`getUndoRedoInfo`方法
   * 如果定义了图标，需要在对应位置存入图片
7. 五大类实现后，在`Columbina`主类中注册菜单，并对代码进行调试、测试；
8. 使用`I18n`目录下的脚本提取文本并进行国际化。

如果需要新创建中间层或直接从`AbstractDrawingAction`继承，参考前文中的抽象模板大致流程。

## 未来重构计划

可能目前写得有点复杂了，慢慢改吧~

1. 目前抽象层数貌似有点多了，考虑逐步整合：
   * 第一阶段：两个中间层可以考虑重新定义为「对于批量输入的（现在的`ActionWithBatchWays`）」和「非批量输入的（现在的`ActionWithNodeWay`）」；
   * 第二阶段：随后非批量输入等同于批量输入一组，最终合并到一起并移动至最底层、取消中间层；
   * 现在两个中间层除了前面的输入不同，主要就是二者失败记录的类型不同、处理不一样，但其实可以考虑改作`Map<ColumbinaSingleInput, Object>`，其中：
     * `ColumbinaSingleInput`是失败或者部分失败的输入，需要给到一个`toString`的方法显示输入具体是什么；
     * `Object`是部分失败记录，具体的动作类定义一个根据`Object`自行输出`String`的方法（`Object`也可以是`null`表示这组输入都失败了）；
     * 两个拼在一起就是现状`ActionWithBatchWays`输出部分失败消息的逻辑；
2. 目前Preference弹Dialog，弹完检查后先更改窗口的内容，保存时又从窗口组件读取——比较耦合、需要重构，不过不慌：
   * ~~可能可以考虑再加一个Dialog泛型和`abstractDialog<ParamType>`抽象类，弹窗由action负责，窗口类提供一个getParam，action接收到之后调用首选项的校验函数，校验有问题就在首选项类抛出异常，OK返回CHECK_OK；~~
   * 也可以考虑不增加它，现状Preference弹出窗口，本来也就是弹出会保存到首选项的「参数设置」窗口，逻辑OK，只是需要整理下避免来回读写；
3. Preference弹窗时会向Dialog传入input以便窗口显示推荐参数（如果需要），现状推荐参数由Dialog自行计算，窗口负责了数据计算，职责不太明晰，现在`ColumbinaSingleInput`有了「快捷传递中间量（`quickPrecomputedData`）」后，也许可以在action类具体检查时计算推荐参数并送入这里，窗口直接读取？