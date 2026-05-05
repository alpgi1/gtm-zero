package com.gtmzero;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class GtmZeroApplication {

    public static void main(String[] args) {
        SpringApplication.run(GtmZeroApplication.class, args);
    }
}
