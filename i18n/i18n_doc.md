# I18n简明步骤指北

* 先运行`getPo.py`，注意修改版本号，其在`i18n`文件夹下生成`template_{VERSION}.po`，该文件包含java代码所有`I18n.tr()`中的硬编码英文文本，作为待翻译英文汇总
* 运行`updatePo.py`合并既往已翻译内容到新待翻译内容，指定：
  * `existing_po`为`./po/zh_CN.po`作为既有已翻译文本
  * `add_existing_into`为`./template_{VERSION}.po`作为完全待翻译文本（注意修改版本号）
  * `output_po`为`./zh_CN.po`作为合并后输出文件
* 打开上一步输出的`./zh_CN.po`，翻译剩余待翻译文本
* 复制替换`.po`文件：
  * 复制`./template_{VERSION}.po`到`./po/`替换其中的`en.po`
  * 复制补充翻译好后的`./zh_CN.po`到`./po/`替换其中的`zh_CN.po`
* 打开`perl`环境（如Git Bash），导航到`i18n`文件夹，运行：
  * `perl ./i18n.pl ./po/en.po`，得到`./en.lang`
  * `perl ./i18n.pl ./po/zh_CN.po`，得到`./zh_CN.lang`
* 使用新的`.lang`文件替换`./lang/`和`data`文件夹中的旧`.lang`文件

〔自v1.0.4〕起，`getPo.py`添加了`decode_java_string`和`escape_po`函数，后续更新若有问题，尝试还原至`8b14a99a32fa005220e9db3352c4f581c3e06a30`的`getPo.py`，并手动检查Unicode转义字符`\uXXXX`是否存在重复转义问题
TODO：斜体ADVANCED支持