// Slide LLM-10: Platform Implementation + Summary
function createSlide(pres, theme) {
  const slide = pres.addSlide();
  slide.addShape(pres.ShapeType.rect, {
    x: 0, y: 0, w: 10, h: 5.625,
    fill: { color: theme.primary }
  });

  // Decorative
  slide.addShape(pres.ShapeType.ellipse, {
    x: -1, y: 3.5, w: 5, h: 5,
    fill: { color: theme.accent, transparency: 80 }
  });
  slide.addShape(pres.ShapeType.ellipse, {
    x: 7.5, y: -1, w: 4, h: 4,
    fill: { color: theme.accent, transparency: 85 }
  });

  slide.addText("平台实现 + 知识图谱", {
    x: 0.6, y: 0.3, w: 9, h: 0.6,
    fontSize: 30, fontFace: "Microsoft YaHei",
    color: "FFFFFF", bold: true
  });
  slide.addShape(pres.ShapeType.rect, {
    x: 0.6, y: 0.95, w: 2.5, h: 0.04,
    fill: { color: theme.accent }
  });

  // Three column summary
  const cols = [
    {
      title: "🏗️ 平台架构",
      items: [
        "Spring Cloud Gateway → Nacos",
        "ONNX Runtime 自研推理",
        "OpenTelemetry 全链路追踪",
        "RAG: Milvus / H2 Fallback",
        "JWT (15min) + Refresh (7d)",
      ],
      color: "3b82f6"
    },
    {
      title: "🔬 核心算法",
      items: [
        "Transformer: Multi-Head + RoPE",
        "RLHF: Reward Model + PPO",
        "DPO: 直接偏好优化",
        "HNSW: ANN 向量索引",
        "RRF: 多检索结果融合",
      ],
      color: "7c3aed"
    },
    {
      title: "📐 数学基础",
      items: [
        "Attention: QKᵀ/√d · V",
        "Embedding: cosine similarity",
        "LoRA: ΔW = A · B (低秩)",
        "Loss: -log P(token_t)",
        "Scaling: Loss ∝ N^α + D^β",
      ],
      color: "059669"
    },
  ];

  cols.forEach((col, i) => {
    const x = 0.4 + i * 3.15;

    slide.addShape(pres.ShapeType.roundRect, {
      x, y: 1.2, w: 3.0, h: 3.3,
      fill: { color: "FFFFFF", transparency: 10 },
      line: { color: "FFFFFF", width: 0.5, transparency: 70 },
      rectRadius: 0.1
    });

    slide.addText(col.title, {
      x, y: 1.35, w: 3.0, h: 0.38,
      fontSize: 12, fontFace: "Microsoft YaHei",
      color: "FFFFFF", bold: true, align: 'center'
    });

    col.items.forEach((item, j) => {
      slide.addText("• " + item, {
        x: x + 0.15, y: 1.82 + j * 0.5, w: 2.7, h: 0.42,
        fontSize: 9.5, fontFace: "Microsoft YaHei",
        color: theme.light
      });
    });
  });

  // Bottom: Key numbers
  slide.addShape(pres.ShapeType.rect, {
    x: 0, y: 4.6, w: 10, h: 1.025,
    fill: { color: "1e3a5f" }
  });

  const stats = [
    { v: "ONNX", l: "自研推理引擎" },
    { v: "HNSW", l: "向量索引算法" },
    { v: "RLHF", l: "人类反馈对齐" },
    { v: "RAG", l: "检索增强生成" },
    { v: "Agent", l: "智能体编排" },
  ];
  stats.forEach((s, i) => {
    const x = 0.4 + i * 1.92;
    slide.addText(s.v, {
      x, y: 4.65, w: 1.7, h: 0.45,
      fontSize: 18, fontFace: "Arial",
      color: theme.accent, bold: true, align: 'center'
    });
    slide.addText(s.l, {
      x, y: 5.08, w: 1.7, h: 0.32,
      fontSize: 8.5, fontFace: "Microsoft YaHei",
      color: theme.light, align: 'center'
    });
  });

  slide.addText("10", {
    x: 9.3, y: 5.1, w: 0.5, h: 0.3,
    fontSize: 11, fontFace: "Arial",
    color: theme.light, align: 'center'
  });
}
module.exports = { createSlide };
