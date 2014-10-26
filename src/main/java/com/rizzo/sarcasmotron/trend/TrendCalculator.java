package com.rizzo.sarcasmotron.trend;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.rizzo.sarcasmotron.domain.Sarcasm;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.SynchronizedDescriptiveStatistics;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.joda.time.DateTime;
import org.elasticsearch.common.joda.time.ReadablePeriod;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogram;
import org.elasticsearch.search.aggregations.bucket.terms.LongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * curl -XGET 'http://localhost:9200/sarcasmotron/_search?pretty' -d '
 *  {
 *      "aggs": {
 *          "votes": {
 *              "date_histogram": {
 *                  "extended_bounds": {
 *                      "max": 500,
 *                      "min": 0
 *                  },
 *                  "field": "timestamp",
 *                  "format": "yyyy-MM-dd",
 *                  "interval": "day",
 *                  "min_doc_count": 0
 *              },
 *              "aggs": {
 *                  "items": {
 *                      "terms": {
 *                          "field": "votes",
 *                          "size": 0
 *                      }
 *                  }
 *              }
 *          }
 *      },
 *      "query": {
 *          "query_string": {
 *              "query": "user:joost"
 *          }
 *     }
 *  }'
 */
@Component
public class TrendCalculator {

    public static final String ES_INDEX = "sarcasmotron";
    public static final String ES_TYPE = "sarcasms";
    public static final String MONGO_COLLECTION = "sarcasm";

    @Autowired
    private Client client;

    @Autowired
    private MongoTemplate mongoTemplate;

    public Map<String, Double> calculateTrendLineForUser(String user,
                                                         ReadablePeriod baseLinePeriod,
                                                         DateHistogram.Interval baseLineInterval) {
        final DateTime now = DateTime.now();
        DateTime oneMonthAgo = now.minus(baseLinePeriod);
        final SearchResponse baseLine = client.prepareSearch(ES_INDEX)
                .setTypes(ES_TYPE)
                .setQuery(QueryBuilders.queryString("user: " + user))
                .addAggregation(
                        AggregationBuilders.dateHistogram("histogramAggVotes")
                                .extendedBounds(oneMonthAgo, now)
                                .field("timestamp")
                                .format(Sarcasm.TIMESTAMP_PATTERN)
                                .minDocCount(0L)
                                .interval(baseLineInterval)
                                .subAggregation(AggregationBuilders.terms("aggVotes").field("voteTotal").size(0))
                )
                .get();
        Map<String, Double> rollingZScores = Maps.newTreeMap();
        List<Double> values = Lists.newArrayList();
        final DateHistogram dateHistogram = baseLine.getAggregations().get("histogramAggVotes");
        DescriptiveStatistics descriptiveStatistics = new SynchronizedDescriptiveStatistics(7);
        assert(dateHistogram != null);
        for (DateHistogram.Bucket bucket : dateHistogram.getBuckets()) {
            final long docCount = bucket.getDocCount();
            Double voteCount = 0D;
            if(docCount > 0) {
                final Aggregations subAggregation = bucket.getAggregations();
                final LongTerms votes = subAggregation.get("aggVotes");
                final Terms.Bucket first = Iterables.getFirst(votes.getBuckets(), null);
                assert first != null;
                voteCount = first.getKeyAsNumber().doubleValue();
            }
            final double mean = descriptiveStatistics.getMean();
            final double standardDeviation = descriptiveStatistics.getStandardDeviation();
            Double zscore = (voteCount - mean) / standardDeviation;
            if(!zscore.isNaN() && !zscore.isInfinite()) {
                rollingZScores.put(bucket.getKey(), zscore);
            } else {
                rollingZScores.put(bucket.getKey(), 0D);
            }
            values.add(voteCount);
            descriptiveStatistics.addValue(voteCount);
        }
        return rollingZScores;
    }

    public List<String> getDistinctUsers() {
        return mongoTemplate.getCollection(MONGO_COLLECTION).distinct("user");
    }

}
