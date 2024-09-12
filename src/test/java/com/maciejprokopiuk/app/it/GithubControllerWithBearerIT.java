package com.maciejprokopiuk.app.it;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.Matchers.containsInAnyOrder;


@IntegrationTestWithWireMockServer
@TestPropertySource(properties = {
        "app.github.api-token=test_token"
})
public class GithubControllerWithBearerIT {

    @Autowired
    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        WireMock.reset();
    }

    @Test
    void should_return_repositories_for_given_user_and_attach_bearer_token_to_the_request() {
        // given
        var username = "maciek-prokopiuk";

        // when & then
        webTestClient.get()
                     .uri("/repos/" + username)
                     .accept(MediaType.APPLICATION_JSON)
                     .exchange()
                     .expectStatus().isOk()
                     .expectBody()
                     .jsonPath("$").isArray()
                     .jsonPath("$[*].repositoryName").value(containsInAnyOrder("AdventOfCode2022", "codewise-internship-task-2k18"))
                     .jsonPath("$[0].repositoryName").isNotEmpty()
                     .jsonPath("$[1].repositoryName").isNotEmpty()
                     .jsonPath("$[0].ownerLogin").isEqualTo(username)
                     .jsonPath("$[1].ownerLogin").isEqualTo(username)
                     .jsonPath("$[0].branches").isArray()
                     .jsonPath("$[1].branches").isArray()
                     .jsonPath("$[0].branches[*].branchName").value(containsInAnyOrder("main", "develop"))
                     .jsonPath("$[1].branches[*].branchName").value(containsInAnyOrder("main", "develop"));

        verify(exactly(1), getRequestedFor(urlEqualTo("/users/maciek-prokopiuk/repos?per_page=1"))
                .withHeader("Authorization", equalTo("Bearer test_token")));
        verify(exactly(2), getRequestedFor(urlMatching("/repos/maciek-prokopiuk/.*/branches\\?per_page=1"))
                .withHeader("Authorization", equalTo("Bearer test_token")));
    }
}