package com.rizzo.sarcasmotron.api;

import com.google.common.collect.Iterables;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.path.json.config.JsonPathConfig;
import com.rizzo.sarcasmotron.boot.Sarcasmotron;
import com.rizzo.sarcasmotron.domain.calc.VoteStats;
import com.rizzo.sarcasmotron.domain.mongodb.Comment;
import com.rizzo.sarcasmotron.domain.mongodb.Sarcasm;
import com.rizzo.sarcasmotron.domain.web.Stats;
import com.rizzo.sarcasmotron.domain.web.StatsRequest;
import com.rizzo.sarcasmotron.domain.web.TrendRequest;
import com.rizzo.sarcasmotron.domain.web.VoteRequest;
import com.rizzo.sarcasmotron.elasticsearch.ElasticsearchSarcasmRepository;
import com.rizzo.sarcasmotron.mongodb.MongoDBSarcasmRepository;
import com.rizzo.sarcasmotron.calc.VoteCalculator;
import org.elasticsearch.common.joda.time.Days;
import org.hamcrest.Matchers;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.config.JsonConfig.jsonConfig;
import static com.jayway.restassured.config.RestAssuredConfig.newConfig;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Sarcasmotron.class)
@WebAppConfiguration
@IntegrationTest({"server.port=0"})
public class RestApiTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestApiTest.class);

    @Value("${local.server.port}")
    private int port = 0;

    @Autowired
    private MongoDBSarcasmRepository mongoDBSarcasmRepository;

    @Autowired
    private ElasticsearchSarcasmRepository elasticsearchSarcasmRepository;

    @Autowired
    private VoteCalculator voteCalculator;

    private TestRestTemplate restTemplate = new TestRestTemplate();

    @Before
    public void setUp() throws InterruptedException {
        RestAssured.port = port;
        mongoDBSarcasmRepository.deleteAll();
    }

    @After
    public void tearDown() throws InterruptedException {
        //mongoDBSarcasmRepository.deleteAll();
    }

    @Test
    public void testHome() throws Exception {
        ResponseEntity<String> entity = restTemplate.getForEntity(
                "http://localhost:" + this.port, String.class);
        assertEquals(HttpStatus.OK, entity.getStatusCode());
        assertTrue("Wrong body (title doesn't match):\n" + entity.getBody(), entity
                .getBody().contains("<title>Sarcasmotron"));
    }

    @Test
    public void testPostSarcasms() throws Exception {
        LOGGER.debug("TEST: testPostSarcasms");
        final Comment comment = new Comment()
                .setComment("Very funny! Cuz it's true!")
                .stamp()
                .setUser("joost");
        final Sarcasm sarcasm0 = new Sarcasm()
                .setQuote("I'm trying to imagine you with a personality.")
                .setContext("In a fight!")
                .stamp()
                .setUser("joost")
                .addComment(comment);

        final Sarcasm sarcasm1 = new Sarcasm()
                .setQuote("I work 40 hours a week to be this poor.")
                .setContext("When I'm tired of doing the same every time.")
                .stamp()
                .setUser("gert");

        given().log().all()
                .contentType(ContentType.JSON).body(sarcasm0)
                .when().post("/sarcasms")
                .then().log().all()
                .statusCode(HttpStatus.CREATED.value());

        given().log().all()
                .contentType(ContentType.JSON).body(sarcasm1)
                .when().post("/sarcasms")
                .then().log().all()
                .statusCode(HttpStatus.CREATED.value());

        assertEquals(2, Iterables.size(elasticsearchSarcasmRepository.findAll()));
    }

    @Test
    public void testGetSarcasms() throws Exception {
        LOGGER.debug("TEST: testGetSarcasms");
        final Comment comment = new Comment()
                .setComment("Very funny! Cuz it's true!")
                .stamp()
                .setUser("jalie");
        final Sarcasm sarcasm = new Sarcasm()
                .setQuote("I'm trying to imagine you with a personality.")
                .setContext("In a fight!")
                .stamp()
                .setUser("jalie")
                .addComment(comment);
        sarcasm.upVote("jalie");
        this.mongoDBSarcasmRepository.deleteAll();
        this.mongoDBSarcasmRepository.save(sarcasm);
        final String sarcasmId = sarcasm.getId();
        LOGGER.debug("Generated Sarcasm id: " + sarcasmId);

        given().log().all()
                .when().get("/sarcasms/{id}", sarcasmId)
                .then().log().all()
                .statusCode(HttpStatus.OK.value())
                .body("user", Matchers.is("jalie"))
                .body("quote", Matchers.is("I'm trying to imagine you with a personality."))
                .body("comments.user", Matchers.hasItem("jalie"))
                .body("comments.comment", Matchers.hasItem("Very funny! Cuz it's true!"));

    }

    @Test
    public void testUpdateSarcasms() throws Exception {
        LOGGER.debug("TEST: testUpdateSarcasms");
        final Comment comment = new Comment()
                .setComment("Very funny! Cuz it's true!")
                .stamp()
                .setUser("jalie");
        final Sarcasm sarcasm = new Sarcasm()
                .setQuote("I'm trying to imagine you with a personality.")
                .setContext("In a fight!")
                .stamp()
                .setUser("jalie")
                .addComment(comment);

        this.mongoDBSarcasmRepository.save(sarcasm);
        final String sarcasmId = sarcasm.getId();
        LOGGER.debug("Generated Sarcasm id: " + sarcasmId);

        final Sarcasm newSarcasm = new Sarcasm()
                .setQuote("This isn't an office. It's Hell with fluorescent lighting.")
                .setContext("On a good office day!")
                .stamp()
                .setUser("joost");

        given().log().all()
                .contentType(ContentType.JSON).body(new VoteRequest().setSarcasmId(sarcasmId))
                .when().post("/downvote")
                .then().log().all()
                .statusCode(HttpStatus.CREATED.value())
                .body("cast", Matchers.is(true));

        given().log().all()
                .contentType(ContentType.JSON).body(newSarcasm)
                .when().put("/sarcasms/{id}", sarcasmId)
                .then().log().all()
                .statusCode(HttpStatus.NO_CONTENT.value());

        given().log().all()
                .when().get("/sarcasms/{id}", sarcasmId)
                .then().log().all()
                .statusCode(HttpStatus.OK.value())
                .body("user", Matchers.is("joost"))
                .body("voteTotal", Matchers.is(0))
                .body("quote", Matchers.is("This isn't an office. It's Hell with fluorescent lighting."))
                .body("comments", Matchers.empty());
    }

    @Test
    public void testDeleteSarcasms() throws Exception {
        LOGGER.debug("TEST: testDeleteSarcasms");
        final Comment comment = new Comment()
                .setComment("Very funny! Cuz it's true!")
                .stamp()
                .setUser("jalie");
        final Sarcasm sarcasm = new Sarcasm()
                .setQuote("I'm trying to imagine you with a personality.")
                .setContext("In a fight!")
                .stamp()
                .setUser("jalie")
                .addComment(comment);
        sarcasm.upVote("jalie");

        this.mongoDBSarcasmRepository.save(sarcasm);
        final String sarcasmId = sarcasm.getId();
        LOGGER.debug("Generated Sarcasm id: " + sarcasmId);

        given().log().all()
                .when().delete("/sarcasms/{id}", sarcasmId)
                .then().log().all()
                .statusCode(HttpStatus.NO_CONTENT.value());

        assertEquals(0, this.mongoDBSarcasmRepository.findAll().size());
        assertEquals(0, Iterables.size(this.elasticsearchSarcasmRepository.findAll()));
    }

    @Test
    public void testTrendLine() throws InterruptedException {
        LOGGER.debug("TEST: testTrendLine");

        final DateTime now = new DateTime();
        final DateTime pastMidnight = now.withTimeAtStartOfDay();
        final DateTime yesterdayMidnight = now.minus(org.joda.time.Days.days(1)).withTimeAtStartOfDay();

        // first create some sarcastic quotes
        final Sarcasm sarcasmFour = new Sarcasm()
                .setUser("jalie")
                .setContext("Some context")
                .setQuote("Very sarcastic quote")
                .stamp(now.minus(org.joda.time.Days.days(6)));
        sarcasmFour.upVote("kenzo");
        sarcasmFour.upVote("joost");
        sarcasmFour.upVote("gert");
        mongoDBSarcasmRepository.save(sarcasmFour);
        LOGGER.debug("Generated Sarcasm id: " + sarcasmFour.getId());

        final Sarcasm sarcasmThree = new Sarcasm()
                .setUser("jalie")
                .setContext("Some context")
                .setQuote("Very sarcastic quote")
                .stamp(now.minus(org.joda.time.Days.days(5)));
        sarcasmThree.downVote("joost");
        sarcasmThree.upVote("wijlen");
        sarcasmThree.upVote("ikke");
        sarcasmThree.upVote("gij");
        mongoDBSarcasmRepository.save(sarcasmThree);
        LOGGER.debug("Generated Sarcasm id: " + sarcasmThree.getId());

        final Sarcasm sarcasmTwo = new Sarcasm()
                .setUser("jalie")
                .setContext("Some context")
                .setQuote("Very sarcastic quote")
                .stamp(now.minus(org.joda.time.Days.days(4)));
        sarcasmTwo.upVote("ikke");
        sarcasmTwo.downVote("gij");
        sarcasmTwo.upVote("wij");
        sarcasmTwo.upVote("zij");
        sarcasmTwo.upVote("jullie");
        mongoDBSarcasmRepository.save(sarcasmTwo);
        LOGGER.debug("Generated Sarcasm id: " + sarcasmTwo.getId());

        final Sarcasm sarcasmOne = new Sarcasm()
                .setUser("jalie")
                .setContext("Some context")
                .setQuote("Very sarcastic quote")
                .stamp(now.minus(org.joda.time.Days.days(1)));
        sarcasmOne.upVote("ikke");
        sarcasmOne.downVote("gij");
        sarcasmOne.upVote("zij");
        mongoDBSarcasmRepository.save(sarcasmOne);
        LOGGER.debug("Generated Sarcasm id: " + sarcasmOne.getId());

        final Sarcasm sarcasmZero = new Sarcasm()
                .setUser("jalie")
                .setContext("Some context")
                .setQuote("Very sarcastic quote")
                .stamp(now);
        sarcasmZero.upVote("ikke");
        sarcasmZero.downVote("gij");
        sarcasmZero.upVote("zij");
        sarcasmZero.upVote("wij");
        sarcasmZero.upVote("jullie");
        sarcasmZero.upVote("iemand");
        mongoDBSarcasmRepository.save(sarcasmZero);
        LOGGER.debug("Generated Sarcasm id: " + sarcasmZero.getId());

        final List<String> distinctUsers = voteCalculator.getDistinctUsers();
        assertEquals(1, distinctUsers.size());
        assertEquals("jalie", distinctUsers.get(0));

        final TrendRequest trendRequest = new TrendRequest()
                .setUser(distinctUsers.get(0))
                .setPeriodExpression("7d")
                .setIntervalExpression("1d");
        final String intervalKey = Sarcasm.TIMESTAMP_FORMAT.format(pastMidnight.toDate());
        final String yesterdayIntervalKey = Sarcasm.TIMESTAMP_FORMAT.format(yesterdayMidnight.toDate());
        final String timezoneNeutralIntervalKey = intervalKey.replace(intervalKey.substring(intervalKey.length() - 4, intervalKey.length()), "0000");
        final String timezoneNeutralYesterdayIntervalKey = yesterdayIntervalKey.replace(yesterdayIntervalKey.substring(yesterdayIntervalKey.length() - 4, yesterdayIntervalKey.length()), "0000");
        given().log().all().config(newConfig().jsonConfig(jsonConfig().numberReturnType(JsonPathConfig.NumberReturnType.BIG_DECIMAL)))
                .contentType(ContentType.JSON).body(trendRequest)
                .when().post("/trend")
                .then().log().all()
                .statusCode(HttpStatus.OK.value())
                .body("trendLine.'" + timezoneNeutralYesterdayIntervalKey + "'", Matchers.is(new BigDecimal("-0.3956282840374723")))
                .body("trendLine.'" + timezoneNeutralIntervalKey + "'", Matchers.is(new BigDecimal("1.8136906252750293")));

    }

    @Test
    public void testVoteCasting() {
        LOGGER.debug("TEST: testVoteCasting");
        final Sarcasm sarcasm = new Sarcasm()
                .setUser("jalie")
                .setContext("Some context")
                .setQuote("Very sarcastic quote")
                .stamp();
        sarcasm.upVote("gert");
        mongoDBSarcasmRepository.save(sarcasm);
        LOGGER.debug("Generated Sarcasm id: " + sarcasm.getId());

        given().log().all()
                .contentType(ContentType.JSON).body(new VoteRequest().setSarcasmId(sarcasm.getId()))
                .when().post("/upvote")
                .then().log().all()
                .statusCode(HttpStatus.CREATED.value())
                .body("cast", Matchers.is(true));

        given().log().all()
                .contentType(ContentType.JSON).body(new VoteRequest().setSarcasmId(sarcasm.getId()))
                .when().post("/downvote")
                .then().log().all()
                .statusCode(HttpStatus.GONE.value())
                .body("cast", Matchers.is(false));

    }

    @Test
    public void testTotalVotes() {
        final DateTime now = new DateTime();

        final Sarcasm sarcasmFour = new Sarcasm()
                .setUser("jalie")
                .setContext("Some context")
                .setQuote("Very sarcastic quote")
                .stamp(now.minus(org.joda.time.Days.days(6)));
        sarcasmFour.upVote("kenzo");
        sarcasmFour.upVote("joost");
        sarcasmFour.upVote("gert");
        mongoDBSarcasmRepository.save(sarcasmFour);
        LOGGER.debug("Generated Sarcasm id: " + sarcasmFour.getId());

        final Sarcasm sarcasmThree = new Sarcasm()
                .setUser("jalie")
                .setContext("Some context")
                .setQuote("Very sarcastic quote")
                .stamp(now.minus(org.joda.time.Days.days(5)));
        sarcasmThree.downVote("joost");
        sarcasmThree.upVote("wijlen");
        sarcasmThree.upVote("ikke");
        sarcasmThree.upVote("gij");
        mongoDBSarcasmRepository.save(sarcasmThree);
        LOGGER.debug("Generated Sarcasm id: " + sarcasmThree.getId());

        final Sarcasm sarcasmTwo = new Sarcasm()
                .setUser("jalie")
                .setContext("Some context")
                .setQuote("Very sarcastic quote")
                .stamp(now.minus(org.joda.time.Days.days(4)));
        sarcasmTwo.upVote("ikke");
        sarcasmTwo.downVote("gij");
        sarcasmTwo.upVote("wij");
        sarcasmTwo.upVote("zij");
        sarcasmTwo.upVote("jullie");
        mongoDBSarcasmRepository.save(sarcasmTwo);
        LOGGER.debug("Generated Sarcasm id: " + sarcasmTwo.getId());

        final Sarcasm sarcasmOne = new Sarcasm()
                .setUser("jalie")
                .setContext("Some context")
                .setQuote("Very sarcastic quote")
                .stamp(now.minus(org.joda.time.Days.days(1)));
        sarcasmOne.upVote("ikke");
        sarcasmOne.downVote("gij");
        sarcasmOne.upVote("zij");
        mongoDBSarcasmRepository.save(sarcasmOne);
        LOGGER.debug("Generated Sarcasm id: " + sarcasmOne.getId());

        final Sarcasm sarcasmZero = new Sarcasm()
                .setUser("jalie")
                .setContext("Some context")
                .setQuote("Very sarcastic quote")
                .stamp(now);
        sarcasmZero.upVote("ikke");
        sarcasmZero.downVote("gij");
        sarcasmZero.upVote("zij");
        sarcasmZero.upVote("wij");
        sarcasmZero.upVote("jullie");
        sarcasmZero.upVote("iemand");
        mongoDBSarcasmRepository.save(sarcasmZero);

        final VoteStats stats = voteCalculator.calculateVoteStatsForUser("jalie", Days.days(3));
        assertEquals(new Long(2), stats.getCount());
        assertEquals(new Double(5.0), stats.getSum());
        assertEquals(new Double(1.0), stats.getMin());
        assertEquals(new Double(4.0), stats.getMax());
    }

    @Test
    public void testWinnerCalculation() throws InterruptedException {
        final DateTime now = new DateTime();

        final Sarcasm sarcasmThree = new Sarcasm()
                .setUser("jalie")
                .setContext("Some context")
                .setQuote("Very sarcastic quote")
                .stamp(now.minus(org.joda.time.Days.days(5)));
        sarcasmThree.downVote("joost");
        sarcasmThree.upVote("wijlen");
        sarcasmThree.upVote("ikke");
        sarcasmThree.upVote("gij");
        mongoDBSarcasmRepository.save(sarcasmThree);
        LOGGER.debug("Generated Sarcasm id: " + sarcasmThree.getId());

        final Sarcasm sarcasmTwo = new Sarcasm()
                .setUser("gert")
                .setContext("Some context")
                .setQuote("Very sarcastic quote")
                .stamp(now.minus(org.joda.time.Days.days(4)));
        sarcasmTwo.upVote("ikke");
        sarcasmTwo.downVote("gij");
        sarcasmTwo.upVote("wij");
        sarcasmTwo.upVote("zij");
        sarcasmTwo.upVote("zijlen");
        sarcasmTwo.upVote("wijlen");
        sarcasmTwo.upVote("gunder");
        sarcasmTwo.upVote("jullie");
        mongoDBSarcasmRepository.save(sarcasmTwo);
        LOGGER.debug("Generated Sarcasm id: " + sarcasmTwo.getId());

        final Sarcasm sarcasmOne = new Sarcasm()
                .setUser("joost")
                .setContext("Some context")
                .setQuote("Very sarcastic quote")
                .stamp(now.minus(org.joda.time.Days.days(1)));
        sarcasmOne.upVote("ikke");
        sarcasmOne.downVote("gij");
        sarcasmOne.upVote("zij");
        mongoDBSarcasmRepository.save(sarcasmOne);
        LOGGER.debug("Generated Sarcasm id: " + sarcasmOne.getId());

        final Sarcasm sarcasmZero = new Sarcasm()
                .setUser("jalie")
                .setContext("Some context")
                .setQuote("Very sarcastic quote")
                .stamp(now);
        sarcasmZero.upVote("ikke");
        sarcasmZero.downVote("gij");
        sarcasmZero.upVote("zij");
        sarcasmZero.upVote("wij");
        sarcasmZero.upVote("jullie");
        sarcasmZero.upVote("iemand");
        mongoDBSarcasmRepository.save(sarcasmZero);

        // TODO rework this to API call
        final List<String> users = voteCalculator.getDistinctUsers();
        Stats stats = new Stats();
        for (String user : users) {
            final VoteStats voteStats = voteCalculator.calculateVoteStatsForUser(user,
                    new StatsRequest().setPeriodExpression("1w").getPeriod());
            stats.addVoteStats(user, voteStats);
        }
        stats.sort();
        final Map<String, VoteStats> sortedStats = stats.getVoteStats();
        assertNotNull(Iterables.getFirst(sortedStats.keySet(), null));
        assertNotNull(Iterables.getLast(sortedStats.keySet(), null));
        assertEquals(3, Iterables.size(sortedStats.keySet()));
        assertEquals("jalie", Iterables.getFirst(sortedStats.keySet(), null));
        assertEquals("gert", Iterables.get(sortedStats.keySet(), 1));
        assertEquals("joost", Iterables.getLast(sortedStats.keySet(), null));

    }

}
