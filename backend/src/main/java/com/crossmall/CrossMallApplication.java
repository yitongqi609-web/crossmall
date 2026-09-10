package com.crossmall;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * CrossMall · 潮汐全球购 —— 跨境电商独立站
 */
@EnableScheduling
@MapperScan("com.crossmall.mapper")
@SpringBootApplication
public class CrossMallApplication {

    public static void main(String[] args) {
        SpringApplication.run(CrossMallApplication.class, args);
        System.out.println("""
                ====================================================
                  CrossMall · 潮汐全球购 started
                  接口文档: http://localhost:8582/doc.html
                ====================================================
                """);
    }
}
