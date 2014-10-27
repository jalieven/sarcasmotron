package com.rizzo.sarcasmotron.domain.web;

import org.apache.commons.lang.builder.ToStringBuilder;

public class Vote {

    private boolean cast;

    private String message;

    public boolean isCast() {
        return cast;
    }

    public Vote setCast(boolean cast) {
        this.cast = cast;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public Vote setMessage(String message) {
        this.message = message;
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("cast", cast)
                .append("message", message)
                .toString();
    }
}
