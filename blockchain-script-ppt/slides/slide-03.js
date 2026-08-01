// 区块链技术架构方案 - Section divider
function createSlide(pres, theme) {
  const slide = pres.addSlide();
  slide.background = { color: theme.primary };

  // Big "01"
  slide.addText("01", {
    x: 0.7, y: 1.4, w: 4, h: 2.5,
    fontSize: 180, fontFace: "Arial", color: theme.accent,
    bold: true, align: "left", valign: "middle", charSpacing: -5
  });

  // Vertical accent line
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 5.0, y: 1.6, w: 0.04, h: 2.0,
    fill: { color: theme.accent }, line: { type: "none" }
  });

  // Section title
  slide.addText("区块链技术架构方案", {
    x: 5.3, y: 1.8, w: 4.2, h: 0.7,
    fontSize: 30, fontFace: "Microsoft YaHei", color: "FFFFFF",
    bold: true, align: "left", valign: "middle"
  });
  slide.addText("Block-chain Technical Architecture", {
    x: 5.3, y: 2.5, w: 4.2, h: 0.4,
    fontSize: 12, fontFace: "Arial", color: theme.accent,
    charSpacing: 3, align: "left"
  });
  slide.addText("联盟链选型 · 网络拓扑 · 智能合约 · 国密集成", {
    x: 5.3, y: 3.1, w: 4.2, h: 0.4,
    fontSize: 13, fontFace: "Microsoft YaHei", color: theme.light,
    align: "left"
  });

  // Bottom info bar
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0, y: 5.0, w: 10, h: 0.625,
    fill: { color: theme.accent }, line: { type: "none" }
  });
  slide.addText("P3 / 19    双录一体化 · 核心技术方案", {
    x: 0.5, y: 5.0, w: 9, h: 0.625,
    fontSize: 11, fontFace: "Microsoft YaHei", color: "FFFFFF",
    align: "left", valign: "middle", margin: 0
  });
}

module.exports = { createSlide };
