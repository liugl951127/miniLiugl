// Slide LLM-03: LLM Foundation - Transformer
function createSlide(pres, theme) {
  const slide = pres.addSlide();
  slide.addShape(pres.ShapeType.rect, {
    x: 0, y: 0, w: 10, h: 5.625,
    fill: { color: theme.bg }
  });
  slide.addShape(pres.ShapeType.rect, {
    x: 0, y: 0, w: 0.08, h: 5.625,
    fill: { color: "059669" }
  });

  slide.addText("01  LLM 基础原理：Transformer 架构", {
    x: 0.5, y: 0.2, w: 9, h: 0.45,
    fontSize: 20, fontFace: "Microsoft YaHei",
    color: theme.primary, bold: true
  });

  // Left: Architecture diagram (simplified boxes)
  const boxY = 0.8;
  const boxH = 0.52;
  const gap = 0.08;

  const layers = [
    { label: "Input Tokens [x₁, x₂, ..., xₙ]", color: "3b82f6" },
    { label: "Embedding + Positional Encoding", color: "6366f1" },
    { label: "N × Encoder Layer (Multi-Head + FFN)", color: "7c3aed" },
    { label: "N × Decoder Layer (Masked-MHA + Cross-Attn + FFN)", color: "8b5cf6" },
    { label: "Linear + Softmax → Output Probabilities", color: "059669" },
  ];

  layers.forEach((l, i) => {
    const y = boxY + i * (boxH + gap);
    slide.addShape(pres.ShapeType.roundRect, {
      x: 0.4, y, w: 4.5, h: boxH,
      fill: { color: l.color },
      rectRadius: 0.05
    });
    slide.addText(l.label, {
      x: 0.5, y, w: 4.3, h: boxH,
      fontSize: 9.5, fontFace: "Arial",
      color: "FFFFFF", valign: 'middle'
    });
    if (i < layers.length - 1) {
      slide.addText("↓", {
        x: 2.45, y: y + boxH - 0.05, w: 0.3, h: 0.22,
        fontSize: 12, color: theme.secondary, align: 'center'
      });
    }
  });

  // Right: Key insight cards
  slide.addText("核心组件", {
    x: 5.2, y: 0.75, w: 4.5, h: 0.35,
    fontSize: 13, fontFace: "Microsoft YaHei",
    color: theme.primary, bold: true
  });

  const insights = [
    { icon: "Q/K/V", title: "Scaled Dot-Product Attention",
      desc: "Attention(Q,K,V) = softmax(QKᵀ / √dₖ) · V\n除 √dₖ 防梯度消失" },
    { icon: "Multi", title: "Multi-Head Attention",
      desc: "h 个注意力头并行\n各头学习不同语义特征" },
    { icon: "FFN", title: "Feed-Forward Network",
      desc: "两层线性+激活\n残差连接: x + Sublayer(x)" },
    { icon: "Pos", title: "位置编码",
      desc: "RoPE (LLaMA): 旋转矩阵编码位置\nALiBi: 无需训练的偏差" },
  ];

  insights.forEach((ins, i) => {
    const y = 1.15 + i * 1.05;
    slide.addShape(pres.ShapeType.roundRect, {
      x: 5.1, y, w: 4.5, h: 0.95,
      fill: { color: 'FFFFFF' },
      line: { color: "e2e8f0", width: 0.8 },
      rectRadius: 0.06
    });
    slide.addShape(pres.ShapeType.ellipse, {
      x: 5.2, y: y + 0.12, w: 0.55, h: 0.55,
      fill: { color: "059669" }
    });
    slide.addText(ins.icon, {
      x: 5.2, y: y + 0.12, w: 0.55, h: 0.55,
      fontSize: 8, fontFace: "Arial",
      color: "FFFFFF", bold: true,
      align: 'center', valign: 'middle'
    });
    slide.addText(ins.title, {
      x: 5.85, y: y + 0.1, w: 3.6, h: 0.3,
      fontSize: 11, fontFace: "Microsoft YaHei",
      color: theme.primary, bold: true
    });
    slide.addText(ins.desc, {
      x: 5.85, y: y + 0.4, w: 3.6, h: 0.5,
      fontSize: 8.5, fontFace: "Microsoft YaHei",
      color: theme.secondary
    });
  });

  slide.addText("03", {
    x: 9.3, y: 5.1, w: 0.5, h: 0.3,
    fontSize: 11, fontFace: "Arial",
    color: theme.secondary, align: 'center'
  });
}
module.exports = { createSlide };
