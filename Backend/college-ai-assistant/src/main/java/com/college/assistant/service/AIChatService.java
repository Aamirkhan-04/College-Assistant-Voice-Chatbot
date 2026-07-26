package com.college.assistant.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import com.college.assistant.memory.ConversationMemory;
import com.college.assistant.memory.MemoryManager;
import com.college.assistant.prompt.CollegePrompt;
import com.college.assistant.validator.CollegeQueryValidator;

@Service
public class AIChatService {

	private final ChatClient chatClient;
	private final MemoryManager memoryManager;
	private final LogService logService;
	private final CollegeKnowledgeService knowledgeService;
	private final CourseTermCorrector courseTermCorrector;

	public AIChatService(ChatClient.Builder builder, MemoryManager memoryManager, LogService logService,
			CollegeKnowledgeService knowledgeService, CourseTermCorrector courseTermCorrector) {

		this.chatClient = builder.build();
		this.memoryManager = memoryManager;
		this.logService = logService;
		this.knowledgeService = knowledgeService;
		this.courseTermCorrector = courseTermCorrector;
	}

	public String askQuestion(String sessionId, String message) {

		try {

			if (message == null || message.isBlank()) {
				return "Please ask a question.";
			}

			ConversationMemory memory = memoryManager.getMemory(sessionId);

			String text = message.toLowerCase().trim();

			text = text.replace(".", "").replace(",", "").replace("!", "").replace("?", "");

			// -----------------------------
			// Instant Replies
			// -----------------------------
			// Exact match ke bajaye flexible matching, kyunki speech-to-text
			// kabhi kabhi extra words/spacing ke saath text deta hai
			// (jaise "hello" ki jagah "hello " ya "um hello")
			int wordCount = text.isBlank() ? 0 : text.split("\\s+").length;

			if (wordCount <= 4) {

				if (text.matches(".*\\b(hi|hello|hey)\\b.*")) {
					return "Hello! I am the Voice Assistant of Shri G.P.M. Degree College of Science and Commerce. How can I help you today?";
				}

				if (text.matches(".*\\bgood morning\\b.*")) {
					return "Good morning! How can I help you today?";
				}

				if (text.matches(".*\\bgood afternoon\\b.*")) {
					return "Good afternoon! How may I assist you today?";
				}

				if (text.matches(".*\\bgood evening\\b.*")) {
					return "Good evening! How can I help you today?";
				}

				if (text.matches(".*\\b(thanks|thank you)\\b.*")) {
					return "You're welcome! Happy to help.";
				}

				if (text.matches(".*\\b(bye|goodbye)\\b.*")) {
					return "Goodbye! Have a wonderful day.";
				}

				if (text.matches("\\b(ok|okay)\\b")) {
					return "Great! Feel free to ask any college-related question.";
				}

				if (text.matches(".*\\b(stop|exit|close)\\b.*")) {
					return "__STOP__";
				}

			}

			// -----------------------------
			// Speech-to-text galtiyon ko fix karo
			// (jaise "PSC" -> "bsc", "BCS" -> "bsc" waghera)
			// -----------------------------
			message = courseTermCorrector.correct(message);

			// -----------------------------
			// Validate Question
			// -----------------------------
			if (!CollegeQueryValidator.isCollegeQuery(message)) {

				return """
						Sorry, I can only answer college-related questions.
						""";
			}

			// -----------------------------
			// Save User Message
			// -----------------------------
			memory.addUserMessage(message);

			String conversation = """
					Previous Conversation:
					%s
					Current User Question:
					%s
					Answer in less than 40 words.
					Use simple English.
					Keep the answer short.
					Reply naturally.
					Do not use bullet points.
					Do not repeat yourself.
					"""
					.formatted(memory.getConversation(), message);

			// -----------------------------
			// System Prompt + Real College Data
			// -----------------------------
			String collegeContext = knowledgeService.getRelevantContext(message);

			String systemPrompt = CollegePrompt.SYSTEM_PROMPT + """

					=========================
					COLLEGE INFORMATION (real data, scraped from official website)
					=========================
					""" + collegeContext;

			// -----------------------------
			// AI Response
			// -----------------------------
			String response = chatClient.prompt().system(systemPrompt).user(conversation).call()
					.content();
			
			response = response
			        .replace("\n", " ")
			        .replaceAll("\\s+", " ")
			        .trim();

			if (response.length() > 180) {

			    response = response.substring(0, 180);

			    int lastSpace = response.lastIndexOf(" ");

			    if (lastSpace > 0) {

			        response = response.substring(0, lastSpace);

			    }

			    response += ".";

			}

			if (response == null || response.isBlank()) {

				response = "Sorry, I couldn't generate a response.";

			}

			response = response.trim();

			// -----------------------------
			// Save AI Response
			// -----------------------------
			memory.addAIMessage(response);

			// -----------------------------
			// Log Conversation
			// -----------------------------
			logService.logConversation(sessionId, message, response);

			return response;

		}

		catch (Exception e) {

			e.printStackTrace();

			logService.logError(sessionId, e);

			return """
					Sorry!

					I'm unable to process your request right now.

					Please try again after a few moments.
					""";
		}

	}

}