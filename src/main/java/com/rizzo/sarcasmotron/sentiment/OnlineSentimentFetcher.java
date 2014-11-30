package com.rizzo.sarcasmotron.sentiment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;

import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class OnlineSentimentFetcher implements InitializingBean, SentimentFetcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(OnlineSentimentFetcher.class);

    private static final String SENTIMENT_ENDPOINT = "http://text-processing.com/api/sentiment/";

    @Value("${fetcher.maximum.request-timeout-milliseconds}")
    private int requestTimeoutInMs = 60000;

    @Value("${fetcher.maximum.connections-per-host}")
    private int maximumConnectionsPerHost = 150;

    @Value("${fetcher.user-agent}")
    private String userAgent = "Mozilla/5.0 (Windows NT 6.3; Win64; x64) AppleWebKit/537.36 (KHTML like Gecko) Chrome/37.0.2049.0 Safari/537.36";

    private AsyncHttpClient asyncHttpClient;

    @Override
    public void afterPropertiesSet() throws Exception {
        final AsyncHttpClientConfig.Builder clientConfigBuilder = new AsyncHttpClientConfig.Builder();
        AsyncHttpClientConfig.Builder httpClientConfigBuilder = clientConfigBuilder
                .setAllowPoolingConnection(true)
                .setRequestTimeoutInMs(requestTimeoutInMs)
                .setMaximumConnectionsPerHost(maximumConnectionsPerHost)
                .setUserAgent(userAgent);
        this.asyncHttpClient = new AsyncHttpClient(httpClientConfigBuilder.build());
    }

    @Override
    public SentimentProbability getSentiment(final String quote, final String context) {
        LOGGER.debug("Fetching sentiment for quote: " + quote);
        SentimentProbability sentimentProbability = null;
        try {
            final HashMap<String, Collection<String>> parameters = Maps.newHashMap();
            parameters.put("language", Lists.newArrayList("dutch"));
            parameters.put("text", Lists.newArrayList(quote + " " + context));
            final Response response = this.asyncHttpClient.preparePost(SENTIMENT_ENDPOINT).setParameters(parameters).execute().get(10, TimeUnit.SECONDS);
            final String responseBody = response.getResponseBody();
            ObjectMapper objectMapper = new ObjectMapper();
            sentimentProbability = objectMapper.readValue(responseBody, SentimentProbability.class);
        } catch (Exception e) {
            LOGGER.error("Fetch sentiment failed!", e);
        }
        return sentimentProbability;
    }
}
