// 区块链选型对比
function createSlide(pres, theme) {
  const slide = pres.addSlide();
  slide.background = { color: theme.bg };

  // Title
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: 0.4, w: 0.08, h: 0.5,
    fill: { color: theme.accent }, line: { type: "none" }
  });
  slide.addText("选型对比:联盟链 vs 公链 vs 私有链", {
    x: 0.7, y: 0.4, w: 7.5, h: 0.5,
    fontSize: 20, fontFace: "Microsoft YaHei", color: theme.primary,
    bold: true, align: "left", valign: "middle", margin: 0
  });
  slide.addText("02 / 4", {
    x: 8.2, y: 0.5, w: 1.3, h: 0.3,
    fontSize: 10, fontFace: "Arial", color: theme.secondary,
    charSpacing: 3, align: "right"
  });

  // Comparison table
  const headers = ["维度", "公链(比特币/以太坊)", "联盟链(Fabric/BCOS)", "私有链(单机构)"];
  const rows = [
    ["性能",        "5-30 TPS",          "1000-3000 TPS",     "5000+ TPS"],
    ["准入",        "完全开放",          "联盟成员准入",      "本机构内部"],
    ["监管友好",    "差(匿名)",          "优(可监管节点)",   "差(数据不共享)"],
    ["隐私保护",    "公钥地址",          "通道隔离+国密",    "内部可控"],
    ["司法采信",    "国际承认",          "国内司法承认",      "无第三方背书"],
    ["合规风险",    "不可控",            "可控(白名单)",      "单点风险"],
    ["改造成本",    "极高",              "中等",              "低"],
    ["推荐度",      "不推荐",            "★★★★★",            "★"],
  ];

  const tableX = 0.5;
  const tableY = 1.05;
  const tableW = 9.0;
  const headerH = 0.5;
  const rowH = 0.36;
  const colW = [1.3, 2.4, 2.6, 2.7];

  // Header
  headers.forEach((h, i) => {
    let x = tableX;
    for (let j = 0; j < i; j++) x += colW[j];
    slide.addShape(pres.shapes.RECTANGLE, {
      x: x, y: tableY, w: colW[i], h: headerH,
      fill: { color: theme.primary }, line: { type: "none" }
    });
    slide.addText(h, {
      x: x, y: tableY, w: colW[i], h: headerH,
      fontSize: 11, fontFace: "Microsoft YaHei", color: "FFFFFF",
      bold: true, align: "center", valign: "middle", margin: 0
    });
  });

  // Rows
  rows.forEach((row, r) => {
    const y = tableY + headerH + r * rowH;
    row.forEach((cell, i) => {
      let x = tableX;
      for (let j = 0; j < i; j++) x += colW[j];

      // 推荐度 行用主色填充
      const isRecommendRow = (r === rows.length - 1);
      // 联盟链列(列索引2)特殊高亮
      const isHighlightCol = (i === 2);
      const bgColor = isHighlightCol ? "E8F5E9" : (r % 2 === 0 ? theme.light : "FFFFFF");

      slide.addShape(pres.shapes.RECTANGLE, {
        x: x, y: y, w: colW[i], h: rowH,
        fill: { color: isHighlightCol ? "E8F5E9" : (r % 2 === 0 ? theme.light : "FFFFFF") },
        line: { color: "DDDDDD", width: 0.5 }
      });
      // 文字颜色
      let textColor = theme.primary;
      let isBold = false;
      if (i === 0) {
        isBold = true;
        textColor = theme.primary;
      }
      if (isHighlightCol && isRecommendRow) {
        textColor = theme.accent;
        isBold = true;
      }

      slide.addText(cell, {
        x: x + 0.05, y: y, w: colW[i] - 0.1, h: rowH,
        fontSize: 10, fontFace: "Microsoft YaHei", color: textColor,
        bold: isBold, align: "center", valign: "middle", margin: 0
      });
    });
  });

  // 选型结论
  const recY = 4.3;
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: recY, w: 9, h: 0.85,
    fill: { color: theme.light }, line: { color: theme.accent, width: 1 }, rectRadius: 0.05
  });
  slide.addText("【选型结论】", {
    x: 0.7, y: recY + 0.08, w: 1.5, h: 0.3,
    fontSize: 11, fontFace: "Microsoft YaHei", color: theme.accent,
    bold: true, align: "left", valign: "middle", margin: 0
  });
  slide.addText("采用 Hyperledger Fabric 联盟链,4 节点共识 + 国密 SM2/SM3/SM4 集成 + 监管节点接入,在性能、合规、采信间取得平衡。", {
    x: 0.7, y: recY + 0.35, w: 8.6, h: 0.5,
    fontSize: 11, fontFace: "Microsoft YaHei", color: theme.primary,
    align: "left", valign: "top", margin: 0
  });

  // Bottom callout
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: 5.2, w: 9, h: 0.32,
    fill: { color: theme.accent, transparency: 85 }, line: { type: "none" }, rectRadius: 0.03
  });
  slide.addText("【细节点 2】联盟链成员 = 本行 + 银保监 + 客户保险公司 + 公证处,共同维护账本,任何一方无法单独篡改。", {
    x: 0.6, y: 5.2, w: 8.8, h: 0.32,
    fontSize: 10, fontFace: "Microsoft YaHei", color: theme.primary,
    bold: true, align: "left", valign: "middle", margin: 0
  });

  slide.addText("05 / 19", {
    x: 9.0, y: 5.55, w: 0.9, h: 0.07,
    fontSize: 8, fontFace: "Arial", color: theme.secondary,
    align: "right"
  });
}

module.exports = { createSlide };
