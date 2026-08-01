// 智能合约设计
function createSlide(pres, theme) {
  const slide = pres.addSlide();
  slide.background = { color: theme.bg };

  // Title
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: 0.4, w: 0.08, h: 0.5,
    fill: { color: theme.accent }, line: { type: "none" }
  });
  slide.addText("智能合约设计:Chaincode 关键逻辑", {
    x: 0.7, y: 0.4, w: 7.5, h: 0.5,
    fontSize: 20, fontFace: "Microsoft YaHei", color: theme.primary,
    bold: true, align: "left", valign: "middle", margin: 0
  });
  slide.addText("CHAINCODE", {
    x: 8.2, y: 0.5, w: 1.3, h: 0.3,
    fontSize: 10, fontFace: "Arial", color: theme.secondary,
    charSpacing: 3, align: "right"
  });

  // Left: 4 core methods
  slide.addText("核心合约方法 (Go / Node.js)", {
    x: 0.5, y: 1.0, w: 4.5, h: 0.3,
    fontSize: 13, fontFace: "Microsoft YaHei", color: theme.accent,
    bold: true, align: "left", margin: 0
  });

  const methods = [
    { name: "submitEvidence", param: "(orderId, hash, signature)", ret: "txId" },
    { name: "verifyEvidence", param: "(orderId, hash)", ret: "bool, timestamp" },
    { name: "freezeContract", param: "(orderId, parties)", ret: "blockHeight" },
    { name: "queryHistory",  param: "(orderId)", ret: "evidence list" },
  ];

  methods.forEach((m, i) => {
    const y = 1.4 + i * 0.65;
    slide.addShape(pres.shapes.RECTANGLE, {
      x: 0.5, y: y, w: 4.5, h: 0.6,
      fill: { color: theme.light }, line: { type: "none" }, rectRadius: 0.03
    });
    slide.addShape(pres.shapes.RECTANGLE, {
      x: 0.5, y: y, w: 0.08, h: 0.6,
      fill: { color: theme.accent }, line: { type: "none" }
    });
    slide.addText(m.name + "()", {
      x: 0.65, y: y + 0.05, w: 2.0, h: 0.25,
      fontSize: 11, fontFace: "Arial", color: theme.accent,
      bold: true, align: "left", valign: "middle", margin: 0
    });
    slide.addText(m.param, {
      x: 0.65, y: y + 0.3, w: 3.0, h: 0.25,
      fontSize: 9, fontFace: "Arial", color: theme.secondary,
      align: "left", valign: "middle", margin: 0
    });
    slide.addText("→ " + m.ret, {
      x: 3.7, y: y + 0.15, w: 1.25, h: 0.3,
      fontSize: 9, fontFace: "Arial", color: theme.primary,
      align: "right", valign: "middle", margin: 0
    });
  });

  // Right: code sample
  slide.addText("合约代码示例(Go)", {
    x: 5.3, y: 1.0, w: 4.2, h: 0.3,
    fontSize: 13, fontFace: "Microsoft YaHei", color: theme.accent,
    bold: true, align: "left", margin: 0
  });

  slide.addShape(pres.shapes.RECTANGLE, {
    x: 5.3, y: 1.4, w: 4.2, h: 3.0,
    fill: { color: theme.primary }, line: { type: "none" }, rectRadius: 0.03
  });

  const code = `func (s *EvidenceContract) SubmitEvidence(
    ctx contractapi.TransactionContextInterface,
    orderId string,
    videoHash string,
    audioHash string,
    contractHash string,
    signature string,
) error {
    // 1. 参数校验
    if orderId == "" || videoHash == "" {
        return errors.New("参数缺失")
    }

    // 2. 国密签名校验
    if !sm2.Verify(orderId+videoHash, signature) {
        return errors.New("签名无效")
    }

    // 3. 构造证据包
    evidence := Evidence{
        OrderId:     orderId,
        VideoHash:   videoHash,
        AudioHash:   audioHash,
        ContractHash: contractHash,
        Signature:   signature,
        Timestamp:   getTrustTime(),
        Submitter:   getCreator(ctx),
    }

    // 4. 写入账本
    evidenceBytes, _ := json.Marshal(evidence)
    return ctx.GetStub().PutState(
        orderId, evidenceBytes,
    )
}`;

  slide.addText(code, {
    x: 5.4, y: 1.5, w: 4.0, h: 2.8,
    fontSize: 7.5, fontFace: "Arial", color: "FFFFFF",
    align: "left", valign: "top", margin: 0
  });

  // 合约设计原则
  const bpY = 4.5;
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: bpY, w: 9, h: 0.6,
    fill: { color: theme.light }, line: { color: theme.accent, width: 1 }, rectRadius: 0.05
  });
  slide.addText("【合约设计原则】幂等性 + 最小权限 + 事件触发 + 可升级(链码版本化)", {
    x: 0.7, y: bpY + 0.05, w: 8.6, h: 0.25,
    fontSize: 11, fontFace: "Microsoft YaHei", color: theme.accent,
    bold: true, align: "left", valign: "middle", margin: 0
  });
  slide.addText("每次写入用 orderId 作 key 实现幂等;Read-only 调用免费;写操作走背书策略需 2 节点签名。", {
    x: 0.7, y: bpY + 0.3, w: 8.6, h: 0.25,
    fontSize: 10, fontFace: "Microsoft YaHei", color: theme.primary,
    align: "left", valign: "middle", margin: 0
  });

  // 细节点
  slide.addShape(pres.shapes.RECTANGLE, {
    x: 0.5, y: 5.2, w: 9, h: 0.32,
    fill: { color: theme.accent, transparency: 85 }, line: { type: "none" }, rectRadius: 0.03
  });
  slide.addText("【细节点 4】合约升级:Hyperledger Fabric 链码版本化机制,新版本经 4 节点共识后启用,旧版本保留。", {
    x: 0.6, y: 5.2, w: 8.8, h: 0.32,
    fontSize: 10, fontFace: "Microsoft YaHei", color: theme.primary,
    bold: true, align: "left", valign: "middle", margin: 0
  });

  slide.addText("08 / 19", {
    x: 9.0, y: 5.55, w: 0.9, h: 0.07,
    fontSize: 8, fontFace: "Arial", color: theme.secondary,
    align: "right"
  });
}

module.exports = { createSlide };
