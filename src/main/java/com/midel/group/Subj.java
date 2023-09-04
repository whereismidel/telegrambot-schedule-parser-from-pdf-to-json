package com.midel.group;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Objects;

public class Subj {
    public String subject;
    public String type;
    public String lector;
    public String auditory;

    @JsonIgnore
    public String subGroup;

    public static final String[] roles = {"доцент", "асистент", "професор", "старший викл", "старший вик", "старший ви", "старший в", "викладач", "викл", "завідувач ка", "завідувач каф", "завідувач кафе", "завідувач", "проректор", "декан"};
    public static final String[] types = {"Лекція", "Практичне", "Лабораторна"};

    public Subj(String subject, String type, String lector, String auditory) {
        this.subject = subject;
        this.type = type;
        this.lector = lector;
        this.auditory = auditory;
    }

    public Subj(LinkedHashMap<String, String> e) {
        String[] words = e.get("subject").split("\\s+");
        String subject = String.join(" ", Arrays.copyOfRange(words, 0, Math.min(4, words.length)));
        if (!subject.equals(e.get("subject"))) {
            subject += "..";
        }

        this.subject = subject;
        this.type = e.get("type");
        this.lector = e.get("lector");
        this.auditory = e.get("auditory");
    }

    @Override
    public String toString() {
        return "Subj{" +
                "subject='" + subject + '\'' +
                ", type='" + type + '\'' +
                ", lector='" + lector + '\'' +
                ", auditory='" + auditory + '\'' +
                '}';
    }

    public LinkedHashMap<String, String> toHashMap() {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();

        map.put("subject", subject);
        map.put("type", type);
        map.put("lector", lector);
        map.put("auditory", auditory);

        return map;
    }

    @Override
    public int hashCode() {
        return Objects.hash(subject, type, lector);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Subj lesson = (Subj) obj;

        return Objects.equals(subject, lesson.subject) &&
                Objects.equals(type, lesson.type) &&
                Objects.equals(lector, lesson.lector);
    }
}