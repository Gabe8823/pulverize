package com.run;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.run.module.*.mapper")
public class RunAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(RunAiApplication.class, args);
    }
}
