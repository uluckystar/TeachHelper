package com.teachhelper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TeachHelperApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(TeachHelperApplication.class, args);
    }
}
