// Pain points analysis - online vs offline comparison
function createSlide(pres, theme) {
  const slide = pres.addSlide();
  slide.background = { color: theme.bg };

  // Title block
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: 0.4, w: 0.08, h: 0.5,
    fill: { color: theme.accent }, line: { type: "none" }
  });
  slide.addText("痛点分析:线上 vs 线下双录现状", {
    x: 0.7, y: 0.4, w: 7, h: 0.5,
    fontSize: 22, fontFace: "Microsoft YaHei", color: theme.primary,
    bold: true, align: "left", valign: "middle", margin: 0
  });
  slide.addText("PAIN POINTS", {
    x: 7.5, y: 0.5, w: 2, h: 0.3,
    fontSize: 10, fontFace: "Arial", color: theme.secondary,
    charSpacing: 4, align: "right"
  });
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: 1.0, w: 9, h: 0.02,
    fill: { color: theme.light }, line: { type: "none" }
  });

  // Two column comparison
  const colY = 1.3;
  const colH = 3.8;

  // LEFT - Online
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: colY, w: 4.3, h: colH,
    fill: { color: theme.light }, line: { type: "none" }, rectRadius: 0.05
  });
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: colY, w: 4.3, h: 0.5,
    fill: { color: theme.primary }, line: { type: "none" }, rectRadius: 0.05
  });
  slide.addText("线上双录", {
    x: 0.7, y: colY, w: 4, h: 0.5,
    fontSize: 16, fontFace: "Microsoft YaHei", color: "FFFFFF",
    bold: true, align: "left", valign: "middle", margin: 0
  });
  slide.addText("ONLINE", {
    x: 0.7, y: colY, w: 4, h: 0.5,
    fontSize: 10, fontFace: "Arial", color: "FFFFFF",
    charSpacing: 3, align: "right", valign: "middle", margin: 0
  });

  const onlinePoints = [
    "话术版本多,产品迭代后线上线下不同步",
    "音视频规范严格,但流程中断后无统一恢复机制",
    "AI 质检覆盖率高,但规则版本与线下不统一",
    "数据流转于线上系统,与线下 CRM/核心系统割裂"
  ];
  onlinePoints.forEach((p, i) => {
    slide.addShape(pres.shapes.OVAL, {
      x: 0.75, y: colY + 0.85 + i * 0.65, w: 0.12, h: 0.12,
      fill: { color: theme.accent }, line: { type: "none" }
    });
    slide.addText(p, {
      x: 0.95, y: colY + 0.7 + i * 0.65, w: 3.7, h: 0.6,
      fontSize: 11, fontFace: "Microsoft YaHei", color: theme.primary,
      align: "left", valign: "top", margin: 0
    });
  });

  // RIGHT - Offline
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 5.2, y: colY, w: 4.3, h: colH,
    fill: { color: theme.light }, line: { type: "none" }, rectRadius: 0.05
  });
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 5.2, y: colY, w: 4.3, h: 0.5,
    fill: { color: theme.secondary }, line: { type: "none" }, rectRadius: 0.05
  });
  slide.addText("线下双录", {
    x: 5.4, y: colY, w: 4, h: 0.5,
    fontSize: 16, fontFace: "Microsoft YaHei", color: "FFFFFF",
    bold: true, align: "left", valign: "middle", margin: 0
  });
  slide.addText("OFFLINE", {
    x: 5.4, y: colY, w: 4, h: 0.5,
    fontSize: 10, fontFace: "Arial", color: "FFFFFF",
    charSpacing: 3, align: "right", valign: "middle", margin: 0
  });

  const offlinePoints = [
    "依赖人工背诵话术,话术执行偏差大",
    "视频合规性依赖设备型号,文件易篡改/丢失",
    "质检以人工抽检为主,覆盖率低、时效差",
    "纸质单据 + U 盘拷视频,数据易不一致"
  ];
  offlinePoints.forEach((p, i) => {
    slide.addShape(pres.shapes.OVAL, {
      x: 5.45, y: colY + 0.85 + i * 0.65, w: 0.12, h: 0.12,
      fill: { color: theme.accent }, line: { type: "none" }
    });
    slide.addText(p, {
      x: 5.65, y: colY + 0.7 + i * 0.65, w: 3.7, h: 0.6,
      fontSize: 11, fontFace: "Microsoft YaHei", color: theme.primary,
      align: "left", valign: "top", margin: 0
    });
  });

  // Bottom callout - 5 pain points summary
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: 5.2, w: 9, h: 0.32,
    fill: { color: theme.accent, transparency: 85 }, line: { type: "none" }, rectRadius: 0.03
  });
  slide.addText("【细节点 1】线上线下的根本矛盾是「流程载体不同」,方案必须从「载体」层面拉通,而非「话术」层面补丁。", {
    x: 0.6, y: 5.2, w: 8.8, h: 0.32,
    fontSize: 10, fontFace: "Microsoft YaHei", color: theme.primary,
    bold: true, align: "left", valign: "middle", margin: 0
  });

  // Page number
  slide.addText("03 / 16", {
    x: 9.0, y: 5.55, w: 0.9, h: 0.07,
    fontSize: 8, fontFace: "Arial", color: theme.secondary,
    align: "right"
  });
}

module.exports = { createSlide };
