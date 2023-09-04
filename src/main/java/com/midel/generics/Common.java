package com.midel.generics;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Common<C> {
    private C common;

    public Common(C common) {
        this.common = common;
    }

    public C getCommon() {
        return common;
    }

    public void setCommon(C common) {
        this.common = common;
    }

    @Override
    public String toString() {
        return "Common{" +
                common +
                '}';
    }

    @JsonCreator
    public static Common<Object> with(
            @JsonProperty("common") Object common
    ) {// body doesn't matter, only signature
        return Common.with(common);
    }

}
