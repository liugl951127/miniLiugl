-- ==============================================================
-- V4.3: Prompt 模板系统 (minimax-prompt 8091)
-- prompt_template 表 — 变量占位符 + 分类管理 + 使用统计
-- ==============================================================

CREATE TABLE IF NOT EXISTS `prompt_template` (
  `id`           BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '模板ID',
  `name`         VARCHAR(100)  NOT NULL COMMENT '模板名称',
  `description`  VARCHAR(500)  DEFAULT '' COMMENT '模板描述',
  `category`     VARCHAR(50)   DEFAULT '其他' COMMENT '分类: 翻译/代码/写作/分析/营销/客服/其他',
  `content`      TEXT          NOT NULL COMMENT '模板内容，支持 {{variable}} 占位符',
  `variables`    JSON          DEFAULT NULL COMMENT '变量定义 JSON 数组',
  `creator_id`   BIGINT        NOT NULL COMMENT '创建者用户ID',
  `creator_name` VARCHAR(100)  DEFAULT '' COMMENT '创建者用户名',
  `is_public`    TINYINT(1)    DEFAULT FALSE COMMENT '是否公开: 1=所有人可用, 0=仅创建者',
  `use_count`    INT           DEFAULT 0 COMMENT '使用次数',
  `created_at`   DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`   DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`      TINYINT(1)    DEFAULT 0 COMMENT '软删除标记',
  INDEX idx_category (`category`),
  INDEX idx_creator (`creator_id`),
  INDEX idx_public (`is_public`),
  INDEX idx_deleted (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Prompt 模板表';

-- ==============================================================
-- 内置 5 个系统模板 (creator_id=1, admin)
-- ==============================================================

INSERT INTO `prompt_template` (`name`, `description`, `category`, `content`, `variables`, `creator_id`, `creator_name`, `is_public`, `use_count`)
VALUES
(
  '翻译助手',
  '专业翻译，支持任意语言对',
  '翻译',
  '你是一个专业的翻译助手。请将以下文本翻译成 {{目标语言}}：\n\n{{原文}}\n\n要求：\n1. 保持原文风格和语气\n2. 专业术语准确\n3. 流畅自然',
  '[{"name":"目标语言","description":"目标语言，如中文、英文、日文","required":true},{"name":"原文","description":"需要翻译的原始文本","required":true}]',
  1, 'admin', TRUE, 0
),
(
  '代码审查',
  '审查代码质量，提出改进建议',
  '代码',
  '你是一位资深的代码审查专家。请审查以下代码并提供详细反馈：\n\n语言：{{语言}}\n代码：\n{{代码}}\n\n请从以下维度评分并给出建议（1-10分）：\n1. 代码可读性\n2. 性能优化\n3. 安全性\n4. 最佳实践\n5. 整体评分',
  '[{"name":"语言","description":"编程语言，如 Java、Python、JavaScript","required":true},{"name":"代码","description":"需要审查的代码","required":true}]',
  1, 'admin', TRUE, 0
),
(
  '会议纪要',
  '从会议内容提取关键信息，生成结构化纪要',
  '写作',
  '你是一位专业的行政助理。请根据以下会议内容，生成一份结构化的会议纪要：\n\n会议内容：\n{{会议内容}}\n\n请包含：\n1. 会议主题\n2. 参会人员\n3. 讨论要点\n4. 决策事项\n5. 下一步行动项',
  '[{"name":"会议内容","description":"会议录音或文字记录","required":true}]',
  1, 'admin', TRUE, 0
),
(
  '营销文案',
  '生成吸引人的营销文案',
  '营销',
  '你是{{公司名}}的专业营销文案专家。请为以下产品生成营销文案：\n\n产品名称：{{产品名称}}\n产品特点：{{产品特点}}\n目标受众：{{目标受众}}\n推广渠道：{{推广渠道}}\n\n要求：\n1. 突出产品核心卖点\n2. 符合目标受众偏好\n3. 适合指定渠道风格\n4. 包含明确的 CTA（行动号召）',
  '[{"name":"公司名","description":"公司名称","required":true},{"name":"产品名称","description":"产品名称","required":true},{"name":"产品特点","description":"产品核心卖点","required":true},{"name":"目标受众","description":"目标用户群体","required":true},{"name":"推广渠道","description":"推广渠道，如小红书/抖音/公众号","required":true}]',
  1, 'admin', TRUE, 0
),
(
  '故障排查助手',
  '分析问题，提供排查步骤',
  '分析',
  '你是一位经验丰富的 SRE/运维专家。用户报告了以下问题：\n\n问题描述：{{问题描述}}\n环境信息：{{环境信息}}\n相关日志：\n{{相关日志}}\n\n请按以下步骤提供排查方案：\n1. 可能原因分析（按概率排序）\n2. 建议排查步骤（从易到难）\n3. 快速止血方案\n4. 根因定位建议',
  '[{"name":"问题描述","description":"问题的详细描述","required":true},{"name":"环境信息","description":"操作系统、版本、中间件版本等","required":false},{"name":"相关日志","description":"错误日志或相关系统日志","required":false}]',
  1, 'admin', TRUE, 0
)
ON DUPLICATE KEY UPDATE `id` = `id`;
