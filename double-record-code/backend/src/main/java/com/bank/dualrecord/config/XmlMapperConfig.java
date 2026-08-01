package com.bank.dualrecord.config;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * XML Mapper 配置(银保监报送用)
 */
@Configuration
public class XmlMapperConfig {

    @Bean
    @Primary
    public XmlMapper xmlMapper() {
        XmlMapper mapper = new XmlMapper();
        mapper.registerModule(new JaxbAnnotationModule());
        return mapper;
    }
}
