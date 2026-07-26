package com.college.assistant.memory;

import java.util.LinkedList;
import java.util.List;

public class ConversationMemory {

    private final List<String> history = new LinkedList<>();

    public void addUserMessage(String message) {

        history.add("User : " + message);

        limitHistory();

    }

    public void addAIMessage(String message) {

        history.add("Assistant : " + message);

        limitHistory();

    }

    public String getConversation() {

        return String.join("\n", history);

    }

    public void clear() {

        history.clear();

    }

    private void limitHistory() {

        while (history.size() > 20) {

            history.remove(0);

        }

    }

}