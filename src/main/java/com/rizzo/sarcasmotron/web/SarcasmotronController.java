package com.rizzo.sarcasmotron.web;

import com.rizzo.sarcasmotron.domain.Sarcasm;
import com.rizzo.sarcasmotron.domain.web.Trend;
import com.rizzo.sarcasmotron.domain.web.TrendRequest;
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
    public ResponseEntity<Boolean> upVote(@RequestParam(value = "id") String sarcasmId) {
        final Sarcasm sarcasm = mongoDBSarcasmRepository.findOne(sarcasmId);
        // TODO get user from security details
        final boolean voteCast = sarcasm.upVote("jalie");
        mongoDBSarcasmRepository.save(sarcasm);
        return new ResponseEntity<>(voteCast, HttpStatus.CREATED);
    }

    @RequestMapping(value = "/downvote", method = RequestMethod.POST)
    public ResponseEntity<Boolean> downVote(@RequestParam(value = "id") String sarcasmId) {
        final Sarcasm sarcasm = mongoDBSarcasmRepository.findOne(sarcasmId);
        // TODO get user from security details
        final boolean voteCast = sarcasm.downVote("jalie");
        mongoDBSarcasmRepository.save(sarcasm);
        return new ResponseEntity<>(voteCast, HttpStatus.CREATED);
    }

}
