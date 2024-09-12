package com.maciejprokopiuk.app.github;

import com.maciejprokopiuk.github.models.MinimalRepositoryDto;
import com.maciejprokopiuk.github.models.ShortBranchCommitDto;
import com.maciejprokopiuk.github.models.ShortBranchDto;
import com.maciejprokopiuk.github.models.SimpleUserDto;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;

import static com.maciejprokopiuk.app.github.GithubRepositoryMapper.mapToRepositoryDto;
import static org.junit.jupiter.api.Assertions.*;

class GithubRepositoryMapperTest {

    @Test
    void should_map_to_repositoryDto() throws Exception {
        // Given
        var minimalRepositoryDto = new MinimalRepositoryDto()
                .name("testRepo")
                .owner(new SimpleUserDto().login("testUser"))
                .fork(false);

        var branchDto1 = new ShortBranchDto()
                .name("testBranch1")
                .commit(new ShortBranchCommitDto("sha1", new URI("http://localhost")));
        var branchDto2 = new ShortBranchDto()
                .name("testBranch2")
                .commit(new ShortBranchCommitDto("sha2", new URI("http://localhost")));
        // When

        var result = mapToRepositoryDto(minimalRepositoryDto, List.of(branchDto1, branchDto2));
        // Then
        assertEquals("testRepo", result.getRepositoryName());
        assertEquals("testUser", result.getOwnerLogin());
        assertEquals(2, result.getBranches().size());
        assertEquals("testBranch1", result.getBranches().get(0).getBranchName());
        assertEquals("sha1", result.getBranches().get(0).getLastCommitSHA());
        assertEquals("testBranch2", result.getBranches().get(1).getBranchName());
        assertEquals("sha2", result.getBranches().get(1).getLastCommitSHA());
    }
}