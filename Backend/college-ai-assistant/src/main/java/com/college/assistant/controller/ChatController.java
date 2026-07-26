package com.college.assistant.controller;

import org.springframework.web.bind.annotation.*;

import com.college.assistant.service.AIChatService;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
public class ChatController {

    private final AIChatService aiChatService;

    public ChatController(AIChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    @GetMapping("/chat")
    public String chat(

            @RequestParam String sessionId,

            @RequestParam String message

    ) {

        return aiChatService.askQuestion(

                sessionId,

                message

        );

    }

}