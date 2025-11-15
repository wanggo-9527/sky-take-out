package com.sky;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.util.DigestUtils;

@SpringBootApplication
@EnableConfigurationProperties
@EnableTransactionManagement //开启注解方式的事务管理
@Slf4j
public class SkyApplication {
    public static void main(String[] args) {
        //System.out.println(DigestUtils.md5DigestAsHex("123456".getBytes()));
        SpringApplication.run(SkyApplication.class, args);
        log.info("server started");
    }
}
