# Cartographic-Oriented Line Utility Modifier with Bevel through Interpolated Node Automation (*Columbina*) <br> 制图向节点插值自动化倒角路径实用修改器

A Java OpenStreetMap (JOSM) pulgin providing convenient fillet (round corner) drawing, and … <br> might provide bevel and transition curve Features in the future. <br> 一个提供圆角工具的Java OpenStreetMap（JOSM）插件，未来计划开发倒角和缓和曲线功能。

<p align="right">
  <img src="ColumbinaRC.png" alt="Round Corner Menu" width="40%">
</p>
<p align="right">
  <small><sub><s>The name of this plugin has absolutely no relationship with Kuutar or Columbina Hyposelenia, and it's definitely not an attempt to fit the meme. xd <br> 本插件的名称绝对与库塔尔或哥伦比娅·希珀塞莱尼娅无关，绝对不是凑的名字。（逃</s></sub></small>
</p>


## Quick Start · 快速开始

To get quick start, please download the [release](https://github.com/Daredemodaisuki/Columbina/releases/tag/official) and copy it to your JOSM plugin folder<code> %APPDATA%\JOSM\plugins\ </code>(for Windows, usually looks like<code> …\AppData\Roaming\JOSM\plugins\ </code>). <br> 如欲快速开始，劳烦您下载一个[发布版本](https://github.com/Daredemodaisuki/Columbina/releases/tag/official)，并复制到JOSM的插件文件夹<code> %APPDATA%\JOSM\plugins\ </code>（视窗系统，通常是<code> …\AppData\Roaming\JOSM\plugins\ </code>）下。

Then in JOSM's plugin preference, search and select *Columbina* and restart. Now you could use this plugin. <br> 随后在JOSM首选项之插件设置中，搜寻*Columbina*并勾选启用，重启JOSM后即可使用插件。

This plugin depends on another plugin, Utilsplugin2, which is typically included with JOSM by default. If your JOSM does not have Utilsplugin2 installed, JOSM will prompt you to install it. <br> 本插件依赖另一插件Utilsplugin2，其通常由JOSM自带，如果您的JOSM没有安装Utilsplugin2，JOSM会要求你一并安装。

## Features · 功能

All features are located under the More Tools (M) menu. <br> 所有功能均在更多工具〔M〕菜单下。

### Round Corner · 圆角 〔Alt+Ctrl+Shift+C〕

Allows users to fillet each corner node of the selected ways with a specified radius. <br> 允许用户对选定路径的每个拐角节点按指定半径倒圆角。

The plugin supports specifying the arc radius for fillets. Node density can be controlled via central angle increments, and a maximum number of points per curve segment is also provided for control. Additionally, the plugin offers options to copy tags from the original way, remove the original way after drawing, and toggle selection to the new way after drawing. <br> 插件支持指定圆角的圆弧半径，可以通过圆心角步进控制节点密度并提供每段曲线最大点数控制。插件亦提供复制原有路径标签、绘制后移除原有路径、绘制后切换选择新路径的选项。

When "Remove original way after drawing" is enabled: <br> 注意：启用「绘制后移除原有路径」时：

* For old ways that have already been uploaded, the plugin will invoke the "Replace Geometry" function of the Utilsplugin2 to replace the old way in order to preserve its data history. Therefore, if the old way does not meet the requirements for the Replace Geometry function (e.g., it is not entirely within the downloaded area), the replacement will fail and the old way will be retained. <br> 对于已上传的旧路径，插件将调用Utilsplgin2之「替换几何图形」功能替换旧路径以保留数据历史版本，故当旧路径不满足替换几何图形功能的要求时（如未完全在下载区域中），替换将失败，旧路径将保留；对于新绘制、未上传的旧路径，插件将会直接删除。
* For newly drawn, unuploaded old ways, the plugin will delete them directly. <br> 对于新绘制、未上传的旧路径，插件将会直接删除。

## Known Issues · 已知问题
* The plugin packages the replace/delete operations within a single command sequence, and no actual changes occur until the sequence is committed. Consequently, when nodes are shared exclusively between the ways being filleted, the command generation logic assumes these nodes are still referenced by another way since the command sequence has not been commited, and therefore excludes them from removal. <br> 因为希望将替换/删除指令打包在一个命令序列中，而在提交命令序列前不会有任何内容实际删除，故当需要圆角的路径间存在交点且交点只由需要圆角的路径引用，移除旧路径生成命令时始终会认为交点被另一条路径引用，交点不会被移除；
* Since the plugin draws the fillet first and then connects the lines, when the segment between two vertices is not long enough, the middle section between two curves may be misaligned, resulting in a sharp corner. <br> 由于是先画圆角再连线，路径折点宽度不够长时，曲线之间可能会错开导致连出尖角。

## About · 关于
Actually, this plugin was developed for OpenGeofiction (OGF), a fictional world mapping project based on the OpenStreetMap (OSM) framework. It addresses the difficulty in JOSM of drawing long-distance transportation features (such as railways and highways) and small-radius fillets (like the rounded corners at airport taxiway intersections). This plugin is a small contribution to the OGF community. <br> 其实这个插件是为了OpenGeofiction（OGF）开发，这是一个基于OpenStreetMap（OSM）框架的架空地图项目，其中的长距离交通设置（如铁路、高速公路）和一些小拐角（如机场滑行道交叉点的圆角）在JOSM中很难实现，故开发了这个插件，算是对OGF社区的一点微小的贡献。

I hope it can also be useful for everyone in the OSM community. <br> 希望对OSM社区的大家也有用处。

<s>Oops, it's turning into CAD. <br> 坏了，成CAD了</s>
