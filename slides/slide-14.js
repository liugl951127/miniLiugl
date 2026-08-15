// Slide 14 - 前端组件架构
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

  slide.addText("前端组件架构", {
    x: 0.5, y: 0.25, w: 9, h: 0.5,
    fontSize: 26, fontFace: "Microsoft YaHei",
    color: theme.primary, bold: true
  });

  // Layout layer
  slide.addShape(pres.ShapeType.roundRect, {
    x: 0.5, y: 0.9, w: 9, h: 0.7,
    fill: { color: "1e40af" },
    rectRadius: 0.08
  });
  slide.addText("layout/Index.vue — 布局层 (导航栏 + 侧边菜单 + 主内容区)", {
    x: 0.7, y: 0.9, w: 8.6, h: 0.7,
    fontSize: 13, fontFace: "Microsoft YaHei",
    color: "FFFFFF", bold: true, valign: "middle"
  });

  // Router
  slide.addShape(pres.ShapeType.roundRect, {
    x: 0.5, y: 1.72, w: 9, h: 0.5,
    fill: { color: "7c3aed" },
    rectRadius: 0.06
  });
  slide.addText("router/index.js — Vue Router (29 个路由 → V2 版本页面组件)", {
    x: 0.7, y: 1.72, w: 8.6, h: 0.5,
    fontSize: 12, fontFace: "Arial",
    color: "FFFFFF", valign: "middle"
  });

  // V2 views grid
  const views = [
    { name: "PageStandard", desc: "标准页容器\n分页 + 搜索 + 刷新" },
    { name: "CrudTable", desc: "表格增删改查\n排序 · 筛选 · 批量" },
    { name: "FormDrawer", desc: "侧边抽屉表单\ncreate/edit/view" },
    { name: "useTable", desc: "表格状态管理\ndata · loading · pagination" },
    { name: "useCrud", desc: "CRUD 流程编排\ncreate/update/delete" },
    { name: "StatCardGroup", desc: "统计卡片组\nKPI 指标展示" },
  ];

  views.forEach((v, i) => {
    const col = i % 3;
    const row = Math.floor(i / 3);
    const x = 0.5 + col * 3.05;
    const y = 2.38 + row * 1.4;

    slide.addShape(pres.ShapeType.roundRect, {
      x, y, w: 2.9, h: 1.2,
      fill: { color: "FFFFFF" },
      line: { color: "7c3aed", width: 1 },
      rectRadius: 0.06
    });

    // Component name
    slide.addShape(pres.ShapeType.roundRect, {
      x, y, w: 2.9, h: 0.38,
      fill: { color: "7c3aed", transparency: 85 },
      rectRadius: 0.04
    });
    slide.addText(v.name, {
      x, y, w: 2.9, h: 0.38,
      fontSize: 12, fontFace: "Arial",
      color: "7c3aed", bold: true,
      align: "center", valign: "middle"
    });

    // Description
    slide.addText(v.desc, {
      x: x + 0.1, y: y + 0.48, w: 2.7, h: 0.65,
      fontSize: 10, fontFace: "Microsoft YaHei",
      color: theme.secondary
    });
  });

  // Store
  slide.addShape(pres.ShapeType.roundRect, {
    x: 0.5, y: 5.0, w: 2.8, h: 0.35,
    fill: { color: "059669" },
    rectRadius: 0.05
  });
  slide.addText("Pinia Store (userStore)", {
    x: 0.5, y: 5.0, w: 2.8, h: 0.35,
    fontSize: 10, fontFace: "Microsoft YaHei",
    color: "FFFFFF", align: "center", valign: "middle"
  });

  slide.addShape(pres.ShapeType.roundRect, {
    x: 3.5, y: 5.0, w: 2.8, h: 0.35,
    fill: { color: "d97706" },
    rectRadius: 0.05
  });
  slide.addText("API Layer (http.js)", {
    x: 3.5, y: 5.0, w: 2.8, h: 0.35,
    fontSize: 10, fontFace: "Microsoft YaHei",
    color: "FFFFFF", align: "center", valign: "middle"
  });

  slide.addText("14", {
    x: 9.3, y: 5.1, w: 0.5, h: 0.3,
    fontSize: 11, fontFace: "Arial",
    color: theme.secondary, align: "center"
  });
}
module.exports = { createSlide };
