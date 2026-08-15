// Slide 05 - 29 前端页面
function createSlide(pres, theme) {
  const slide = pres.addSlide();
  slide.addShape(pres.ShapeType.rect, {
    x: 0, y: 0, w: 10, h: 5.625,
    fill: { color: theme.bg }
  });
  slide.addShape(pres.ShapeType.rect, {
    x: 0, y: 0, w: 0.08, h: 5.625,
    fill: { color: theme.accent }
  });

  slide.addText("29 个前端页面", {
    x: 0.5, y: 0.3, w: 9, h: 0.6,
    fontSize: 28, fontFace: "Microsoft YaHei",
    color: theme.primary, bold: true
  });

  const pages = [
    { cat: "核心模块", items: ["AI Chat", "知识库", "Agent", "记忆", "模型", "函数", "多模态", "KG"] },
    { cat: "分析监控", items: ["Dashboard", "Analytics", "Monitor", "Traces", "Metrics", "Alerts"] },
    { cat: "管道编排", items: ["Pipeline", "Pipeline Designer", "Runs", "Analytics"] },
    { cat: "运营管理", items: ["Admin", "Provider", "Audit", "Push", "Cluster", "Wechat"] },
    { cat: "AI 能力", items: ["PPT 生成", "项目生成", "Agent Group", "意图识别"] },
    { cat: "其他", items: ["Token", "I18n", "Auth", "About", "Profile", "Plugins"] },
  ];

  const cols = 3;
  pages.forEach((group, gi) => {
    const x = 0.5 + (gi % cols) * 3.15;
    const y = 1.05 + Math.floor(gi / cols) * 2.15;

    // Group header
    slide.addShape(pres.ShapeType.roundRect, {
      x, y, w: 3.0, h: 0.4,
      fill: { color: theme.primary },
      rectRadius: 0.05
    });
    slide.addText(group.cat, {
      x, y, w: 3.0, h: 0.4,
      fontSize: 12, fontFace: "Microsoft YaHei",
      color: "FFFFFF", bold: true,
      align: "center", valign: "middle"
    });

    // Items
    group.items.forEach((item, ii) => {
      const iy = y + 0.5 + ii * 0.22;
      slide.addShape(pres.ShapeType.rect, {
        x: x + 0.15, y: iy + 0.07, w: 0.12, h: 0.12,
        fill: { color: theme.accent }
      });
      slide.addText(item, {
        x: x + 0.35, y: iy, w: 2.5, h: 0.26,
        fontSize: 11, fontFace: "Microsoft YaHei",
        color: theme.secondary
      });
    });
  });

  slide.addText("05", {
    x: 9.3, y: 5.1, w: 0.5, h: 0.3,
    fontSize: 11, fontFace: "Arial",
    color: theme.secondary, align: "center"
  });
}
module.exports = { createSlide };
