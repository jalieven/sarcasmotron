package com.rizzo.sarcasmotron.pusher;


import com.pusher.rest.Pusher;
import com.rizzo.sarcasmotron.domain.mongodb.Sarcasm;
import org.springframework.beans.factory.annotation.Value;

import java.util.Collections;

public class PusherService {

    @Value(value = "${pusher.appId}")
    private String appId;

    @Value(value = "${pusher.appKey}")
    private String apiKey;

    @Value(value = "${pusher.apiSecret}")
    private String apiSecret;

    public void pushSarcasm(Sarcasm sarcasm) {
        Pusher pusher = new Pusher(appId, apiKey, apiSecret);
        pusher.trigger("sarcasm", "create", Collections.singletonMap("sarcasm", sarcasm));
    }

}
