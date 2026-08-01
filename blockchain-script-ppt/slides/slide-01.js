// Cover - Project title
function createSlide(pres, theme) {
  const slide = pres.addSlide();
  slide.background = { color: theme.bg };

  // Left accent bar
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0, y: 0, w: 0.35, h: 5.625,
    fill: { color: theme.accent }, line: { type: "none" }
  });

  // Right dark block
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 7.0, y: 0, w: 3.0, h: 5.625,
    fill: { color: theme.primary }, line: { type: "none" }
  });

  // Decorative pattern on right
  for (let i = 0; i < 4; i++) {
    slide.addShape(pres.shapes.RECTANGLE, {
      x: 7.2, y: 0.6 + i * 0.4, w: 2.6, h: 0.02,
      fill: { color: theme.accent, transparency: 60 }, line: { type: "none" }
    });
  }

  // Right side text - theme tags
  slide.addText("BLOCKCHAIN + SCRIPT", {
    x: 7.3, y: 2.2, w: 2.5, h: 0.4,
    fontSize: 11, fontFace: "Arial", color: "FFFFFF",
    charSpacing: 4, align: "left", bold: true
  });
  slide.addText("UNIFIED  EXPERIENCE", {
    x: 7.3, y: 2.5, w: 2.5, h: 0.4,
    fontSize: 11, fontFace: "Arial", color: theme.accent,
    charSpacing: 4, align: "left", bold: true
  });

  // Main title
  slide.addText("区块链 + 话术统一", {
    x: 0.7, y: 1.5, w: 6.2, h: 0.8,
    fontSize: 36, fontFace: "Microsoft YaHei", color: theme.primary,
    bold: true, align: "left", valign: "middle"
  });
  slide.addText("技术架构方案", {
    x: 0.7, y: 2.3, w: 6.2, h: 0.8,
    fontSize: 36, fontFace: "Microsoft YaHei", color: theme.primary,
    bold: true, align: "left", valign: "middle"
  });

  // Subtitle
  slide.addText("数据一致 + 体验一致 · 区块链存证 + 智能播报", {
    x: 0.7, y: 3.2, w: 6.2, h: 0.4,
    fontSize: 16, fontFace: "Microsoft YaHei", color: theme.secondary,
    align: "left"
  });

  // Divider
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.7, y: 3.7, w: 1.0, h: 0.04,
    fill: { color: theme.accent }, line: { type: "none" }
  });

  // Description
  slide.addText("双录一体化核心技术方案 · 适用于技术评审和架构汇报", {
    x: 0.7, y: 3.9, w: 6.2, h: 0.3,
    fontSize: 12, fontFace: "Microsoft YaHei", color: theme.secondary,
    align: "left"
  });

  // Meta info
  slide.addText("汇报人:[姓名]    |    汇报日期:2026 年 8 月", {
    x: 0.7, y: 4.9, w: 6.2, h: 0.3,
    fontSize: 11, fontFace: "Microsoft YaHei", color: theme.secondary,
    align: "left"
  });

  // Top label badge
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.7, y: 0.7, w: 1.8, h: 0.32,
    fill: { color: theme.primary }, line: { type: "none" }
  });
  slide.addText("核心技术方案 V1.0", {
    x: 0.7, y: 0.7, w: 1.8, h: 0.32,
    fontSize: 11, fontFace: "Microsoft YaHei", color: "FFFFFF",
    align: "center", valign: "middle", margin: 0
  });
}

module.exports = { createSlide };
