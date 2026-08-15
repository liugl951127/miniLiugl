package com.minimax.common.i18n;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Locale 配置 (V2.7.8)
 *
 * <p>1. MessageSource 从 i18n/messages 加载 .properties</p>
 * <p>2. AcceptHeaderLocaleResolver 从 Accept-Language 头读取</p>
 * <p>3. 默认 Locale zh_CN</p>
 */
@Configuration
public class LocaleConfig {

    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver r = new AcceptHeaderLocaleResolver();
        r.setDefaultLocale(Locale.SIMPLIFIED_CHINESE);
        r.setSupportedLocales(java.util.List.of(
                Locale.SIMPLIFIED_CHINESE,
                Locale.US,
                Locale.ENGLISH
        ));
        return r;
    }

    @Bean
    public ResourceBundleMessageSource messageSource() {
        ResourceBundleMessageSource m = new ResourceBundleMessageSource();
        m.setBasename("i18n/messages");
        m.setDefaultEncoding(StandardCharsets.UTF_8.name());
        m.setFallbackToSystemLocale(false);
        m.setUseCodeAsDefaultMessage(true);
        return m;
    }
}
