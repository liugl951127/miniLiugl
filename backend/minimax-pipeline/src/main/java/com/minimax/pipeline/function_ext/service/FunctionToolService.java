package com.minimax.pipeline.function_ext.service;

import com.minimax.pipeline.function_ext.entity.FunctionTool;
import com.minimax.pipeline.function_ext.mapper.FunctionToolMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FunctionToolService {

    private final FunctionToolMapper mapper;

    public List<FunctionTool> listAll() {
        return mapper.selectEnabled();
    }

    public List<FunctionTool> listByCategory(String category) {
        if (category == null || category.isBlank()) return listAll();
        return mapper.selectByCategory(category);
    }

    public FunctionTool get(Long id) {
        return mapper.selectById(id);
    }

    public FunctionTool getByName(String name) {
        return mapper.selectByName(name);
    }

    /**
     * 用户注册自定义工具。
     */
    public Long createUserTool(Long ownerId, String name, String displayName, String description,
                                String parameters, String endpoint, String httpMethod, String tags) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name 必填");
        if (mapper.selectByName(name) != null) {
            throw new IllegalArgumentException("工具名已存在: " + name);
        }
        FunctionTool t = new FunctionTool();
        t.setName(name);
        t.setDisplayName(displayName != null ? displayName : name);
        t.setDescription(description);
        t.setCategory("custom");
        t.setScope("user");
        t.setOwnerId(ownerId);
        t.setParameters(parameters);
        t.setEndpoint(endpoint);
        t.setHttpMethod(httpMethod != null ? httpMethod : "POST");
        t.setEnabled(1);
        t.setTags(tags);
        mapper.insert(t);
        return t.getId();
    }

    public boolean update(Long id, Long ownerId, String displayName, String description,
                           String parameters, String endpoint, Integer enabled) {
        FunctionTool t = mapper.selectById(id);
        if (t == null) return false;
        if (!"builtin".equals(t.getScope()) && !t.getOwnerId().equals(ownerId)) return false;
        if (displayName != null) t.setDisplayName(displayName);
        if (description != null) t.setDescription(description);
        if (parameters != null) t.setParameters(parameters);
        if (endpoint != null) t.setEndpoint(endpoint);
        if (enabled != null) t.setEnabled(enabled);
        mapper.updateById(t);
        return true;
    }

    public boolean delete(Long id, Long ownerId) {
        FunctionTool t = mapper.selectById(id);
        if (t == null) return false;
        if (!"builtin".equals(t.getScope()) && !t.getOwnerId().equals(ownerId)) return false;
        mapper.deleteById(id);
        return true;
    }
}
