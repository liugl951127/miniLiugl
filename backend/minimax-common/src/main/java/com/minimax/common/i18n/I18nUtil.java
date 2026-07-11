package com.minimax.common.i18n;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * 国际化工具 (V2.7.8)
 *
 * <p>支持中英双语, 优先从请求头 Accept-Language 读取, 默认 zh-CN.</p>
 *
 * <p>用法: {@code I18nUtil.t("user.welcome")}</p>
 */
@Component
public class I18nUtil {

    private static MessageSource messageSource;

    @Autowired
    public void setMessageSource(MessageSource ms) {
        I18nUtil.messageSource = ms;
    }

    /**
     * 获取当前 Locale
     */
    public static Locale currentLocale() {
        return LocaleContextHolder.getLocale();
    }

    /**
     * 当前是否中文
     */
    public static boolean isChinese() {
        Locale l = currentLocale();
        return l == null || l.getLanguage().equals("zh");
    }

    /**
     * 翻译 (带默认)
     */
    public static String t(String key, String defaultMsg) {
        if (messageSource == null) return defaultMsg;
        try {
            return messageSource.getMessage(key, null, defaultMsg, currentLocale());
        } catch (Exception e) {
            return defaultMsg;
        }
    }

    /**
     * 翻译 (key 即默认)
     */
    public static String t(String key) {
        return t(key, key);
    }

    /**
     * 翻译 (带参数)
     */
    public static String t(String key, Object... args) {
        if (messageSource == null) return key;
        try {
            return messageSource.getMessage(key, args, currentLocale());
        } catch (Exception e) {
            return key;
        }
    }
}
