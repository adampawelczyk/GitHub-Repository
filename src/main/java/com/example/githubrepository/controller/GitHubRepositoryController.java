package com.example.githubrepository.controller;

import com.example.githubrepository.model.RepositoryInfo;
import com.example.githubrepository.service.GitHubRepositoryService;
import org.springframework.beans.factory.annotation.Autowired;
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
    @Autowired
    private GitHubRepositoryService gitHubRepositoryService;

    @GetMapping("/users/{username}/repositories")
    public Mono<ResponseEntity<List<RepositoryInfo>>> listUserRepositories(@PathVariable String username) {
        return gitHubRepositoryService.getUserRepositories(username)
                .map(repositories -> ResponseEntity.ok().body(repositories));
    }
}
