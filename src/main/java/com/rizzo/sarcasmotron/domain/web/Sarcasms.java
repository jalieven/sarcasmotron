package com.rizzo.sarcasmotron.domain.web;

import com.rizzo.sarcasmotron.domain.mongodb.Sarcasm;

import java.util.List;

public class Sarcasms {

    private List<Sarcasm> sarcasms;

    private Long total;

    private Integer pages;

    public List<Sarcasm> getSarcasms() {
        return sarcasms;
    }

    public Sarcasms setSarcasms(List<Sarcasm> sarcasms) {
        this.sarcasms = sarcasms;
        return this;
    }

    public Long getTotal() {
        return total;
    }

    public Sarcasms setTotal(Long total) {
        this.total = total;
        return this;
    }

    public Integer getPages() {
        return pages;
    }

    public Sarcasms setPages(Integer pages) {
        this.pages = pages;
        return this;
    }
}
