// Detail annotation table for leadership report
function createSlide(pres, theme) {
  const slide = pres.addSlide();
  slide.background = { color: theme.bg };

  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: 0.4, w: 0.08, h: 0.5,
    fill: { color: theme.accent }, line: { type: "none" }
  });
  slide.addText("细节点标注附录:汇报/掌控双用途", {
    x: 0.7, y: 0.4, w: 7.5, h: 0.5,
    fontSize: 20, fontFace: "Microsoft YaHei", color: theme.primary,
    bold: true, align: "left", valign: "middle", margin: 0
  });
  slide.addText("ANNOTATION INDEX", {
    x: 7.0, y: 0.5, w: 2.5, h: 0.3,
    fontSize: 10, fontFace: "Arial", color: theme.secondary,
    charSpacing: 4, align: "right"
  });

  // Annotation table
  const tableY = 1.1;
  const tableData = [
    [
      { text: "编号", options: { bold: true, color: "FFFFFF", fill: { color: theme.primary }, align: "center" } },
      { text: "主题", options: { bold: true, color: "FFFFFF", fill: { color: theme.primary }, align: "center" } },
      { text: "细节点内容", options: { bold: true, color: "FFFFFF", fill: { color: theme.primary }, align: "center" } },
      { text: "适用场景", options: { bold: true, color: "FFFFFF", fill: { color: theme.primary }, align: "center" } }
    ],
    ["01", "痛点根源", "线上线下矛盾本质是「流程载体不同」,非「话术不同」", "战略汇报"],
    ["02", "架构核心", "流程编排 + 能力中台 + 数据治理 三层解耦", "技术评审"],
    ["03", "话术设计", "原子化(N1~Nn)+ 强制确认位,跳过即阻断", "业务宣讲"],
    ["04", "视频合规", "音视频/签名/凭证同帧同步同存,国家授时为准", "合规检查"],
    ["05", "质检一致", "同规则 + 同模型 + 同评分卡,结果可比可对账", "质控汇报"],
    ["06", "数据一致", "Saga 补偿 + 状态机回退,不留数据空洞", "架构评审"],
    ["07", "流程 Gate", "G1-G5 五道关卡,任一不过即挂起,不可跳过", "监管迎检"],
    ["08", "异常恢复", "任何异常必须有明确恢复路径或人工兜底", "客服/运营"],
    ["09", "话术原则", "必含「风险揭示 + 适当性匹配 + 客户确认」三段", "业务培训"],
    ["10", "风评红线", "评估结果与购买必须在同一次双录中", "监管红线"],
    ["11", "体验设计", "合规是下限,客户感知是上限,两者同时满足", "产品/UX"],
    ["12", "价值闭环", "监管 → 体验 → 效率 三层同时达成", "领导汇报"]
  ];

  // Build cell data
  const rows = tableData.slice(1).map(r => {
    return r.map((cell, ci) => {
      const base = { fontSize: 9, fontFace: "Microsoft YaHei", color: theme.primary, valign: "middle" };
      if (ci === 0) {
        return { text: cell, options: Object.assign({}, base, { align: "center", bold: true, color: theme.accent, fontFace: "Arial" }) };
      } else if (ci === 1) {
        return { text: cell, options: Object.assign({}, base, { align: "center", bold: true }) };
      } else if (ci === 2) {
        return { text: cell, options: Object.assign({}, base, { align: "left" }) };
      } else {
        return { text: cell, options: Object.assign({}, base, { align: "center", color: theme.secondary }) };
      }
    });
  });

  // Header row formatted
  const headerRow = tableData[0];
  const allRows = [headerRow].concat(rows);

  slide.addTable(allRows, {
    x: 0.5, y: tableY, w: 9.0, h: 4.0,
    colW: [0.6, 1.5, 5.0, 1.9],
    rowH: 0.3,
    border: { pt: 0.5, color: "D4D4D4" },
    fontSize: 9, fontFace: "Microsoft YaHei",
    fill: { color: "FFFFFF" }
  });

  // Bottom callout
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: 5.2, w: 9, h: 0.32,
    fill: { color: theme.accent, transparency: 85 }, line: { type: "none" }, rectRadius: 0.03
  });
  slide.addText("【细节点 13】使用建议:12 个细节点对应不同汇报对象,可在 PPT 备注页或附录中展开完整论述。", {
    x: 0.6, y: 5.2, w: 8.8, h: 0.32,
    fontSize: 10, fontFace: "Microsoft YaHei", color: theme.primary,
    bold: true, align: "left", valign: "middle", margin: 0
  });

  slide.addText("15 / 16", {
    x: 9.0, y: 5.55, w: 0.9, h: 0.07,
    fontSize: 8, fontFace: "Arial", color: theme.secondary,
    align: "right"
  });
}

module.exports = { createSlide };
