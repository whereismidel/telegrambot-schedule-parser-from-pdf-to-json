package com.midel.group;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.midel.generics.Common;
import com.midel.generics.SubGroupPair;

import java.util.HashMap;
import java.util.Map;

public class Day {

    @JsonIgnore
    public HashMap<String, Object> lessons;

    public Day(HashMap<String, Object> lessons) {
        this.lessons = lessons;
    }

    @Override
    public String toString() {
        return "Day{" +
                "lessons1=" + lessons +
                "}";
    }

    @JsonAnyGetter
    public HashMap<String, Object> getLessons() {
        HashMap<String, Object> filteredLessons = new HashMap<>();
        for (String key : lessons.keySet()) {
            if (key.matches("[1-6]")) {
                filteredLessons.put(key, lessons.get(key));
            }
        }
        return filteredLessons;
    }

    @JsonCreator
    public static Day fromJson(Map<String, Object> lessons) {
        //HashMap<String, Object> lessons = new HashMap<>();

        System.out.println();
        for (Map.Entry<String, Object> entry : lessons.entrySet()) {
            //System.out.println(entry);
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Map) {
                Map<?, ?> mapValue = (Map<?, ?>) value;
                Subj firstGroup = new Subj(
                        (String) mapValue.get("subject"),
                        (String) mapValue.get("type"),
                        (String) mapValue.get("lector"),
                        (String) mapValue.get("auditory")
                );
                Subj secondGroup = new Subj(
                        (String) mapValue.get("subject"),
                        (String) mapValue.get("type"),
                        (String) mapValue.get("lector"),
                        (String) mapValue.get("auditory")
                );

                lessons.put(key, new SubGroupPair<>(firstGroup, secondGroup));
            } else if (value instanceof Subj) {
                lessons.put(key, new Common<>((Subj) value));
            } else {
                lessons.put(key, null);
            }
        }

        return new Day((HashMap<String, Object>) lessons);
    }
}
