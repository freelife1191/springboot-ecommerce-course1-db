package com.onion.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync // 비동기 처리를 위한 어노테이션
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
