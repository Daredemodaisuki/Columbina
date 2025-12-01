# Cartographic-Oriented Line Utility Modifier with Bevel through Interpolated Node Automation (*Columbina*) <br> 制图向节点插值自动化倒角路径实用修改器

A JOSM pulgin providing convinient fillet (round corner) darwing, and … <br> might provide bevel and transition curve fuctions in the future. <br> 一个提供圆角工具的JOSM插件，未来计划开发倒角和缓和曲线功能。

## Quick Start · 快速开始

To get quick start, please download the release and copy it to you JOSM plugin filder<code> %APPDATA%\JOSM\plugins\ </code>(for Windows). <br> 如欲快速开始，劳烦您下载发布版本，并复制到JOSM的插件文件夹<code> %APPDATA%\JOSM\plugins\ </code>（视窗系统）下。

Then in JOSM's plugin preference, search and select *Columbina* and restart. Now you could use this plugin. <br> 随后在JOSM首选项之插件设置中，搜寻*Columbina*并勾选启用，重启JOSM后即可使用插件。

## Functions · 功能

### Round Corners · 圆角 〔Alt+Ctrl+Shift+C〕

Allows users to fillet each corner node of the selected way with a specified radius. <br> 允许用户对选定路径的每个拐角节点按指定半径倒圆角。

The plugin provides options for fillet radius, number of points per arc, copying tags from the original way, removing the original way after drawing, and toggling selection to the new way after drawing. <br> 插件提供倒角半径、每段曲线点数、复制原有路径标签、绘制后移除原有路径、绘制后切换选择新路径选项。

## Known Issues · 已知问题
* When there are intersections between the ways that need to be filleted and these intersections are only referenced by the ways being filleted, they will not be removed when the old ways are deleted; <br> 当需要圆角的路径间存在交点且交点只由需要圆角的路径引用，移除旧路径时交点不会被移除；
* Since the plugin draws the fillet first and then connects the lines, when the segment between two vertices is not long enough, the middle of the curve may be misaligned, resulting in a sharp corner; <br> 由于是先画圆角再连线，路径折点宽度不够长时，曲线中间可能会错开导致连出尖角；
* An arc will still be drawn when the deflection angle at a vertex is close to 180° (not exactly straight, but almost straight, meaning the central angle of the arc is almost 0°), which may lead to very dense nodes. <br> 当拐点张角接近180°时（不是直的、但几乎直的，或者说圆弧圆心角几乎为0°）也会画圆弧，可能会导致节点很密集。
