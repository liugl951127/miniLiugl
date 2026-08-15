// Slide LLM-08: Function Calling
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

  slide.addText("06  Function Calling 工具调用", {
    x: 0.5, y: 0.2, w: 9, h: 0.45,
    fontSize: 20, fontFace: "Microsoft YaHei",
    color: theme.primary, bold: true
  });

  // Flow: 6 steps
  const steps = [
    { num: "1", icon: "💬", title: "用户提问", detail: "北京今天天气如何？", color: "3b82f6" },
    { num: "2", icon: "🔍", title: "意图识别", detail: "LLM 判断需调用工具", color: "6366f1" },
    { num: "3", icon: "📋", title: "函数选择", detail: "{name: get_weather, args: {city: 北京}}", color: "7c3aed" },
    { num: "4", icon: "⚙️", title: "执行函数", detail: "weather_api(city='北京') → 25°C 晴", color: "d97706" },
    { num: "5", icon: "📥", title: "结果注入", detail: "函数结果注入 Prompt 上下文", color: "dc2626" },
    { num: "6", icon: "🤖", title: "最终回答", detail: "北京今天 25°C，天气晴朗", color: "059669" },
  ];

  steps.forEach((s, i) => {
    const col = i % 3;
    const row = Math.floor(i / 3);
    const x = 0.4 + col * 3.15;
    const y = 0.72 + row * 2.2;

    slide.addShape(pres.ShapeType.roundRect, {
      x, y, w: 3.0, h: 1.95,
      fill: { color: 'FFFFFF' },
      line: { color: s.color, width: 1.2 },
      rectRadius: 0.08
    });

    // Number badge
    slide.addShape(pres.ShapeType.ellipse, {
      x: x + 0.15, y: y + 0.15, w: 0.5, h: 0.5,
      fill: { color: s.color }
    });
    slide.addText(s.num, {
      x: x + 0.15, y: y + 0.15, w: 0.5, h: 0.5,
      fontSize: 14, fontFace: "Arial",
      color: "FFFFFF", bold: true,
      align: 'center', valign: 'middle'
    });

    slide.addText(s.icon + " " + s.title, {
      x: x + 0.72, y: y + 0.2, w: 2.15, h: 0.35,
      fontSize: 13, fontFace: "Microsoft YaHei",
      color: theme.primary, bold: true
    });

    slide.addShape(pres.ShapeType.rect, {
      x: x + 0.15, y: y + 0.65, w: 2.7, h: 0.02,
      fill: { color: "f1f5f9" }
    });

    slide.addText(s.detail, {
      x: x + 0.15, y: y + 0.75, w: 2.7, h: 1.1,
      fontSize: 9.5, fontFace: "Microsoft YaHei",
      color: theme.secondary
    });

    // Arrow to next (horizontal)
    if (col < 2) {
      slide.addText("→", {
        x: x + 3.0, y: y + 0.8, w: 0.15, h: 0.4,
        fontSize: 16, color: "cbd5e1", align: 'center'
      });
    }
  });

  // Arrow between rows
  slide.addText("↓", {
    x: 4.55, y: 2.55, w: 0.4, h: 0.3,
    fontSize: 14, color: "cbd5e1", align: 'center'
  });

  // Function definition JSON
  slide.addShape(pres.ShapeType.roundRect, {
    x: 0.4, y: 5.0, w: 9.2, h: 0.42,
    fill: { color: "1e293b" },
    rectRadius: 0.06
  });
  slide.addText('{ "name": "get_weather", "description": "获取城市天气", "parameters": { "type": "object", "properties": { "city": { "type": "string" } }, "required": ["city"] } }', {
    x: 0.5, y: 5.0, w: 9.0, h: 0.42,
    fontSize: 8, fontFace: "Courier New",
    color: "a5f3fc", valign: 'middle'
  });

  slide.addText("08", {
    x: 9.3, y: 5.1, w: 0.5, h: 0.3,
    fontSize: 11, fontFace: "Arial",
    color: theme.secondary, align: 'center'
  });
}
module.exports = { createSlide };
