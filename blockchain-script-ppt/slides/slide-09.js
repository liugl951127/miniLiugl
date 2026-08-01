// 性能与扩展性
function createSlide(pres, theme) {
  const slide = pres.addSlide();
  slide.background = { color: theme.bg };

  // Title
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: 0.4, w: 0.08, h: 0.5,
    fill: { color: theme.accent }, line: { type: "none" }
  });
  slide.addText("性能与扩展性:扛住业务高峰", {
    x: 0.7, y: 0.4, w: 7.5, h: 0.5,
    fontSize: 20, fontFace: "Microsoft YaHei", color: theme.primary,
    bold: true, align: "left", valign: "middle", margin: 0
  });
  slide.addText("PERFORMANCE", {
    x: 8.2, y: 0.5, w: 1.3, h: 0.3,
    fontSize: 10, fontFace: "Arial", color: theme.secondary,
    charSpacing: 3, align: "right"
  });

  // 性能指标 - 4 big stats
  const stats = [
    { num: "3000+", unit: "TPS", label: "峰值吞吐", desc: "Raft 共识 + 批量打包" },
    { num: "<2s",  unit: "P95",  label: "上链延迟", desc: "从提交到最终性" },
    { num: "10k+", unit: "TPS", label: "批量峰值", desc: "Kafka 排序节点扩展" },
    { num: "0",    unit: "故障", label: "数据丢失", desc: "Raft + 3 副本强一致" },
  ];

  const statW = 2.15;
  const statH = 1.8;
  const statGap = 0.15;
  const statStartX = 0.5;
  const statY = 1.1;

  stats.forEach((s, i) => {
    const x = statStartX + i * (statW + statGap);
    slide.addShape(pres.shapes.RECTANGLE, {
      x: x, y: statY, w: statW, h: statH,
      fill: { color: theme.light }, line: { type: "none" }, rectRadius: 0.05
    });
    slide.addShape(pres.shapes.RECTANGLE, {
      x: x, y: statY, w: statW, h: 0.08,
      fill: { color: theme.accent }, line: { type: "none" }
    });
    slide.addText(s.num, {
      x: x, y: statY + 0.25, w: statW, h: 0.6,
      fontSize: 32, fontFace: "Arial", color: theme.accent,
      bold: true, align: "center", valign: "middle", margin: 0
    });
    slide.addText(s.unit, {
      x: x, y: statY + 0.85, w: statW, h: 0.3,
      fontSize: 11, fontFace: "Microsoft YaHei", color: theme.secondary,
      align: "center", valign: "middle", margin: 0
    });
    slide.addText(s.label, {
      x: x, y: statY + 1.15, w: statW, h: 0.3,
      fontSize: 12, fontFace: "Microsoft YaHei", color: theme.primary,
      bold: true, align: "center", valign: "middle", margin: 0
    });
    slide.addText(s.desc, {
      x: x, y: statY + 1.45, w: statW, h: 0.3,
      fontSize: 8.5, fontFace: "Microsoft YaHei", color: theme.secondary,
      align: "center", valign: "middle", margin: 0
    });
  });

  // Performance optimization
  const optY = 3.2;
  slide.addText("性能优化手段", {
    x: 0.5, y: optY, w: 4, h: 0.3,
    fontSize: 13, fontFace: "Microsoft YaHei", color: theme.accent,
    bold: true, align: "left", margin: 0
  });

  const optimizations = [
    { icon: "①", title: "批量打包", desc: "Orderer 节点 100ms 批量出块,TPS 提升 10x" },
    { icon: "②", title: "读写分离", desc: "查询走 LevelDB 缓存,写才走共识,读免费" },
    { icon: "③", title: "状态分片", desc: "按月份建 Channel,大表拆小降低单链压力" },
    { icon: "④", title: "并行背书", desc: "背书策略并行执行,延迟从 800ms 降至 200ms" },
  ];

  optimizations.forEach((o, i) => {
    const col = i % 2;
    const row = Math.floor(i / 2);
    const x = 0.5 + col * 4.6;
    const y = optY + 0.4 + row * 0.55;
    slide.addShape(pres.shapes.RECTANGLE, {
      x: x, y: y, w: 4.4, h: 0.5,
      fill: { color: theme.light }, line: { type: "none" }, rectRadius: 0.03
    });
    slide.addText(o.icon, {
      x: x + 0.1, y: y, w: 0.4, h: 0.5,
      fontSize: 18, fontFace: "Arial", color: theme.accent,
      bold: true, align: "center", valign: "middle", margin: 0
    });
    slide.addText(o.title, {
      x: x + 0.6, y: y, w: 1.3, h: 0.5,
      fontSize: 11, fontFace: "Microsoft YaHei", color: theme.primary,
      bold: true, align: "left", valign: "middle", margin: 0
    });
    slide.addText(o.desc, {
      x: x + 1.9, y: y, w: 2.5, h: 0.5,
      fontSize: 9.5, fontFace: "Microsoft YaHei", color: theme.secondary,
      align: "left", valign: "middle", margin: 0
    });
  });

  // 细节点
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: 5.2, w: 9, h: 0.32,
    fill: { color: theme.accent, transparency: 85 }, line: { type: "none" }, rectRadius: 0.03
  });
  slide.addText("【细节点 5】业务估算:全行日均 50 万笔双录 × 每笔 1 次上链 = 50 万 TPS 峰值需求,需分片 + 多链扩展。", {
    x: 0.6, y: 5.2, w: 8.8, h: 0.32,
    fontSize: 10, fontFace: "Microsoft YaHei", color: theme.primary,
    bold: true, align: "left", valign: "middle", margin: 0
  });

  slide.addText("09 / 19", {
    x: 9.0, y: 5.55, w: 0.9, h: 0.07,
    fontSize: 8, fontFace: "Arial", color: theme.secondary,
    align: "right"
  });
}

module.exports = { createSlide };
