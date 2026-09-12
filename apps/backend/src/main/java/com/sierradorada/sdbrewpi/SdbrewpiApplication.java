package com.sierradorada.sdbrewpi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class SdbrewpiApplication {
    public static void main(String[] args) {
        SpringApplication.run(SdbrewpiApplication.class, args);
    }
}

