-- Agent Forge 预置数据 (V2.0)
-- 6 个行业智能体模板

INSERT INTO agent_template (code, name, industry, description, emoji, color, agents, workflow, tools, recommended_model, usage_count, status) VALUES
('edu-cs', '在线教育客服', '教育', '课程咨询 / 退费处理 / 学习指导 / 质检', '🎓',
 'linear-gradient(135deg, #6366f1, #8b5cf6)',
 '[
    {"name":"小课","role":"课程顾问","emoji":"📚","color":"linear-gradient(135deg, #6366f1, #8b5cf6)","desc":"回答课程相关问题, 推荐合适课程","tools":["课程搜索","价格查询","试听预约"],"model":"Qwen2.5-7B"},
    {"name":"小助","role":"退费专员","emoji":"💰","color":"linear-gradient(135deg, #f59e0b, #ef4444)","desc":"处理退费流程, 解释政策","tools":["订单查询","工单创建","支付接口"],"model":"Qwen2.5-7B"},
    {"name":"小导","role":"学习规划师","emoji":"🎯","color":"linear-gradient(135deg, #10b981, #06b6d4)","desc":"基于学员情况定制学习计划","tools":["用户画像","课程匹配","进度跟踪"],"model":"Qwen2.5-7B"},
    {"name":"小审","role":"质检员","emoji":"🔍","color":"linear-gradient(135deg, #ec4899, #f43f5e)","desc":"监控对话质量, 标记异常","tools":["情感分析","敏感词检测"],"model":"Qwen2.5-0.5B"}
 ]',
 '[
    {"step":1,"name":"用户提问"},{"step":2,"name":"意图识别"},{"step":3,"name":"路由分发"},
    {"step":4,"name":"小课 处理"},{"step":5,"name":"小助 处理"},{"step":6,"name":"小导 处理"},
    {"step":7,"name":"小审 质检"},{"step":8,"name":"回复用户"}
 ]',
 '["课程搜索","订单查询","工单系统","用户画像","情感分析"]',
 'Qwen2.5-7B + Qwen2.5-0.5B', 1247, 'PUBLISHED'),

('ecom-cs', '电商客服系统', '电商', '订单查询 / 退换货 / 评价 / 物流', '🛒',
 'linear-gradient(135deg, #f59e0b, #ef4444)',
 '[
    {"name":"小购","role":"购物顾问","emoji":"🛒","color":"linear-gradient(135deg, #f59e0b, #ef4444)","desc":"推荐商品, 处理订单","tools":["商品搜索","推荐算法"],"model":"Qwen2.5-7B"},
    {"name":"小售","role":"售后客服","emoji":"📦","color":"linear-gradient(135deg, #10b981, #06b6d4)","desc":"退换货, 物流查询","tools":["物流接口","订单系统"],"model":"Qwen2.5-7B"},
    {"name":"小评","role":"评价分析","emoji":"⭐","color":"linear-gradient(135deg, #8b5cf6, #ec4899)","desc":"分析用户评价","tools":["NLP","情感分析"],"model":"Qwen2.5-0.5B"}
 ]',
 '[
    {"step":1,"name":"用户提问"},{"step":2,"name":"意图识别"},{"step":3,"name":"小购/小售 路由"},
    {"step":4,"name":"订单处理"},{"step":5,"name":"小评 反馈"},{"step":6,"name":"回复用户"}
 ]',
 '["商品搜索","物流接口","订单系统","NLP"]',
 'Qwen2.5-7B + Qwen2.5-0.5B', 892, 'PUBLISHED'),

('code-review', '代码评审助手', '开发', 'PR 审查 / 规范检查 / 测试建议', '💻',
 'linear-gradient(135deg, #10b981, #06b6d4)',
 '[
    {"name":"小审","role":"代码审查","emoji":"💻","color":"linear-gradient(135deg, #10b981, #06b6d4)","desc":"PR 审查, 规范检查","tools":["Git API","Linter","SonarQube"],"model":"Qwen2.5-7B-Coder"},
    {"name":"小测","role":"测试生成","emoji":"🧪","color":"linear-gradient(135deg, #8b5cf6, #ec4899)","desc":"生成单元测试","tools":["测试框架","覆盖率分析"],"model":"Qwen2.5-7B-Coder"},
    {"name":"小规","role":"规范专家","emoji":"📏","color":"linear-gradient(135deg, #f59e0b, #f97316)","desc":"代码规范, 安全检查","tools":["ESLint","安全扫描"],"model":"Qwen2.5-7B"}
 ]',
 '[
    {"step":1,"name":"PR 提交"},{"step":2,"name":"触发审查"},{"step":3,"name":"小审 分析"},
    {"step":4,"name":"小规 检查"},{"step":5,"name":"小测 生成"},{"step":6,"name":"汇总报告"}
 ]',
 '["Git API","Linter","SonarQube","ESLint","安全扫描"]',
 'Qwen2.5-7B-Coder', 654, 'PUBLISHED'),

('finance-risk', '金融风控平台', '金融', '欺诈检测 / 信用评估 / 合规审核', '🏦',
 'linear-gradient(135deg, #1e293b, #475569)',
 '[
    {"name":"小风","role":"风控官","emoji":"🛡️","color":"linear-gradient(135deg, #1e293b, #475569)","desc":"欺诈检测, 风险评估","tools":["征信接口","行为分析"],"model":"Qwen2.5-72B"},
    {"name":"小投","role":"投资顾问","emoji":"💹","color":"linear-gradient(135deg, #10b981, #059669)","desc":"投资建议, 资产配置","tools":["行情接口","组合优化"],"model":"Qwen2.5-72B"},
    {"name":"小审","role":"合规审核","emoji":"⚖️","color":"linear-gradient(135deg, #6366f1, #8b5cf6)","desc":"KYC, 反洗钱","tools":["身份核验","反洗钱"],"model":"Qwen2.5-72B"}
 ]',
 '[
    {"step":1,"name":"用户请求"},{"step":2,"name":"小风 风控"},{"step":3,"name":"小投 投资建议"},
    {"step":4,"name":"小审 合规审核"},{"step":5,"name":"汇总报告"}
 ]',
 '["征信接口","行情接口","身份核验","反洗钱"]',
 'Qwen2.5-72B', 421, 'PUBLISHED'),

('medical-triage', '医疗问诊机器人', '医疗', '症状问诊 / 导诊 / 健康咨询', '⚕️',
 'linear-gradient(135deg, #ec4899, #f43f5e)',
 '[
    {"name":"小医","role":"问诊医生","emoji":"⚕️","color":"linear-gradient(135deg, #ec4899, #f43f5e)","desc":"症状问诊, 初步诊断","tools":["医学知识库","ICD-10"],"model":"Qwen2.5-72B-Med"},
    {"name":"小护","role":"导诊护士","emoji":"💊","color":"linear-gradient(135deg, #10b981, #06b6d4)","desc":"导诊, 用药指导","tools":["药品库","医院数据库"],"model":"Qwen2.5-7B"},
    {"name":"小顾","role":"健康顾问","emoji":"💪","color":"linear-gradient(135deg, #f59e0b, #f97316)","desc":"健康咨询, 慢病管理","tools":["健康档案","运动饮食"],"model":"Qwen2.5-7B"}
 ]',
 '[
    {"step":1,"name":"用户症状"},{"step":2,"name":"小医 问诊"},{"step":3,"name":"初步诊断"},
    {"step":4,"name":"小护 导诊"},{"step":5,"name":"小顾 后续管理"}
 ]',
 '["医学知识库","ICD-10","药品库","医院数据库","健康档案"]',
 'Qwen2.5-72B-Med', 318, 'PUBLISHED'),

('custom', '自定义项目', '通用', '自由组合任意智能体, 灵活部署', '✨',
 'linear-gradient(135deg, #8b5cf6, #ec4899)',
 '[]',
 '[]',
 '[]',
 'Qwen2.5-7B', 256, 'PUBLISHED');

-- 更新使用次数
UPDATE agent_template SET usage_count = usage_count + 10 WHERE code IN ('edu-cs', 'ecom-cs', 'code-review', 'finance-risk', 'medical-triage', 'custom');
