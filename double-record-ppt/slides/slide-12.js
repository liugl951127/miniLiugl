// Script templates: Risk Assessment (风评)
function createSlide(pres, theme) {
  const slide = pres.addSlide();
  slide.background = { color: theme.bg };

  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: 0.4, w: 0.08, h: 0.5,
    fill: { color: theme.accent }, line: { type: "none" }
  });
  slide.addText("话术模板(2/2):风险评估流程 · 客户画像 · 智能匹配", {
    x: 0.7, y: 0.4, w: 8.5, h: 0.5,
    fontSize: 19, fontFace: "Microsoft YaHei", color: theme.primary,
    bold: true, align: "left", valign: "middle", margin: 0
  });

  // 风评流程图 - 大块卡片
  const fy = 1.05;
  slide.addText("风评流程(KYC 问卷 → 风险等级 → 产品匹配)", {
    x: 0.5, y: fy, w: 9, h: 0.3,
    fontSize: 14, fontFace: "Microsoft YaHei", color: theme.primary,
    bold: true, align: "left", margin: 0
  });

  // 5 step horizontal
  const stepW = 1.65;
  const stepGap = 0.15;
  const stepY = fy + 0.4;
  const stepH = 0.8;

  const riskSteps = [
    { n: "1", t: "基本信息", d: "年龄/职业/收入/资产" },
    { n: "2", t: "投资经验", d: "年限/产品类型/规模" },
    { n: "3", t: "风险偏好", d: "波动承受/亏损容忍" },
    { n: "4", t: "资金性质", d: "短期/长期/应急" },
    { n: "5", t: "等级评定", d: "C1-C5 风险等级" }
  ];

  riskSteps.forEach((s, i) => {
    const x = 0.5 + i * (stepW + stepGap);
    slide.addShape(pres.shapes.RECTANGLE, {
      x: x, y: stepY, w: stepW, h: stepH,
      fill: { color: theme.light }, line: { type: "none" }, rectRadius: 0.05
    });
    slide.addShape(pres.shapes.OVAL, {
      x: x + 0.15, y: stepY + 0.15, w: 0.4, h: 0.4,
      fill: { color: theme.primary }, line: { type: "none" }
    });
    slide.addText(s.n, {
      x: x + 0.15, y: stepY + 0.15, w: 0.4, h: 0.4,
      fontSize: 16, fontFace: "Arial", color: "FFFFFF",
      bold: true, align: "center", valign: "middle", margin: 0
    });
    slide.addText(s.t, {
      x: x + 0.6, y: stepY + 0.1, w: stepW - 0.7, h: 0.3,
      fontSize: 12, fontFace: "Microsoft YaHei", color: theme.primary,
      bold: true, align: "left", valign: "middle", margin: 0
    });
    slide.addText(s.d, {
      x: x + 0.6, y: stepY + 0.4, w: stepW - 0.7, h: 0.3,
      fontSize: 8, fontFace: "Microsoft YaHei", color: theme.secondary,
      align: "left", valign: "middle", margin: 0
    });
    if (i < riskSteps.length - 1) {
      slide.addText(">", {
        x: x + stepW - 0.05, y: stepY + 0.2, w: 0.2, h: 0.4,
        fontSize: 16, fontFace: "Arial", color: theme.accent,
        bold: true, align: "center", valign: "middle", margin: 0
      });
    }
  });

  // 风评话术样例
  const sy = 2.3;
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: sy, w: 9, h: 2.85,
    fill: { color: theme.light }, line: { type: "none" }, rectRadius: 0.05
  });
  slide.addText("风评话术样例(节选关键节点)", {
    x: 0.7, y: sy + 0.1, w: 6, h: 0.3,
    fontSize: 13, fontFace: "Microsoft YaHei", color: theme.primary,
    bold: true, align: "left", margin: 0
  });

  const scripts = [
    { tag: "Q1 知情同意", txt: "为给您匹配合适的产品,需先做风险评估,回答将作为销售依据,您是否同意?" },
    { tag: "Q2 收入询问", txt: "请告知您家庭的年收入范围:A.10 万以下  B.10-30 万  C.30-100 万  D.100 万以上" },
    { tag: "Q3 投资经验", txt: "您过往投资过哪些类型?股票/基金/银行理财/不动产/无,投资年限?" },
    { tag: "Q4 风险态度", txt: "若投资亏损 20%,您会:A.无法接受  B.焦虑但持有  C.可接受  D.加仓抄底" },
    { tag: "Q5 流动性", txt: "这笔资金的预计持有期限?短期(1 年内)/中期(1-3 年)/长期(3 年以上)" },
    { tag: "Q6 风险揭示", txt: "评估结果:您的风险等级为 C3(稳健型),适合 R3 及以下风险产品,是否知晓?" },
    { tag: "Q7 二次确认", txt: "以上评估基于您的真实意愿,如有虚假需承担相应责任,是否确认?" }
  ];

  scripts.forEach((s, i) => {
    const y = sy + 0.5 + i * 0.32;
    slide.addShape(pres.shapes.RECTANGLE, {
      x: 0.7, y: y, w: 1.1, h: 0.24,
      fill: { color: theme.primary }, line: { type: "none" }, rectRadius: 0.02
    });
    slide.addText(s.tag, {
      x: 0.7, y: y, w: 1.1, h: 0.24,
      fontSize: 8, fontFace: "Microsoft YaHei", color: "FFFFFF",
      bold: true, align: "center", valign: "middle", margin: 0
    });
    slide.addText(s.txt, {
      x: 1.9, y: y, w: 7.4, h: 0.24,
      fontSize: 9, fontFace: "Microsoft YaHei", color: theme.primary,
      align: "left", valign: "middle", margin: 0
    });
  });

  // Bottom callout
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: 5.2, w: 9, h: 0.32,
    fill: { color: theme.accent, transparency: 85 }, line: { type: "none" }, rectRadius: 0.03
  });
  slide.addText("【细节点 10】风评双录关联:评估结果与产品购买必须在同一次双录中,跨次评估视作失效(监管红线)。", {
    x: 0.6, y: 5.2, w: 8.8, h: 0.32,
    fontSize: 10, fontFace: "Microsoft YaHei", color: theme.primary,
    bold: true, align: "left", valign: "middle", margin: 0
  });

  slide.addText("12 / 16", {
    x: 9.0, y: 5.55, w: 0.9, h: 0.07,
    fontSize: 8, fontFace: "Arial", color: theme.secondary,
    align: "right"
  });
}

module.exports = { createSlide };
