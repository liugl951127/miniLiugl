// Slide 12 - AI Chat 完整链路
function createSlide(pres, theme) {
  const slide = pres.addSlide();
  slide.addShape(pres.ShapeType.rect, {
    x: 0, y: 0, w: 10, h: 5.625,
    fill: { color: theme.bg }
  });
  slide.addShape(pres.ShapeType.rect, {
    x: 0, y: 0, w: 0.08, h: 5.625,
    fill: { color: "059669" }
  });

  slide.addText("AI Chat 完整链路", {
    x: 0.5, y: 0.25, w: 9, h: 0.5,
    fontSize: 26, fontFace: "Microsoft YaHei",
    color: theme.primary, bold: true
  });

  // Left: frontend
  slide.addShape(pres.ShapeType.roundRect, {
    x: 0.4, y: 0.9, w: 2.8, h: 4.2,
    fill: { color: "FFFFFF" },
    line: { color: "3b82f6", width: 1.5 },
    rectRadius: 0.08
  });
  slide.addShape(pres.ShapeType.roundRect, {
    x: 0.4, y: 0.9, w: 2.8, h: 0.38,
    fill: { color: "3b82f6" },
    rectRadius: 0.06
  });
  slide.addText("前端 Vue 3", {
    x: 0.4, y: 0.9, w: 2.8, h: 0.38,
    fontSize: 12, fontFace: "Microsoft YaHei",
    color: "FFFFFF", bold: true,
    align: "center", valign: "middle"
  });

  const feSteps = [
    "chat/Index.vue",
    "useSpeechCall composable",
    "SSE 流式请求",
    "Message 实时渲染",
    "Markdown + 工具卡片",
  ];
  feSteps.forEach((s, i) => {
    slide.addShape(pres.ShapeType.rect, {
      x: 0.55, y: 1.42 + i * 0.66, w: 0.12, h: 0.12,
      fill: { color: "3b82f6" }
    });
    slide.addText(s, {
      x: 0.75, y: 1.35 + i * 0.66, w: 2.3, h: 0.28,
      fontSize: 10, fontFace: "Microsoft YaHei",
      color: theme.secondary
    });
    if (i < feSteps.length - 1) {
      slide.addText("│", {
        x: 0.55, y: 1.48 + i * 0.66, w: 0.3, h: 0.2,
        fontSize: 9, color: "cbd5e1", align: "center"
      });
    }
  });

  // Middle: gateway
  slide.addShape(pres.ShapeType.roundRect, {
    x: 3.5, y: 2.1, w: 3, h: 2.0,
    fill: { color: "7c3aed" },
    rectRadius: 0.1
  });
  slide.addText("Spring Cloud Gateway", {
    x: 3.5, y: 2.2, w: 3, h: 0.4,
    fontSize: 13, fontFace: "Arial",
    color: "FFFFFF", bold: true,
    align: "center"
  });
  const gwItems = [
    "动态路由 → ai_service",
    "JWT Bearer Token 验证",
    "限流 Bucket4j",
    "TraceId 透传",
  ];
  gwItems.forEach((item, i) => {
    slide.addText("• " + item, {
      x: 3.65, y: 2.65 + i * 0.3, w: 2.7, h: 0.28,
      fontSize: 10, fontFace: "Microsoft YaHei",
      color: "FFFFFF"
    });
  });

  // Right: backend
  slide.addShape(pres.ShapeType.roundRect, {
    x: 6.8, y: 0.9, w: 2.8, h: 4.2,
    fill: { color: "FFFFFF" },
    line: { color: "059669", width: 1.5 },
    rectRadius: 0.08
  });
  slide.addShape(pres.ShapeType.roundRect, {
    x: 6.8, y: 0.9, w: 2.8, h: 0.38,
    fill: { color: "059669" },
    rectRadius: 0.06
  });
  slide.addText("后端微服务", {
    x: 6.8, y: 0.9, w: 2.8, h: 0.38,
    fontSize: 12, fontFace: "Microsoft YaHei",
    color: "FFFFFF", bold: true,
    align: "center", valign: "middle"
  });

  const beSteps = [
    "AiController",
    "Spring AI Router",
    "ONNX Runtime / OpenAI",
    "向量 Embedding",
    "RAG 上下文增强",
    "SSE 流式响应",
  ];
  beSteps.forEach((s, i) => {
    slide.addShape(pres.ShapeType.rect, {
      x: 6.95, y: 1.42 + i * 0.58, w: 0.12, h: 0.12,
      fill: { color: "059669" }
    });
    slide.addText(s, {
      x: 7.15, y: 1.35 + i * 0.58, w: 2.3, h: 0.28,
      fontSize: 10, fontFace: "Microsoft YaHei",
      color: theme.secondary
    });
    if (i < beSteps.length - 1) {
      slide.addText("│", {
        x: 6.95, y: 1.48 + i * 0.58, w: 0.3, h: 0.2,
        fontSize: 9, color: "cbd5e1", align: "center"
      });
    }
  });

  // Arrows
  slide.addText("→", {
    x: 3.25, y: 1.95, w: 0.25, h: 0.4,
    fontSize: 20, color: theme.primary, bold: true
  });
  slide.addText("→", {
    x: 6.55, y: 1.95, w: 0.25, h: 0.4,
    fontSize: 20, color: theme.primary, bold: true
  });

  slide.addText("12", {
    x: 9.3, y: 5.1, w: 0.5, h: 0.3,
    fontSize: 11, fontFace: "Arial",
    color: theme.secondary, align: "center"
  });
}
module.exports = { createSlide };
