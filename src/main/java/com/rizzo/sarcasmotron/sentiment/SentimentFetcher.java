package com.rizzo.sarcasmotron.sentiment;

public interface SentimentFetcher {

    SentimentProbability getSentiment(String quote, String context);

}
