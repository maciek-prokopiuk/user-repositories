package com.maciejprokopiuk.app.github;

import com.maciejprokopiuk.app.core.models.BranchDto;
import com.maciejprokopiuk.app.core.models.RepositoryDto;
import com.maciejprokopiuk.github.models.MinimalRepositoryDto;
import com.maciejprokopiuk.github.models.ShortBranchDto;
import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
public final class GithubRepositoryMapper {

    public static RepositoryDto mapToRepositoryDto(MinimalRepositoryDto repo, List<ShortBranchDto> branches) {
        return RepositoryDto.builder()
                            .repositoryName(repo.getName())
                            .ownerLogin(repo.getOwner().getLogin())
                            .branches(branches.stream()
                                              .map(branch -> new BranchDto(branch.getName(), branch.getCommit().getSha())).toList())
                            .build();
    }
}