package com.midel.group;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.LinkedHashMap;

public class Group {
    public String group;
    public String spec;
    public LinkedHashMap<String, Week> weeks;

    public static HashMap<String, Group> groups = new HashMap<>();

    @JsonCreator
    public Group(@JsonProperty("group") String group,
                 @JsonProperty("spec") String spec,
                 @JsonProperty("weeks") LinkedHashMap<String, Week> weeks) {
        this.group = group;
        this.spec = spec;
        this.weeks = weeks;
    }

    @Override
    public String toString() {
        return "Group{" +
                "\n\tname=" + group +
                ",\n\tspec=" + spec +
                ",\n\tweeks={\n\t\t" + weeks + "\n}" +
                "}";
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getSpec() {
        return spec;
    }

    public void setSpec(String spec) {
        this.spec = spec;
    }

    public HashMap<String, Week> getWeeks() {
        return weeks;
    }

    public void setWeeks(LinkedHashMap<String, Week> weeks) {
        this.weeks = weeks;
    }

    public static HashMap<String, Group> getGroups() {
        return groups;
    }

    public static void setGroups(HashMap<String, Group> groups) {
        Group.groups = groups;
    }
}