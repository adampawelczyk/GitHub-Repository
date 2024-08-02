package com.example.githubrepository.model;

import java.util.List;

public record RepositoryInfo(
     String name,
     Owner owner,
     List<Branch> branches,
     boolean fork
) { }
