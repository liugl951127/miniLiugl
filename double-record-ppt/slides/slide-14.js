// Pain point resolution and customer value summary
function createSlide(pres, theme) {
  const slide = pres.addSlide();
  slide.background = { color: theme.bg };

  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: 0.4, w: 0.08, h: 0.5,
    fill: { color: theme.accent }, line: { type: "none" }
  });
  slide.addText("价值总结:8 大痛点 × 3 维价值", {
    x: 0.7, y: 0.4, w: 7.5, h: 0.5,
    fontSize: 20, fontFace: "Microsoft YaHei", color: theme.primary,
    bold: true, align: "left", valign: "middle", margin: 0
  });
  slide.addText("VALUE MATRIX", {
    x: 7.0, y: 0.5, w: 2.5, h: 0.3,
    fontSize: 10, fontFace: "Arial", color: theme.secondary,
    charSpacing: 4, align: "right"
  });

  // Three value dimensions
  const dimY = 1.1;
  const dimH = 4.0;
  const dimW = 2.9;
  const dimGap = 0.15;

  const dimensions = [
    {
      title: "痛点闭环",
      icon: "01",
      color: theme.accent,
      items: [
        { p: "话术互通", s: "统一话术中心,版本强一致" },
        { p: "视频合规", s: "全链路加密 + 区块链存证" },
        { p: "质检一致", s: "同一规则+同一评分卡" },
        { p: "数据一致", s: "Saga 分布式事务 + 状态机" }
      ]
    },
    {
      title: "客户价值",
      icon: "02",
      color: "2A5C8A",
      items: [
        { p: "时长缩短 70%", s: "线上 5-8min / 线下 15min" },
        { p: "一次通过 95%", s: "AI 预检降低重做" },
        { p: "足不出户", s: "PAD 上门 + 线上自助" },
        { p: "可恢复", s: "断点续传不重来" }
      ]
    },
    {
      title: "机构价值",
      icon: "03",
      color: "2A8A5C",
      items: [
        { p: "合规零处罚", s: "全量留痕 + 监管直连" },
        { p: "效率 3x", s: "客户经理产能翻倍" },
        { p: "数据资产", s: "全链路埋点 + 客户画像" },
        { p: "降本", s: "纸质/U盘/抽检成本清零" }
      ]
    }
  ];

  dimensions.forEach((d, i) => {
    const x = 0.5 + i * (dimW + dimGap);
    // Card
    slide.addShape(pres.shapes.RECTANGLE, {
      x: x, y: dimY, w: dimW, h: dimH,
      fill: { color: theme.light }, line: { type: "none" }, rectRadius: 0.05
    });
    // Header
    slide.addShape(pres.shapes.RECTANGLE, {
      x: x, y: dimY, w: dimW, h: 0.7,
      fill: { color: d.color }, line: { type: "none" }, rectRadius: 0.05
    });
    slide.addText(d.icon, {
      x: x + 0.15, y: dimY + 0.1, w: 0.6, h: 0.5,
      fontSize: 26, fontFace: "Arial", color: "FFFFFF",
      bold: true, align: "left", valign: "middle", margin: 0
    });
    slide.addText(d.title, {
      x: x + 0.7, y: dimY, w: dimW - 0.8, h: 0.7,
      fontSize: 18, fontFace: "Microsoft YaHei", color: "FFFFFF",
      bold: true, align: "left", valign: "middle", margin: 0
    });
    // Items
    d.items.forEach((it, j) => {
      const iy = dimY + 0.85 + j * 0.78;
      // Number
      slide.addShape(pres.shapes.OVAL, {
        x: x + 0.15, y: iy + 0.05, w: 0.3, h: 0.3,
        fill: { color: d.color }, line: { type: "none" }
      });
      slide.addText(String(j + 1), {
        x: x + 0.15, y: iy + 0.05, w: 0.3, h: 0.3,
        fontSize: 12, fontFace: "Arial", color: "FFFFFF",
        bold: true, align: "center", valign: "middle", margin: 0
      });
      // Point
      slide.addText(it.p, {
        x: x + 0.55, y: iy, w: dimW - 0.65, h: 0.3,
        fontSize: 12, fontFace: "Microsoft YaHei", color: theme.primary,
        bold: true, align: "left", valign: "middle", margin: 0
      });
      // Solution
      slide.addText(it.s, {
        x: x + 0.55, y: iy + 0.3, w: dimW - 0.65, h: 0.35,
        fontSize: 9, fontFace: "Microsoft YaHei", color: theme.secondary,
        align: "left", valign: "top", margin: 0
      });
    });
  });

  // Bottom callout
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: 5.2, w: 9, h: 0.32,
    fill: { color: theme.accent, transparency: 85 }, line: { type: "none" }, rectRadius: 0.03
  });
  slide.addText("【细节点 12】核心价值闭环:监管合规(下限) → 客户体验(中线) → 机构效率(上限),三层同时达成。", {
    x: 0.6, y: 5.2, w: 8.8, h: 0.32,
    fontSize: 10, fontFace: "Microsoft YaHei", color: theme.primary,
    bold: true, align: "left", valign: "middle", margin: 0
  });

  slide.addText("14 / 16", {
    x: 9.0, y: 5.55, w: 0.9, h: 0.07,
    fontSize: 8, fontFace: "Arial", color: theme.secondary,
    align: "right"
  });
}

module.exports = { createSlide };
