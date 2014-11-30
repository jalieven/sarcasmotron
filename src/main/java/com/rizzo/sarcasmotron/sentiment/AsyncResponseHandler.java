package com.rizzo.sarcasmotron.sentiment;

public interface AsyncResponseHandler<T> {

    T onCompleted(String quote, String context, T response);

    void onThrowable(Throwable t);

}
