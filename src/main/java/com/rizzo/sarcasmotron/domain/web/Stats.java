package com.rizzo.sarcasmotron.domain.web;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.rizzo.sarcasmotron.domain.calc.VoteStats;

import java.util.Map;

public class Stats {

    private Map<String, VoteStats> voteStats;

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
}
