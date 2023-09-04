package com.midel.generics;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SubGroupPair<L, R> {
    private L firstGroup;
    private R secondGroup;

    public SubGroupPair(L firstGroup, R secondGroup) {
        this.firstGroup = firstGroup;
        this.secondGroup = secondGroup;
    }

    public L getFirstGroup() {
        return firstGroup;
    }

    public void setFirstGroup(L firstGroup) {
        this.firstGroup = firstGroup;
    }

    public R getSecondGroup() {
        return secondGroup;
    }

    public void setSecondGroup(R secondGroup) {
        this.secondGroup = secondGroup;
    }

    @JsonCreator
    public static SubGroupPair<Object, Object> with(
            @JsonProperty("firstGroup") Object firstGroup,
            @JsonProperty("secondGroup") Object secondGroup
    ) { // body does not matter, only signature
        return SubGroupPair.with(firstGroup, secondGroup);
    }

    @Override
    public String toString() {
        return "SubGroupPair{" +
                "firstGroup=" + firstGroup +
                ", secondGroup=" + secondGroup +
                '}';
    }
}
