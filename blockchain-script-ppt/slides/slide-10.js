// 国密集成 + 隐私保护
function createSlide(pres, theme) {
  const slide = pres.addSlide();
  slide.background = { color: theme.bg };

  // Title
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: 0.4, w: 0.08, h: 0.5,
    fill: { color: theme.accent }, line: { type: "none" }
  });
  slide.addText("国密集成:SM2/SM3/SM4 全栈适配", {
    x: 0.7, y: 0.4, w: 7.5, h: 0.5,
    fontSize: 20, fontFace: "Microsoft YaHei", color: theme.primary,
    bold: true, align: "left", valign: "middle", margin: 0
  });
  slide.addText("NATIONAL CRYPTO", {
    x: 8.2, y: 0.5, w: 1.3, h: 0.3,
    fontSize: 10, fontFace: "Arial", color: theme.secondary,
    charSpacing: 3, align: "right"
  });

  // 3 crypto cards
  const cryptos = [
    {
      name: "SM2",
      sub: "椭圆曲线非对称",
      usage: "数字签名 + 身份认证",
      desc: "256 位密钥,等价 RSA 3072 位,签一次 < 1ms"
    },
    {
      name: "SM3",
      sub: "杂凑算法",
      usage: "证据指纹 + Merkle Tree",
      desc: "256 位输出,抗碰撞性强,等价 SHA-256"
    },
    {
      name: "SM4",
      sub: "对称分组密码",
      usage: "视频/合同加密",
      desc: "128 位密钥,加解密速度 > 1GB/s"
    },
  ];

  const cW = 2.85;
  const cH = 2.5;
  const cGap = 0.225;
  const cStartX = 0.5;
  const cY = 1.05;

  cryptos.forEach((c, i) => {
    const x = cStartX + i * (cW + cGap);
    slide.addShape(pres.shapes.RECTANGLE, {
      x: x, y: cY, w: cW, h: cH,
      fill: { color: theme.light }, line: { type: "none" }, rectRadius: 0.05
    });
    // Top color
    slide.addShape(pres.shapes.RECTANGLE, {
      x: x, y: cY, w: cW, h: 0.7,
      fill: { color: theme.primary }, line: { type: "none" }, rectRadius: 0.05
    });
    slide.addText(c.name, {
      x: x, y: cY, w: cW, h: 0.7,
      fontSize: 28, fontFace: "Arial", color: "FFFFFF",
      bold: true, align: "center", valign: "middle", margin: 0
    });
    // Sub
    slide.addText(c.sub, {
      x: x + 0.15, y: cY + 0.85, w: cW - 0.3, h: 0.3,
      fontSize: 11, fontFace: "Microsoft YaHei", color: theme.accent,
      bold: true, align: "center", valign: "middle", margin: 0
    });
    // Usage
    slide.addText(c.usage, {
      x: x + 0.15, y: cY + 1.2, w: cW - 0.3, h: 0.4,
      fontSize: 12, fontFace: "Microsoft YaHei", color: theme.primary,
      bold: true, align: "center", valign: "middle", margin: 0
    });
    // Desc
    slide.addText(c.desc, {
      x: x + 0.15, y: cY + 1.65, w: cW - 0.3, h: 0.7,
      fontSize: 9.5, fontFace: "Microsoft YaHei", color: theme.secondary,
      align: "center", valign: "top", margin: 0
    });
  });

  // 隐私保护分层
  const priY = 3.8;
  slide.addText("隐私保护:3 层防护 + 通道隔离", {
    x: 0.5, y: priY, w: 9, h: 0.3,
    fontSize: 13, fontFace: "Microsoft YaHei", color: theme.accent,
    bold: true, align: "left", margin: 0
  });

  const protections = [
    { tier: "L1 网络层", desc: "本行 / 监管 / 保险公司 / 公证 各自独立 Channel,数据物理隔离" },
    { tier: "L2 数据层", desc: "敏感字段(身份证/手机号)链下加密存储,链上只存哈希" },
    { tier: "L3 访问层", desc: "基于属性的访问控制(ABAC),细粒度到字段 + 字段级国密解密" },
  ];

  protections.forEach((p, i) => {
    const y = priY + 0.4 + i * 0.45;
    slide.addShape(pres.shapes.RECTANGLE, {
      x: 0.5, y: y, w: 9, h: 0.4,
      fill: { color: theme.light }, line: { type: "none" }, rectRadius: 0.02
    });
    slide.addShape(pres.shapes.RECTANGLE, {
      x: 0.5, y: y, w: 1.4, h: 0.4,
      fill: { color: theme.accent }, line: { type: "none" }, rectRadius: 0.02
    });
    slide.addText(p.tier, {
      x: 0.5, y: y, w: 1.4, h: 0.4,
      fontSize: 10, fontFace: "Microsoft YaHei", color: "FFFFFF",
      bold: true, align: "center", valign: "middle", margin: 0
    });
    slide.addText(p.desc, {
      x: 2.0, y: y, w: 7.4, h: 0.4,
      fontSize: 10, fontFace: "Microsoft YaHei", color: theme.primary,
      align: "left", valign: "middle", margin: 0
    });
  });

  // 细节点
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: 5.2, w: 9, h: 0.32,
    fill: { color: theme.accent, transparency: 85 }, line: { type: "none" }, rectRadius: 0.03
  });
  slide.addText("【细节点 6】硬件国密机:采用卫士通/三未信安等国产密码机,密钥永不离开硬件,符合 GM/T 0034 标准。", {
    x: 0.6, y: 5.2, w: 8.8, h: 0.32,
    fontSize: 10, fontFace: "Microsoft YaHei", color: theme.primary,
    bold: true, align: "left", valign: "middle", margin: 0
  });

  slide.addText("10 / 19", {
    x: 9.0, y: 5.55, w: 0.9, h: 0.07,
    fontSize: 8, fontFace: "Arial", color: theme.secondary,
    align: "right"
  });
}

module.exports = { createSlide };
