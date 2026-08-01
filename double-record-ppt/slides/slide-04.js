// Overall architecture - 5 layers
function createSlide(pres, theme) {
  const slide = pres.addSlide();
  slide.background = { color: theme.bg };

  // Title
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: 0.4, w: 0.08, h: 0.5,
    fill: { color: theme.accent }, line: { type: "none" }
  });
  slide.addText("整体架构:线上线下双录一体化平台", {
    x: 0.7, y: 0.4, w: 7, h: 0.5,
    fontSize: 22, fontFace: "Microsoft YaHei", color: theme.primary,
    bold: true, align: "left", valign: "middle", margin: 0
  });
  slide.addText("PLATFORM ARCHITECTURE", {
    x: 7.0, y: 0.5, w: 2.5, h: 0.3,
    fontSize: 10, fontFace: "Arial", color: theme.secondary,
    charSpacing: 4, align: "right"
  });
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: 1.0, w: 9, h: 0.02,
    fill: { color: theme.light }, line: { type: "none" }
  });

  // Architecture - 5 horizontal layers
  const layers = [
    { name: "接入层", color: theme.primary, modules: "线上 H5/小程序 | 线下双录一体机 | PAD 移动展业 | 网点 PC 端" },
    { name: "流程编排层", color: "1f3a5f", modules: "统一流程引擎 BPMN | 状态机 | 异常分支 | 回退补偿" },
    { name: "能力中台层", color: "2b5c8a", modules: "话术中心 | 双录引擎 | 智能质检 | 电子签 | 视频合规 | 风评引擎" },
    { name: "数据治理层", color: "3d7eb5", modules: "统一订单中心 | 分布式事务 | 客户主数据 | 影像存证 | 审计追溯" },
    { name: "基础底座层", color: "5a9bd5", modules: "统一身份/权限 | 消息中间件 | 对象存储 OSS | 国密加密 | 监管上报通道" }
  ];

  const layerX = 0.5;
  const layerW = 4.2;
  const layerH = 0.65;
  const startY = 1.2;
  const gap = 0.13;

  layers.forEach((l, i) => {
    const y = startY + i * (layerH + gap);
    // Layer color block (left)
    slide.addShape(pres.shapes.RECTANGLE, {
      x: layerX, y: y, w: 1.5, h: layerH,
      fill: { color: l.color }, line: { type: "none" }, rectRadius: 0.03
    });
    slide.addText(l.name, {
      x: layerX, y: y, w: 1.5, h: layerH,
      fontSize: 14, fontFace: "Microsoft YaHei", color: "FFFFFF",
      bold: true, align: "center", valign: "middle", margin: 0
    });
    // Modules block (right)
    slide.addShape(pres.shapes.RECTANGLE, {
      x: layerX + 1.5, y: y, w: layerW - 1.5, h: layerH,
      fill: { color: theme.light }, line: { type: "none" }, rectRadius: 0.03
    });
    slide.addText(l.modules, {
      x: layerX + 1.6, y: y, w: layerW - 1.6, h: layerH,
      fontSize: 10, fontFace: "Microsoft YaHei", color: theme.primary,
      align: "left", valign: "middle", margin: 0
    });
  });

  // Right column - cross-cutting capabilities
  const crossX = 5.0;
  slide.addShape(pres.shapes.RECTANGLE, {
    x: crossX, y: 1.2, w: 4.5, h: 4.0,
    fill: { color: theme.light }, line: { type: "none" }, rectRadius: 0.05
  });
  slide.addShape(pres.shapes.RECTANGLE, {
    x: crossX, y: 1.2, w: 4.5, h: 0.5,
    fill: { color: theme.accent }, line: { type: "none" }, rectRadius: 0.05
  });
  slide.addText("横切能力(贯穿所有层)", {
    x: crossX + 0.15, y: 1.2, w: 4.2, h: 0.5,
    fontSize: 14, fontFace: "Microsoft YaHei", color: "FFFFFF",
    bold: true, align: "left", valign: "middle", margin: 0
  });

  const crossItems = [
    { t: "统一话术中心", d: "话术/风险点/产品参数/合规要点统一管理" },
    { t: "智能质检引擎", d: "规则 + ASR/NLP/情绪 多模态 AI 质检" },
    { t: "全链路追踪", d: "从预约 → 双录 → 签单 → 归档全链路埋点" },
    { t: "合规审计", d: "监管接口直连 · 全量留痕 · 可回放" },
    { t: "可视化运营", d: "管理者驾驶舱 · 实时大屏 · 异常告警" }
  ];
  crossItems.forEach((c, i) => {
    const y = 1.85 + i * 0.6;
    slide.addShape(pres.shapes.OVAL, {
      x: crossX + 0.2, y: y + 0.05, w: 0.18, h: 0.18,
      fill: { color: theme.accent }, line: { type: "none" }
    });
    slide.addText(c.t, {
      x: crossX + 0.5, y: y, w: 3.9, h: 0.28,
      fontSize: 12, fontFace: "Microsoft YaHei", color: theme.primary,
      bold: true, align: "left", valign: "middle", margin: 0
    });
    slide.addText(c.d, {
      x: crossX + 0.5, y: y + 0.27, w: 3.9, h: 0.25,
      fontSize: 9, fontFace: "Microsoft YaHei", color: theme.secondary,
      align: "left", valign: "middle", margin: 0
    });
  });

  // Bottom callout
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: 5.2, w: 9, h: 0.32,
    fill: { color: theme.accent, transparency: 85 }, line: { type: "none" }, rectRadius: 0.03
  });
  slide.addText("【细节点 2】架构核心是「流程编排 + 能力中台 + 数据治理」三层解耦,任何接入端都共享同一份业务逻辑。", {
    x: 0.6, y: 5.2, w: 8.8, h: 0.32,
    fontSize: 10, fontFace: "Microsoft YaHei", color: theme.primary,
    bold: true, align: "left", valign: "middle", margin: 0
  });

  // Page number
  slide.addText("04 / 16", {
    x: 9.0, y: 5.55, w: 0.9, h: 0.07,
    fontSize: 8, fontFace: "Arial", color: theme.secondary,
    align: "right"
  });
}

module.exports = { createSlide };
