package com.example.githubrepository.service;

import com.example.githubrepository.exception.UserNotFoundException;
import com.example.githubrepository.model.Branch;
import com.example.githubrepository.model.RepositoryInfo;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class GitHubRepositoryService {
    private final WebClient webClient;
    private final String GITHUB_API_URL = "https://api.github.com";

    public GitHubRepositoryService() {
        this.webClient = WebClient.create(GITHUB_API_URL);
    }

    public GitHubRepositoryService(WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<List<RepositoryInfo>> getUserRepositories(String username) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/users/{username}/repos").build(username))
                .retrieve()
                .bodyToFlux(RepositoryInfo.class)
                .filter(repository -> !repository.fork())
                .flatMap(repository -> getBranches(username, repository.name())
                        .collectList()
                        .map(branches -> new RepositoryInfo(
                                repository.name(),
                                repository.owner(),
                                branches,
                                repository.fork())))
                .collectList()
                .onErrorMap(WebClientResponseException.NotFound.class, e ->
                        new UserNotFoundException(String.format("User with '%s' username wasn't found", username)));
    }

    private Flux<Branch> getBranches(String username, String repositoryName) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/repos/{username}/{repository}/branches").build(username, repositoryName))
                .retrieve()
                .bodyToFlux(Branch.class);
    }
}
