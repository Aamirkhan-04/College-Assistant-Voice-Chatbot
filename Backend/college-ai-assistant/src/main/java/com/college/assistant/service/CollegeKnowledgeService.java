package com.college.assistant.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * College ki website se scrape kiya hua data memory mein rakhta hai
 * aur AIChatService ko provide karta hai.
 *
 * IMPORTANT: Poora scraped data (website pages + Prospectus + Placements PDF)
 * itna bada hai ki Groq ke free-tier token limit (6000 TPM) se zyada ho
 * jaata hai agar har request mein sab kuch bhej diya jaaye. Isliye
 * getRelevantContext() sirf user ke sawal se match hone wala hissa
 * chunk karke nikalta hai — poora data nahi bhejta.
 */
@Service
public class CollegeKnowledgeService {

    private static final String CACHE_FILE = "college-knowledge.json";

    // Chote/single-page sections — inhe pura (thoda truncate karke) bhej sakte hain
    private static final Set<String> SHORT_SECTIONS = Set.of(
            "home", "about", "academics", "life_at_college", "student_support",
            "notices", "committees", "library", "admission", "alumni",
            "iqac", "naac", "careers"
    );

    // Bade documents — inko chunk karke sirf relevant part bhejenge
    private static final Set<String> LARGE_SECTIONS = Set.of(
            "prospectus", "placements_info"
    );

    private static final int MAX_CHARS_PER_SHORT_SECTION = 600;
    private static final int CHUNK_SIZE = 500;          // characters per chunk
    private static final int MAX_CHUNKS_PER_LARGE_DOC = 4;
    private static final int MAX_TOTAL_CONTEXT_CHARS = 5000; // safety cap (~1200-1500 tokens)

    private final WebScraperService scraperService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private Map<String, String> knowledge = new LinkedHashMap<>();

    public CollegeKnowledgeService(WebScraperService scraperService) {
        this.scraperService = scraperService;
    }

    @SuppressWarnings("unchecked")
	@PostConstruct
    public void init() {

        File cacheFile = new File(CACHE_FILE);

        if (cacheFile.exists()) {
            try {
                knowledge = objectMapper.readValue(cacheFile, Map.class);
                System.out.println("College knowledge loaded from cache.");
                return;
            } catch (IOException e) {
                System.out.println("Cache read failed, re-scraping: " + e.getMessage());
            }
        }

        refreshKnowledge();

    }

    public void refreshKnowledge() {

        System.out.println("Scraping college website...");

        knowledge = scraperService.scrapeAll();

        try {
            objectMapper.writeValue(new File(CACHE_FILE), knowledge);
            System.out.println("College knowledge cached to " + CACHE_FILE);
        } catch (IOException e) {
            System.out.println("Failed to cache knowledge: " + e.getMessage());
        }

    }

    /**
     * User ke sawal ke hisaab se sirf RELEVANT college data return karta hai
     * (poora data nahi) — taaki LLM ke token limit mein aasani se fit ho jaaye.
     */
    public String getRelevantContext(String userQuery) {

        StringBuilder sb = new StringBuilder();

        // 1) Chote sections — har ek ko thoda truncate karke daalo
        for (String section : SHORT_SECTIONS) {

            String content = knowledge.get(section);

            if (content == null || content.isBlank()) {
                continue;
            }

            String truncated = content.length() > MAX_CHARS_PER_SHORT_SECTION
                    ? content.substring(0, MAX_CHARS_PER_SHORT_SECTION)
                    : content;

            sb.append("[").append(section.toUpperCase()).append("]\n");
            sb.append(truncated).append("\n\n");

        }

        // 2) Bade documents (Prospectus, Placements) — sirf query se match
        //    karne wale chunks nikalo
        Set<String> queryKeywords = extractKeywords(userQuery);

        for (String section : LARGE_SECTIONS) {

            String content = knowledge.get(section);

            if (content == null || content.isBlank()) {
                continue;
            }

            List<String> relevantChunks = findRelevantChunks(content, queryKeywords, MAX_CHUNKS_PER_LARGE_DOC);

            if (!relevantChunks.isEmpty()) {
                sb.append("[").append(section.toUpperCase()).append(" - RELEVANT EXCERPTS]\n");
                for (String chunk : relevantChunks) {
                    sb.append(chunk).append("\n");
                }
                sb.append("\n");
            }

        }

        String result = sb.toString();

        // Safety cap — kabhi bhi hard limit se zyada na jaaye
        if (result.length() > MAX_TOTAL_CONTEXT_CHARS) {
            result = result.substring(0, MAX_TOTAL_CONTEXT_CHARS);
        }

        return result;

    }

    private Set<String> extractKeywords(String query) {

        if (query == null) {
            return Set.of();
        }

        // Periods hata do BINA space daale, taaki "B.Sc" -> "BSc" ban jaaye
        // (space se replace karne par "B.Sc" -> "b sc" ban jaata, aur "bsc"
        // keyword kabhi match nahi karta)
        String cleaned = query.toLowerCase().replace(".", "");

        String[] words = cleaned.replaceAll("[^a-z0-9\\s]", " ").split("\\s+");

        Set<String> stopWords = Set.of(
                "the", "is", "are", "a", "an", "of", "for", "in", "on", "to",
                "and", "how", "many", "what", "which", "your", "college",
                "does", "do", "can", "you", "tell", "me", "about"
        );

        Set<String> keywords = new HashSet<>();

        for (String w : words) {
            // 2-letter words bhi allow karo (jaise "it", "cs", "ai" — ye
            // course abbreviations ho sakte hain, bahut short filter mat karo)
            if (w.length() >= 2 && !stopWords.contains(w)) {
                keywords.add(w);
            }
        }

        return keywords;

    }

    private List<String> findRelevantChunks(String text, Set<String> keywords, int maxChunks) {

        List<String> chunks = new ArrayList<>();

        for (int i = 0; i < text.length(); i += CHUNK_SIZE) {
            chunks.add(text.substring(i, Math.min(i + CHUNK_SIZE, text.length())));
        }

        // Har chunk ko score karo — kitne keywords match hue
        List<Map.Entry<String, Integer>> scored = new ArrayList<>();

        for (String chunk : chunks) {

            // Yahan bhi periods hata ke match karo (chunk mein "B.Sc." ho
            // sakta hai, jo "bsc" keyword se match hona chahiye)
            String normalizedChunk = chunk.toLowerCase().replace(".", "");
            int score = 0;

            for (String kw : keywords) {
                if (normalizedChunk.contains(kw)) {
                    score++;
                }
            }

            if (score > 0) {
                scored.add(Map.entry(chunk, score));
            }

        }

        scored.sort((a, b) -> b.getValue() - a.getValue());

        List<String> result = new ArrayList<>();

        for (int i = 0; i < Math.min(maxChunks, scored.size()); i++) {
            result.add(scored.get(i).getKey());
        }

        return result;

    }

}