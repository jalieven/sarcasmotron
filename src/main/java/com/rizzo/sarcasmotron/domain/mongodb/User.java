package com.rizzo.sarcasmotron.domain.mongodb;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.springframework.data.annotation.Id;

import java.util.Date;

// TODO when someone logs in: persist this POJO in MongoDB.
// will be used for user selection on the UI
public class User {

    @Id
    private String id;

    private Date lastLogin;

    private String nickName;

    private String givenName;

    private String surName;

    private String email;

    public String getId() {
        return id;
    }

    public User setId(String id) {
        this.id = id;
        return this;
    }

    public Date getLastLogin() {
        return lastLogin;
    }

    public User setLastLogin(Date lastLogin) {
        this.lastLogin = lastLogin;
        return this;
    }

    public String getNickName() {
        return nickName;
    }

    public User setNickName(String nickName) {
        this.nickName = nickName;
        return this;
    }

    public String getGivenName() {
        return givenName;
    }

    public User setGivenName(String givenName) {
        this.givenName = givenName;
        return this;
    }

    public String getSurName() {
        return surName;
    }

    public User setSurName(String surName) {
        this.surName = surName;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public User setEmail(String email) {
        this.email = email;
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("id", id)
                .append("lastLogin", lastLogin)
                .append("nickName", nickName)
                .append("givenName", givenName)
                .append("surName", surName)
                .append("email", email)
                .toString();
    }
}