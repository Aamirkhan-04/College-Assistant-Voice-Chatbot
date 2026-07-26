package com.college.assistant.validator;

import java.util.HashSet;
import java.util.Set;

public class CollegeQueryValidator {

    private static final Set<String> KEYWORDS = new HashSet<>();

    static {

        // Admission
        KEYWORDS.add("admission");
        KEYWORDS.add("admissions");
        KEYWORDS.add("apply");
        KEYWORDS.add("application");
        KEYWORDS.add("eligibility");
        KEYWORDS.add("cutoff");

        // Course
        KEYWORDS.add("course");
        KEYWORDS.add("courses");
        KEYWORDS.add("subject");
        KEYWORDS.add("subjects");
        KEYWORDS.add("syllabus");
        KEYWORDS.add("curriculum");

        // Fees
        KEYWORDS.add("fee");
        KEYWORDS.add("fees");
        KEYWORDS.add("payment");
        KEYWORDS.add("scholarship");

        // Exam
        KEYWORDS.add("exam");
        KEYWORDS.add("exams");
        KEYWORDS.add("semester");
        KEYWORDS.add("internal");
        KEYWORDS.add("external");
        KEYWORDS.add("result");
        KEYWORDS.add("marks");
        KEYWORDS.add("grade");
        KEYWORDS.add("cgpa");

        // Timetable
        KEYWORDS.add("timetable");
        KEYWORDS.add("schedule");
        KEYWORDS.add("class");
        KEYWORDS.add("lecture");

        // Attendance
        KEYWORDS.add("attendance");
        KEYWORDS.add("present");
        KEYWORDS.add("absent");

        // Faculty
        KEYWORDS.add("faculty");
        KEYWORDS.add("teacher");
        KEYWORDS.add("professor");
        KEYWORDS.add("hod");
        KEYWORDS.add("principal");

        // Placement
        KEYWORDS.add("placement");
        KEYWORDS.add("placements");
        KEYWORDS.add("internship");
        KEYWORDS.add("job");
        KEYWORDS.add("company");

        // Library
        KEYWORDS.add("library");
        KEYWORDS.add("book");

        // Lab
        KEYWORDS.add("lab");
        KEYWORDS.add("laboratory");

        // Hostel
        KEYWORDS.add("hostel");
        KEYWORDS.add("canteen");
        KEYWORDS.add("bus");
        KEYWORDS.add("transport");

        // Events
        KEYWORDS.add("event");
        KEYWORDS.add("fest");
        KEYWORDS.add("seminar");
        KEYWORDS.add("workshop");
        KEYWORDS.add("sports");
        KEYWORDS.add("club");

        // General
        KEYWORDS.add("college");
        KEYWORDS.add("campus");
        KEYWORDS.add("university");
        KEYWORDS.add("student");
        KEYWORDS.add("department");
        KEYWORDS.add("certificate");
        KEYWORDS.add("id card");
        KEYWORDS.add("academic");
    }

    public static boolean isCollegeQuery(String message) {

        if (message == null || message.isBlank()) {
            return false;
        }

        String text = message.toLowerCase();

        for (String keyword : KEYWORDS) {

            if (text.contains(keyword)) {
                return true;
            }

        }

        return false;
    }

}