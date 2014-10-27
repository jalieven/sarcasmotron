package com.rizzo.sarcasmotron.domain.web;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.rizzo.sarcasmotron.domain.calc.VoteStats;

import java.util.Collections;
import java.util.List;
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
    public List<Map.Entry<String, VoteStats>> sortedVoteStats() {
        List<Map.Entry<String, VoteStats>> sortedStats = Lists.newArrayList(getVoteStats().entrySet());
        Ordering<Map.Entry<String, VoteStats>> byMapValues = new Ordering<Map.Entry<String, VoteStats>>() {
            @Override
            public int compare(Map.Entry<String, VoteStats> left, Map.Entry<String, VoteStats> right) {
                final int sumCompare = right.getValue().getSum().compareTo(left.getValue().getSum());
                final int countCompare = right.getValue().getCount().compareTo(left.getValue().getCount());
                return (sumCompare != 0) ? sumCompare : countCompare;
            }
        };
        Collections.sort(sortedStats, byMapValues);
        return sortedStats;
    }
}
