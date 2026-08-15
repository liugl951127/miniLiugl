package com.minimax.model.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.minimax.model.dto.ChatRequest;
import com.minimax.model.entity.ModelConfig;
import com.minimax.model.entity.ModelProvider;
import com.minimax.model.mapper.ModelConfigMapper;
import com.minimax.model.mapper.ModelProviderMapper;
import com.minimax.model.service.ModelService;
import com.minimax.model.vo.ChatResponse;
import com.minimax.model.vo.ModelVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * V7.0: h2local 沙箱模式 ModelService。
 * V7.0 Flow④+②: 动态从数据库加载训练模型（打通训练→Agent链路）
 */
@Slf4j
@Service
@Primary
@Profile("h2local")
@RequiredArgsConstructor
public class HardcodedModelService implements ModelService {

    /** 内置模型（总是可用） */
    private static final List<ModelVO> BUILTIN = List.of(
            ModelVO.builder().id(1L).code("deepseek-chat").displayName("DeepSeek Chat")
                    .maxContext(16384).maxOutput(4096).supportsStream(true)
                    .providerId(1L).providerCode("deepseek").providerName("DeepSeek").build(),
            ModelVO.builder().id(2L).code("MiniMax-Text-01").displayName("MiniMax Text")
                    .maxContext(8192).maxOutput(2048).supportsStream(true)
                    .providerId(2L).providerCode("minimax").providerName("MiniMax").build(),
            ModelVO.builder().id(3L).code("deepseek-coder").displayName("DeepSeek Coder")
                    .maxContext(16384).maxOutput(4096).supportsStream(true)
                    .providerId(1L).providerCode("deepseek").providerName("DeepSeek").build()
    );

    private final ModelConfigMapper modelConfigMapper;
    private final ModelProviderMapper providerMapper;

    @Override
    public List<ModelVO> listEnabled() {
        // V7.0 Flow④: 动态加载训练模型
        List<ModelVO> result = new ArrayList<>(BUILTIN);
        try {
            List<ModelConfig> trained = modelConfigMapper.selectList(
                    new LambdaQueryWrapper<ModelConfig>()
                            .eq(ModelConfig::getEnabled, 1)
                            .eq(ModelConfig::getProviderId, 3)  // provider_id=3 是训练模型分类
            );
            Map<Long, ModelProvider> providerMap = providerMapper.selectList(null).stream()
                    .collect(Collectors.toMap(ModelProvider::getId, p -> p));
            for (ModelConfig m : trained) {
                ModelProvider p = providerMap.get(m.getProviderId());
                result.add(ModelVO.builder()
                        .id(m.getId())
                        .code(m.getModelCode())
                        .displayName(m.getDisplayName())
                        .maxContext(m.getMaxContext())
                        .maxOutput(m.getMaxOutput())
                        .supportsStream(m.getSupportsStream() != null && m.getSupportsStream() == 1)
                        .providerId(p != null ? p.getId() : 3L)
                        .providerCode(p != null ? p.getCode() : "trained")
                        .providerName(p != null ? p.getName() : "训练模型")
                        .build());
            }
            log.info("[HardcodedModelService] 加载 {} 个训练模型, 总计 {} 个模型", trained.size(), result.size());
        } catch (Exception e) {
            log.warn("[HardcodedModelService] 加载训练模型失败: {}", e.getMessage());
        }
        return result;
    }

    @Override
    public ChatResponse chat(Long userId, ChatRequest req) {
        String modelCode = req.getModel();
        ModelVO model = listEnabled().stream()
                .filter(m -> modelCode.equals(m.getCode()))
                .findFirst()
                .orElse(listEnabled().get(0));

        log.info("[HardcodedModelService] chat: model={}, provider={}", modelCode, model.getProviderCode());
        return ChatResponse.builder()
                .id("sandbox-" + System.currentTimeMillis())
                .model(modelCode)
                .content("✅ Sandbox 模式: AI 模型服务正常运行中!\n\n" +
                        "当前模型: " + modelCode + " (" + model.getDisplayName() + ")\n" +
                        "提供商: " + model.getProviderName() + "\n" +
                        "提示: 沙箱模式下使用模拟数据，生产环境请配置真实的 API key。")
                .totalTokens(50)
                .latencyMs(100L)
                .providerCode(model.getProviderCode())
                .build();
    }

    @Override
    public Flux<String> stream(Long userId, ChatRequest req) {
        return Flux.just("✅ Sandbox 模式流式响应...");
    }
}
