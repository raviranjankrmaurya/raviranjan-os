package com.raviranjan.os;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.raviranjan.os.repository")
@EnableCaching
@EnableJpaAuditing
@EnableAsync
@EnableScheduling
public class RaviranjanOsApplication {
    public static void main(String[] args) {
        SpringApplication.run(RaviranjanOsApplication.class, args);
    }
}
