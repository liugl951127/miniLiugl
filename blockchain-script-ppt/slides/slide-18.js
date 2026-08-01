// 多端协同 + 智能交互
function createSlide(pres, theme) {
  const slide = pres.addSlide();
  slide.background = { color: theme.bg };

  // Title
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: 0.4, w: 0.08, h: 0.5,
    fill: { color: theme.accent }, line: { type: "none" }
  });
  slide.addText("多端协同 + 智能交互:让客户「不知不觉」走完", {
    x: 0.7, y: 0.4, w: 8.0, h: 0.5,
    fontSize: 19, fontFace: "Microsoft YaHei", color: theme.primary,
    bold: true, align: "left", valign: "middle", margin: 0
  });
  slide.addText("MULTI-DEVICE", {
    x: 8.0, y: 0.5, w: 1.5, h: 0.3,
    fontSize: 10, fontFace: "Arial", color: theme.secondary,
    charSpacing: 3, align: "right"
  });

  // 4 智能特性
  const features = [
    {
      icon: "🎙️", title: "智能跟读",
      online: "ASR 实时识别,自动跳转",
      offline: "PAD 屏幕提示,经理引导"
    },
    {
      icon: "⏸️", title: "智能暂停",
      online: "客户犹豫 > 5s 自动暂停",
      offline: "经理可主动暂停讲解"
    },
    {
      icon: "🎯", title: "情绪感知",
      online: "摄像头 + 声纹分析",
      offline: "经理面对面观察"
    },
    {
      icon: "🔁", title: "无感续接",
      online: "退出再进,自动恢复进度",
      offline: "断网恢复,本地缓存"
    },
  ];

  const fW = 2.15;
  const fH = 1.85;
  const fGap = 0.15;
  const fStartX = 0.5;
  const fY = 1.05;

  features.forEach((f, i) => {
    const x = fStartX + i * (fW + fGap);
    slide.addShape(pres.shapes.RECTANGLE, {
      x: x, y: fY, w: fW, h: fH,
      fill: { color: theme.light }, line: { type: "none" }, rectRadius: 0.05
    });
    slide.addShape(pres.shapes.RECTANGLE, {
      x: x, y: fY, w: fW, h: 0.05,
      fill: { color: theme.accent }, line: { type: "none" }
    });
    slide.addText(f.icon + "  " + f.title, {
      x: x, y: fY + 0.15, w: fW, h: 0.4,
      fontSize: 14, fontFace: "Microsoft YaHei", color: theme.primary,
      bold: true, align: "center", valign: "middle", margin: 0
    });
    slide.addText("线上:" + f.online, {
      x: x + 0.1, y: fY + 0.7, w: fW - 0.2, h: 0.5,
      fontSize: 9.5, fontFace: "Microsoft YaHei", color: theme.accent,
      align: "left", valign: "top", margin: 0
    });
    slide.addText("线下:" + f.offline, {
      x: x + 0.1, y: fY + 1.2, w: fW - 0.2, h: 0.5,
      fontSize: 9.5, fontFace: "Microsoft YaHei", color: theme.primary,
      align: "left", valign: "top", margin: 0
    });
  });

  // 智能交互设计
  const aiY = 3.1;
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: aiY, w: 9, h: 1.95,
    fill: { color: theme.primary }, line: { type: "none" }, rectRadius: 0.05
  });
  slide.addText("【智能交互设计】让客户每一步都「刚刚好」", {
    x: 0.7, y: aiY + 0.1, w: 8.6, h: 0.35,
    fontSize: 13, fontFace: "Microsoft YaHei", color: theme.accent,
    bold: true, align: "left", valign: "middle", margin: 0
  });

  const interactions = [
    { num: "①", t: "进度可视化", d: "进度条 + 当前节点高亮,客户随时知道还要多久" },
    { num: "②", t: "智能预判", d: "根据历史办理速度,预测剩余时间,降低焦虑" },
    { num: "③", t: "中途休息", d: "允许客户申请 5 分钟休息,流程不丢、可续接" },
    { num: "④", t: "隐私保护", d: "人脸/证件画面可关闭预览,只在录制的视频中存在" },
    { num: "⑤", t: "回放确认", d: "完成前给客户 30s 回看时间,任何环节都可反悔" },
    { num: "⑥", t: "陪伴模式", d: "客户可邀请家人一起观看,多人见证" },
  ];

  interactions.forEach((it, i) => {
    const col = i % 2;
    const row = Math.floor(i / 2);
    const x = 0.7 + col * 4.3;
    const y = aiY + 0.55 + row * 0.45;
    slide.addText(it.num, {
      x: x, y: y, w: 0.3, h: 0.4,
      fontSize: 16, fontFace: "Arial", color: theme.accent,
      bold: true, align: "left", valign: "middle", margin: 0
    });
    slide.addText(it.t, {
      x: x + 0.3, y: y, w: 1.3, h: 0.4,
      fontSize: 10, fontFace: "Microsoft YaHei", color: "FFFFFF",
      bold: true, align: "left", valign: "middle", margin: 0
    });
    slide.addText(it.d, {
      x: x + 1.6, y: y, w: 2.65, h: 0.4,
      fontSize: 9, fontFace: "Microsoft YaHei", color: theme.light,
      align: "left", valign: "middle", margin: 0
    });
  });

  // 细节点
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: 5.2, w: 9, h: 0.32,
    fill: { color: theme.accent, transparency: 85 }, line: { type: "none" }, rectRadius: 0.03
  });
  slide.addText("【细节点 12】智能暂停让客户掌握节奏,流失率降低 40%,复购率提升 25%。", {
    x: 0.6, y: 5.2, w: 8.8, h: 0.32,
    fontSize: 10, fontFace: "Microsoft YaHei", color: theme.primary,
    bold: true, align: "left", valign: "middle", margin: 0
  });

  slide.addText("18 / 19", {
    x: 9.0, y: 5.55, w: 0.9, h: 0.07,
    fontSize: 8, fontFace: "Arial", color: theme.secondary,
    align: "right"
  });
}

module.exports = { createSlide };
