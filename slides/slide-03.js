// Slide 03 - Section Divider: 项目概览
function createSlide(pres, theme) {
  const slide = pres.addSlide();
  slide.addShape(pres.ShapeType.rect, {
    x: 0, y: 0, w: 10, h: 5.625,
    fill: { color: theme.primary }
  });

  // Large section number
  slide.addText("01", {
    x: 0.6, y: 0.8, w: 3, h: 2,
    fontSize: 120, fontFace: "Arial",
    color: "FFFFFF", bold: true,
    transparency: 85
  });

  // Section label
  slide.addText("SECTION", {
    x: 0.6, y: 2.1, w: 3, h: 0.4,
    fontSize: 11, fontFace: "Arial",
    color: theme.accent, charSpacing: 4
  });

  // Divider line
  slide.addShape(pres.ShapeType.rect, {
    x: 0.6, y: 2.6, w: 1.2, h: 0.05,
    fill: { color: theme.accent }
  });

  // Title
  slide.addText("项目概览", {
    x: 0.6, y: 2.8, w: 9, h: 0.9,
    fontSize: 44, fontFace: "Microsoft YaHei",
    color: "FFFFFF", bold: true
  });

  // Subtitle
  slide.addText("14 后端微服务 · 29 前端页面 · 40+ 功能模块", {
    x: 0.6, y: 3.75, w: 9, h: 0.5,
    fontSize: 16, fontFace: "Microsoft YaHei",
    color: theme.light
  });

  // Page number
  slide.addText("03", {
    x: 9.3, y: 5.1, w: 0.5, h: 0.3,
    fontSize: 11, fontFace: "Arial",
    color: theme.light, align: "center"
  });
}
module.exports = { createSlide };
