// Pain Point 3: Quality inspection unification
function createSlide(pres, theme) {
  const slide = pres.addSlide();
  slide.background = { color: theme.bg };

  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: 0.4, w: 0.08, h: 0.5,
    fill: { color: theme.accent }, line: { type: "none" }
  });
  slide.addText("方案三:智能质检引擎 - 解决「质检一致性」痛点", {
    x: 0.7, y: 0.4, w: 7.5, h: 0.5,
    fontSize: 20, fontFace: "Microsoft YaHei", color: theme.primary,
    bold: true, align: "left", valign: "middle", margin: 0
  });
  slide.addText("PAIN POINT 03/5", {
    x: 7.0, y: 0.5, w: 2.5, h: 0.3,
    fontSize: 10, fontFace: "Arial", color: theme.secondary,
    charSpacing: 3, align: "right"
  });

  // Pain point badge
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: 1.05, w: 9, h: 0.4,
    fill: { color: theme.accent, transparency: 88 }, line: { type: "none" }, rectRadius: 0.03
  });
  slide.addText("【痛点 3】线上 AI 质检、线下人工抽检,两套标准两套结果,无法闭环。", {
    x: 0.6, y: 1.05, w: 8.8, h: 0.4,
    fontSize: 11, fontFace: "Microsoft YaHei", color: theme.accent,
    bold: true, align: "left", valign: "middle", margin: 0
  });

  // Left side: Three-layer detection pipeline
  const leftX = 0.5;
  const leftW = 4.3;
  slide.addText("三层质检流水线", {
    x: leftX, y: 1.6, w: leftW, h: 0.3,
    fontSize: 14, fontFace: "Microsoft YaHei", color: theme.primary,
    bold: true, align: "left", margin: 0
  });

  const layers = [
    { t: "L1 规则层", d: "100+ 规则模板:必读项、必答项、必签字、风险点关键词", c: "0.5s 内" },
    { t: "L2 AI 智能层", d: "ASR 转写 + NLP 意图识别 + 情感分析 + 图像识别", c: "30s 内" },
    { t: "L3 人工复核层", d: "高风险单 100% 复检,中风险按比例,低风险 AI 自闭环", c: "T+1" }
  ];

  layers.forEach((l, i) => {
    const y = 2.0 + i * 0.95;
    slide.addShape(pres.shapes.RECTANGLE, {
      x: leftX, y: y, w: leftW, h: 0.85,
      fill: { color: theme.light }, line: { type: "none" }, rectRadius: 0.05
    });
    slide.addShape(pres.shapes.RECTANGLE, {
      x: leftX, y: y, w: 0.08, h: 0.85,
      fill: { color: theme.accent }, line: { type: "none" }
    });
    slide.addText(l.t, {
      x: leftX + 0.2, y: y + 0.08, w: 2, h: 0.3,
      fontSize: 13, fontFace: "Microsoft YaHei", color: theme.primary,
      bold: true, align: "left", margin: 0
    });
    // Time badge
    slide.addShape(pres.shapes.RECTANGLE, {
      x: leftX + leftW - 0.9, y: y + 0.1, w: 0.8, h: 0.26,
      fill: { color: theme.primary }, line: { type: "none" }, rectRadius: 0.03
    });
    slide.addText(l.c, {
      x: leftX + leftW - 0.9, y: y + 0.1, w: 0.8, h: 0.26,
      fontSize: 9, fontFace: "Microsoft YaHei", color: "FFFFFF",
      bold: true, align: "center", valign: "middle", margin: 0
    });
    slide.addText(l.d, {
      x: leftX + 0.2, y: y + 0.4, w: leftW - 0.3, h: 0.4,
      fontSize: 10, fontFace: "Microsoft YaHei", color: theme.secondary,
      align: "left", valign: "top", margin: 0
    });
  });

  // Right side: Unified scorecard
  const rightX = 5.1;
  const rightW = 4.4;
  slide.addText("统一质检评分卡(线上/线下同模板)", {
    x: rightX, y: 1.6, w: rightW, h: 0.3,
    fontSize: 14, fontFace: "Microsoft YaHei", color: theme.primary,
    bold: true, align: "left", margin: 0
  });

  // Score dimensions
  const dims = [
    { name: "话术完整度", score: 30, weight: "30%" },
    { name: "风险揭示准确度", score: 25, weight: "25%" },
    { name: "客户确认清晰度", score: 20, weight: "20%" },
    { name: "音视频合规度", score: 15, weight: "15%" },
    { name: "流程完整度", score: 10, weight: "10%" }
  ];

  dims.forEach((d, i) => {
    const y = 2.0 + i * 0.45;
    slide.addText(d.name, {
      x: rightX, y: y, w: 1.8, h: 0.3,
      fontSize: 10, fontFace: "Microsoft YaHei", color: theme.primary,
      bold: true, align: "left", valign: "middle", margin: 0
    });
    // Progress bar bg
    slide.addShape(pres.shapes.RECTANGLE, {
      x: rightX + 1.85, y: y + 0.1, w: 1.8, h: 0.1,
      fill: { color: theme.light }, line: { type: "none" }, rectRadius: 0.05
    });
    // Progress bar fill
    const fillW = 1.8 * (d.score / 30);
    slide.addShape(pres.shapes.RECTANGLE, {
      x: rightX + 1.85, y: y + 0.1, w: fillW, h: 0.1,
      fill: { color: theme.accent }, line: { type: "none" }, rectRadius: 0.05
    });
    slide.addText(d.weight, {
      x: rightX + 3.7, y: y, w: 0.7, h: 0.3,
      fontSize: 11, fontFace: "Arial", color: theme.primary,
      bold: true, align: "right", valign: "middle", margin: 0
    });
  });

  // Verdict levels
  const vy = 4.3;
  slide.addShape(pres.shapes.RECTANGLE, {
    x: rightX, y: vy, w: rightW, h: 0.8,
    fill: { color: theme.primary }, line: { type: "none" }, rectRadius: 0.05
  });
  const verdicts = [
    { c: "FFB703", t: "高分(>=90)", d: "自动归档" },
    { c: theme.accent, t: "中分(70-89)", d: "AI 标记 + 抽检" },
    { c: "8d99ae", t: "低分(<70)", d: "人工 100% 复检" }
  ];
  verdicts.forEach((v, i) => {
    const x = rightX + 0.1 + i * 1.45;
    slide.addShape(pres.shapes.OVAL, {
      x: x, y: vy + 0.1, w: 0.2, h: 0.2,
      fill: { color: v.c }, line: { type: "none" }
    });
    slide.addText(v.t, {
      x: x + 0.25, y: vy + 0.05, w: 1.2, h: 0.25,
      fontSize: 10, fontFace: "Microsoft YaHei", color: "FFFFFF",
      bold: true, align: "left", valign: "middle", margin: 0
    });
    slide.addText(v.d, {
      x: x, y: vy + 0.4, w: 1.4, h: 0.3,
      fontSize: 9, fontFace: "Microsoft YaHei", color: "FFFFFF",
      align: "left", valign: "top", margin: 0
    });
  });

  // Bottom callout
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: 5.2, w: 9, h: 0.32,
    fill: { color: theme.accent, transparency: 85 }, line: { type: "none" }, rectRadius: 0.03
  });
  slide.addText("【细节点 5】关键设计:线上/线下质检「同一份规则 + 同一套模型 + 同一张评分卡」,结果可比、可对账。", {
    x: 0.6, y: 5.2, w: 8.8, h: 0.32,
    fontSize: 10, fontFace: "Microsoft YaHei", color: theme.primary,
    bold: true, align: "left", valign: "middle", margin: 0
  });

  slide.addText("07 / 16", {
    x: 9.0, y: 5.55, w: 0.9, h: 0.07,
    fontSize: 8, fontFace: "Arial", color: theme.secondary,
    align: "right"
  });
}

module.exports = { createSlide };
