package com.college.assistant.memory;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
public class MemoryManager {

    private final ConcurrentHashMap<String, ConversationMemory> memories =
            new ConcurrentHashMap<>();

    public ConversationMemory getMemory(String sessionId) {

        return memories.computeIfAbsent(
                sessionId,
                id -> new ConversationMemory()
        );
    }

    public void removeMemory(String sessionId) {

        memories.remove(sessionId);

    }

    public void clearAll() {

        memories.clear();

    }

}