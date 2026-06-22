package com.minimax.analytics.service.ingest;

import com.minimax.analytics.entity.IngestTask;
import com.minimax.analytics.vo.QualityReport;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文件导入服务接口 (V5.31)
 */
public interface FileIngestService {

    /** 上传并启动解析任务 (返回 taskId) */
    String upload(Long userId, MultipartFile file);

    /** 任务状态 */
    IngestTask status(String taskId);

    /** 质量报告 */
    QualityReport quality(String taskId);

    /** 预览前 N 行 */
    List<java.util.Map<String, Object>> preview(String taskId, int limit);

    /** 重新解析 (改 separator/encoding) */
    void reparse(String taskId, String separator, String encoding);

    /** 历史任务 */
    List<IngestTask> history(Long userId, int page, int size);
}
