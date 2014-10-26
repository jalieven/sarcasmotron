package com.rizzo.sarcasmotron.elasticsearch;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import com.rizzo.sarcasmotron.boot.Sarcasmotron;
import com.rizzo.sarcasmotron.domain.Sarcasm;
import com.rizzo.sarcasmotron.domain.web.TrendRequest;
import com.rizzo.sarcasmotron.mongodb.MongoDBSarcasmRepository;
import com.rizzo.sarcasmotron.trend.TrendCalculator;
import org.joda.time.DateTime;
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

import java.util.List;

import static com.jayway.restassured.RestAssured.given;
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
    public void testBaseLine() {
        mongoDBSarcasmRepository.deleteAll();
        // first create some sarcastic quotes
        final Sarcasm sarcasmFour = new Sarcasm()
                .setUser("jalie")
                .setContext("Some context")
                .setQuote("Very sarcastic quote")
                .upVote("jalie")
                .upVote("joost")
                .upVote("gert")
                .stamp(new DateTime().minus(org.joda.time.Days.days(7)));
        mongoDBSarcasmRepository.save(sarcasmFour);

        final Sarcasm sarcasmThree = new Sarcasm()
                .setUser("jalie")
                .setContext("Some context")
                .setQuote("Very sarcastic quote")
                .downVote("joost")
                .upVote("jalie")
                .stamp(new DateTime().minus(org.joda.time.Days.days(6)));
        mongoDBSarcasmRepository.save(sarcasmThree);

        final Sarcasm sarcasmTwo = new Sarcasm()
                .setUser("jalie")
                .setContext("Some context")
                .setQuote("Very sarcastic quote")
                .upVote("ikke")
                .downVote("gij")
                .upVote("wij")
                .upVote("zij")
                .stamp(new DateTime().minus(org.joda.time.Days.days(5)));
        mongoDBSarcasmRepository.save(sarcasmTwo);

        final Sarcasm sarcasmOne = new Sarcasm()
                .setUser("jalie")
                .setContext("Some context")
                .setQuote("Very sarcastic quote")
                .upVote("ikke")
                .downVote("gij")
                .upVote("wij")
                .upVote("zij")
                .stamp(new DateTime().minus(org.joda.time.Days.days(2)));
        mongoDBSarcasmRepository.save(sarcasmOne);

        final List<String> distinctUsers = trendCalculator.getDistinctUsers();
        assertEquals(1, distinctUsers.size());
        assertEquals("jalie", distinctUsers.get(0));

        final TrendRequest trendRequest = new TrendRequest()
                .setUser(distinctUsers.get(0))
                .setPeriodExpression("14d")
                .setIntervalExpression("1d");
        given()
                .contentType(ContentType.JSON).body(trendRequest)
                .when().post("/trend")
                .then()
                .statusCode(HttpStatus.OK.value());

    }

}
