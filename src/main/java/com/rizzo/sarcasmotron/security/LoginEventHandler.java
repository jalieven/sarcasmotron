package com.rizzo.sarcasmotron.security;

import be.milieuinfo.security.openam.api.OpenAMUserdetails;
import com.rizzo.sarcasmotron.domain.mongodb.User;
import com.rizzo.sarcasmotron.mongodb.MongoDBUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;

import java.util.Date;

public class LoginEventHandler implements ApplicationListener {

    @Autowired
    private MongoDBUserRepository mongoDBUserRepository;

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if(event instanceof AuthenticationSuccessEvent) {
            AuthenticationSuccessEvent successEvent =
                    (AuthenticationSuccessEvent) event;
            final Authentication authentication = successEvent.getAuthentication();
            final OpenAMUserdetails details = (OpenAMUserdetails) authentication.getDetails();
            final String nickname = details.getUsername();
            User user = mongoDBUserRepository.findOneByNickName(nickname);
            if (user == null) {
                final String surname = details.getAttributeValue("sn");
                final String givenname = details.getAttributeValue("givenname");
                final String email = details.getAttributeValue("mail");
                user = new User()
                        .setLastLogin(new Date())
                        .setEmail(email)
                        .setSurName(surname)
                        .setGivenName(givenname)
                        .setNickName(nickname);
            } else {
                user.setLastLogin(new Date());
            }
            mongoDBUserRepository.save(user);
        }
    }

}
