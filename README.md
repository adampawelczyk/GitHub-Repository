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
- Mockito (for testing)
- JUnit 5 (for testing)
- MockWebServer (for testing)

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

Returns a JSON array of `RepositoryInfo` objects.

Example:
```json
[
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
```
#### Error Responses

- `404 Not Found`: If the user does not exist.

Example:
```json
{
    "message": "User with 'exampleUsername' username wasn't found",
    "status": "404"
}
```

## Testing

Run the unit tests using:

```bash
./gradlew test
