package com.example.githubrepository.service;

import com.example.githubrepository.exception.*;
import com.example.githubrepository.model.Branch;
import com.example.githubrepository.model.RepositoryInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class GitHubRepositoryService {
    @Autowired
    private WebClient webClient;

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
                                repository.fork()))
                        .onErrorResume(e -> Mono.just(new RepositoryInfo(
                                repository.name(),
                                repository.owner(),
                                List.of(),
                                repository.fork()))))
                .collectList()
                .onErrorMap(WebClientResponseException.NotFound.class, e ->
                        new UserNotFoundException(String.format("User with '%s' username wasn't found", username)))
                .onErrorMap(WebClientResponseException.Forbidden.class, e ->
                        new AccessDeniedException(("Access denied to the resource. Check your permissions.")))
                .onErrorMap(WebClientResponseException.BadRequest.class, e ->
                        new BadRequestException("Bad request. Please check the request parameters."))
                .onErrorMap(WebClientResponseException.ServiceUnavailable.class, e ->
                        new ServiceUnavailableException("Service is temporarily unavailable. Please try again later."));
    }

    private Flux<Branch> getBranches(String username, String repositoryName) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/repos/{username}/{repository}/branches").build(username, repositoryName))
                .retrieve()
                .bodyToFlux(Branch.class);
    }
}
