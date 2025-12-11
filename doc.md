# Columbina文档

## 总业务流程

1. JOSM调用Columbina主类，通过具体操作类的`create`函数新建实体、注册菜单；
2. 当用户点击菜单出发点击事件时，走抽象绘图操作类确定下的模板流程（见抽象绘图操作类），流程内的一些具体细节由中间层和各个功能具体的操作类提供；
3. 流程走完后，功能结束。

## 重要的类

### 抽象类

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

抽象绘图操作类因为是插件操作的最底层，有相当多泛型：
* `GeneratorType extends AbstractGenerator<ParamType, InputFeatureType>`：生成器泛型
* `PreferenceType extends AbstractPreference<ParamType>`：首选项泛型
* `ParamType extends AbstractParams`：输入参数泛型
* `InputFeatureType extends OsmPrimitive`：输入要素类型泛型（TODO：需要重构，不从OsmPrimitive沿伸，而是按照下面俩子类进行区分）

泛型有点多，而且考虑到面向不同类型的输入要素，获取失败结果的类型、移除旧输入命令的具体操作可能会不同，所以稍微具体点，派生出了两个中间类。

与这些差异有关的内容（如获取失败结果并弹窗、产生移除旧输入命令）需要在子类中实现；同时，按照输入区分子类可以明确输入要素类型泛型，减少理解负担。

##### 面向路径输入的抽象绘图操作类（`yakxin.columbina.abstractClasses.actionMiddle.ActionWithBatchWays`）

路径倒圆角、倒斜角和根据路径绘制缓和曲线功能由此派生，这些操作有下面的共同性：

1. 对于这些功能而言，生成器会对每个拐角进行分别计算、再汇总，故一条路径上有成功的也有失败的拐角，其失败结果可以用`List<Map<Way, List<Node>>>`表示，即：
   * `List<Map>`：失败结果列表
     * `Way`：存在失败情况的输入路径
     * `List<Node>`：处理这条路径时没有成功的节点列表
2. 因为是细化原有路径，所以输入路径的操作往往需要移除或替换输入的旧路径。

##### 面向一条路径 + 一个节点输入的抽象绘图操作类（`yakxin.columbina.abstractClasses.AbstractDrawingActionWayNode`）

导向直线（相对角度模式）、路径切圆功能由此派生，这些操作有下面的共同性：

1. 对于这些功能而言，因为不是「分别计算的」，其失败就是整个失败，失败结果使用`List<Node>`即可；
2. 因为输入的节点就是新绘制路径的起点，所以不可以删除输入节点；在相对角度下也无需删除原有线段，产生空命令列表交给抽象类执行西北风即可。

注意：导向直线功能可以只输入一个节点（绝对角度模式），相当于传入的Way是空的。

> [!NOTE]
> 
> 未来如果还需要更多种类型输入，如果不涉及复用（比如就某个功能使用这种输入，可以考虑直接从`AbstractDrawingAction`继承）
> 
> 如果涉及复用，还可以继续开中间层

#### 抽象生成器类（`yakxin.columbina.abstractClasses.AbstractGenerator`）

生成器主要用于计算结果和统计失败结果，主要是`AbstractDrawingAction`需要调用一个统一的`getNewNodeWayForSingleInput`接口，所以每种操作的具体生成器需要实现它，并返回一个`DrawingNewNodeResult`。

根据前面不同操作的共性差别，`DrawingNewNodeResult`使用一个泛型`FailedResultType`来确定失败结果的类型。

#### 抽象参数类（`yakxin.columbina.abstractClasses.AbstractParams`）

因为发现所有功能的输入参数几乎都包含了「绘制后切换选择新路径」「绘制后移除旧路径」「复制旧路径标签」这3个，同时出于方便生成器辨别的考虑，抽象了参数类，不同的操作具体实现参数。

#### 抽象首选项类（`yakxin.columbina.abstractClasses.AbstractPreference`）

和抽象参数类差不多，主要是起到明确泛型的作用，当然，每个抽象首选项类也需要负责弹出参数设置对话框、获取参数、保存到自身并返回给动作类，有一个统一的抽象函数`getParamsAndUpdatePreference`。

### 工具类

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
