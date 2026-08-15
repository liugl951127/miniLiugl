// Slide LLM-06: Agent Orchestration
function createSlide(pres, theme) {
  const slide = pres.addSlide();
  slide.addShape(pres.ShapeType.rect, {
    x: 0, y: 0, w: 10, h: 5.625,
    fill: { color: theme.bg }
  });
  slide.addShape(pres.ShapeType.rect, {
    x: 0, y: 0, w: 0.08, h: 5.625,
    fill: { color: "dc2626" }
  });

  slide.addText("04  Agent 智能体编排", {
    x: 0.5, y: 0.2, w: 9, h: 0.45,
    fontSize: 20, fontFace: "Microsoft YaHei",
    color: theme.primary, bold: true
  });

  // Agent core diagram (center)
  slide.addShape(pres.ShapeType.ellipse, {
    x: 3.9, y: 1.05, w: 2.2, h: 1.4,
    fill: { color: "dc2626" }
  });
  slide.addText("LLM\n大脑", {
    x: 3.9, y: 1.05, w: 2.2, h: 1.4,
    fontSize: 16, fontFace: "Microsoft YaHei",
    color: "FFFFFF", bold: true,
    align: 'center', valign: 'middle'
  });

  // Surrounding modules
  const modules = [
    { x: 0.4, y: 0.8, w: 2.6, h: 0.9, label: "📋 Planner", sub: "RulePlanner / LlmPlanner\n任务分解 + Kahn 拓扑排序", color: "7c3aed" },
    { x: 0.4, y: 1.85, w: 2.6, h: 0.9, label: "🧠 Memory", sub: "短期: ConcurrentHashMap\n长期: List<Fact> (H2)", color: "059669" },
    { x: 0.4, y: 2.9, w: 2.6, h: 0.9, label: "🔧 ToolRegistry", sub: "InMemory / FeignToolBus\n函数调用 + 权限检查", color: "d97706" },
    { x: 6.8, y: 0.8, w: 2.6, h: 0.9, label: "⚡ Executor", sub: "占位符替换 ${stepId.output}\n风险拦截 CRITICAL 需审批", color: "3b82f6" },
    { x: 6.8, y: 1.85, w: 2.6, h: 0.9, label: "🛡️ Risk Guard", sub: "LOW/MEDIUM/HIGH/CRITICAL\n自动审批 or 人工确认", color: "dc2626" },
    { x: 6.8, y: 2.9, w: 2.6, h: 0.9, label: "🔄 Monitor", sub: "OpenTelemetry TraceId\n全链路可观测", color: "0891b2" },
  ];

  modules.forEach(m => {
    slide.addShape(pres.ShapeType.roundRect, {
      x: m.x, y: m.y, w: m.w, h: m.h,
      fill: { color: 'FFFFFF' },
      line: { color: m.color, width: 1.2 },
      rectRadius: 0.07
    });
    slide.addShape(pres.ShapeType.rect, {
      x: m.x, y: m.y, w: m.w, h: 0.32,
      fill: { color: m.color },
      rectRadius: 0.06
    });
    slide.addText(m.label, {
      x: m.x, y: m.y, w: m.w, h: 0.32,
      fontSize: 11, fontFace: "Microsoft YaHei",
      color: "FFFFFF", bold: true,
      align: 'center', valign: 'middle'
    });
    slide.addText(m.sub, {
      x: m.x + 0.1, y: m.y + 0.36, w: m.w - 0.2, h: 0.5,
      fontSize: 8.5, fontFace: "Microsoft YaHei",
      color: theme.secondary
    });
  });

  // Bottom: Planning strategies
  slide.addText("规划策略", {
    x: 0.5, y: 4.0, w: 9, h: 0.3,
    fontSize: 12, fontFace: "Microsoft YaHei",
    color: theme.primary, bold: true
  });

  const strategies = [
    { name: "ReAct", desc: "Reason + Act: Thought → Action → Observation → ...", color: "3b82f6" },
    { name: "CoT", desc: "Chain of Thought: 分步推理链 (1+2+3=6)", color: "7c3aed" },
    { name: "ToT", desc: "Tree of Thought: 多分支探索 + 评估剪枝", color: "059669" },
    { name: "Reflexion", desc: "自我反思: 失败后调整策略重新执行", color: "d97706" },
  ];

  strategies.forEach((s, i) => {
    const x = 0.5 + i * 2.35;
    slide.addShape(pres.ShapeType.roundRect, {
      x, y: 4.35, w: 2.2, h: 0.95,
      fill: { color: 'FFFFFF' },
      line: { color: s.color, width: 1 },
      rectRadius: 0.06
    });
    slide.addShape(pres.ShapeType.rect, {
      x, y: 4.35, w: 2.2, h: 0.28,
      fill: { color: s.color }
    });
    slide.addText(s.name, {
      x, y: 4.35, w: 2.2, h: 0.28,
      fontSize: 10, fontFace: "Arial",
      color: "FFFFFF", bold: true,
      align: 'center', valign: 'middle'
    });
    slide.addText(s.desc, {
      x: x + 0.1, y: 4.68, w: 2.0, h: 0.58,
      fontSize: 8.5, fontFace: "Microsoft YaHei",
      color: theme.secondary
    });
  });

  slide.addText("06", {
    x: 9.3, y: 5.1, w: 0.5, h: 0.3,
    fontSize: 11, fontFace: "Arial",
    color: theme.secondary, align: 'center'
  });
}
module.exports = { createSlide };
