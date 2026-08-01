package com.bank.dualrecord.compliance.csrc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 银保监报送报文生成器
 *
 * <p>协议:中国银保监会《保险销售行为可回溯管理办法》
 * <p>格式:XML(国标 GB/T 25064)
 * <p>字段:约 80 个,覆盖客户、产品、销售、合同、回溯
 *
 * <p>报送频率:
 * <ul>
 *   <li>实时:客户投诉/重大违规 → 即时上报
 *   <li>日终:当日双录完成订单 → T+1 09:00 前
 *   <li>月度:汇总数据 → 次月 5 日前
 * </ul>
 *
 * @author Mavis
 */
@Slf4j
@Component
public class CsrcReportBuilder {

    private static final DateTimeFormatter DATE_FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.of("Asia/Shanghai"));
    private static final DateTimeFormatter DATETIME_FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("Asia/Shanghai"));

    /**
     * 构建单笔双录订单的报送报文
     */
    public CsrcDualRecordReport buildSingleOrder(CsrcOrderData data) {
        log.info("构建单笔订单报送: orderId={}", data.getOrderId());

        CsrcDualRecordReport report = new CsrcDualRecordReport();
        report.setVersion("1.0");
        report.setInstitutionCode(data.getInstitutionCode());
        report.setInstitutionName(data.getInstitutionName());
        report.setReportType("REAL_TIME_DUAL_RECORD");
        report.setReportTime(DATETIME_FMT.format(Instant.now()));
        report.setBusinessNo(data.getOrderNo());
        report.setDualRecordInfo(buildDualRecordInfo(data));
        return report;
    }

    /**
     * 构建日报(批量)
     */
    public CsrcDailyReport buildDailyReport(String institutionCode, String institutionName, LocalDate reportDate, List<CsrcOrderData> orders) {
        log.info("构建日报: date={}, orders={}", reportDate, orders.size());

        CsrcDailyReport report = new CsrcDailyReport();
        report.setVersion("1.0");
        report.setInstitutionCode(institutionCode);
        report.setInstitutionName(institutionName);
        report.setReportType("DAILY_DUAL_RECORD");
        report.setReportDate(reportDate.format(DATE_FMT));
        report.setReportTime(DATETIME_FMT.format(Instant.now()));
        report.setTotalCount(orders.size());

        // 统计
        int completed = 0, failed = 0, cancelled = 0;
        for (CsrcOrderData o : orders) {
            if (o.getState() == 6) completed++;
            if (o.getState() == -2) failed++;
            if (o.getState() == -1) cancelled++;
        }
        report.setCompletedCount(completed);
        report.setFailedCount(failed);
        report.setCancelledCount(cancelled);

        // 明细
        List<CsrcDualRecordReport> details = new ArrayList<>();
        for (CsrcOrderData o : orders) {
            details.add(buildSingleOrder(o));
        }
        report.setDetails(details);
        return report;
    }

    private CsrcDualRecordInfo buildDualRecordInfo(CsrcOrderData data) {
        CsrcDualRecordInfo info = new CsrcDualRecordInfo();
        info.setOrderNo(data.getOrderNo());
        info.setProductType(data.getProductType());
        info.setProductName(data.getProductName());
        info.setAmount(data.getAmount());
        info.setCustomer(buildCustomer(data));
        info.setSales(buildSales(data));
        info.setDualRecord(buildDualRecord(data));
        info.setEvidenceHash(data.getEvidenceHash());
        info.setBlockChainTx(data.getBlockChainTx());
        return info;
    }

    private CsrcCustomer buildCustomer(CsrcOrderData d) {
        CsrcCustomer c = new CsrcCustomer();
        c.setCustomerNo(d.getCustomerNo());
        c.setName(d.getCustomerName());
        c.setIdType(d.getIdType());
        c.setIdNoMask(maskIdNo(d.getIdNo()));
        c.setMobileMask(maskMobile(d.getMobile()));
        c.setRiskLevel(d.getRiskLevel());
        return c;
    }

    private CsrcSales buildSales(CsrcOrderData d) {
        CsrcSales s = new CsrcSales();
        s.setSalesUserNo(d.getSalesUserNo());
        s.setSalesName(d.getSalesName());
        s.setBranchCode(d.getBranchCode());
        s.setBranchName(d.getBranchName());
        s.setChannel(d.getChannel());
        return s;
    }

    private CsrcDualRecord buildDualRecord(CsrcOrderData d) {
        CsrcDualRecord r = new CsrcDualRecord();
        r.setSessionId(d.getSessionId());
        r.setStartTime(d.getStartTime());
        r.setEndTime(d.getEndTime());
        r.setVideoUrl(d.getVideoUrl());
        r.setVideoHash(d.getVideoHash());
        r.setVideoDuration(d.getVideoDuration());
        r.setScriptVersion(d.getScriptVersion());
        r.setScriptHash(d.getScriptHash());
        r.setMerkleRoot(d.getMerkleRoot());
        r.setState(d.getState());
        r.setResult(d.getResult());
        r.setQualityScore(d.getQualityScore());
        r.setQualityVerdict(d.getQualityVerdict());
        return r;
    }

    private String maskIdNo(String idNo) {
        if (idNo == null || idNo.length() < 8) return "****";
        return idNo.substring(0, 4) + "**********" + idNo.substring(idNo.length() - 4);
    }

    private String maskMobile(String mobile) {
        if (mobile == null || mobile.length() < 11) return "****";
        return mobile.substring(0, 3) + "****" + mobile.substring(7);
    }

    // ============================================================
    // 报文数据类(对应 XML schema)
    // ============================================================

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JacksonXmlRootElement(localName = "银保监双录报送")
    public static class CsrcDualRecordReport {
        @JacksonXmlProperty(localName = "版本号")
        private String version;
        @JacksonXmlProperty(localName = "机构代码")
        private String institutionCode;
        @JacksonXmlProperty(localName = "机构名称")
        private String institutionName;
        @JacksonXmlProperty(localName = "报送类型")
        private String reportType;
        @JacksonXmlProperty(localName = "报送时间")
        private String reportTime;
        @JacksonXmlProperty(localName = "业务流水号")
        private String businessNo;
        @JacksonXmlProperty(localName = "双录信息")
        private CsrcDualRecordInfo dualRecordInfo;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CsrcDualRecordInfo {
        @JacksonXmlProperty(localName = "订单号")
        private String orderNo;
        @JacksonXmlProperty(localName = "产品类型")
        private Integer productType;
        @JacksonXmlProperty(localName = "产品名称")
        private String productName;
        @JacksonXmlProperty(localName = "金额")
        private String amount;
        @JacksonXmlProperty(localName = "客户信息")
        private CsrcCustomer customer;
        @JacksonXmlProperty(localName = "销售信息")
        private CsrcSales sales;
        @JacksonXmlProperty(localName = "双录详情")
        private CsrcDualRecord dualRecord;
        @JacksonXmlProperty(localName = "证据哈希")
        private String evidenceHash;
        @JacksonXmlProperty(localName = "区块链交易号")
        private String blockChainTx;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CsrcCustomer {
        @JacksonXmlProperty(localName = "客户号")
        private String customerNo;
        @JacksonXmlProperty(localName = "姓名")
        private String name;
        @JacksonXmlProperty(localName = "证件类型")
        private Integer idType;
        @JacksonXmlProperty(localName = "证件号_脱敏")
        private String idNoMask;
        @JacksonXmlProperty(localName = "手机号_脱敏")
        private String mobileMask;
        @JacksonXmlProperty(localName = "风险等级")
        private String riskLevel;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CsrcSales {
        @JacksonXmlProperty(localName = "客户经理号")
        private String salesUserNo;
        @JacksonXmlProperty(localName = "客户经理姓名")
        private String salesName;
        @JacksonXmlProperty(localName = "网点编号")
        private String branchCode;
        @JacksonXmlProperty(localName = "网点名称")
        private String branchName;
        @JacksonXmlProperty(localName = "渠道")
        private Integer channel;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CsrcDualRecord {
        @JacksonXmlProperty(localName = "会话ID")
        private String sessionId;
        @JacksonXmlProperty(localName = "开始时间")
        private String startTime;
        @JacksonXmlProperty(localName = "结束时间")
        private String endTime;
        @JacksonXmlProperty(localName = "视频地址")
        private String videoUrl;
        @JacksonXmlProperty(localName = "视频哈希")
        private String videoHash;
        @JacksonXmlProperty(localName = "视频时长")
        private Integer videoDuration;
        @JacksonXmlProperty(localName = "话术版本")
        private String scriptVersion;
        @JacksonXmlProperty(localName = "话术哈希")
        private String scriptHash;
        @JacksonXmlProperty(localName = "Merkle根")
        private String merkleRoot;
        @JacksonXmlProperty(localName = "状态")
        private Integer state;
        @JacksonXmlProperty(localName = "质检结果")
        private String result;
        @JacksonXmlProperty(localName = "质检分数")
        private Double qualityScore;
        @JacksonXmlProperty(localName = "质检评级")
        private String qualityVerdict;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JacksonXmlRootElement(localName = "日报")
    public static class CsrcDailyReport {
        @JacksonXmlProperty(localName = "版本号")
        private String version;
        @JacksonXmlProperty(localName = "机构代码")
        private String institutionCode;
        @JacksonXmlProperty(localName = "机构名称")
        private String institutionName;
        @JacksonXmlProperty(localName = "报送类型")
        private String reportType;
        @JacksonXmlProperty(localName = "报送日期")
        private String reportDate;
        @JacksonXmlProperty(localName = "报送时间")
        private String reportTime;
        @JacksonXmlProperty(localName = "总笔数")
        private Integer totalCount;
        @JacksonXmlProperty(localName = "完成笔数")
        private Integer completedCount;
        @JacksonXmlProperty(localName = "失败笔数")
        private Integer failedCount;
        @JacksonXmlProperty(localName = "取消笔数")
        private Integer cancelledCount;
        @JacksonXmlProperty(localName = "明细")
        private List<CsrcDualRecordReport> details;
    }

    @Data
    public static class CsrcOrderData {
        private String orderId;
        private String orderNo;
        private String customerNo;
        private String customerName;
        private Integer idType;
        private String idNo;
        private String mobile;
        private String riskLevel;
        private Integer productType;
        private String productName;
        private String amount;
        private String salesUserNo;
        private String salesName;
        private String branchCode;
        private String branchName;
        private Integer channel;
        private String sessionId;
        private String startTime;
        private String endTime;
        private String videoUrl;
        private String videoHash;
        private Integer videoDuration;
        private String scriptVersion;
        private String scriptHash;
        private String merkleRoot;
        private Integer state;
        private String result;
        private Double qualityScore;
        private String qualityVerdict;
        private String evidenceHash;
        private String blockChainTx;
        private String institutionCode;
        private String institutionName;
    }
}
