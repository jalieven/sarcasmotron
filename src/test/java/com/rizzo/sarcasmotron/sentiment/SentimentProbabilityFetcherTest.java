package com.rizzo.sarcasmotron.sentiment;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SentimentProbabilityFetcherTest {

    private OnlineSentimentFetcher onlineSentimentFetcher;

    @Before
    public void setUp() throws Exception {
        this.onlineSentimentFetcher = new OnlineSentimentFetcher();
        onlineSentimentFetcher.afterPropertiesSet();
    }

    @Test
    public void testFetchPositive() {
        final SentimentProbability sentimentProbability = onlineSentimentFetcher.getSentiment("goed, zeer goed", "excellent");
        assertEquals("pos", sentimentProbability.getLabel());
    }

    @Test
    public void testFetchNeutral() {
        final SentimentProbability sentimentProbability = onlineSentimentFetcher.getSentiment("ik ben een", "zwitser");
        assertEquals("neutral", sentimentProbability.getLabel());
    }

    @Test
    public void testFetchNegative() {
        final SentimentProbability sentimentProbability = onlineSentimentFetcher.getSentiment("verschrikkelijk", "slecht");
        assertEquals("neg", sentimentProbability.getLabel());
    }
}
