// Slide LLM-05: RAG Architecture
function createSlide(pres, theme) {
  const slide = pres.addSlide();
  slide.addShape(pres.ShapeType.rect, {
    x: 0, y: 0, w: 10, h: 5.625,
    fill: { color: theme.bg }
  });
  slide.addShape(pres.ShapeType.rect, {
    x: 0, y: 0, w: 0.08, h: 5.625,
    fill: { color: "d97706" }
  });

  slide.addText("03  RAG 检索增强生成", {
    x: 0.5, y: 0.2, w: 9, h: 0.45,
    fontSize: 20, fontFace: "Microsoft YaHei",
    color: theme.primary, bold: true
  });

  // Left column: Indexing pipeline
  slide.addShape(pres.ShapeType.roundRect, {
    x: 0.4, y: 0.72, w: 4.4, h: 0.4,
    fill: { color: "3b82f6" },
    rectRadius: 0.06
  });
  slide.addText("📚 索引构建 (Indexing)", {
    x: 0.4, y: 0.72, w: 4.4, h: 0.4,
    fontSize: 12, fontFace: "Microsoft YaHei",
    color: "FFFFFF", bold: true, align: 'center', valign: 'middle'
  });

  const indexSteps = [
    { icon: "📄", label: "文档上传", detail: "PDF / Word / Markdown / HTML" },
    { icon: "✂️", label: "文本分块", detail: "512-1024 tokens, 128 overlap" },
    { icon: "🔢", label: "Embedding", detail: "text → 向量 (1536 dim)" },
    { icon: "🗄️", label: "向量入库", detail: "HNSW 索引 → Milvus / H2" },
  ];
  indexSteps.forEach((s, i) => {
    const y = 1.22 + i * 0.9;
    slide.addShape(pres.ShapeType.roundRect, {
      x: 0.4, y, w: 4.4, h: 0.8,
      fill: { color: 'FFFFFF' },
      line: { color: "e2e8f0", width: 0.8 },
      rectRadius: 0.06
    });
    slide.addShape(pres.ShapeType.rect, {
      x: 0.4, y, w: 0.06, h: 0.8,
      fill: { color: "3b82f6" }
    });
    slide.addText(s.icon + " " + s.label, {
      x: 0.58, y: y + 0.1, w: 4.0, h: 0.32,
      fontSize: 12, fontFace: "Microsoft YaHei",
      color: theme.primary, bold: true
    });
    slide.addText(s.detail, {
      x: 0.58, y: y + 0.42, w: 4.0, h: 0.32,
      fontSize: 9.5, fontFace: "Microsoft YaHei",
      color: theme.secondary
    });
    if (i < indexSteps.length - 1) {
      slide.addText("↓", {
        x: 2.35, y: y + 0.78, w: 0.4, h: 0.2,
        fontSize: 11, color: "3b82f6", align: 'center'
      });
    }
  });

  // Right column: Query pipeline
  slide.addShape(pres.ShapeType.roundRect, {
    x: 5.2, y: 0.72, w: 4.4, h: 0.4,
    fill: { color: "059669" },
    rectRadius: 0.06
  });
  slide.addText("🔍 查询检索 (Query)", {
    x: 5.2, y: 0.72, w: 4.4, h: 0.4,
    fontSize: 12, fontFace: "Microsoft YaHei",
    color: "FFFFFF", bold: true, align: 'center', valign: 'middle'
  });

  const querySteps = [
    { icon: "❓", label: "用户问题", detail: "自然语言提问" },
    { icon: "🔢", label: "问题向量化", detail: "Embedding → 1536 dim" },
    { icon: "📊", label: "Top-K 检索", detail: "HNSW ANN 搜索, cos_sim > 0.75" },
    { icon: "🔄", label: "重排序", detail: "Cross-Encoder 精排 Top-20" },
    { icon: "📝", label: "Prompt 组装", detail: "检索结果注入上下文" },
    { icon: "🤖", label: "LLM 生成", detail: "Streaming SSE 回复 + 引用" },
  ];
  querySteps.forEach((s, i) => {
    const y = 1.22 + i * 0.66;
    slide.addShape(pres.ShapeType.roundRect, {
      x: 5.2, y, w: 4.4, h: 0.58,
      fill: { color: 'FFFFFF' },
      line: { color: "e2e8f0", width: 0.8 },
      rectRadius: 0.06
    });
    slide.addShape(pres.ShapeType.rect, {
      x: 5.2, y, w: 0.06, h: 0.58,
      fill: { color: "059669" }
    });
    slide.addText(s.icon + " " + s.label, {
      x: 5.38, y: y + 0.06, w: 4.0, h: 0.26,
      fontSize: 11, fontFace: "Microsoft YaHei",
      color: theme.primary, bold: true
    });
    slide.addText(s.detail, {
      x: 5.38, y: y + 0.3, w: 4.0, h: 0.24,
      fontSize: 9, fontFace: "Microsoft YaHei",
      color: theme.secondary
    });
    if (i < querySteps.length - 1) {
      slide.addText("↓", {
        x: 7.15, y: y + 0.56, w: 0.4, h: 0.18,
        fontSize: 10, color: "059669", align: 'center'
      });
    }
  });

  // Bottom: Hybrid Search note
  slide.addShape(pres.ShapeType.roundRect, {
    x: 0.4, y: 4.92, w: 9.2, h: 0.5,
    fill: { color: "d97706", transparency: 88 },
    rectRadius: 0.06
  });
  slide.addText("Hybrid Search: BM25 (关键词) + 向量相似度 → RRF 融合 (k=60) → Top-K", {
    x: 0.6, y: 4.92, w: 9.0, h: 0.5,
    fontSize: 10, fontFace: "Arial",
    color: "d97706", valign: 'middle'
  });

  slide.addText("05", {
    x: 9.3, y: 5.1, w: 0.5, h: 0.3,
    fontSize: 11, fontFace: "Arial",
    color: theme.secondary, align: 'center'
  });
}
module.exports = { createSlide };
