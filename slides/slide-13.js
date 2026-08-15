// Slide 13 - RAG 检索增强流程
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

  slide.addText("RAG 检索增强流程", {
    x: 0.5, y: 0.25, w: 9, h: 0.5,
    fontSize: 26, fontFace: "Microsoft YaHei",
    color: theme.primary, bold: true
  });

  // Timeline steps
  const steps = [
    { num: "1", title: "文档上传", detail: "PDF/Word/Markdown → /rag/doc/upload\n→ Milvus/H2 向量库存储", color: "3b82f6" },
    { num: "2", title: "Embedding", detail: "文本切分 → Sentence Embedding\n→ 向量入库 (1536 dim)", color: "7c3aed" },
    { num: "3", title: "Query 检索", detail: "用户问题 → Embedding\n→ Top-K 向量相似度匹配", color: "059669" },
    { num: "4", title: "上下文组装", detail: "检索结果 + 原问题\n→ 组装为 LLM Prompt", color: "d97706" },
    { num: "5", title: "LLM 生成", detail: "带 RAG 上下文的 Prompt\n→ 流式返回 AI 回答 + 引用", color: "dc2626" },
  ];

  steps.forEach((s, i) => {
    const y = 0.9 + i * 0.88;
    const isLeft = i % 2 === 0;

    // Step card
    slide.addShape(pres.ShapeType.roundRect, {
      x: isLeft ? 0.5 : 5.1, y, w: 4.4, h: 0.78,
      fill: { color: "FFFFFF" },
      line: { color: s.color, width: 1 },
      rectRadius: 0.06
    });

    // Left bar
    slide.addShape(pres.ShapeType.rect, {
      x: isLeft ? 0.5 : 5.1, y, w: 0.07, h: 0.78,
      fill: { color: s.color }
    });

    // Number
    slide.addShape(pres.ShapeType.ellipse, {
      x: (isLeft ? 0.7 : 5.3), y: y + 0.15, w: 0.48, h: 0.48,
      fill: { color: s.color }
    });
    slide.addText(s.num, {
      x: isLeft ? 0.7 : 5.3, y: y + 0.15, w: 0.48, h: 0.48,
      fontSize: 14, fontFace: "Arial",
      color: "FFFFFF", bold: true,
      align: "center", valign: "middle"
    });

    // Title
    slide.addText(s.title, {
      x: (isLeft ? 1.3 : 5.9), y: y + 0.1, w: 3.4, h: 0.3,
      fontSize: 13, fontFace: "Microsoft YaHei",
      color: theme.primary, bold: true
    });

    // Detail
    slide.addText(s.detail, {
      x: (isLeft ? 1.3 : 5.9), y: y + 0.4, w: 3.4, h: 0.35,
      fontSize: 9, fontFace: "Microsoft YaHei",
      color: theme.secondary
    });

    // Connector arrow
    if (i < steps.length - 1) {
      const cx = isLeft ? 4.9 : 5.1;
      const cy = y + 0.82;
      slide.addText("▼", {
        x: cx - 0.2, y: cy, w: 0.4, h: 0.2,
        fontSize: 8, color: theme.secondary, align: "center"
      });
    }
  });

  slide.addText("13", {
    x: 9.3, y: 5.1, w: 0.5, h: 0.3,
    fontSize: 11, fontFace: "Arial",
    color: theme.secondary, align: "center"
  });
}
module.exports = { createSlide };
