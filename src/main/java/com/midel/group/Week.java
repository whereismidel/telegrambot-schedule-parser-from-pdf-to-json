package com.midel.group;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.*;

public class Week {
    @JsonIgnore
    public HashMap<String, Day> days;

    public Week(HashMap<String, Day> days) {
        this.days = days;
    }

    @JsonAnyGetter
    public HashMap<String, Day> getDays() {
        // create a LinkedHashMap to keep the sort order
        LinkedHashMap<String, Day> sortedDays = new LinkedHashMap<>();

        List<String> weekDays = Arrays.asList("monday", "tuesday", "wednesday", "thursday", "friday", "saturday");

        for (String day : weekDays) {
            if (days.containsKey(day)) {
                sortedDays.put(day, days.get(day));
            }
        }

        return sortedDays;
    }

    @JsonCreator
    public static Week fromJson(Map<String, Object> json) {
        HashMap<String, Day> days = new HashMap<>();

        for (Map.Entry<String, Object> entry : json.entrySet()) {
            Day day = new Day((HashMap<String, Object>) entry.getValue());
            days.put(entry.getKey(), day);
        }

        return new Week(days);
    }

    @Override
    public String toString() {
        return "Week{" +
                "days=" + days +
                '}';
    }
}
