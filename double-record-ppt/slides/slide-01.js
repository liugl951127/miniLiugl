// Cover - Project title and intro
function createSlide(pres, theme) {
  const slide = pres.addSlide();
  slide.background = { color: theme.bg };

  // Left accent block
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0, y: 0, w: 0.35, h: 5.625,
    fill: { color: theme.accent }, line: { type: "none" }
  });

  // Decorative number block (right side)
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 7.0, y: 0, w: 3.0, h: 5.625,
    fill: { color: theme.primary }, line: { type: "none" }
  });

  // Decorative pattern: thin lines on the dark block
  for (let i = 0; i < 4; i++) {
    slide.addShape(pres.shapes.RECTANGLE, {
      x: 7.2, y: 0.6 + i * 0.4, w: 2.6, h: 0.02,
      fill: { color: theme.accent, transparency: 60 }, line: { type: "none" }
    });
  }

  // Vertical "DUAL RECORD" label on right block
  slide.addText("DUAL RECORD INTEGRATION", {
    x: 7.3, y: 2.4, w: 2.5, h: 0.5,
    fontSize: 11, fontFace: "Arial", color: "FFFFFF",
    charSpacing: 4, align: "left"
  });

  // Main title (Chinese)
  slide.addText("线上线下双录一体化", {
    x: 0.7, y: 1.5, w: 6.2, h: 1.0,
    fontSize: 40, fontFace: "Microsoft YaHei", color: theme.primary,
    bold: true, align: "left", valign: "middle"
  });

  // Subtitle
  slide.addText("合规 · 互通 · 质检统一 · 流程一致", {
    x: 0.7, y: 2.6, w: 6.2, h: 0.6,
    fontSize: 20, fontFace: "Microsoft YaHei", color: theme.secondary,
    align: "left"
  });

  // Divider line
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.7, y: 3.4, w: 1.0, h: 0.04,
    fill: { color: theme.accent }, line: { type: "none" }
  });

  // Description text
  slide.addText("痛点驱动的端到端方案设计 · 适用于领导汇报与业务知识掌控", {
    x: 0.7, y: 3.6, w: 6.2, h: 0.4,
    fontSize: 13, fontFace: "Microsoft YaHei", color: theme.secondary,
    align: "left"
  });

  // Meta info bottom
  slide.addText("汇报人:[姓名]    |    汇报日期:2026 年", {
    x: 0.7, y: 4.9, w: 6.2, h: 0.3,
    fontSize: 11, fontFace: "Microsoft YaHei", color: theme.secondary,
    align: "left"
  });

  // Top label badge
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.7, y: 0.7, w: 1.5, h: 0.32,
    fill: { color: theme.primary }, line: { type: "none" }
  });
  slide.addText("合规升级方案", {
    x: 0.7, y: 0.7, w: 1.5, h: 0.32,
    fontSize: 11, fontFace: "Microsoft YaHei", color: "FFFFFF",
    align: "center", valign: "middle", margin: 0
  });
}

module.exports = { createSlide };
