// 统一话术模型架构
function createSlide(pres, theme) {
  const slide = pres.addSlide();
  slide.background = { color: theme.bg };

  // Title
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: 0.4, w: 0.08, h: 0.5,
    fill: { color: theme.accent }, line: { type: "none" }
  });
  slide.addText("统一话术模型:一套元数据,适配 N 种载体", {
    x: 0.7, y: 0.4, w: 7.5, h: 0.5,
    fontSize: 20, fontFace: "Microsoft YaHei", color: theme.primary,
    bold: true, align: "left", valign: "middle", margin: 0
  });
  slide.addText("02 / 3", {
    x: 8.2, y: 0.5, w: 1.3, h: 0.3,
    fontSize: 10, fontFace: "Arial", color: theme.secondary,
    charSpacing: 3, align: "right"
  });

  // 4 layers
  const layers = [
    {
      name: "L1 内容源",
      color: theme.primary,
      desc: "法务/合规/业务三方评审通过的标准话术文本",
      items: ["问候话术", "产品话术", "风险揭示", "客户确认"]
    },
    {
      name: "L2 元数据层",
      color: "1f3a5f",
      desc: "原子化节点定义:语义 + 强约束 + 触发条件",
      items: ["语义标签", "强制约束", "触发词", "必答关键词"]
    },
    {
      name: "L3 渲染引擎",
      color: "2b5c8a",
      desc: "根据载体自动选择播报形式(TTS/文字/语音)",
      items: ["TTS 语音", "字幕动效", "动画引导", "表情提示"]
    },
    {
      name: "L4 接入适配",
      color: "3d7eb5",
      desc: "为各端提供统一 SDK,差异由 SDK 屏蔽",
      items: ["H5 SDK", "一体机 SDK", "PAD SDK", "PC SDK"]
    },
  ];

  const lX = 0.5;
  const lW = 9.0;
  const lH = 0.85;
  const lYStart = 1.1;
  const lGap = 0.12;

  layers.forEach((l, i) => {
    const y = lYStart + i * (lH + lGap);
    slide.addShape(pres.shapes.RECTANGLE, {
      x: lX, y: y, w: 1.7, h: lH,
      fill: { color: l.color }, line: { type: "none" }, rectRadius: 0.03
    });
    slide.addText(l.name, {
      x: lX, y: y, w: 1.7, h: lH,
      fontSize: 12, fontFace: "Microsoft YaHei", color: "FFFFFF",
      bold: true, align: "center", valign: "middle", margin: 0
    });
    slide.addShape(pres.shapes.RECTANGLE, {
      x: lX + 1.7, y: y, w: lW - 1.7, h: lH,
      fill: { color: theme.light }, line: { type: "none" }, rectRadius: 0.03
    });
    slide.addText(l.desc, {
      x: lX + 1.85, y: y + 0.05, w: 4.5, h: lH - 0.1,
      fontSize: 10, fontFace: "Microsoft YaHei", color: theme.primary,
      align: "left", valign: "middle", margin: 0
    });
    // Items
    const itemText = l.items.map(it => "· " + it).join("  ");
    slide.addText(itemText, {
      x: lX + 6.4, y: y, w: lW - 6.5, h: lH,
      fontSize: 9.5, fontFace: "Microsoft YaHei", color: theme.accent,
      align: "left", valign: "middle", margin: 0
    });
  });

  // 关键数据点
  const dpY = 4.9;
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: dpY, w: 9, h: 0.55,
    fill: { color: theme.primary }, line: { type: "none" }, rectRadius: 0.05
  });
  slide.addText("【核心数据】9 类产品 × 平均 6 节点 × 3 语种 × 4 接入端 = 648 套话术配置,统一管理 + 智能适配", {
    x: 0.7, y: dpY, w: 8.6, h: 0.55,
    fontSize: 12, fontFace: "Microsoft YaHei", color: "FFFFFF",
    bold: true, align: "left", valign: "middle", margin: 0
  });

  // 细节点
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: 5.2, w: 9, h: 0.32,
    fill: { color: theme.accent, transparency: 85 }, line: { type: "none" }, rectRadius: 0.03
  });
  slide.addText("【细节点 9】话术原子化:每个节点独立可测试,新增产品只需配置,无需开发代码。", {
    x: 0.6, y: 5.2, w: 8.8, h: 0.32,
    fontSize: 10, fontFace: "Microsoft YaHei", color: theme.primary,
    bold: true, align: "left", valign: "middle", margin: 0
  });

  slide.addText("14 / 19", {
    x: 9.0, y: 5.55, w: 0.9, h: 0.07,
    fontSize: 8, fontFace: "Arial", color: theme.secondary,
    align: "right"
  });
}

module.exports = { createSlide };
