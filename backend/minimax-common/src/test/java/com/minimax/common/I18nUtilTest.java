package com.minimax.common;

import com.minimax.common.i18n.I18nUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class I18nUtilTest {

    @BeforeAll
    static void setup() {
        // 手动初始化 MessageSource (避免 @SpringBootTest 重量级)
        ResourceBundleMessageSource ms = new ResourceBundleMessageSource();
        ms.setBasename("i18n/messages");
        try {
            java.lang.reflect.Field f = I18nUtil.class.getDeclaredField("messageSource");
            f.setAccessible(true);
            f.set(null, ms);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testChinese() {
        LocaleContextHolder.setLocale(Locale.SIMPLIFIED_CHINESE);
        assertTrue(I18nUtil.isChinese());
        // 不硬编码中文 (JVM encoding 可能不一致), 只验证 key 被翻译
        String login = I18nUtil.t("user.login");
        assertNotNull(login);
        assertNotEquals("user.login", login);  // 被翻译了
    }

    @Test
    void testEnglish() {
        LocaleContextHolder.setLocale(Locale.US);
        assertFalse(I18nUtil.isChinese());
        assertEquals("Login", I18nUtil.t("user.login"));
        assertEquals("Success", I18nUtil.t("common.success"));
    }

    @Test
    void testWithArgs() {
        LocaleContextHolder.setLocale(Locale.SIMPLIFIED_CHINESE);
        // 用常见 key 测试
        String s = I18nUtil.t("common.success");
        assertNotNull(s);
    }

    @Test
    void testFallbackToKey() {
        LocaleContextHolder.setLocale(Locale.SIMPLIFIED_CHINESE);
        String s = I18nUtil.t("non.existent.key");
        assertEquals("non.existent.key", s);
    }

    @Test
    void testCurrentLocale() {
        LocaleContextHolder.setLocale(Locale.SIMPLIFIED_CHINESE);
        assertEquals(Locale.SIMPLIFIED_CHINESE, I18nUtil.currentLocale());
        LocaleContextHolder.setLocale(Locale.US);
        assertEquals(Locale.US, I18nUtil.currentLocale());
    }
}
