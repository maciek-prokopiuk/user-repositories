package com.maciejprokopiuk.app.it;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import wiremock.org.eclipse.jetty.http.HttpStatus;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.Matchers.containsInAnyOrder;


@IntegrationTestWithWireMockServer
public class GithubControllerIT {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private Environment env;

    @BeforeEach
    void setUp() {
        WireMock.reset();
    }

    @Test
    void should_return_repositories_for_given_user() {
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

        verify(exactly(1), getRequestedFor(urlEqualTo("/users/maciek-prokopiuk/repos?per_page=1")));
        verify(exactly(2), getRequestedFor(urlMatching("/repos/maciek-prokopiuk/.*/branches\\?per_page=1")));
    }

    @Test
    void should_return_repositories_for_given_user_using_pagination() {
        // given
        var username = "maciek-prokopiuk";

        var wiremockPort = Integer.parseInt(env.getProperty("wiremock.server.port"));

        stubFor(get(urlEqualTo("/users/maciek-prokopiuk/repos?per_page=1"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withHeader("link", "<http://localhost:" + wiremockPort + "/users/maciek-prokopiuk/repos?per_page=1&page=2>; rel=\"next\"")
                        .withBodyFile("get_repos_200_page1.json")
                        .withStatus(HttpStatus.OK_200)));

        stubFor(get(urlEqualTo("/users/maciek-prokopiuk/repos?per_page=1&page=2"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withHeader("link", "<http://localhost:" + wiremockPort + "/users/maciek-prokopiuk/repos?per_page=1&page=1>; rel=\"prev\"")
                        .withBodyFile("get_repos_200_page2.json")
                        .withStatus(HttpStatus.OK_200)));

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

        verify(exactly(1), getRequestedFor(urlEqualTo("/users/maciek-prokopiuk/repos?per_page=1")));
        verify(exactly(1), getRequestedFor(urlEqualTo("/users/maciek-prokopiuk/repos?per_page=1&page=2")));
        verify(exactly(2), getRequestedFor(urlMatching("/repos/maciek-prokopiuk/.*/branches\\?per_page=1")));
    }

    @Test
    void should_return_404_if_user_not_found() {
        // given
        var username = "nonexistentuser";

        // when & then
        webTestClient.get()
                     .uri("/repos/" + username)
                     .accept(MediaType.APPLICATION_JSON)
                     .exchange()
                     .expectStatus().isNotFound();
    }

    @Test
    void should_return_403_if_ratelimited() {
        // given
        var username = "ratelimitexceeded";

        // when & then
        webTestClient.get()
                     .uri("/repos/" + username)
                     .accept(MediaType.APPLICATION_JSON)
                     .exchange()
                     .expectStatus().isForbidden();
    }

    @Test
    void shouldReturnNotAcceptableForInvalidMediaType() {
        // given
        var username = "maciek-prokopiuk";

        // when & then
        webTestClient.get()
                     .uri("/repos/" + username)
                     .accept(MediaType.APPLICATION_XML)
                     .exchange()
                     .expectStatus().isEqualTo(HttpStatus.NOT_ACCEPTABLE_406);
    }


}