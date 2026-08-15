package com.minimax.model.init;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.minimax.model.entity.ModelConfig;
import com.minimax.model.entity.ModelProvider;
import com.minimax.model.mapper.ModelConfigMapper;
import com.minimax.model.mapper.ModelProviderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 自研 ONNX 模型启动初始化器 (V7.3)
 *
 * <h3>自动扫描模式，零配置</h3>
 * 配置项: application-common.yml → minimax.self-models
 *
 * <h3>初始化流程</h3>
 * 1. 检查 enabled 配置，若禁用则跳过
 * 2. 扫描 base-dir 目录下的所有 .onnx 文件
 * 3. 自动注册所有扫描到的 ONNX 文件（无需手动配置）
 *    - 配置中手动定义的模型优先使用配置元信息
 *    - 未在配置中定义的 ONNX 文件自动使用默认参数注册
 * 4. 若 provider 不存在则创建，已存在则复用
 *
 * <h3>约定</h3>
 * - provider.protocol = "local"
 * - provider.base_url = 配置的 base-dir
 * - 模型文件路径 = base_dir + "/" + model_code + ".onnx"
 * - apiKey（OnnxLLMAdapter 用）= model_code（不含 .onnx 后缀）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SelfModelInitializer implements ApplicationRunner {

    private final SelfModelsProperties props;
    private final ModelProviderMapper providerMapper;
    private final ModelConfigMapper modelConfigMapper;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!props.isEnabled()) {
            log.info("[SelfModelInit] 已禁用，跳过自研模型加载");
            return;
        }

        log.info("[SelfModelInit] 开始自研模型初始化 (baseDir={})", props.getBaseDir());

        // 1. 扫描 ONNX 文件
        Set<String> availableOnnxFiles = scanOnnxFiles(props.getBaseDir());
        if (availableOnnxFiles.isEmpty()) {
            log.warn("[SelfModelInit] 目录下未发现任何 .onnx 文件: {}\n" +
                     "  → 若尚未部署模型，请将 .onnx 文件放入该目录后重启",
                     props.getBaseDir());
        } else {
            log.info("[SelfModelInit] 扫描到 {} 个 ONNX 文件: {}", availableOnnxFiles.size(), availableOnnxFiles);
        }

        // 2. 构建有效模型列表（配置 × ONNX 存在性交叉验证）
        List<SelfModelsProperties.ModelDef> validModels = resolveValidModels(availableOnnxFiles);

        // 3. 查找或创建 provider
        ModelProvider provider = ensureProvider();
        if (provider == null) {
            log.error("[SelfModelInit] 无法创建 provider，跳过模型注册");
            return;
        }

        // 4. 注册有效模型
        int added = 0, skipped = 0;
        for (SelfModelsProperties.ModelDef def : validModels) {
            ModelConfig existing = modelConfigMapper.selectOne(
                    new LambdaQueryWrapper<ModelConfig>()
                            .eq(ModelConfig::getModelCode, def.getCode())
                            .eq(ModelConfig::getProviderId, provider.getId()));
            if (existing != null) {
                skipped++;
                continue;
            }

            ModelConfig model = new ModelConfig();
            model.setProviderId(provider.getId());
            model.setModelCode(def.getCode());
            model.setDisplayName(
                    def.getDisplayName() != null ? def.getDisplayName() : def.getCode());
            model.setDescription(def.getDescription() != null ? def.getDescription() : "");
            model.setMaxContext(def.getMaxContext());
            model.setMaxOutput(def.getMaxOutput());
            model.setInputPrice(def.getInputPrice());
            model.setOutputPrice(def.getOutputPrice());
            model.setSupportsVision(def.isSupportsVision() ? 1 : 0);
            model.setSupportsTools(0);
            model.setSupportsStream(def.isSupportsStream() ? 1 : 0);
            model.setEnabled(1);
            modelConfigMapper.insert(model);

            log.info("[SelfModelInit] ✅ 注册自研模型: {} ({})",
                    def.getCode(), model.getDisplayName());
            added++;
        }

        log.info("[SelfModelInit] 完成: 新增 {} 个，跳过 {} 个自研模型 (ONNX 可用: {}, 配置定义: {})",
                added, skipped, availableOnnxFiles.size(), props.getModels().size());
    }

    /**
     * 扫描目录下的所有 .onnx 文件，返回去掉后缀的模型代码集合。
     */
    private Set<String> scanOnnxFiles(String baseDir) {
        if (baseDir == null || baseDir.isBlank()) {
            return Collections.emptySet();
        }
        File dir = new File(baseDir);
        if (!dir.exists() || !dir.isDirectory()) {
            return Collections.emptySet();
        }
        File[] files = dir.listFiles((d, name) -> name.endsWith(".onnx"));
        if (files == null) return Collections.emptySet();
        return Arrays.stream(files)
                .map(f -> f.getName().replaceFirst("\\.onnx$", ""))
                .collect(Collectors.toSet());
    }

    /**
     * 交叉验证：配置定义 × ONNX 文件存在 (V7.3 自动扫描增强)
     * - 若配置有定义 AND .onnx 文件存在 → 使用配置元信息注册
     * - 若配置有定义 AND 无 .onnx 文件 → 记录但跳过（等模型文件就位）
     * - 若 ONNX 文件存在但配置未定义 → 自动生成 ModelDef（零配置自动扫描）
     */
    private List<SelfModelsProperties.ModelDef> resolveValidModels(Set<String> availableOnnx) {
        List<SelfModelsProperties.ModelDef> valid = new ArrayList<>();
        Set<String> configuredCodes = new HashSet<>();

        // 1. 处理配置中定义的模型
        for (SelfModelsProperties.ModelDef def : props.getModels()) {
            if (def.getCode() == null || def.getCode().isBlank()) {
                log.warn("[SelfModelInit] 配置中存在空的 model code，跳过");
                continue;
            }
            configuredCodes.add(def.getCode());
            if (availableOnnx.contains(def.getCode())) {
                valid.add(def);
            } else {
                log.info("[SelfModelInit] ⏳ 模型配置存在但 .onnx 文件未找到: {} (期望 {}/{}.onnx)\n" +
                         "  → 请将模型文件放入 {} 后重启服务",
                        def.getCode(), props.getBaseDir(), def.getCode(), props.getBaseDir());
            }
        }

        // 2. 自动发现：扫描到但配置未定义的 ONNX 文件 → 自动注册
        for (String onnxCode : availableOnnx) {
            if (!configuredCodes.contains(onnxCode)) {
                SelfModelsProperties.ModelDef autoDef = new SelfModelsProperties.ModelDef();
                autoDef.setCode(onnxCode);
                autoDef.setDisplayName(onnxCode);
                autoDef.setDescription("自动发现的 ONNX 模型");
                autoDef.setSupportsVision(false);
                autoDef.setSupportsStream(true);
                autoDef.setMaxContext(4096);
                autoDef.setMaxOutput(1024);
                valid.add(autoDef);
                log.info("[SelfModelInit] 🔍 自动发现 ONNX 模型: {} (未在配置中定义，使用默认参数)", onnxCode);
            }
        }

        return valid;
    }

    /** 确保 provider 存在，不存在则创建 */
    private ModelProvider ensureProvider() {
        var existing = providerMapper.selectOne(
                new LambdaQueryWrapper<ModelProvider>()
                        .eq(ModelProvider::getCode, props.getProvider().getCode()));
        if (existing != null) {
            log.info("[SelfModelInit] 自研 provider 已存在: id={}", existing.getId());
            return existing;
        }

        ModelProvider p = new ModelProvider();
        p.setCode(props.getProvider().getCode());
        p.setName(props.getProvider().getName());
        p.setDescription(props.getProvider().getDescription());
        p.setBaseUrl(props.getBaseDir());
        p.setApiKey("");
        p.setProtocol("local");
        p.setEnabled(1);
        p.setSort(0);
        providerMapper.insert(p);
        log.info("[SelfModelInit] 创建自研 provider: id={}, code={}", p.getId(), p.getCode());
        return p;
    }
}
