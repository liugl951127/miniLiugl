#!/usr/bin/env python3
"""
为 .vue 文件批量加注释 (V3.5.12+)

策略:
- 文件头: <template> 之前加 JSDoc 注释
- <script setup>: import 之前加 section 注释
- 关键 ref/computed: 加内联 // 注释
"""
import re
from pathlib import Path

BASE = Path('/workspace/miniLiugl/frontend/src')

# 文件名 → 用途推断 (常用 keywords)
PURPOSE_HINTS = {
    'Login': '登录页',
    'Register': '注册页',
    'H5Login': 'H5 跨平台登录页 (移动端)',
    'Home': '首页',
    'Dashboard': '仪表盘',
    'Profile': '个人中心',
    'Settings': '设置',
    'Chat': 'AI 聊天对话',
    'Sessions': '会话列表',
    'Messages': '消息管理',
    'Models': '模型管理',
    'Tools': '工具管理',
    'Prompts': '提示词管理',
    'Knowledge': '知识库',
    'Documents': '文档管理',
    'Users': '用户管理',
    'Roles': '角色管理',
    'Tenants': '租户管理',
    'Stats': '统计',
    'Monitor': '监控',
    'Alert': '告警',
    'Pipelines': '流水线',
    'Workflow': '工作流',
    'Training': '训练',
    'Marketplace': '市场',
    'Memory': '记忆',
    'Collab': '协作',
    'ApiKey': 'API Key',
    'System': '系统',
    'Analytics': '分析',
    'Multimodal': '多模态',
    'Image': '图片',
    'Video': '视频',
    'Audio': '音频',
    'Music': '音乐',
    'Ppt': 'PPT',
    'Index': '入口/列表',
    'Layout': '布局',
    'NavBar': '导航栏',
    'Sidebar': '侧边栏',
    'Header': '顶部',
    'Footer': '底部',
    'Status': '状态',
    'LoginForm': '登录表单',
    'UserInfo': '用户信息',
    'Avatar': '头像',
    'Icon': '图标',
    'Button': '按钮',
    'Modal': '模态框',
    'Dialog': '对话框',
    'Card': '卡片',
    'Table': '表格',
    'Form': '表单',
    'Chart': '图表',
}

def infer_purpose(filepath: Path) -> str:
    """根据文件名推断用途"""
    name = filepath.stem
    # 完全匹配
    if name in PURPOSE_HINTS:
        return PURPOSE_HINTS[name]
    # 包含匹配
    for kw, desc in PURPOSE_HINTS.items():
        if kw in name:
            return f'{desc} ({name})'
    return f'{name} 页面'

# 已知核心页 (有详细描述)
DETAILED_PURPOSES = {
    'Index.vue':         '应用主布局 (含侧边栏/头部/路由出口)',
    'Layout':            'Layout 组件',
    'Login':             '账号密码登录 (邮箱/手机号/微信扫码)',
    'H5Login':           'H5 移动端登录 (OAuth/手机号验证码)',
    'Home':              '首页 (展示核心指标 + 快捷入口)',
    'Chat':              'AI 对话页 (支持 13 意图识别 + 多模态输入)',
    'Sessions':          'AI 会话列表 (分页/搜索/删除)',
    'Models':            'AI 模型管理 (Provider/Config/对战日志)',
    'Tools':             'AI 工具管理 (CRUD + 调用测试)',
    'Prompts':           '提示词模板管理 (分类/版本/调用)',
    'Knowledge':         'RAG 知识库 (文档上传/分块/检索)',
    'Users':             '后台用户管理 (CRUD + 角色绑定)',
    'Tenants':           '租户管理 (多租户隔离)',
    'ApiKey':            'API Key 管理 (生成/吊销/调用统计)',
    'Monitor':           '服务监控 (CPU/内存/接口/告警)',
    'Pipelines':         'AI 训练/ETL 流水线 (DAG 编辑)',
    'Multimodal':        '多模态能力 (图/音/视频上传与生成)',
    'Analytics':         '数据分析 (6 意图 + 5 实体 + 12 问题类型)',
    'Marketplace':       'AI 模型/工具/Agent 市场 (浏览/订阅)',
    'Memory':            'AI 长短期记忆 (对话上下文管理)',
    'Training':          '模型训练任务 (LoRA/微调/部署)',
    'Collab':            '实时协作房间 (CRDT 多人编辑)',
    'Dashboard':         '指标仪表盘 (ECharts 可视化)',
    'Index.vue':         '侧边栏导航 + 路由容器',
    'PwaStatusBar':      'PWA 状态栏组件 (在线/更新提示)',
    'LoginForm':         '登录表单组件 (验证 + 提交)',
    'Sidebar':           '侧边栏组件 (菜单树 + 折叠)',
    'useAuth':           '认证组合式函数 (login/logout/refresh)',
    'usePwa':            'PWA 组合式函数 (SW 注册/版本检查)',
    'useTheme':          '主题切换组合式函数 (深色/浅色)',
    'useBrowserCompat':  '浏览器兼容检测 (polyfill 加载)',
}

def get_purpose(filepath: Path) -> str:
    name = filepath.stem
    if name in DETAILED_PURPOSES:
        return DETAILED_PURPOSES[name]
    return infer_purpose(filepath)

def annotate_vue(filepath: Path) -> bool:
    """给 .vue 文件加注释头"""
    if not filepath.exists():
        return False
    content = filepath.read_text()
    # 已有 V3.5.12 注释头不动
    if 'V3.5.12+' in content[:300]:
        return False

    purpose = get_purpose(filepath)
    relative = filepath.relative_to(BASE)

    header = f'''<!--
  @file {relative} ({purpose})
  @version V3.5.12+ (前端注释补全)
  @description {purpose}
-->
'''

    # 找 <script setup> 块开头, 加 import 分组注释
    new_content = content
    # 找第一个 <script> 或 <script setup> 之后的 import
    m = re.search(r'(<script[^>]*>\n)', content)
    if m:
        script_start = m.end()
        # 加 module 注释
        new_content = (
            content[:script_start]
            + '// ───── 依赖导入 ─────\n'
            + content[script_start:]
        )

    new_content = header + new_content
    if new_content != content:
        filepath.write_text(new_content)
        return True
    return False

def annotate_js(filepath: Path, purpose: str) -> bool:
    """给 .js 文件加注释头"""
    if not filepath.exists():
        return False
    content = filepath.read_text()
    if 'V3.5.12+' in content[:300]:
        return False
    header = f'''/**
 * @file {filepath.name} - {purpose}
 * @version V3.5.12+ (前端注释补全)
 */
'''
    filepath.write_text(header + content)
    return True

# ── 主流程 ──────────────────
def main():
    print("═══════════════════════════════════════════════════════════")
    print("  前端 .vue / 其它 .js 注释补全 (V3.5.12+)")
    print("═══════════════════════════════════════════════════════════")
    print()

    # views/
    views_dir = BASE / 'views'
    if views_dir.exists():
        print("【1/4】views/ 目录")
        cnt = 0
        for f in sorted(views_dir.rglob('*.vue')):
            if annotate_vue(f):
                cnt += 1
        total = sum(1 for _ in views_dir.rglob('*.vue'))
        print(f"  补 {cnt} / 共 {total} 个 .vue")

    # components/
    print()
    print("【2/4】components/ 目录")
    comp_dir = BASE / 'components'
    if comp_dir.exists():
        cnt = 0
        for f in sorted(comp_dir.rglob('*.vue')):
            if annotate_vue(f):
                cnt += 1
        total = sum(1 for _ in comp_dir.rglob('*.vue'))
        print(f"  补 {cnt} / 共 {total} 个 .vue")

    # composables/
    print()
    print("【3/4】composables/ 目录")
    compo_dir = BASE / 'composables'
    if compo_dir.exists():
        cnt = 0
        for f in sorted(compo_dir.glob('*.js')):
            name = f.stem
            purpose = DETAILED_PURPOSES.get(name, f'{name} 组合式 API')
            if annotate_js(f, purpose):
                cnt += 1
        total = sum(1 for _ in compo_dir.glob('*.js'))
        print(f"  补 {cnt} / 共 {total} 个 .js")

    # directives/ + layout/ + router/
    print()
    print("【4/4】directives/ + layout/ + router/ 目录")
    for sub in ['directives', 'layout']:
        d = BASE / sub
        if d.exists():
            for f in sorted(d.rglob('*.js')):
                annotate_js(f, f.stem)
            for f in sorted(d.rglob('*.vue')):
                annotate_vue(f)

    print()
    print("✅ views/ + components/ + composables/ + 其它 注释补全")

if __name__ == '__main__':
    main()
