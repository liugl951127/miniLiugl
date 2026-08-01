// 整体架构:4 层架构
function createSlide(pres, theme) {
  const slide = pres.addSlide();
  slide.background = { color: theme.bg };

  // Title
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: 0.4, w: 0.08, h: 0.5,
    fill: { color: theme.accent }, line: { type: "none" }
  });
  slide.addText("整体架构:4 层分层 + 6 节点网络", {
    x: 0.7, y: 0.4, w: 7.5, h: 0.5,
    fontSize: 20, fontFace: "Microsoft YaHei", color: theme.primary,
    bold: true, align: "left", valign: "middle", margin: 0
  });
  slide.addText("03 / 4", {
    x: 8.2, y: 0.5, w: 1.3, h: 0.3,
    fontSize: 10, fontFace: "Arial", color: theme.secondary,
    charSpacing: 3, align: "right"
  });

  // 4 layers
  const layers = [
    {
      name: "L1 业务接入层", color: theme.primary,
      modules: ["双录业务系统", "CRM", "合规审计", "监管报送"]
    },
    {
      name: "L2 区块链中间件", color: "1f3a5f",
      modules: ["Hyperledger Fabric SDK", "智能合约引擎", "事件订阅", "国密适配器"]
    },
    {
      name: "L3 共识网络层", color: "2b5c8a",
      modules: ["Orderer 排序节点", "Peer 记账节点", "CA 证书节点", "Channel 通道"]
    },
    {
      name: "L4 存储层", color: "3d7eb5",
      modules: ["账本数据(区块链)", "状态数据库(LevelDB)", "历史数据(CouchDB)", "国密 KMS"]
    },
  ];

  const layerX = 0.5;
  const layerW = 9.0;
  const layerH = 0.65;
  const layerYStart = 1.05;
  const layerGap = 0.15;

  layers.forEach((l, i) => {
    const y = layerYStart + i * (layerH + layerGap);
    // Layer name
    slide.addShape(pres.shapes.RECTANGLE, {
      x: layerX, y: y, w: 2.0, h: layerH,
      fill: { color: l.color }, line: { type: "none" }, rectRadius: 0.03
    });
    slide.addText(l.name, {
      x: layerX, y: y, w: 2.0, h: layerH,
      fontSize: 12, fontFace: "Microsoft YaHei", color: "FFFFFF",
      bold: true, align: "center", valign: "middle", margin: 0
    });
    // Modules
    slide.addShape(pres.shapes.RECTANGLE, {
      x: layerX + 2.0, y: y, w: layerW - 2.0, h: layerH,
      fill: { color: theme.light }, line: { type: "none" }, rectRadius: 0.03
    });
    const modText = l.modules.join("    |    ");
    slide.addText(modText, {
      x: layerX + 2.1, y: y, w: layerW - 2.2, h: layerH,
      fontSize: 11, fontFace: "Microsoft YaHei", color: theme.primary,
      align: "left", valign: "middle", margin: 0
    });
  });

  // 6 节点网络拓扑
  const netY = 4.2;
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: netY, w: 9, h: 0.95,
    fill: { color: theme.light }, line: { color: theme.accent, width: 1 }, rectRadius: 0.05
  });
  slide.addText("联盟链 6 节点网络拓扑", {
    x: 0.7, y: netY + 0.08, w: 5, h: 0.3,
    fontSize: 12, fontFace: "Microsoft YaHei", color: theme.accent,
    bold: true, align: "left", valign: "middle", margin: 0
  });

  const nodes = [
    { x: 0.7, y: netY + 0.5, label: "Orderer\n(排序)" },
    { x: 1.85, y: netY + 0.5, label: "本行\nPeer" },
    { x: 3.0, y: netY + 0.5, label: "银保监\nPeer" },
    { x: 4.15, y: netY + 0.5, label: "保险\nPeer" },
    { x: 5.3, y: netY + 0.5, label: "公证处\nPeer" },
    { x: 6.45, y: netY + 0.5, label: "CA\n中心" },
  ];
  nodes.forEach((n) => {
    slide.addShape(pres.shapes.RECTANGLE, {
      x: n.x, y: n.y, w: 1.0, h: 0.4,
      fill: { color: theme.primary }, line: { type: "none" }, rectRadius: 0.03
    });
    slide.addText(n.label, {
      x: n.x, y: n.y, w: 1.0, h: 0.4,
      fontSize: 9, fontFace: "Microsoft YaHei", color: "FFFFFF",
      align: "center", valign: "middle", margin: 0
    });
  });

  // Arrow: P2P connections
  slide.addText("P2P Gossip 协议互联 · Raft 共识 · Kafka 排序", {
    x: 7.6, y: netY + 0.55, w: 1.85, h: 0.3,
    fontSize: 9, fontFace: "Microsoft YaHei", color: theme.accent,
    align: "left", valign: "middle", margin: 0
  });

  slide.addText("06 / 19", {
    x: 9.0, y: 5.25, w: 0.9, h: 0.3,
    fontSize: 10, fontFace: "Arial", color: theme.secondary,
    align: "right"
  });
}

module.exports = { createSlide };
