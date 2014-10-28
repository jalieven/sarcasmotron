package com.rizzo.sarcasmotron.domain.mongodb;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.joda.time.DateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class Sarcasm implements Serializable {

    public static final String TIMESTAMP_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    public static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat(TIMESTAMP_PATTERN);

    @Id
    private String id;

    private String timestamp;

    private String user;

    private String creator;

    private String quote;

    private String context;

    @JsonIgnore
    @Field(type = FieldType.Nested)
    private Map<String, Integer> votes;

    @Field(type = FieldType.Nested)
    private List<Comment> comments;

    public String getId() {
        return this.id;
    }

    public Sarcasm setId(String id) {
        this.id = id;
        return this;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public Sarcasm stamp(DateTime datetime) {
        this.timestamp = TIMESTAMP_FORMAT.format(datetime.toDate());
        return this;
    }

    public Sarcasm stamp() {
        this.timestamp = TIMESTAMP_FORMAT.format(new Date());
        return this;
    }

    public String getUser() {
        return this.user;
    }

    public Sarcasm setUser(String user) {
        this.user = user;
        return this;
    }

    public String getCreator() {
        return creator;
    }

    public Sarcasm setCreator(String creator) {
        this.creator = creator;
        return this;
    }

    public String getQuote() {
        return this.quote;
    }

    public Sarcasm setQuote(String quote) {
        this.quote = quote;
        return this;
    }

    public String getContext() {
        return this.context;
    }

    public Sarcasm setContext(String context) {
        this.context = context;
        return this;
    }

    public Integer getVoteTotal() {
        Integer total = 0;
        for (Map.Entry<String, Integer> userVoteEntry : getVotes().entrySet()) {
            total += userVoteEntry.getValue();
        }
        return total;
    }

    public boolean upVote(String user) {
        if(!getVotes().containsKey(user)) {
            getVotes().put(user, 1);
            return true;
        } else {
            return false;
        }

    }

    public boolean downVote(String user) {
        if(!getVotes().containsKey(user)) {
            getVotes().put(user, -1);
            return true;
        } else {
            return false;
        }
    }

    public Map<String, Integer> getVotes() {
        if(this.votes == null) {
            this.votes = Maps.newHashMap();
        }
        return votes;
    }

    public Sarcasm setVotes(Map<String, Integer> votes) {
        this.votes = votes;
        return this;
    }

    public List<Comment> getComments() {
        if(this.comments == null) {
            this.comments = Lists.newArrayList();
        }
        return this.comments;
    }

    public Sarcasm setComments(List<Comment> comments) {
        this.comments = comments;
        return this;
    }

    public Sarcasm addComment(Comment comment) {
        getComments().add(comment);
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("id", id)
                .append("timestamp", timestamp)
                .append("user", user)
                .append("creator", creator)
                .append("quote", quote)
                .append("context", context)
                .append("votes", votes)
                .append("comments", comments)
                .toString();
    }
}
