package com.example.githubrepository.controller;

import com.example.githubrepository.model.RepositoryInfo;
import com.example.githubrepository.service.GitHubRepositoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.Collections;

import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureWebTestClient
public class GitHubRepositoryControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private GitHubRepositoryService gitHubRepositoryService;

    @Test
    void testListUserRepositories() {
        String username = "testUsername";
        RepositoryInfo repository = new RepositoryInfo(
                "testRepository",
                null,
                Collections.emptyList(),
                false);

        when(gitHubRepositoryService.getUserRepositories(username))
                .thenReturn(Mono.just(Collections.singletonList(repository)));

        webTestClient.get().uri("/api/github/users/{username}/repositories", username)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(RepositoryInfo.class)
                .hasSize(1)
                .contains(repository);
    }
}
