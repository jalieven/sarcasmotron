package com.rizzo.sarcasmotron.domain.web;

import org.apache.commons.lang.builder.ToStringBuilder;

public class VoteRequest {

    private String sarcasmId;

    public String getSarcasmId() {
        return sarcasmId;
    }

    public VoteRequest setSarcasmId(String sarcasmId) {
        this.sarcasmId = sarcasmId;
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("sarcasmId", sarcasmId)
                .toString();
    }
}
