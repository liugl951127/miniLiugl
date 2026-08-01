package com.bank.dualrecord.util;

import com.bank.dualrecord.model.OrderState;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 订单状态机
 *
 * <p>校验状态流转合法性,严防非法跳转
 *
 * @author Mavis
 */
public final class StateMachine {

    private static final Map<OrderState, Set<OrderState>> TRANSITIONS;

    static {
        TRANSITIONS = new EnumMap<>(OrderState.class);
        TRANSITIONS.put(OrderState.RESERVED, EnumSet.of(
            OrderState.VERIFIED, OrderState.SCRIPTING,
            OrderState.CANCELLED, OrderState.FAILED));
        TRANSITIONS.put(OrderState.VERIFIED, EnumSet.of(
            OrderState.SCRIPTING, OrderState.CANCELLED, OrderState.FAILED));
        TRANSITIONS.put(OrderState.SCRIPTING, EnumSet.of(
            OrderState.RECORDING, OrderState.FAILED));
        // 录制中允许回到 SCRIPTING(重录)
        TRANSITIONS.put(OrderState.RECORDING, EnumSet.of(
            OrderState.SIGNING, OrderState.SCRIPTING, OrderState.FAILED));
        // 签约中允许回到 RECORDING
        TRANSITIONS.put(OrderState.SIGNING, EnumSet.of(
            OrderState.QA_PASSED, OrderState.RECORDING, OrderState.FAILED));
        // 质检通过后允许复检
        TRANSITIONS.put(OrderState.QA_PASSED, EnumSet.of(
            OrderState.COMPLETED, OrderState.SIGNING, OrderState.FAILED));
        // 终态
        TRANSITIONS.put(OrderState.COMPLETED, EnumSet.noneOf(OrderState.class));
        // 异常态不能再转
        TRANSITIONS.put(OrderState.CANCELLED, EnumSet.noneOf(OrderState.class));
        TRANSITIONS.put(OrderState.FAILED, EnumSet.noneOf(OrderState.class));
    }

    private StateMachine() {
    }

    /**
     * 校验状态流转是否合法
     */
    public static boolean isValidTransition(OrderState from, OrderState to) {
        if (from == null || to == null) {
            return false;
        }
        // 终态不能流转
        if (from.isTerminal() || from.isAbnormal()) {
            return false;
        }
        // 异常态可作为目标,但不能是 COMPLETED
        if (to == OrderState.COMPLETED && from != OrderState.QA_PASSED) {
            return false;
        }
        Set<OrderState> allowed = TRANSITIONS.get(from);
        return allowed != null && allowed.contains(to);
    }

    /**
     * 获取 from 状态允许的下一状态集合
     */
    public static Set<OrderState> getNextStates(OrderState from) {
        if (from == null) {
            return EnumSet.noneOf(OrderState.class);
        }
        return EnumSet.copyOf(TRANSITIONS.getOrDefault(from, EnumSet.noneOf(OrderState.class)));
    }
}
