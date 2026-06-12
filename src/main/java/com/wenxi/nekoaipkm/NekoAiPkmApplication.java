package com.wenxi.nekoaipkm;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.wenxi.nekoaipkm.mapper")
public class NekoAiPkmApplication {

    public static void main(String[] args) {
        SpringApplication.run(NekoAiPkmApplication.class, args);
    }

}
