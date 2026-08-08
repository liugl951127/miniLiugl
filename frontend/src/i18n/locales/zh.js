/**
 * V6.3+ 简化版中文翻译表
 * 只保留核心页面用到的 key, 减少体积
 */
export default {
  app: {
    name: 'Liugl-AI 大模型平台',
    tagline: '企业级 LLM 应用平台'
  },
  login: {
    title: '登录',
    subtitle: '企业级 AI 平台 · 自研引擎 · 数据私有',
    guest: '访客试用',
    tab: { account: '账号登录', register: '注册', wechat: '微信' },
    label: { username: '用户名', nickname: '昵称', password: '密码', email: '邮箱' },
    placeholder: { username: '请输入用户名', nickname: '请输入昵称', password: '请输入密码', email: '请输入邮箱' },
    remember: '记住我', forgot: '忘记密码?'
  },
  nav: {
    chat: '智能对话', knowledge: '知识库', memory: '记忆', agent: 'Agent 自主任务',
    kg: '知识图谱', collab: '实时协作', plugins: '插件市场', admin: '管理后台', about: '关于'
  },
  admin: {
    title: '管理后台', home: '首页',
    health: { up: '正常', down: '异常', unknown: '未知' },
    alerts: '告警', collapse: '折叠侧边栏',
    quick: { title: '快捷入口', chat: '对话', kg: '知识图谱', agent: 'Agent', ai: 'AI 助手', marketplace: '市场', monitor: '监控', user: '用户', profile: '个人' },
    menu: { dashboard: '指标仪表盘', metrics: '实时指标', audit: '审计日志', alerts: '告警', traces: '分布式追踪', monitor: '系统监控', cluster: '集群', provider: '模型提供方', leaderboard: '排行榜', apikey: 'API Key 统计', framework: '框架', governance: '治理', document: '文档', push: '推送', wechat: '微信', wechatUnionid: 'UnionID' },
    group: { core: '核心管理', observability: '可观测性', system: '系统' }
  },
  chat: { title: '对话', placeholder: '输入消息...', send: '发送', empty: '开始第一次对话', newChat: '新建对话', history: '历史', settings: '设置' },
  apikey: {
    title: 'API Key 管理', myKeys: '管理您的 API Key', createKey: '创建密钥',
    name: '名称', scopes: '权限', expiresAt: '过期时间', expiresNever: '永不过期',
    namePlaceholder: '为密钥起个名字', scopesPlaceholder: '输入权限, 如: read,write',
    colLastUsed: '最后使用', colStatus: '状态', colActions: '操作', neverUsed: '从未使用',
    enabled: '已启用', disabled: '已禁用', disable: '禁用', enable: '启用', rotate: '轮换',
    rawKey: '密钥', confirmDelete: '确定要删除这个密钥吗?'
  },
  common: {
    confirm: '确定', cancel: '取消', save: '保存', delete: '删除', edit: '编辑', create: '创建',
    search: '搜索', loading: '加载中...', noData: '暂无数据', success: '操作成功', failed: '操作失败',
    networkError: '网络错误, 请重试', serverError: '服务异常, 请稍后重试', copy: '复制', copied: '已复制', refresh: '刷新'
  }
}
