package com.rizzo.sarcasmotron.domain.mongodb;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

public class User {

    private static final Logger LOGGER = LoggerFactory.getLogger(User.class);

    @Id
    private String id;

    private String email;

    @JsonIgnore
    private Date lastLogin;

    private String nickName;

    private String givenName;

    private String surName;

    private String gravatar;

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

    public String getGravatar() {
        return gravatar;
    }

    public User setGravatar(String gravatar) {
        this.gravatar = gravatar;
        return this;
    }

    public static String hex(byte[] array) {
        StringBuilder sb = new StringBuilder();
        for (byte anArray : array) {
            sb.append(Integer.toHexString((anArray
                    & 0xFF) | 0x100).substring(1, 3));
        }
        return sb.toString();
    }
    public static String md5Hex(String message) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        return hex(md.digest(message.getBytes("CP1252")));
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("id", email)
                .append("lastLogin", lastLogin)
                .append("nickName", nickName)
                .append("givenName", givenName)
                .append("surName", surName)
                .append("email", email)
                .append("gravatar", gravatar)
                .toString();
    }
}
