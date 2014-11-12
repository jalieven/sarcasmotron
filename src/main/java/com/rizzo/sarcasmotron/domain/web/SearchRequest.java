package com.rizzo.sarcasmotron.domain.web;

import com.google.common.collect.Maps;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.util.Map;

public class SearchRequest {

    private String query;

    private Map<String, Float> boostMap;

    public String getQuery() {
        return query;
    }

    public SearchRequest setQuery(String query) {
        this.query = query;
        return this;
    }

    public Map<String, Float> getBoostMap() {
        if(this.boostMap == null) {
            this.boostMap = Maps.newHashMap();
        }
        return boostMap;
    }

    public SearchRequest setBoostMap(Map<String, Float> boostMap) {
        this.boostMap = boostMap;
        return this;
    }

    public SearchRequest addBoost(String field, Float boost) {
        getBoostMap().put(field, boost);
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("query", query)
                .append("boostMap", boostMap)
                .toString();
    }
}
