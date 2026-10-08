package dev.stefanjakic.lifecycle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class LifecycleApplication {

    public static void main(String[] args) {
        SpringApplication.run(LifecycleApplication.class, args);
    }
}
