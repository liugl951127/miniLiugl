package com.minimax.model.service;

import com.minimax.model.dto.ChatRequest;
import com.minimax.model.vo.ChatResponse;
import com.minimax.model.vo.ModelVO;
import reactor.core.publisher.Flux;

import java.util.List;

public interface ModelService {

    List<ModelVO> listEnabled();

    ChatResponse chat(Long userId, ChatRequest req);

    Flux<String> stream(Long userId, ChatRequest req);
}
