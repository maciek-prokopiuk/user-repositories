package com.maciejprokopiuk.app.github;

import com.maciejprokopiuk.app.core.models.RepositoryDto;
import com.maciejprokopiuk.app.core.services.UserRepositoriesService;
import com.maciejprokopiuk.github.models.MinimalRepositoryDto;
import com.maciejprokopiuk.github.models.ShortBranchDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Slf4j
@Service
@RequiredArgsConstructor
public class GithubRepositoriesService implements UserRepositoriesService {

    @Value("${app.github.concurrency-level}")
    private static final int CONCURRENCY_LEVEL = 10; // concurrency level for getting branches for multiple repos in parallel

    private final GithubClient reposClient;

    public Flux<RepositoryDto> getAllRepositoriesForUser(String username) {
        return reposClient.getAllRepositoriesForUser(username, MinimalRepositoryDto.class)
                          .filter(repo -> !repo.getFork())
                          .flatMap(repo -> reposClient.getAllBranchesForRepo(username, repo.getName(), ShortBranchDto.class)
                                                      .collectList()
                                                      .map(branches -> GithubRepositoryMapper.mapToRepositoryDto(repo, branches)), CONCURRENCY_LEVEL);

    }


}