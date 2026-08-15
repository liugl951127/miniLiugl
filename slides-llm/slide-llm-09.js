// Slide LLM-09: Prompt Engineering
function createSlide(pres, theme) {
  const slide = pres.addSlide();
  slide.addShape(pres.ShapeType.rect, {
    x: 0, y: 0, w: 10, h: 5.625,
    fill: { color: theme.bg }
  });
  slide.addShape(pres.ShapeType.rect, {
    x: 0, y: 0, w: 0.08, h: 5.625,
    fill: { color: "7c3aed" }
  });

  slide.addText("07  Prompt 工程", {
    x: 0.5, y: 0.2, w: 9, h: 0.45,
    fontSize: 20, fontFace: "Microsoft YaHei",
    color: theme.primary, bold: true
  });

  // Left: Prompt structure
  slide.addShape(pres.ShapeType.roundRect, {
    x: 0.4, y: 0.72, w: 4.5, h: 4.55,
    fill: { color: 'FFFFFF' },
    line: { color: "7c3aed", width: 1 },
    rectRadius: 0.08
  });
  slide.addShape(pres.ShapeType.rect, {
    x: 0.4, y: 0.72, w: 4.5, h: 0.38,
    fill: { color: "7c3aed" }
  });
  slide.addText("Prompt 结构", {
    x: 0.4, y: 0.72, w: 4.5, h: 0.38,
    fontSize: 12, fontFace: "Microsoft YaHei",
    color: "FFFFFF", bold: true, align: 'center', valign: 'middle'
  });

  const sections = [
    { role: "System", content: "你是一个专业的Java后端工程师，\n简洁、注重实践、避免废话", color: "7c3aed", alpha: 90 },
    { role: "User", content: "Spring Boot 如何实现异步？\n\n要求：\n1. 完整代码示例\n2. 关键注解解释\n3. 注意事项", color: "3b82f6", alpha: 90 },
    { role: "Assistant", content: "（LLM 生成回复区域）\n\n@Async + @EnableAsync\nCompletableFuture\nSpring TaskExecutor", color: "059669", alpha: 90 },
  ];

  let curY = 1.18;
  sections.forEach((s, i) => {
    const h = i === 1 ? 1.7 : (i === 0 ? 1.1 : 1.1);
    slide.addShape(pres.ShapeType.roundRect, {
      x: 0.55, y: curY, w: 4.2, h: h,
      fill: { color: s.color, transparency: s.alpha },
      rectRadius: 0.05
    });
    slide.addText(s.role, {
      x: 0.65, y: curY + 0.06, w: 1.0, h: 0.26,
      fontSize: 9, fontFace: "Arial",
      color: s.color, bold: true
    });
    slide.addText(s.content, {
      x: 0.65, y: curY + 0.28, w: 4.0, h: h - 0.35,
      fontSize: 8.5, fontFace: "Microsoft YaHei",
      color: theme.secondary
    });
    curY += h + 0.12;
  });

  // Right: Strategies
  slide.addShape(pres.ShapeType.roundRect, {
    x: 5.1, y: 0.72, w: 4.5, h: 4.55,
    fill: { color: 'FFFFFF' },
    line: { color: "0891b2", width: 1 },
    rectRadius: 0.08
  });
  slide.addShape(pres.ShapeType.rect, {
    x: 5.1, y: 0.72, w: 4.5, h: 0.38,
    fill: { color: "0891b2" }
  });
  slide.addText("核心策略", {
    x: 5.1, y: 0.72, w: 4.5, h: 0.38,
    fontSize: 12, fontFace: "Microsoft YaHei",
    color: "FFFFFF", bold: true, align: 'center', valign: 'middle'
  });

  const strategies = [
    { name: "Few-shot", code: "示例:\n'天气好' → 正面\n'心情差' → 负面\n'吃了饭' → ?", desc: "注入示例引导输出格式", color: "3b82f6" },
    { name: "CoT", code: "请分步回答:\nStep 1: ...\nStep 2: ...\nFinal: ...", desc: "Chain of Thought 推理链", color: "7c3aed" },
    { name: "Temperature", code: "T=0.0: 确定性 (代码/数学)\nT=0.7: 创意 (写作/头脑风暴)\nT>1.0: 高随机", desc: "温度控制随机性", color: "059669" },
    { name: "Top-P", code: "Nucleus Sampling:\n只采样累积概率 > P\n(通常 P=0.9) 的 token", desc: "核采样替代纯温度", color: "d97706" },
  ];

  strategies.forEach((s, i) => {
    const y = 1.18 + i * 1.02;
    slide.addShape(pres.ShapeType.roundRect, {
      x: 5.2, y, w: 4.3, h: 0.92,
      fill: { color: 'FFFFFF' },
      line: { color: s.color, width: 1 },
      rectRadius: 0.06
    });
    slide.addShape(pres.ShapeType.rect, {
      x: 5.2, y, w: 0.07, h: 0.92,
      fill: { color: s.color }
    });
    slide.addText(s.name, {
      x: 5.35, y: y + 0.06, w: 1.2, h: 0.26,
      fontSize: 11, fontFace: "Arial",
      color: s.color, bold: true
    });
    slide.addText(s.desc, {
      x: 6.55, y: y + 0.06, w: 2.85, h: 0.26,
      fontSize: 8.5, fontFace: "Microsoft YaHei",
      color: theme.secondary
    });
    slide.addText(s.code, {
      x: 5.35, y: y + 0.32, w: 4.0, h: 0.55,
      fontSize: 8, fontFace: "Courier New",
      color: theme.primary
    });
  });

  slide.addText("09", {
    x: 9.3, y: 5.1, w: 0.5, h: 0.3,
    fontSize: 11, fontFace: "Arial",
    color: theme.secondary, align: 'center'
  });
}
module.exports = { createSlide };
