package com.queueless.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class QueueLessAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(QueueLessAiApplication.class, args);
    }
}
