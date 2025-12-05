import re
from collections import defaultdict
from pathlib import Path
from scanFiles import find_java_files_relative

# 在GitBash中
# cd到 /g/JvProject/Columbina/i18n
# 执行 perl ./i18n.pl ./po/zh_CN.po

# 配置
input_files = [f"../{file}" for file in find_java_files_relative("../")]

VERSION = "v1.0.1"
OUTPUT_FILE = f"template_{VERSION}.po"

# This file is distributed under the same license as the josm package.
HEAD = '''# I18n for Columbina plugin
# Copyright (c) Yakxin 2025
#
msgid ""
msgstr ""
"Project-Id-Version: Columbina Plugin\\n"
"Report-Msgid-Bugs-To: \\n"
"POT-Creation-Date: 2025-12-02 11:45+0800\\n"
"PO-Revision-Date: 2025-12-02 19:19+0800\\n"
"Last-Translator: \\n"
"Language-Team: \\n"
"MIME-Version: 1.0\\n"
"Content-Type: text/plain; charset=UTF-8\\n"
"Content-Transfer-Encoding: 8bit\\n"
"Plural-Forms: nplurals=1; plural=0;\\n"
"X-Generator: Manual\\n"
"Language: en\\n"


'''


# 跨行匹配 I18n.tr(...)
CALL_PATTERN = re.compile(r'I18n\.tr\s*\(', re.MULTILINE)

# 匹配字符串常量 "xxxx"
STRING_PATTERN = re.compile(r'"([^"\\]*(?:\\.[^"\\]*)*)"')


def extract_calls_from_text(text):
    """找出所有 I18n.tr(...) 调用，返回 (pos, call_text) 列表"""
    results = []
    for m in CALL_PATTERN.finditer(text):
        start = m.start()

        # 从 "(" 后匹配括号直到配对结束
        depth = 1
        i = m.end()
        while i < len(text) and depth > 0:
            if text[i] == '(':
                depth += 1
            elif text[i] == ')':
                depth -= 1
            i += 1

        if depth == 0:
            call_text = text[m.start():i]
            results.append((m.start(), call_text))

    return results


def extract_msgid(call_text):
    """提取 I18n.tr(...) 的第一个字符串参数"""
    strings = STRING_PATTERN.findall(call_text)
    if not strings:
        return None
    return strings[0]


def process_file(path: Path):
    """处理一个 Java 文件，返回 (行号, msgid) 列表"""
    text = path.read_text(encoding="utf-8", errors="ignore")
    calls = extract_calls_from_text(text)

    results = []
    for pos, call in calls:
        msgid = extract_msgid(call)
        if msgid:
            line_number = text.count("\n", 0, pos) + 1
            results.append((line_number, msgid))
    return results


# ======== 主逻辑（带去重与合并） ========

# { msgid: [ "文件:行号", ... ] }
msg_map = defaultdict(list)

for file_path in input_files:
    path = Path(file_path)
    if not path.exists():
        print(f"[WARN] 文件不存在：{file_path}")
        continue

    for line_no, msgid in process_file(path):
        msg_map[msgid].append(f"{file_path}:{line_no}")

# 生成输出内容
output_lines = []

for msgid, refs in msg_map.items():
    # 输出所有 #: 文件名:行号
    for ref in refs:
        output_lines.append(f"#: {ref}")

    output_lines.append(f'msgid "{msgid}"')
    output_lines.append(f'msgstr "{msgid}"')
    output_lines.append("")  # 空行


# 写入文件
Path(OUTPUT_FILE).write_text(HEAD + "\n".join(output_lines), encoding="utf-8", newline="\n")

print(f"完成，共输出 {len(msg_map)} 条唯一 msgid → {OUTPUT_FILE}")
