// 客户旅程地图 + 痛点识别
function createSlide(pres, theme) {
  const slide = pres.addSlide();
  slide.background = { color: theme.bg };

  // Title
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: 0.4, w: 0.08, h: 0.5,
    fill: { color: theme.accent }, line: { type: "none" }
  });
  slide.addText("客户旅程地图:从「抗拒」到「信任」的 5 段心路", {
    x: 0.7, y: 0.4, w: 8.0, h: 0.5,
    fontSize: 19, fontFace: "Microsoft YaHei", color: theme.primary,
    bold: true, align: "left", valign: "middle", margin: 0
  });
  slide.addText("CUSTOMER JOURNEY", {
    x: 7.5, y: 0.5, w: 2.0, h: 0.3,
    fontSize: 10, fontFace: "Arial", color: theme.secondary,
    charSpacing: 3, align: "right"
  });

  // 5 stages of journey
  const stages = [
    {
      num: "01", t: "被邀请", emotion: "😐", emotionL: "无所谓",
      action: "客户被理财经理或 APP 推送邀约",
      pain: "「又来推销?」"
    },
    {
      num: "02", t: "了解产品", emotion: "🤔", emotionL: "有兴趣",
      action: "看到产品介绍,觉得有点意思",
      pain: "「到底靠不靠谱?」"
    },
    {
      num: "03", t: "开始双录", emotion: "😟", emotionL: "担心",
      action: "进入双录流程,看到摄像头和录音",
      pain: "「这么麻烦?能省事点吗?」"
    },
    {
      num: "04", t: "完成签单", emotion: "😊", emotionL: "安心",
      action: "专业流程走完,看到回放和凭证",
      pain: "「原来也没那么难」"
    },
    {
      num: "05", t: "后续服务", emotion: "🤝", emotionL: "信任",
      action: "收到风险提示、回访、收益更新",
      pain: "「这家行挺靠谱的」"
    },
  ];

  const sW = 1.74;
  const sH = 2.3;
  const sGap = 0.1;
  const sStartX = 0.5;
  const sY = 1.1;

  stages.forEach((s, i) => {
    const x = sStartX + i * (sW + sGap);
    slide.addShape(pres.shapes.RECTANGLE, {
      x: x, y: sY, w: sW, h: sH,
      fill: { color: theme.light }, line: { type: "none" }, rectRadius: 0.05
    });
    // Number
    slide.addText(s.num, {
      x: x, y: sY + 0.1, w: sW, h: 0.35,
      fontSize: 22, fontFace: "Arial", color: theme.accent,
      bold: true, align: "center", valign: "middle", margin: 0
    });
    // Emotion emoji
    slide.addText(s.emotion, {
      x: x, y: sY + 0.5, w: sW, h: 0.5,
      fontSize: 28, fontFace: "Arial", color: theme.primary,
      align: "center", valign: "middle", margin: 0
    });
    slide.addText(s.emotionL, {
      x: x, y: sY + 1.0, w: sW, h: 0.25,
      fontSize: 10, fontFace: "Microsoft YaHei", color: theme.accent,
      align: "center", valign: "middle", margin: 0
    });
    // Stage title
    slide.addText(s.t, {
      x: x, y: sY + 1.3, w: sW, h: 0.3,
      fontSize: 13, fontFace: "Microsoft YaHei", color: theme.primary,
      bold: true, align: "center", valign: "middle", margin: 0
    });
    // Divider
    slide.addShape(pres.shapes.RECTANGLE, {
      x: x + 0.3, y: sY + 1.65, w: sW - 0.6, h: 0.02,
      fill: { color: theme.secondary }, line: { type: "none" }
    });
    // Action
    slide.addText(s.action, {
      x: x + 0.1, y: sY + 1.7, w: sW - 0.2, h: 0.4,
      fontSize: 8.5, fontFace: "Microsoft YaHei", color: theme.primary,
      align: "left", valign: "top", margin: 0
    });
    // Pain
    slide.addText(s.pain, {
      x: x + 0.1, y: sY + 2.0, w: sW - 0.2, h: 0.3,
      fontSize: 9, fontFace: "Microsoft YaHei", color: theme.accent,
      italic: true, align: "left", valign: "top", margin: 0
    });
  });

  // Arrows between stages
  for (let i = 0; i < 4; i++) {
    const x = sStartX + (i + 1) * sW + i * sGap - 0.05;
    slide.addText("→", {
      x: x, y: sY + 0.7, w: 0.2, h: 0.3,
      fontSize: 16, fontFace: "Arial", color: theme.accent,
      bold: true, align: "center", valign: "middle", margin: 0
    });
  }

  // Critical moment callout
  const cmY = 3.6;
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: cmY, w: 9, h: 1.45,
    fill: { color: theme.primary }, line: { type: "none" }, rectRadius: 0.05
  });
  slide.addText("【关键卡点】Stage 03「开始双录」是流失率最高的环节(流失 30%)", {
    x: 0.7, y: cmY + 0.1, w: 8.6, h: 0.3,
    fontSize: 13, fontFace: "Microsoft YaHei", color: theme.accent,
    bold: true, align: "left", valign: "middle", margin: 0
  });
  slide.addText("3 大客户抗拒心理:怕麻烦 / 怕泄露 / 怕不通过。", {
    x: 0.7, y: cmY + 0.45, w: 8.6, h: 0.3,
    fontSize: 11, fontFace: "Microsoft YaHei", color: "FFFFFF",
    align: "left", valign: "middle", margin: 0
  });

  const fixes = [
    "怕麻烦 → 5 分钟极速办理 + 智能跳过已确认项",
    "怕泄露 → 实时水印 + 隐私条款可视化",
    "怕不通过 → 预审模式,提前告知通过率"
  ];
  fixes.forEach((f, i) => {
    const y = cmY + 0.8 + i * 0.18;
    slide.addText("· " + f, {
      x: 1.0, y: y, w: 8.3, h: 0.2,
      fontSize: 10, fontFace: "Microsoft YaHei", color: theme.light,
      align: "left", valign: "middle", margin: 0
    });
  });

  // 细节点
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: 5.2, w: 9, h: 0.32,
    fill: { color: theme.accent, transparency: 85 }, line: { type: "none" }, rectRadius: 0.03
  });
  slide.addText("【细节点 11】客户 NPS 提升 10 分 = 业务转化率 +5%,体验感 = 商业价值。", {
    x: 0.6, y: 5.2, w: 8.8, h: 0.32,
    fontSize: 10, fontFace: "Microsoft YaHei", color: theme.primary,
    bold: true, align: "left", valign: "middle", margin: 0
  });

  slide.addText("17 / 19", {
    x: 9.0, y: 5.55, w: 0.9, h: 0.07,
    fontSize: 8, fontFace: "Arial", color: theme.secondary,
    align: "right"
  });
}

module.exports = { createSlide };
