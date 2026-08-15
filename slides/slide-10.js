// Slide 10 - 用户认证流程
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

  slide.addText("用户认证流程", {
    x: 0.5, y: 0.3, w: 9, h: 0.55,
    fontSize: 26, fontFace: "Microsoft YaHei",
    color: theme.primary, bold: true
  });

  // Flow diagram
  const flow = [
    { icon: "🔐", label: "登录页面", detail: "输入账号密码", y: 1.1, color: "3b82f6" },
    { icon: "→", label: "POST /api/v1/auth/login", detail: "http.post('/auth/login', credentials)", y: 1.95, color: "7c3aed" },
    { icon: "✓", label: "JWT Token 生成", detail: "accessToken (15min) + refreshToken (7d)", y: 2.8, color: "059669" },
    { icon: "→", label: "存入 Pinia Store", detail: "userStore.accessToken + refreshToken (localStorage persist)", y: 3.65, color: "d97706" },
    { icon: "🔓", label: "后续请求携带 Token", detail: "Authorization: Bearer {accessToken}", y: 4.5, color: "dc2626" },
  ];

  flow.forEach((step, i) => {
    // Step row
    slide.addShape(pres.ShapeType.roundRect, {
      x: 0.5, y: step.y, w: 9, h: 0.7,
      fill: { color: "FFFFFF" },
      line: { color: step.color, width: 1 },
      rectRadius: 0.06
    });

    // Color left bar
    slide.addShape(pres.ShapeType.rect, {
      x: 0.5, y: step.y, w: 0.07, h: 0.7,
      fill: { color: step.color }
    });

    // Step number
    slide.addShape(pres.ShapeType.ellipse, {
      x: 0.7, y: step.y + 0.15, w: 0.4, h: 0.4,
      fill: { color: step.color }
    });
    slide.addText(String(i + 1), {
      x: 0.7, y: step.y + 0.15, w: 0.4, h: 0.4,
      fontSize: 12, fontFace: "Arial",
      color: "FFFFFF", bold: true,
      align: "center", valign: "middle"
    });

    // Label
    slide.addText(step.label, {
      x: 1.25, y: step.y + 0.08, w: 4.5, h: 0.3,
      fontSize: 13, fontFace: "Microsoft YaHei",
      color: theme.primary, bold: true
    });

    // Detail
    slide.addText(step.detail, {
      x: 1.25, y: step.y + 0.38, w: 7.8, h: 0.26,
      fontSize: 10, fontFace: "Microsoft YaHei",
      color: theme.secondary
    });

    // Arrow down
    if (i < flow.length - 1) {
      slide.addText("▼", {
        x: 4.7, y: step.y + 0.7, w: 0.6, h: 0.25,
        fontSize: 10, color: theme.secondary, align: "center"
      });
    }
  });

  slide.addText("10", {
    x: 9.3, y: 5.1, w: 0.5, h: 0.3,
    fontSize: 11, fontFace: "Arial",
    color: theme.secondary, align: "center"
  });
}
module.exports = { createSlide };
