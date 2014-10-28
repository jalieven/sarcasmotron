package com.rizzo.sarcasmotron.domain.web;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.rizzo.sarcasmotron.domain.calc.VoteStats;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.elasticsearch.common.joda.time.DateTime;
import org.elasticsearch.common.joda.time.ReadablePeriod;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.Map;

@Document(collection = "votestats")
public class Stats {

    @Id
    private String id;

    private String message;

    private Date start;

    private Date end;

    private Map<String, VoteStats> voteStats;

    @JsonIgnore
    public String getId() {
        return id;
    }

    public Stats setId(String id) {
        this.id = id;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public Stats setMessage(String message) {
        this.message = message;
        return this;
    }

    public Date getStart() {
        return start;
    }

    public Stats setStart(Date start) {
        this.start = start;
        return this;
    }

    public Date getEnd() {
        return end;
    }

    public Stats setEnd(Date end) {
        this.end = end;
        return this;
    }

    public Stats setValidity(ReadablePeriod validPeriod) {
        DateTime end = DateTime.now();
        final DateTime start = end.minus(validPeriod);
        setEnd(end.toDate());
        setStart(start.toDate());
        return this;
    }

    public Map<String, VoteStats> getVoteStats() {
        if(this.voteStats == null) {
            this.voteStats = Maps.newTreeMap();
        }
        return voteStats;
    }

    public Stats setVoteStats(Map<String, VoteStats> voteStats) {
        this.voteStats = voteStats;
        return this;
    }

    @JsonIgnore
    public Stats addVoteStats(String user, VoteStats voteStats) {
        getVoteStats().put(user, voteStats);
        return this;
    }

    @JsonIgnore
    public void sort() {
        Ordering<Map.Entry<String, VoteStats>> entryOrdering = Ordering.natural()
                .onResultOf(new Function<Map.Entry<String, VoteStats>, VoteStats>() {
                    public VoteStats apply(Map.Entry<String, VoteStats> entry) {
                        return entry.getValue();
                    }
                }).reverse();
        ImmutableMap.Builder<String, VoteStats> builder = ImmutableMap.builder();
        for (Map.Entry<String, VoteStats> entry :
                entryOrdering.sortedCopy(this.voteStats.entrySet())) {
            builder.put(entry.getKey(), entry.getValue());
        }
        this.voteStats = builder.build();
    }


    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("voteStats", voteStats)
                .append("id", id)
                .append("message", message)
                .append("start", start)
                .append("end", end)
                .toString();
    }


}
