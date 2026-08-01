// 监管对接 + 司法采信
function createSlide(pres, theme) {
  const slide = pres.addSlide();
  slide.background = { color: theme.bg };

  // Title
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: 0.4, w: 0.08, h: 0.5,
    fill: { color: theme.accent }, line: { type: "none" }
  });
  slide.addText("监管对接 + 司法采信:让证据「真正管用」", {
    x: 0.7, y: 0.4, w: 7.5, h: 0.5,
    fontSize: 20, fontFace: "Microsoft YaHei", color: theme.primary,
    bold: true, align: "left", valign: "middle", margin: 0
  });
  slide.addText("COMPLIANCE", {
    x: 8.2, y: 0.5, w: 1.3, h: 0.3,
    fontSize: 10, fontFace: "Arial", color: theme.secondary,
    charSpacing: 3, align: "right"
  });

  // 监管对接 - 左侧
  const regX = 0.5;
  slide.addText("监管对接模式", {
    x: regX, y: 1.0, w: 4.5, h: 0.3,
    fontSize: 13, fontFace: "Microsoft YaHei", color: theme.accent,
    bold: true, align: "left", margin: 0
  });

  // 监管节点示意
  slide.addShape(pres.shapes.RECTANGLE, {
    x: regX, y: 1.4, w: 4.5, h: 0.55,
    fill: { color: theme.primary }, line: { type: "none" }, rectRadius: 0.03
  });
  slide.addText("本行双录节点", {
    x: regX + 0.15, y: 1.4, w: 1.5, h: 0.55,
    fontSize: 11, fontFace: "Microsoft YaHei", color: "FFFFFF",
    bold: true, align: "left", valign: "middle", margin: 0
  });
  slide.addText("背书 + 上链", {
    x: regX + 1.6, y: 1.4, w: 1.4, h: 0.55,
    fontSize: 10, fontFace: "Microsoft YaHei", color: theme.accent,
    align: "right", valign: "middle", margin: 0
  });

  // Arrow
  slide.addShape(pres.shapes.RIGHT_TRIANGLE, {
    x: 2.55, y: 2.0, w: 0.2, h: 0.25,
    fill: { color: theme.accent }, line: { type: "none" },
    rotate: 90
  });

  slide.addShape(pres.shapes.RECTANGLE, {
    x: regX, y: 2.3, w: 4.5, h: 0.55,
    fill: { color: theme.accent }, line: { type: "none" }, rectRadius: 0.03
  });
  slide.addText("监管节点(银保监)", {
    x: regX + 0.15, y: 2.3, w: 2.0, h: 0.55,
    fontSize: 11, fontFace: "Microsoft YaHei", color: "FFFFFF",
    bold: true, align: "left", valign: "middle", margin: 0
  });
  slide.addText("实时查询", {
    x: regX + 2.2, y: 2.3, w: 2.0, h: 0.55,
    fontSize: 10, fontFace: "Microsoft YaHei", color: "FFFFFF",
    align: "right", valign: "middle", margin: 0
  });

  // 监管模式
  const regModes = [
    { title: "白名单审计", desc: "监管可查询,不可篡改" },
    { title: "实时告警", desc: "异常交易 30s 内推送" },
    { title: "批量报送", desc: "T+1 自动化,无需人工" },
  ];
  regModes.forEach((m, i) => {
    const y = 3.0 + i * 0.45;
    slide.addShape(pres.shapes.RECTANGLE, {
      x: regX, y: y, w: 4.5, h: 0.4,
      fill: { color: theme.light }, line: { type: "none" }, rectRadius: 0.02
    });
    slide.addShape(pres.shapes.RECTANGLE, {
      x: regX, y: y, w: 0.08, h: 0.4,
      fill: { color: theme.accent }, line: { type: "none" }
    });
    slide.addText(m.title, {
      x: regX + 0.2, y: y, w: 1.3, h: 0.4,
      fontSize: 10, fontFace: "Microsoft YaHei", color: theme.primary,
      bold: true, align: "left", valign: "middle", margin: 0
    });
    slide.addText(m.desc, {
      x: regX + 1.5, y: y, w: 2.9, h: 0.4,
      fontSize: 9.5, fontFace: "Microsoft YaHei", color: theme.secondary,
      align: "left", valign: "middle", margin: 0
    });
  });

  // 司法采信 - 右侧
  const judX = 5.1;
  slide.addText("司法采信路径", {
    x: judX, y: 1.0, w: 4.4, h: 0.3,
    fontSize: 13, fontFace: "Microsoft YaHei", color: theme.accent,
    bold: true, align: "left", margin: 0
  });

  const judicial = [
    { step: "01", t: "区块链查验", d: "法官/律师可登录链上查询接口,实时校验证据" },
    { step: "02", t: "哈希校验", d: "原始数据 vs 链上哈希,任何差异立即可见" },
    { step: "03", t: "时间戳认证", d: "国家授时中心证明,法律认可的权威时间" },
    { step: "04", t: "多签见证", d: "银行 + 监管 + 保险公司 + 公证 4 方签名" },
    { step: "05", t: "证据包导出", d: "一键导出含视频+合同+链上证明,直接提交法院" },
  ];

  judicial.forEach((j, i) => {
    const y = 1.4 + i * 0.55;
    slide.addShape(pres.shapes.RECTANGLE, {
      x: judX, y: y, w: 4.4, h: 0.5,
      fill: { color: theme.light }, line: { type: "none" }, rectRadius: 0.03
    });
    slide.addShape(pres.shapes.RECTANGLE, {
      x: judX, y: y, w: 0.6, h: 0.5,
      fill: { color: theme.primary }, line: { type: "none" }, rectRadius: 0.03
    });
    slide.addText(j.step, {
      x: judX, y: y, w: 0.6, h: 0.5,
      fontSize: 14, fontFace: "Arial", color: "FFFFFF",
      bold: true, align: "center", valign: "middle", margin: 0
    });
    slide.addText(j.t, {
      x: judX + 0.7, y: y, w: 1.5, h: 0.5,
      fontSize: 11, fontFace: "Microsoft YaHei", color: theme.primary,
      bold: true, align: "left", valign: "middle", margin: 0
    });
    slide.addText(j.d, {
      x: judX + 2.2, y: y, w: 2.1, h: 0.5,
      fontSize: 9, fontFace: "Microsoft YaHei", color: theme.secondary,
      align: "left", valign: "middle", margin: 0
    });
  });

  // 细节点
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: 5.2, w: 9, h: 0.32,
    fill: { color: theme.accent, transparency: 85 }, line: { type: "none" }, rectRadius: 0.03
  });
  slide.addText("【细节点 7】《人民法院在线诉讼规则》明确认可区块链存证,本案架构完全符合司法采信 4 大要件。", {
    x: 0.6, y: 5.2, w: 8.8, h: 0.32,
    fontSize: 10, fontFace: "Microsoft YaHei", color: theme.primary,
    bold: true, align: "left", valign: "middle", margin: 0
  });

  slide.addText("11 / 19", {
    x: 9.0, y: 5.55, w: 0.9, h: 0.07,
    fontSize: 8, fontFace: "Arial", color: theme.secondary,
    align: "right"
  });
}

module.exports = { createSlide };
