// Slide 04 - 14 后端微服务模块
function createSlide(pres, theme) {
  const slide = pres.addSlide();
  slide.addShape(pres.ShapeType.rect, {
    x: 0, y: 0, w: 10, h: 5.625,
    fill: { color: theme.bg }
  });
  slide.addShape(pres.ShapeType.rect, {
    x: 0, y: 0, w: 0.08, h: 5.625,
    fill: { color: theme.primary }
  });

  slide.addText("14 个后端微服务", {
    x: 0.5, y: 0.3, w: 9, h: 0.6,
    fontSize: 28, fontFace: "Microsoft YaHei",
    color: theme.primary, bold: true
  });

  const services = [
    { name: "gateway", port: "7080", role: "网关 · 路由 · 鉴权", color: "7c3aed" },
    { name: "auth", port: "8081", role: "认证 · JWT · OAuth2", color: "0891b2" },
    { name: "ai", port: "8094", role: "AI · LLM · 向量", color: "059669" },
    { name: "chat", port: "8082", role: "会话 · 消息 · 流式", color: "d97706" },
    { name: "model", port: "8083", role: "模型 · Provider", color: "dc2626" },
    { name: "memory", port: "8084", role: "短期记忆 · 上下文", color: "7c3aed" },
    { name: "rag", port: "8085", role: "知识库 · 向量检索", color: "059669" },
    { name: "agent", port: "8086", role: "Agent · 工具调用", color: "d97706" },
    { name: "monitor", port: "8087", role: "监控 · 指标 · 告警", color: "dc2626" },
    { name: "admin", port: "8090", role: "管理后台 · 用户", color: "0891b2" },
    { name: "analytics", port: "8092", role: "数据分析 · NLSQL", color: "059669" },
    { name: "pipeline", port: "8093", role: "流水线 · DAG", color: "d97706" },
    { name: "multimodal", port: "8088", role: "多模态 · 图像", color: "7c3aed" },
    { name: "ws", port: "8095", role: "WebSocket · 推送", color: "0891b2" },
  ];

  services.forEach((s, i) => {
    const col = i % 4;
    const row = Math.floor(i / 4);
    const x = 0.5 + col * 2.35;
    const y = 1.05 + row * 1.08;

    // Card background
    slide.addShape(pres.ShapeType.roundRect, {
      x, y, w: 2.2, h: 0.9,
      fill: { color: "FFFFFF" },
      line: { color: "e2e8f0", width: 1 },
      rectRadius: 0.06
    });

    // Color bar on left
    slide.addShape(pres.ShapeType.rect, {
      x, y, w: 0.06, h: 0.9,
      fill: { color: s.color }
    });

    // Service name
    slide.addText(s.name, {
      x: x + 0.18, y: y + 0.12, w: 1.9, h: 0.32,
      fontSize: 14, fontFace: "Arial",
      color: theme.primary, bold: true
    });

    // Port
    slide.addText(`:${s.port}`, {
      x: x + 0.18, y: y + 0.44, w: 1.0, h: 0.22,
      fontSize: 10, fontFace: "Arial",
      color: theme.accent
    });

    // Role
    slide.addText(s.role, {
      x: x + 0.18, y: y + 0.62, w: 1.9, h: 0.22,
      fontSize: 9, fontFace: "Microsoft YaHei",
      color: theme.secondary
    });
  });

  slide.addText("04", {
    x: 9.3, y: 5.1, w: 0.5, h: 0.3,
    fontSize: 11, fontFace: "Arial",
    color: theme.secondary, align: "center"
  });
}
module.exports = { createSlide };
