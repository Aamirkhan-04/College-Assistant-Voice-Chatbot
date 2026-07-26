package com.college.assistant.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;

@Service
public class LogService {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void logConversation(String sessionId,
                                String user,
                                String ai) {

        System.out.println();

        System.out.println("======================================================");

        System.out.println("Time      : "
                + LocalDateTime.now().format(FORMATTER));

        System.out.println("Session   : " + sessionId);

        System.out.println();

        System.out.println("User");

        System.out.println(user);

        System.out.println();

        System.out.println("Assistant");

        System.out.println(ai);

        System.out.println();

        System.out.println("======================================================");

    }

    public void logError(String sessionId,
                         Exception e) {

        System.out.println();

        System.out.println("**************** ERROR ****************");

        System.out.println("Time      : "
                + LocalDateTime.now().format(FORMATTER));

        System.out.println("Session   : " + sessionId);

        System.out.println("Message   : "
                + e.getMessage());

        System.out.println("***************************************");

        System.out.println();

    }

}