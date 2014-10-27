package com.rizzo.sarcasmotron.web;

import com.rizzo.sarcasmotron.calc.VoteCalculator;
import com.rizzo.sarcasmotron.domain.calc.VoteStats;
import com.rizzo.sarcasmotron.domain.mongodb.Sarcasm;
import com.rizzo.sarcasmotron.domain.web.*;
import com.rizzo.sarcasmotron.mongodb.MongoDBSarcasmRepository;
import net.logstash.logback.encoder.org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.util.List;

@RestController
public class SarcasmotronController {

    @Autowired
    private VoteCalculator voteCalculator;

    @Autowired
    private MongoDBSarcasmRepository mongoDBSarcasmRepository;

    @RequestMapping(value = "/trend", method = RequestMethod.POST)
    public @ResponseBody ResponseEntity<Trend> trend(@RequestBody TrendRequest trendRequest) throws ParseException {
        Trend trend = new Trend().setTrendLine(
                voteCalculator.calculateTrendLineForUser(
                        trendRequest.getUser(),
                        trendRequest.getPeriod(),
                        trendRequest.getInterval()));
        return new ResponseEntity<>(trend, HttpStatus.OK);
    }

    @RequestMapping(value = "/votestats", method = RequestMethod.POST)
    public @ResponseBody ResponseEntity<Stats> stats(@RequestBody StatsRequest statsRequest) throws ParseException {
        if (StringUtils.isNotBlank(statsRequest.getUser())) {
            final VoteStats voteStats = voteCalculator.calculateVoteStatsForUser(
                    statsRequest.getUser(),
                    statsRequest.getPeriod());
            Stats stats = new Stats().addVoteStats(statsRequest.getUser(), voteStats);
            return new ResponseEntity<>(stats, HttpStatus.OK);
        } else {
            final List<String> distinctUsers = voteCalculator.getDistinctUsers();
            Stats stats = new Stats();
            for (String distinctUser : distinctUsers) {
                final VoteStats voteStats = voteCalculator.calculateVoteStatsForUser(
                        distinctUser,
                        statsRequest.getPeriod());
                stats.addVoteStats(distinctUser, voteStats);
                stats.sort();
            }
            return new ResponseEntity<>(stats, HttpStatus.OK);
        }
    }

    @RequestMapping(value = "/upvote", method = RequestMethod.POST)
    public @ResponseBody ResponseEntity<Vote> upVote(@RequestBody VoteRequest voteRequest) {
        final Sarcasm sarcasm = mongoDBSarcasmRepository.findOne(voteRequest.getSarcasmId());
        // TODO get user from security details
        final boolean voteCast = sarcasm.upVote("jalie");
        mongoDBSarcasmRepository.save(sarcasm);
        final ResponseEntity<Vote> responseEntity;
        if (voteCast) {
            responseEntity = new ResponseEntity<>(new Vote().setCast(true).setMessage("Upvote cast success!"), HttpStatus.CREATED);
        } else {
            responseEntity = new ResponseEntity<>(new Vote().setCast(false).setMessage("Vote already cast!"), HttpStatus.GONE);
        }
        return responseEntity;
    }

    @RequestMapping(value = "/downvote", method = RequestMethod.POST)
    public @ResponseBody ResponseEntity<Vote> downVote(@RequestBody VoteRequest voteRequest) {
        final Sarcasm sarcasm = mongoDBSarcasmRepository.findOne(voteRequest.getSarcasmId());
        // TODO get user from security details
        final boolean voteCast = sarcasm.downVote("jalie");
        mongoDBSarcasmRepository.save(sarcasm);
        final ResponseEntity<Vote> responseEntity;
        if (voteCast) {
            responseEntity = new ResponseEntity<>(new Vote().setCast(true).setMessage("Downvote cast success!"), HttpStatus.CREATED);
        } else {
            responseEntity = new ResponseEntity<>(new Vote().setCast(false).setMessage("Vote already cast!"), HttpStatus.GONE);
        }
        return responseEntity;
    }

}
