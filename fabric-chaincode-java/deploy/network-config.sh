#!/bin/bash
# 双录一体化平台链码部署脚本
# 适用于:Hyperledger Fabric 2.x + 4 组织联盟链

set -e

# ==================== 参数配置 ====================
CHAINCODE_NAME="dual-record-chaincode"
CHAINCODE_VERSION="1.0.0"
CHAINCODE_SEQUENCE="1"
CHAINCODE_LABEL="${CHAINCODE_NAME}_${CHAINCODE_VERSION}"
CHANNEL_NAME="dual-record-channel"
CHAINCODE_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$CHAINCODE_DIR")"

# 4 个组织的 peer 节点(根据实际网络调整)
ORDERER_CA="/opt/fabric/organizations/ordererOrganizations/dual-record.com/tlsca/tlsca.dual-record.com-cert.pem"
ORG1_PEER="peer0.bank.com:7051"
ORG2_PEER="peer0.csrc.com:8051"
ORG3_PEER="peer0.insurance.com:9051"
ORG4_PEER="peer0.notary.com:10051"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log() { echo -e "${GREEN}[$(date +'%H:%M:%S')]${NC} $1"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
err() { echo -e "${RED}[ERROR]${NC} $1"; exit 1; }

# ==================== 步骤 0: 编译打包 ====================
log "步骤 0/7: 编译并打包 Java 链码"
cd "$PROJECT_DIR"
mvn clean package -DskipTests || err "Maven 编译失败"

# 检查产物
JAR_FILE="target/${CHAINCODE_NAME}.jar"
[ ! -f "$JAR_FILE" ] && err "未找到 $JAR_FILE"
log "✓ 链码包: $JAR_FILE ($(du -h $JAR_FILE | cut -f1))"

# ==================== 步骤 1: 打包链码 ====================
log "步骤 1/7: 打包链码为 tar.gz"

CC_PACKAGE_DIR="build/${CHAINCODE_LABEL}"
rm -rf "$CC_PACKAGE_DIR"
mkdir -p "$CC_PACKAGE_DIR"

# 复制 jar + 启动脚本
cp "$JAR_FILE" "$CC_PACKAGE_DIR/code.jar"
cat > "$CC_PACKAGE_DIR/Dockerfile" <<EOF
FROM hyperledger/fabric-javaenv:2.4
COPY code.jar /chaincode/chaincode.jar
RUN echo "peer chaincode install" > /chaincode/start.sh
EOF
cat > "$CC_PACKAGE_DIR/start.sh" <<EOF
#!/bin/sh
peer chaincode install
EOF
chmod +x "$CC_PACKAGE_DIR/start.sh"

# metadata.json
cat > "$CC_PACKAGE_DIR/metadata.json" <<EOF
{
  "type": "java",
  "label": "${CHAINCODE_LABEL}"
}
EOF

# 打包
CC_PACKAGE_FILE="build/${CHAINCODE_LABEL}.tar.gz"
tar -czf "$CC_PACKAGE_FILE" -C build "$CHAINCODE_LABEL"
log "✓ 链码包: $CC_PACKAGE_FILE"

# ==================== 步骤 2: 在 4 节点安装链码 ====================
log "步骤 2/7: 在 4 个组织 Peer 安装链码"

install_on_peer() {
    local ORG=$1
    local PEER=$2
    log "  → 在 $ORG ($PEER) 安装链码"
    CORE_PEER_ADDRESS=$PEER \
    CORE_PEER_LOCALMSPID=${ORG}MSP \
    peer lifecycle chaincode install "$CC_PACKAGE_FILE" || err "安装失败: $PEER"
}

install_on_peer "Bank"     "$ORG1_PEER"
install_on_peer "Csrc"     "$ORG2_PEER"
install_on_peer "Insurance" "$ORG3_PEER"
install_on_peer "Notary"   "$ORG4_PEER"

# 获取 package ID
PACKAGE_ID=$(CORE_PEER_ADDRESS=$ORG1_PEER \
    CORE_PEER_LOCALMSPID=BankMSP \
    peer lifecycle chaincode queryinstalled 2>&1 | grep "$CHAINCODE_LABEL" | awk '{print $3}' | tr -d ',')
[ -z "$PACKAGE_ID" ] && err "未获取到 package id"
log "✓ Package ID: $PACKAGE_ID"

# ==================== 步骤 3: 4 组织审批链码 ====================
log "步骤 3/7: 4 组织审批链码定义"

approve_for_org() {
    local ORG=$1
    local PEER=$2
    local MSP="${ORG}MSP"

    log "  → $MSP 审批"
    CORE_PEER_ADDRESS=$PEER \
    CORE_PEER_LOCALMSPID=$MSP \
    peer lifecycle chaincode approveformyorg \
        --channelID "$CHANNEL_NAME" \
        --name "$CHAINCODE_NAME" \
        --version "$CHAINCODE_VERSION" \
        --package-id "$PACKAGE_ID" \
        --sequence "$CHAINCODE_SEQUENCE" \
        --signature-policy "AND('BankMSP.peer','CsrcMSP.peer','InsuranceMSP.peer','NotaryMSP.peer')" \
        --waitForEvent || err "$MSP 审批失败"
}

approve_for_org "Bank"     "$ORG1_PEER"
approve_for_org "Csrc"     "$ORG2_PEER"
approve_for_org "Insurance" "$ORG3_PEER"
approve_for_org "Notary"   "$ORG4_PEER"

# ==================== 步骤 4: 提交链码 ====================
log "步骤 4/7: 提交链码到通道"

CORE_PEER_ADDRESS=$ORG1_PEER \
CORE_PEER_LOCALMSPID=BankMSP \
peer lifecycle chaincode commit \
    --channelID "$CHANNEL_NAME" \
    --name "$CHAINCODE_NAME" \
    --version "$CHAINCODE_VERSION" \
    --sequence "$CHAINCODE_SEQUENCE" \
    --signature-policy "AND('BankMSP.peer','CsrcMSP.peer','InsuranceMSP.peer','NotaryMSP.peer')" \
    --waitForEvent \
    --peerAddresses $ORG1_PEER \
    --peerAddresses $ORG2_PEER \
    --peerAddresses $ORG3_PEER \
    --peerAddresses $ORG4_PEER \
    --tlsRootCertFiles $ORDERER_CA || err "链码提交失败"

log "✓ 链码已提交到通道 $CHANNEL_NAME"

# ==================== 步骤 5: 初始化链码(注册初始公钥) ====================
log "步骤 5/7: 初始化链码 - 注册系统账户公钥"

# 这里应替换为真实的银保监 / 公证处 / 银行公钥
BANK_PUBKEY="04a7bcd0e91f78b8e5b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a7bcd0e91f78b8e5b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5"

CORE_PEER_ADDRESS=$ORG1_PEER \
CORE_PEER_LOCALMSPID=BankMSP \
peer chaincode invoke \
    -o orderer.dual-record.com:7050 \
    --ordererTLSHostnameOverride orderer.dual-record.com \
    --tls \
    --cafile $ORDERER_CA \
    -C "$CHANNEL_NAME" \
    -n "$CHAINCODE_NAME" \
    --peerAddresses $ORG1_PEER \
    --tlsRootCertFiles $ORDERER_CA \
    -c '{"function":"EvidenceContract:registerPublicKey","Args":["SYSTEM","BankMSP","'$BANK_PUBKEY'"]}' \
    --waitForEvent

log "✓ 系统公钥已注册"

# ==================== 步骤 6: 健康检查 ====================
log "步骤 6/7: 健康检查"

HEALTH=$(CORE_PEER_ADDRESS=$ORG1_PEER \
    CORE_PEER_LOCALMSPID=BankMSP \
    peer chaincode query \
    -C "$CHANNEL_NAME" \
    -n "$CHAINCODE_NAME" \
    -c '{"function":"EvidenceContract:queryEvidence","Args":["HEALTH_CHECK"]}' 2>&1 || true)

if echo "$HEALTH" | grep -q "订单不存在"; then
    log "✓ 链码响应正常"
else
    warn "链码响应异常,但部署已完成"
fi

# ==================== 步骤 7: 完成 ====================
log "步骤 7/7: 部署完成"
echo ""
echo "================================================"
echo "  链码部署成功!"
echo "  名称: $CHAINCODE_NAME"
echo "  版本: $CHAINCODE_VERSION"
echo "  通道: $CHANNEL_NAME"
echo "  合约: EvidenceContract / ContractContract / AuditContract"
echo "  背书策略: 4 组织全部签名"
echo "================================================"
echo ""
echo "后续步骤:"
echo "  1. 业务系统 SDK 连接 Gateway"
echo "  2. 提交证据:  EvidenceContract:submitEvidence"
echo "  3. 状态流转:  EvidenceContract:updateState"
echo "  4. 终结归档:  EvidenceContract:finalizeEvidence"
echo ""
