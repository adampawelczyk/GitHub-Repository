package com.example.githubrepository.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@Profile("!test")
public class AppConfig {
    @Value("${github.api.base-url}")
    private String githubApiBaseUrl;

    @Bean
    public WebClient webClient() {
        return WebClient.create(githubApiBaseUrl);
    }
}
