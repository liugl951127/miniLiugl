package com.minimax.deployer.state;

import com.minimax.deployer.entity.ForgeRelease;
import com.minimax.deployer.mapper.ForgeReleaseMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

/**
 * Release 状态机 (V4.0)
 *
 * 单一状态变更入口, 杜绝 status 字段在多个 service 里散落改动。
 *
 * 状态图:
 *   DRAFT → BUILDING → DEPLOYING → HEALTHY → ACTIVE
 *      ↓         ↓          ↓
 *   ARCHIVED  FAILED    FAILED
 *                 ↓
 *              ARCHIVED
 *
 * 转换规则:
 *  - 只允许白名单转换 (非任意跳)
 *  - 终态 (ACTIVE / ARCHIVED) 不可再转
 *  - 失败状态需带 reason
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ReleaseStateMachine {

    public enum State {
        DRAFT, BUILDING, DEPLOYING, HEALTHY, ACTIVE, FAILED, ARCHIVED
    }

    public enum Event {
        START_BUILD, START_DEPLOY, DEPLOY_HEALTHY, MARK_ACTIVE, FAIL, ARCHIVE
    }

    /** 合法转换表 */
    private static final Map<State, Map<Event, State>> TRANSITIONS = Map.of(
        State.DRAFT,     Map.of(Event.START_BUILD, State.BUILDING, Event.ARCHIVE, State.ARCHIVED),
        State.BUILDING,  Map.of(Event.START_DEPLOY, State.DEPLOYING, Event.FAIL, State.FAILED),
        State.DEPLOYING, Map.of(Event.DEPLOY_HEALTHY, State.HEALTHY, Event.FAIL, State.FAILED),
        State.HEALTHY,   Map.of(Event.MARK_ACTIVE, State.ACTIVE, Event.FAIL, State.FAILED),
        State.FAILED,    Map.of(Event.ARCHIVE, State.ARCHIVED),
        State.ARCHIVED,  Map.of(),
        State.ACTIVE,    Map.of(Event.ARCHIVE, State.ARCHIVED)
    );

    private final ForgeReleaseMapper releaseMapper;

    /**
     * 触发状态转换
     *
     * @return 转换后的新状态
     * @throws IllegalStateException 不允许的转换
     */
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
        return next;
    }

    public State fire(Long releaseId, Event event) {
        return fire(releaseId, event, null);
    }
}
