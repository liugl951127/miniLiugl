// Slide 02 - Table of Contents
function createSlide(pres, theme) {
  const slide = pres.addSlide();
  slide.addShape(pres.ShapeType.rect, {
    x: 0, y: 0, w: 10, h: 5.625,
    fill: { color: theme.bg }
  });

  // Left accent bar
  slide.addShape(pres.ShapeType.rect, {
    x: 0, y: 0, w: 0.08, h: 5.625,
    fill: { color: theme.primary }
  });

  // Title
  slide.addText("目录", {
    x: 0.6, y: 0.35, w: 9, h: 0.7,
    fontSize: 36, fontFace: "Microsoft YaHei",
    color: theme.primary, bold: true
  });

  const sections = [
    { num: "01", title: "项目概览", desc: "14 后端微服务 · 29 前端页面 · 核心技术栈" },
    { num: "02", title: "模块关联与调用关系", desc: "微服务依赖图 · 前端请求链路 · 认证流程" },
    { num: "03", title: "核心执行流程", desc: "AI Chat 链路 · RAG 检索 · 前端组件架构" }
  ];

  sections.forEach((s, i) => {
    const y = 1.4 + i * 1.3;

    // Number circle
    slide.addShape(pres.ShapeType.ellipse, {
      x: 0.6, y: y, w: 0.65, h: 0.65,
      fill: { color: theme.primary }
    });
    slide.addText(s.num, {
      x: 0.6, y: y, w: 0.65, h: 0.65,
      fontSize: 16, fontFace: "Arial",
      color: "FFFFFF", bold: true,
      align: "center", valign: "middle"
    });

    // Title
    slide.addText(s.title, {
      x: 1.5, y: y, w: 7, h: 0.4,
      fontSize: 22, fontFace: "Microsoft YaHei",
      color: theme.primary, bold: true
    });

    // Description
    slide.addText(s.desc, {
      x: 1.5, y: y + 0.42, w: 7, h: 0.3,
      fontSize: 13, fontFace: "Microsoft YaHei",
      color: theme.secondary
    });

    // Divider
    if (i < sections.length - 1) {
      slide.addShape(pres.ShapeType.rect, {
        x: 1.5, y: y + 1.05, w: 7.5, h: 0.01,
        fill: { color: "e2e8f0" }
      });
    }
  });

  // Page number
  slide.addText("02", {
    x: 9.3, y: 5.1, w: 0.5, h: 0.3,
    fontSize: 11, fontFace: "Arial",
    color: theme.secondary, align: "center"
  });
}
module.exports = { createSlide };
