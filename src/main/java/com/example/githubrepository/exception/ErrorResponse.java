package com.example.githubrepository.exception;

public record ErrorResponse(
        int status,
        String message
) { }
