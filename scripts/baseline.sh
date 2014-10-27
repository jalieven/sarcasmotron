#!/bin/sh
curl -XGET 'http://localhost:9200/sarcasmotron/_search?pretty' -d '{
      "aggs": {
          "votes": {
              "date_histogram": {
                  "extended_bounds": {
                      "max": 500,
                      "min": 0
                  },
                  "field": "timestamp",
                  "format": "yyyy-MM-dd",
                  "interval": "day",
                  "min_doc_count": 0
              },
              "aggs": {
                  "items": {
                      "terms": {
                          "field": "votes",
                          "size": 0
                      }
                  }
              }
          }
      },
      "query": {
          "query_string": {
              "query": "user:jalie"
          }
     }
}'
