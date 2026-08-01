// 落地保障 + 价值总结
function createSlide(pres, theme) {
  const slide = pres.addSlide();
  slide.background = { color: theme.primary };

  // Top label
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0, y: 0, w: 10, h: 0.6,
    fill: { color: theme.accent }, line: { type: "none" }
  });
  slide.addText("价值总结 · 落地节奏 · 客户承诺", {
    x: 0.5, y: 0, w: 9, h: 0.6,
    fontSize: 12, fontFace: "Microsoft YaHei", color: "FFFFFF",
    align: "left", valign: "middle", margin: 0
  });

  // Title
  slide.addText("从「合规成本」到「商业资产」", {
    x: 0.5, y: 0.75, w: 9, h: 0.55,
    fontSize: 26, fontFace: "Microsoft YaHei", color: "FFFFFF",
    bold: true, align: "left", valign: "middle"
  });
  slide.addText("Three Dimensions of Value", {
    x: 0.5, y: 1.3, w: 9, h: 0.3,
    fontSize: 11, fontFace: "Arial", color: theme.accent,
    charSpacing: 3, align: "left"
  });

  // 3 价值维度
  const dims = [
    {
      num: "01",
      title: "监管价值",
      color: theme.accent,
      items: [
        "证据 100% 可信,司法采信率 30% → 95%",
        "T+1 → 实时监管报送,违规预警 0 延迟",
        "区块链存证,直接对接电子证据司法链"
      ]
    },
    {
      num: "02",
      title: "客户价值",
      color: "2A9D8F",
      items: [
        "双录时长 30+ min → 5-8 min,降幅 75%",
        "一次通过率 70% → 95%,流失率降低 60%",
        "NPS 提升 10+ 分,复购率提升 25%"
      ]
    },
    {
      num: "03",
      title: "机构价值",
      color: "FFB703",
      items: [
        "客户经理产能释放 30%,服务更多客户",
        "纸质单据归零,年节省运营成本 500 万+",
        "客户全链路数据资产,精准营销转化 +20%"
      ]
    },
  ];

  const dW = 2.9;
  const dH = 2.5;
  const dGap = 0.18;
  const dStartX = 0.5;
  const dY = 1.85;

  dims.forEach((d, i) => {
    const x = dStartX + i * (dW + dGap);
    // Card
    slide.addShape(pres.shapes.RECTANGLE, {
      x: x, y: dY, w: dW, h: dH,
      fill: { color: "FFFFFF" }, line: { type: "none" }, rectRadius: 0.05
    });
    // Top
    slide.addShape(pres.shapes.RECTANGLE, {
      x: x, y: dY, w: dW, h: 0.7,
      fill: { color: d.color }, line: { type: "none" }, rectRadius: 0.05
    });
    slide.addText(d.num, {
      x: x + 0.15, y: dY + 0.1, w: 0.6, h: 0.5,
      fontSize: 22, fontFace: "Arial", color: "FFFFFF",
      bold: true, align: "left", valign: "middle", margin: 0
    });
    slide.addText(d.title, {
      x: x + 0.8, y: dY, w: dW - 0.9, h: 0.7,
      fontSize: 16, fontFace: "Microsoft YaHei", color: "FFFFFF",
      bold: true, align: "right", valign: "middle", margin: 0
    });
    // Items
    d.items.forEach((item, j) => {
      const iy = dY + 0.85 + j * 0.5;
      slide.addShape(pres.shapes.RECTANGLE, {
        x: x + 0.2, y: iy + 0.18, w: 0.06, h: 0.06,
        fill: { color: d.color }, line: { type: "none" }
      });
      slide.addText(item, {
        x: x + 0.35, y: iy, w: dW - 0.5, h: 0.45,
        fontSize: 10, fontFace: "Microsoft YaHei", color: theme.primary,
        align: "left", valign: "top", margin: 0
      });
    });
  });

  // Bottom callout - 落地节奏
  const bdY = 4.5;
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: bdY, w: 9, h: 0.55,
    fill: { color: theme.accent }, line: { type: "none" }, rectRadius: 0.05
  });
  slide.addText("【落地节奏】区块链 + 话术统一:第 1-3 月试点(1 个产品) → 第 4-6 月扩面(全产品) → 第 7 月起全行推广", {
    x: 0.7, y: bdY, w: 8.6, h: 0.55,
    fontSize: 11, fontFace: "Microsoft YaHei", color: "FFFFFF",
    bold: true, align: "left", valign: "middle", margin: 0
  });

  // Bottom signature
  slide.addText("汇报人:[姓名]    |    汇报日期:2026 年 8 月    |    19 / 19", {
    x: 0.5, y: 5.2, w: 9, h: 0.3,
    fontSize: 10, fontFace: "Microsoft YaHei", color: theme.light,
    align: "left", valign: "middle", margin: 0
  });
}

module.exports = { createSlide };
