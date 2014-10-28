package com.rizzo.sarcasmotron.domain.web;

import com.google.common.collect.Maps;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.util.Map;

public class Trend {

    private Map<String, Double> trendLine;

    private String message;

    public Map<String, Double> getTrendLine() {
        if(this.trendLine == null) {
            this.trendLine = Maps.newTreeMap();
        }
        return trendLine;
    }

    public Trend setTrendLine(Map<String, Double> trendLine) {
        this.trendLine = trendLine;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public Trend setMessage(String message) {
        this.message = message;
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("trendLine", trendLine)
                .append("message", message)
                .toString();
    }
}
