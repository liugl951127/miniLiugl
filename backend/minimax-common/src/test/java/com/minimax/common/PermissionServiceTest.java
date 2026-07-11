package com.minimax.common;

import com.minimax.common.security.PermissionService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PermissionServiceTest {

    @Test
    void testSuperAdmin() {
        assertTrue(PermissionService.has("SUPER_ADMIN", "anything"));
        assertTrue(PermissionService.has("SUPER_ADMIN", "ai.admin"));
    }

    @Test
    void testAdmin() {
        assertTrue(PermissionService.has("ADMIN", "ai.use"));
        assertTrue(PermissionService.has("ADMIN", "ai.admin"));
        assertTrue(PermissionService.has("ADMIN", "alert.create"));
    }

    @Test
    void testUser() {
        assertTrue(PermissionService.has("USER", "ai.use"));
        assertTrue(PermissionService.has("USER", "ai.chat"));
        assertFalse(PermissionService.has("USER", "ai.admin"));
        assertFalse(PermissionService.has("USER", "user.manage"));
    }

    @Test
    void testGuest() {
        assertTrue(PermissionService.has("GUEST", "ai.read"));
        assertFalse(PermissionService.has("GUEST", "ai.use"));
    }

    @Test
    void testWildcardMatch() {
        assertTrue(PermissionService.has("ADMIN", "alert.read"));
        assertTrue(PermissionService.has("ADMIN", "alert.write"));
    }

    @Test
    void testHasAny() {
        assertTrue(PermissionService.hasAny("USER", "ai.admin", "ai.chat"));
        assertFalse(PermissionService.hasAny("USER", "ai.admin", "system.config"));
    }

    @Test
    void testHasAll() {
        assertTrue(PermissionService.hasAll("USER", "ai.use", "ai.chat"));
        assertFalse(PermissionService.hasAll("USER", "ai.use", "ai.admin"));
    }

    @Test
    void testNullRole() {
        assertFalse(PermissionService.has(null, "any"));
    }

    @Test
    void testUnknownRole() {
        assertFalse(PermissionService.has("NOBODY", "any"));
    }

    @Test
    void testListRoles() {
        assertTrue(PermissionService.listRoles().contains("ADMIN"));
        assertTrue(PermissionService.listRoles().contains("USER"));
        assertTrue(PermissionService.listRoles().size() >= 4);
    }

    @Test
    void testPermissionsOf() {
        assertTrue(PermissionService.permissionsOf("ADMIN").contains("ai.use"));
        assertTrue(PermissionService.permissionsOf("SUPER_ADMIN").contains("*"));
    }
}
