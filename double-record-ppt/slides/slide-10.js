// Exception handling flow
function createSlide(pres, theme) {
  const slide = pres.addSlide();
  slide.background = { color: theme.bg };

  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: 0.4, w: 0.08, h: 0.5,
    fill: { color: theme.accent }, line: { type: "none" }
  });
  slide.addText("异常分支流程:确保客户「走得完」", {
    x: 0.7, y: 0.4, w: 7.5, h: 0.5,
    fontSize: 20, fontFace: "Microsoft YaHei", color: theme.primary,
    bold: true, align: "left", valign: "middle", margin: 0
  });
  slide.addText("EXCEPTION HANDLING", {
    x: 7.0, y: 0.5, w: 2.5, h: 0.3,
    fontSize: 10, fontFace: "Arial", color: theme.secondary,
    charSpacing: 4, align: "right"
  });

  // Three exception types
  const exY = 1.2;
  const exH = 1.85;
  const exW = 2.9;
  const exGap = 0.15;

  const exceptions = [
    {
      t: "技术异常",
      icon: "!",
      cases: ["网络断线", "设备故障", "音视频异常", "OSS 上传失败"],
      action: "本地缓存 + 断点续传 + 自动告警"
    },
    {
      t: "流程异常",
      icon: "?",
      cases: ["客户拒答", "话术中断", "中途离席", "二次确认失败"],
      action: "重新进入该节点 + 客户经理介入"
    },
    {
      t: "合规异常",
      icon: "X",
      cases: ["风险不匹配", "证件失效", "非本人办理", "高风险反洗钱"],
      action: "阻断流程 + 升级审核 + 留痕上报"
    }
  ];

  exceptions.forEach((e, i) => {
    const x = 0.5 + i * (exW + exGap);
    // Card
    slide.addShape(pres.shapes.RECTANGLE, {
      x: x, y: exY, w: exW, h: exH,
      fill: { color: theme.light }, line: { type: "none" }, rectRadius: 0.05
    });
    // Top icon bar
    slide.addShape(pres.shapes.RECTANGLE, {
      x: x, y: exY, w: exW, h: 0.5,
      fill: { color: theme.accent }, line: { type: "none" }, rectRadius: 0.05
    });
    slide.addText(e.icon, {
      x: x + 0.15, y: exY + 0.08, w: 0.4, h: 0.35,
      fontSize: 18, fontFace: "Arial", color: "FFFFFF",
      bold: true, align: "center", valign: "middle", margin: 0
    });
    slide.addText(e.t, {
      x: x + 0.6, y: exY, w: exW - 0.7, h: 0.5,
      fontSize: 14, fontFace: "Microsoft YaHei", color: "FFFFFF",
      bold: true, align: "left", valign: "middle", margin: 0
    });
    // Cases
    slide.addText("典型场景", {
      x: x + 0.15, y: exY + 0.6, w: exW - 0.3, h: 0.25,
      fontSize: 9, fontFace: "Microsoft YaHei", color: theme.secondary,
      align: "left", margin: 0
    });
    e.cases.forEach((c, j) => {
      slide.addText("· " + c, {
        x: x + 0.2, y: exY + 0.85 + j * 0.2, w: exW - 0.3, h: 0.2,
        fontSize: 9, fontFace: "Microsoft YaHei", color: theme.primary,
        align: "left", valign: "middle", margin: 0
      });
    });
    // Action
    slide.addShape(pres.shapes.RECTANGLE, {
      x: x + 0.1, y: exY + exH - 0.4, w: exW - 0.2, h: 0.3,
      fill: { color: theme.primary }, line: { type: "none" }, rectRadius: 0.03
    });
    slide.addText("▶ " + e.action, {
      x: x + 0.1, y: exY + exH - 0.4, w: exW - 0.2, h: 0.3,
      fontSize: 9, fontFace: "Microsoft YaHei", color: "FFFFFF",
      bold: true, align: "center", valign: "middle", margin: 0
    });
  });

  // Customer protection mechanism
  const protY = 3.3;
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: protY, w: 9, h: 1.8,
    fill: { color: theme.primary }, line: { type: "none" }, rectRadius: 0.05
  });
  slide.addText("客户保护机制 - 保证「能正常走完购买流程」", {
    x: 0.7, y: protY + 0.1, w: 8, h: 0.35,
    fontSize: 14, fontFace: "Microsoft YaHei", color: "FFFFFF",
    bold: true, align: "left", valign: "middle", margin: 0
  });

  const protections = [
    { t: "多端续接", d: "线上中断 → 可在线下继续,反之亦然" },
    { t: "暂存恢复", d: "本地加密暂存 24h,网络恢复自动续传" },
    { t: "远程协助", d: "客户经理远程接管 + 屏幕共享" },
    { t: "二次预约", d: "本次失败 → 自动 7 天内可重新预约" },
    { t: "人工兜底", d: "复杂情况转 955xx 人工坐席" },
    { t: "短信进度", d: "每完成 1 个节点 → 短信通知客户" }
  ];

  protections.forEach((p, i) => {
    const col = i % 3;
    const row = Math.floor(i / 3);
    const x = 0.7 + col * 3.0;
    const y = protY + 0.55 + row * 0.55;
    slide.addShape(pres.shapes.RECTANGLE, {
      x: x, y: y, w: 0.12, h: 0.4,
      fill: { color: theme.accent }, line: { type: "none" }
    });
    slide.addText(p.t, {
      x: x + 0.2, y: y, w: 2.7, h: 0.2,
      fontSize: 11, fontFace: "Microsoft YaHei", color: "FFFFFF",
      bold: true, align: "left", valign: "middle", margin: 0
    });
    slide.addText(p.d, {
      x: x + 0.2, y: y + 0.2, w: 2.7, h: 0.2,
      fontSize: 8, fontFace: "Microsoft YaHei", color: theme.light,
      align: "left", valign: "middle", margin: 0
    });
  });

  // Bottom callout
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: 5.2, w: 9, h: 0.32,
    fill: { color: theme.accent, transparency: 85 }, line: { type: "none" }, rectRadius: 0.03
  });
  slide.addText("【细节点 8】设计原则:任何异常都不能让客户「卡在流程里」,必须有明确的恢复路径或人工兜底。", {
    x: 0.6, y: 5.2, w: 8.8, h: 0.32,
    fontSize: 10, fontFace: "Microsoft YaHei", color: theme.primary,
    bold: true, align: "left", valign: "middle", margin: 0
  });

  slide.addText("10 / 16", {
    x: 9.0, y: 5.55, w: 0.9, h: 0.07,
    fontSize: 8, fontFace: "Arial", color: theme.secondary,
    align: "right"
  });
}

module.exports = { createSlide };
