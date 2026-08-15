// Slide 09 - 前端请求流程
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

  slide.addText("前端请求流程", {
    x: 0.5, y: 0.3, w: 9, h: 0.55,
    fontSize: 26, fontFace: "Microsoft YaHei",
    color: theme.primary, bold: true
  });

  // Flow steps
  const steps = [
    { num: "1", title: "Vue Component", desc: "http.get('/admin/users')", sub: "前端调用", color: "3b82f6" },
    { num: "2", title: "http.js Interceptor", desc: "自动追加 /api/v1 前缀\n注入 Authorization: Bearer token", sub: "axios 拦截器", color: "7c3aed" },
    { num: "3", title: "Nginx 80", desc: "location /api/v1/ai/\nproxy_pass → ai_service", sub: "反向代理", color: "059669" },
    { num: "4", title: "Gateway :7080", desc: "动态路由 → StripPrefix\nJWT 验证 · 限流", sub: "Spring Cloud Gateway", color: "d97706" },
    { num: "5", title: "微服务", desc: "ai :8094 / auth :8081\nchat :8082 / rag :8085", sub: "业务处理", color: "dc2626" },
  ];

  steps.forEach((s, i) => {
    const x = 0.3 + i * 1.95;

    // Step card
    slide.addShape(pres.ShapeType.roundRect, {
      x, y: 1.05, w: 1.75, h: 3.1,
      fill: { color: "FFFFFF" },
      line: { color: s.color, width: 1.5 },
      rectRadius: 0.08
    });

    // Number badge
    slide.addShape(pres.ShapeType.ellipse, {
      x: x + 0.6, y: 1.2, w: 0.55, h: 0.55,
      fill: { color: s.color }
    });
    slide.addText(s.num, {
      x: x + 0.6, y: 1.2, w: 0.55, h: 0.55,
      fontSize: 16, fontFace: "Arial",
      color: "FFFFFF", bold: true,
      align: "center", valign: "middle"
    });

    // Title
    slide.addText(s.title, {
      x: x + 0.08, y: 1.88, w: 1.6, h: 0.55,
      fontSize: 11, fontFace: "Microsoft YaHei",
      color: theme.primary, bold: true,
      align: "center", valign: "middle"
    });

    // Description
    slide.addText(s.desc, {
      x: x + 0.08, y: 2.5, w: 1.6, h: 0.85,
      fontSize: 9, fontFace: "Microsoft YaHei",
      color: theme.secondary,
      align: "center", valign: "top"
    });

    // Sub label
    slide.addShape(pres.ShapeType.roundRect, {
      x: x + 0.15, y: 3.5, w: 1.45, h: 0.28,
      fill: { color: s.color, transparency: 90 },
      rectRadius: 0.04
    });
    slide.addText(s.sub, {
      x: x + 0.15, y: 3.5, w: 1.45, h: 0.28,
      fontSize: 8, fontFace: "Microsoft YaHei",
      color: s.color,
      align: "center", valign: "middle"
    });

    // Arrow
    if (i < steps.length - 1) {
      slide.addShape(pres.ShapeType.rect, {
        x: x + 1.78, y: 2.3, w: 0.14, h: 0.04,
        fill: { color: theme.secondary }
      });
      // Arrow head
      slide.addText("▶", {
        x: x + 1.83, y: 2.18, w: 0.3, h: 0.3,
        fontSize: 10, color: theme.secondary
      });
    }
  });

  // Key note
  slide.addShape(pres.ShapeType.roundRect, {
    x: 0.5, y: 4.42, w: 9, h: 0.5,
    fill: { color: "fef3c7" },
    rectRadius: 0.06
  });
  slide.addText("⚠ 所有前端 API 调用必须以 / 开头:  http.get('/auth/login')  →  /api/v1/auth/login", {
    x: 0.7, y: 4.42, w: 8.6, h: 0.5,
    fontSize: 11, fontFace: "Microsoft YaHei",
    color: "92400e", valign: "middle"
  });

  slide.addText("09", {
    x: 9.3, y: 5.1, w: 0.5, h: 0.3,
    fontSize: 11, fontFace: "Arial",
    color: theme.secondary, align: "center"
  });
}
module.exports = { createSlide };
