package com.example.githubrepository.controller;

import com.example.githubrepository.dto.RepositoryDto;
import com.example.githubrepository.dto.RepositoryDtoArray;
import com.example.githubrepository.service.GitHubRepositoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/github")
public class GitHubRepositoryController {
    private final GitHubRepositoryService gitHubRepositoryService;

    GitHubRepositoryController(GitHubRepositoryService service) {
        this.gitHubRepositoryService = service;
    }

    @GetMapping("/users/{username}/repositories")
    public Mono<ResponseEntity<RepositoryDtoArray>> listUserRepositories(@PathVariable String username) {
        return gitHubRepositoryService.getUserRepositories(username)
                .map(repositories -> ResponseEntity.ok().body(repositories));
    }
}
