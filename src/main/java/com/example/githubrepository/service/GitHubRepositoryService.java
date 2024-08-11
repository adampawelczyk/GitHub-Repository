package com.example.githubrepository.service;

import com.example.githubrepository.adapter.GitHubRepositoryAdapter;
import com.example.githubrepository.dto.RepositoryDto;
import com.example.githubrepository.dto.RepositoryDtoArray;
import com.example.githubrepository.model.Branch;
import com.example.githubrepository.model.Repository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class GitHubRepositoryService {
    private final GitHubRepositoryAdapter gitHubRepositoryAdapter;

    public GitHubRepositoryService(GitHubRepositoryAdapter gitHubRepositoryAdapter) {
        this.gitHubRepositoryAdapter = gitHubRepositoryAdapter;
    }

    public Mono<RepositoryDtoArray> getUserRepositories(String username) {
        return gitHubRepositoryAdapter.getUserRepositories(username)
                .filter(repository -> !repository.fork())
                .flatMap(repository -> getBranches(username, repository.name())
                        .collectList()
                        .map(branches -> toRepositoryDto(repository, branches)))
                .collectList()
                .map(RepositoryDtoArray::new);
    }

    private Flux<Branch> getBranches(String username, String repositoryName) {
        return gitHubRepositoryAdapter.getBranches(username, repositoryName);
    }

    private RepositoryDto toRepositoryDto(Repository repository, List<Branch> branches) {
        return new RepositoryDto(repository.name(), repository.owner(), branches, repository.fork());
    }
}
