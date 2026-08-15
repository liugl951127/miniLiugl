// Slide LLM-07: Embedding & Vector Search
function createSlide(pres, theme) {
  const slide = pres.addSlide();
  slide.addShape(pres.ShapeType.rect, {
    x: 0, y: 0, w: 10, h: 5.625,
    fill: { color: theme.bg }
  });
  slide.addShape(pres.ShapeType.rect, {
    x: 0, y: 0, w: 0.08, h: 5.625,
    fill: { color: "0891b2" }
  });

  slide.addText("05  向量嵌入与相似度检索", {
    x: 0.5, y: 0.2, w: 9, h: 0.45,
    fontSize: 20, fontFace: "Microsoft YaHei",
    color: theme.primary, bold: true
  });

  // Left: Embedding process
  slide.addShape(pres.ShapeType.roundRect, {
    x: 0.4, y: 0.75, w: 4.5, h: 4.5,
    fill: { color: 'FFFFFF' },
    line: { color: "0891b2", width: 1 },
    rectRadius: 0.1
  });
  slide.addShape(pres.ShapeType.roundRect, {
    x: 0.4, y: 0.75, w: 4.5, h: 0.45,
    fill: { color: "0891b2" },
    rectRadius: 0.1
  });
  slide.addShape(pres.ShapeType.rect, {
    x: 0.4, y: 1.05, w: 4.5, h: 0.18,
    fill: { color: "0891b2" }
  });
  slide.addText("🔢 Embedding 模型", {
    x: 0.4, y: 0.75, w: 4.5, h: 0.45,
    fontSize: 13, fontFace: "Microsoft YaHei",
    color: "FFFFFF", bold: true, align: 'center', valign: 'middle'
  });

  const embedItems = [
    { label: "文本 → 1536 维向量", icon: "📝" },
    { label: "语义相似 → 向量距离近", icon: "🎯" },
    { label: "Cosine Similarity: dot/(||a||·||b||)", icon: "📐" },
    { label: "Bi-Encoder: 独立编码 doc/query", icon: "⚡" },
    { label: "Cross-Encoder: 联合编码 (更精准)", icon: "🎖️" },
  ];
  embedItems.forEach((item, i) => {
    slide.addText(item.icon + "  " + item.label, {
      x: 0.6, y: 1.35 + i * 0.5, w: 4.1, h: 0.42,
      fontSize: 11, fontFace: "Microsoft YaHei",
      color: theme.secondary
    });
  });

  // Models table
  slide.addShape(pres.ShapeType.rect, {
    x: 0.5, y: 3.95, w: 4.3, h: 0.28,
    fill: { color: "0891b2", transparency: 85 }
  });
  slide.addText("模型", {
    x: 0.5, y: 3.95, w: 1.4, h: 0.28,
    fontSize: 9, fontFace: "Microsoft YaHei",
    color: "0891b2", bold: true, align: 'center', valign: 'middle'
  });
  slide.addText("维度", {
    x: 1.9, y: 3.95, w: 0.8, h: 0.28,
    fontSize: 9, fontFace: "Arial",
    color: "0891b2", bold: true, align: 'center', valign: 'middle'
  });
  slide.addText("中文", {
    x: 2.7, y: 3.95, w: 0.6, h: 0.28,
    fontSize: 9, fontFace: "Microsoft YaHei",
    color: "0891b2", bold: true, align: 'center', valign: 'middle'
  });
  slide.addText("MTEB", {
    x: 3.3, y: 3.95, w: 1.5, h: 0.28,
    fontSize: 9, fontFace: "Arial",
    color: "0891b2", bold: true, align: 'center', valign: 'middle'
  });

  const models = [
    ["bge-large-zh-v1.5", "1024", "✅", "~64"],
    ["BGE-M3", "1024", "✅", "~66"],
    ["m3e-large", "1024", "✅", "~63"],
    ["jina-embeddings", "1024", "一般", "~64"],
  ];
  models.forEach((row, i) => {
    const y = 4.28 + i * 0.24;
    slide.addText(row[0], {
      x: 0.5, y, w: 1.4, h: 0.24,
      fontSize: 8, fontFace: "Arial",
      color: theme.secondary, align: 'center', valign: 'middle'
    });
    slide.addText(row[1], {
      x: 1.9, y, w: 0.8, h: 0.24,
      fontSize: 8, fontFace: "Arial",
      color: theme.secondary, align: 'center', valign: 'middle'
    });
    slide.addText(row[2], {
      x: 2.7, y, w: 0.6, h: 0.24,
      fontSize: 8, fontFace: "Arial",
      color: theme.secondary, align: 'center', valign: 'middle'
    });
    slide.addText(row[3], {
      x: 3.3, y, w: 1.5, h: 0.24,
      fontSize: 8, fontFace: "Arial",
      color: theme.secondary, align: 'center', valign: 'middle'
    });
  });

  // Right: HNSW visualization
  slide.addShape(pres.ShapeType.roundRect, {
    x: 5.1, y: 0.75, w: 4.5, h: 4.5,
    fill: { color: 'FFFFFF' },
    line: { color: "7c3aed", width: 1 },
    rectRadius: 0.1
  });
  slide.addShape(pres.ShapeType.roundRect, {
    x: 5.1, y: 0.75, w: 4.5, h: 0.45,
    fill: { color: "7c3aed" },
    rectRadius: 0.1
  });
  slide.addShape(pres.ShapeType.rect, {
    x: 5.1, y: 1.05, w: 4.5, h: 0.18,
    fill: { color: "7c3aed" }
  });
  slide.addText("🗺️ HNSW 索引算法", {
    x: 5.1, y: 0.75, w: 4.5, h: 0.45,
    fontSize: 13, fontFace: "Microsoft YaHei",
    color: "FFFFFF", bold: true, align: 'center', valign: 'middle'
  });

  // Draw HNSW layers
  const layers = [
    { y: 1.4, nodes: 3, label: "Layer 2 (稀疏)" },
    { y: 2.1, nodes: 6, label: "Layer 1 (中等)" },
    { y: 2.8, nodes: 12, label: "Layer 0 (全量)" },
  ];

  layers.forEach((l, li) => {
    slide.addText(l.label, {
      x: 5.2, y: l.y - 0.02, w: 1.2, h: 0.22,
      fontSize: 8, fontFace: "Arial",
      color: "7c3aed", bold: true
    });
    const spacing = 3.6 / (l.nodes + 1);
    for (let n = 0; n < l.nodes; n++) {
      const nx = 5.5 + (n + 1) * spacing;
      slide.addShape(pres.ShapeType.ellipse, {
        x: nx - 0.14, y: l.y + 0.14, w: 0.28, h: 0.28,
        fill: { color: li === 0 ? "7c3aed" : li === 1 ? "a78bfa" : "c4b5fd" }
      });
    }
    if (li < layers.length - 1) {
      slide.addText("↓", {
        x: 7.15, y: l.y + 0.35, w: 0.3, h: 0.2,
        fontSize: 10, color: "7c3aed", align: 'center'
      });
    }
  });

  // HNSW properties
  const hnswProps = [
    { label: "构建复杂度", value: "O(N log N)" },
    { label: "查询复杂度", value: "O(log N)" },
    { label: "搜索策略", value: "贪心 + 剪枝" },
    { label: "M (连接数)", value: "16-64" },
    { label: "efSearch", value: "100-500" },
  ];
  hnswProps.forEach((p, i) => {
    slide.addText(p.label + ":", {
      x: 5.3, y: 3.6 + i * 0.3, w: 1.8, h: 0.26,
      fontSize: 9, fontFace: "Microsoft YaHei",
      color: theme.secondary
    });
    slide.addText(p.value, {
      x: 7.1, y: 3.6 + i * 0.3, w: 2.3, h: 0.26,
      fontSize: 9, fontFace: "Arial",
      color: "7c3aed", bold: true
    });
  });

  slide.addText("07", {
    x: 9.3, y: 5.1, w: 0.5, h: 0.3,
    fontSize: 11, fontFace: "Arial",
    color: theme.secondary, align: 'center'
  });
}
module.exports = { createSlide };
