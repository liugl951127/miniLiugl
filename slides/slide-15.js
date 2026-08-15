// Slide 15 - Summary / Closing
function createSlide(pres, theme) {
  const slide = pres.addSlide();
  slide.addShape(pres.ShapeType.rect, {
    x: 0, y: 0, w: 10, h: 5.625,
    fill: { color: theme.primary }
  });

  // Accent stripe
  slide.addShape(pres.ShapeType.rect, {
    x: 0, y: 2.0, w: 10, h: 0.04,
    fill: { color: theme.accent }
  });

  // Title
  slide.addText("项目总结", {
    x: 0.6, y: 0.5, w: 9, h: 0.8,
    fontSize: 36, fontFace: "Microsoft YaHei",
    color: "FFFFFF", bold: true
  });

  // Stats row
  const stats = [
    { value: "14", label: "后端微服务" },
    { value: "29", label: "前端页面" },
    { value: "40+", label: "API 端点" },
    { value: "V6.8", label: "当前版本" },
  ];
  stats.forEach((s, i) => {
    const x = 0.6 + i * 2.35;
    slide.addText(s.value, {
      x, y: 1.2, w: 2.1, h: 0.65,
      fontSize: 36, fontFace: "Arial",
      color: theme.accent, bold: true,
      align: "center"
    });
    slide.addText(s.label, {
      x, y: 1.82, w: 2.1, h: 0.28,
      fontSize: 12, fontFace: "Microsoft YaHei",
      color: theme.light, align: "center"
    });
  });

  // Key takeaways
  const takeaways = [
    "✅ 微服务架构: gateway 统一入口，各服务独立部署，Nacos 服务发现",
    "✅ 前端规范: 所有 API 调用以 / 开头，http.js 统一追加 /api/v1 前缀",
    "✅ 认证流程: JWT accessToken (15min) + refreshToken (7d) 自动续期",
    "✅ 组件化: PageStandard/CrudTable/FormDrawer + useTable/useCrud composable",
    "✅ 监控可观测: OpenTelemetry + Prometheus + Grafana 全链路追踪",
  ];

  takeaways.forEach((t, i) => {
    slide.addText(t, {
      x: 0.6, y: 2.25 + i * 0.5, w: 8.8, h: 0.4,
      fontSize: 13, fontFace: "Microsoft YaHei",
      color: "FFFFFF"
    });
  });

  // Bottom
  slide.addShape(pres.ShapeType.rect, {
    x: 0, y: 5.1, w: 10, h: 0.525,
    fill: { color: "1e3a5f" }
  });
  slide.addText("github.com/liugl951127/miniLiugl  ·  Apache 2.0 License  ·  2026-08", {
    x: 0.6, y: 5.15, w: 8.8, h: 0.42,
    fontSize: 11, fontFace: "Arial",
    color: theme.light, valign: "middle"
  });

  slide.addText("15", {
    x: 9.3, y: 5.1, w: 0.5, h: 0.3,
    fontSize: 11, fontFace: "Arial",
    color: theme.light, align: "center"
  });
}
module.exports = { createSlide };
