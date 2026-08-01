// Closing - thank you with contact
function createSlide(pres, theme) {
  const slide = pres.addSlide();
  slide.background = { color: theme.primary };

  // Decorative left bar
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0, y: 0, w: 0.35, h: 5.625,
    fill: { color: theme.accent }, line: { type: "none" }
  });

  // Big THANK YOU
  slide.addText("THANK YOU", {
    x: 0.7, y: 1.4, w: 8, h: 1.0,
    fontSize: 60, fontFace: "Arial", color: "FFFFFF",
    bold: true, align: "left", valign: "middle", charSpacing: 8
  });

  // Subtitle
  slide.addText("感谢聆听 · 欢迎讨论", {
    x: 0.7, y: 2.5, w: 8, h: 0.5,
    fontSize: 22, fontFace: "Microsoft YaHei", color: theme.accent,
    align: "left"
  });

  // Divider
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.7, y: 3.2, w: 1.0, h: 0.04,
    fill: { color: theme.accent }, line: { type: "none" }
  });

  // Contact info row
  const cY = 3.6;
  const contacts = [
    { t: "汇报人", v: "[您的姓名]" },
    { t: "部门", v: "金融科技部 / 合规部" },
    { t: "电话", v: "[您的联系方式]" },
    { t: "邮箱", v: "[your.email@bank.com]" }
  ];

  contacts.forEach((c, i) => {
    const col = i % 2;
    const row = Math.floor(i / 2);
    const x = 0.7 + col * 4.0;
    const y = cY + row * 0.5;
    slide.addText(c.t, {
      x: x, y: y, w: 0.7, h: 0.3,
      fontSize: 11, fontFace: "Microsoft YaHei", color: theme.accent,
      bold: true, align: "left", valign: "middle", margin: 0
    });
    slide.addText(c.v, {
      x: x + 0.7, y: y, w: 3.2, h: 0.3,
      fontSize: 12, fontFace: "Microsoft YaHei", color: "FFFFFF",
      align: "left", valign: "middle", margin: 0
    });
  });

  // Footer line
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.7, y: 5.0, w: 8.6, h: 0.02,
    fill: { color: theme.accent, transparency: 60 }, line: { type: "none" }
  });
  slide.addText("线上线下双录一体化方案  |  V1.0  |  2026", {
    x: 0.7, y: 5.1, w: 6, h: 0.3,
    fontSize: 10, fontFace: "Microsoft YaHei", color: theme.light,
    align: "left", valign: "middle"
  });
  slide.addText("16 / 16", {
    x: 8.5, y: 5.1, w: 1, h: 0.3,
    fontSize: 10, fontFace: "Arial", color: theme.light,
    align: "right", valign: "middle"
  });
}

module.exports = { createSlide };
