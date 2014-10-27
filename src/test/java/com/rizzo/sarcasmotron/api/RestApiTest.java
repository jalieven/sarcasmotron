package com.rizzo.sarcasmotron.api;

import com.google.common.collect.Iterables;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import com.rizzo.sarcasmotron.boot.Sarcasmotron;
import com.rizzo.sarcasmotron.domain.Comment;
import com.rizzo.sarcasmotron.domain.Sarcasm;
import com.rizzo.sarcasmotron.elasticsearch.ElasticsearchSarcasmRepository;
import com.rizzo.sarcasmotron.mongodb.MongoDBSarcasmRepository;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.util.Date;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.RestAssured.when;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Sarcasmotron.class)
@WebAppConfiguration
@IntegrationTest("server.port=0")
@DirtiesContext
public class RestApiTest {

    @Value("${local.server.port}")
    private int port = 0;

    @Autowired
    private MongoDBSarcasmRepository mongoDBSarcasmRepository;

    @Autowired
    private ElasticsearchSarcasmRepository elasticsearchSarcasmRepository;

    private TestRestTemplate restTemplate = new TestRestTemplate();

    @Before
    public void setUp() {
        RestAssured.port = port;
    }

    @After
    public void tearDown() {
        mongoDBSarcasmRepository.deleteAll();
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
        sarcasm0.upVote("jalie");

        final Sarcasm sarcasm1 = new Sarcasm()
                .setQuote("I work 40 hours a week to be this poor.")
                .setContext("When I'm tired of doing the same every time.")
                .stamp()
                .setUser("gert");

        given()
                .contentType(ContentType.JSON).body(sarcasm0)
                .when().post("/sarcasms")
                .then()
                .statusCode(HttpStatus.CREATED.value());

        given()
                .contentType(ContentType.JSON).body(sarcasm1)
                .when().post("/sarcasms")
                .then()
                .statusCode(HttpStatus.CREATED.value());

        final Iterable<Sarcasm> all = elasticsearchSarcasmRepository.findAll();
        assertEquals(2, Iterables.size(all));
    }

    @Test
    public void testGetSarcasms() throws Exception {
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

        when().get("/sarcasms/{id}", sarcasmId)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("user", Matchers.is("jalie"))
                .body("quote", Matchers.is("I'm trying to imagine you with a personality."))
                .body("comments.user", Matchers.hasItem("jalie"))
                .body("comments.comment", Matchers.hasItem("Very funny! Cuz it's true!"));

    }

    @Test
    public void testUpdateSarcasms() throws Exception {
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
        sarcasm.upVote("gert");

        this.mongoDBSarcasmRepository.deleteAll();
        this.mongoDBSarcasmRepository.save(sarcasm);
        final String sarcasmId = sarcasm.getId();

        final Sarcasm newSarcasm = new Sarcasm()
                .setQuote("This isn't an office. It's Hell with fluorescent lighting.")
                .setContext("On a good office day!")
                .stamp()
                .setUser("joost");
        newSarcasm.downVote("jalie");

        given()
                .contentType(ContentType.JSON).body(newSarcasm)
                .when().put("/sarcasms/{id}", sarcasmId)
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());

        when().get("/sarcasms/{id}", sarcasmId)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("user", Matchers.is("joost"))
                .body("votes", Matchers.is(-1))
                .body("quote", Matchers.is("This isn't an office. It's Hell with fluorescent lighting."))
                .body("comments", Matchers.empty());
    }

    @Test
    public void testDeleteSarcasms() throws Exception {
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

        when().delete("/sarcasms/{id}", sarcasmId)
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());

        assertEquals(0, this.mongoDBSarcasmRepository.findAll().size());
    }


}
