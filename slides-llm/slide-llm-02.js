// Slide LLM-02: Table of Contents
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

  slide.addText("目录", {
    x: 0.5, y: 0.25, w: 9, h: 0.55,
    fontSize: 28, fontFace: "Microsoft YaHei",
    color: theme.primary, bold: true
  });

  const chapters = [
    { num: "01", title: "LLM 基础原理", sub: "Transformer · Attention · Scaling Law" },
    { num: "02", title: "训练流程", sub: "预训练 · SFT · RLHF · DPO · LoRA" },
    { num: "03", title: "RAG 检索增强", sub: "分块 · Embedding · HNSW · Hybrid Search" },
    { num: "04", title: "Agent 智能体", sub: "ReAct · CoT · ToT · 规划 · 记忆" },
    { num: "05", title: "向量与算法", sub: "Embedding · ANN · Function Calling" },
    { num: "06", title: "平台实现", sub: "模型路由 · SSE · ONNX · Skill Engine" },
  ];

  chapters.forEach((ch, i) => {
    const y = 0.95 + i * 0.72;
    const isLeft = i % 2 === 0;
    const x = isLeft ? 0.5 : 5.1;

    slide.addShape(pres.ShapeType.roundRect, {
      x, y, w: 4.4, h: 0.62,
      fill: { color: 'FFFFFF' },
      line: { color: theme.primary, width: 0.5 },
      rectRadius: 0.06
    });

    slide.addShape(pres.ShapeType.rect, {
      x, y, w: 0.07, h: 0.62,
      fill: { color: theme.accent }
    });

    slide.addText(ch.num, {
      x: x + 0.2, y, w: 0.55, h: 0.62,
      fontSize: 18, fontFace: "Arial",
      color: theme.accent, bold: true, valign: 'middle'
    });

    slide.addText(ch.title, {
      x: x + 0.75, y: y + 0.08, w: 3.4, h: 0.3,
      fontSize: 14, fontFace: "Microsoft YaHei",
      color: theme.primary, bold: true
    });

    slide.addText(ch.sub, {
      x: x + 0.75, y: y + 0.32, w: 3.4, h: 0.25,
      fontSize: 9, fontFace: "Arial",
      color: theme.secondary
    });
  });

  slide.addText("02", {
    x: 9.3, y: 5.1, w: 0.5, h: 0.3,
    fontSize: 11, fontFace: "Arial",
    color: theme.secondary, align: 'center'
  });
}
module.exports = { createSlide };
