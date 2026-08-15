// Slide LLM-01: Cover
function createSlide(pres, theme) {
  const slide = pres.addSlide();
  slide.addShape(pres.ShapeType.rect, {
    x: 0, y: 0, w: 10, h: 5.625,
    fill: { color: theme.primary }
  });
  // Grid pattern (decorative)
  for (let i = 0; i < 20; i++) {
    slide.addShape(pres.ShapeType.line, {
      x: i * 0.55, y: 0, w: 0, h: 5.625,
      line: { color: 'FFFFFF', width: 0.3, transparency: 85 }
    });
  }
  for (let i = 0; i < 12; i++) {
    slide.addShape(pres.ShapeType.line, {
      x: 0, y: i * 0.5, w: 10, h: 0,
      line: { color: 'FFFFFF', width: 0.3, transparency: 85 }
    });
  }

  // Accent circle
  slide.addShape(pres.ShapeType.ellipse, {
    x: 7.5, y: 3.5, w: 3.5, h: 3.5,
    fill: { color: theme.accent, transparency: 70 }
  });
  slide.addShape(pres.ShapeType.ellipse, {
    x: 8.2, y: 4.0, w: 2.2, h: 2.2,
    fill: { color: theme.accent, transparency: 50 }
  });

  // Text
  slide.addText("大模型技术详解", {
    x: 0.6, y: 1.0, w: 7, h: 0.9,
    fontSize: 40, fontFace: "Microsoft YaHei",
    color: 'FFFFFF', bold: true
  });
  slide.addText("LLM 原理 · 训练流程 · RAG · Agent · 算法详解", {
    x: 0.6, y: 1.95, w: 7, h: 0.5,
    fontSize: 16, fontFace: "Microsoft YaHei",
    color: theme.light
  });
  slide.addShape(pres.ShapeType.rect, {
    x: 0.6, y: 2.6, w: 3, h: 0.04,
    fill: { color: theme.accent }
  });
  slide.addText("MiniMax Platform V6.8", {
    x: 0.6, y: 2.8, w: 5, h: 0.35,
    fontSize: 13, fontFace: "Arial",
    color: theme.light
  });
  slide.addText("原理 · 架构 · 算法 · 平台实现", {
    x: 0.6, y: 3.15, w: 5, h: 0.35,
    fontSize: 12, fontFace: "Microsoft YaHei",
    color: theme.light, transparency: 40
  });

  slide.addText("01", {
    x: 9.0, y: 5.0, w: 0.7, h: 0.35,
    fontSize: 11, fontFace: "Arial",
    color: theme.light, align: 'center'
  });
}
module.exports = { createSlide };
