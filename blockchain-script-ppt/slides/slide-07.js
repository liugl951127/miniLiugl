// 数据上链模型:什么上链、什么不上链
function createSlide(pres, theme) {
  const slide = pres.addSlide();
  slide.background = { color: theme.bg };

  // Title
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: 0.4, w: 0.08, h: 0.5,
    fill: { color: theme.accent }, line: { type: "none" }
  });
  slide.addText("数据上链模型:什么上链、什么不上链", {
    x: 0.7, y: 0.4, w: 7.5, h: 0.5,
    fontSize: 20, fontFace: "Microsoft YaHei", color: theme.primary,
    bold: true, align: "left", valign: "middle", margin: 0
  });
  slide.addText("04 / 4", {
    x: 8.2, y: 0.5, w: 1.3, h: 0.3,
    fontSize: 10, fontFace: "Arial", color: theme.secondary,
    charSpacing: 3, align: "right"
  });

  // 上链 - 左侧
  const onX = 0.5;
  slide.addShape(pres.shapes.RECTANGLE, {
    x: onX, y: 1.1, w: 4.4, h: 0.5,
    fill: { color: theme.accent }, line: { type: "none" }, rectRadius: 0.03
  });
  slide.addText("✓ 上链内容(哈希指纹,不是原数据)", {
    x: onX + 0.15, y: 1.1, w: 4.1, h: 0.5,
    fontSize: 13, fontFace: "Microsoft YaHei", color: "FFFFFF",
    bold: true, align: "left", valign: "middle", margin: 0
  });

  const onChain = [
    { tag: "视频指纹", desc: "SHA-256 + SM3 双重哈希,256 bit" },
    { tag: "音频指纹", desc: "音频文件哈希,确保音轨未被替换" },
    { tag: "合同哈希", desc: "电子合同 PDF 哈希 + 签名" },
    { tag: "证据包摘要", desc: "Merkle Root 包含所有节点结果" },
    { tag: "可信时间戳", desc: "国家授时中心 NTP 校时结果" },
    { tag: "参与方签名", desc: "客户 + 经理 + 见证人 SM2 签名" },
  ];
  onChain.forEach((item, i) => {
    const y = 1.7 + i * 0.45;
    slide.addShape(pres.shapes.RECTANGLE, {
      x: onX, y: y, w: 4.4, h: 0.4,
      fill: { color: theme.light }, line: { type: "none" }, rectRadius: 0.02
    });
    slide.addShape(pres.shapes.RECTANGLE, {
      x: onX, y: y, w: 1.2, h: 0.4,
      fill: { color: theme.primary }, line: { type: "none" }, rectRadius: 0.02
    });
    slide.addText(item.tag, {
      x: onX, y: y, w: 1.2, h: 0.4,
      fontSize: 10, fontFace: "Microsoft YaHei", color: "FFFFFF",
      bold: true, align: "center", valign: "middle", margin: 0
    });
    slide.addText(item.desc, {
      x: onX + 1.3, y: y, w: 3.0, h: 0.4,
      fontSize: 10, fontFace: "Microsoft YaHei", color: theme.primary,
      align: "left", valign: "middle", margin: 0
    });
  });

  // 不上链 - 右侧
  const offX = 5.1;
  slide.addShape(pres.shapes.RECTANGLE, {
    x: offX, y: 1.1, w: 4.4, h: 0.5,
    fill: { color: theme.secondary }, line: { type: "none" }, rectRadius: 0.03
  });
  slide.addText("× 不上链(存对象存储,链下保留)", {
    x: offX + 0.15, y: 1.1, w: 4.1, h: 0.5,
    fontSize: 13, fontFace: "Microsoft YaHei", color: "FFFFFF",
    bold: true, align: "left", valign: "middle", margin: 0
  });

  const offChain = [
    { tag: "视频原文件", desc: "OSS 对象存储 10 年留存" },
    { tag: "音频原文件", desc: "OSS 标准存储,按需取用" },
    { tag: "合同 PDF", desc: "OSS + CDN 分发" },
    { tag: "客户身份证", desc: "加密存储,符合隐私法" },
    { tag: "人脸照片", desc: "OSS 加密,24h 自动清理" },
    { tag: "手写签名图", desc: "OSS 归档,与合同绑定" },
  ];
  offChain.forEach((item, i) => {
    const y = 1.7 + i * 0.45;
    slide.addShape(pres.shapes.RECTANGLE, {
      x: offX, y: y, w: 4.4, h: 0.4,
      fill: { color: theme.light }, line: { type: "none" }, rectRadius: 0.02
    });
    slide.addShape(pres.shapes.RECTANGLE, {
      x: offX, y: y, w: 1.2, h: 0.4,
      fill: { color: theme.secondary }, line: { type: "none" }, rectRadius: 0.02
    });
    slide.addText(item.tag, {
      x: offX, y: y, w: 1.2, h: 0.4,
      fontSize: 10, fontFace: "Microsoft YaHei", color: "FFFFFF",
      bold: true, align: "center", valign: "middle", margin: 0
    });
    slide.addText(item.desc, {
      x: offX + 1.3, y: y, w: 3.0, h: 0.4,
      fontSize: 10, fontFace: "Microsoft YaHei", color: theme.primary,
      align: "left", valign: "middle", margin: 0
    });
  });

  // Bottom callout
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: 4.55, w: 9, h: 0.6,
    fill: { color: theme.primary }, line: { type: "none" }, rectRadius: 0.05
  });
  slide.addText("【关键设计】上链存「指纹」、不上链存「数据」", {
    x: 0.7, y: 4.6, w: 8.6, h: 0.25,
    fontSize: 11, fontFace: "Microsoft YaHei", color: theme.accent,
    bold: true, align: "left", valign: "middle", margin: 0
  });
  slide.addText("链上验证指纹 + 链下取数据 = 兼顾存证严肃性与存储成本,数据可恢复、可校验、不可篡改。", {
    x: 0.7, y: 4.85, w: 8.6, h: 0.25,
    fontSize: 10, fontFace: "Microsoft YaHei", color: "FFFFFF",
    align: "left", valign: "middle", margin: 0
  });

  // 细节点
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: 5.2, w: 9, h: 0.32,
    fill: { color: theme.accent, transparency: 85 }, line: { type: "none" }, rectRadius: 0.03
  });
  slide.addText("【细节点 3】上链成本:单笔双录约 0.5 元(对比存证价值 1000 元+,ROI 2000 倍)。", {
    x: 0.6, y: 5.2, w: 8.8, h: 0.32,
    fontSize: 10, fontFace: "Microsoft YaHei", color: theme.primary,
    bold: true, align: "left", valign: "middle", margin: 0
  });

  slide.addText("07 / 19", {
    x: 9.0, y: 5.55, w: 0.9, h: 0.07,
    fontSize: 8, fontFace: "Arial", color: theme.secondary,
    align: "right"
  });
}

module.exports = { createSlide };
