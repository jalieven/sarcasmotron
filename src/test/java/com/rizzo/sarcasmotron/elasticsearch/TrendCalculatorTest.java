package com.rizzo.sarcasmotron.elasticsearch;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.path.json.config.JsonPathConfig;
import com.rizzo.sarcasmotron.boot.Sarcasmotron;
import com.rizzo.sarcasmotron.domain.Sarcasm;
import com.rizzo.sarcasmotron.domain.web.TrendRequest;
import com.rizzo.sarcasmotron.mongodb.MongoDBSarcasmRepository;
import com.rizzo.sarcasmotron.trend.TrendCalculator;
import org.hamcrest.Matchers;
import org.hamcrest.number.IsCloseTo;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Days;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.config.JsonConfig.jsonConfig;
import static com.jayway.restassured.config.RestAssuredConfig.newConfig;
import static junit.framework.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Sarcasmotron.class)
@WebAppConfiguration
@IntegrationTest("server.port=0")
@DirtiesContext
public class TrendCalculatorTest {

    @Value("${local.server.port}")
    private int port = 0;

    @Autowired
    private TrendCalculator trendCalculator;

    @Autowired
    private MongoDBSarcasmRepository mongoDBSarcasmRepository;

    @Before
    public void setUp() {
        RestAssured.port = port;
    }

    @Test
    public void testBaseLine() throws InterruptedException {
        mongoDBSarcasmRepository.deleteAll();

        final DateTime now = new DateTime();
        final DateTime pastMidnight = now.withTimeAtStartOfDay();
        final DateTime yesterdayMidnight = now.minus(Days.days(1)).withTimeAtStartOfDay();

        // first create some sarcastic quotes
        final Sarcasm sarcasmFour = new Sarcasm()
                .setUser("jalie")
                .setContext("Some context")
                .setQuote("Very sarcastic quote")
                .stamp(now.minus(org.joda.time.Days.days(6)));
        sarcasmFour.upVote("jalie");
        sarcasmFour.upVote("joost");
        sarcasmFour.upVote("gert");
        mongoDBSarcasmRepository.save(sarcasmFour);

        final Sarcasm sarcasmThree = new Sarcasm()
                .setUser("jalie")
                .setContext("Some context")
                .setQuote("Very sarcastic quote")
                .stamp(now.minus(org.joda.time.Days.days(5)));
        sarcasmThree.downVote("joost");
        sarcasmThree.upVote("jalie");
        sarcasmThree.upVote("ikke");
        sarcasmThree.upVote("gij");
        mongoDBSarcasmRepository.save(sarcasmThree);

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

        final Sarcasm sarcasmOne = new Sarcasm()
                .setUser("jalie")
                .setContext("Some context")
                .setQuote("Very sarcastic quote")
                .stamp(now.minus(org.joda.time.Days.days(1)));
        sarcasmOne.upVote("ikke");
        sarcasmOne.downVote("gij");
        sarcasmOne.upVote("zij");
        mongoDBSarcasmRepository.save(sarcasmOne);

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

        final List<String> distinctUsers = trendCalculator.getDistinctUsers();
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
        given().config(newConfig().jsonConfig(jsonConfig().numberReturnType(JsonPathConfig.NumberReturnType.BIG_DECIMAL)))
                .contentType(ContentType.JSON).body(trendRequest)
                .when().post("/trend")
                .then()
                .statusCode(HttpStatus.OK.value())
        .body("trendLine.'" + timezoneNeutralYesterdayIntervalKey + "'", Matchers.is(new BigDecimal("-0.3956282840374723")))
        .body("trendLine.'" + timezoneNeutralIntervalKey + "'", Matchers.is(new BigDecimal("1.8136906252750293")));

        Thread.sleep(5000000);
    }

}
