package com.bank.dualrecord;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 双录一体化平台 - 业务后端启动类
 *
 * <p>技术栈:
 * <ul>
 *   <li>Spring Boot 2.7 + Spring Security + Spring AOP
 *   <li>MyBatis Plus + MySQL 8 + HikariCP
 *   <li>Hyperledger Fabric Gateway 2.2
 *   <li>国密 SM2/SM3/SM4 (BouncyCastle)
 *   <li>JWT + Springdoc OpenAPI
 * </ul>
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableTransactionManagement
@MapperScan("com.bank.dualrecord.mapper")
public class DualRecordApplication {

    public static void main(String[] args) {
        SpringApplication.run(DualRecordApplication.class, args);
    }
}
