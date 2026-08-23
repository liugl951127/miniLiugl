package com.minimax.deployer.state;

import com.minimax.deployer.entity.ForgeRelease;
import com.minimax.deployer.mapper.ForgeReleaseMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Release 状态机 (V4.1)
 *
 * V4.1 命名统一 (全部用动词):
 *  - BUILD     (DRAFT     → BUILDING)   构建 manifest
 *  - DEPLOY    (BUILDING  → DEPLOYING)  push 镜像 / 推送 Git
 *  - READY     (DEPLOYING → HEALTHY)    探针通过
 *  - ACTIVATE  (HEALTHY   → ACTIVE)     流量接入
 *  - FAIL      (*         → FAILED)     失败 (带 reason)
 *  - ARCHIVE   (*         → ARCHIVED)   归档 (终态)
 *
 * 任何非法转换抛 IllegalStateException。
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ReleaseStateMachine {

    public enum State {
        DRAFT, BUILDING, DEPLOYING, HEALTHY, ACTIVE, FAILED, ARCHIVED
    }

    public enum Event {
        BUILD, DEPLOY, READY, ACTIVATE, FAIL, ARCHIVE
    }

    private static final Map<State, Map<Event, State>> TRANSITIONS = Map.of(
        State.DRAFT,     Map.of(Event.BUILD, State.BUILDING,  Event.ARCHIVE, State.ARCHIVED),
        State.BUILDING,  Map.of(Event.DEPLOY, State.DEPLOYING, Event.FAIL, State.FAILED),
        State.DEPLOYING, Map.of(Event.READY, State.HEALTHY,    Event.FAIL, State.FAILED),
        State.HEALTHY,   Map.of(Event.ACTIVATE, State.ACTIVE,  Event.FAIL, State.FAILED),
        State.FAILED,    Map.of(Event.ARCHIVE, State.ARCHIVED),
        State.ARCHIVED,  Map.of(),
        State.ACTIVE,    Map.of(Event.ARCHIVE, State.ARCHIVED)
    );

    private final ForgeReleaseMapper releaseMapper;

    public State fire(Long releaseId, Event event, String reason) {
        ForgeRelease release = releaseMapper.selectById(releaseId);
        if (release == null) throw new IllegalStateException("Release 不存在: " + releaseId);

        State current = State.valueOf(release.getStatus());
        State next = TRANSITIONS.getOrDefault(current, Map.of()).get(event);
        if (next == null) {
            throw new IllegalStateException(
                "非法状态转换: " + current + " --" + event + "--> ? (release=" + releaseId + ")"
            );
        }
        log.info("[StateMachine] release={} {} --{}--> {} ({})",
            releaseId, current, event, next, reason != null ? reason : "");

        releaseMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<ForgeRelease>()
            .eq("id", releaseId)
            .set("status", next.name())
            .set("updated_at", LocalDateTime.now()));
        if (reason != null && (event == Event.FAIL)) {
            releaseMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<ForgeRelease>()
                .eq("id", releaseId)
                .set("failure_reason", reason));
        }
        return next;
    }

    public State fire(Long releaseId, Event event) {
        return fire(releaseId, event, null);
    }
}
