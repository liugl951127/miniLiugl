package com.bank.dualrecord.util;

import com.bank.dualrecord.model.OrderState;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StateMachineTest {

    @Test
    void testNormalForwardTransition() {
        assertTrue(StateMachine.isValidTransition(OrderState.VERIFIED, OrderState.SCRIPTING));
        assertTrue(StateMachine.isValidTransition(OrderState.SCRIPTING, OrderState.RECORDING));
        assertTrue(StateMachine.isValidTransition(OrderState.RECORDING, OrderState.SIGNING));
        assertTrue(StateMachine.isValidTransition(OrderState.SIGNING, OrderState.QA_PASSED));
        assertTrue(StateMachine.isValidTransition(OrderState.QA_PASSED, OrderState.COMPLETED));
    }

    @Test
    void testTerminalCannotTransition() {
        assertFalse(StateMachine.isValidTransition(OrderState.COMPLETED, OrderState.VERIFIED));
        assertFalse(StateMachine.isValidTransition(OrderState.COMPLETED, OrderState.CANCELLED));
    }

    @Test
    void testCannotSkip() {
        assertFalse(StateMachine.isValidTransition(OrderState.RESERVED, OrderState.COMPLETED));
        assertFalse(StateMachine.isValidTransition(OrderState.VERIFIED, OrderState.RECORDING));
        assertFalse(StateMachine.isValidTransition(OrderState.RECORDING, OrderState.COMPLETED));
    }

    @Test
    void testRollbackAllowed() {
        // 录制中可回到 SCRIPTING(重录)
        assertTrue(StateMachine.isValidTransition(OrderState.RECORDING, OrderState.SCRIPTING));
        // 签约中可回到 RECORDING
        assertTrue(StateMachine.isValidTransition(OrderState.SIGNING, OrderState.RECORDING));
        // 质检后可复检
        assertTrue(StateMachine.isValidTransition(OrderState.QA_PASSED, OrderState.SIGNING));
    }

    @Test
    void testCancelFromAnyNonTerminal() {
        assertTrue(StateMachine.isValidTransition(OrderState.RESERVED, OrderState.CANCELLED));
        assertTrue(StateMachine.isValidTransition(OrderState.RECORDING, OrderState.CANCELLED));
        assertTrue(StateMachine.isValidTransition(OrderState.QA_PASSED, OrderState.CANCELLED));
    }

    @Test
    void testGetNextStates() {
        assertTrue(StateMachine.getNextStates(OrderState.RECORDING).contains(OrderState.SIGNING));
        assertTrue(StateMachine.getNextStates(OrderState.RECORDING).contains(OrderState.SCRIPTING));
        assertTrue(StateMachine.getNextStates(OrderState.COMPLETED).isEmpty());
    }
}
