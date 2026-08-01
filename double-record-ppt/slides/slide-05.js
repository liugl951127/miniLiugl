// Pain Point 1: Script interoperability
function createSlide(pres, theme) {
  const slide = pres.addSlide();
  slide.background = { color: theme.bg };

  // Title
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: 0.4, w: 0.08, h: 0.5,
    fill: { color: theme.accent }, line: { type: "none" }
  });
  slide.addText("方案一:统一话术中心 - 解决「话术互通」痛点", {
    x: 0.7, y: 0.4, w: 7, h: 0.5,
    fontSize: 20, fontFace: "Microsoft YaHei", color: theme.primary,
    bold: true, align: "left", valign: "middle", margin: 0
  });
  slide.addText("PAIN POINT 01/5", {
    x: 7.0, y: 0.5, w: 2.5, h: 0.3,
    fontSize: 10, fontFace: "Arial", color: theme.secondary,
    charSpacing: 3, align: "right"
  });

  // Pain point badge
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: 1.05, w: 9, h: 0.4,
    fill: { color: theme.accent, transparency: 88 }, line: { type: "none" }, rectRadius: 0.03
  });
  slide.addText("【痛点 1】线上/线下两套话术,产品迭代后不同步,口径不一致引发监管风险。", {
    x: 0.6, y: 1.05, w: 8.8, h: 0.4,
    fontSize: 11, fontFace: "Microsoft YaHei", color: theme.accent,
    bold: true, align: "left", valign: "middle", margin: 0
  });

  // 4-step design flow
  const stepY = 1.7;
  const stepW = 2.1;
  const stepH = 1.6;
  const gap = 0.18;
  const startX = 0.5;

  const steps = [
    { num: "1", t: "话术建模", d: "原子化拆解:问候/产品介绍/风险揭示/确认/结束,每个节点独立配置" },
    { num: "2", t: "版本管理", d: "按产品+监管版本+生效日期管理,自动灰度,旧版本可回溯" },
    { num: "3", t: "统一推送", d: "线上 SDK / 线下一体机统一拉取同一份话术,MD5 校验防篡改" },
    { num: "4", t: "强制执行", d: "话术节点必读必答,跳过即阻断,事后 100% 回放校验" }
  ];

  steps.forEach((s, i) => {
    const x = startX + i * (stepW + gap);
    // Step card
    slide.addShape(pres.shapes.RECTANGLE, {
      x: x, y: stepY, w: stepW, h: stepH,
      fill: { color: theme.light }, line: { type: "none" }, rectRadius: 0.05
    });
    // Number circle
    slide.addShape(pres.shapes.OVAL, {
      x: x + 0.2, y: stepY + 0.2, w: 0.5, h: 0.5,
      fill: { color: theme.primary }, line: { type: "none" }
    });
    slide.addText(s.num, {
      x: x + 0.2, y: stepY + 0.2, w: 0.5, h: 0.5,
      fontSize: 20, fontFace: "Arial", color: "FFFFFF",
      bold: true, align: "center", valign: "middle", margin: 0
    });
    // Title
    slide.addText(s.t, {
      x: x + 0.15, y: stepY + 0.8, w: stepW - 0.3, h: 0.3,
      fontSize: 14, fontFace: "Microsoft YaHei", color: theme.primary,
      bold: true, align: "center", valign: "middle", margin: 0
    });
    // Description
    slide.addText(s.d, {
      x: x + 0.15, y: stepY + 1.1, w: stepW - 0.3, h: 0.5,
      fontSize: 9, fontFace: "Microsoft YaHei", color: theme.secondary,
      align: "left", valign: "top", margin: 0
    });

    // Arrow between steps
    if (i < steps.length - 1) {
      slide.addShape(pres.shapes.RIGHT_TRIANGLE, {
        x: x + stepW + 0.02, y: stepY + 0.7, w: 0.14, h: 0.18,
        fill: { color: theme.accent }, line: { type: "none" },
        rotate: 90
      });
    }
  });

  // Bottom: script model example
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: 3.5, w: 9, h: 1.6,
    fill: { color: theme.light }, line: { type: "none" }, rectRadius: 0.05
  });
  slide.addText("话术原子化样例(以保险产品为例)", {
    x: 0.7, y: 3.6, w: 9, h: 0.3,
    fontSize: 12, fontFace: "Microsoft YaHei", color: theme.primary,
    bold: true, align: "left", margin: 0
  });

  const atoms = [
    { tag: "N1 问候", txt: "您好,我是 XX 银行客户经理,本次双录将全程录音录像,请问您同意吗?" },
    { tag: "N2 身份核验", txt: "请出示身份证,我将与您一同核对姓名、证件号、有效期。" },
    { tag: "N3 风险揭示", txt: "本产品为分红型保险,收益不确定,可能低于演示,您是否充分理解?" },
    { tag: "N4 犹豫期", txt: "您享有 15 天犹豫期,期间退保仅扣除不超过 10 元工本费,是否知晓?" },
    { tag: "N5 确认", txt: "以上内容均为您的真实意愿表达,是否确认投保?" }
  ];

  atoms.forEach((a, i) => {
    const y = 3.95 + i * 0.22;
    slide.addShape(pres.shapes.RECTANGLE, {
      x: 0.7, y: y, w: 1.1, h: 0.2,
      fill: { color: theme.primary }, line: { type: "none" }, rectRadius: 0.02
    });
    slide.addText(a.tag, {
      x: 0.7, y: y, w: 1.1, h: 0.2,
      fontSize: 8, fontFace: "Microsoft YaHei", color: "FFFFFF",
      bold: true, align: "center", valign: "middle", margin: 0
    });
    slide.addText(a.txt, {
      x: 1.9, y: y, w: 7.5, h: 0.2,
      fontSize: 9, fontFace: "Microsoft YaHei", color: theme.primary,
      align: "left", valign: "middle", margin: 0
    });
  });

  // Bottom callout
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: 5.2, w: 9, h: 0.32,
    fill: { color: theme.accent, transparency: 85 }, line: { type: "none" }, rectRadius: 0.03
  });
  slide.addText("【细节点 3】关键设计:话术原子化(N1~Nn)+强制确认位,任何节点未通过则流程不可继续。", {
    x: 0.6, y: 5.2, w: 8.8, h: 0.32,
    fontSize: 10, fontFace: "Microsoft YaHei", color: theme.primary,
    bold: true, align: "left", valign: "middle", margin: 0
  });

  slide.addText("05 / 16", {
    x: 9.0, y: 5.55, w: 0.9, h: 0.07,
    fontSize: 8, fontFace: "Arial", color: theme.secondary,
    align: "right"
  });
}

module.exports = { createSlide };
