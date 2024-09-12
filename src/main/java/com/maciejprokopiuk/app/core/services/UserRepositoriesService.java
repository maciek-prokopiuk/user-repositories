package com.maciejprokopiuk.app.core.services;

import com.maciejprokopiuk.app.core.models.RepositoryDto;
import reactor.core.publisher.Flux;

public interface UserRepositoriesService {

    Flux<RepositoryDto> getAllRepositoriesForUser(String username);
}
