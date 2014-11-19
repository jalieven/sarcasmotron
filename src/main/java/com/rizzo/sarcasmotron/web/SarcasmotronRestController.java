package com.rizzo.sarcasmotron.web;

import be.milieuinfo.security.openam.api.OpenAMUserdetails;
import com.google.common.collect.Lists;
import com.rizzo.sarcasmotron.calc.VoteCalculator;
import com.rizzo.sarcasmotron.domain.calc.VoteStats;
import com.rizzo.sarcasmotron.domain.elasticsearch.ESSarcasm;
import com.rizzo.sarcasmotron.domain.elasticsearch.ESUser;
import com.rizzo.sarcasmotron.domain.mongodb.Comment;
import com.rizzo.sarcasmotron.domain.mongodb.Sarcasm;
import com.rizzo.sarcasmotron.domain.mongodb.User;
import com.rizzo.sarcasmotron.domain.web.*;
import com.rizzo.sarcasmotron.elasticsearch.ElasticsearchSarcasmRepository;
import com.rizzo.sarcasmotron.mongodb.MongoDBSarcasmRepository;
import com.rizzo.sarcasmotron.mongodb.MongoDBUserRepository;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
    private ElasticsearchSarcasmRepository elasticsearchSarcasmRepository;

    @Autowired
    private SecurityContextHolderStrategy securityContextHolderStrategy;

    @RequestMapping(value = "/user", method = RequestMethod.GET)
    public @ResponseBody ResponseEntity<List<User>> getUsers(
            @RequestParam(value = "page", defaultValue = "0") final Integer page,
            @RequestParam(value = "size", defaultValue = "100") final Integer size) {
        final PageRequest pageRequest = new PageRequest(page, size,
                new Sort(new Sort.Order(Sort.Direction.DESC, "surName")));
        final Page<User> userPage = mongoDBUserRepository.findAll(pageRequest);
        return new ResponseEntity<>(userPage.getContent(), HttpStatus.OK);
    }

    @RequestMapping(value = "/sarcasm", method = RequestMethod.POST)
    public @ResponseBody ResponseEntity<Sarcasm> createSarcasm(@RequestBody final Sarcasm sarcasm) {
        final User user = sarcasm.getUser();
        if (user != null) {
            final User foundUser = mongoDBUserRepository.findOneByNickName(user.getNickName());
            final User foundCreator = mongoDBUserRepository.findOneByNickName(getCurrentUserNickname());
            if(foundUser != null && foundCreator != null) {
                sarcasm.setUser(foundUser);
                sarcasm.setCreator(foundCreator.getNickName());
                return new ResponseEntity<>(mongoDBSarcasmRepository.save(sarcasm), HttpStatus.CREATED);
            } else {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @RequestMapping(value = "/sarcasm", method = RequestMethod.GET)
    public @ResponseBody ResponseEntity<List<Sarcasm>> getSarcasms(
            @RequestParam(value = "page", defaultValue = "0") final Integer page,
            @RequestParam(value = "size", defaultValue = "50") final Integer size) {
        final PageRequest pageRequest = new PageRequest(page, size,
                new Sort(new Sort.Order(Sort.Direction.DESC, "timestamp")));
        final Page<Sarcasm> sarcasmPage = mongoDBSarcasmRepository.findAll(pageRequest);
        for (Sarcasm sarcasm : sarcasmPage) {
            sarcasm.checkState(getCurrentUserNickname());
        }
        return new ResponseEntity<>(sarcasmPage.getContent(), HttpStatus.OK);
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
        if (!nickname.equals(sarcasm.getUser().getNickName())) {
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
        if (!nickname.equals(sarcasm.getUser().getNickName())) {
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
    public @ResponseBody ResponseEntity<Sarcasm> createFavorite(@PathVariable("id") String id) {
        ResponseEntity<Sarcasm> responseEntity;
        final Sarcasm foundSarcasm = mongoDBSarcasmRepository.findOne(id);
        if(foundSarcasm != null) {
            foundSarcasm.toggleFavorite(getCurrentUserNickname());
            mongoDBSarcasmRepository.save(foundSarcasm);
            responseEntity = new ResponseEntity<>(foundSarcasm, HttpStatus.CREATED);
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
            final List<String> distinctUsers = voteCalculator.getDistinctSarcasticUsers();
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

    @RequestMapping(value = "/sarcasm/search", method = RequestMethod.GET)
    public @ResponseBody ResponseEntity<List<Sarcasm>> search(@RequestParam(value = "query", defaultValue = "*") String query,
                                                              @RequestParam(value = "page", defaultValue = "0") final Integer page,
                                                              @RequestParam(value = "size", defaultValue = "50") final Integer size) {
        final QueryStringQueryBuilder stringQueryBuilder = new QueryStringQueryBuilder(query);
        List<Sarcasm> sarcasms;
        if(query.contains("votes.")) {
            NestedQueryBuilder nestedQueryBuilder = new NestedQueryBuilder("votes", stringQueryBuilder);
            final Iterable<ESSarcasm> foundSarcasms = elasticsearchSarcasmRepository.search(nestedQueryBuilder,
                    new PageRequest(page, size, new Sort(new Sort.Order(Sort.Direction.DESC, "timestamp"))));
            sarcasms = Lists.newArrayList(mapSarcasms(foundSarcasms));
        } else if(query.contains("user.")) {
            NestedQueryBuilder nestedQueryBuilder = new NestedQueryBuilder("user", stringQueryBuilder);
            final Iterable<ESSarcasm> foundSarcasms = elasticsearchSarcasmRepository.search(nestedQueryBuilder,
                    new PageRequest(page, size, new Sort(new Sort.Order(Sort.Direction.DESC, "timestamp"))));
            sarcasms = Lists.newArrayList(mapSarcasms(foundSarcasms));
        } else {
            final Iterable<ESSarcasm> foundSarcasms = elasticsearchSarcasmRepository.search(stringQueryBuilder,
                    new PageRequest(page, size, new Sort(new Sort.Order(Sort.Direction.DESC, "timestamp"))));
            sarcasms = Lists.newArrayList(mapSarcasms(foundSarcasms));
        }
        return new ResponseEntity<>(sarcasms, HttpStatus.OK);
    }

    private String getCurrentUserNickname() {
        final SecurityContext securityContext = this.securityContextHolderStrategy.getContext();
        final Authentication authentication = securityContext.getAuthentication();
        final OpenAMUserdetails details = (OpenAMUserdetails) authentication.getDetails();
        return details.getUsername();
    }

    private Iterable<Sarcasm> mapSarcasms(Iterable<ESSarcasm> esSarcasms) {
        List<Sarcasm> sarcasms = Lists.newArrayList();
        for (ESSarcasm esSarcasm : esSarcasms) {
            sarcasms.add(mapSarcasm(esSarcasm));
        }
        return sarcasms;
    }

    private Sarcasm mapSarcasm(ESSarcasm esSarcasm) {
        final Sarcasm sarcasm = new Sarcasm()
                .setId(esSarcasm.getId()).setQuote(esSarcasm.getQuote())
                .setContext(esSarcasm.getContext()).setUser(mapUser(esSarcasm.getUser()))
                .setCreator(esSarcasm.getCreator())
                .setVotes(esSarcasm.getVotes())
                .setTimestamp(esSarcasm.getTimestamp());
        sarcasm.checkState(getCurrentUserNickname());
        return sarcasm;
    }

    private User mapUser(ESUser esUser) {
        return new User().setEmail(esUser.getEmail())
                .setGivenName(esUser.getGivenName())
                .setSurName(esUser.getSurName())
                .setNickName(esUser.getNickName())
                .setGravatar(esUser.getGravatar());
    }
}
