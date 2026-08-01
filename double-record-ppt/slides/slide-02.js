// Table of contents - 5 sections
function createSlide(pres, theme) {
  const slide = pres.addSlide();
  slide.background = { color: theme.bg };

  // Title
  slide.addText("目录", {
    x: 0.5, y: 0.4, w: 4, h: 0.6,
    fontSize: 30, fontFace: "Microsoft YaHei", color: theme.primary,
    bold: true, align: "left"
  });
  slide.addText("CONTENTS", {
    x: 0.5, y: 1.0, w: 4, h: 0.3,
    fontSize: 12, fontFace: "Arial", color: theme.secondary,
    charSpacing: 6, align: "left"
  });
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: 1.35, w: 0.6, h: 0.04,
    fill: { color: theme.accent }, line: { type: "none" }
  });

  const sections = [
    { num: "01", title: "痛点分析", desc: "线上线下双录现状与 5 大核心痛点" },
    { num: "02", title: "整体架构", desc: "分层架构设计与 5 大能力中心" },
    { num: "03", title: "核心方案设计", desc: "话术互通 / 双录合规 / 智能质检 / 数据一致性" },
    { num: "04", title: "话术模板", desc: "保险/理财/基金/风评全场景话术样例" },
    { num: "05", title: "价值与细节点标注", desc: "痛点闭环 · 客户价值 · 汇报用标注表" }
  ];

  // Two-column grid: 3 left, 2 right
  const startY = 1.85;
  const rowH = 0.85;
  const colXs = [0.5, 5.1];
  const colW = 4.4;

  sections.forEach((s, i) => {
    const col = i < 3 ? 0 : 1;
    const row = i < 3 ? i : (i - 3);
    const x = colXs[col];
    const y = startY + row * rowH;

    // Card background
    slide.addShape(pres.shapes.RECTANGLE, {
      x: x, y: y, w: colW, h: 0.75,
      fill: { color: theme.light }, line: { type: "none" },
      rectRadius: 0.05
    });
    // Accent bar
    slide.addShape(pres.shapes.RECTANGLE, {
      x: x, y: y, w: 0.08, h: 0.75,
      fill: { color: theme.accent }, line: { type: "none" }
    });
    // Section number
    slide.addText(s.num, {
      x: x + 0.2, y: y + 0.08, w: 0.7, h: 0.6,
      fontSize: 28, fontFace: "Arial", color: theme.accent,
      bold: true, align: "left", valign: "middle", margin: 0
    });
    // Section title
    slide.addText(s.title, {
      x: x + 0.95, y: y + 0.08, w: colW - 1.05, h: 0.32,
      fontSize: 16, fontFace: "Microsoft YaHei", color: theme.primary,
      bold: true, align: "left", valign: "middle", margin: 0
    });
    // Description
    slide.addText(s.desc, {
      x: x + 0.95, y: y + 0.4, w: colW - 1.05, h: 0.3,
      fontSize: 10, fontFace: "Microsoft YaHei", color: theme.secondary,
      align: "left", valign: "middle", margin: 0
    });
  });

  // Page number
  slide.addText("02 / 16", {
    x: 9.0, y: 5.25, w: 0.9, h: 0.3,
    fontSize: 10, fontFace: "Arial", color: theme.secondary,
    align: "right"
  });
}

module.exports = { createSlide };
