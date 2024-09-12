package com.maciejprokopiuk.app.github;

import com.maciejprokopiuk.app.core.exceptions.NotFoundException;
import com.maciejprokopiuk.app.core.models.BranchDto;
import com.maciejprokopiuk.app.core.models.RepositoryDto;
import com.maciejprokopiuk.github.models.MinimalRepositoryDto;
import com.maciejprokopiuk.github.models.ShortBranchCommitDto;
import com.maciejprokopiuk.github.models.ShortBranchDto;
import com.maciejprokopiuk.github.models.SimpleUserDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@SpringBootTest
class GithubUserRepositoriesServiceTest {

    @MockBean
    private GithubClient githubClient;

    @Autowired
    private GithubRepositoriesService githubRepositoriesService;

    @Test
    void should_return_single_repository() {
        // Given
        var username = "testUser";
        var repositoryName = "testRepo";
        var branchName = "testBranch";
        var sha = "sha";

        var repositoryDto = RepositoryDto.builder()
                                         .repositoryName(repositoryName)
                                         .ownerLogin(username)
                                         .branches(List.of(new BranchDto(branchName, sha)))
                                         .build();

        var minimalRepositoryDto = new MinimalRepositoryDto()
                .name(repositoryName)
                .owner(new SimpleUserDto().login(username))
                .fork(false);
        when(githubClient.getAllRepositoriesForUser(username, MinimalRepositoryDto.class)).thenReturn(Flux.just(minimalRepositoryDto));

        var branch = new ShortBranchDto().name(branchName).commit(new ShortBranchCommitDto(sha, URI.create("uri")));
        when(githubClient.getAllBranchesForRepo(username, repositoryName, ShortBranchDto.class)).thenReturn(Flux.just(branch));
        // When
        Flux<RepositoryDto> result = githubRepositoriesService.getAllRepositoriesForUser(username);

        // Then
        StepVerifier.create(result)
                    .expectNext(repositoryDto)
                    .verifyComplete();
    }

    @Test
    void should_return_non_fork_repository() {
        // Given
        String username = "testUser";

        var forkedRepo = new MinimalRepositoryDto()
                .name("forkedRepo")
                .owner(new SimpleUserDto().login("testUser"))
                .fork(true);

        var nonForkedRepo = new MinimalRepositoryDto()
                .name("nonForkedRepo")
                .owner(new SimpleUserDto().login("testUser"))
                .fork(false);
        when(githubClient.getAllRepositoriesForUser(username, MinimalRepositoryDto.class)).thenReturn(Flux.just(forkedRepo, nonForkedRepo));

        var branch = new ShortBranchDto().name("testBranch").commit(new ShortBranchCommitDto("sha", URI.create("uri")));
        when(githubClient.getAllBranchesForRepo("testUser", "nonForkedRepo", ShortBranchDto.class)).thenReturn(Flux.just(branch));

        // When
        Flux<RepositoryDto> result = githubRepositoriesService.getAllRepositoriesForUser(username);

        // Then
        StepVerifier.create(result)
                    .assertNext(repositoryDto -> {
                        assertEquals("nonForkedRepo", repositoryDto.getRepositoryName());
                        assertEquals("testUser", repositoryDto.getOwnerLogin());
                    })
                    .verifyComplete();
    }

    @Test
    void should_return_multiple_repositories() {
        // Given
        String username = "testUser";

        var repo1 = new MinimalRepositoryDto()
                .name("repo1")
                .owner(new SimpleUserDto().login("testUser"))
                .fork(false);

        var repo2 = new MinimalRepositoryDto()
                .name("repo2")
                .owner(new SimpleUserDto().login("testUser"))
                .fork(false);

        when(githubClient.getAllRepositoriesForUser(username, MinimalRepositoryDto.class)).thenReturn(Flux.fromIterable(List.of(repo1, repo2)));

        var branch = new ShortBranchDto().name("testBranch").commit(new ShortBranchCommitDto("sha", URI.create("uri")));
        when(githubClient.getAllBranchesForRepo(eq("testUser"), any(), eq(ShortBranchDto.class))).thenReturn(Flux.just(branch));

        // When
        Flux<RepositoryDto> result = githubRepositoriesService.getAllRepositoriesForUser(username);

        // Then
        StepVerifier.create(result)
                    .assertNext(repositoryDto -> {
                        assertEquals("repo1", repositoryDto.getRepositoryName());
                        assertEquals("testUser", repositoryDto.getOwnerLogin());
                    })
                    .assertNext(repositoryDto -> {
                        assertEquals("repo2", repositoryDto.getRepositoryName());
                        assertEquals("testUser", repositoryDto.getOwnerLogin());
                    })
                    .verifyComplete();
    }

    @Test
    void should_fetch_branches_in_parallel() {
        // Given
        String username = "testUser";
        int delayInSeconds = 5;
        int numberOfRepos = 5;

        var repos = IntStream.range(0, numberOfRepos)
                             .mapToObj(i -> new MinimalRepositoryDto()
                                     .name("repo" + i)
                                     .owner(new SimpleUserDto().login(username))
                                     .fork(false))
                             .collect(Collectors.toList());




        when(githubClient.getAllRepositoriesForUser(username, MinimalRepositoryDto.class)).thenReturn(Flux.fromIterable(repos));

        var branch = new ShortBranchDto().name("testBranch").commit(new ShortBranchCommitDto("sha", URI.create("uri")));

        // Simulate delay in fetching branches
        when(githubClient.getAllBranchesForRepo(anyString(), anyString(), eq(ShortBranchDto.class)))
                .thenAnswer(invocation -> Mono.delay(Duration.ofSeconds(delayInSeconds)).thenMany(Flux.defer(() -> Flux.just(branch))));

        // When
        long start = System.currentTimeMillis();
        var result = githubRepositoriesService.getAllRepositoriesForUser(username);
        StepVerifier.create(result).expectNextCount(numberOfRepos).verifyComplete();
        long duration = System.currentTimeMillis() - start;

        // Then
        assertTrue(duration < delayInSeconds * numberOfRepos * 1000, "Branches should be fetched in parallel");
    }

    @Test
    void should_propagate_404_from_github_client() {
        // Given
        String username = "testUser";
        when(githubClient.getAllRepositoriesForUser(username, MinimalRepositoryDto.class))
                .thenThrow(new NotFoundException("An error occurred"));

        // When
        // Then
        assertThrows(NotFoundException.class, () -> githubRepositoriesService.getAllRepositoriesForUser(username));
    }

    @Test
    void should_handle_empty_flux_from_github_client() {
        // Given
        String username = "testUser";
        when(githubClient.getAllRepositoriesForUser(username, MinimalRepositoryDto.class)).thenReturn(Flux.empty());

        // When
        Flux<RepositoryDto> result = githubRepositoriesService.getAllRepositoriesForUser(username);

        // Then
        StepVerifier.create(result)
                    .expectNextCount(0)
                    .verifyComplete();
    }

}