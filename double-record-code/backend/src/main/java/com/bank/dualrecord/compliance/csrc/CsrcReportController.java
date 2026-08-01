package com.bank.dualrecord.compliance.csrc;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

/**
 * 银保监报送 Controller
 *
 * <p>提供 4 类报送接口:
 * <ul>
 *   <li>单笔实时报送
 *   <li>日报批量报送
 *   <li>月报汇总报送
 *   <li>投诉举报即时上报
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/compliance/csrc")
@RequiredArgsConstructor
@Tag(name = "银保监报送", description = "中国银保监会合规报送接口")
public class CsrcReportController {

    private final CsrcReportBuilder reportBuilder;
    private final XmlMapper xmlMapper = new XmlMapper();

    /**
     * 单笔实时报送
     */
    @PostMapping(value = "/order", produces = MediaType.APPLICATION_XML_VALUE)
    @Operation(summary = "单笔订单实时报送")
    public ResponseEntity<Resource> submitOrder(@RequestBody CsrcReportBuilder.CsrcOrderData data) {
        log.info("银保监单笔报送: orderId={}", data.getOrderId());
        var report = reportBuilder.buildSingleOrder(data);
        return toXmlResource(report, "order-" + data.getOrderId() + ".xml");
    }

    /**
     * 日报批量报送
     */
    @PostMapping(value = "/daily", produces = MediaType.APPLICATION_XML_VALUE)
    @Operation(summary = "日报批量报送")
    public ResponseEntity<Resource> submitDaily(
        @RequestParam String institutionCode,
        @RequestParam String institutionName,
        @RequestParam String date,
        @RequestBody List<CsrcReportBuilder.CsrcOrderData> orders
    ) {
        log.info("银保监日报报送: date={}, orders={}", date, orders.size());
        var report = reportBuilder.buildDailyReport(
            institutionCode, institutionName, LocalDate.parse(date), orders);
        return toXmlResource(report, "daily-" + date + ".xml");
    }

    /**
     * 投诉举报即时上报
     */
    @PostMapping(value = "/complaint", produces = MediaType.APPLICATION_XML_VALUE)
    @Operation(summary = "客户投诉即时上报")
    public ResponseEntity<Resource> submitComplaint(@RequestBody ComplaintReport report) {
        log.warn("银保监投诉上报: orderId={}, type={}", report.orderId, report.complaintType);
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<投诉举报>\n" +
            "  <订单号>" + report.orderId + "</订单号>\n" +
            "  <客户号>" + report.customerNo + "</客户号>\n" +
            "  <投诉类型>" + report.complaintType + "</投诉类型>\n" +
            "  <投诉内容>" + report.description + "</投诉内容>\n" +
            "  <上报时间>" + LocalDate.now() + "</上报时间>\n" +
            "</投诉举报>";
        ByteArrayResource resource = new ByteArrayResource(xml.getBytes(StandardCharsets.UTF_8));
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_XML)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=complaint-" + report.orderId + ".xml")
            .body(resource);
    }

    private ResponseEntity<Resource> toXmlResource(Object obj, String filename) {
        try {
            byte[] xml = xmlMapper.writeValueAsBytes(obj);
            ByteArrayResource resource = new ByteArrayResource(xml);
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .body(resource);
        } catch (Exception e) {
            log.error("XML 序列化失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 投诉举报报文
     */
    @lombok.Data
    public static class ComplaintReport {
        private String orderId;
        private String customerNo;
        private String complaintType;
        private String description;
    }
}
