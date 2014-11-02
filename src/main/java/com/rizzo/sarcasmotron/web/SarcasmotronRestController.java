package com.rizzo.sarcasmotron.web;

import be.milieuinfo.security.openam.api.OpenAMUserdetails;
import com.rizzo.sarcasmotron.calc.VoteCalculator;
import com.rizzo.sarcasmotron.domain.calc.VoteStats;
import com.rizzo.sarcasmotron.domain.mongodb.Sarcasm;
import com.rizzo.sarcasmotron.domain.web.*;
import com.rizzo.sarcasmotron.mongodb.MongoDBSarcasmRepository;
import com.rizzo.sarcasmotron.mongodb.MongoDBUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.util.List;

@RestController
public class SarcasmotronRestController {

    @Autowired
    private VoteCalculator voteCalculator;

    @Autowired
    private MongoDBSarcasmRepository mongoDBSarcasmRepository;

    @Autowired
    private MongoDBUserRepository mongoDBUserRepository;

    @Autowired
    private SecurityContextHolderStrategy securityContextHolderStrategy;

    @RequestMapping(value = "/trend", method = RequestMethod.POST)
    public @ResponseBody ResponseEntity<Trend> trend(@RequestBody TrendRequest trendRequest) throws ParseException {
        if(mongoDBUserRepository.findOneByNickName(trendRequest.getUser()) != null) {
            Trend trend = new Trend().setTrendLine(
                    voteCalculator.calculateTrendLineForUser(
                            trendRequest.getUser(),
                            trendRequest.getPeriod(),
                            trendRequest.getInterval()));
            return new ResponseEntity<>(trend, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(new Trend().setMessage("User: " + trendRequest.getUser() + " not found!"), HttpStatus.NOT_FOUND);
        }
    }

    @RequestMapping(value = "/votestats", method = RequestMethod.POST)
    public @ResponseBody ResponseEntity<Stats> stats(@RequestBody StatsRequest statsRequest) throws ParseException {
        if (!"*".equals(statsRequest.getUser())) {
            if(mongoDBUserRepository.findOneByNickName(statsRequest.getUser()) != null) {
                final VoteStats voteStats = voteCalculator.calculateVoteStatsForUser(
                        statsRequest.getUser(),
                        statsRequest.getPeriod());
                Stats stats = new Stats().setValidity(statsRequest.getPeriod())
                        .addVoteStats(statsRequest.getUser(), voteStats);
                return new ResponseEntity<>(stats.setValidity(statsRequest.getPeriod()).setMessage("VoteStats calculation success!"), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(new Stats().setValidity(statsRequest.getPeriod()).setMessage("User: " + statsRequest.getUser() + " not found!"), HttpStatus.NOT_FOUND);
            }
        } else {
            final List<String> distinctUsers = voteCalculator.getDistinctUsers();
            Stats stats = new Stats();
            for (String distinctUser : distinctUsers) {
                final VoteStats voteStats = voteCalculator.calculateVoteStatsForUser(
                        distinctUser, statsRequest.getPeriod());
                stats.addVoteStats(distinctUser, voteStats);
            }
            stats.sort();
            stats.setValidity(statsRequest.getPeriod());
            return new ResponseEntity<>(stats.setMessage("VoteStats calculation success!"), HttpStatus.OK);
        }
    }

    @RequestMapping(value = "/upvote", method = RequestMethod.POST)
    public @ResponseBody ResponseEntity<Vote> upVote(@RequestBody VoteRequest voteRequest) {
        final Sarcasm sarcasm = mongoDBSarcasmRepository.findOne(voteRequest.getSarcasmId());
        final SecurityContext securityContext = this.securityContextHolderStrategy.getContext();
        final Authentication authentication = securityContext.getAuthentication();
        final OpenAMUserdetails details = (OpenAMUserdetails) authentication.getDetails();
        final String nickname = details.getUsername();
        final ResponseEntity<Vote> responseEntity;
        // users can't vote for their own sarcasm!
        if (!nickname.equals(sarcasm.getUser())) {
            final boolean voteCast = sarcasm.upVote(nickname);
            mongoDBSarcasmRepository.save(sarcasm);
            if (voteCast) {
                responseEntity = new ResponseEntity<>(new Vote().setCast(true).setMessage("Upvote cast success!"), HttpStatus.CREATED);
            } else {
                responseEntity = new ResponseEntity<>(new Vote().setCast(false).setMessage("Vote already cast!"), HttpStatus.GONE);
            }
        } else {
            responseEntity = new ResponseEntity<>(new Vote().setCast(false).setMessage("You cannot vote for your own sarcasm!"), HttpStatus.GONE);
        }
        return responseEntity;
    }

    @RequestMapping(value = "/downvote", method = RequestMethod.POST)
    public @ResponseBody ResponseEntity<Vote> downVote(@RequestBody VoteRequest voteRequest) {
        final Sarcasm sarcasm = mongoDBSarcasmRepository.findOne(voteRequest.getSarcasmId());
        final SecurityContext securityContext = this.securityContextHolderStrategy.getContext();
        final Authentication authentication = securityContext.getAuthentication();
        final OpenAMUserdetails details = (OpenAMUserdetails) authentication.getDetails();
        final String nickname = details.getUsername();
        final ResponseEntity<Vote> responseEntity;
        // users can't vote for their own sarcasm!
        if (!nickname.equals(sarcasm.getUser())) {
            final boolean voteCast = sarcasm.downVote(nickname);
            mongoDBSarcasmRepository.save(sarcasm);
            if (voteCast) {
                responseEntity = new ResponseEntity<>(new Vote().setCast(true).setMessage("Downvote cast success!"), HttpStatus.CREATED);
            } else {
                responseEntity = new ResponseEntity<>(new Vote().setCast(false).setMessage("Vote already cast!"), HttpStatus.GONE);
            }
        } else {
            responseEntity = new ResponseEntity<>(new Vote().setCast(false).setMessage("You cannot vote for your own sarcasm!"), HttpStatus.GONE);
        }
        return responseEntity;
    }

}
