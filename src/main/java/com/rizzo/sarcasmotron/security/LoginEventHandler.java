package com.rizzo.sarcasmotron.security;

import be.milieuinfo.security.openam.api.OpenAMUserdetails;
import com.rizzo.sarcasmotron.domain.mongodb.User;
import com.rizzo.sarcasmotron.mongodb.MongoDBUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

public class LoginEventHandler implements ApplicationListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoginEventHandler.class);

    @Autowired
    private MongoDBUserRepository mongoDBUserRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if(event instanceof AuthenticationSuccessEvent) {
            AuthenticationSuccessEvent successEvent =
                    (AuthenticationSuccessEvent) event;
            final Authentication authentication = successEvent.getAuthentication();
            final OpenAMUserdetails details = (OpenAMUserdetails) authentication.getDetails();
            final String nickname = details.getUsername();

            Query query = new Query();
            query.addCriteria(Criteria.where("nickName").is(nickname));

            final String surname = details.getAttributeValue("sn");
            final String givenname = details.getAttributeValue("givenname");
            final String email = details.getAttributeValue("mail");

            Update update = new Update();
            update.set("email", email);
            update.set("givenName", givenname);
            update.set("surName", surname);
            update.set("lastLogin", new Date());
            try {
                update.set("gravatar", User.md5Hex(email));
            } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
                LOGGER.error("Failed while calculating gravatar hash!");
            }

            final User user = mongoTemplate.findAndModify(query, update, FindAndModifyOptions.options().upsert(true).returnNew(true), User.class);
            if (user != null) {
                LOGGER.debug("LoginEvent for user: " + user.toString());
            } else {
                LOGGER.debug("Upsert didn't return new user!");
            }
        }
    }

}
