// 话术统一一致性挑战
function createSlide(pres, theme) {
  const slide = pres.addSlide();
  slide.background = { color: theme.bg };

  // Title
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: 0.4, w: 0.08, h: 0.5,
    fill: { color: theme.accent }, line: { type: "none" }
  });
  slide.addText("线上线下话术不统一,带来哪些麻烦?", {
    x: 0.7, y: 0.4, w: 7.5, h: 0.5,
    fontSize: 20, fontFace: "Microsoft YaHei", color: theme.primary,
    bold: true, align: "left", valign: "middle", margin: 0
  });
  slide.addText("01 / 3", {
    x: 8.2, y: 0.5, w: 1.3, h: 0.3,
    fontSize: 10, fontFace: "Arial", color: theme.secondary,
    charSpacing: 3, align: "right"
  });

  // 4 challenges
  const challenges = [
    {
      icon: "①",
      title: "版本混乱",
      desc: "线上 V3.2、线下 V2.8 同时运行,产品更新 3-7 天才能同步",
      stat: "7 天",
      statLabel: "平均同步周期"
    },
    {
      icon: "②",
      title: "口径分歧",
      desc: "同一产品在线上线下话术不一致,客户发现后质疑业务专业性",
      stat: "23%",
      statLabel: "客户投诉率"
    },
    {
      icon: "③",
      title: "体验割裂",
      desc: "线上 H5 自动播报,线下客户经理背诵,客户感受两套流程",
      stat: "2x",
      statLabel: "线下办理时长"
    },
    {
      icon: "④",
      title: "合规风险",
      desc: "口径不一致导致监管检查发现「同产品不同风险揭示」",
      stat: "100%",
      statLabel: "监管检查重点"
    },
  ];

  const cardW = 2.15;
  const cardH = 2.4;
  const cardGap = 0.15;
  const cardStartX = 0.5;
  const cardY = 1.15;

  challenges.forEach((c, i) => {
    const x = cardStartX + i * (cardW + cardGap);
    slide.addShape(pres.shapes.RECTANGLE, {
      x: x, y: cardY, w: cardW, h: cardH,
      fill: { color: theme.light }, line: { type: "none" }, rectRadius: 0.05
    });
    // Icon
    slide.addText(c.icon, {
      x: x + 0.15, y: cardY + 0.1, w: 0.6, h: 0.5,
      fontSize: 28, fontFace: "Arial", color: theme.accent,
      bold: true, align: "left", valign: "middle", margin: 0
    });
    // Title
    slide.addText(c.title, {
      x: x + 0.15, y: cardY + 0.6, w: cardW - 0.3, h: 0.35,
      fontSize: 14, fontFace: "Microsoft YaHei", color: theme.primary,
      bold: true, align: "left", valign: "middle", margin: 0
    });
    // Stat
    slide.addText(c.stat, {
      x: x, y: cardY + 1.0, w: cardW, h: 0.5,
      fontSize: 28, fontFace: "Arial", color: theme.accent,
      bold: true, align: "center", valign: "middle", margin: 0
    });
    slide.addText(c.statLabel, {
      x: x, y: cardY + 1.5, w: cardW, h: 0.25,
      fontSize: 9, fontFace: "Microsoft YaHei", color: theme.secondary,
      align: "center", valign: "middle", margin: 0
    });
    // Divider
    slide.addShape(pres.shapes.RECTANGLE, {
      x: x + 0.3, y: cardY + 1.8, w: cardW - 0.6, h: 0.02,
      fill: { color: theme.secondary }, line: { type: "none" }
    });
    // Desc
    slide.addText(c.desc, {
      x: x + 0.15, y: cardY + 1.85, w: cardW - 0.3, h: 0.5,
      fontSize: 9, fontFace: "Microsoft YaHei", color: theme.primary,
      align: "left", valign: "top", margin: 0
    });
  });

  // 核心目标 callout
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: 3.65, w: 9, h: 0.5,
    fill: { color: theme.primary }, line: { type: "none" }, rectRadius: 0.05
  });
  slide.addText("【核心目标】一份话术库 · 一套播报引擎 · 两种接入形态,客户体验完全一致", {
    x: 0.7, y: 3.65, w: 8.6, h: 0.5,
    fontSize: 12, fontFace: "Microsoft YaHei", color: "FFFFFF",
    bold: true, align: "left", valign: "middle", margin: 0
  });

  // 设计原则
  const ppY = 4.0;
  slide.addText("【3 大设计原则】", {
    x: 0.5, y: ppY, w: 2, h: 0.3,
    fontSize: 11, fontFace: "Microsoft YaHei", color: theme.accent,
    bold: true, align: "left", valign: "middle", margin: 0
  });

  const principles = [
    { num: "01", t: "单一信源(SSOT)", d: "线上线下共享同一份话术,只读不写" },
    { num: "02", t: "抽象统一模型", d: "话术 → 节点 → 语料 → 渲染,与载体无关" },
    { num: "03", t: "适配层下沉", d: "差异在 SDK 适配,业务逻辑零变化" },
  ];
  principles.forEach((p, i) => {
    const y = ppY + 0.32 + i * 0.28;
    slide.addText(p.num + "  " + p.t, {
      x: 0.5, y: y, w: 2.5, h: 0.28,
      fontSize: 10, fontFace: "Microsoft YaHei", color: theme.primary,
      bold: true, align: "left", valign: "middle", margin: 0
    });
    slide.addText(p.d, {
      x: 3.0, y: y, w: 6.5, h: 0.28,
      fontSize: 10, fontFace: "Microsoft YaHei", color: theme.secondary,
      align: "left", valign: "middle", margin: 0
    });
  });

  // 细节点
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: 5.2, w: 9, h: 0.32,
    fill: { color: theme.accent, transparency: 85 }, line: { type: "none" }, rectRadius: 0.03
  });
  slide.addText("【细节点 8】话术同步必须支持灰度发布:5% 流量 → 24h 观察 → 全量,避免版本事故。", {
    x: 0.6, y: 5.2, w: 8.8, h: 0.32,
    fontSize: 10, fontFace: "Microsoft YaHei", color: theme.primary,
    bold: true, align: "left", valign: "middle", margin: 0
  });

  slide.addText("13 / 19", {
    x: 9.0, y: 5.55, w: 0.9, h: 0.07,
    fontSize: 8, fontFace: "Arial", color: theme.secondary,
    align: "right"
  });
}

module.exports = { createSlide };
