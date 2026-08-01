// Script templates: insurance / wealth / fund
function createSlide(pres, theme) {
  const slide = pres.addSlide();
  slide.background = { color: theme.bg };

  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: 0.4, w: 0.08, h: 0.5,
    fill: { color: theme.accent }, line: { type: "none" }
  });
  slide.addText("话术模板(1/2):保险 · 理财 · 基金", {
    x: 0.7, y: 0.4, w: 7.5, h: 0.5,
    fontSize: 20, fontFace: "Microsoft YaHei", color: theme.primary,
    bold: true, align: "left", valign: "middle", margin: 0
  });
  slide.addText("SCRIPT TEMPLATES", {
    x: 7.0, y: 0.5, w: 2.5, h: 0.3,
    fontSize: 10, fontFace: "Arial", color: theme.secondary,
    charSpacing: 4, align: "right"
  });

  // Three product columns
  const prodY = 1.1;
  const prodH = 4.0;
  const prodW = 2.9;
  const prodGap = 0.15;

  const products = [
    {
      name: "保险产品",
      sub: "INSURANCE",
      color: theme.primary,
      nodes: [
        { tag: "N1 问候核身", txt: "您好,我是 XX 客户经理,本次双录将全程录音录像,作为合规凭证保存 10 年。" },
        { tag: "N2 产品告知", txt: "本产品为终身寿险(分红型),保险责任以合同条款为准。" },
        { tag: "N3 风险揭示", txt: "分红不确定,演示利率非保证收益,可能为零,您是否充分理解?" },
        { tag: "N4 犹豫期", txt: "您有 15 天犹豫期,期内退保仅扣 10 元工本费。" },
        { tag: "N5 健康告知", txt: "请如实告知健康状况,隐瞒可能影响理赔。" },
        { tag: "N6 签字确认", txt: "以上内容均为您的真实意愿,是否确认投保?" }
      ]
    },
    {
      name: "理财产品",
      sub: "WEALTH",
      color: "2A5C8A",
      nodes: [
        { tag: "N1 风险匹配", txt: "您的风险等级为 C3(稳健型),本产品风险等级 C2,匹配通过。" },
        { tag: "N2 产品要素", txt: "本产品为封闭式净值型理财,期限 365 天,业绩比较基准 3.5%-4.2%。" },
        { tag: "N3 收益说明", txt: "业绩比较基准非保证收益,实际可能低于基准,本金不保证。" },
        { tag: "N4 资金用途", txt: "本产品募集资金主要投资于债券和同业存单。" },
        { tag: "N5 流动性", txt: "产品封闭期内不可赎回,是否影响您的资金安排?" },
        { tag: "N6 确认", txt: "请确认知悉风险与产品要素,是否继续购买?" }
      ]
    },
    {
      name: "基金产品",
      sub: "FUND",
      color: "2A8A5C",
      nodes: [
        { tag: "N1 适当性", txt: "经评估您的风险等级为 C4(成长型),本基金风险等级 C4,匹配通过。" },
        { tag: "N2 基金类型", txt: "本基金为股票型开放式基金,股票仓位不低于 80%。" },
        { tag: "N3 净值波动", txt: "基金净值随市场波动,可能出现本金损失,极端情况下亏损超过 30%。" },
        { tag: "N4 费率说明", txt: "申购费 1.5%,管理费 1.5%/年,托管费 0.25%/年,持有 7 天内赎回 1.5% 罚息。" },
        { tag: "N5 历史业绩", txt: "过往业绩不预示未来表现,投资需谨慎。" },
        { tag: "N6 确认", txt: "请确认已阅读《招募说明书》《风险揭示书》,是否确认申购?" }
      ]
    }
  ];

  products.forEach((p, i) => {
    const x = 0.5 + i * (prodW + prodGap);
    // Card
    slide.addShape(pres.shapes.RECTANGLE, {
      x: x, y: prodY, w: prodW, h: prodH,
      fill: { color: theme.light }, line: { type: "none" }, rectRadius: 0.05
    });
    // Top header
    slide.addShape(pres.shapes.RECTANGLE, {
      x: x, y: prodY, w: prodW, h: 0.6,
      fill: { color: p.color }, line: { type: "none" }, rectRadius: 0.05
    });
    slide.addText(p.name, {
      x: x + 0.15, y: prodY, w: prodW - 0.3, h: 0.35,
      fontSize: 16, fontFace: "Microsoft YaHei", color: "FFFFFF",
      bold: true, align: "left", valign: "middle", margin: 0
    });
    slide.addText(p.sub, {
      x: x + 0.15, y: prodY + 0.32, w: prodW - 0.3, h: 0.25,
      fontSize: 9, fontFace: "Arial", color: theme.accent,
      charSpacing: 3, align: "left", valign: "middle", margin: 0
    });

    // Nodes
    p.nodes.forEach((n, j) => {
      const ny = prodY + 0.7 + j * 0.54;
      // Tag
      slide.addShape(pres.shapes.RECTANGLE, {
        x: x + 0.12, y: ny, w: 1.0, h: 0.22,
        fill: { color: theme.accent }, line: { type: "none" }, rectRadius: 0.02
      });
      slide.addText(n.tag, {
        x: x + 0.12, y: ny, w: 1.0, h: 0.22,
        fontSize: 8, fontFace: "Microsoft YaHei", color: "FFFFFF",
        bold: true, align: "center", valign: "middle", margin: 0
      });
      // Text
      slide.addText(n.txt, {
        x: x + 0.12, y: ny + 0.23, w: prodW - 0.24, h: 0.3,
        fontSize: 8, fontFace: "Microsoft YaHei", color: theme.primary,
        align: "left", valign: "top", margin: 0
      });
    });
  });

  // Bottom callout
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: 5.2, w: 9, h: 0.32,
    fill: { color: theme.accent, transparency: 85 }, line: { type: "none" }, rectRadius: 0.03
  });
  slide.addText("【细节点 9】话术要点:每个产品类别必含「风险揭示 + 适当性匹配 + 客户确认」三段,且必须由客户亲口确认。", {
    x: 0.6, y: 5.2, w: 8.8, h: 0.32,
    fontSize: 10, fontFace: "Microsoft YaHei", color: theme.primary,
    bold: true, align: "left", valign: "middle", margin: 0
  });

  slide.addText("11 / 16", {
    x: 9.0, y: 5.55, w: 0.9, h: 0.07,
    fontSize: 8, fontFace: "Arial", color: theme.secondary,
    align: "right"
  });
}

module.exports = { createSlide };
