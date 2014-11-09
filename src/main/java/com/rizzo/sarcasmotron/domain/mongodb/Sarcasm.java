package com.rizzo.sarcasmotron.domain.mongodb;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.joda.time.DateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    @Transient
    private boolean favorite;

    @Transient
    private boolean votedUp;

    @Transient
    private boolean votedDown;

    @JsonIgnore
    private boolean edited;

    @JsonIgnore
    @Field(type = FieldType.Nested)
    private Map<String, Integer> votes;

    @JsonIgnore
    @Field(type = FieldType.Nested)
    private Set<String> favorites;

    @JsonIgnore
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

    public boolean isEdited() {
        return edited;
    }

    public Sarcasm setEdited(boolean edited) {
        this.edited = edited;
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

    public Set<String> getFavorites() {
        if(this.favorites == null) {
            this.favorites = Sets.newHashSet();
        }
        return favorites;
    }

    public Sarcasm setFavorites(Set<String> favorites) {
        this.favorites = favorites;
        return this;
    }

    public boolean getFavorite() {
        return favorite;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    public Sarcasm toggleFavorite(String user) {
        if(getFavorites().contains(user)) {
            getFavorites().remove(user);
        } else {
            getFavorites().add(user);
        }
        this.checkState(user);
        return this;
    }

    public Sarcasm checkState(String user) {
        this.favorite = getFavorites().contains(user);
        if(getVotes().containsKey(user)){
            final Integer vote = getVotes().get(user);
            if(vote == 1) {
                this.setVotedUp(true);
            } else if(vote == -1) {
                this.setVotedDown(true);
            }
        }
        return this;
    }

    public boolean isVotedUp() {
        return votedUp;
    }

    public void setVotedUp(boolean votedUp) {
        this.votedUp = votedUp;
    }

    public boolean isVotedDown() {
        return votedDown;
    }

    public void setVotedDown(boolean votedDown) {
        this.votedDown = votedDown;
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
                .append("edited", edited)
                .append("votes", votes)
                .append("favorites", favorites)
                .append("comments", comments)
                .toString();
    }
}
