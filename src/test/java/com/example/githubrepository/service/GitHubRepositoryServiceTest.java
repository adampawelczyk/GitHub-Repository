package com.example.githubrepository.service;

import com.example.githubrepository.exception.UserNotFoundException;
import com.example.githubrepository.model.Branch;
import com.example.githubrepository.model.Commit;
import com.example.githubrepository.model.Owner;
import com.example.githubrepository.model.RepositoryInfo;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.MockResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class GitHubRepositoryServiceTest {

    private MockWebServer mockWebServer;
    private GitHubRepositoryService gitHubRepositoryService;

    @BeforeEach
    void setUp() {
        try {
            mockWebServer = new MockWebServer();
            mockWebServer.start();

            WebClient webClient = WebClient.builder()
                    .baseUrl(mockWebServer.url("").toString())
                    .build();

            gitHubRepositoryService = new GitHubRepositoryService(webClient);
        } catch (IOException e) {
            System.err.println("Failed to start MockWebServer: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("An unexpected error occurred during setup: " + e.getMessage());
        }
    }

    @AfterEach
    void tearDown() {
        try {
            mockWebServer.shutdown();
        } catch (IOException e) {
            System.err.println("Failed to shut down MockWebServer: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("An unexpected error occurred during test teardown: " + e.getMessage());
        }
    }

    @Test
    void testGetUserRepositories() {
        String username = "testUsername";
        String repositoriesResponse = "[{\"name\":\"testRepository\",\"owner\":{\"login\":\"testUsername\"},\"fork\":false}]";

        mockWebServer.enqueue(new MockResponse()
                .setBody(repositoriesResponse)
                .addHeader("Content-Type", "application/json"));

        String branchesResponse = "[{\"name\":\"main\",\"commit\":{\"sha\":\"commit-sha\"}}]";
        mockWebServer.enqueue(new MockResponse()
                .setBody(branchesResponse)
                .addHeader("Content-Type", "application/json"));

        Mono<List<RepositoryInfo>> result = gitHubRepositoryService.getUserRepositories(username);

        StepVerifier.create(result)
                .expectNext(Collections.singletonList(
                        new RepositoryInfo("testRepository", new Owner("testUsername"),
                                Collections.singletonList(new Branch("main", new Commit("commit-sha"))),
                                false)))
                .verifyComplete();
    }

    @Test
    void testGetUserRepositoriesUserNotFound() {
        String username = "nonexistent";

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(404)
                .setBody("{\"message\":\"User with 'nonexistent' username wasn't found\"}")
                .addHeader("Content-Type", "application/json"));

        Mono<List<RepositoryInfo>> result = gitHubRepositoryService.getUserRepositories(username);

        StepVerifier.create(result)
                .expectError(UserNotFoundException.class)
                .verify();
    }

    @Test
    void testGetUserRepositoriesOnlyNonForked() {
        String username = "testUsername";
        String repositoriesResponse = "["
                + "{\"name\":\"nonForkedRepository\",\"owner\":{\"login\":\"testUsername\"},\"fork\":false},"
                + "{\"name\":\"forkedRepository\",\"owner\":{\"login\":\"testUsername\"},\"fork\":true}"
                + "]";

        mockWebServer.enqueue(new MockResponse()
                .setBody(repositoriesResponse)
                .addHeader("Content-Type", "application/json"));

        String branchesResponse = "[{\"name\":\"main\",\"commit\":{\"sha\":\"commit-sha\"}}]";
        mockWebServer.enqueue(new MockResponse()
                .setBody(branchesResponse)
                .addHeader("Content-Type", "application/json"));

        Mono<List<RepositoryInfo>> result = gitHubRepositoryService.getUserRepositories(username);

        StepVerifier.create(result)
                .expectNext(Collections.singletonList(
                        new RepositoryInfo("nonForkedRepository", new Owner("testUsername"),
                                Collections.singletonList(new Branch("main", new Commit("commit-sha"))),
                                false)))
                .verifyComplete();
    }
}
