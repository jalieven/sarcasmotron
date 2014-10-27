package com.rizzo.sarcasmotron.task;

import com.rizzo.sarcasmotron.calc.VoteCalculator;
import com.rizzo.sarcasmotron.domain.calc.VoteStats;
import com.rizzo.sarcasmotron.domain.web.Stats;
import com.rizzo.sarcasmotron.domain.web.StatsRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

public class ScheduledTasks {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledTasks.class);

    private String winnerPeriod;

    @Autowired
    private VoteCalculator voteCalculator;

    public ScheduledTasks(String winnerPeriod) {
        this.winnerPeriod = winnerPeriod;
    }

    public void calculateWinner() {
        LOGGER.info("calculateWinner");
        final List<String> users = voteCalculator.getDistinctUsers();
        Stats stats = new Stats();
        for (String user : users) {
            final VoteStats voteStats = voteCalculator.calculateVoteStatsForUser(user,
                    new StatsRequest().setPeriodExpression(winnerPeriod).getPeriod());
            stats.addVoteStats(user, voteStats);
        }
        stats.sort();
        final Map<String, VoteStats> winnerRanking = stats.getVoteStats();
        for (Map.Entry<String, VoteStats> rankingEntry : winnerRanking.entrySet()) {
            LOGGER.info("User: " + rankingEntry.getKey() + " - Stats: " + rankingEntry.getValue().toString());
        }
        // TODO eternalize the weekly results...
        //Integer votes = trendCalculator.getVotes();
    }
}