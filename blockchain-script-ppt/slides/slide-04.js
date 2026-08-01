// 业务背景与挑战 - 为什么需要区块链
function createSlide(pres, theme) {
  const slide = pres.addSlide();
  slide.background = { color: theme.bg };

  // Title
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: 0.4, w: 0.08, h: 0.5,
    fill: { color: theme.accent }, line: { type: "none" }
  });
  slide.addText("业务背景:为什么双录系统需要区块链?", {
    x: 0.7, y: 0.4, w: 7.5, h: 0.5,
    fontSize: 20, fontFace: "Microsoft YaHei", color: theme.primary,
    bold: true, align: "left", valign: "middle", margin: 0
  });
  slide.addText("01 / 4", {
    x: 8.2, y: 0.5, w: 1.3, h: 0.3,
    fontSize: 10, fontFace: "Arial", color: theme.secondary,
    charSpacing: 3, align: "right"
  });

  // 4 pain point cards
  const pains = [
    {
      title: "证据易篡改",
      desc: "传统方案视频/合同/签字分散存储,任一环节被篡改,事后追溯难,司法采信率低",
      stat: "72%",
      statLabel: "监管检查存证质疑"
    },
    {
      title: "多方对账难",
      desc: "银保监/证监会/银行/客户/保险公司多方数据散落,出现争议时各执一词,责任难界定",
      stat: "5+",
      statLabel: "数据副本对账方"
    },
    {
      title: "监管报送滞后",
      desc: "T+1 人工报送,数据真实性依赖人工审核,无法做到实时存证 + 监管直连",
      stat: "24h",
      statLabel: "监管报送时间窗口"
    },
    {
      title: "司法采信弱",
      desc: "电子数据易灭失、易篡改,法庭上需要复杂的鉴真流程,举证成本高",
      stat: "30%",
      statLabel: "电子证据采信率"
    },
  ];

  const cardW = 2.15;
  const cardH = 3.4;
  const cardGap = 0.15;
  const cardStartX = 0.5;
  const cardY = 1.15;

  pains.forEach((p, i) => {
    const x = cardStartX + i * (cardW + cardGap);
    // Card background
    slide.addShape(pres.shapes.RECTANGLE, {
      x: x, y: cardY, w: cardW, h: cardH,
      fill: { color: theme.light }, line: { type: "none" }, rectRadius: 0.05
    });
    // Top color
    slide.addShape(pres.shapes.RECTANGLE, {
      x: x, y: cardY, w: cardW, h: 0.4,
      fill: { color: theme.accent }, line: { type: "none" }, rectRadius: 0.05
    });
    // Index number
    slide.addText(String(i + 1).padStart(2, "0"), {
      x: x + 0.15, y: cardY + 0.05, w: 0.6, h: 0.3,
      fontSize: 14, fontFace: "Arial", color: "FFFFFF",
      bold: true, align: "left", valign: "middle", margin: 0
    });
    slide.addText("痛点", {
      x: x + cardW - 0.6, y: cardY + 0.05, w: 0.5, h: 0.3,
      fontSize: 10, fontFace: "Microsoft YaHei", color: "FFFFFF",
      align: "right", valign: "middle", margin: 0
    });

    // Title
    slide.addText(p.title, {
      x: x + 0.15, y: cardY + 0.6, w: cardW - 0.3, h: 0.5,
      fontSize: 16, fontFace: "Microsoft YaHei", color: theme.primary,
      bold: true, align: "center", valign: "middle", margin: 0
    });

    // Big stat
    slide.addText(p.stat, {
      x: x + 0.15, y: cardY + 1.2, w: cardW - 0.3, h: 0.7,
      fontSize: 36, fontFace: "Arial", color: theme.accent,
      bold: true, align: "center", valign: "middle", margin: 0
    });
    slide.addText(p.statLabel, {
      x: x + 0.15, y: cardY + 1.95, w: cardW - 0.3, h: 0.3,
      fontSize: 9, fontFace: "Microsoft YaHei", color: theme.secondary,
      align: "center", valign: "middle", margin: 0
    });

    // Divider
    slide.addShape(pres.shapes.RECTANGLE, {
      x: x + 0.3, y: cardY + 2.35, w: cardW - 0.6, h: 0.02,
      fill: { color: theme.secondary }, line: { type: "none" }
    });

    // Description
    slide.addText(p.desc, {
      x: x + 0.15, y: cardY + 2.45, w: cardW - 0.3, h: 0.9,
      fontSize: 9.5, fontFace: "Microsoft YaHei", color: theme.primary,
      align: "left", valign: "top", margin: 0
    });
  });

  // Bottom callout
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: 4.7, w: 9, h: 0.45,
    fill: { color: theme.primary }, line: { type: "none" }, rectRadius: 0.03
  });
  slide.addText("【细节点 1】双录数据的「真实性、完整性、不可篡改性」是监管的硬性要求,区块链是当前唯一可数学证明的解法。", {
    x: 0.7, y: 4.7, w: 8.6, h: 0.45,
    fontSize: 11, fontFace: "Microsoft YaHei", color: "FFFFFF",
    bold: true, align: "left", valign: "middle", margin: 0
  });

  // Page number
  slide.addText("04 / 19", {
    x: 9.0, y: 5.25, w: 0.9, h: 0.3,
    fontSize: 10, fontFace: "Arial", color: theme.secondary,
    align: "right"
  });
}

module.exports = { createSlide };
