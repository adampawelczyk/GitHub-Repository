package com.example.githubrepository.service;

import com.example.githubrepository.config.TestConfig;
import com.example.githubrepository.model.Branch;
import com.example.githubrepository.model.RepositoryInfo;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Comparator;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@WireMockTest(httpPort = 8080)
@ContextConfiguration(classes = TestConfig.class)
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
                        .withBody("[{\"name\":\"" + repositoryName + "\",\"owner\":{\"login\":\"" + username +"\"},\"fork\":false}]")));

        stubFor(get(urlPathEqualTo("/repos/" + username + "/testRepository/branches"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("[{\"name\":\"" + branchName + "\",\"commit\":{\"sha\":\"" + commitSha + "\"}}]")));

        webTestClient.get()
                .uri("/api/github/users/{username}/repositories", username)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(RepositoryInfo.class)
                .consumeWith(response -> {
                    List<RepositoryInfo> repositories = response.getResponseBody();
                    assert repositories != null;
                    assert repositories.size() == 1;
                    RepositoryInfo repository = repositories.getFirst();
                    assert repositoryName.equals(repository.name());
                    assert username.equals(repository.owner().login());
                    assert !repository.fork();
                    assert repository.branches().size() == 1;
                    Branch branch = repository.branches().getFirst();
                    assert branchName.equals(branch.name());
                    assert commitSha.equals(branch.commit().sha());
                });

        verify(getRequestedFor(urlPathEqualTo("/users/" + username + "/repos")));
        verify(getRequestedFor(urlPathEqualTo("/repos/" + username + "/" + repositoryName + "/branches")));
    }

    @Test
    void testListUserRepositoriesUserNotFound() {
        final String username = "testUser";

        stubFor(get(urlPathEqualTo("/users/" + username + "/repos"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"message\":\"User with '" + username + "' username wasn't found\"}")));

        webTestClient.get()
                .uri("/api/github/users/{username}/repositories", username)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.status").isEqualTo("404")
                .jsonPath("$.message").isEqualTo("User with '" + username + "' username wasn't found");

        verify(getRequestedFor(urlPathEqualTo("/users/" + username + "/repos")));
    }

    @Test
    void testListUserRepositoriesOnlyNonForked() {
        final String username = "testUser";
        final String[] repositoryNames = {"nonForked", "forked"};
        final String branchName = "testBranch";
        final String commitSha = "commit-sha";

        String repositoriesResponse = "["
                + "{\"name\":\"" + repositoryNames[0] + "\",\"owner\":{\"login\":\"" + username + "\"},\"fork\":false},"
                + "{\"name\":\"" + repositoryNames[1] + "\",\"owner\":{\"login\":\"" + username + "\"},\"fork\":true}"
                + "]";

        stubFor(get(urlPathEqualTo("/users/" + username + "/repos"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(repositoriesResponse)));

        String branchesResponse = "[{\"name\":\"" + branchName + "\",\"commit\":{\"sha\":\"" + commitSha + "\"}}]";

        stubFor(get(urlPathEqualTo("/repos/" + username + "/" + repositoryNames[0] + "/branches"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(branchesResponse)));

        webTestClient.get()
                .uri("/api/github/users/{username}/repositories", username)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(RepositoryInfo.class)
                .consumeWith(response -> {
                    List<RepositoryInfo> repositories = response.getResponseBody();
                    assert repositories != null;
                    assert repositories.size() == 1;
                    RepositoryInfo repository = repositories.getFirst();
                    assert repositoryNames[0].equals(repository.name());
                    assert username.equals(repository.owner().login());
                    assert !repository.fork();
                    assert repository.branches().size() == 1;
                    Branch branch = repository.branches().getFirst();
                    assert branchName.equals(branch.name());
                    assert commitSha.equals(branch.commit().sha());
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
                .expectBodyList(RepositoryInfo.class)
                .consumeWith(response -> {
                    List<RepositoryInfo> repositories = response.getResponseBody();
                    assert repositories != null;
                    assert repositories.isEmpty();
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
                        .withBody("[{\"name\":\"" + repositoryName + "\",\"owner\":{\"login\":\"" + username + "\"},\"fork\":false}]")));

        stubFor(get(urlPathEqualTo("/repos/" + username + "/" + repositoryName + "/branches"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("[]")));

        webTestClient.get()
                .uri("/api/github/users/{username}/repositories", username)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(RepositoryInfo.class)
                .consumeWith(response -> {
                    List<RepositoryInfo> repositories = response.getResponseBody();
                    assert repositories != null;
                    assert repositories.size() == 1;
                    RepositoryInfo repository = repositories.getFirst();
                    assert repositoryName.equals(repository.name());
                    assert username.equals(repository.owner().login());
                    assert !repository.fork();
                    assert repository.branches().isEmpty();
                });

        verify(getRequestedFor(urlPathEqualTo("/users/" + username + "/repos")));
        verify(getRequestedFor(urlPathEqualTo("/repos/" + username + "/" + repositoryName + "/branches")));
    }

    @Test
    void testListUserRepositoriesPartialBranchRetrievalFailure() {
        final String username = "testUser";
        final String[] repositoryNames = {"testRepository1", "testRepository2"};
        final String branchName = "testBranch";

        stubFor(get(urlPathEqualTo("/users/" + username + "/repos"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("[{\"name\":\"" + repositoryNames[0] + "\",\"owner\":{\"login\":\"" + username +
                                "\"},\"fork\":false},{\"name\":\"" + repositoryNames[1] + "\",\"owner\":{\"login\":\""
                                + username + "\"},\"fork\":false}]")));

        stubFor(get(urlPathEqualTo("/repos/" + username + "/" + repositoryNames[0] + "/branches"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("[{\"name\":\"" + branchName + "\",\"commit\":{\"sha\":\"commit-sha1\"}}]")));

        stubFor(get(urlPathEqualTo("/repos/" + username + "/" + repositoryNames[1] + "/branches"))
                .willReturn(aResponse()
                        .withStatus(500)));

        webTestClient.get()
                .uri("/api/github/users/{username}/repositories", username)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(RepositoryInfo.class)
                .consumeWith(response -> {
                    List<RepositoryInfo> repositories = response.getResponseBody();
                    assert repositories != null;
                    assert repositories.size() == 2;

                    repositories.sort(Comparator.comparing(RepositoryInfo::name));

                    RepositoryInfo repo1 = repositories.getFirst();
                    assert repositoryNames[0].equals(repo1.name());
                    assert username.equals(repo1.owner().login());
                    assert !repo1.fork();
                    assert repo1.branches().size() == 1;
                    assert branchName.equals(repo1.branches().getFirst().name());

                    RepositoryInfo repo2 = repositories.get(1);
                    assert repositoryNames[1].equals(repo2.name());
                    assert username.equals(repo2.owner().login());
                    assert !repo2.fork();
                    assert repo2.branches().isEmpty();
                });

        verify(getRequestedFor(urlPathEqualTo("/users/" + username + "/repos")));
        verify(getRequestedFor(urlPathEqualTo("/repos/" + username + "/" + repositoryNames[0] + "/branches")));
        verify(getRequestedFor(urlPathEqualTo("/repos/" + username + "/" + repositoryNames[1] + "/branches")));
    }

    @Test
    void testListUserRepositoriesBranchRetrievalNon200Response() {
        final String username = "testUser";
        final String repositoryName = "testRepository";

        stubFor(get(urlPathEqualTo("/users/" + username + "/repos"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("[{\"name\":\"" + repositoryName + "\",\"owner\":{\"login\":\"" + username + "\"},\"fork\":false}]")));

        stubFor(get(urlPathEqualTo("/repos/" + username + "/" + repositoryName + "/branches"))
                .willReturn(aResponse()
                        .withStatus(403)));

        webTestClient.get()
                .uri("/api/github/users/{username}/repositories", username)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(RepositoryInfo.class)
                .consumeWith(response -> {
                    List<RepositoryInfo> repositories = response.getResponseBody();
                    assert repositories != null;
                    assert repositories.size() == 1;

                    RepositoryInfo repo = repositories.getFirst();
                    assert repositoryName.equals(repo.name());
                    assert username.equals(repo.owner().login());
                    assert !repo.fork();
                    assert repo.branches().isEmpty();
                });

        verify(getRequestedFor(urlPathEqualTo("/users/" + username + "/repos")));
        verify(getRequestedFor(urlPathEqualTo("/repos/" + username + "/" + repositoryName + "/branches")));
    }

    @Test
    void testListUserRepositoriesNetworkError() {
        String username = "testUser";

        stubFor(get(urlPathEqualTo("/users/" + username + "/repos"))
                .willReturn(aResponse()
                        .withFault(Fault.CONNECTION_RESET_BY_PEER)));

        webTestClient.get()
                .uri("/api/github/users/{username}/repositories", username)
                .exchange()
                .expectStatus().is5xxServerError();
    }
}