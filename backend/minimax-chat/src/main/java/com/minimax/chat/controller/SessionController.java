package com.minimax.chat.controller;

import com.minimax.common.security.JwtAuthenticationFilter.AuthenticatedUser;
import com.minimax.chat.dto.CreateSessionRequest;
import com.minimax.chat.dto.UpdateSessionRequest;
import com.minimax.chat.service.ChatSessionService;
import com.minimax.chat.vo.SessionVO;
import com.minimax.common.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final ChatSessionService sessionService;

    @GetMapping
    public Result<List<SessionVO>> list(@AuthenticationPrincipal AuthenticatedUser principal,
                                        @RequestParam(required = false) Integer status) {
        return Result.ok(sessionService.listByUser(principal.id(), status));
    }

    @PostMapping
    public Result<SessionVO> create(@AuthenticationPrincipal AuthenticatedUser principal,
                                    @Valid @RequestBody CreateSessionRequest req) {
        return Result.ok(sessionService.create(principal.id(), req));
    }

    @GetMapping("/{id}")
    public Result<SessionVO> detail(@AuthenticationPrincipal AuthenticatedUser principal,
                                    @PathVariable Long id) {
        return Result.ok(sessionService.detail(id, principal.id()));
    }

    @PutMapping("/{id}")
    public Result<SessionVO> update(@AuthenticationPrincipal AuthenticatedUser principal,
                                    @PathVariable Long id,
                                    @Valid @RequestBody UpdateSessionRequest req) {
        return Result.ok(sessionService.update(id, principal.id(), req));
    }

    @DeleteMapping("/{id}")
    public Result<Void> archive(@AuthenticationPrincipal AuthenticatedUser principal,
                                @PathVariable Long id) {
        sessionService.archive(id, principal.id());
        return Result.ok();
    }
}
