package com.college.assistant.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.college.assistant.service.DeepgramService;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/tts")
@CrossOrigin(origins = "http://localhost:5173")
public class TTSController {

    private final DeepgramService deepgramService;

    public TTSController(DeepgramService deepgramService) {
        this.deepgramService = deepgramService;
    }

    // GET so that <audio src="..."> can call it directly
    @GetMapping
    public Mono<ResponseEntity<byte[]>> speak(@RequestParam("text") String text) {

        if (text == null || text.isBlank()) {
            return Mono.just(ResponseEntity.badRequest().build());
        }

        long controllerStart = System.currentTimeMillis();
        System.out.println("TTS request received at: " + controllerStart);

        return deepgramService.textToSpeech(text)
                .doOnSuccess(r -> System.out.println("Total controller time: " +
                        (System.currentTimeMillis() - controllerStart) + " ms"))
                .map(audio -> ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=speech.mp3")
                        .contentType(MediaType.valueOf("audio/mpeg"))
                        .contentLength(audio.length)
                        .body(audio))
                .onErrorResume(e -> {
                    e.printStackTrace();
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }
}