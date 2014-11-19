package com.rizzo.sarcasmotron.domain.elasticsearch;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.joda.time.DateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Document(indexName = "sarcasmotron", type = "sarcasms", shards = 1, replicas = 0, refreshInterval = "-1")
public class ESSarcasm implements Serializable {

    public static final String TIMESTAMP_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    public static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat(TIMESTAMP_PATTERN);

    @Id
    private String id;

    private String timestamp;

    @Field(type = FieldType.Nested)
    private ESUser user;

    private String creator;

    private String quote;

    private String context;

    @Field(type = FieldType.Nested)
    private Map<String, Integer> votes;

    @Field(type = FieldType.Nested)
    private List<ESComment> comments;

    @Field(type = FieldType.Auto, store=true)
    private Set<String> favorites;

    public String getId() {
        return this.id;
    }

    public ESSarcasm setId(String id) {
        this.id = id;
        return this;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public ESSarcasm setTimestamp(String timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public ESSarcasm stamp(DateTime datetime) {
        this.timestamp = TIMESTAMP_FORMAT.format(datetime.toDate());
        return this;
    }

    public ESSarcasm stamp() {
        this.timestamp = TIMESTAMP_FORMAT.format(new Date());
        return this;
    }

    public ESUser getUser() {
        return this.user;
    }

    public ESSarcasm setUser(ESUser user) {
        this.user = user;
        return this;
    }

    public String getCreator() {
        return creator;
    }

    public ESSarcasm setCreator(String creator) {
        this.creator = creator;
        return this;
    }

    public String getQuote() {
        return this.quote;
    }

    public ESSarcasm setQuote(String quote) {
        this.quote = quote;
        return this;
    }

    public String getContext() {
        return this.context;
    }

    public ESSarcasm setContext(String context) {
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

    public ESSarcasm setVotes(Map<String, Integer> votes) {
        this.votes = votes;
        return this;
    }

    public List<ESComment> getComments() {
        if(this.comments == null) {
            this.comments = Lists.newArrayList();
        }
        return this.comments;
    }

    public ESSarcasm setComments(List<ESComment> comments) {
        this.comments = comments;
        return this;
    }

    public ESSarcasm addComment(ESComment ESComment) {
        getComments().add(ESComment);
        return this;
    }

    public Set<String> getFavorites() {
        return favorites;
    }

    public ESSarcasm setFavorites(Set<String> favorites) {
        this.favorites = favorites;
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
                .append("favorites", favorites)
                .toString();
    }
}
