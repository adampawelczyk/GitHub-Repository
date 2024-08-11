package com.example.githubrepository.adapter;

import com.example.githubrepository.exception.*;
import com.example.githubrepository.model.Branch;
import com.example.githubrepository.model.Repository;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;

@Component
public class GitHubRepositoryAdapter {
    private final WebClient webClient;

    public GitHubRepositoryAdapter(WebClient webClient) {
        this.webClient = webClient;
    }

    public Flux<Repository> getUserRepositories(String username) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/users/{username}/repos").build(username))
                .retrieve()
                .bodyToFlux(Repository.class)
                .onErrorMap(WebClientResponseException.class, this::handleWebClientException);
    }

    public Flux<Branch> getBranches(String username, String repositoryName) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/repos/{username}/{repository}/branches").build(username, repositoryName))
                .retrieve()
                .bodyToFlux(Branch.class)
                .onErrorMap(WebClientResponseException.class, e ->
                        new BranchRetrievalException("Failed to retrieve branches for repository: " + repositoryName)
                );
    }

    private Throwable handleWebClientException(WebClientResponseException e) {
        if (e.getStatusCode().is4xxClientError()) {
            return switch (e) {
                case WebClientResponseException.NotFound notFound -> new UserNotFoundException("User not found");
                case WebClientResponseException.Forbidden forbidden -> new AccessDeniedException("Access denied");
                case WebClientResponseException.BadRequest badRequest -> new BadRequestException("Bad request");

                default -> new ClientErrorException("Client error: " + e.getMessage());
            };

        } else if (e.getStatusCode().is5xxServerError()) {
            if (e instanceof WebClientResponseException.ServiceUnavailable) {
                return new ServiceUnavailableException("Service unavailable");
            }

            return new ServerErrorException("Server error: " + e.getMessage());
        }
        return e;
    }
}
