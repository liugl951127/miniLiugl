package com.minimax.admin;

import com.minimax.admin.client.ServiceClient;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class ServiceClientTest {

    private ServiceClient client() {
        ServiceClient c = new ServiceClient();
        ReflectionTestUtils.setField(c, "timeout", 2);
        ReflectionTestUtils.setField(c, "serviceToken", "");
        return c;
    }

    @Test
    void errorResp() {
        ServiceClient c = client();
        var r = c.errorResp("test");
        assertEquals(1500, r.get("code"));
        assertEquals("test", r.get("message"));
    }

    @Test
    void isReachableLocalUnreachable() {
        ServiceClient c = client();
        // 假设 9999 端口没服务
        assertFalse(c.isReachable("http://localhost:9999"));
    }

    @Test
    void isReachableInvalidUrl() {
        ServiceClient c = client();
        assertFalse(c.isReachable("not-a-url"));
    }
}
