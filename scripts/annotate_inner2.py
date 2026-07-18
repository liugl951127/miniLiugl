#!/usr/bin/env python3
"""
V3.5.12+ 给 .vue <script setup> 内的 method 加 JSDoc
严格限制: 只在 <script setup>...</script> 块内操作, 不碰 template
"""
import re
from pathlib import Path

BASE = Path('frontend/src')

METHOD_HINTS = {
    'load':  '加载',
    'fetch': '查询',
    'get':   '获取',
    'list':  '列表',
    'search':'搜索',
    'create':'创建',
    'save':  '保存',
    'update':'更新',
    'edit':  '编辑',
    'delete':'删除',
    'send':  '发送',
    'submit':'提交',
    'on':    '事件',
    'handle':'处理',
    'toggle':'切换',
    'switch':'切换',
    'select':'选择',
    'show':  '显示',
    'hide':  '隐藏',
    'open':  '打开',
    'close': '关闭',
    'start': '开始',
    'stop':  '停止',
    'test':  '测试',
    'validate':'校验',
    'check': '检查',
    'confirm':'确认',
    'cancel':'取消',
    'reset': '重置',
    'clear': '清空',
    'refresh':'刷新',
    'upload':'上传',
    'download':'下载',
    'export':'导出',
    'import':'导入',
    'init':  '初始化',
    'login': '登录',
    'logout':'退出',
    'register':'注册',
    'rename':'重命名',
    'drag':  '拖拽',
    'drop':  '放下',
    'exec':  '执行',
    'run':   '运行',
    'sync':  '同步',
    'push':  '推送',
    'fill':  '填充',
    'calc':  '计算',
    'parse': '解析',
    'format':'格式化',
    'copy':  '复制',
}

SKIP = {
    'onMounted', 'onUnmounted', 'onActivated', 'onDeactivated',
    'onBeforeMount', 'onBeforeUnmount', 'onBeforeUpdate',
    'onUpdated', 'onErrorCaptured', 'computed', 'watch', 'watchEffect',
    'defineEmits', 'defineProps', 'useRoute', 'useRouter', 'useStore',
    'useI18n', 'useHead', 'provide', 'inject',
    'ref', 'reactive', 'readonly', 'shallowRef', 'triggerRef',
    'nextTick', 'toRef', 'toRefs', 'unref',
}

def infer_doc(method_name):
    name = re.sub(r'^(on|do|_)?', '', method_name)
    for verb, base_desc in METHOD_HINTS.items():
        if name.startswith(verb):
            return f'{base_desc} ({method_name})'
    return method_name

def has_jsdoc_nearby(content, pos, scope=200):
    """检查 pos 之前 scope 字符内是否已有 JSDoc 注释"""
    prefix = content[max(0, pos-scope):pos]
    return '*/' in prefix[-150:]

def annotate_vue_strict(filepath):
    """只操作 <script setup> 块"""
    if not filepath.exists():
        return 0
    content = filepath.read_text()
    
    # 找 <script setup> 块
    m = re.search(r'(<script\s+setup[^>]*>)(.*?)(</script>)', content, re.DOTALL)
    if not m:
        return 0
    
    script_start_tag = m.group(1)
    script_body = m.group(2)
    script_end_tag = m.group(3)
    
    n_added = 0
    new_body = script_body
    
    # 找 method 定义: const X = async (...) / function X(...) / async function X(...)
    patterns = [
        r'(\n)([ \t]*)(const\s+(\w+)\s*=\s*async\s*\()',
        r'(\n)([ \t]*)(const\s+(\w+)\s*=\s*\()',
        r'(\n)([ \t]*)(async\s+function\s+(\w+)\s*\()',
        r'(\n)([ \t]*)(function\s+(\w+)\s*\()',
    ]
    
    for pat in patterns:
        for m2 in re.finditer(pat, new_body):
            indent = m2.group(2)
            method_name = m2.group(3)
            
            if method_name in SKIP:
                continue
            if method_name.startswith('use') or method_name.startswith('_'):
                continue
            if has_jsdoc_nearby(new_body, m2.start()):
                continue
            
            doc = infer_doc(method_name)
            old = m2.group(0)
            new = (
                f'{m2.group(1)}{indent}/**\n'
                f'{indent} * {doc}\n'
                f'{indent} */\n'
                f'{m2.group(1)}{old.lstrip()}'
            )
            new_body = new_body[:m2.start()] + new + new_body[m2.end():]
            n_added += 1
    
    if n_added > 0:
        new_content = content[:m.start()] + script_start_tag + new_body + script_end_tag + content[m.end():]
        filepath.write_text(new_content)
    return n_added

# 跑全部
total_files = 0
total_methods = 0
for f in BASE.rglob('*.vue'):
    n = annotate_vue_strict(f)
    if n > 0:
        total_files += 1
        total_methods += n
print(f"✅ {total_files} 个 .vue 内部 method 注释补全, 共 {total_methods} 个 method")
