-- ============================================================
-- 19_v4_3_prompt.sql
-- Prompt 模板系统 (V4.3, minimax-prompt 8091)
--
-- 模块: backend/minimax-prompt/
-- 实体: PromptTemplate.java (@TableName("prompt_template"))
-- 字段 (14):
--   id           BIGINT       AUTO_INCREMENT  PRIMARY KEY
--   name         VARCHAR(128) NOT NULL                  -- 模板名
--   description  VARCHAR(512)                          -- 模板描述
--   category     VARCHAR(32)                           -- 翻译/代码/写作/分析/客服/其他
--   content      TEXT         NOT NULL                 -- 模板内容 (含 {{variable}})
--   variables    TEXT                                  -- 变量 JSON: [{"name","description","required"}]
--   creator_id   BIGINT                                 -- 创建者 user_id
--   creator_name VARCHAR(64)                            -- 创建者 username (冗余加速查询)
--   is_public    TINYINT(1)   DEFAULT 1                -- 公开/私有
--   use_count    INT          DEFAULT 0                 -- 使用次数
--   created_at   DATETIME     DEFAULT CURRENT_TIMESTAMP
--   updated_at   DATETIME     ON UPDATE CURRENT_TIMESTAMP
--   deleted      INT          DEFAULT 0                 -- 逻辑删除 (MyBatis-Plus @TableLogic)
-- ============================================================

USE `minimax_platform`;

DROP TABLE IF EXISTS `prompt_template`;
CREATE TABLE `prompt_template` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `name`          VARCHAR(128) NOT NULL                            COMMENT '模板名称 (唯一 + 分类内)',
  `description`   VARCHAR(512)                         DEFAULT NULL COMMENT '模板描述',
  `category`      VARCHAR(32)                          DEFAULT '其他' COMMENT '分类: 翻译/代码/写作/分析/客服/其他',
  `content`       TEXT         NOT NULL                            COMMENT '模板内容, 含 {{variable}} 占位符',
  `variables`     TEXT                                  DEFAULT NULL COMMENT '变量列表 JSON: [{"name":"语言","description":"目标语言","required":true}]',
  `creator_id`    BIGINT                                DEFAULT NULL COMMENT '创建者 user_id',
  `creator_name`  VARCHAR(64)                          DEFAULT NULL COMMENT '创建者 username (冗余)',
  `is_public`     TINYINT(1)                           DEFAULT 1    COMMENT '是否公开 (1=公开, 0=私有)',
  `use_count`     INT                                  DEFAULT 0    COMMENT '使用次数',
  `created_at`    DATETIME                             DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME                             DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`       INT                                  DEFAULT 0    COMMENT '逻辑删除 (0=正常, 1=已删)',
  PRIMARY KEY (`id`),
  KEY `idx_category`     (`category`),
  KEY `idx_creator`      (`creator_id`),
  KEY `idx_public_count` (`is_public`, `use_count` DESC),
  KEY `idx_created`      (`created_at` DESC),
  KEY `idx_name_unique`  (`name`, `creator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Prompt 模板表 (V4.3)';

-- ============================================================
-- 初始化数据: 5 个内置系统模板 (PromptTemplateService.BUILTIN_TEMPLATES)
-- creator_id=1 = 系统 (admin)
-- ============================================================
INSERT INTO `prompt_template`
  (`name`, `description`, `category`, `content`, `variables`,
   `creator_id`, `creator_name`, `is_public`, `use_count`)
VALUES
('翻译助手',
 '中英日韩法德俄 7 国语言互译, 保留原文格式',
 '翻译',
 '你是一个专业翻译官, 请将以下文本翻译成 {{target_language}}, 保留原文格式、专有名词和语气:\n\n{{text}}',
 '[{"name":"target_language","description":"目标语言","required":true},{"name":"text","description":"原文","required":true}]',
 1, 'admin', 1, 0),

('代码审查',
 '资深工程师视角审查代码, 找出 bug / 性能 / 风格问题',
 '代码',
 '请以资深 {{language}} 工程师的视角, 审查以下代码:\n\n```{{language}}\n{{code}}\n```\n\n输出格式:\n1. 🐛 Bug (必现问题)\n2. ⚠️ 风险 (潜在问题)\n3. 🚀 性能 (可优化点)\n4. 🎨 风格 (可读性)\n5. ✅ 总结 (整体评分 1-10)',
 '[{"name":"language","description":"编程语言","required":true},{"name":"code","description":"代码","required":true}]',
 1, 'admin', 1, 0),

('会议纪要',
 '把会议录音转写或聊天记录整理成结构化纪要',
 '写作',
 '请把以下会议内容整理成结构化纪要:\n\n```\n{{transcript}}\n```\n\n输出格式:\n# 会议主题: <主题>\n# 时间地点: <推断>\n# 与会人员: <提取>\n## 决议事项\n- ...\n## 待办事项\n- [ ] 负责人 / 截止日期 / 任务\n## 讨论要点\n- ...',
 '[{"name":"transcript","description":"会议转写或聊天记录","required":true}]',
 1, 'admin', 1, 0),

('营销文案',
 '根据产品卖点生成小红书/抖音/朋友圈文案',
 '写作',
 '你是 {{platform}} 爆款文案写手, 擅长 {{tone}} 风格.\n请基于以下产品信息写 3 条文案:\n\n产品名: {{product_name}}\n核心卖点: {{selling_points}}\n目标人群: {{target_audience}}\n\n要求:\n- 标题 ≤ 20 字, 吸引点击\n- 正文 100-200 字\n- 末尾加 3-5 个 #标签',
 '[{"name":"platform","description":"平台 (小红书/抖音/朋友圈)","required":true},{"name":"tone","description":"语气 (搞笑/温暖/专业)","required":false},{"name":"product_name","description":"产品名","required":true},{"name":"selling_points","description":"卖点","required":true},{"name":"target_audience","description":"目标人群","required":false}]',
 1, 'admin', 1, 0),

('故障排查',
 '运维/SRE 视角, 根据现象逐步定位根因',
 '分析',
 '你是资深 SRE, 服务器/服务出现以下故障:\n\n现象: {{symptom}}\n环境: {{env}}\n最近变更: {{recent_changes}}\n\n请按以下步骤排查:\n1. 🔍 现象分析 (可能原因 TOP 3)\n2. 🧪 验证步骤 (具体命令)\n3. 🎯 根因推断\n4. 🛠 解决方案 (临时 + 永久)\n5. 📋 预防措施',
 '[{"name":"symptom","description":"故障现象","required":true},{"name":"env","description":"环境 (prod/staging)","required":false},{"name":"recent_changes","description":"最近变更","required":false}]',
 1, 'admin', 1, 0);
