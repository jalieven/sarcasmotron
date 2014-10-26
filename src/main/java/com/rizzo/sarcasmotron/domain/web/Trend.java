package com.rizzo.sarcasmotron.domain.web;

import com.google.common.collect.Maps;

import java.util.Map;

public class Trend {

    private Map<String, Double> trendLine;

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
}
