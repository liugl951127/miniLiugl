// Main business flow chart
function createSlide(pres, theme) {
  const slide = pres.addSlide();
  slide.background = { color: theme.bg };

  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: 0.4, w: 0.08, h: 0.5,
    fill: { color: theme.accent }, line: { type: "none" }
  });
  slide.addText("端到端业务流程:从预约到归档", {
    x: 0.7, y: 0.4, w: 7.5, h: 0.5,
    fontSize: 20, fontFace: "Microsoft YaHei", color: theme.primary,
    bold: true, align: "left", valign: "middle", margin: 0
  });
  slide.addText("END-TO-END FLOW", {
    x: 7.0, y: 0.5, w: 2.5, h: 0.3,
    fontSize: 10, fontFace: "Arial", color: theme.secondary,
    charSpacing: 4, align: "right"
  });

  // Flow stages
  const stages = [
    { n: "01", t: "预约", d: "客户身份预校验 + 产品匹配", icon: "" },
    { n: "02", t: "排程", d: "客户经理排期 + 渠道分配", icon: "" },
    { n: "03", t: "核身", d: "证件 OCR + 活体 + 双签", icon: "" },
    { n: "04", t: "风评", d: "KYC 问卷 + 风险等级评定", icon: "" },
    { n: "05", t: "双录", d: "话术执行 + 音视频同步录制", icon: "" },
    { n: "06", t: "签约", d: "电子合同 + CA 数字签名", icon: "" },
    { n: "07", t: "质检", d: "AI 智能 + 人工复核", icon: "" },
    { n: "08", t: "归档", d: "区块链存证 + 监管上报", icon: "" }
  ];

  const stageY = 1.4;
  const stageW = 1.1;
  const stageH = 1.2;
  const stageGap = 0.07;
  const stageStartX = 0.5;

  stages.forEach((s, i) => {
    const x = stageStartX + i * (stageW + stageGap);
    // Card
    slide.addShape(pres.shapes.RECTANGLE, {
      x: x, y: stageY, w: stageW, h: stageH,
      fill: { color: theme.light }, line: { type: "none" }, rectRadius: 0.05
    });
    // Top color block
    slide.addShape(pres.shapes.RECTANGLE, {
      x: x, y: stageY, w: stageW, h: 0.35,
      fill: { color: theme.primary }, line: { type: "none" }, rectRadius: 0.05
    });
    slide.addText(s.n, {
      x: x, y: stageY, w: stageW, h: 0.35,
      fontSize: 11, fontFace: "Arial", color: theme.accent,
      bold: true, align: "center", valign: "middle", margin: 0
    });
    // Title
    slide.addText(s.t, {
      x: x, y: stageY + 0.4, w: stageW, h: 0.3,
      fontSize: 13, fontFace: "Microsoft YaHei", color: theme.primary,
      bold: true, align: "center", valign: "middle", margin: 0
    });
    // Description
    slide.addText(s.d, {
      x: x + 0.05, y: stageY + 0.7, w: stageW - 0.1, h: 0.5,
      fontSize: 8, fontFace: "Microsoft YaHei", color: theme.secondary,
      align: "center", valign: "top", margin: 0
    });

    // Arrow between
    if (i < stages.length - 1) {
      slide.addText(">", {
        x: x + stageW - 0.05, y: stageY + 0.4, w: 0.17, h: 0.4,
        fontSize: 14, fontFace: "Arial", color: theme.accent,
        bold: true, align: "center", valign: "middle", margin: 0
      });
    }
  });

  // Branch layer
  const branchY = 2.85;
  slide.addText("渠道分支(同一流程,不同载体)", {
    x: 0.5, y: branchY, w: 9, h: 0.3,
    fontSize: 13, fontFace: "Microsoft YaHei", color: theme.primary,
    bold: true, align: "left", margin: 0
  });

  const channels = [
    { t: "线上 H5/小程序", c: theme.primary, d: "全程 5-8 分钟,客户自助" },
    { t: "线下一体机(高柜)", c: theme.primary, d: "客户经理陪同,15-20 分钟" },
    { t: "PAD 移动展业", c: theme.primary, d: "外拓场景,10-15 分钟" }
  ];

  const chW = 2.9;
  const chGap = 0.15;
  channels.forEach((c, i) => {
    const x = 0.5 + i * (chW + chGap);
    const y = branchY + 0.4;
    slide.addShape(pres.shapes.RECTANGLE, {
      x: x, y: y, w: chW, h: 1.0,
      fill: { color: theme.light }, line: { type: "none" }, rectRadius: 0.05
    });
    slide.addShape(pres.shapes.RECTANGLE, {
      x: x, y: y, w: 0.08, h: 1.0,
      fill: { color: theme.accent }, line: { type: "none" }
    });
    slide.addText(c.t, {
      x: x + 0.2, y: y + 0.1, w: chW - 0.3, h: 0.3,
      fontSize: 13, fontFace: "Microsoft YaHei", color: theme.primary,
      bold: true, align: "left", valign: "middle", margin: 0
    });
    slide.addText(c.d, {
      x: x + 0.2, y: y + 0.4, w: chW - 0.3, h: 0.5,
      fontSize: 10, fontFace: "Microsoft YaHei", color: theme.secondary,
      align: "left", valign: "top", margin: 0
    });
  });

  // Key checkpoints row
  const cpY = 4.45;
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: cpY, w: 9, h: 0.7,
    fill: { color: theme.primary }, line: { type: "none" }, rectRadius: 0.05
  });
  slide.addText("关键检查点(Gate)", {
    x: 0.7, y: cpY + 0.05, w: 2, h: 0.25,
    fontSize: 11, fontFace: "Microsoft YaHei", color: "FFFFFF",
    bold: true, align: "left", margin: 0
  });
  const gates = [
    "G1 实名核身通过",
    "G2 风评等级与产品匹配",
    "G3 话术 100% 执行",
    "G4 客户意愿明确确认",
    "G5 质检分数 ≥ 70"
  ];
  const gateW = 1.7;
  gates.forEach((g, i) => {
    const x = 0.7 + i * (gateW + 0.05);
    slide.addShape(pres.shapes.RECTANGLE, {
      x: x, y: cpY + 0.35, w: gateW, h: 0.28,
      fill: { color: theme.accent }, line: { type: "none" }, rectRadius: 0.03
    });
    slide.addText(g, {
      x: x, y: cpY + 0.35, w: gateW, h: 0.28,
      fontSize: 8, fontFace: "Microsoft YaHei", color: "FFFFFF",
      bold: true, align: "center", valign: "middle", margin: 0
    });
  });

  // Bottom callout
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: 5.2, w: 9, h: 0.32,
    fill: { color: theme.accent, transparency: 85 }, line: { type: "none" }, rectRadius: 0.03
  });
  slide.addText("【细节点 7】任一 Gate 不通过 → 流程挂起/重做,绝对不可跳过,这是监管检查的核心证据。", {
    x: 0.6, y: 5.2, w: 8.8, h: 0.32,
    fontSize: 10, fontFace: "Microsoft YaHei", color: theme.primary,
    bold: true, align: "left", valign: "middle", margin: 0
  });

  slide.addText("09 / 16", {
    x: 9.0, y: 5.55, w: 0.9, h: 0.07,
    fontSize: 8, fontFace: "Arial", color: theme.secondary,
    align: "right"
  });
}

module.exports = { createSlide };
