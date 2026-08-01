# 双录一体化平台 - 全量交付

> **Dual-Record Compliance Platform** · Java + TypeScript · 4-组织联盟链 · 国密 SM2/SM3/SM4

金融行业双录(销售录音录像)业务的端到端技术方案,覆盖:
- **领导决策** PPT(16 页)
- **技术评审** DOCX(32 页 + 10 张架构图)
- **前端代码** Vue3 + SQL(24 文件)
- **区块链链码** Java + Fabric 2.4(38 文件,3 合约)
- **话术 SDK** TypeScript(33 文件,7 模块)
- **深化设计** PPT(19 页,区块链 + 话术 + UX)

## 目录结构

| 目录 | 说明 | 行数 |
|------|------|------|
| `double-record-ppt/` | 16 页领导汇报 PPT | - |
| `double-record-spec/` | 32 页技术方案 DOCX + 10 图 | - |
| `double-record-code/` | Vue3 前端 + SQL 5 张表 | 5183 |
| `blockchain-script-ppt/` | 19 页深度设计 PPT | - |
| `fabric-chaincode-java/` | Java 链码(3 合约) | 4244 |
| `script-sdk/` | TypeScript SDK(7 模块) | 10803 |

## 技术栈

- **后端**: Spring Boot · Java 11+ · Hyperledger Fabric 2.4
- **前端**: Vue 3 · TypeScript · Element Plus
- **链码**: Java 11 · BouncyCastle 1.70 · Fabric Java SDK
- **SDK**: TypeScript 5 · Jest · 国密 SM3 纯 JS
- **国密**: SM2 签名 · SM3 摘要 · SM4 对称加密
- **联盟链**: 4 组织(本行/银保监/保险/公证处)· 4/4 背书策略

## 推荐阅读顺序

1. **领导决策**:`double-record-ppt/double-record-integration-design.pptx`
2. **技术评审**:`double-record-spec/双录一体化平台技术方案说明书.docx`
3. **代码实现**:`double-record-code/` → `fabric-chaincode-java/` → `script-sdk/`
4. **深化设计**:`blockchain-script-ppt/blockchain-script-design.pptx`

## 部署顺序

1. `double-record-code/sql/` - 建库建表(MySQL/MariaDB)
2. `fabric-chaincode-java/` - 部署链码(参考 `deploy/network-config.sh`)
3. `script-sdk/` - 集成到前端(`npm install && npm run build`)
4. `double-record-code/frontend/` - 启动前端

## 核心交付指标

| 指标 | 数值 |
|------|------|
| PPT 页数 | 16 + 19 = 35 |
| DOCX 页数 | 32 |
| 架构图 | 10 张(PNG,Python matplotlib 生成) |
| 前端代码行数 | 5183 |
| 链码行数 | 4244 |
| SDK 行数 | 10803 |
| 测试用例 | Java 53 + TypeScript 62 = 115 |
| 文档字数 | ~50000 字 |

## 维护联系

- **架构设计**: Mavis
- **国密适配**: 信息安全部
- **业务对接**: 双录业务部
- **联盟链运维**: 区块链团队

## 许可证

Apache-2.0

---

**版本**: 1.0.0
**最后更新**: 2026-08-01
**状态**: 生产就绪 ✅
