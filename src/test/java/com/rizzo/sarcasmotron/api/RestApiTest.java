package com.rizzo.sarcasmotron.api;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.path.json.config.JsonPathConfig;
import com.rizzo.sarcasmotron.boot.Sarcasmotron;
import com.rizzo.sarcasmotron.calc.VoteCalculator;
import com.rizzo.sarcasmotron.domain.calc.VoteStats;
import com.rizzo.sarcasmotron.domain.mongodb.Comment;
import com.rizzo.sarcasmotron.domain.mongodb.Sarcasm;
import com.rizzo.sarcasmotron.domain.mongodb.User;
import com.rizzo.sarcasmotron.domain.web.StatsRequest;
import com.rizzo.sarcasmotron.domain.web.TrendRequest;
import com.rizzo.sarcasmotron.domain.web.VoteRequest;
import com.rizzo.sarcasmotron.elasticsearch.ElasticsearchSarcasmRepository;
import com.rizzo.sarcasmotron.mongodb.MongoDBSarcasmRepository;
import com.rizzo.sarcasmotron.mongodb.MongoDBStatsRepository;
import com.rizzo.sarcasmotron.mongodb.MongoDBUserRepository;
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
import java.util.Date;
import java.util.List;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.config.JsonConfig.jsonConfig;
import static com.jayway.restassured.config.RestAssuredConfig.newConfig;
import static org.junit.Assert.assertEquals;
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
    private MongoDBUserRepository mongoDBUserRepository;

    @Autowired
    private MongoDBStatsRepository mongoDBStatsRepository;

    @Autowired
    private ElasticsearchSarcasmRepository elasticsearchSarcasmRepository;

    @Autowired
    private VoteCalculator voteCalculator;

    private TestRestTemplate restTemplate = new TestRestTemplate();

    @Before
    public void setUp() throws InterruptedException {
        LOGGER.warn("IntegrationTest running on port: " + port);
        RestAssured.port = port;
        mongoDBSarcasmRepository.deleteAll();
        mongoDBUserRepository.deleteAll();
        mongoDBStatsRepository.deleteAll();

        List<User> users = ImmutableList.of(
                new User().setLastLogin(new Date()).setGivenName("Jan").setSurName("Lievens").setNickName("jalie").setEmail("jan.lievens@gmail.com"),
                new User().setLastLogin(new Date()).setGivenName("Joost").setSurName("Bouckenooghe").setNickName("joost").setEmail("jan.lievens@gmail.com"),
                new User().setLastLogin(new Date()).setGivenName("Tom").setSurName("Van Gulck").setNickName("tom").setEmail("jan.lievens@gmail.com"),
                new User().setLastLogin(new Date()).setGivenName("Gert").setSurName("Dewit").setNickName("gert").setEmail("jan.lievens@gmail.com"));
        mongoDBUserRepository.save(users);
    }

//    @After
//    public void tearDown() {
//        mongoDBSarcasmRepository.deleteAll();
//        mongoDBUserRepository.deleteAll();
//        mongoDBStatsRepository.deleteAll();
//    }

    @Test
    public void testHome() throws Exception {
        ResponseEntity<String> entity = restTemplate.getForEntity(
                "http://localhost:" + this.port, String.class);
        assertEquals(HttpStatus.OK, entity.getStatusCode());
        assertTrue("Wrong body (title doesn't match):\n" + entity.getBody(), entity
                .getBody().contains("<title>Sarcasmotron"));
    }

    private Sarcasm createSarcasm(String user, String creator, DateTime timestamp, boolean withComment) {
        final Sarcasm sarcasm = new Sarcasm()
                .setQuote("I'm trying to imagine you with a personality.")
                .setContext("In a fight!")
                .stamp(timestamp)
                .setCreator(creator)
                .setUser(user);
        if (withComment) {
            final Comment comment = new Comment()
                    .setComment("Very funny! Cuz it's true!")
                    .stamp(timestamp)
                    .setUser(creator);
            sarcasm.addComment(comment);
        }
        return sarcasm;
    }

    @Test
    public void testPostSarcasms() throws Exception {
        LOGGER.debug("TEST: testPostSarcasms");

        final Sarcasm sarcasm0 = createSarcasm("joost", "jalie", DateTime.now(), true);
        final Sarcasm sarcasm1 = createSarcasm("gert", "joost", DateTime.now(), false);

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

        final Sarcasm sarcasm = createSarcasm("jalie", "gert", DateTime.now(), true);

        this.mongoDBSarcasmRepository.save(sarcasm);
        final String sarcasmId = sarcasm.getId();
        LOGGER.debug("Generated Sarcasm id: " + sarcasmId);

        given().log().all()
                .when().get("/sarcasms/{id}", sarcasmId)
                .then().log().all()
                .statusCode(HttpStatus.OK.value())
                .body("user", Matchers.is("jalie"))
                .body("creator", Matchers.is("gert"))
                .body("quote", Matchers.is("I'm trying to imagine you with a personality."))
                .body("comments.user", Matchers.hasItem("gert"))
                .body("comments.comment", Matchers.hasItem("Very funny! Cuz it's true!"));

    }

    @Test
    public void testUpdateSarcasms() throws Exception {
        LOGGER.debug("TEST: testUpdateSarcasms");

        final Sarcasm sarcasm = createSarcasm("jalie", "gert", DateTime.now(), true);

        this.mongoDBSarcasmRepository.save(sarcasm);
        final String sarcasmId = sarcasm.getId();
        LOGGER.debug("Generated Sarcasm id: " + sarcasmId);

        final Sarcasm newSarcasm = createSarcasm("joost", "gert", DateTime.now(), false)
                .setQuote("This isn't an office. It's Hell with fluorescent lighting.")
                .setContext("On a good office day!");

        given().log().all()
                .contentType(ContentType.JSON).body(new VoteRequest().setSarcasmId(sarcasmId))
                .when().post("/downvote")
                .then().log().all()
                .statusCode(HttpStatus.GONE.value())
                .body("message", Matchers.is("You cannot vote for your own sarcasm!"));

        given().log().all()
                .contentType(ContentType.JSON).body(newSarcasm)
                .when().put("/sarcasms/{id}", sarcasmId)
                .then().log().all()
                .statusCode(HttpStatus.NO_CONTENT.value());

        given().log().all()
                .contentType(ContentType.JSON).body(new VoteRequest().setSarcasmId(sarcasmId))
                .when().post("/downvote")
                .then().log().all()
                .statusCode(HttpStatus.CREATED.value())
                .body("cast", Matchers.is(true));

        given().log().all()
                .when().get("/sarcasms/{id}", sarcasmId)
                .then().log().all()
                .statusCode(HttpStatus.OK.value())
                .body("user", Matchers.is("joost"))
                .body("voteTotal", Matchers.is(-1))
                .body("quote", Matchers.is("This isn't an office. It's Hell with fluorescent lighting."))
                .body("comments", Matchers.empty());
    }

    @Test
    public void testDeleteSarcasms() throws Exception {
        LOGGER.debug("TEST: testDeleteSarcasms");
        final Sarcasm sarcasm = createSarcasm("jalie", "gert", DateTime.now(), true);

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
        final Sarcasm sarcasmFour = createSarcasm("jalie", "gert", now.minus(org.joda.time.Days.days(6)), false);
        sarcasmFour.upVote("kenzo");
        sarcasmFour.upVote("joost");
        sarcasmFour.upVote("gert");
        mongoDBSarcasmRepository.save(sarcasmFour);
        LOGGER.debug("Generated Sarcasm id: " + sarcasmFour.getId());

        final Sarcasm sarcasmThree = createSarcasm("jalie", "joost", now.minus(org.joda.time.Days.days(5)), false);
        sarcasmThree.downVote("joost");
        sarcasmThree.upVote("wijlen");
        sarcasmThree.upVote("ikke");
        sarcasmThree.upVote("gij");
        mongoDBSarcasmRepository.save(sarcasmThree);
        LOGGER.debug("Generated Sarcasm id: " + sarcasmThree.getId());

        final Sarcasm sarcasmTwo = createSarcasm("jalie", "joost", now.minus(org.joda.time.Days.days(4)), false);
        sarcasmTwo.upVote("ikke");
        sarcasmTwo.downVote("gij");
        sarcasmTwo.upVote("wij");
        sarcasmTwo.upVote("zij");
        sarcasmTwo.upVote("jullie");
        mongoDBSarcasmRepository.save(sarcasmTwo);
        LOGGER.debug("Generated Sarcasm id: " + sarcasmTwo.getId());

        final Sarcasm sarcasmOne = createSarcasm("jalie", "gert", now.minus(org.joda.time.Days.days(1)), false);
        sarcasmOne.upVote("ikke");
        sarcasmOne.downVote("gij");
        sarcasmOne.upVote("zij");
        mongoDBSarcasmRepository.save(sarcasmOne);
        LOGGER.debug("Generated Sarcasm id: " + sarcasmOne.getId());

        final Sarcasm sarcasmZero = createSarcasm("jalie", "tom", now, false);
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
        final Sarcasm sarcasm = createSarcasm("joost", "tom", DateTime.now(), false);
        sarcasm.upVote("gert");
        mongoDBSarcasmRepository.save(sarcasm);
        LOGGER.debug("Generated Sarcasm id: " + sarcasm.getId());

        // TODO make sure when security is in place to try to (up/down)vote as "joost"
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
                .body("cast", Matchers.is(false))
                .body("message", Matchers.is("Vote already cast!"));

    }

    @Test
    public void testTotalVotes() {
        final DateTime now = new DateTime();

        final Sarcasm sarcasmFour = createSarcasm("jalie", "gert", now.minus(org.joda.time.Days.days(6)), false);
        sarcasmFour.upVote("kenzo");
        sarcasmFour.upVote("joost");
        sarcasmFour.upVote("gert");
        mongoDBSarcasmRepository.save(sarcasmFour);
        LOGGER.debug("Generated Sarcasm id: " + sarcasmFour.getId());

        final Sarcasm sarcasmThree = createSarcasm("jalie", "tom", now.minus(org.joda.time.Days.days(5)), false);
        sarcasmThree.downVote("joost");
        sarcasmThree.upVote("wijlen");
        sarcasmThree.upVote("ikke");
        sarcasmThree.upVote("gij");
        mongoDBSarcasmRepository.save(sarcasmThree);
        LOGGER.debug("Generated Sarcasm id: " + sarcasmThree.getId());

        final Sarcasm sarcasmTwo = createSarcasm("jalie", "tom", now.minus(org.joda.time.Days.days(4)), false);
        sarcasmTwo.upVote("ikke");
        sarcasmTwo.downVote("gij");
        sarcasmTwo.upVote("wij");
        sarcasmTwo.upVote("zij");
        sarcasmTwo.upVote("jullie");
        mongoDBSarcasmRepository.save(sarcasmTwo);
        LOGGER.debug("Generated Sarcasm id: " + sarcasmTwo.getId());

        final Sarcasm sarcasmOne = createSarcasm("jalie", "joost", now.minus(org.joda.time.Days.days(1)), false);
        sarcasmOne.upVote("ikke");
        sarcasmOne.downVote("gij");
        sarcasmOne.upVote("zij");
        mongoDBSarcasmRepository.save(sarcasmOne);
        LOGGER.debug("Generated Sarcasm id: " + sarcasmOne.getId());

        final Sarcasm sarcasmZero = createSarcasm("jalie", "gert", now, false);
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

        final Sarcasm sarcasmThree = createSarcasm("jalie", "gert", now.minus(org.joda.time.Days.days(5)), false);
        sarcasmThree.downVote("joost");
        sarcasmThree.upVote("wijlen");
        sarcasmThree.upVote("ikke");
        sarcasmThree.upVote("gij");
        mongoDBSarcasmRepository.save(sarcasmThree);
        LOGGER.debug("Generated Sarcasm id: " + sarcasmThree.getId());

        final Sarcasm sarcasmTwo = createSarcasm("gert", "jalie", now.minus(org.joda.time.Days.days(4)), false);
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

        final Sarcasm sarcasmOne = createSarcasm("joost", "gert", now.minus(org.joda.time.Days.days(1)), false);
        sarcasmOne.upVote("ikke");
        sarcasmOne.downVote("gij");
        sarcasmOne.upVote("zij");
        mongoDBSarcasmRepository.save(sarcasmOne);
        LOGGER.debug("Generated Sarcasm id: " + sarcasmOne.getId());

        final Sarcasm sarcasmZero = createSarcasm("jalie", "joost", now, false);
        sarcasmZero.upVote("ikke");
        sarcasmZero.downVote("gij");
        sarcasmZero.upVote("zij");
        sarcasmZero.upVote("wij");
        sarcasmZero.upVote("jullie");
        sarcasmZero.upVote("iemand");
        mongoDBSarcasmRepository.save(sarcasmZero);

        given().log().all().config(newConfig().jsonConfig(jsonConfig().numberReturnType(JsonPathConfig.NumberReturnType.FLOAT_AND_DOUBLE)))
                .contentType(ContentType.JSON).body(new StatsRequest().setUser("jalie").setPeriodExpression("1w"))
                .when().post("/votestats")
                .then().log().all()
                .statusCode(HttpStatus.OK.value())
                .body("voteStats.jalie.sum", Matchers.is(6.0F))
                .body("voteStats.jalie.count", Matchers.is(2))
                .body("voteStats.jalie.max", Matchers.is(4.0F))
                .body("voteStats.jalie.min", Matchers.is(2.0F));

        given().log().all().config(newConfig().jsonConfig(jsonConfig().numberReturnType(JsonPathConfig.NumberReturnType.FLOAT_AND_DOUBLE)))
                .contentType(ContentType.JSON).body(new StatsRequest().setUser("*").setPeriodExpression("3d"))
                .when().post("/votestats")
                .then().log().all()
                .statusCode(HttpStatus.OK.value())
                .body("voteStats.jalie.sum", Matchers.is(4.0F))
                .body("voteStats.jalie.count", Matchers.is(1))
                .body("voteStats.jalie.max", Matchers.is(4.0F))
                .body("voteStats.jalie.min", Matchers.is(4.0F))

                .body("voteStats.joost.sum", Matchers.is(1.0F))
                .body("voteStats.joost.count", Matchers.is(1))
                .body("voteStats.joost.max", Matchers.is(1.0F))
                .body("voteStats.joost.min", Matchers.is(1.0F))

                .body("voteStats.gert.sum", Matchers.is(0.0F))
                .body("voteStats.gert.count", Matchers.is(0))
                .body("voteStats.gert.max", Matchers.is("-Infinity"))
                .body("voteStats.gert.min", Matchers.is("Infinity"));

        Thread.sleep(500000000);

    }

}
