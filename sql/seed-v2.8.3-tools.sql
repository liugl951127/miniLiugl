-- ============================================================
-- V2.8.3 AI 工具种子 (新增 8 个)
-- ============================================================

INSERT INTO `ai_tool` (`code`, `name`, `description`, `category`, `enabled`, `version`, `inputSchema`, `createdAt`, `updatedAt`)
VALUES
-- V2.8.3 新增
('text.analyze',           '文本分析',         '摘要/情感/实体/关键词',           'text',         1, '1.0.0', '{"type":"object","properties":{"text":{"type":"string"},"task":{"type":"string","enum":["summary","sentiment","entities","keywords","all"]},"topK":{"type":"number"},"maxSentences":{"type":"number"}}}', NOW(), NOW()),
('vision.analyze',         '视觉分析',         '颜色/风格/相似度',                'vision',       1, '1.0.0', '{"type":"object","properties":{"imageBase64":{"type":"string"},"task":{"type":"string","enum":["analyze","compare"]},"imageA":{"type":"string"},"imageB":{"type":"string"}}}', NOW(), NOW()),
('audio.analyze',          '音频分析',         '音量/频谱/情绪',                  'audio',        1, '1.0.0', '{"type":"object","properties":{"audioBase64":{"type":"string"}}}', NOW(), NOW()),
('file.convert',           '文件转换',         'JSON/YAML/CSV/Base64 互转',        'file',         1, '1.0.0', '{"type":"object","properties":{"op":{"type":"string","enum":["text2csv","csv2text","json2yaml","yaml2json","json2csv","base642text","text2base64","format","zip2list","merge"]},"text":{"type":"string"}}}', NOW(), NOW()),
('data.analyze.correlation','相关性分析',       'Pearson / Spearman',              'data.analyze', 1, '1.0.0', '{"type":"object","properties":{"dataSourceId":{"type":"number"},"table":{"type":"string"},"columns":{"type":"array"},"method":{"type":"string","enum":["pearson","spearman"]}}}', NOW(), NOW()),
('data.predict.linear',    '线性预测',         '线性回归 + 移动平均 + 指数平滑',  'data.analyze', 1, '1.0.0', '{"type":"object","properties":{"dataSourceId":{"type":"number"},"table":{"type":"string"},"column":{"type":"string"},"method":{"type":"string","enum":["linear","ma3","ma5","ma7","exp"]},"periods":{"type":"number"},"values":{"type":"array"}}}', NOW(), NOW()),
('time.convert',           '时间工具',         '格式转换/计算/时区',              'time',         1, '1.0.0', '{"type":"object","properties":{"op":{"type":"string","enum":["now","parse","format","add","diff","convert","formats","zones"]},"text":{"type":"string"},"epochMillis":{"type":"number"},"timezone":{"type":"string"}}}', NOW(), NOW()),
('image.generate',         'AIGC 图片生成',    '7 种类型程序化图像',                'image',        1, '1.0.0', '{"type":"object","properties":{"prompt":{"type":"string"},"type":{"type":"string"},"width":{"type":"number"},"height":{"type":"number"},"seed":{"type":"number"}}}', NOW(), NOW()),
('chart.generate',         'AI 图表',          '7 种图表',                          'chart',        1, '1.0.0', '{"type":"object","properties":{"type":{"type":"string"},"title":{"type":"string"},"series":{"type":"array"}}}', NOW(), NOW()),
('music.generate',         'MIDI 音乐',        '6 风格 7 调式',                    'music',        1, '1.0.0', '{"type":"object","properties":{"style":{"type":"string"},"key":{"type":"string"},"bpm":{"type":"number"},"bars":{"type":"number"}}}', NOW(), NOW())
ON DUPLICATE KEY UPDATE name=VALUES(name), description=VALUES(description), version='1.0.0';

-- 工具调用统计示例
INSERT INTO `ai_tool_invocation` (`toolCode`, `input`, `output`, `costMs`, `success`, `userId`, `createdAt`)
VALUES
('chart.generate', '{"type":"BAR","title":"示例"}', '{"sizeBytes":12345}', 250, 1, 1, NOW()),
('music.generate', '{"style":"POP","key":"C","bars":8}', '{"sizeBytes":1024}', 320, 1, 1, NOW()),
('text.analyze',   '{"text":"MiniMax 是企业级 AI 平台, 性能优秀, 体验好!","task":"all"}', '{"summary":"...","sentiment":{"label":"POSITIVE"}}', 45, 1, 1, NOW()),
('data.predict.linear', '{"values":[1,2,3,4,5,6,7,8]}', '{"slope":1.0,"rSquared":1.0,"forecast":[{"period":1,"value":9}]}', 5, 1, 1, NOW())
ON DUPLICATE KEY UPDATE costMs=VALUES(costMs);

SELECT 'V2.8.3 AI 工具种子导入完成' AS status;
SELECT COUNT(*) AS total_tools FROM ai_tool;
SELECT category, COUNT(*) AS count FROM ai_tool GROUP BY category ORDER BY count DESC;
