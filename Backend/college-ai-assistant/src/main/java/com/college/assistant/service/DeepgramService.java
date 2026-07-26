package com.college.assistant.service;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
public class DeepgramService {

    private final WebClient webClient;

    @Value("${deepgram.api.key}")
    private String apiKey;

    public DeepgramService(WebClient webClient) {
        this.webClient = webClient;
    }

    // Non-blocking: server thread free rehta hai, lekin poora audio ban ne ke
    // baad hi response jaata hai (no partial-request / Range issues, no leaks)
    public Mono<byte[]> textToSpeech(String text) {

        text = text == null ? "" : text.trim();
        text = text.replaceAll("\\s+", " ");

        if (text.length() > 120) {
            text = text.substring(0, 120);
            int lastSpace = text.lastIndexOf(" ");
            if (lastSpace > 0) {
                text = text.substring(0, lastSpace);
            }
            text += ".";
        }

        Map<String, String> body = Map.of("text", text);

        long startTime = System.currentTimeMillis();

        System.out.println("Generating Speech...");

        return webClient
                .post()
                .uri("https://api.deepgram.com/v1/speak?model=aura-2-thalia-en")
                .header(HttpHeaders.AUTHORIZATION, "Token " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_OCTET_STREAM)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(byte[].class)
                .timeout(Duration.ofSeconds(60))
                .doOnSuccess(audio ->
                        System.out.println("Deepgram call took: " +
                                (System.currentTimeMillis() - startTime) + " ms"))
                .doOnError(e -> System.out.println("Deepgram Error: " + e.getMessage()));
    }
}