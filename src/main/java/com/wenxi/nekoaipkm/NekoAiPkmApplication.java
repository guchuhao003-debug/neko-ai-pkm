package com.wenxi.nekoaipkm;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.wenxi.nekoaipkm.mapper")
@EnableScheduling  // 开启定时任务
public class NekoAiPkmApplication {

    public static void main(String[] args) {
        SpringApplication.run(NekoAiPkmApplication.class, args);
    }

}
