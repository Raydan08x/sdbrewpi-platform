package com.sierradorada.sdbrewpi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@ConfigurationPropertiesScan
public class SdbrewpiApplication {
    public static void main(String[] args) {
        SpringApplication.run(SdbrewpiApplication.class, args);
    }
}
