package com.aigrs.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AigrsApplication {

    public static void main(String[] args) {
        SpringApplication.run(AigrsApplication.class, args);
    }
}
