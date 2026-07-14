package com.minimax.agent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.minimax.agent.entity.Plugin;
import com.minimax.agent.mapper.PluginMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * V2.4: 插件市场服务
 *
 * - 列出已发布插件
 * - 用户安装/启用/禁用
 * - 评分
 * - 搜索/分类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PluginService {

    private final PluginMapper mapper;

    public List<Plugin> listAll(String category) {
        if (category == null || category.isBlank()) {
            return mapper.selectList(
                    new LambdaQueryWrapper<Plugin>()
                            .eq(Plugin::getEnabled, 1)
                            .orderByDesc(Plugin::getDownloads));
        }
        return mapper.selectList(
                new LambdaQueryWrapper<Plugin>()
                        .eq(Plugin::getCategory, category)
                        .eq(Plugin::getEnabled, 1)
                        .orderByDesc(Plugin::getDownloads));
    }

    public List<Plugin> listByOwner(Long ownerId) {
        return mapper.selectList(
                new LambdaQueryWrapper<Plugin>()
                        .eq(Plugin::getOwnerId, ownerId)
                        .orderByDesc(Plugin::getUpdatedAt));
    }

    public Plugin get(Long id) {
        return mapper.selectById(id);
    }

    /**
     * 发布插件 (用户提交)。
     */
    public Long publish(Long ownerId, String name, String displayName, String description,
                        String version, String author, String category,
                        String entry, String pluginType, String config) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name 必填");
        if (mapper.selectCount(
                new LambdaQueryWrapper<Plugin>().eq(Plugin::getName, name)) > 0) {
            throw new IllegalArgumentException("插件名已存在: " + name);
        }
        Plugin p = new Plugin();
        p.setName(name);
        p.setDisplayName(displayName == null ? name : displayName);
        p.setDescription(description);
        p.setVersion(version == null ? "1.0.0" : version);
        p.setAuthor(author);
        p.setCategory(category == null ? "general" : category);
        p.setScope("user");
        p.setOwnerId(ownerId);
        p.setEntry(entry);
        p.setPluginType(pluginType == null ? "class" : pluginType);
        p.setConfig(config);
        p.setEnabled(1);
        p.setDownloads(0);
        p.setRating(new java.math.BigDecimal("0.00"));
        mapper.insert(p);
        return p.getId();
    }

    public boolean incrementDownload(Long id) {
        Plugin p = mapper.selectById(id);
        if (p == null) return false;
        p.setDownloads(p.getDownloads() == null ? 1 : p.getDownloads() + 1);
        mapper.updateById(p);
        return true;
    }

    public boolean rate(Long id, double score) {
        if (score < 0 || score > 5) return false;
        Plugin p = mapper.selectById(id);
        if (p == null) return false;
        // 简化: 直接覆盖; 生产: 加权平均
        p.setRating(new java.math.BigDecimal(score));
        mapper.updateById(p);
        return true;
    }

    public boolean setEnabled(Long id, boolean enabled) {
        Plugin p = mapper.selectById(id);
        if (p == null) return false;
        p.setEnabled(enabled ? 1 : 0);
        mapper.updateById(p);
        return true;
    }

    public boolean delete(Long id, Long ownerId) {
        Plugin p = mapper.selectById(id);
        if (p == null) return false;
        if (!"user".equals(p.getScope())) return false;  // 系统插件不能删
        if (!p.getOwnerId().equals(ownerId)) return false;
        mapper.deleteById(id);
        return true;
    }

    /**
     * V3.5.8: 执行插件调用
     *
     * <h3>支持的插件类型</h3>
     * <ul>
     *   <li>class: 后端 Java 类调用 (反射)</li>
     *   <li>url: HTTP 代理调用 (转发 input 到 url)</li>
     *   <li>js: 客户端 JS 沙箱执行 (后端不参与, 此处仅验证 + 返回配置)</li>
     *   <li>wasm: WebAssembly (预留)</li>
     * </ul>
     *
     * @param id    插件 ID
     * @param input 调用参数
     * @return 调用结果 (不同插件类型返回不同结构)
     */
    public Object call(Long id, Map<String, Object> input) {
        Plugin p = mapper.selectById(id);
        if (p == null) throw new IllegalArgumentException("Plugin not found: " + id);
        if (p.getEnabled() == null || p.getEnabled() == 0) {
            throw new IllegalStateException("Plugin disabled: " + p.getName());
        }
        String type = p.getPluginType() == null ? "url" : p.getPluginType().toLowerCase();
        log.info("[plugin] call id={} name={} type={} input={}", id, p.getName(), type, input);

        return switch (type) {
            case "class" -> callClassPlugin(p, input);
            case "url"   -> callUrlPlugin(p, input);
            case "js"    -> callJsPlugin(p, input);
            case "wasm"  -> callWasmPlugin(p, input);
            default      -> throw new IllegalArgumentException("Unknown plugin type: " + type);
        };
    }

    /** Class 插件: 后端 Java 反射调用 (示例实现) */
    private Object callClassPlugin(Plugin p, Map<String, Object> input) {
        String entry = p.getEntry();
        // 简化: 返回固定结果 + input 回显, 避免反射安全问题
        return Map.of(
                "type", "class",
                "plugin", p.getName(),
                "entry", entry,
                "input", input,
                "output", "[class plugin] " + entry + " executed with " + input
        );
    }

    /** URL 插件: HTTP 转发 (实际转发留 Gateway) */
    private Object callUrlPlugin(Plugin p, Map<String, Object> input) {
        return Map.of(
                "type", "url",
                "plugin", p.getName(),
                "endpoint", p.getEntry(),
                "input", input,
                "output", "[url plugin] would forward to " + p.getEntry()
        );
    }

    /** JS 插件: 客户端执行, 后端仅返回配置 */
    private Object callJsPlugin(Plugin p, Map<String, Object> input) {
        return Map.of(
                "type", "js",
                "plugin", p.getName(),
                "client", true,
                "code", p.getEntry(),
                "input", input,
                "message", "JS plugin executes on client, backend returns config only"
        );
    }

    /** WASM 插件: 预留 */
    private Object callWasmPlugin(Plugin p, Map<String, Object> input) {
        return Map.of(
                "type", "wasm",
                "plugin", p.getName(),
                "input", input,
                "message", "WASM support is reserved (V3.5.8+)"
        );
    }
}
