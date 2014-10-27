package com.rizzo.sarcasmotron.domain.elasticsearch;

import org.apache.commons.lang.builder.ToStringBuilder;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ESComment implements Serializable {

    private static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    private String user;

    private String timestamp;

    private String comment;

    public String getUser() {
        return user;
    }

    public ESComment setUser(String user) {
        this.user = user;
        return this;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public ESComment setTimestamp(String timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public ESComment stamp(Date date) {
        this.timestamp = TIMESTAMP_FORMAT.format(date);
        return this;
    }

    public ESComment stamp() {
        this.timestamp = TIMESTAMP_FORMAT.format(new Date());
        return this;
    }

    public String getComment() {
        return comment;
    }

    public ESComment setComment(String comment) {
        this.comment = comment;
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("user", user)
                .append("timestamp", timestamp)
                .append("comment", comment)
                .toString();
    }
}
