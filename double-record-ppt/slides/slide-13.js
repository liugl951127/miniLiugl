// Customer purchase flow guarantee
function createSlide(pres, theme) {
  const slide = pres.addSlide();
  slide.background = { color: theme.bg };

  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: 0.4, w: 0.08, h: 0.5,
    fill: { color: theme.accent }, line: { type: "none" }
  });
  slide.addText("客户体验保障:让购买流程「走得顺」", {
    x: 0.7, y: 0.4, w: 7.5, h: 0.5,
    fontSize: 20, fontFace: "Microsoft YaHei", color: theme.primary,
    bold: true, align: "left", valign: "middle", margin: 0
  });
  slide.addText("CUSTOMER EXPERIENCE", {
    x: 7.0, y: 0.5, w: 2.5, h: 0.3,
    fontSize: 10, fontFace: "Arial", color: theme.secondary,
    charSpacing: 4, align: "right"
  });

  // Before / After comparison
  const compY = 1.15;
  const compH = 1.8;
  const compW = 4.3;

  // BEFORE
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: compY, w: compW, h: compH,
    fill: { color: theme.light }, line: { type: "none" }, rectRadius: 0.05
  });
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: compY, w: compW, h: 0.45,
    fill: { color: "8d99ae" }, line: { type: "none" }, rectRadius: 0.05
  });
  slide.addText("现状(Before)", {
    x: 0.7, y: compY, w: compW - 0.3, h: 0.45,
    fontSize: 14, fontFace: "Microsoft YaHei", color: "FFFFFF",
    bold: true, align: "left", valign: "middle", margin: 0
  });
  const beforeItems = [
    "双录 30+ 分钟,客户等待焦虑",
    "话术不熟反复卡顿,体验差",
    "线下需多次到店,跑路成本高",
    "出现问题需重新预约,周期长"
  ];
  beforeItems.forEach((b, i) => {
    slide.addText("✗ " + b, {
      x: 0.7, y: compY + 0.6 + i * 0.27, w: compW - 0.3, h: 0.25,
      fontSize: 11, fontFace: "Microsoft YaHei", color: theme.primary,
      align: "left", valign: "middle", margin: 0
    });
  });

  // AFTER
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 5.2, y: compY, w: compW, h: compH,
    fill: { color: theme.light }, line: { type: "none" }, rectRadius: 0.05
  });
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 5.2, y: compY, w: compW, h: 0.45,
    fill: { color: theme.accent }, line: { type: "none" }, rectRadius: 0.05
  });
  slide.addText("方案后(After)", {
    x: 5.4, y: compY, w: compW - 0.3, h: 0.45,
    fontSize: 14, fontFace: "Microsoft YaHei", color: "FFFFFF",
    bold: true, align: "left", valign: "middle", margin: 0
  });
  const afterItems = [
    "线上 5-8 分钟,线下 15 分钟",
    "智能话术引导,逐项确认无卡顿",
    "PAD 上门,足不出户办业务",
    "中途中断可续接,不重来"
  ];
  afterItems.forEach((a, i) => {
    slide.addText("✓ " + a, {
      x: 5.4, y: compY + 0.6 + i * 0.27, w: compW - 0.3, h: 0.25,
      fontSize: 11, fontFace: "Microsoft YaHei", color: theme.primary,
      align: "left", valign: "middle", margin: 0
    });
  });

  // Key improvement metrics
  const mY = 3.2;
  slide.addText("关键体验指标", {
    x: 0.5, y: mY, w: 9, h: 0.3,
    fontSize: 14, fontFace: "Microsoft YaHei", color: theme.primary,
    bold: true, align: "left", margin: 0
  });

  const metrics = [
    { num: "70%", t: "双录时长下降", d: "从 30+ min 降至 8-15 min" },
    { num: "95%", t: "一次性通过率", d: "AI 预检 + 引导话术" },
    { num: "3x", t: "渠道覆盖提升", d: "线上 + 高柜 + PAD 移动" },
    { num: "0", t: "客户卡顿投诉", d: "断点续传 + 远程兜底" }
  ];

  const mW = 2.1;
  const mGap = 0.15;
  metrics.forEach((m, i) => {
    const x = 0.5 + i * (mW + mGap);
    const y = mY + 0.4;
    slide.addShape(pres.shapes.RECTANGLE, {
      x: x, y: y, w: mW, h: 1.5,
      fill: { color: theme.light }, line: { type: "none" }, rectRadius: 0.05
    });
    slide.addShape(pres.shapes.RECTANGLE, {
      x: x, y: y, w: mW, h: 0.08,
      fill: { color: theme.accent }, line: { type: "none" }
    });
    slide.addText(m.num, {
      x: x, y: y + 0.2, w: mW, h: 0.6,
      fontSize: 40, fontFace: "Arial", color: theme.accent,
      bold: true, align: "center", valign: "middle", margin: 0
    });
    slide.addText(m.t, {
      x: x, y: y + 0.85, w: mW, h: 0.3,
      fontSize: 12, fontFace: "Microsoft YaHei", color: theme.primary,
      bold: true, align: "center", valign: "middle", margin: 0
    });
    slide.addText(m.d, {
      x: x, y: y + 1.15, w: mW, h: 0.3,
      fontSize: 8, fontFace: "Microsoft YaHei", color: theme.secondary,
      align: "center", valign: "middle", margin: 0
    });
  });

  // Bottom callout
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: 5.2, w: 9, h: 0.32,
    fill: { color: theme.accent, transparency: 85 }, line: { type: "none" }, rectRadius: 0.03
  });
  slide.addText("【细节点 11】体验设计的红线:合规要求是「下限」,客户感知是「上限」,两者必须同时满足。", {
    x: 0.6, y: 5.2, w: 8.8, h: 0.32,
    fontSize: 10, fontFace: "Microsoft YaHei", color: theme.primary,
    bold: true, align: "left", valign: "middle", margin: 0
  });

  slide.addText("13 / 16", {
    x: 9.0, y: 5.55, w: 0.9, h: 0.07,
    fontSize: 8, fontFace: "Arial", color: theme.secondary,
    align: "right"
  });
}

module.exports = { createSlide };
