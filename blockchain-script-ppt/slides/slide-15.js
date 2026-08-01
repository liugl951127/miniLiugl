// 多模态播报 + 同步协同
function createSlide(pres, theme) {
  const slide = pres.addSlide();
  slide.background = { color: theme.bg };

  // Title
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: 0.4, w: 0.08, h: 0.5,
    fill: { color: theme.accent }, line: { type: "none" }
  });
  slide.addText("多模态播报 + 同步协同:让客户感受「同一套流程」", {
    x: 0.7, y: 0.4, w: 8.0, h: 0.5,
    fontSize: 19, fontFace: "Microsoft YaHei", color: theme.primary,
    bold: true, align: "left", valign: "middle", margin: 0
  });
  slide.addText("03 / 3", {
    x: 8.7, y: 0.5, w: 0.8, h: 0.3,
    fontSize: 10, fontFace: "Arial", color: theme.secondary,
    charSpacing: 3, align: "right"
  });

  // 4 模态对比
  const modalities = [
    {
      name: "TTS 语音",
      icon: "🔊",
      online: "AI 合成女声,语速 0.85x",
      offline: "AI 合成女声,语速 0.85x(同源)",
      consistency: "100%",
      desc: "同一份话术库,同一 TTS 服务,音色一致"
    },
    {
      name: "字幕动效",
      icon: "📝",
      online: "高亮关键词 + 进度条",
      offline: "PAD 屏幕同步显示",
      consistency: "100%",
      desc: "客户低头/听不清时可看文字兜底"
    },
    {
      name: "背景音",
      icon: "🎵",
      online: "环境音自动降噪",
      offline: "营业网点统一背景音",
      consistency: "90%",
      desc: "提升专业感,降低客户焦虑"
    },
    {
      name: "交互反馈",
      icon: "👆",
      online: "按钮 + 倒计时",
      offline: "PAD 手写板 + 倒计时",
      consistency: "95%",
      desc: "强制确认位样式高度统一"
    },
  ];

  const mW = 2.15;
  const mH = 2.5;
  const mGap = 0.15;
  const mStartX = 0.5;
  const mY = 1.05;

  modalities.forEach((m, i) => {
    const x = mStartX + i * (mW + mGap);
    slide.addShape(pres.shapes.RECTANGLE, {
      x: x, y: mY, w: mW, h: mH,
      fill: { color: theme.light }, line: { type: "none" }, rectRadius: 0.05
    });
    slide.addShape(pres.shapes.RECTANGLE, {
      x: x, y: mY, w: mW, h: 0.5,
      fill: { color: theme.primary }, line: { type: "none" }, rectRadius: 0.05
    });
    slide.addText(m.icon + "  " + m.name, {
      x: x, y: mY, w: mW, h: 0.5,
      fontSize: 13, fontFace: "Microsoft YaHei", color: "FFFFFF",
      bold: true, align: "center", valign: "middle", margin: 0
    });

    slide.addText("线上 H5", {
      x: x + 0.1, y: mY + 0.6, w: mW - 0.2, h: 0.25,
      fontSize: 10, fontFace: "Microsoft YaHei", color: theme.accent,
      bold: true, align: "left", margin: 0
    });
    slide.addText(m.online, {
      x: x + 0.1, y: mY + 0.85, w: mW - 0.2, h: 0.4,
      fontSize: 9, fontFace: "Microsoft YaHei", color: theme.primary,
      align: "left", valign: "top", margin: 0
    });
    slide.addText("线下 PAD", {
      x: x + 0.1, y: mY + 1.3, w: mW - 0.2, h: 0.25,
      fontSize: 10, fontFace: "Microsoft YaHei", color: theme.accent,
      bold: true, align: "left", margin: 0
    });
    slide.addText(m.offline, {
      x: x + 0.1, y: mY + 1.55, w: mW - 0.2, h: 0.4,
      fontSize: 9, fontFace: "Microsoft YaHei", color: theme.primary,
      align: "left", valign: "top", margin: 0
    });

    // Consistency badge
    slide.addShape(pres.shapes.RECTANGLE, {
      x: x + 0.4, y: mY + 2.05, w: mW - 0.8, h: 0.35,
      fill: { color: theme.accent }, line: { type: "none" }, rectRadius: 0.03
    });
    slide.addText("一致度 " + m.consistency, {
      x: x + 0.4, y: mY + 2.05, w: mW - 0.8, h: 0.35,
      fontSize: 10, fontFace: "Microsoft YaHei", color: "FFFFFF",
      bold: true, align: "center", valign: "middle", margin: 0
    });
  });

  // 同步协同机制
  const synY = 3.75;
  slide.addText("【同步协同机制】双录会话的「主从同步」", {
    x: 0.5, y: synY, w: 9, h: 0.3,
    fontSize: 13, fontFace: "Microsoft YaHei", color: theme.accent,
    bold: true, align: "left", margin: 0
  });

  const sync = [
    { num: "①", t: "WebSocket 长连接", d: "客户端与服务器保持 30s 心跳,延迟 < 200ms" },
    { num: "②", t: "状态广播", d: "当前节点进度,所有端实时同步,不会走神" },
    { num: "③", t: "操作互锁", d: "客户在任一端确认,所有端立即同步状态" },
  ];
  sync.forEach((s, i) => {
    const y = synY + 0.4 + i * 0.35;
    slide.addText(s.num, {
      x: 0.5, y: y, w: 0.3, h: 0.3,
      fontSize: 14, fontFace: "Arial", color: theme.accent,
      bold: true, align: "left", valign: "middle", margin: 0
    });
    slide.addText(s.t, {
      x: 0.85, y: y, w: 2, h: 0.3,
      fontSize: 11, fontFace: "Microsoft YaHei", color: theme.primary,
      bold: true, align: "left", valign: "middle", margin: 0
    });
    slide.addText(s.d, {
      x: 2.9, y: y, w: 6.5, h: 0.3,
      fontSize: 10, fontFace: "Microsoft YaHei", color: theme.secondary,
      align: "left", valign: "middle", margin: 0
    });
  });

  // 细节点
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: 5.2, w: 9, h: 0.32,
    fill: { color: theme.accent, transparency: 85 }, line: { type: "none" }, rectRadius: 0.03
  });
  slide.addText("【细节点 10】TTS 服务统一采购阿里云,音色样本由总行固定,确保全国任何网点听到的声音一致。", {
    x: 0.6, y: 5.2, w: 8.8, h: 0.32,
    fontSize: 10, fontFace: "Microsoft YaHei", color: theme.primary,
    bold: true, align: "left", valign: "middle", margin: 0
  });

  slide.addText("15 / 19", {
    x: 9.0, y: 5.55, w: 0.9, h: 0.07,
    fontSize: 8, fontFace: "Arial", color: theme.secondary,
    align: "right"
  });
}

module.exports = { createSlide };
