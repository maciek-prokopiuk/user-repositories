package com.maciejprokopiuk.app.core.exceptions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maciejprokopiuk.app.core.models.ErrorResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.NotAcceptableStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;


@Component
@Order(-1)
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler implements WebExceptionHandler {

    private final ObjectMapper objectMapper;

    @SneakyThrows
    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        if (ex instanceof NotFoundException notFoundException) {
            return respond(exchange, notFoundException.getMessage(), HttpStatus.NOT_FOUND);
        } else if (ex instanceof NotAcceptableStatusException notAcceptableException) {
            return respond(exchange, notAcceptableException.getMessage(), HttpStatus.NOT_ACCEPTABLE);
        } else if (ex instanceof ForbiddenException forbidden) {
            return respond(exchange, forbidden.getMessage(), HttpStatus.FORBIDDEN);
        }
        log.error("Unexpected error occurred", ex);
        return Mono.error(ex);
    }

    public Mono<Void> respond(ServerWebExchange exchange, String reason, HttpStatus httpStatus) throws JsonProcessingException {
        exchange.getResponse().setStatusCode(httpStatus);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        var errorResponse = ErrorResponseDto.builder().status(httpStatus.value()).message(reason).build();
        var bufferFactory = exchange.getResponse().bufferFactory();
        var dataBuffer = bufferFactory.wrap(getBytes(errorResponse));
        return exchange.getResponse().writeWith(Mono.just(dataBuffer));
    }

    private byte[] getBytes(ErrorResponseDto errorResponse) throws JsonProcessingException {
        return objectMapper.writeValueAsString(errorResponse).getBytes(StandardCharsets.UTF_8);
    }
}