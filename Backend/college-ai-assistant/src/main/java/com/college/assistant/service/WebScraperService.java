package com.college.assistant.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.Connection;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Shri G.P.M. Degree College of Science and Commerce ki website
 * ke pages (HTML) aur documents (PDF - Prospectus, Placements)
 * dono scrape karke unka text content nikaalta hai.
 */
@Service
public class WebScraperService {

    // Normal HTML pages
    private static final Map<String, String> PAGES_TO_SCRAPE = Map.ofEntries(
            Map.entry("home", "https://shrigpmcollegeandheri.org/"),
            Map.entry("about", "https://shrigpmcollegeandheri.org/about-us/"),
            Map.entry("academics", "https://shrigpmcollegeandheri.org/academics/"),
            Map.entry("life_at_college", "https://shrigpmcollegeandheri.org/life-sgdc/"),
            Map.entry("student_support", "https://shrigpmcollegeandheri.org/student-support/"),
            Map.entry("notices", "https://shrigpmcollegeandheri.org/notices/"),
            Map.entry("committees", "https://shrigpmcollegeandheri.org/committees-associations/"),
            Map.entry("library", "https://shrigpmcollegeandheri.org/library/"),
            Map.entry("admission", "https://shrigpmcollegeandheri.org/admission/"),
            Map.entry("alumni", "https://shrigpmcollegeandheri.org/alumni/"),
            Map.entry("iqac", "https://shrigpmcollegeandheri.org/iqac/"),
            Map.entry("naac", "https://shrigpmcollegeandheri.org/naac/"),
            Map.entry("careers", "https://shrigpmcollegeandheri.org/rti/")
    );

    // PDF documents (fees, courses, placement details usually yahin hoti hain)
    private static final Map<String, String> PDFS_TO_SCRAPE = Map.ofEntries(
            Map.entry("prospectus",
                    "https://shrigpmcollegeandheri.org/wp-content/uploads/2026/05/Degree-College-Prospectus-2026-27-21-pgs-ok.pdf"),
            Map.entry("placements_info",
                    "https://shrigpmcollegeandheri.org/wp-content/uploads/2025/03/Training-and-Placement-Cell-Head-2024-25-updated.pdf")
    );

    public Map<String, String> scrapeAll() {

        Map<String, String> result = new LinkedHashMap<>();

        // HTML pages
        for (Map.Entry<String, String> entry : PAGES_TO_SCRAPE.entrySet()) {

            String section = entry.getKey();
            String url = entry.getValue();

            try {
                String text = scrapePage(url);
                result.put(section, text);
                System.out.println("Scraped page: " + section + " (" + text.length() + " chars)");
            } catch (IOException e) {
                System.out.println("Failed to scrape page " + section + ": " + e.getMessage());
                result.put(section, "");
            }

        }

        // PDF documents
        for (Map.Entry<String, String> entry : PDFS_TO_SCRAPE.entrySet()) {

            String section = entry.getKey();
            String url = entry.getValue();

            try {
                String text = scrapePdf(url);
                result.put(section, text);
                System.out.println("Scraped PDF: " + section + " (" + text.length() + " chars)");
            } catch (IOException e) {
                System.out.println("Failed to scrape PDF " + section + ": " + e.getMessage());
                result.put(section, "");
            }

        }

        return result;

    }

    private String scrapePage(String url) throws IOException {

        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0")
                .timeout(15000)
                .get();

        doc.select("nav, footer, script, style, header").remove();

        String text = doc.select("body").text();
        text = text.replaceAll("\\s+", " ").trim();

        return text;

    }

    private String scrapePdf(String url) throws IOException {

        // Jsoup se hi PDF ke raw bytes download karte hain
        // (ignoreContentType zaroori hai kyunki response HTML nahi hai)
        Connection.Response response = Jsoup.connect(url)
                .userAgent("Mozilla/5.0")
                .timeout(30000)
                .ignoreContentType(true)
                .maxBodySize(0)
                .execute();

        byte[] pdfBytes = response.bodyAsBytes();

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {

            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            return text.replaceAll("\\s+", " ").trim();

        }

    }

}