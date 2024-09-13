# User Repositories Service

This repository demonstrates an API-first approach to developing a Spring Boot service. 
API-first means that we define the API specification before implementing the service.

The primary responsibility of this service is to fetch public user repositories from the GitHub API and present only a subset of the data to the client.

The service is built using hexagonal architecture, which ensures a clear separation between the domain logic and the infrastructure. The `core` domain includes the `UserRepositoriesService` interface, which is implemented by the `GitHubUserRepositoriesService` class inside the `github` adapter package. This setup allows for easy swapping of GitHub with other repository providers (e.g., GitLab, Bitbucket) without modifying the core domain.

Additionally, there are the `infra` and `docker` packages. `infra` containing Terraform scripts for setting up the infrastructure, including ECS clusters, task definitions, ECR, API Gateway, and other resources necessary for deploying the service on AWS. `docker` with files required to run Jenkins locally.

In `docs` you can see short demo of the final step of building and deploying the application using Jenkins pipeline and Terraform scripts.

## Features

* Api First Design
* OpenAPI 3.0
* Hexagonal Architecture
* Spring Boot
* WebFlux
* Java 17 (can be easily upgraded to Java 21, but due to lack of time I've decided to stick with Java 17 as OpenApi Generator had some issues with running on Java 21)
* Gradle
* Docker
* Terraform

## Getting Started

### Prerequisites:

* Java 17
* Docker
* Terraform (optional to create infrastructure from local)
* AWS CLI (optional to deploy image to ECR from local)

### Local development:
1. Use your favorite IDE to import the project as a Gradle project.
2. Review **User repositories** OpenAPI specification. Open [src/main/resources/api.yml](./src/main/resources/api.yml)
3. Generate Spring controllers and models from the specification using Gradle plugin
    ```shell
    ./gradlew openApiGenerate
    ```
4. Generate Github models from the specification using Gradle plugin
    ```shell
    ./gradlew generateGitHubApi
    ```
   Github API Specification was cleaned up from unnecessary paths to not generate unnecessary controllers. For sake of simplicity all models were left intact.

### Running the Application Locally
Model and API generator tasks are run as a prerequisite to the `compileJava` task. This means that the using `bootRun` will first generate models then compile, run tests and at the end run the application.

Use the following command to run the application locally:
```shell
./gradlew bootRun
```

### Running the Application in Docker
Project contains multi-stage `Dockerfile` to build docker image with the application and produce minimal image size.
1. To build and run the application in Docker use the following commands:
```shell
docker build -t user-repositories-service .
```

2. Built docker image can be run using the following command:
```shell
docker run -p 8080:8080 user-repositories-service
```

3. Following application properties can be modified using environment variables:
* APP_CONCURRENCY_LEVEL - defaults to 10, can modify the number of concurrent requests to github api for getting branches
* APP_PAGE_SIZE - defaults to 100, can modify the number items fetched from github api in a single call 
* APP_GITHUB_API_TOKEN - defaults to blank, can be set to github token to increase rate limit

4. Example of running docker image with modified properties:
```shell
docker run -p 8080:8080 user-repositories-service -e APP_CONCURRENCY_LEVEL=5 -e APP_PAGE_SIZE=50
```

### API Usage
The application exposes a single endpoint: `GET /repos/{username}` to fetch a user's repositories from GitHub. Ensure the request includes the `Accept: application/json` header. Without this header, or if an invalid value is provided, the response will return `406 Not Acceptable`. If the username is invalid or missing, a `404 Not Found` response will be returned.

By default, the application makes unauthenticated requests to the GitHub API, which may result in limited API usage. In case of hitting the limit `403 Forbidden` is returned.  To increase the rate limit, you can set the `APP_GITHUB_API_TOKEN` environment variable with a GitHub API token, which will be used as a Bearer token in requests.
Example: 
```shell
docker run -p 8080:8080 user-repositories-service  -e APP_GITHUB_API_TOKEN='your_token'
```

**Note** The application only retrieves **public repositories** for the specified username (if the user exists). The GitHub API returns private repositories for authenticated users via a different endpoint (`/user/repos`), not the one used in this service (`users/{username}/repos`). To access private repositories, a Bearer token would need to be included in the request header and forwarded to the GitHub API. In such a scenario, the `/repos/{username}` endpoint would not be appropriate.

Example curl request:
```shell
curl --location 'http://localhost:8080/repos/maciek-prokopiuk' --header 'Accept: application/json'
```
Example response: 
```json
[
    {
        "repositoryName": "AdventOfCode2022",
        "ownerLogin": "maciek-prokopiuk",
        "branches": [
            {
                "branchName": "main",
                "lastCommitSHA": "0b76f75249ee9f3b257d815e2b19ae3244839897"
            }
        ]
    }
]
```

Error response model: 
```json
{
  "status": 404,
  "message": "Resource not found at https://api.github.com/users/nonexistentuser/repos?per_page=100"
}
```

## Infrastructure and CI/CD pipeline

### Infrastructure
Infrastructure is managed using Terraform scripts located in the `infra` directory. These scripts create the ECS cluster, task definition, ECR, API Gateway, and other necessary AWS resources. The infrastructure is built on a default VPC with public subnets. In a production environment, it would be better to create a dedicated VPC for better resource isolation.

**Note:** Terraform state is kept locally for simplicity. In a production environment, it should be stored remotely (e.g., S3 bucket) to allow for collaboration and state locking. Due to this fact it is not recommended to run terraform scripts from multiple machines. 

To set up the AWS environment:
1. Set up AWS credentials:
```shell
export AWS_ACCESS_KEY_ID=your_access_key
export AWS_SECRET_ACCESS_KEY=your_secret_key
```

2. Initialize and apply the Terraform configuration:
```shell
cd infra
terraform init
terraform apply
```

3. Push image to ECR:
* Authenticate docker to created ECR repository:

```shell
docker logout <account_id>.dkr.ecr.<region>.amazonaws.com # to make sure old credentials are not cached
aws ecr get-login-password --region <region> | docker login --username AWS --password-stdin <account_id>.dkr.ecr.<region>.amazonaws.com
```
 
* Build multi-platform image and push it to ECR:
```shell
docker buildx build --platform linux/amd64,linux/arm64 -t <account_id>.dkr.ecr.<region>.amazonaws.com/my-ecr-repo:latest --push .
```
* Force redeployment of ECS service
```shell
aws ecs update-service --cluster my-ecs-cluster --service my-ecs-service --force-new-deployment
```

After initial healthchecks application is avail

### CI/CD pipeline
Prerequisites:
- AWS Credentials: Make sure your Jenkins environment has access to AWS credentials (using environment variables or the AWS credentials plugin).
- Docker: Jenkins agents must have Docker and docker buildx installed.
- Terraform: The agent running the pipeline needs to have Terraform installed and configured to access AWS.

Jenkins pipeline is defined in `Jenkinsfile`. It setup the infrastructure, builds the application, runs tests, builds docker image and pushes it to ECR. Then it forces new deployment. 
API gateway url is returned from terraform output and can be checked in Jenkins console or using AWS Console. 

**Note:** Jenkins pipeline presents a simple example of how the CI/CD pipeline could be set up. 
In a production environment, more things should be considered, such as security scanning, secrets management, monitoring, smoke tests, blue/gree deployments etc.
Also it is worth mentioning that setting up infrastructure and deploying application in the same pipeline is not recommended. It is better to have separate pipelines for infrastructure and application deployment for better separation of concerns.

### Running Jenkins locally
To play with Jenkins locally you can use docker-compose file located in the `docker` directory.

**Note:** This setup is meant to be used on ARM64 architecture. If you are using x86_64 architecture you need to change versions of the tools in the Dockerfile
```shell
cd docker
docker-compose up -d 
```
It will start customised Jenkins server with preinstalled plugins. Jenkins will be available at `http://localhost:8080`.
From there you can setup AWS credentials, SCM repository and run the pipeline.




