// Pain Point 4: Data atomicity and consistency
function createSlide(pres, theme) {
  const slide = pres.addSlide();
  slide.background = { color: theme.bg };

  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: 0.4, w: 0.08, h: 0.5,
    fill: { color: theme.accent }, line: { type: "none" }
  });
  slide.addText("方案四:数据一致性 - 解决「流程原子性」痛点", {
    x: 0.7, y: 0.4, w: 7.5, h: 0.5,
    fontSize: 20, fontFace: "Microsoft YaHei", color: theme.primary,
    bold: true, align: "left", valign: "middle", margin: 0
  });
  slide.addText("PAIN POINT 04/5", {
    x: 7.0, y: 0.5, w: 2.5, h: 0.3,
    fontSize: 10, fontFace: "Arial", color: theme.secondary,
    charSpacing: 3, align: "right"
  });

  // Pain point badge
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: 1.05, w: 9, h: 0.4,
    fill: { color: theme.accent, transparency: 88 }, line: { type: "none" }, rectRadius: 0.03
  });
  slide.addText("【痛点 4】双录视频、订单、合同、CRM 状态分散,任一环节失败导致数据不一致。", {
    x: 0.6, y: 1.05, w: 8.8, h: 0.4,
    fontSize: 11, fontFace: "Microsoft YaHei", color: theme.accent,
    bold: true, align: "left", valign: "middle", margin: 0
  });

  // State machine flow
  const smY = 1.7;
  slide.addText("双录订单状态机(全流程原子化)", {
    x: 0.5, y: smY, w: 9, h: 0.3,
    fontSize: 14, fontFace: "Microsoft YaHei", color: theme.primary,
    bold: true, align: "left", margin: 0
  });

  const states = [
    { s: "已预约", c: theme.secondary, n: "S0" },
    { s: "客户已核验", c: theme.secondary, n: "S1" },
    { s: "话术执行中", c: theme.secondary, n: "S2" },
    { s: "视频录制中", c: theme.primary, n: "S3" },
    { s: "电子签约", c: theme.primary, n: "S4" },
    { s: "质检通过", c: "2A9D8F", n: "S5" },
    { s: "订单完成", c: theme.accent, n: "S6" }
  ];

  const stateW = 1.18;
  const stateGap = 0.08;
  const stateStartX = 0.5;
  states.forEach((st, i) => {
    const x = stateStartX + i * (stateW + stateGap);
    slide.addShape(pres.shapes.RECTANGLE, {
      x: x, y: smY + 0.4, w: stateW, h: 0.7,
      fill: { color: st.c }, line: { type: "none" }, rectRadius: 0.05
    });
    slide.addText(st.n, {
      x: x, y: smY + 0.42, w: stateW, h: 0.25,
      fontSize: 9, fontFace: "Arial", color: "FFFFFF",
      align: "center", valign: "middle", margin: 0
    });
    slide.addText(st.s, {
      x: x, y: smY + 0.65, w: stateW, h: 0.4,
      fontSize: 10, fontFace: "Microsoft YaHei", color: "FFFFFF",
      bold: true, align: "center", valign: "middle", margin: 0
    });
    if (i < states.length - 1) {
      slide.addText(">", {
        x: x + stateW - 0.02, y: smY + 0.4, w: 0.12, h: 0.7,
        fontSize: 16, fontFace: "Arial", color: theme.accent,
        bold: true, align: "center", valign: "middle", margin: 0
      });
    }
  });

  // Saga distributed transaction explanation
  const sagaY = 3.0;
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: sagaY, w: 9, h: 2.0,
    fill: { color: theme.light }, line: { type: "none" }, rectRadius: 0.05
  });
  slide.addText("分布式事务:Saga + 状态机补偿", {
    x: 0.7, y: sagaY + 0.1, w: 5, h: 0.3,
    fontSize: 13, fontFace: "Microsoft YaHei", color: theme.primary,
    bold: true, align: "left", margin: 0
  });

  const sagaSteps = [
    { t: "T1 预占资源", d: "创建订单主记录 + 视频 OSS 预分配路径" },
    { t: "T2 双录录制", d: "采集音视频 → 加密 → 上传 → SHA256 校验" },
    { t: "T3 风险评估", d: "调用风评引擎,生成风评结果并落库" },
    { t: "T4 电子签约", d: "客户人脸 + 意愿确认 → CA 数字证书签名" },
    { t: "T5 质检任务", d: "异步发起智能质检,不阻塞主流程" },
    { t: "T6 订单回写", d: "CRM/核心系统同步,失败进入补偿队列" }
  ];

  sagaSteps.forEach((s, i) => {
    const col = i % 2;
    const row = Math.floor(i / 2);
    const x = 0.7 + col * 4.4;
    const y = sagaY + 0.5 + row * 0.5;
    // Step badge
    slide.addShape(pres.shapes.RECTANGLE, {
      x: x, y: y, w: 0.7, h: 0.32,
      fill: { color: theme.accent }, line: { type: "none" }, rectRadius: 0.03
    });
    slide.addText(s.t.split(" ")[0], {
      x: x, y: y, w: 0.7, h: 0.32,
      fontSize: 9, fontFace: "Arial", color: "FFFFFF",
      bold: true, align: "center", valign: "middle", margin: 0
    });
    slide.addText(s.d, {
      x: x + 0.8, y: y, w: 3.5, h: 0.32,
      fontSize: 10, fontFace: "Microsoft YaHei", color: theme.primary,
      align: "left", valign: "middle", margin: 0
    });
  });

  // Bottom callout
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: 5.2, w: 9, h: 0.32,
    fill: { color: theme.accent, transparency: 85 }, line: { type: "none" }, rectRadius: 0.03
  });
  slide.addText("【细节点 6】补偿机制:任一步失败 → 自动回滚已占用资源 + 状态回退 + 通知客户经理,不留数据空洞。", {
    x: 0.6, y: 5.2, w: 8.8, h: 0.32,
    fontSize: 10, fontFace: "Microsoft YaHei", color: theme.primary,
    bold: true, align: "left", valign: "middle", margin: 0
  });

  slide.addText("08 / 16", {
    x: 9.0, y: 5.55, w: 0.9, h: 0.07,
    fontSize: 8, fontFace: "Arial", color: theme.secondary,
    align: "right"
  });
}

module.exports = { createSlide };
