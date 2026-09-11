#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
实训成果 ZIP 解压与文件树输出脚本

功能：
  - 将指定 ZIP 文件解压到临时目录
  - 输出前 3 层目录结构
  - 统计文件类型分布
  - 支持中文文件名（自动处理编码问题）

用法：
  python extract_zip.py <zip文件路径> [--output <输出目录>] [--max-depth <层数>]

示例：
  python extract_zip.py submission.zip
  python extract_zip.py project.zip --output ./extracted --max-depth 4
"""

import argparse
import os
import shutil
import sys
import tempfile
import zipfile
from collections import Counter, defaultdict
from pathlib import Path


def safe_extract(zip_path: str, extract_dir: str) -> tuple[bool, str]:
    """
    安全解压 ZIP 文件，处理中文编码问题。

    返回 (success, error_message)
    """
    try:
        with zipfile.ZipFile(zip_path, 'r') as zf:
            # 尝试不同编码处理中文文件名
            for info in zf.infolist():
                # 尝试 cp437 -> gbk 编码转换（常见的中文 ZIP 问题）
                try:
                    filename = info.filename.encode('cp437').decode('gbk')
                except (UnicodeEncodeError, UnicodeDecodeError):
                    try:
                        filename = info.filename.encode('cp437').decode('utf-8')
                    except (UnicodeEncodeError, UnicodeDecodeError):
                        filename = info.filename

                target_path = os.path.join(extract_dir, filename)

                # 防止路径穿越攻击 (Zip Slip)
                abs_target = os.path.abspath(target_path)
                abs_extract = os.path.abspath(extract_dir)
                if not abs_target.startswith(abs_extract):
                    print(f"[警告] 跳过可疑路径: {filename}")
                    continue

                if info.is_dir():
                    os.makedirs(target_path, exist_ok=True)
                else:
                    os.makedirs(os.path.dirname(target_path), exist_ok=True)
                    with zf.open(info) as src, open(target_path, 'wb') as dst:
                        shutil.copyfileobj(src, dst)

        return True, ""
    except zipfile.BadZipFile:
        return False, "文件不是有效的 ZIP 格式"
    except Exception as e:
        return False, f"解压失败: {str(e)}"


def get_file_category(ext: str) -> str:
    """根据扩展名返回文件类别"""
    ext = ext.lower()
    doc_exts = {'.doc', '.docx', '.pdf', '.txt', '.md', '.rst', '.tex', '.odt'}
    code_exts = {'.py', '.java', '.js', '.ts', '.jsx', '.tsx', '.html', '.htm',
                 '.css', '.scss', '.less', '.sql', '.cpp', '.c', '.h', '.hpp',
                 '.go', '.rs', '.rb', '.php', '.swift', '.kt', '.scala', '.r',
                 '.m', '.pl', '.sh', '.bash', '.ps1', '.bat', '.xml', '.json',
                 '.yaml', '.yml', '.toml', '.ini', '.cfg', '.conf'}
    image_exts = {'.png', '.jpg', '.jpeg', '.gif', '.bmp', '.svg', '.webp', '.ico'}
    archive_exts = {'.zip', '.rar', '.7z', '.tar', '.gz', '.bz2', '.xz'}
    config_exts = {'.gitignore', '.dockerignore', '.env', 'dockerfile', 'makefile',
                   '.editorconfig', '.eslintrc', '.prettierrc'}

    if ext in doc_exts:
        return '📄 文档'
    elif ext in code_exts:
        return '💻 代码'
    elif ext in image_exts:
        return '🖼️  图片'
    elif ext in archive_exts:
        return '📦 压缩包'
    elif ext in config_exts or ext == '':
        return '⚙️  配置'
    else:
        return '📁 其他'


def build_file_tree(root_dir: str, max_depth: int = 3) -> str:
    """
    构建文件树字符串，限制递归深度。

    返回格式化的文件树字符串。
    """
    lines = []
    root_path = Path(root_dir)

    def _tree(dir_path: Path, prefix: str = "", depth: int = 0):
        if depth > max_depth:
            # 统计深层文件数
            file_count = sum(1 for _ in dir_path.rglob('*') if _.is_file())
            dir_count = sum(1 for _ in dir_path.rglob('*') if _.is_dir())
            if file_count > 0 or dir_count > 0:
                lines.append(f"{prefix}└── ... ({file_count} 个文件, {dir_count} 个子目录，已省略)")
            return

        entries = sorted(dir_path.iterdir(), key=lambda e: (e.is_file(), e.name.lower()))

        for i, entry in enumerate(entries):
            is_last = (i == len(entries) - 1)
            connector = "└── " if is_last else "├── "
            next_prefix = "    " if is_last else "│   "

            if entry.is_dir():
                lines.append(f"{prefix}{connector}📁 {entry.name}/")
                _tree(entry, prefix + next_prefix, depth + 1)
            else:
                ext = entry.suffix.lower()
                category_icon = get_file_category(ext).split()[0]
                size = entry.stat().st_size
                size_str = format_size(size)
                lines.append(f"{prefix}{connector}{category_icon} {entry.name} ({size_str})")

    _tree(root_path)
    return "\n".join(lines)


def format_size(size_bytes: int) -> str:
    """格式化文件大小"""
    if size_bytes < 1024:
        return f"{size_bytes}B"
    elif size_bytes < 1024 * 1024:
        return f"{size_bytes / 1024:.1f}KB"
    else:
        return f"{size_bytes / (1024 * 1024):.1f}MB"


def count_file_types(root_dir: str) -> dict:
    """统计文件类型分布"""
    categories = Counter()
    ext_counter = Counter()
    total_files = 0
    total_size = 0

    for file_path in Path(root_dir).rglob('*'):
        if file_path.is_file():
            total_files += 1
            total_size += file_path.stat().st_size
            ext = file_path.suffix.lower()
            ext_counter[ext] += 1
            categories[get_file_category(ext)] += 1

    return {
        'total_files': total_files,
        'total_size': total_size,
        'categories': dict(categories),
        'extensions': dict(ext_counter.most_common(10))
    }


def detect_project_type(root_dir: str) -> list[str]:
    """检测项目类型"""
    root = Path(root_dir)
    indicators = []

    # 在根目录和一级子目录中查找特征文件
    search_dirs = [root] + [d for d in root.iterdir() if d.is_dir()]

    for d in search_dirs[:5]:  # 最多查 5 个目录
        files_in_dir = {f.name.lower() for f in d.iterdir() if f.is_file()}
        dirs_in_dir = {f.name.lower() for f in d.iterdir() if f.is_dir()}

        checks = [
            ('package.json', 'Node.js / 前端项目'),
            ('pom.xml', 'Java Maven 项目'),
            ('build.gradle', 'Java Gradle 项目'),
            ('build.gradle.kts', 'Java Gradle (Kotlin DSL) 项目'),
            ('requirements.txt', 'Python 项目'),
            ('setup.py', 'Python 包项目'),
            ('pyproject.toml', 'Python 现代项目'),
            ('go.mod', 'Go 项目'),
            ('cargo.toml', 'Rust 项目'),
            ('composer.json', 'PHP 项目'),
            ('gemfile', 'Ruby 项目'),
            ('pubspec.yaml', 'Flutter/Dart 项目'),
            ('dockerfile', '容器化项目'),
            ('docker-compose.yml', 'Docker Compose 项目'),
            ('vue.config.js', 'Vue.js 项目'),
            ('vite.config.js', 'Vite 前端项目'),
            ('vite.config.ts', 'Vite (TypeScript) 前端项目'),
            ('angular.json', 'Angular 项目'),
            ('next.config.js', 'Next.js 项目'),
            ('next.config.ts', 'Next.js (TypeScript) 项目'),
            ('nuxt.config.js', 'Nuxt.js 项目'),
            ('svelte.config.js', 'Svelte 项目'),
            ('tsconfig.json', 'TypeScript 项目'),
            ('webpack.config.js', 'Webpack 项目'),
        ]

        for filename, project_type in checks:
            if filename in files_in_dir:
                indicators.append(project_type)

        if 'node_modules' in dirs_in_dir:
            if not any('Node.js' in i for i in indicators):
                indicators.append('Node.js 项目（含 node_modules）')
        if 'src' in dirs_in_dir and 'pom.xml' in files_in_dir:
            if 'Java Maven 项目' not in indicators:
                indicators.append('Java Maven 项目')

    # 去重并保持顺序
    seen = set()
    unique_indicators = []
    for item in indicators:
        if item not in seen:
            seen.add(item)
            unique_indicators.append(item)

    return unique_indicators if unique_indicators else ['无法自动识别项目类型']


def main():
    parser = argparse.ArgumentParser(
        description='实训成果 ZIP 解压与文件树输出脚本',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
示例:
  python extract_zip.py submission.zip
  python extract_zip.py project.zip --output ./extracted --max-depth 4
        """
    )
    parser.add_argument('zip_path', help='ZIP 文件路径')
    parser.add_argument('--output', '-o', default=None,
                        help='解压输出目录（默认：系统临时目录）')
    parser.add_argument('--max-depth', '-d', type=int, default=3,
                        help='文件树最大递归深度（默认：3）')

    args = parser.parse_args()

    zip_path = os.path.abspath(args.zip_path)

    if not os.path.exists(zip_path):
        print(f"[错误] 文件不存在: {zip_path}", file=sys.stderr)
        sys.exit(1)

    # 确定输出目录
    if args.output:
        extract_dir = os.path.abspath(args.output)
        os.makedirs(extract_dir, exist_ok=True)
        is_temp = False
    else:
        extract_dir = tempfile.mkdtemp(prefix='training_extract_')
        is_temp = True

    # 解压
    print(f"📦 正在解压: {os.path.basename(zip_path)}")
    success, error = safe_extract(zip_path, extract_dir)

    if not success:
        print(f"[错误] {error}", file=sys.stderr)
        sys.exit(1)

    print(f"📁 解压至: {extract_dir}")
    print()

    # 输出文件树
    print("=" * 60)
    print(f"📂 文件目录结构（前 {args.max_depth} 层）")
    print("=" * 60)
    print()
    # 获取解压后的顶层目录
    top_items = list(Path(extract_dir).iterdir())
    if len(top_items) == 1 and top_items[0].is_dir():
        # 只有一个顶层目录，直接用它作为根
        tree_root = str(top_items[0])
    else:
        tree_root = extract_dir

    tree = build_file_tree(tree_root, max_depth=args.max_depth)
    print(tree if tree else "(空目录)")
    print()

    # 统计信息
    print("=" * 60)
    print("📊 文件统计")
    print("=" * 60)
    print()

    stats = count_file_types(extract_dir)
    print(f"文件总数: {stats['total_files']}")
    print(f"总大小:   {format_size(stats['total_size'])}")
    print()

    print("按类型分布:")
    for category, count in sorted(stats['categories'].items(), key=lambda x: -x[1]):
        bar = "█" * min(count, 30)
        print(f"  {category}: {count} 个 {bar}")

    print()
    print("常见文件扩展名 (Top 10):")
    for ext, count in stats['extensions'].items():
        display_ext = ext if ext else '(无扩展名)'
        print(f"  {display_ext}: {count} 个")

    print()

    # 项目类型检测
    print("=" * 60)
    print("🔍 项目类型检测")
    print("=" * 60)
    print()

    project_types = detect_project_type(extract_dir)
    for pt in project_types:
        print(f"  ✅ {pt}")

    print()

    if is_temp:
        print(f"💡 提示：解压文件保存在临时目录 {extract_dir}")
        print(f"   使用 --output 参数可指定保存目录")
    else:
        print(f"✅ 解压完成，文件保存在 {extract_dir}")


if __name__ == '__main__':
    main()
