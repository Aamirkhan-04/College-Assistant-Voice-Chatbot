package com.college.assistant.service;

import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Speech-to-text kabhi kabhi college ke course names/terms galat sunta hai
 * (jaise "BSc" ko "PSC" ya "BCS" samajh leta hai). Ye service aise words ko
 * detect karke, unke sabse "milte-julte" (spelling ke hisaab se) known term
 * se replace kar deta hai — taaki keyword-matching aur AI dono sahi samjhein.
 *
 * IMPORTANT: KNOWN_TERMS list mein apne college ke ASLI courses/terms daalo.
 */
@Service
public class CourseTermCorrector {

    // Apne college ke actual course-abbreviations/important terms yahan
    // add/remove karo (sirf 3+ letter wale terms rakho — 2-letter jaise
    // "it"/"cs" ko fuzzy-match karna risky hai, normal words se collide
    // ho sakta hai)
    private static final List<String> KNOWN_TERMS = List.of(
            "bcom", "baf", "bbi", "bms", "bfm", "bca", "mmc",
            "bsc", "bscit", "bsccs",
            "commerce", "science", "arts", "course", "courses",
            "admission", "admissions", "fees", "hostel", "library",
            "placement", "placements", "prospectus", "syllabus",
            "semester", "faculty", "department", "departments",
            "exam", "exams", "examination", "result", "results",
            "scholarship", "scholarships", "internship", "internships",
            "timetable", "attendance", "canteen", "laboratory",
            "laboratories", "sports", "events", "alumni", "iqac",
            "naac", "committee", "committees", "notice", "notices"
    );

    // Ye common English words hain — inhe kabhi bhi correct/replace nahi
    // karna (warna "is" jaisa word galti se "it" ban sakta hai)
    private static final List<String> SKIP_WORDS = List.of(
            "the", "and", "for", "are", "was", "has", "you", "your",
            "how", "what", "when", "does", "can", "not", "yes", "college"
    );

    // Alag-alag tareeke se bola gaya matlab ek hi ho sakta hai — inhe
    // college-data ke actual vocabulary se jodne ke liye synonym map.
    // "User jo bhi bole, uska MATLAB samajhna" — isi ke liye ye zaroori hai.
    private static final Map<String, List<String>> SYNONYMS = new LinkedHashMap<>();

    static {
        SYNONYMS.put("fees", List.of("cost", "costs", "charge", "charges", "price", "payment", "fee"));
        SYNONYMS.put("admission", List.of("admit", "enrol", "enroll", "enrollment", "join", "apply",
                "application", "eligibility", "eligible"));
        SYNONYMS.put("course", List.of("courses", "branch", "branches", "stream", "streams",
                "program", "programme", "programs", "subject", "subjects", "degree", "degrees"));
        SYNONYMS.put("faculty", List.of("teacher", "teachers", "professor", "professors", "staff", "lecturer", "lecturers"));
        SYNONYMS.put("hostel", List.of("accommodation", "stay", "residence", "dormitory"));
        SYNONYMS.put("placement", List.of("placements", "job", "jobs", "career", "careers", "recruitment", "recruiter"));
        SYNONYMS.put("library", List.of("books", "reading"));
        SYNONYMS.put("contact", List.of("phone", "number", "email", "address", "location", "reach"));
        SYNONYMS.put("timetable", List.of("schedule", "timing", "timings"));
        SYNONYMS.put("scholarship", List.of("scholarships", "financial aid", "funding"));
    }

    /**
     * Ye method message ko badalta NAHI hai (LLM ko wahi natural text milta
     * hai jo user ne bola) — bas EXTRA search-keywords return karta hai jo
     * knowledge-search ke liye use honge. Isse "cost" bolne par bhi "fees"
     * wala content mil jaata hai, "branch" bolne par "course" wala content
     * mil jaata hai, waghera.
     */
    public String expandForSearch(String message) {

        if (message == null || message.isBlank()) {
            return message;
        }

        String lower = message.toLowerCase();
        StringBuilder extra = new StringBuilder();

        for (Map.Entry<String, List<String>> entry : SYNONYMS.entrySet()) {

            String canonical = entry.getKey();

            for (String alias : entry.getValue()) {

                if (lower.contains(alias)) {
                    extra.append(" ").append(canonical);
                    break;
                }

            }

        }

        return message + extra;

    }

    public String correct(String message) {

        if (message == null || message.isBlank()) {
            return message;
        }

        String[] words = message.split("\\s+");
        StringBuilder corrected = new StringBuilder();

        for (String word : words) {

            String cleanWord = word.toLowerCase().replaceAll("[^a-z0-9]", "");

            String bestMatch = findClosestMatch(cleanWord);

            if (bestMatch != null) {
                corrected.append(bestMatch).append(" ");
            } else {
                corrected.append(word).append(" ");
            }

        }

        return corrected.toString().trim();

    }

    private String findClosestMatch(String word) {

        // Bahut chote words ya common English words ko touch mat karo
        if (word.length() < 3 || SKIP_WORDS.contains(word)) {
            return null;
        }

        // Agar word already exactly ek known term hai, kuch karne ki zaroorat nahi
        if (KNOWN_TERMS.contains(word)) {
            return null;
        }

        String best = null;
        int bestDistance = Integer.MAX_VALUE;

        for (String term : KNOWN_TERMS) {

            int distance = levenshtein(word, term);

            // Chote terms ke liye kam tolerance (1 letter galti), lambe
            // terms ke liye thoda zyada tolerance (2 letter galti)
            int threshold = term.length() <= 4 ? 1 : 2;

            if (distance <= threshold && distance < bestDistance) {
                bestDistance = distance;
                best = term;
            }

        }

        return best;

    }

    private int levenshtein(String a, String b) {

        int[][] dp = new int[a.length() + 1][b.length() + 1];

        for (int i = 0; i <= a.length(); i++) {

            for (int j = 0; j <= b.length(); j++) {

                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    dp[i][j] = Math.min(
                            dp[i - 1][j - 1] + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1),
                            Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1)
                    );
                }

            }

        }

        return dp[a.length()][b.length()];

    }

}