package com.rizzo.sarcasmotron.web;

import com.rizzo.sarcasmotron.domain.mongodb.Sarcasm;
import com.rizzo.sarcasmotron.domain.web.Trend;
import com.rizzo.sarcasmotron.domain.web.TrendRequest;
import com.rizzo.sarcasmotron.domain.web.Vote;
import com.rizzo.sarcasmotron.domain.web.VoteRequest;
import com.rizzo.sarcasmotron.mongodb.MongoDBSarcasmRepository;
import com.rizzo.sarcasmotron.trend.TrendCalculator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;

@RestController
public class SarcasmotronController {

    @Autowired
    private TrendCalculator trendCalculator;

    @Autowired
    private MongoDBSarcasmRepository mongoDBSarcasmRepository;

    @RequestMapping(value = "/trend", method = RequestMethod.POST)
    public @ResponseBody ResponseEntity<Trend> seed(@RequestBody TrendRequest trendRequest) throws ParseException {
        Trend trend = new Trend().setTrendLine(
                trendCalculator.calculateTrendLineForUser(
                        trendRequest.getUser(),
                        trendRequest.getPeriod(),
                        trendRequest.getInterval()));
        return new ResponseEntity<>(trend, HttpStatus.OK);
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
