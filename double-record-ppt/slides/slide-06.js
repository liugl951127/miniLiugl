// Pain Point 2: Dual recording compliance
function createSlide(pres, theme) {
  const slide = pres.addSlide();
  slide.background = { color: theme.bg };

  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: 0.4, w: 0.08, h: 0.5,
    fill: { color: theme.accent }, line: { type: "none" }
  });
  slide.addText("方案二:全场景双录合规 - 解决「视频合规性」痛点", {
    x: 0.7, y: 0.4, w: 6.8, h: 0.5,
    fontSize: 19, fontFace: "Microsoft YaHei", color: theme.primary,
    bold: true, align: "left", valign: "middle", margin: 0
  });
  slide.addText("PAIN POINT 02/5", {
    x: 7.6, y: 0.5, w: 1.9, h: 0.3,
    fontSize: 10, fontFace: "Arial", color: theme.secondary,
    charSpacing: 3, align: "right"
  });

  // Pain point badge
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: 1.05, w: 9, h: 0.4,
    fill: { color: theme.accent, transparency: 88 }, line: { type: "none" }, rectRadius: 0.03
  });
  slide.addText("【痛点 2】线下视频易篡改/丢失/介质不一,合规证据链不完整。", {
    x: 0.6, y: 1.05, w: 8.8, h: 0.4,
    fontSize: 11, fontFace: "Microsoft YaHei", color: theme.accent,
    bold: true, align: "left", valign: "middle", margin: 0
  });

  // Three channels
  const chanY = 1.65;
  const chanH = 2.2;
  const chanW = 2.85;
  const chanGap = 0.22;
  const chanStartX = 0.5;

  const channels = [
    {
      name: "线上双录",
      icon: "WEB",
      points: [
        "音视频 SDK 内嵌,边录边传",
        "实时水印 + 时间戳(可信时间)",
        "断点续传,失败自动重连",
        "双流(人脸 + 凭证)同步录制"
      ]
    },
    {
      name: "线下一体机",
      icon: "ATM",
      points: [
        "嵌入式定制设备,防拆卸",
        "本地暂存 + 4G 实时回传",
        "国密 SM4 加密芯片",
        "活体检测 + 证件 OCR 联动"
      ]
    },
    {
      name: "移动展业",
      icon: "APP",
      points: [
        "PAD/手机端 APP 内置双录 SDK",
        "客户经理外拓(社区/上门)",
        "GPS + WIFI 定位佐证",
        "完成后立即加密上传"
      ]
    }
  ];

  channels.forEach((c, i) => {
    const x = chanStartX + i * (chanW + chanGap);
    slide.addShape(pres.shapes.RECTANGLE, {
      x: x, y: chanY, w: chanW, h: chanH,
      fill: { color: theme.light }, line: { type: "none" }, rectRadius: 0.05
    });
    // Top color bar
    slide.addShape(pres.shapes.RECTANGLE, {
      x: x, y: chanY, w: chanW, h: 0.6,
      fill: { color: theme.primary }, line: { type: "none" }, rectRadius: 0.05
    });
    slide.addText(c.icon, {
      x: x + 0.15, y: chanY + 0.05, w: 0.7, h: 0.5,
      fontSize: 18, fontFace: "Arial", color: theme.accent,
      bold: true, align: "center", valign: "middle", margin: 0
    });
    slide.addText(c.name, {
      x: x + 0.9, y: chanY, w: chanW - 1.0, h: 0.6,
      fontSize: 16, fontFace: "Microsoft YaHei", color: "FFFFFF",
      bold: true, align: "left", valign: "middle", margin: 0
    });
    c.points.forEach((p, j) => {
      const py = chanY + 0.75 + j * 0.34;
      slide.addShape(pres.shapes.RECTANGLE, {
        x: x + 0.2, y: py + 0.12, w: 0.08, h: 0.08,
        fill: { color: theme.accent }, line: { type: "none" }
      });
      slide.addText(p, {
        x: x + 0.35, y: py, w: chanW - 0.4, h: 0.32,
        fontSize: 10, fontFace: "Microsoft YaHei", color: theme.primary,
        align: "left", valign: "middle", margin: 0
      });
    });
  });

  // Bottom: compliance storage pipeline
  const pipeY = 4.0;
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: pipeY, w: 9, h: 1.1,
    fill: { color: theme.primary }, line: { type: "none" }, rectRadius: 0.05
  });
  slide.addText("合规证据链:全链路加密 + 区块链存证", {
    x: 0.7, y: pipeY + 0.08, w: 5, h: 0.3,
    fontSize: 12, fontFace: "Microsoft YaHei", color: "FFFFFF",
    bold: true, align: "left", margin: 0
  });

  const pipeSteps = ["录制采集", "SM4 加密", "OSS 存储", "SHA256 指纹", "区块链存证", "监管上报"];
  const pipeW = 1.3;
  const pipeGap = 0.1;
  const pipeStartX = 0.7;
  pipeSteps.forEach((s, i) => {
    const x = pipeStartX + i * (pipeW + pipeGap);
    slide.addShape(pres.shapes.RECTANGLE, {
      x: x, y: pipeY + 0.5, w: pipeW, h: 0.4,
      fill: { color: theme.accent }, line: { type: "none" }, rectRadius: 0.05
    });
    slide.addText(s, {
      x: x, y: pipeY + 0.5, w: pipeW, h: 0.4,
      fontSize: 10, fontFace: "Microsoft YaHei", color: "FFFFFF",
      bold: true, align: "center", valign: "middle", margin: 0
    });
    if (i < pipeSteps.length - 1) {
      slide.addText(">", {
        x: x + pipeW - 0.05, y: pipeY + 0.5, w: 0.2, h: 0.4,
        fontSize: 14, fontFace: "Arial", color: "FFFFFF",
        bold: true, align: "center", valign: "middle", margin: 0
      });
    }
  });

  // Bottom callout
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: 5.2, w: 9, h: 0.32,
    fill: { color: theme.accent, transparency: 85 }, line: { type: "none" }, rectRadius: 0.03
  });
  slide.addText("【细节点 4】合规要点:视频/音频/签名/凭证必须「同帧同步同存」,时间戳以国家授时中心为准。", {
    x: 0.6, y: 5.2, w: 8.8, h: 0.32,
    fontSize: 10, fontFace: "Microsoft YaHei", color: theme.primary,
    bold: true, align: "left", valign: "middle", margin: 0
  });

  slide.addText("06 / 16", {
    x: 9.0, y: 5.55, w: 0.9, h: 0.07,
    fontSize: 8, fontFace: "Arial", color: theme.secondary,
    align: "right"
  });
}

module.exports = { createSlide };
