package com.chroniclesoftime;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** 全工程唯一启动类：一次组件扫描装配所有限界上下文。 */
@SpringBootApplication
@MapperScan({
        "com.chroniclesoftime.identity.infrastructure.persistence",
        "com.chroniclesoftime.timeline.infrastructure.persistence",
        "com.chroniclesoftime.learning.infrastructure.persistence",
        "com.chroniclesoftime.planning.infrastructure.persistence",
        "com.chroniclesoftime.archive.infrastructure.persistence",
        "com.chroniclesoftime.community.infrastructure.persistence"
})
public class CotApplication {
    public static void main(String[] args) { SpringApplication.run(CotApplication.class, args); }
}
