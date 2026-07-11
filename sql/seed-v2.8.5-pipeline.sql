-- ============================================================
-- V2.8.5 Pipeline 流水线种子数据
-- 表: ai_intent_keyword, pipeline_log
-- ============================================================

-- --------------------------------------------------------
-- 1. ai_intent_keyword (意图关键词)
-- --------------------------------------------------------

INSERT INTO ai_intent_keyword (intent, keyword, weight, is_regex, enabled, language, remark, created_at, updated_at) VALUES
-- GENERATE_CHART
('GENERATE_CHART', '图表', 2, 0, 1, 'zh', '通用图表', NOW(), NOW()),
('GENERATE_CHART', '柱状图', 3, 0, 1, 'zh', '柱状', NOW(), NOW()),
('GENERATE_CHART', '折线图', 3, 0, 1, 'zh', '折线', NOW(), NOW()),
('GENERATE_CHART', '饼图', 3, 0, 1, 'zh', '饼', NOW(), NOW()),
('GENERATE_CHART', '雷达图', 3, 0, 1, 'zh', '雷达', NOW(), NOW()),
('GENERATE_CHART', '热力图', 3, 0, 1, 'zh', '热力', NOW(), NOW()),
('GENERATE_CHART', '散点图', 3, 0, 1, 'zh', '散点', NOW(), NOW()),
('GENERATE_CHART', '桑基图', 3, 0, 1, 'zh', '桑基', NOW(), NOW()),
('GENERATE_CHART', 'chart', 2, 0, 1, 'en', 'English', NOW(), NOW()),
('GENERATE_CHART', 'graph', 2, 0, 1, 'en', 'English', NOW(), NOW()),
('GENERATE_CHART', 'plot', 2, 0, 1, 'en', 'English', NOW(), NOW()),
('GENERATE_CHART', '生成.*?(柱状图|折线图|饼图|雷达图|热力图|散点图|桑基图)', 5, 1, 1, 'zh', '正则', NOW(), NOW()),

-- GENERATE_MUSIC
('GENERATE_MUSIC', '音乐', 2, 0, 1, 'zh', '音乐', NOW(), NOW()),
('GENERATE_MUSIC', '旋律', 2, 0, 1, 'zh', '旋律', NOW(), NOW()),
('GENERATE_MUSIC', '曲子', 2, 0, 1, 'zh', '曲子', NOW(), NOW()),
('GENERATE_MUSIC', 'MIDI', 2, 0, 1, 'zh', 'MIDI', NOW(), NOW()),
('GENERATE_MUSIC', '作曲', 2, 0, 1, 'zh', '作曲', NOW(), NOW()),
('GENERATE_MUSIC', 'music', 2, 0, 1, 'en', 'English', NOW(), NOW()),
('GENERATE_MUSIC', 'melody', 2, 0, 1, 'en', 'English', NOW(), NOW()),
('GENERATE_MUSIC', 'song', 2, 0, 1, 'en', 'English', NOW(), NOW()),
('GENERATE_MUSIC', '(生成|创作|作).*?(音乐|旋律|曲子|歌)', 5, 1, 1, 'zh', '正则', NOW(), NOW()),

-- GENERATE_ANIMATION
('GENERATE_ANIMATION', '动画', 2, 0, 1, 'zh', '动画', NOW(), NOW()),
('GENERATE_ANIMATION', 'GIF', 2, 0, 1, 'zh', 'GIF', NOW(), NOW()),
('GENERATE_ANIMATION', '动图', 2, 0, 1, 'zh', '动图', NOW(), NOW()),
('GENERATE_ANIMATION', 'animation', 2, 0, 1, 'en', 'English', NOW(), NOW()),

-- QUERY_DATA
('QUERY_DATA', '查询', 2, 0, 1, 'zh', '查询', NOW(), NOW()),
('QUERY_DATA', 'SELECT', 3, 0, 1, 'en', 'SQL', NOW(), NOW()),
('QUERY_DATA', 'FROM', 1, 0, 1, 'en', 'SQL', NOW(), NOW()),
('QUERY_DATA', '数据', 1, 0, 1, 'zh', '数据', NOW(), NOW()),
('QUERY_DATA', '记录', 1, 0, 1, 'zh', '记录', NOW(), NOW()),
('QUERY_DATA', 'query', 2, 0, 1, 'en', 'English', NOW(), NOW()),
('QUERY_DATA', '(查询|统计|列出|显示).*?(前\\s*\\d+|最近|所有).*?(条|个|记录)', 5, 1, 1, 'zh', '正则', NOW(), NOW()),

-- ANALYZE_DATA
('ANALYZE_DATA', '统计', 2, 0, 1, 'zh', '统计', NOW(), NOW()),
('ANALYZE_DATA', '平均', 2, 0, 1, 'zh', '平均', NOW(), NOW()),
('ANALYZE_DATA', '求和', 2, 0, 1, 'zh', '求和', NOW(), NOW()),
('ANALYZE_DATA', '最大值', 2, 0, 1, 'zh', '最大', NOW(), NOW()),
('ANALYZE_DATA', '最小值', 2, 0, 1, 'zh', '最小', NOW(), NOW()),
('ANALYZE_DATA', '分组', 2, 0, 1, 'zh', '分组', NOW(), NOW()),
('ANALYZE_DATA', '聚合', 2, 0, 1, 'zh', '聚合', NOW(), NOW()),
('ANALYZE_DATA', '趋势', 2, 0, 1, 'zh', '趋势', NOW(), NOW()),
('ANALYZE_DATA', '异常', 2, 0, 1, 'zh', '异常', NOW(), NOW()),
('ANALYZE_DATA', 'analyze', 2, 0, 1, 'en', 'English', NOW(), NOW()),
('ANALYZE_DATA', 'statistics', 2, 0, 1, 'en', 'English', NOW(), NOW()),

-- GENERATE_CODE
('GENERATE_CODE', '代码', 2, 0, 1, 'zh', '代码', NOW(), NOW()),
('GENERATE_CODE', 'Spring Boot', 3, 0, 1, 'zh', 'Spring Boot', NOW(), NOW()),
('GENERATE_CODE', 'Vue', 2, 0, 1, 'zh', 'Vue', NOW(), NOW()),
('GENERATE_CODE', 'React', 2, 0, 1, 'zh', 'React', NOW(), NOW()),
('GENERATE_CODE', 'Python', 2, 0, 1, 'zh', 'Python', NOW(), NOW()),
('GENERATE_CODE', 'Flask', 2, 0, 1, 'zh', 'Flask', NOW(), NOW()),
('GENERATE_CODE', '项目', 1, 0, 1, 'zh', '项目', NOW(), NOW()),
('GENERATE_CODE', '(生成|创建).*?(Spring Boot|Vue|React|Flask|Express).*?项目', 5, 1, 1, 'zh', '正则', NOW(), NOW()),

-- CHAT
('CHAT', '你好', 2, 0, 1, 'zh', '问候', NOW(), NOW()),
('CHAT', '请问', 1, 0, 1, 'zh', '请问', NOW(), NOW()),
('CHAT', '什么是', 1, 0, 1, 'zh', '询问', NOW(), NOW()),
('CHAT', '怎么', 1, 0, 1, 'zh', '询问', NOW(), NOW()),
('CHAT', '如何', 1, 0, 1, 'zh', '询问', NOW(), NOW()),
('CHAT', '为什么', 1, 0, 1, 'zh', '询问', NOW(), NOW()),
('CHAT', '介绍', 1, 0, 1, 'zh', '介绍', NOW(), NOW()),
('CHAT', 'hello', 2, 0, 1, 'en', 'English', NOW(), NOW()),
('CHAT', 'hi', 1, 0, 1, 'en', 'English', NOW(), NOW()),
('CHAT', 'help', 1, 0, 1, 'en', 'English', NOW(), NOW()),

-- TRANSFER_HUMAN
('TRANSFER_HUMAN', '转人工', 5, 0, 1, 'zh', '转人工', NOW(), NOW()),
('TRANSFER_HUMAN', '真人', 3, 0, 1, 'zh', '真人', NOW(), NOW()),
('TRANSFER_HUMAN', '人工客服', 5, 0, 1, 'zh', '人工', NOW(), NOW()),
('TRANSFER_HUMAN', '坐席', 2, 0, 1, 'zh', '坐席', NOW(), NOW()),
('TRANSFER_HUMAN', 'transfer', 3, 0, 1, 'en', 'English', NOW(), NOW()),
('TRANSFER_HUMAN', 'human', 2, 0, 1, 'en', 'English', NOW(), NOW()),
('TRANSFER_HUMAN', '(转|找|叫|来)\\s*(人工|真人|客服)', 5, 1, 1, 'zh', '正则', NOW(), NOW()),

-- TTS
('TTS', '语音合成', 3, 0, 1, 'zh', 'TTS', NOW(), NOW()),
('TTS', '朗读', 2, 0, 1, 'zh', '朗读', NOW(), NOW()),
('TTS', '读出来', 2, 0, 1, 'zh', '读出来', NOW(), NOW()),
('TTS', 'TTS', 2, 0, 1, 'en', 'TTS', NOW(), NOW()),

-- STT
('STT', '语音识别', 3, 0, 1, 'zh', 'STT', NOW(), NOW()),
('STT', '转文字', 3, 0, 1, 'zh', '转文字', NOW(), NOW()),
('STT', 'STT', 2, 0, 1, 'en', 'STT', NOW(), NOW()),

-- IMAGE_ANALYZE
('IMAGE_ANALYZE', '分析图片', 3, 0, 1, 'zh', '图片', NOW(), NOW()),
('IMAGE_ANALYZE', '看图', 2, 0, 1, 'zh', '看图', NOW(), NOW()),
('IMAGE_ANALYZE', '识别图片', 3, 0, 1, 'zh', '识别', NOW(), NOW()),
('IMAGE_ANALYZE', 'analyze image', 3, 0, 1, 'en', 'English', NOW(), NOW()),

-- VIDEO_ANALYZE
('VIDEO_ANALYZE', '分析视频', 3, 0, 1, 'zh', '视频', NOW(), NOW()),
('VIDEO_ANALYZE', '看视频', 2, 0, 1, 'zh', '看视频', NOW(), NOW()),
('VIDEO_ANALYZE', 'analyze video', 3, 0, 1, 'en', 'English', NOW(), NOW()),

-- AUDIO_ANALYZE
('AUDIO_ANALYZE', '分析音频', 3, 0, 1, 'zh', '音频', NOW(), NOW()),
('AUDIO_ANALYZE', '听声音', 2, 0, 1, 'zh', '听', NOW(), NOW()),
('AUDIO_ANALYZE', 'analyze audio', 3, 0, 1, 'en', 'English', NOW(), NOW());

-- --------------------------------------------------------
-- 2. pipeline_log (默认无种子, 由执行时写入)
-- --------------------------------------------------------
