import os


# 获取相对路径
def find_java_files_relative(folder_path):
    java_files = []

    # 确保folder_path是绝对路径
    folder_path = os.path.abspath(folder_path)

    for root, dirs, files in os.walk(folder_path):
        for file in files:
            if file.endswith('.java'):
                full_path = os.path.join(root, file)
                # 计算相对于指定文件夹的路径
                relative_path = os.path.relpath(full_path, folder_path)
                java_files.append(relative_path)

    return java_files


def tset(folder_path):
    java_files_relative = find_java_files_relative(folder_path)
    print(f"找到 {len(java_files_relative)} 个Java文件:")
    for file in java_files_relative:
        print(f"  - {file}")

    return java_files_relative


tset("../")
