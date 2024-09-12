package com.maciejprokopiuk.app.github;


import com.maciejprokopiuk.app.core.exceptions.ForbiddenException;
import com.maciejprokopiuk.app.core.exceptions.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class GithubClient {

    @Value("${app.github.page-size}")
    private int pageSize;
    private static final Pattern NEXT_LINK_PATTERN = Pattern.compile("<([^>]+)>;\\s*rel=\"next\"");
    public static final String LINK_HEADER_NAME = "link";
    private static final String BRANCHES_URL = "%s/repos/%s/%s/branches?per_page=%d";
    private static final String REPOSE_URL = "%s/users/%s/repos?per_page=%d";
    private final WebClient webClient;

    @Value("${app.github.api-url}")
    private String baseUrl;

    public <T> Flux<T> getAllRepositoriesForUser(String username, Class<T> type) {
        return getDataFromUrl(String.format(REPOSE_URL, baseUrl, username, pageSize), type);
    }

    public <T> Flux<T> getAllBranchesForRepo(String owner, String repositoryName, Class<T> type) {
        return getDataFromUrl(String.format(BRANCHES_URL, baseUrl, owner, repositoryName, pageSize), type);
    }

    public <T> Flux<T> getDataFromUrl(String url, Class<T> type) {
        return webClient.get()
                        .uri(url)
                        .exchangeToFlux(clientResponse -> {
                            if (clientResponse.statusCode().equals(HttpStatus.NOT_FOUND)) {
                                return Flux.error(new NotFoundException("Resource not found at " + url));
                            }

                            if (clientResponse.statusCode().equals(HttpStatus.FORBIDDEN)) {
                                return Flux.error(new ForbiddenException("Throttled due to rate limit. Wait and try again later or pass a valid Bearer token to increase the limits"));
                            }

                            var body = clientResponse.bodyToFlux(type);
                            var linkHeader = clientResponse.headers().header(LINK_HEADER_NAME).stream().findFirst().orElse("");
                            var nextPage = extractNextLink(linkHeader);
                            return nextPage != null ? body.concatWith(getDataFromUrl(nextPage, type)) : body;
                        });
    }

    private String extractNextLink(String linkHeader) {
        // Regular expression to match the 'next' rel link in the Link header
        Matcher matcher = NEXT_LINK_PATTERN.matcher(linkHeader);
        if (matcher.find()) {
            return matcher.group(1); // Returns the next URL if found
        }
        return null;
    }

}

