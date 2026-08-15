// Slide 07 - Section Divider: 模块关联
function createSlide(pres, theme) {
  const slide = pres.addSlide();
  slide.addShape(pres.ShapeType.rect, {
    x: 0, y: 0, w: 10, h: 5.625,
    fill: { color: theme.accent }
  });

  slide.addText("02", {
    x: 0.6, y: 0.8, w: 3, h: 2,
    fontSize: 120, fontFace: "Arial",
    color: "FFFFFF", bold: true,
    transparency: 85
  });

  slide.addText("SECTION", {
    x: 0.6, y: 2.1, w: 3, h: 0.4,
    fontSize: 11, fontFace: "Arial",
    color: "FFFFFF", charSpacing: 4
  });

  slide.addShape(pres.ShapeType.rect, {
    x: 0.6, y: 2.6, w: 1.2, h: 0.05,
    fill: { color: "FFFFFF" }
  });

  slide.addText("模块关联与调用关系", {
    x: 0.6, y: 2.8, w: 9, h: 0.9,
    fontSize: 44, fontFace: "Microsoft YaHei",
    color: "FFFFFF", bold: true
  });

  slide.addText("微服务依赖图 · 前端请求链路 · 用户认证流程", {
    x: 0.6, y: 3.75, w: 9, h: 0.5,
    fontSize: 16, fontFace: "Microsoft YaHei",
    color: "FFFFFF", transparency: 30
  });

  slide.addText("07", {
    x: 9.3, y: 5.1, w: 0.5, h: 0.3,
    fontSize: 11, fontFace: "Arial",
    color: "FFFFFF", align: "center"
  });
}
module.exports = { createSlide };
