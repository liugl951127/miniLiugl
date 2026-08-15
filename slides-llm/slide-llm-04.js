// Slide LLM-04: Training Pipeline
function createSlide(pres, theme) {
  const slide = pres.addSlide();
  slide.addShape(pres.ShapeType.rect, {
    x: 0, y: 0, w: 10, h: 5.625,
    fill: { color: theme.bg }
  });
  slide.addShape(pres.ShapeType.rect, {
    x: 0, y: 0, w: 0.08, h: 5.625,
    fill: { color: "7c3aed" }
  });

  slide.addText("02  训练流程：预训练 → SFT → RLHF / DPO", {
    x: 0.5, y: 0.2, w: 9, h: 0.45,
    fontSize: 20, fontFace: "Microsoft YaHei",
    color: theme.primary, bold: true
  });

  // Three phase cards
  const phases = [
    {
      num: "1", title: "预训练 (Pre-training)",
      color: "3b82f6",
      items: [
        "海量无标注文本 (10T+ tokens)",
        "Next Token Prediction",
        "下一个 token 预测损失",
        "混合精度 BF16/FP16",
        "梯度检查点节省显存"
      ],
      flops: "~10²³ FLOPs"
    },
    {
      num: "2", title: "监督微调 (SFT)",
      color: "7c3aed",
      items: [
        "人工标注的对话数据",
        "ChatML 格式: system/user/assistant",
        "只对 assistant token 计算 loss",
        "LoRA: ΔW = A · B (低秩更新)",
        "QLoRA: 4-bit NF4 量化 + LoRA"
      ],
      flops: "~10²² FLOPs"
    },
    {
      num: "3", title: "对齐训练 (RLHF / DPO)",
      color: "059669",
      items: [
        "RLHF: Reward Model + PPO",
        "DPO: 直接偏好优化 (无需 RM)",
        "loss = -log σ(β · (logπ_θ(win) - logπ_θ(lose)))",
        "避免生成有害/无用内容",
        "迭代优化多轮"
      ],
      flops: "~10²¹ FLOPs"
    },
  ];

  phases.forEach((p, i) => {
    const x = 0.4 + i * 3.15;

    slide.addShape(pres.ShapeType.roundRect, {
      x, y: 0.72, w: 3.0, h: 4.5,
      fill: { color: 'FFFFFF' },
      line: { color: p.color, width: 1 },
      rectRadius: 0.08
    });

    // Header
    slide.addShape(pres.ShapeType.roundRect, {
      x, y: 0.72, w: 3.0, h: 0.55,
      fill: { color: p.color },
      rectRadius: 0.08
    });
    // Cover bottom corners of header
    slide.addShape(pres.ShapeType.rect, {
      x, y: 1.1, w: 3.0, h: 0.2,
      fill: { color: p.color }
    });

    slide.addText("PHASE " + p.num, {
      x, y: 0.72, w: 3.0, h: 0.3,
      fontSize: 9, fontFace: "Arial",
      color: "FFFFFF", align: 'center', valign: 'middle'
    });
    slide.addText(p.title, {
      x, y: 0.98, w: 3.0, h: 0.3,
      fontSize: 11, fontFace: "Microsoft YaHei",
      color: "FFFFFF", bold: true, align: 'center', valign: 'middle'
    });

    // Items
    p.items.forEach((item, j) => {
      slide.addShape(pres.ShapeType.rect, {
        x: x + 0.18, y: 1.45 + j * 0.58, w: 0.1, h: 0.1,
        fill: { color: p.color }
      });
      slide.addText(item, {
        x: x + 0.35, y: 1.38 + j * 0.58, w: 2.5, h: 0.5,
        fontSize: 9.5, fontFace: "Microsoft YaHei",
        color: theme.secondary
      });
    });

    // FLOPs badge
    slide.addShape(pres.ShapeType.roundRect, {
      x: x + 0.3, y: 4.7, w: 2.4, h: 0.38,
      fill: { color: p.color, transparency: 88 },
      rectRadius: 0.05
    });
    slide.addText("💻 " + p.flops, {
      x: x + 0.3, y: 4.7, w: 2.4, h: 0.38,
      fontSize: 9, fontFace: "Arial",
      color: p.color, bold: true, align: 'center', valign: 'middle'
    });
  });

  // Arrows between phases
  slide.addText("→", {
    x: 3.38, y: 2.5, w: 0.35, h: 0.5,
    fontSize: 22, color: theme.secondary, align: 'center'
  });
  slide.addText("→", {
    x: 6.52, y: 2.5, w: 0.35, h: 0.5,
    fontSize: 22, color: theme.secondary, align: 'center'
  });

  slide.addText("04", {
    x: 9.3, y: 5.1, w: 0.5, h: 0.3,
    fontSize: 11, fontFace: "Arial",
    color: theme.secondary, align: 'center'
  });
}
module.exports = { createSlide };
