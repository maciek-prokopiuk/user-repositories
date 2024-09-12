package com.maciejprokopiuk.app.core.controllers;

import com.maciejprokopiuk.app.core.models.RepositoryDto;
import com.maciejprokopiuk.app.core.services.UserRepositoriesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.springframework.http.ResponseEntity.ok;

@RestController
@RequiredArgsConstructor
public class ReposController implements ReposApi {

    private final UserRepositoriesService userRepositoriesService;

    @Override
    public Mono<ResponseEntity<Flux<RepositoryDto>>> listUserRepositories(String username, String accept, ServerWebExchange exchange) {
        return Mono.just(ok(userRepositoriesService.getAllRepositoriesForUser(username)));
    }

}
