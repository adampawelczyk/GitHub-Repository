package config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.web.reactive.function.client.WebClient;

@TestConfiguration
@Profile("test")
public class TestConfig {

    @Value("${test.api.base-url}")
    private String testApiBaseUrl;

    @Bean
    public WebClient webClient() {
        return WebClient.create(testApiBaseUrl);
    }
}
