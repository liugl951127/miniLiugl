package com.minimax.common.utils;

import jakarta.servlet.http.HttpServletRequest;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * IP 工具
 */
public final class IpUtils {

    private IpUtils() {}

    private static final String UNKNOWN = "unknown";

    public static String getClientIp(HttpServletRequest request) {
        if (request == null) return UNKNOWN;
        String ip = request.getHeader("X-Forwarded-For");
        if (isInvalid(ip)) ip = request.getHeader("X-Real-IP");
        if (isInvalid(ip)) ip = request.getHeader("Proxy-Client-IP");
        if (isInvalid(ip)) ip = request.getHeader("WL-Proxy-Client-IP");
        if (isInvalid(ip)) ip = request.getRemoteAddr();

        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        if ("127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) {
            try {
                ip = InetAddress.getLocalHost().getHostAddress();
            } catch (UnknownHostException ignore) {
                ip = UNKNOWN;
            }
        }
        return ip;
    }

    private static boolean isInvalid(String ip) {
        return ip == null || ip.isEmpty() || UNKNOWN.equalsIgnoreCase(ip);
    }
}
