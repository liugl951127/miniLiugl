// Table of contents - 4 sections
function createSlide(pres, theme) {
  const slide = pres.addSlide();
  slide.background = { color: theme.bg };

  // Title
  slide.addText("目  录", {
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
    { num: "01", title: "区块链技术架构方案", desc: "联盟链选型 + 网络拓扑 + 智能合约 + 国密集成" },
    { num: "02", title: "话术统一一致性方案", desc: "统一话术模型 + TTS/ASR 适配 + 同步播报 + 版本管理" },
    { num: "03", title: "对客体验感优化方案", desc: "客户旅程地图 + 多端协同 + 智能交互 + 实时反馈" },
    { num: "04", title: "落地保障与价值总结", desc: "上线节奏 + 风险预案 + 客户/机构/监管三方价值" },
  ];

  // 2x2 grid
  const startY = 1.85;
  const rowH = 1.5;
  const colXs = [0.5, 5.1];
  const colW = 4.4;

  sections.forEach((s, i) => {
    const col = i < 2 ? 0 : 1;
    const row = i % 2;
    const x = colXs[col];
    const y = startY + row * (rowH + 0.15);

    // Card background
    slide.addShape(pres.shapes.RECTANGLE, {
      x: x, y: y, w: colW, h: rowH,
      fill: { color: theme.light }, line: { type: "none" },
      rectRadius: 0.05
    });
    // Accent bar
    slide.addShape(pres.shapes.RECTANGLE, {
      x: x, y: y, w: 0.08, h: rowH,
      fill: { color: theme.accent }, line: { type: "none" }
    });
    // Section number
    slide.addText(s.num, {
      x: x + 0.2, y: y + 0.15, w: 0.9, h: 0.7,
      fontSize: 32, fontFace: "Arial", color: theme.accent,
      bold: true, align: "left", valign: "middle", margin: 0
    });
    // Section title
    slide.addText(s.title, {
      x: x + 1.1, y: y + 0.18, w: colW - 1.2, h: 0.4,
      fontSize: 17, fontFace: "Microsoft YaHei", color: theme.primary,
      bold: true, align: "left", valign: "middle", margin: 0
    });
    // Description
    slide.addText(s.desc, {
      x: x + 1.1, y: y + 0.65, w: colW - 1.2, h: 0.7,
      fontSize: 11, fontFace: "Microsoft YaHei", color: theme.secondary,
      align: "left", valign: "top", margin: 0
    });
  });

  // Page number
  slide.addText("02 / 19", {
    x: 9.0, y: 5.25, w: 0.9, h: 0.3,
    fontSize: 10, fontFace: "Arial", color: theme.secondary,
    align: "right"
  });
}

module.exports = { createSlide };
