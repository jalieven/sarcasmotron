sarcasmotron
============

Sarcasm tracker for the enterprise



Trend Line functionality:

'''bash
curl -v -H "Content-Type: application/json" -XPOST 'http://localhost:50256/trend' -d '{"user": "jalie", "intervalExpression": "1d", "periodExpression": "7d"}'
'''

'''javascript
{
    "trendLine": {
        "2014-10-21T00:00:00.000+0000": 0.0,
        "2014-10-22T00:00:00.000+0000": 0.0,
        "2014-10-23T00:00:00.000+0000": 0.70710678118654746,
        "2014-10-24T00:00:00.000+0000": -4.618802153517005,
        "2014-10-25T00:00:00.000+0000": -1.4142135623730949,
        "2014-10-26T00:00:00.000+0000": -0.3956282840374723,
        "2014-10-27T00:00:00.000+0000": 1.8136906252750293
    }
}
'''

