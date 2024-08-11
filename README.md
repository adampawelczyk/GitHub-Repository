# GitHub Repository Info API

## Overview

This Spring Boot application provides a RESTful API to retrieve non-forked repositories of a specified GitHub user along with their branches.

## Features

- Retrieve a list of non-forked repositories for a given GitHub user.
- For each repository, fetch and return the branches along with the branch name and the last commit SHA.

## Technologies Used

- Java 21
- Spring Boot 3.3.2
- Spring WebFlux
- Project Reactor
- WireMock (for testing)
- JUnit 5 (for testing)
- AssertJ (for testing)

## Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/adampawelczyk/GitHub-Repository
   cd GitHub-Repository
   ```

2. Build the project:
   ```bash
   ./gradlew clean build
   
3. Run the application:
    ```bash
    ./gradlew bootRun
    ```
4. The API will be available at http://localhost:8080/api/github.


## API Endpoints

### List User Repositories

- **GET** `/api/github/users/{username}/repositories`

  Retrieves a list of non-forked repositories for the specified GitHub username.

#### Response

Returns a JSON object containing a repositories field, which is an array of `RepositoryDto` objects.

Example:
```json
{
  "repositories": [
    {
      "name": "repositoryName",
      "owner": {
        "login": "ownerUsername"
      },
      "branches": [
        {
          "name": "branchName",
          "commit": {
            "sha": "commitSha"
          }
        }
      ],
      "fork": false
    }
  ]
}
```
#### Error Responses

- `404 Not Found`: If the user does not exist.

Example:
```json
{
    "message": "User not found",
    "status": "404"
}
```

<br>

- `403 Forbidden`: If access to the requested resource is denied.

Example:
```json
{
    "message": "Access denied",
    "status": "403"
}
```

<br>

- `400 Bad Request`: If the request is invalid.

Example:
```json
{
    "message": "Bad request",
    "status": "400"
}
```

<br>

- `Generic Client Error`: For any other client-side errors (4xx).

Example:
```json
{
    "message": "Client error: <error_message>",
    "status": "4xx"
}
```

<br>

- `500 Internal Server Error`: Indicates a failure to retrieve branches for a specific repository due to an unexpected server-side issue.

Example:
```json
{
    "message": "Failed to retrieve branches for repository: <repositoryName>",
    "status": "500"
}
```

<br>

- `503 Service Unavailable`: If the service is temporarily unavailable.

Example:
```json
{
    "message": "Service unavailable",
    "status": "503"
}
```

<br>

- `Generic Server Error`: For any other server-side errors (5xx).

Example:
```json
{
    "message": "Server error: <error_message>",
    "status": "5xx"
}
```

## Testing

Run the tests using:

```bash
./gradlew test
```


## License

This project is licensed under the [MIT License](https://github.com/adampawelczyk/GitHub-Repository/blob/master/LICENSE).
