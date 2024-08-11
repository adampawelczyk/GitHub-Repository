package com.example.githubrepository.dto;

import com.example.githubrepository.model.Branch;
import com.example.githubrepository.model.Owner;

import java.util.List;

public record RepositoryDto(
    String name,
    Owner owner,
    List<Branch> branches,
    boolean fork
) { }
