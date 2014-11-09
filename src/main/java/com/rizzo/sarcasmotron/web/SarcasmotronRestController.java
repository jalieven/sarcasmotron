package com.rizzo.sarcasmotron.web;

import be.milieuinfo.security.openam.api.OpenAMUserdetails;
import com.rizzo.sarcasmotron.calc.VoteCalculator;
import com.rizzo.sarcasmotron.domain.calc.VoteStats;
import com.rizzo.sarcasmotron.domain.mongodb.Comment;
import com.rizzo.sarcasmotron.domain.mongodb.Sarcasm;
import com.rizzo.sarcasmotron.domain.web.*;
import com.rizzo.sarcasmotron.mongodb.MongoDBSarcasmRepository;
import com.rizzo.sarcasmotron.mongodb.MongoDBUserRepository;
import net.logstash.logback.encoder.org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
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

    @RequestMapping(value = "/sarcasm", method = RequestMethod.POST)
    public @ResponseBody ResponseEntity<Sarcasm> createSarcasm(@RequestBody @Valid final Sarcasm sarcasm) {
        return new ResponseEntity<>(mongoDBSarcasmRepository.save(sarcasm), HttpStatus.CREATED);
    }

    @RequestMapping(value = "/sarcasm", method = RequestMethod.GET)
    public @ResponseBody ResponseEntity<Sarcasms> getSarcasms(
            @RequestParam(value = "page", defaultValue = "0") final Integer page,
            @RequestParam(value = "size", defaultValue = "50") final Integer size) {
        final PageRequest pageRequest = new PageRequest(page, size);
        final Page<Sarcasm> sarcasmPage = mongoDBSarcasmRepository.findAll(pageRequest);
        return new ResponseEntity<>(new Sarcasms().setPages(sarcasmPage.getTotalPages()).setTotal(sarcasmPage.getTotalElements()).setSarcasms(sarcasmPage.getContent()), HttpStatus.OK);
    }

    @RequestMapping(value = "/sarcasm/{id}", method = RequestMethod.GET)
    public @ResponseBody ResponseEntity<Sarcasm> getSarcasm(@PathVariable("id") String id) {
        ResponseEntity<Sarcasm> responseEntity;
        final Sarcasm foundSarcasm = mongoDBSarcasmRepository.findOne(id);
        if(foundSarcasm != null) {
            responseEntity = new ResponseEntity<>(foundSarcasm, HttpStatus.OK);
        } else {
            responseEntity = new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return responseEntity;
    }

    @RequestMapping(value = "/sarcasm/{id}", method = RequestMethod.PUT)
    public @ResponseBody ResponseEntity<Sarcasm> updateSarcasm(@PathVariable("id") String id,
            @RequestBody final Sarcasm sarcasm) {
        ResponseEntity<Sarcasm> responseEntity;
        final Sarcasm foundSarcasm = mongoDBSarcasmRepository.findOne(id);
        if(foundSarcasm != null) {
            String currentUser = getCurrentUserNickname();
            if(currentUser.equals(foundSarcasm.getCreator())) {
                foundSarcasm.setQuote(sarcasm.getQuote()).setContext(sarcasm.getContext()).setEdited(true);
                final Sarcasm updatedSarcasm = mongoDBSarcasmRepository.save(foundSarcasm);
                responseEntity = new ResponseEntity<>(updatedSarcasm, HttpStatus.NO_CONTENT);
            } else {
                responseEntity = new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
            }
        } else {
            responseEntity = new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return responseEntity;
    }

    @RequestMapping(value = "/sarcasm/{id}", method = RequestMethod.DELETE)
    public @ResponseBody ResponseEntity deleteSarcasm(@PathVariable("id") String id) {
        ResponseEntity responseEntity;
        final Sarcasm foundSarcasm = mongoDBSarcasmRepository.findOne(id);
        if(foundSarcasm != null) {
            mongoDBSarcasmRepository.delete(id);
            responseEntity = new ResponseEntity(HttpStatus.NO_CONTENT);
        } else {
            responseEntity = new ResponseEntity(HttpStatus.NOT_FOUND);
        }
        return responseEntity;
    }

    @RequestMapping(value = "/sarcasm/{id}/upvote", method = RequestMethod.POST)
    public @ResponseBody ResponseEntity<Vote> upVote(@PathVariable("id") String id) {
        final Sarcasm sarcasm = mongoDBSarcasmRepository.findOne(id);
        final String nickname = getCurrentUserNickname();
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

    @RequestMapping(value = "/sarcasm/{id}/downvote", method = RequestMethod.POST)
    public @ResponseBody ResponseEntity<Vote> downVote(@PathVariable("id") String id) {
        final Sarcasm sarcasm = mongoDBSarcasmRepository.findOne(id);
        final String nickname = getCurrentUserNickname();
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

    @RequestMapping(value = "/sarcasm/{id}/comment", method = RequestMethod.POST)
    public @ResponseBody ResponseEntity createComment(@PathVariable("id") String id,
                                                      @RequestBody @Valid final Comment comment) {
        ResponseEntity responseEntity;
        final Sarcasm foundSarcasm = mongoDBSarcasmRepository.findOne(id);
        if(foundSarcasm != null) {
            comment.setUser(getCurrentUserNickname()).stamp();
            foundSarcasm.addComment(comment);
            mongoDBSarcasmRepository.save(foundSarcasm);
            responseEntity = new ResponseEntity<>(HttpStatus.CREATED);
        } else {
            responseEntity = new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return responseEntity;
    }



    @RequestMapping(value = "/sarcasm/{id}/comment", method = RequestMethod.GET)
    public @ResponseBody ResponseEntity<List<Comment>> getComments(@PathVariable("id") String id) {
        ResponseEntity<List<Comment>> responseEntity;
        final Sarcasm foundSarcasm = mongoDBSarcasmRepository.findOne(id);
        if(foundSarcasm != null) {
            responseEntity = new ResponseEntity<>(foundSarcasm.getComments(), HttpStatus.OK);
        } else {
            responseEntity = new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return responseEntity;
    }


    @RequestMapping(value = "/sarcasm/{id}/favorite", method = RequestMethod.POST)
    public @ResponseBody ResponseEntity createFavorite(@PathVariable("id") String id) {
        ResponseEntity responseEntity;
        final Sarcasm foundSarcasm = mongoDBSarcasmRepository.findOne(id);
        if(foundSarcasm != null) {
            foundSarcasm.addFavorite(getCurrentUserNickname());
            mongoDBSarcasmRepository.save(foundSarcasm);
            responseEntity = new ResponseEntity<>(HttpStatus.CREATED);
        } else {
            responseEntity = new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return responseEntity;
    }

    @RequestMapping(value = "/sarcasm/favorite", method = RequestMethod.GET)
    public @ResponseBody ResponseEntity<List<Sarcasm>> favorites() {
        final String nickname = getCurrentUserNickname();
        final List<Sarcasm> favorites = mongoDBSarcasmRepository.findFavorites(nickname);
        return new ResponseEntity<>(favorites, HttpStatus.OK);
    }

    @RequestMapping(value = "/sarcasm/tovote", method = RequestMethod.GET)
    public @ResponseBody ResponseEntity<List<Sarcasm>> toVote() {
        final String nickname = getCurrentUserNickname();
        final List<Sarcasm> sarcasmsToVote = mongoDBSarcasmRepository.findToVote(nickname);
        return new ResponseEntity<>(sarcasmsToVote, HttpStatus.OK);
    }

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



    private String getCurrentUserNickname() {
        final SecurityContext securityContext = this.securityContextHolderStrategy.getContext();
        final Authentication authentication = securityContext.getAuthentication();
        final OpenAMUserdetails details = (OpenAMUserdetails) authentication.getDetails();
        return details.getUsername();
    }
}
