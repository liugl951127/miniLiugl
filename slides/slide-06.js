// Slide 06 - 核心技术栈
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

  slide.addText("核心技术栈", {
    x: 0.5, y: 0.3, w: 9, h: 0.6,
    fontSize: 28, fontFace: "Microsoft YaHei",
    color: theme.primary, bold: true
  });

  const stacks = [
    {
      layer: "前端",
      items: [
        { name: "Vue 3.4 + Composition API", detail: "响应式 · Pinia · Vue Router" },
        { name: "Vite 5 构建", detail: "ESM · HMR · Code Splitting" },
        { name: "Element Plus UI", detail: "70+ 组件 · 主题定制" },
        { name: "ECharts 可视化", detail: "Dashboard · Metrics" },
      ]
    },
    {
      layer: "网关",
      items: [
        { name: "Spring Cloud Gateway", detail: "动态路由 · StripPrefix" },
        { name: "Nacos 服务发现", detail: "注册中心 · 配置中心" },
        { name: "JWT 鉴权", detail: "Bearer Token · Refresh" },
        { name: "RateLimit 限流", detail: "Bucket4j · 多维度" },
      ]
    },
    {
      layer: "后端",
      items: [
        { name: "Spring Boot 3.2 + JDK17", detail: "虚拟线程 · AOT" },
        { name: "MySQL + H2", detail: "主从 · Schema 版本" },
        { name: "OpenTelemetry", detail: "Trace · Metrics · Logs" },
        { name: "Spring AI + ONNX", detail: "多模型路由 · SandBox" },
      ]
    },
    {
      layer: "基础设施",
      items: [
        { name: "Docker Compose", detail: "14 服务 · 独立端口" },
        { name: "Nginx 反向代理", detail: "80 端口 · HTTPS" },
        { name: "Prometheus + Grafana", detail: "监控 · 仪表盘" },
        { name: "MariaDB 10.11", detail: "字符集 utf8mb4" },
      ]
    }
  ];

  stacks.forEach((stack, si) => {
    const x = 0.5 + si * 2.35;

    // Layer header
    slide.addShape(pres.ShapeType.roundRect, {
      x, y: 1.0, w: 2.2, h: 0.38,
      fill: { color: theme.primary },
      rectRadius: 0.06
    });
    slide.addText(stack.layer, {
      x, y: 1.0, w: 2.2, h: 0.38,
      fontSize: 12, fontFace: "Microsoft YaHei",
      color: "FFFFFF", bold: true,
      align: "center", valign: "middle"
    });

    stack.items.forEach((item, ii) => {
      const iy = 1.52 + ii * 0.95;

      // Item card
      slide.addShape(pres.ShapeType.roundRect, {
        x, y: iy, w: 2.2, h: 0.85,
        fill: { color: "FFFFFF" },
        line: { color: "e2e8f0", width: 0.5 },
        rectRadius: 0.05
      });

      // Accent dot
      slide.addShape(pres.ShapeType.ellipse, {
        x: x + 0.12, y: iy + 0.18, w: 0.14, h: 0.14,
        fill: { color: theme.accent }
      });

      slide.addText(item.name, {
        x: x + 0.32, y: iy + 0.1, w: 1.8, h: 0.32,
        fontSize: 10, fontFace: "Microsoft YaHei",
        color: theme.primary, bold: true
      });

      slide.addText(item.detail, {
        x: x + 0.32, y: iy + 0.42, w: 1.8, h: 0.32,
        fontSize: 9, fontFace: "Microsoft YaHei",
        color: theme.secondary
      });
    });
  });

  slide.addText("06", {
    x: 9.3, y: 5.1, w: 0.5, h: 0.3,
    fontSize: 11, fontFace: "Arial",
    color: theme.secondary, align: "center"
  });
}
module.exports = { createSlide };
