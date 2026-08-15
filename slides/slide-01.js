// Slide 01 - Cover Page
function createSlide(pres, theme) {
  const slide = pres.addSlide();

  // Dark gradient background
  slide.addShape(pres.ShapeType.rect, {
    x: 0, y: 0, w: 10, h: 5.625,
    fill: { color: theme.primary }
  });

  // Accent diagonal stripe
  slide.addShape(pres.ShapeType.rect, {
    x: 0, y: 4.2, w: 10, h: 0.06,
    fill: { color: theme.accent }
  });

  // Brand label
  slide.addText("MiniMax Platform", {
    x: 0.6, y: 0.5, w: 8.8, h: 0.45,
    fontSize: 13, fontFace: "Arial",
    color: theme.light, bold: false,
    charSpacing: 3
  });

  // Main title
  slide.addText("企业级大模型平台", {
    x: 0.6, y: 1.3, w: 8.8, h: 1.2,
    fontSize: 48, fontFace: "Microsoft YaHei",
    color: "FFFFFF", bold: true
  });

  // Subtitle
  slide.addText("项目功能概览 · 模块关联 · 执行流程", {
    x: 0.6, y: 2.55, w: 8.8, h: 0.6,
    fontSize: 20, fontFace: "Microsoft YaHei",
    color: theme.light
  });

  // Version badge
  slide.addShape(pres.ShapeType.roundRect, {
    x: 0.6, y: 3.4, w: 1.6, h: 0.42,
    fill: { color: theme.accent },
    rectRadius: 0.08
  });
  slide.addText("V6.8.10", {
    x: 0.6, y: 3.4, w: 1.6, h: 0.42,
    fontSize: 14, fontFace: "Arial",
    color: "FFFFFF", bold: true,
    align: "center", valign: "middle"
  });

  // Meta info
  slide.addText("14 微服务  ·  29 前端页面  ·  2026-08-10", {
    x: 0.6, y: 4.5, w: 8.8, h: 0.4,
    fontSize: 12, fontFace: "Arial",
    color: theme.light
  });
}
module.exports = { createSlide };
