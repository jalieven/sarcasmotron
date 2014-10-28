package com.rizzo.sarcasmotron.domain.mongodb;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.joda.time.DateTime;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Comment implements Serializable {

    private static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    private String user;

    private String timestamp;

    private String comment;

    public String getUser() {
        return user;
    }

    public Comment setUser(String user) {
        this.user = user;
        return this;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public Comment stamp(DateTime date) {
        this.timestamp = TIMESTAMP_FORMAT.format(date.toDate());
        return this;
    }

    public Comment stamp() {
        this.timestamp = TIMESTAMP_FORMAT.format(new Date());
        return this;
    }

    public String getComment() {
        return comment;
    }

    public Comment setComment(String comment) {
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
