// Slide 08 - 后端微服务依赖图
function createSlide(pres, theme) {
  const slide = pres.addSlide();
  slide.addShape(pres.ShapeType.rect, {
    x: 0, y: 0, w: 10, h: 5.625,
    fill: { color: theme.bg }
  });
  slide.addShape(pres.ShapeType.rect, {
    x: 0, y: 0, w: 0.08, h: 5.625,
    fill: { color: theme.accent }
  });

  slide.addText("后端微服务依赖关系", {
    x: 0.5, y: 0.25, w: 9, h: 0.5,
    fontSize: 26, fontFace: "Microsoft YaHei",
    color: theme.primary, bold: true
  });

  // Layer 0: Gateway (top)
  const gwX = 3.8, gwY = 0.85;
  slide.addShape(pres.ShapeType.roundRect, {
    x: gwX, y: gwY, w: 2.4, h: 0.65,
    fill: { color: "7c3aed" },
    rectRadius: 0.08
  });
  slide.addText("Gateway :7080", {
    x: gwX, y: gwY, w: 2.4, h: 0.35,
    fontSize: 12, fontFace: "Arial",
    color: "FFFFFF", bold: true,
    align: "center", valign: "middle"
  });
  slide.addText("路由 · 鉴权 · 限流", {
    x: gwX, y: gwY + 0.33, w: 2.4, h: 0.28,
    fontSize: 9, fontFace: "Microsoft YaHei",
    color: "FFFFFF", align: "center"
  });

  // Arrow down
  slide.addShape(pres.ShapeType.rect, {
    x: 4.93, y: gwY + 0.68, w: 0.04, h: 0.22,
    fill: { color: theme.secondary }
  });

  // Layer 1: Core services
  const coreSvcs = [
    { name: "auth :8081", role: "认证", x: 0.4, color: "0891b2" },
    { name: "ai :8094", role: "AI LLM", x: 2.85, color: "059669" },
    { name: "chat :8082", role: "会话", x: 5.3, color: "d97706" },
    { name: "model :8083", role: "模型", x: 7.75, color: "dc2626" },
  ];
  coreSvcs.forEach(s => {
    slide.addShape(pres.ShapeType.roundRect, {
      x: s.x, y: 1.9, w: 2.2, h: 0.65,
      fill: { color: s.color },
      rectRadius: 0.08
    });
    slide.addText(s.name, {
      x: s.x, y: 1.9, w: 2.2, h: 0.35,
      fontSize: 11, fontFace: "Arial",
      color: "FFFFFF", bold: true,
      align: "center", valign: "middle"
    });
    slide.addText(s.role, {
      x: s.x, y: 2.23, w: 2.2, h: 0.28,
      fontSize: 9, fontFace: "Microsoft YaHei",
      color: "FFFFFF", align: "center"
    });
    // Arrow from gateway
    const ax = s.x + 1.1;
    slide.addShape(pres.ShapeType.rect, {
      x: ax, y: gwY + 0.68, w: 0.04, h: 0.35,
      fill: { color: "cbd5e1" }
    });
  });

  // Layer 2: Middle services
  const midSvcs = [
    { name: "memory :8084", role: "记忆", x: 1.6, color: "7c3aed" },
    { name: "rag :8085", role: "知识库", x: 4.05, color: "059669" },
    { name: "agent :8086", role: "Agent", x: 6.5, color: "d97706" },
  ];
  midSvcs.forEach(s => {
    slide.addShape(pres.ShapeType.roundRect, {
      x: s.x, y: 3.15, w: 2.2, h: 0.65,
      fill: { color: s.color },
      rectRadius: 0.08
    });
    slide.addText(s.name, {
      x: s.x, y: 3.15, w: 2.2, h: 0.35,
      fontSize: 11, fontFace: "Arial",
      color: "FFFFFF", bold: true,
      align: "center", valign: "middle"
    });
    slide.addText(s.role, {
      x: s.x, y: 3.48, w: 2.2, h: 0.28,
      fontSize: 9, fontFace: "Microsoft YaHei",
      color: "FFFFFF", align: "center"
    });
  });

  // Layer 3: Consumer services
  const consSvcs = [
    { name: "admin :8090", role: "管理后台" },
    { name: "monitor :8087", role: "监控告警" },
    { name: "analytics :8092", role: "数据分析" },
    { name: "pipeline :8093", role: "流水线" },
    { name: "multimodal :8088", role: "多模态" },
    { name: "ws :8095", role: "WebSocket" },
  ];
  consSvcs.forEach((s, i) => {
    const col = i % 3;
    const row = Math.floor(i / 3);
    const x = 0.5 + col * 3.15;
    const y = 4.2 + row * 0.62;
    slide.addShape(pres.ShapeType.roundRect, {
      x, y, w: 2.9, h: 0.52,
      fill: { color: "FFFFFF" },
      line: { color: theme.primary, width: 1 },
      rectRadius: 0.06
    });
    slide.addText(s.name, {
      x: x + 0.1, y: y + 0.05, w: 1.6, h: 0.25,
      fontSize: 10, fontFace: "Arial",
      color: theme.primary, bold: true
    });
    slide.addText(s.role, {
      x: x + 0.1, y: y + 0.28, w: 2.7, h: 0.2,
      fontSize: 9, fontFace: "Microsoft YaHei",
      color: theme.secondary
    });
  });

  // Common module annotation
  slide.addText("common (所有模块依赖)", {
    x: 0.5, y: 5.02, w: 2.2, h: 0.22,
    fontSize: 9, fontFace: "Microsoft YaHei",
    color: theme.secondary, italic: true
  });

  slide.addText("08", {
    x: 9.3, y: 5.1, w: 0.5, h: 0.3,
    fontSize: 11, fontFace: "Arial",
    color: theme.secondary, align: "center"
  });
}
module.exports = { createSlide };
