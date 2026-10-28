package com.dilema;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DilemaApplication {
    public static void main(String[] args) {
        SpringApplication.run(DilemaApplication.class, args);
    }
}
