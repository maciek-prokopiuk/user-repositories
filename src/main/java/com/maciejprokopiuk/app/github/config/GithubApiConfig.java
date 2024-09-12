package com.maciejprokopiuk.app.github.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class GithubApiConfig {

    @Bean
    public WebClient githubWebClient(ObjectMapper mapper, @Value("${app.github.api-token}") String apiToken) {
        ExchangeStrategies strategies = ExchangeStrategies
                .builder()
                .codecs(clientDefaultCodecsConfigurer -> {
                    clientDefaultCodecsConfigurer.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder(mapper, MediaType.APPLICATION_JSON));
                    clientDefaultCodecsConfigurer.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder(mapper, MediaType.APPLICATION_JSON));
                })
                .build();
        var builder = WebClient.builder()
                                            .defaultHeader("Accept", "application/vnd.github+json")
                                            .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                                            .exchangeStrategies(strategies);

        if(!apiToken.isBlank()) {
            builder.defaultHeader("Authorization", "Bearer " + apiToken);
        }

        return builder.build();
    }

}
