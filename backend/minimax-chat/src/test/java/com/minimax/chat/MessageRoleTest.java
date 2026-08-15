package com.minimax.chat;

import com.minimax.chat.enums.MessageRole;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MessageRoleTest {

    @Test
    void ofValidRoles() {
        assertEquals(MessageRole.USER, MessageRole.of("user"));
        assertEquals(MessageRole.ASSISTANT, MessageRole.of("assistant"));
        assertEquals(MessageRole.SYSTEM, MessageRole.of("system"));
        assertEquals(MessageRole.TOOL, MessageRole.of("tool"));
    }

    @Test
    void ofCaseInsensitive() {
        assertEquals(MessageRole.USER, MessageRole.of("USER"));
        assertEquals(MessageRole.ASSISTANT, MessageRole.of("Assistant"));
    }

    @Test
    void ofUnknownDefaultsToUser() {
        assertEquals(MessageRole.USER, MessageRole.of("gibberish"));
        assertEquals(MessageRole.USER, MessageRole.of(null));
    }
}
