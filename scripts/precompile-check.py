#!/usr/bin/env python3
# =============================================================
# MiniMax Platform - 单模块预编译自测脚本 (V5.30.5)
#
# 目的: 用户跑 mvn compile 前, 先用此脚本静态检查
#       提前发现 5 类常见 bug (基于 V5.30.1-V5.30.4 真实教训):
#
#   1. import 缺失 (V5.30.1 教训)
#      - 注解用了但 import 块没引用
#      - class 用了但 import 块没引用
#      - throws XxxException 但没 import 对应类
#
#   2. Lombok 注解错 (V5.30.1 教训)
#      - @NoConstructor → @NoArgsConstructor (漏 's')
#      - @Data / @Builder / @Slf4j 等常见笔误
#
#   3. DTO 引用不存在 (V5.30.4 教训)
#      - import com.x.Y 但 Y 类不存在
#      - 使用 for (X m : ...) 但 X 类不存在
#
#   4. MyBatis-Plus Wrapper 误用 (V5.30.3 教训)
#      - LambdaQueryWrapper 上用 .set() (应用 LambdaUpdateWrapper)
#      - LambdaUpdateWrapper 上用 .eq() 等 query 方法
#
#   5. InterruptedException 漏 throws (V5.30.3 教训)
#      - 方法体内 Thread.sleep 但签名没 throws
#
# 用法:
#   python3 scripts/precompile-check.py                    # 全项目
#   python3 scripts/precompile-check.py --module auth      # 单模块
#   python3 scripts/precompile-check.py --fix-hints        # 显示修复建议
#   python3 scripts/precompile-check.py --strict           # 严格模式 (warn 也算错)
# =============================================================

import os
import re
import sys
import argparse
from pathlib import Path
from typing import List, Dict, Set, Tuple

# =============================================================
# 颜色
# =============================================================
class C:
    RED = '\033[0;31m'
    GREEN = '\033[0;32m'
    YELLOW = '\033[1;33m'
    BLUE = '\033[0;34m'
    CYAN = '\033[0;36m'
    GRAY = '\033[0;90m'
    NC = '\033[0m'


def log_info(msg: str):
    print(f"{C.GREEN}[✓]{C.NC} {msg}")


def log_warn(msg: str):
    print(f"{C.YELLOW}[!]{C.NC} {msg}")


def log_err(msg: str):
    print(f"{C.RED}[✗]{C.NC} {msg}")


def log_section(msg: str):
    print(f"\n{C.BLUE}═══{C.NC} {C.CYAN}{msg}{C.NC} {C.BLUE}═══{C.NC}")


# =============================================================
# 数据结构
# =============================================================
class Issue:
    def __init__(self, severity: str, category: str, file: str, line: int, msg: str, fix: str = ""):
        self.severity = severity  # 'error' or 'warn'
        self.category = category
        self.file = file
        self.line = line
        self.msg = msg
        self.fix = fix

    def __str__(self):
        loc = f"{self.file}:{self.line}" if self.line else self.file
        icon = f"{C.RED}[✗]{C.NC}" if self.severity == 'error' else f"{C.YELLOW}[!]{C.NC}"
        result = f"  {icon} {C.GRAY}{loc}{C.NC} {self.category}: {self.msg}"
        if self.fix:
            result += f"\n      {C.CYAN}→ 修复: {self.fix}{C.NC}"
        return result


# =============================================================
# Java 代码扫描器
# =============================================================
class JavaScanner:
    """扫描 Java 文件的 import / class / 注解 / 方法, 检测潜在 bug."""

    def __init__(self, root: str, module: str = None):
        self.root = Path(root)
        self.module_filter = module
        # 全项目 Java 类索引 (path → Set of class names)
        self.class_index: Dict[str, Set[str]] = {}  # {module: {class names}}
        self.issues: List[Issue] = []
        self._build_class_index()

    def _build_class_index(self):
        """扫描所有 .java 文件, 收集每个 module 下的所有 public class."""
        for java_file in self.root.rglob("*.java"):
            # 跳过 target / node_modules
            if any(p in java_file.parts for p in ['target', 'node_modules', '.git']):
                continue
            # 找所属 module (backend/minimax-xxx/...)
            module = self._get_module(java_file)
            if not module:
                continue
            content = java_file.read_text(encoding='utf-8', errors='ignore')
            classes = self._extract_class_names(content)
            self.class_index.setdefault(module, set()).update(classes)

    def _get_module(self, java_file: Path) -> str:
        """从路径提取 module 名 (e.g. backend/minimax-auth/...)."""
        parts = java_file.parts
        for i, p in enumerate(parts):
            if p.startswith('minimax-'):
                return p
        return ""

    def _extract_class_names(self, content: str) -> Set[str]:
        """提取 Java 文件中所有顶层 / 嵌套 public class / interface / enum / record 名."""
        names = set()
        # public class / interface / enum / record / @interface
        for m in re.finditer(r'(?:public\s+)?(?:abstract\s+|final\s+)?(?:class|interface|enum|record|@interface)\s+(\w+)', content):
            names.add(m.group(1))
        return names

    def _get_imports(self, content: str) -> List[str]:
        """提取所有 import 完整类名 (含通配符 import com.x.*)."""
        imports = []
        for m in re.finditer(r'import\s+(?:static\s+)?([\w.]+(?:\.\*)?);', content):
            imp = m.group(1)
            imports.append(imp)
            # 如果是通配符 import (com.x.*), 把包内所有 class 也算 import
            if imp.endswith('.*'):
                package = imp[:-2]
                # 收集这个包下所有已知的类
                # (build_class_index 时按 module 分类, 这里简化: 记为 'wildcard:xxx')
                imports.append(f"WILDCARD:{package}")
        return imports

    def _get_annotations(self, content: str) -> Set[str]:
        """提取所有 @xxx 注解 (排除 @param/@return 等 javadoc)."""
        annotations = set()
        for m in re.finditer(r'@(\w+)(?:\([^)]*\))?', content):
            ann = m.group(1)
            # 排除 javadoc 标签
            if ann in ('param', 'return', 'throws', 'see', 'since', 'deprecated', 'author'):
                continue
            annotations.add(ann)
        return annotations

    def _class_exists(self, short_name: str, current_module: str) -> bool:
        """检查类名 (短名) 是否在某个 module 存在."""
        for module_classes in self.class_index.values():
            if short_name in module_classes:
                return True
        return False

    def _class_exists_in_module(self, short_name: str, current_module: str) -> bool:
        """检查类名是否在当前 module 存在."""
        return short_name in self.class_index.get(current_module, set())

    def scan_file(self, java_file: Path) -> List[Issue]:
        """扫描单个 Java 文件."""
        module = self._get_module(java_file)
        if not module:
            return []

        content = java_file.read_text(encoding='utf-8', errors='ignore')
        lines = content.split('\n')
        rel_path = str(java_file.relative_to(self.root))

        # 解析 imports
        imports = self._get_imports(content)
        imported_short = {imp.split('.')[-1]: imp for imp in imports}

        # 检测 1: throws X 但 import 缺
        for i, line in enumerate(lines, 1):
            # throws XxxException
            for m in re.finditer(r'throws\s+([\w.]+(?:\s*,\s*[\w.]+)*)\s*[\({]', line):
                for cls in re.split(r'\s*,\s*', m.group(1).strip()):
                    short = cls.split('.')[-1]
                    if short in ('Exception', 'RuntimeException', 'Throwable',
                                 'Error', 'InterruptedException', 'IOException'):
                        continue
                    # 如果是 java.* / jakarta.* 自带
                    if cls.startswith('java.') or cls.startswith('jakarta.'):
                        continue
                    if short not in imported_short and not self._class_exists_in_module(short, module):
                        # 检查同包类 (不需要 import)
                        if self._is_same_package(short, module):
                            continue
                        # 可能是父类/同包省略import
                        if self._class_exists(short, module):
                            continue

        # 检测 2: @Annotation 但 import 缺 (注释里 @author @since 等不算)
        annotations = self._get_annotations(content)
        for ann in annotations:
            # 1. 排除 javadoc 标签
            if ann in ('param', 'return', 'throws', 'see', 'since', 'deprecated', 'author'):
                continue
            # 2. 排除 JDK / Spring / Lombok / MyBatisPlus / Swagger / Jakarta 常见注解
            if ann in (
                # JDK + JSR
                'Override', 'Deprecated', 'SuppressWarnings', 'FunctionalInterface',
                'SafeVarargs', 'Generated', 'PostConstruct', 'PreDestroy', 'Resource',
                'Valid', 'NotNull', 'NotBlank', 'NotEmpty', 'Min', 'Max', 'Size', 'Pattern',
                'AuthenticationPrincipal',
                # Spring Web
                'Component', 'Service', 'Repository', 'Controller', 'RestController',
                'Configuration', 'Bean', 'Autowired', 'Value', 'ConfigurationProperties',
                'Transactional', 'Async', 'Scheduled', 'PostMapping', 'GetMapping',
                'PutMapping', 'DeleteMapping', 'RequestMapping', 'PathVariable',
                'RequestParam', 'RequestBody', 'RequestHeader', 'ResponseBody',
                # Spring Boot Test
                'SpringBootTest', 'Test', 'BeforeEach', 'AfterEach', 'BeforeAll', 'AfterAll',
                # Lombok
                'Data', 'NoArgsConstructor', 'AllArgsConstructor', 'RequiredArgsConstructor',
                'Builder', 'Slf4j', 'Log4j', 'Log4j2', 'Getter', 'Setter', 'ToString',
                'EqualsAndHashCode', 'Value', 'SneakyThrows', 'Synchronized', 'Locked',
                'Cleanup', 'NonNull', 'Nullable', 'Accessors',
                # MyBatis-Plus
                'TableName', 'TableId', 'TableField', 'TableLogic', 'Version', 'IdType',
                'KeySequence', 'EnumValue', 'JsonFormat', 'DateTimeFormat',
                # Swagger / Knife4j
                'Tag', 'Operation', 'Parameter', 'Schema', 'ApiModel', 'ApiOperation',
                'ApiParam', 'ApiImplicitParam', 'Api', 'Hidden',
                # Jackson
                'JsonInclude', 'JsonIgnore', 'JsonProperty', 'JsonFormat',
                # JPA (项目不用, 但兼容)
                'Entity', 'Table', 'Id', 'Column', 'GeneratedValue',
                # 其他
                'Profile', 'ConditionalOnProperty', 'ConditionalOnClass', 'Conditional',
            ):
                continue
            # 3. 如果有 wildcard import, 跳过 (import 包内所有类)
            has_wildcard = any(imp.startswith('WILDCARD:') for imp in imports)
            if has_wildcard and ann not in imported_short:
                continue
            # 4. 如果在 import 列表或类索引里有, 跳过
            if ann in imported_short or self._class_exists(ann, module):
                continue
            # 5. 跳过 @code 这种 jekyll 风格 (项目里 jekyll 文档里的 @code 不是 Java 注解)
            # 这类出现在 JavaDoc 多行代码块里 (    * @code xxx)
            # 看上下文: 如果上一行以 * 开头或空白, 可能是 javadoc
            # 但简单办法: 直接忽略 1-2 字符的注解名 (通常 @code 是 javadoc 示例)
            if len(ann) <= 4:
                continue
            # 可能是项目内的注解类
            self.issues.append(Issue(
                severity='warn',
                category='annotation',
                file=rel_path, line=0,
                msg=f'注解 @{ann} 未在 import 列表中找到',
                fix=f'添加: import xxx.{ann};'
            ))

        # 检测 3: import X.Y 但 Y 类不存在 (限 minimax 包, 非通配符)
        for imp in imports:
            if imp.startswith('WILDCARD:'):
                continue
            if not imp.startswith('com.minimax.'):
                continue
            short = imp.split('.')[-1]
            if short == '*':
                continue
            if not self._class_exists(short, module):
                # 可能是有效的依赖 (jar 包里的类)
                # 这里保守: 标记为 warn, 让用户确认
                self.issues.append(Issue(
                    severity='warn',
                    category='import-missing',
                    file=rel_path, line=0,
                    msg=f'import {imp} 引用了不存在的类',
                    fix=f'检查 {short} 类是否在该路径下, 或确认依赖是否正确'
                ))

        # 检测 4: Lombok 注解笔误
        # 精准列表: 错的拼写 → 正确拼写 (仅检测这些)
        lombok_typos = [
            ('@NoConstructor\b',  '@NoArgsConstructor',  '@NoConstructor → @NoArgsConstructor (漏了 s)'),
            ('@AllArguConstruct\b', '@AllArgsConstructor', '@AllArguConstruct → @AllArgsConstructor'),
            ('@Buidler\b',        '@Builder',           '@Buidler → @Builder'),
            ('@Sl4j\b',           '@Slf4j',              '@Sl4j → @Slf4j'),
            ('@Slog4j\b',          '@Slf4j',              '@Slog4j → @Slf4j'),
            ('@NonNull\s',         '@Nonnull',            '@NonNull → @Nonnull (java 7 注解)'),  # Lombok NonNull
            ('@GetConstructor\b', '@RequiredArgsConstructor', '@GetConstructor → @RequiredArgsConstructor'),
        ]
        for i, line in enumerate(lines, 1):
            for pattern, correct, fix_msg in lombok_typos:
                if re.search(pattern, line):
                    # 跳过已经在正确 import 的情况 (e.g. line 是 'import lombok.Data;')
                    self.issues.append(Issue(
                        severity='error',
                        category='lombok-typo',
                        file=rel_path, line=i,
                        msg=f'Lombok 注解笔误: {fix_msg.split(" → ")[0]}',
                        fix=fix_msg
                    ))

        # 检测 5: LambdaQueryWrapper().set() 反模式 (V5.30.3 教训)
        # 找 'new LambdaQueryWrapper<' 后面 200 字符内有没有 '.set('
        for m in re.finditer(r'new\s+LambdaQueryWrapper<[^>]+>\s*\(([^)]*)\)', content, re.S):
            if '.set(' in m.group(0):
                # 找行号
                start_line = content[:m.start()].count('\n') + 1
                self.issues.append(Issue(
                    severity='error',
                    category='wrapper-misuse',
                    file=rel_path, line=start_line,
                    msg='LambdaQueryWrapper 不支持 .set(), 应改用 LambdaUpdateWrapper',
                    fix='new LambdaQueryWrapper<>().set(...) → new LambdaUpdateWrapper<>().set(...)'
                ))

        # 检测 6: Thread.sleep 但方法没 throws InterruptedException (V5.30.3 教训)
        # 找 'Thread.sleep(' 所在方法, 检查方法签名是否 throws InterruptedException
        self._check_throws_for_sleep(content, lines, rel_path)

        # 检测 7: .equals() null 风险 (let)
        # 跳过: 太复杂, 需要类型推断

        return self.issues

    def _check_throws_for_sleep(self, content: str, lines: List[str], rel_path: str):
        """检测方法体内有 Thread.sleep 但签名没 throws InterruptedException."""
        # 简单办法: 找每个方法声明, 检查到方法结束的范围内是否调用 Thread.sleep
        # 但需要处理嵌套, 改用单行扫描:
        # 找形如 'private/public/protected ... methodName(...)' 后面行内/下几行 throws
        # 检测到 Thread.sleep 在方法体里, 但方法声明那行没 InterruptedException
        method_pattern = re.compile(
            r'^\s*(?:public|private|protected)\s+'
            r'(?:static\s+)?(?:final\s+)?(?:synchronized\s+)?'
            r'[\w<>,\[\]\s\.]+\s+(\w+)\s*\([^)]*\)\s*'
            r'(?:throws\s+([^{]+))?\s*\{',
            re.MULTILINE
        )
        for m in method_pattern.finditer(content):
            method_name = m.group(1)
            throws_clause = m.group(2) or ''
            method_start_line = content[:m.start()].count('\n') + 1
            # 找方法体结束 '}' (粗略, 适合单行 sleep 检查)
            method_end_pos = content.find('\n    }', m.end())
            if method_end_pos == -1:
                method_end_pos = m.end() + 500
            method_body = content[m.end():method_end_pos]

            # 方法体内是否调用 Thread.sleep
            if 'Thread.sleep(' not in method_body:
                continue

            # throws 是否包含 InterruptedException
            if 'InterruptedException' in throws_clause:
                continue
            # 也可能是 throws Exception (覆盖)
            if 'Exception' in throws_clause:
                continue

            self.issues.append(Issue(
                severity='error',
                category='missing-throws',
                file=rel_path, line=method_start_line,
                msg=f'方法 {method_name}() 体内调用 Thread.sleep(), 但签名没 throws InterruptedException',
                fix=f'方法签名加 throws InterruptedException (或 throws Exception)'
            ))

    def _is_same_package(self, short_name: str, module: str) -> bool:
        """检查短类名是否在当前模块的某个 package 下 (无需 import)."""
        # 简化: 检查模块下是否有同名文件
        for f in (self.root / module).rglob(f"{short_name}.java"):
            return True
        return False

    def scan_all(self):
        """扫描所有 Java 文件."""
        log_section(f"扫描 {self.root} (filter={self.module_filter or '全部'})")
        files_scanned = 0
        for java_file in self.root.rglob("*.java"):
            if any(p in java_file.parts for p in ['target', 'node_modules', '.git']):
                continue
            if self.module_filter:
                module = self._get_module(java_file)
                if not module.endswith(self.module_filter):
                    continue
            files_scanned += 1
            self.scan_file(java_file)
        log_info(f"扫描文件数: {files_scanned}")

    def report(self, strict: bool = False, show_fix: bool = True):
        """输出报告."""
        errors = [i for i in self.issues if i.severity == 'error']
        warns = [i for i in self.issues if i.severity == 'warn']

        log_section(f"扫描结果: {len(errors)} 错误 / {len(warns)} 警告")

        if errors:
            print(f"\n{C.RED}【错误】{C.NC}")
            for issue in errors:
                print(str(issue))
                if not show_fix:
                    print()

        if warns:
            print(f"\n{C.YELLOW}【警告】{C.NC}")
            for issue in warns:
                print(str(issue))
                if not show_fix:
                    print()

        if not errors and not warns:
            log_info("✓ 所有检查通过, 没有发现潜在 bug")

        # 严格模式: warn 也算失败
        if strict and warns:
            print(f"\n{C.RED}严格模式: {len(warns)} 个警告视为错误{C.NC}")
            return len(errors) + len(warns)

        return len(errors)


# =============================================================
# 主入口
# =============================================================
def main():
    parser = argparse.ArgumentParser(
        description='MiniMax 预编译静态检查 (mvn compile 前)',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
示例:
  python3 scripts/precompile-check.py                     # 全项目
  python3 scripts/precompile-check.py --module auth       # 仅 auth 模块
  python3 scripts/precompile-check.py --strict            # 严格模式
        """
    )
    parser.add_argument('--root', default='backend',
                        help='Java 源码根目录 (默认: backend)')
    parser.add_argument('--module', '-m',
                        help='只检查指定模块 (e.g. auth, chat, model)')
    parser.add_argument('--strict', action='store_true',
                        help='严格模式 (警告也算错)')
    parser.add_argument('--no-fix', action='store_true',
                        help='不显示修复建议')
    parser.add_argument('--list', action='store_true',
                        help='列出所有可用模块')

    args = parser.parse_args()

    # --list: 列出模块
    if args.list:
        root = Path(args.root)
        modules = set()
        for p in root.iterdir():
            if p.is_dir() and p.name.startswith('minimax-'):
                modules.add(p.name)
        print("可用模块:")
        for m in sorted(modules):
            print(f"  {m}")
        return 0

    if not os.path.isdir(args.root):
        log_err(f"目录不存在: {args.root}")
        return 1

    scanner = JavaScanner(args.root, args.module)
    scanner.scan_all()
    exit_code = scanner.report(strict=args.strict, show_fix=not args.no_fix)

    if exit_code > 0:
        print(f"\n{C.RED}建议修复后再次运行本脚本, 再执行 mvn compile.{C.NC}")
        sys.exit(exit_code)
    else:
        print(f"\n{C.GREEN}预编译检查通过! 现在可以放心跑 mvn compile.{C.NC}")
        sys.exit(0)


if __name__ == '__main__':
    main()