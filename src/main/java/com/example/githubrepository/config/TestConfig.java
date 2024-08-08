package com.example.githubrepository.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

@TestConfiguration
public class TestConfig {

    @Bean
    public WebClient webClient() {
        return WebClient.create("http://localhost:8080"); // WireMock server URL
    }
}
