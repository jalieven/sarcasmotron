#!/bin/sh
curl -XGET 'http://localhost:9200/sarcasmotron/_search?pretty' -d '{
	"query": {
           "query_string": {
               "query": "user:jalie"
           }
      },
  	"aggs": {
		"votes": {
 			"value_count": {
 				"field": "voteTotal"
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
 	}
}'