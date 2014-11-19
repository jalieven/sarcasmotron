package com.rizzo.sarcasmotron.domain.elasticsearch;

import org.apache.commons.lang.builder.ToStringBuilder;

import java.util.Date;

public class ESUser {

    private String email;

    private String nickName;

    private String givenName;

    private String surName;

    private String gravatar;

    public String getEmail() {
        return email;
    }

    public ESUser setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getNickName() {
        return nickName;
    }

    public ESUser setNickName(String nickName) {
        this.nickName = nickName;
        return this;
    }

    public String getGivenName() {
        return givenName;
    }

    public ESUser setGivenName(String givenName) {
        this.givenName = givenName;
        return this;
    }

    public String getSurName() {
        return surName;
    }

    public ESUser setSurName(String surName) {
        this.surName = surName;
        return this;
    }

    public String getGravatar() {
        return gravatar;
    }

    public ESUser setGravatar(String gravatar) {
        this.gravatar = gravatar;
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("email", email)
                .append("nickName", nickName)
                .append("givenName", givenName)
                .append("surName", surName)
                .append("gravatar", gravatar)
                .toString();
    }
}
