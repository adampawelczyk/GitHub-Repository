package com.example.githubrepository.service;

import com.example.githubrepository.dto.RepositoryDtoArray;
import config.TestConfig;
import com.example.githubrepository.model.Repository;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@WireMockTest(httpPort = 8080)
@ContextConfiguration(classes = TestConfig.class)
@ActiveProfiles("test")
public class GitHubRepositoryServiceTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void testListUserRepositories() {
        final String username = "testUser";
        final String repositoryName = "testRepository";
        final String branchName = "testBranch";
        final String commitSha = "commit-sha";

        stubFor(get(urlPathEqualTo("/users/" + username + "/repos"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    [
                        {
                            "name": "%s",
                            "owner": {
                                "login": "%s"
                            },
                            "fork": false
                        }
                    ]
                    """.formatted(repositoryName, username))));

        stubFor(get(urlPathEqualTo("/repos/" + username + "/testRepository/branches"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    [
                        {
                            "name": "%s",
                            "commit": {
                                "sha": "%s"
                            }
                        }
                    ]
                    """.formatted(branchName, commitSha))));

        webTestClient.get()
                .uri("/api/github/users/{username}/repositories", username)
                .exchange()
                .expectStatus().isOk()
                .expectBody(RepositoryDtoArray.class)
                .consumeWith(response -> {
                    var repositoryDtoArray = response.getResponseBody();

                    assertThat(repositoryDtoArray).isNotNull();
                    assertThat(repositoryDtoArray.repositories()).hasSize(1);

                    var repository = repositoryDtoArray.repositories().getFirst();

                    assertThat(repository.name()).isEqualTo(repositoryName);
                    assertThat(repository.owner().login()).isEqualTo(username);
                    assertThat(repository.fork()).isFalse();
                    assertThat(repository.branches()).hasSize(1);

                    var branch = repository.branches().getFirst();

                    assertThat(branch.name()).isEqualTo(branchName);
                    assertThat(branch.commit().sha()).isEqualTo(commitSha);
                });

        verify(getRequestedFor(urlPathEqualTo("/users/" + username + "/repos")));
        verify(getRequestedFor(urlPathEqualTo("/repos/" + username + "/" + repositoryName + "/branches")));
    }

    @Test
    void testListUserRepositoriesUserNotFound() {
        final String username = "testUser";
        final String responseBodyMessage = "User not found";

        stubFor(get(urlPathEqualTo("/users/" + username + "/repos"))
            .willReturn(aResponse()
                .withStatus(HttpStatus.NOT_FOUND.value())
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "message": %s
                    }
                    """.formatted(responseBodyMessage))));

        webTestClient.get()
                .uri("/api/github/users/{username}/repositories", username)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.status").isEqualTo(HttpStatus.NOT_FOUND.value())
                .jsonPath("$.message").isEqualTo(responseBodyMessage);

        verify(getRequestedFor(urlPathEqualTo("/users/" + username + "/repos")));
    }

    @Test
    void testListUserRepositoriesOnlyNonForked() {
        final String username = "testUser";
        final String[] repositoryNames = {"nonForked", "forked"};
        final String branchName = "testBranch";
        final String commitSha = "commit-sha";

        String repositoriesResponse = """
            [
                {
                    "name": "%s",
                    "owner": {
                        "login": "%s"
                    },
                    "fork": false
                },
                {
                    "name": "%s",
                    "owner": {
                        "login": "%s"
                    },
                    "fork": true
                }
            ]
            """.formatted(repositoryNames[0], username, repositoryNames[1], username);

        stubFor(get(urlPathEqualTo("/users/" + username + "/repos"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(repositoriesResponse)));

        String branchesResponse = """
            [
                {
                    "name": "%s",
                    "commit": {
                        "sha": "%s"
                    }
                }
            ]
            """.formatted(branchName, commitSha);

        stubFor(get(urlPathEqualTo("/repos/" + username + "/" + repositoryNames[0] + "/branches"))
                .willReturn(aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody(branchesResponse)));

        webTestClient.get()
                .uri("/api/github/users/{username}/repositories", username)
                .exchange()
                .expectStatus().isOk()
                .expectBody(RepositoryDtoArray.class)
                .consumeWith(response -> {
                    var repositoryDtoArray = response.getResponseBody();

                    assertThat(repositoryDtoArray).isNotNull();
                    assertThat(repositoryDtoArray.repositories()).hasSize(1);

                    var repository = repositoryDtoArray.repositories().getFirst();

                    assertThat(repository.name()).isEqualTo(repositoryNames[0]);
                    assertThat(repository.owner().login()).isEqualTo(username);
                    assertThat(repository.fork()).isFalse();
                    assertThat(repository.branches()).hasSize(1);

                    var branch = repository.branches().getFirst();

                    assertThat(branch.name()).isEqualTo(branchName);
                    assertThat(branch.commit().sha()).isEqualTo(commitSha);
                });

        verify(getRequestedFor(urlPathEqualTo("/users/" + username + "/repos")));
        verify(getRequestedFor(urlPathEqualTo("/repos/" + username + "/" + repositoryNames[0] + "/branches")));
    }

    @Test
    void testListUserRepositoriesEmptyList() {
        final String username = "testUser";

        stubFor(get(urlPathEqualTo("/users/" + username + "/repos"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("[]")));

        webTestClient.get()
                .uri("/api/github/users/{username}/repositories", username)
                .exchange()
                .expectStatus().isOk()
                .expectBody(RepositoryDtoArray.class)
                .consumeWith(response -> {
                    var repositories = response.getResponseBody();

                    assertThat(repositories).isNotNull();
                    assertThat(repositories.repositories()).isEmpty();
                });

        verify(getRequestedFor(urlPathEqualTo("/users/" + username + "/repos")));
    }

    @Test
    void testListUserRepositoriesWithNoBranches() {
        final String username = "testUser";
        final String repositoryName = "testRepository";

        stubFor(get(urlPathEqualTo("/users/" + username + "/repos"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    [
                        {
                            "name": "%s",
                            "owner": {
                                "login": "%s"
                            },
                            "fork": false
                        }
                    ]
                    """.formatted(repositoryName, username))));

        stubFor(get(urlPathEqualTo("/repos/" + username + "/" + repositoryName + "/branches"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("[]")));

        webTestClient.get()
                .uri("/api/github/users/{username}/repositories", username)
                .exchange()
                .expectStatus().isOk()
                .expectBody(RepositoryDtoArray.class)
                .consumeWith(response -> {
                    var repositories = response.getResponseBody();

                    assertThat(repositories).isNotNull();
                    assertThat(repositories.repositories()).hasSize(1);

                    var repository = repositories.repositories().getFirst();

                    assertThat(repository.name()).isEqualTo(repositoryName);
                    assertThat(repository.owner().login()).isEqualTo(username);
                    assertThat(repository.fork()).isFalse();
                    assertThat(repository.branches()).isEmpty();
                });

        verify(getRequestedFor(urlPathEqualTo("/users/" + username + "/repos")));
        verify(getRequestedFor(urlPathEqualTo("/repos/" + username + "/" + repositoryName + "/branches")));
    }

    @Test
    void testListUserRepositoriesBranchRetrievalFailure() {
        final String username = "testUser";
        final String repositoryName = "testRepository";
        final String responseBodyMessage = "Failed to retrieve branches for repository: " + repositoryName;

        stubFor(get(urlPathEqualTo("/users/" + username + "/repos"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            [
                                {
                                    "name": "%s",
                                    "owner": {
                                        "login": "%s"
                                    },
                                    "fork": false
                                }
                            ]
                            """.formatted(repositoryName, username))));

        stubFor(get(urlPathEqualTo("/repos/" + username + "/" + repositoryName + "/branches"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())));

        webTestClient.get()
            .uri("/api/github/users/{username}/repositories", username)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .expectBody()
            .jsonPath("$.status").isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .jsonPath("$.message").isEqualTo(responseBodyMessage);

        verify(getRequestedFor(urlPathEqualTo("/users/" + username + "/repos")));
        verify(getRequestedFor(urlPathEqualTo("/repos/" + username + "/" + repositoryName + "/branches")));
    }

    @Test
    void testListUserRepositoriesServiceUnavailable() {
        final String username = "testUser";
        final String returnBodyMessage = "Service unavailable";

        stubFor(get(urlPathEqualTo("/users/" + username + "/repos"))
            .willReturn(aResponse()
                .withStatus(HttpStatus.SERVICE_UNAVAILABLE.value())
                .withBody(returnBodyMessage)));

        webTestClient.get()
                .uri("/api/github/users/{username}/repositories", username)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
                .expectBody()
                .jsonPath("$.status").isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value())
                .jsonPath("$.message").isEqualTo(returnBodyMessage);
    }
}