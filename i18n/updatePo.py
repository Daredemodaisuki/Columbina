import polib


def merge_translations(a_path, b_path, output_path):
    # 读取 A（完整翻译）
    a_po = polib.pofile(a_path)
    # 建立字典：msgid -> msgstr
    a_dict = {entry.msgid: entry.msgstr for entry in a_po if entry.msgstr}

    # 读取 B（待补全）
    b_po = polib.pofile(b_path)

    for entry in b_po:
        # 如果 B 的 msgid 在 A 中出现且 B 尚未翻译（或 msgstr == msgid）
        if entry.msgid in a_dict:
            if not entry.msgstr or entry.msgstr.strip() == entry.msgid.strip():
                entry.msgstr = a_dict[entry.msgid]

    # 保存结果
    b_po.save(output_path)
    print(f"合并完成，输出已保存至 {output_path}")

    # 强制将 CRLF → LF
    with open(output_path, "rb") as f:
        content = f.read()
    content = content.replace(b"\r\n", b"\n")
    with open(output_path, "wb") as f:
        f.write(content)


merge_translations("./po/zh_CN.po", "template_v1.0.1.po", "./zh_CN.po")
