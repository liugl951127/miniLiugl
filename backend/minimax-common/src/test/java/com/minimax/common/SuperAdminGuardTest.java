package com.minimax.common;

import com.minimax.common.security.SuperAdminGuard;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SuperAdminGuardTest {

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void noAuthMeansNotSuper() {
        assertFalse(SuperAdminGuard.isSuperAdmin());
    }

    @Test
    void superAdminRecognized() {
        var auth = new UsernamePasswordAuthenticationToken(
                "u", null,
                List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(auth);
        assertTrue(SuperAdminGuard.isSuperAdmin());
        assertDoesNotThrow(SuperAdminGuard::requireSuperAdmin);
    }

    @Test
    void adminNotSuper() {
        var auth = new UsernamePasswordAuthenticationToken(
                "u", null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(auth);
        assertFalse(SuperAdminGuard.isSuperAdmin());
        assertTrue(SuperAdminGuard.isAdminOrAbove());
    }

    @Test
    void requireSuperAdminThrowsForNormalAdmin() {
        var auth = new UsernamePasswordAuthenticationToken(
                "u", null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(auth);
        assertThrows(Exception.class, SuperAdminGuard::requireSuperAdmin);
    }

    @Test
    void userRoleIsNothing() {
        var auth = new UsernamePasswordAuthenticationToken(
                "u", null,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);
        assertFalse(SuperAdminGuard.isSuperAdmin());
        assertFalse(SuperAdminGuard.isAdminOrAbove());
    }
}
